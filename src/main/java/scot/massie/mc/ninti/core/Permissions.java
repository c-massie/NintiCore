package scot.massie.mc.ninti.core;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.events.Event;
import scot.massie.lib.events.InvokableEvent;
import scot.massie.lib.events.ProtectedEvent;
import scot.massie.lib.events.SetEvent;
import scot.massie.lib.events.args.EventArgs;
import scot.massie.lib.permissions.PermissionStatus;
import scot.massie.lib.permissions.PermissionsRegistry;
import scot.massie.lib.permissions.exceptions.UserMissingPermissionException;
import scot.massie.mc.ninti.core.exceptions.PlayerMissingPermissionException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Static permissions registry, with methods for checking the permissions of players.
 *
 * This class wraps an instance of {@link PermissionsRegistry<UUID>} and provides access to its methods through static
 * functions. These static methods implement thread safety by locking on the registry object.
 *
 * Permissions are saved in the format defined by {@link PermissionsRegistry}, at permissions.txt and
 * permission_groups.txt in the server root folder.
 */
public final class Permissions
{
    //region Inner classes
    //region Event args

    /**
     * Event args for suggestions for permissions being requested.
     */
    public static final class PermissionsAreBeingSuggestedEventArgs implements EventArgs
    {
        /**
         * Creates a new instance with the given suggestions.
         * @param suggestions The initial suggestions to suggest.
         */
        PermissionsAreBeingSuggestedEventArgs(Set<String> suggestions)
        { this.suggestedPermissions = suggestions; }

        private Set<String> suggestedPermissions;
        private boolean isUnmodified = true;

        /**
         * Suggests a new permission.
         * @param permission The permission to be suggested.
         */
        public void suggestPermission(String permission)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            suggestedPermissions.add(permission);
        }

        /**
         * Suggests a number of new permissions.
         * @param permissions The permissions to be suggested.
         */
        public void suggestPermissions(Collection<String> permissions)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            suggestedPermissions.addAll(permissions);
        }

        /**
         * Suggests a number of new permissions.
         * @param permissions The permissions to be suggested.
         */
        public void suggestPermissions(String... permissions)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            Collections.addAll(suggestedPermissions, permissions);
        }

        /**
         * Gets the suggestions currently recommended.
         * @return A set containing the currently suggested permissions.
         */
        public Set<String> getSuggestedPermissions()
        { return new HashSet<>(suggestedPermissions); }
    }
    //endregion

    //region Inner static function stores

    /**
     * Management for permission presets. Presets are pre-defined groups of permissions (possibly including groups, by
     * prefixing them with "#") that may be assigned to a player or group, or used to generate a set of preset
     * permissions.
     */
    public static final class Presets
    {
        private Presets()
        {}

        /**
         * The preset name for server admins.
         */
        public static final String ADMIN = "Admin";

        /**
         * The preset name for server moderators.
         */
        public static final String MOD = "Mod";

        /**
         * The preset name for just standard players on a server.
         */
        public static final String PLAYER = "Player";

        /**
         * The preset name for players that aren't considered full players. Guests, visitors, probationary new players,
         * etc.
         */
        public static final String GUEST = "Guest";

        private static final Map<String, Set<String>> presets = new HashMap<>();

        /**
         * Gets the names of all current presets.
         * @return A list of preset names, in alphabetical order.
         */
        public static List<String> getPresetNames()
        {
            Collection<String> presetNames;

            synchronized(presets)
            { presetNames = presets.keySet(); }

            List<String> result = new ArrayList<>(presetNames);
            result.sort(Comparator.naturalOrder());
            return result;
        }

        /**
         * Gets the permissions assigned to a given preset.
         * @param presetName The name of the preset to get the permissions of.
         * @return A collection of permission strings that were assigned to the preset.
         */
        public static Collection<String> getPresetPermissions(String presetName)
        {
            Set<String> preset;

            synchronized(presets)
            { preset = presets.get(presetName); }

            return preset != null ? Collections.unmodifiableCollection(preset) : Collections.emptySet();
        }

        /**
         * Gets the names of presets and the permissions assigned to them as a map.
         * @return A map, where the keys are the names of presets and the values are sets of strings, each containing
         *         the permission strings associated with that preset.
         */
        public static Map<String, Set<String>> getPresets()
        {
            synchronized(presets)
            { return Collections.unmodifiableMap(new HashMap<>(presets)); }
        }

        /**
         * Assigns a permission to a preset.
         * @param presetName The name of the preset to assign a permission to.
         * @param permission The permission to assign.
         */
        public static void addPermission(String presetName, String permission)
        {
            synchronized(presets)
            { presets.computeIfAbsent(presetName, s -> new HashSet<>()).add(permission); }
        }

        /**
         * Assigns a number of permissions to a preset.
         * @param presetName The name of the preset to assign a permission to.
         * @param permissions The permissions to assign.
         */
        public static void addPermissions(String presetName, String... permissions)
        {
            synchronized(presets)
            { Collections.addAll(presets.computeIfAbsent(presetName, s -> new HashSet<>()), permissions); }
        }

        /**
         * Assigns a number of permissions to a preset.
         * @param presetName The name of the preset to assign a permission to.
         * @param permissions The permissions to assign.
         */
        public static void addPermissions(String presetName, Collection<String> permissions)
        {
            synchronized(presets)
            { presets.computeIfAbsent(presetName, s -> new HashSet<>()).addAll(permissions); }
        }

        /**
         * Assigns the permissions of a preset to a player.
         * @param presetName The name of the preset to apply to the player.
         * @param playerId The ID of the player to assign permissions to.
         */
        public static void assignToPlayer(String presetName, UUID playerId)
        { Permissions.Write.assignPlayerPreset(playerId, presetName); }

        /**
         * Assigns the permissions of a preset to a player.
         * @param presetName The name of the preset to apply to the player.
         * @param player The player to assign permissions to.
         */
        public static void assignToPlayer(String presetName, PlayerEntity player)
        { assignToPlayer(presetName, player.getUniqueID()); }

        /**
         * Assigns the permissions of a preset to a group.
         * @param presetName The name of the preset to apply to the group.
         * @param groupName The name of the group to assign permissions to.
         */
        public static void assignToGroup(String presetName, String groupName)
        { Permissions.Write.assignGroupPreset(groupName, presetName); }
    }

    /**
     * Management for permission suggestions. When permissions are being modified via command, permissions to give to a
     * user or group may be suggested, and the permissions suggested are handled here.
     */
    public static final class Suggestions
    {
        private Suggestions()
        {}

        private static final Set<String> permissionsToBeSuggested = new HashSet<>();

        private static final InvokableEvent<PermissionsAreBeingSuggestedEventArgs> beingSuggested_internal
                = new SetEvent<>();

        /**
         * Event for suggestions being requested. This allows you to make additional suggestions based on current
         * circumstances.
         */
        public static final Event<PermissionsAreBeingSuggestedEventArgs> beingSuggested
                = new ProtectedEvent<>(beingSuggested_internal);

        // not public for now, until I decide whether to pass arguments to pass into the event args.

        /**
         * Gets a list of suggestions to suggest.
         * @return A list of provided suggestions to suggest, in alphabetical order.
         */
        static List<String> get()
        {
            PermissionsAreBeingSuggestedEventArgs eventArgs;

            synchronized(permissionsToBeSuggested)
            { eventArgs = new PermissionsAreBeingSuggestedEventArgs(permissionsToBeSuggested); }

            beingSuggested_internal.invoke(eventArgs);

            List<String> suggestions = new ArrayList<>(eventArgs.getSuggestedPermissions());
            suggestions.sort(Comparator.naturalOrder());
            return suggestions;
        }

        /**
         * Adds a permission to suggest when permissions are suggested.
         * @param permission The permission to add as a suggestion.
         */
        public static void add(String permission)
        {
            synchronized(permissionsToBeSuggested)
            { permissionsToBeSuggested.add(permission); }
        }

        /**
         * Adds a number of permissions to suggest when permissions are suggested.
         * @param permissions An array of permissions to add as suggestions.
         */
        public static void add(String... permissions)
        {
            synchronized(permissionsToBeSuggested)
            { Collections.addAll(permissionsToBeSuggested, permissions); }
        }

        /**
         * Adds a number of permissions to suggest when permissions are suggested.
         * @param permissions A collection of permission to add as suggestions.
         */
        public static void add(Collection<String> permissions)
        {
            synchronized(permissionsToBeSuggested)
            { permissionsToBeSuggested.addAll(permissions); }
        }
    }

    /**
     * Methods for modifying the contents of the registry.
     */
    public static final class Write
    {
        private Write()
        {}

        //region assign permissions/groups

        /**
         * Assigns a player a permission
         * @param player The player to assign a permission to.
         * @param permission The permission to assign to the player.
         * @see PermissionsRegistry#assignUserPermission(Comparable, String)
         */
        public static void assignPlayerPermission(PlayerEntity player, String permission)
        { assignPlayerPermission(player.getUniqueID(), permission); }

        /**
         * Assigns a player a permission
         * @param playerId The ID of the player to assign a permission to.
         * @param permission The permission to assign to the player.
         * @see PermissionsRegistry#assignUserPermission(Comparable, String)
         */
        public static void assignPlayerPermission(UUID playerId, String permission)
        {
            if(permission.startsWith("#"))
            {
                assignPlayerGroup(playerId, permission.substring(1));
                return;
            }
            else if(permission.startsWith("@"))
            {
                assignPlayerPreset(playerId, permission.substring(1));
                return;
            }

            synchronized(registry)
            { registry.assignUserPermission(playerId, permission); }
        }

        /**
         * Assigns a group a permission.
         * @param groupId The name of the group to assign a permission to.
         * @param permission The permission to assign to the group.
         * @see PermissionsRegistry#assignGroupPermission(String, String)
         */
        public static void assignGroupPermission(String groupId, String permission)
        {
            if(permission.startsWith("#"))
            {
                assignPermissionGroupGroup(groupId, permission.substring(1));
                return;
            }
            else if(permission.startsWith("@"))
            {
                assignGroupPreset(groupId, permission.substring(1));
                return;
            }

            synchronized(registry)
            { registry.assignGroupPermission(groupId, permission); }
        }

        /**
         * Assigns a player a group.
         * @param player The player to assign a group to.
         * @param groupId The name of the group to assign.
         * @see PermissionsRegistry#assignGroupToUser(Comparable, String)
         */
        public static void assignPlayerGroup(PlayerEntity player, String groupId)
        {
            synchronized(registry)
            { registry.assignGroupToUser(player.getUniqueID(), groupId); }
        }

        /**
         * Assigns a player a group.
         * @param playerId The ID of the player to assign a group to.
         * @param groupId The name of the group to assign.
         * @see PermissionsRegistry#assignGroupToUser(Comparable, String)
         */
        public static void assignPlayerGroup(UUID playerId, String groupId)
        {
            synchronized(registry)
            { registry.assignGroupToUser(playerId, groupId); }
        }

        /**
         * Assigns a group another group.
         * @param groupIdBeingAssignedTo The group to be assigned a group.
         * @param groupIdBeingAssigned The group to be assigned to the group.
         * @see PermissionsRegistry#assignGroupToGroup(String, String)
         */
        public static void assignPermissionGroupGroup(String groupIdBeingAssignedTo, String groupIdBeingAssigned)
        {
            synchronized(registry)
            { registry.assignGroupToGroup(groupIdBeingAssignedTo, groupIdBeingAssigned); }
        }

        /**
         * Assigns a player a preset collection of groups and permissions.
         * @param playerId The ID of the player to assign the preset to.
         * @param presetName The name of the preset to assign.
         * @see Presets
         */
        public static void assignPlayerPreset(UUID playerId, String presetName)
        {
            Collection<String> perms = Presets.getPresetPermissions(presetName);

            synchronized(registry)
            {
                for(String perm : perms)
                    registry.assignUserPermission(playerId, perm);
            }
        }

        /**
         * Assigns a player a preset collection of groups and permissions.
         * @param player The player to assign the preset to.
         * @param presetName The name of the preset to assign.
         * @see Presets
         */
        public static void assignPlayerPreset(PlayerEntity player, String presetName)
        { assignPlayerPreset(player.getUniqueID(), presetName); }

        /**
         * Assigns a group a preset collection of groups and permissions.
         * @param groupId The name of the group to assign the preset to.
         * @param presetName The name of the preset to assign.
         */
        public static void assignGroupPreset(String groupId, String presetName)
        {
            Collection<String> perms = Presets.getPresetPermissions(presetName);

            synchronized(registry)
            {
                for(String perm : perms)
                    registry.assignGroupPermission(groupId, perm);
            }
        }
        //endregion

        //region revoke permissions/groups

        /**
         * Removes a permission assigned to a player. Does not remove permissions that cover the provided one, nor
         * permissions that the provided one covers, only the specific permission. Does not negate the permission nor
         * remove it from assigned groups.
         * @param player The player to remove a permission from.
         * @param permission The permission to remove.
         * @see PermissionsRegistry#revokeUserPermission(Comparable, String)
         */
        public static void revokePlayerPermission(PlayerEntity player, String permission)
        { revokePlayerPermission(player.getUniqueID(), permission); }

        /**
         * Removes a permission assigned to a player. Does not remove permissions that cover the provided one, nor
         * permissions that the provided one covers, only the specific permission. Does not negate the permission nor
         * remove it from assigned groups.
         * @param playerId The ID of the player to remove a permission from.
         * @param permission The permission to remove.
         * @see PermissionsRegistry#revokeUserPermission(Comparable, String)
         */
        public static void revokePlayerPermission(UUID playerId, String permission)
        {
            if(permission.startsWith("#"))
            {
                removePlayerFromGroup(playerId, permission.substring(1));
                return;
            }

            synchronized(registry)
            { registry.revokeUserPermission(playerId, permission); }
        }

        /**
         * Removes a permission assigned to a group. Does not remove permissions that cover the provided one, nor
         * permissions that the provided one covers, only the specific permission. Does not negate the permission nor
         * remove it from assigned groups.
         * @param groupId The name of the group to remove a permission from.
         * @param permission The permission to remove.
         * @see PermissionsRegistry#revokeGroupPermission(String, String)
         */
        public static void revokeGroupPermission(String groupId, String permission)
        {
            if(permission.startsWith("#"))
            {
                removeGroupFromGroup(groupId, permission.substring(1));
                return;
            }

            synchronized(registry)
            { registry.revokeGroupPermission(groupId, permission); }
        }

        /**
         * Removes a group assigned to a player, a player from a group.
         * @param player The player to remove from the group.
         * @param groupId The name of the group to deässign from the player.
         * @see PermissionsRegistry#revokeGroupFromUser(Comparable, String)
         */
        public static void removePlayerFromGroup(PlayerEntity player, String groupId)
        {
            synchronized(registry)
            { registry.revokeGroupFromUser(player.getUniqueID(), groupId); }
        }

        /**
         * Removes a group assigned to a player, a player from a group.
         * @param playerId The ID of the player to remove from the group.
         * @param groupId The name of the group to deässign from the player.
         * @see PermissionsRegistry#revokeGroupFromUser(Comparable, String)
         */
        public static void removePlayerFromGroup(UUID playerId, String groupId)
        {
            synchronized(registry)
            { registry.revokeGroupFromUser(playerId, groupId); }
        }

        /**
         * Removes a group assigned to another group, a group from another group.
         * @param groupIdBeingDeassigned The name of the group to remove from the group.
         * @param groupIdBeingRemovedFrom The name of the group to deässign from the group.
         * @see PermissionsRegistry#revokeGroupFromGroup(String, String)
         */
        public static void removeGroupFromGroup(String groupIdBeingDeassigned, String groupIdBeingRemovedFrom)
        {
            synchronized(registry)
            { registry.revokeGroupFromGroup(groupIdBeingDeassigned, groupIdBeingRemovedFrom); }
        }
        //endregion

        //region clear

        /**
         * Removes all user and group information from the registry.
         * @see PermissionsRegistry#clear()
         */
        static void clear()
        {
            synchronized(registry)
            { registry.clear(); }
        }
        //endregion

        //region initialising

        /**
         * Replaces the contents of the registry with the presets, using preset names as group names.
         * @see Presets
         */
        static void initialisePermissionsWithPresets()
        {
            Set<Map.Entry<String, Set<String>>> presets = Presets.getPresets().entrySet();

            synchronized(registry)
            {
                registry.clear();

                for(Map.Entry<String, Set<String>> preset : presets)
                    for(String perm : preset.getValue())
                        registry.assignGroupPermission(preset.getKey(), perm);
            }
        }
        //endregion
    }
    //endregion
    //endregion

    private Permissions()
    {}

    //region fields

    /**
     * This is defined in the UUID contract.
     * @see UUID#toString()
     */
    static final int uuidStringLength = 36;

    private static final PermissionsRegistry<UUID> registry = new PermissionsRegistry<>(
            uuid ->
            {
                String username = UsernameCache.getLastKnownUsername(uuid);

                if(username == null)
                    username = "???";

                return username + " - " + uuid;
            },
            name -> UUID.fromString(name.substring(name.length() - uuidStringLength)),
            Paths.get("permissions.txt"),
            Paths.get("permission_groups.txt"));
    //endregion

    //region methods
    //region accessors
    //region hasPermissions

    /**
     * Gets whether or not the player represented by the ID has the given permission.
     * @param playerId The ID of the player.
     * @param permission The permission to check if the player has.
     * @return True if the player has the given permission. Otherwise, false.
     * @see PermissionsRegistry#userHasPermission(Comparable, String)
     */
    public static boolean playerHasPermission(UUID playerId, String permission)
    {
        if(permission.startsWith("#"))
            return playerIsInGroup(playerId, permission.substring(1));

        if(PluginUtils.playerIsOp(playerId))
            return true;

        synchronized(registry)
        { return registry.userHasPermission(playerId, permission); }
    }

    /**
     * Gets whether or not the player has the given permission.
     * @param player The player to check.
     * @param permission The permission to check if the player has.
     * @return True if the player has the given permission. Otherwise, false.
     * @see PermissionsRegistry#userHasPermission(Comparable, String)
     */
    public static boolean playerHasPermission(PlayerEntity player, String permission)
    {
        if(permission.startsWith("#"))
            return playerIsInGroup(player, permission.substring(1));

        if(PluginUtils.playerIsOp(player))
            return true;

        synchronized(registry)
        { return registry.userHasPermission(player.getUniqueID(), permission); }
    }

    /**
     * Gets whether or not the source of a command has the given permission.
     * @param commandContext The command context object of the command being run.
     * @param permissions The permissions to check for.
     * @return True if the command is being run by something that doesn't need permission (like the server console) or
     *         if the source has all the given permissions. Otherwise, false.
     */
    public static boolean commandSourceHasPermission(CommandContext<? extends CommandSource> commandContext,
                                                     String... permissions)
    { return commandSourceHasPermission(commandContext.getSource(), permissions); }

    /**
     * Gets whether or not the source of a command has the given permission.
     * @param commandSource The source of the command being run.
     * @param permissions The permissions to check for.
     * @return True if the command is being run by something that doesn't need permission (like the server console) or
     *         if the source has all the given permissions. Otherwise, false.
     */
    public static boolean commandSourceHasPermission(CommandSource commandSource,
                                                     String... permissions)
    {
        Entity sourceEntity = commandSource.getEntity();

        if(!(sourceEntity instanceof PlayerEntity))
            return true;

        PlayerEntity sourcePlayer = (PlayerEntity)sourceEntity;

        if(PluginUtils.playerIsOp(sourcePlayer))
            return true;

        Collection<String> permissionChecks = new ArrayList<>();
        Collection<String> groupChecks = new ArrayList<>();

        for(String p : permissions)
            if(p.startsWith("#"))
                groupChecks.add(p.substring(1));
            else
                permissionChecks.add(p);

        synchronized(registry)
        {
            for(String p : permissionChecks)
                if(!registry.userHasPermission(sourcePlayer.getUniqueID(), p))
                    return false;

            for(String g : groupChecks)
                if(!registry.userHasGroup(sourcePlayer.getUniqueID(), g))
                    return false;
        }

        return true;
    }

    /**
     * <p>Gets whether or not a group has the given permission.</p>
     *
     * <p>Checking whether a group has a given permission does not check the default permissions if no relevant
     * permissions have been assigned to it or any groups assigned to it.</p>
     * @param groupName The name of the group to check.
     * @param permission The permission to check if the group has.
     * @return True if the group has the given permission. Otherwise, false.
     * @see PermissionsRegistry#groupHasPermission(String, String)
     */
    public static boolean groupHasPermission(String groupName, String permission)
    {
        if(permission.startsWith("#"))
            return groupIsInGroup(groupName, permission.substring(1));

        synchronized(registry)
        { return registry.groupHasPermission(groupName, permission); }
    }
    //endregion

    //region assertHasPermissions

    /**
     * Asserts that a player represented by the ID has the given permission.
     * @param playerId The ID of the player.
     * @param permission The permission to assert that the player has.
     * @throws PlayerMissingPermissionException If the player does not have the given permission.
     */
    public static void assertPlayerHasPermission(UUID playerId, String permission)
            throws PlayerMissingPermissionException
    {
        if(PluginUtils.playerIsOp(playerId))
            return;

        synchronized(registry)
        {
            try
            { registry.assertUserHasPermission(playerId, permission); }
            catch(UserMissingPermissionException e)
            { throw new PlayerMissingPermissionException(playerId, permission); }
        }
    }

    /**
     * Asserts that a player has the given permission.
     * @param player The player.
     * @param permission The permission to assert that the player has.
     * @throws PlayerMissingPermissionException If the player does not have the given permission.
     */
    public static void assertPlayerHasPermission(PlayerEntity player, String permission)
            throws PlayerMissingPermissionException
    {
        if(PluginUtils.playerIsOp(player))
            return;

        synchronized(registry)
        {
            try
            { registry.assertUserHasPermission(player.getUniqueID(), permission); }
            catch(UserMissingPermissionException e)
            { throw new PlayerMissingPermissionException(player.getUniqueID(), permission); }
        }
    }

    /**
     * Asserts that the source of a command has the given permission.
     * @param commandContext The command context object of the command being run.
     * @param permissions The permissions to check for.
     * @throws PlayerMissingPermissionException If the command is being run by something that needs permission (that is,
     *                                          a player) and the source does not have all the given permissions.
     */
    public static void assertCommandSourceHasPermission(CommandContext<? extends CommandSource> commandContext,
                                                        String... permissions)
            throws PlayerMissingPermissionException
    { assertCommandSourceHasPermission(commandContext.getSource(), permissions); }

    /**
     * Asserts that the source of a command has the given permission.
     * @param commandSource The source of the command being run.
     * @param permissions The permissions to check for.
     * @throws PlayerMissingPermissionException If the command is being run by something that needs permission (that is,
     *                                          a player) and the source does not have all the given permissions.
     */
    public static void assertCommandSourceHasPermission(CommandSource commandSource,
                                                        String... permissions)
            throws PlayerMissingPermissionException
    {
        Entity sourceEntity = commandSource.getEntity();

        if(!(sourceEntity instanceof PlayerEntity))
            return;

        PlayerEntity sourcePlayer = (PlayerEntity)sourceEntity;

        if(PluginUtils.playerIsOp(sourcePlayer))
            return;

        synchronized(registry)
        {
            for(String p : permissions)
            {
                try
                { registry.assertUserHasPermission(sourcePlayer.getUniqueID(), p); }
                catch(UserMissingPermissionException e)
                { throw new PlayerMissingPermissionException(sourcePlayer, p); }
            }
        }
    }

    //endregion

    //region hasAnyPermissionsUnder
    public static boolean playerHasAnyPermissionUnder(UUID playerId, String permission)
    {
        if(PluginUtils.playerIsOp(playerId))
            return true;

        synchronized(registry)
        { return registry.userHasAnySubPermissionOf(playerId, permission); }
    }

    public static boolean playerHasAnyPermissionUnder(PlayerEntity player, String permission)
    {
        if(PluginUtils.playerIsOp(player.getUniqueID()))
            return true;

        synchronized(registry)
        { return registry.userHasAnySubPermissionOf(player.getUniqueID(), permission); }
    }

    public static boolean commandSourceHasAnyPermissionUnder(CommandContext<? extends CommandSource> commandContext,
                                                             String... permissions)
    { return commandSourceHasAnyPermissionUnder(commandContext.getSource(), permissions); }

    public static boolean commandSourceHasAnyPermissionUnder(CommandSource commandSource,
                                                             String... permissions)
    {
        Entity sourceEntity = commandSource.getEntity();

        if(!(sourceEntity instanceof PlayerEntity))
            return true;

        PlayerEntity sourcePlayer = (PlayerEntity)sourceEntity;

        if(PluginUtils.playerIsOp(sourcePlayer))
            return true;

        synchronized(registry)
        {
            for(String p : permissions)
                if(registry.userHasAnySubPermissionOf(sourcePlayer.getUniqueID(), p))
                    return true;
        }

        return false;
    }

    public static boolean groupHasAnyPermissionUnder(String groupName, String permission)
    {
        synchronized(registry)
        { return registry.groupHasAnySubPermissionOf(groupName, permission); }
    }
    //endregion

    //region getPermissionArgs

    /**
     * Gets the permission argument associated with the given player's permission.
     * @param playerId The ID of the player to get the argument of.
     * @param permission The permission to get the argument of.
     * @return The argument associated with the given permission of the given player. Null if the player doesn't have
     *         the given permission, or if the player has no argument associated with the given permission.
     * @see PermissionsRegistry#getUserPermissionArg(Comparable, String)
     */
    public static String getPlayerPermissionArg(UUID playerId, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionArg(playerId, permission); }
    }

    /**
     * Gets the permission argument associated with the given player's permission.
     * @param player The player to get the argument of.
     * @param permission The permission to get the argument of.
     * @return The argument associated with the given permission of the given player. Null if the player doesn't have
     *         the given permission, or if the player has no argument associated with the given permission.
     * @see PermissionsRegistry#getUserPermissionArg(Comparable, String)
     */
    public static String getPlayerPermissionArg(PlayerEntity player, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionArg(player.getUniqueID(), permission); }
    }

    /**
     * Gets the permission argument associated with the given player's permission.
     * @param playerProfile The player to get the argument of.
     * @param permission The permission to get the argument of.
     * @return The argument associated with the given permission of the given player. Null if the player doesn't have
     *         the given permission, or if the player has no argument associated with the given permission.
     * @see PermissionsRegistry#getUserPermissionArg(Comparable, String)
     */
    public static String getPlayerPermissionArg(GameProfile playerProfile, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionArg(playerProfile.getId(), permission); }
    }

    /**
     * Gets the permission argument associated with the given group's permission.
     * @param groupName The name of the group to get the argument of.
     * @param permission The permission to get the argument of.
     * @return The argument associated with the given permission of the given group. Null if the group doesn't have the
     *         given permission, or if the group has no argument associated with the given permission.
     * @see PermissionsRegistry#getGroupPermissionArg(String, String)
     */
    public static String getGroupPermissionArg(String groupName, String permission)
    {
        synchronized(registry)
        { return registry.getGroupPermissionArg(groupName, permission); }
    }
    //endregion

    //region getPermissionStatus

    /**
     * Gets all of the information pertaining to a permission's relationship with the specified player, as a single
     * object.
     * @param playerId The ID of the player to get the relationship information of the given permission of.
     * @param permission The permission to get the relationship information of for the specific player.
     * @return A PermissionStatus object, containing the permission, whether or not the player has it, and the
     *         permission argument if applicable.
     */
    public static PermissionStatus getPlayerPermissionStatus(UUID playerId, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionStatus(playerId, permission); }
    }

    /**
     * Gets all of the information pertaining to a permission's relationship with the given player, as a single
     * object.
     * @param player The player to get the relationship information of the given permission of.
     * @param permission The permission to get the relationship information of for the given player.
     * @return A PermissionStatus object, containing the permission, whether or not the player has it, and the
     *         permission argument if applicable.
     */
    public static PermissionStatus getPlayerPermissionStatus(PlayerEntity player, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionStatus(player.getUniqueID(), permission); }
    }

    /**
     * Gets all of the information pertaining to a permission's relationship with the given player, as a single
     * object.
     * @param playerProfile The profile of the player to get the relationship information of the given permission of.
     * @param permission The permission to get the relationship information of for the given player.
     * @return A PermissionStatus object, containing the permission, whether or not the player has it, and the
     *         permission argument if applicable.
     */
    public static PermissionStatus getPlayerPermissionStatus(GameProfile playerProfile, String permission)
    {
        synchronized(registry)
        { return registry.getUserPermissionStatus(playerProfile.getId(), permission); }
    }

    /**
     * Gets all of the information pertaining to a permission's relationship with the specified group, as a single
     * object.
     * @param groupName The name of the group to get the relationship information of the given permission of.
     * @param permission The permission to get the relationship information of for the specified group.
     * @return A PermissionStatus object, containing the permission, whether or not the group has it, and the permission
     *         argument if applicable.
     */
    public static PermissionStatus getGroupPermissionStatus(String groupName, String permission)
    {
        synchronized(registry)
        { return registry.getGroupPermissionStatus(groupName, permission); }
    }
    //endregion

    //region isInGroup

    /**
     * <p>Gets whether or not a player is in a group.</p>
     *
     * <p>If any groups assigned to the given player are, themselves, assigned to a group, the player will be considered
     * to be in that group as well.</p>
     * @param playerId The ID of the player.
     * @param groupId The name of the group.
     * @return True if the player is in the group, either directly or indirectly. Otherwise, false.
     * @see PermissionsRegistry#userHasGroup(Comparable, String)
     */
    public static boolean playerIsInGroup(UUID playerId, String groupId)
    {
        synchronized(registry)
        { return registry.userHasGroup(playerId, groupId); }
    }

    /**
     * <p>Gets whether or not a player is in a group.</p>
     *
     * <p>If any groups assigned to the given player are, themselves, assigned to a group, the player will be considered
     * to be in that group as well.</p>
     * @param player The player to check.
     * @param groupId The name of the group.
     * @return True if the player is in the group, either directly or indirectly. Otherwise, false.
     * @see PermissionsRegistry#userHasGroup(Comparable, String)
     */
    public static boolean playerIsInGroup(PlayerEntity player, String groupId)
    {
        synchronized(registry)
        { return registry.userHasGroup(player.getUniqueID(), groupId); }
    }

    /**
     * <p>Gets whether or not a player is in a group.</p>
     *
     * <p>If any groups assigned to the given player are, themselves, assigned to a group, the player will be considered
     * to be in that group as well.</p>
     * @param playerProfile The player to check.
     * @param groupId The name of the group.
     * @return True if the player is in the group, either directly or indirectly. Otherwise, false.
     * @see PermissionsRegistry#userHasGroup(Comparable, String)
     */
    public static boolean playerIsInGroup(GameProfile playerProfile, String groupId)
    {
        synchronized(registry)
        { return registry.userHasGroup(playerProfile.getId(), groupId); }
    }

    /**
     * <p>Gets whether or not a group is in another group.</p>
     *
     * <p>If any groups assigned to the given group have groups assigned to them themselves, the group being checked
     * will be considered to be in those groups as well.</p>
     * @param groupName The name of the group to check.
     * @param superGroupId The name of the group to check if the other group is in.
     * @return True if the first group is in the second group. Otherwise, false.
     * @see PermissionsRegistry#groupExtendsFromGroup(String, String)
     */
    public static boolean groupIsInGroup(String groupName, String superGroupId)
    {
        synchronized(registry)
        { return registry.groupExtendsFromGroup(groupName, superGroupId); }
    }
    //endregion

    //region getters

    /**
     * Gets a list of the names of all groups in the permissions registry.
     * @return A list of group names in alphabetical order.
     * @see PermissionsRegistry#getGroupNames()
     */
    public static List<String> getGroupNames()
    {
        List<String> result;

        synchronized(registry)
        { result = new ArrayList<>(registry.getGroupNames()); }

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets a list of the names of all groups in the permissions registry (prefixed with "#") and all the suggested
     * permissions to add.
     * @return A list of group names prefixed with "#" and suggested permissions.
     * @see PermissionsRegistry#getGroupNames()
     * @see Suggestions#get()
     */
    static List<String> getGroupNamesAndSuggestedPermissions()
    {
        List<String> result = new ArrayList<>();

        for(String groupName : getGroupNames())
            result.add("#" + groupName);

        result.addAll(Suggestions.get());
        return result;
    }

    /**
     * Gets a list of all the permissions assigned to a group. Does not include assigned groups.
     * @param groupName The name of the group to get the permissions of.
     * @return A list of all permissions assigned directly to the specified group. If the group has none or if the group
     *         does not currently exist in the permissions registry, returns an empty list.
     * @see PermissionsRegistry#getGroupPermissions(String)
     */
    public static List<String> getPermissionsOfGroup(String groupName)
    {
        synchronized(registry)
        { return registry.getGroupPermissions(groupName); }
    }

    /**
     * Gets a list of all the permissions assigned to a player. Does not include assigned groups.
     * @param playerId The ID of the player to get the permissions of.
     * @return A list of all permissions assigned directly to the specified player. If the player has none or if the
     *         player does not currently exist in the permissions registry, returns an empty list.
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getPermissionsOfPlayer(UUID playerId)
    {
        synchronized(registry)
        { return registry.getUserPermissions(playerId); }
    }

    /**
     * Gets a list of all the permissions assigned to a player. Does not include assigned groups.
     * @param player The player to get the permissions of.
     * @return A list of all permissions assigned directly to the specified player. If the player has none or if the
     *         player does not currently exist in the permissions registry, returns an empty list.
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getPermissionsOfPlayer(PlayerEntity player)
    {
        synchronized(registry)
        { return registry.getUserPermissions(player.getUniqueID()); }
    }

    /**
     * Gets a list of all the permissions assigned to a player. Does not include assigned groups.
     * @param playerProfile The player to get the permissions of.
     * @return A list of all permissions assigned directly to the specified player. If the player has none or if the
     *         player does not currently exist in the permissions registry, returns an empty list.
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getPermissionsOfPlayer(GameProfile playerProfile)
    {
        synchronized(registry)
        { return registry.getUserPermissions(playerProfile.getId()); }
    }

    /**
     * Gets a list of the names of all groups assigned to the given group. Does not include the groups assigned to those
     * groups.
     * @param groupName The name of the group to get the groups of.
     * @return A list of all groups assigned directly to the group of the given name.
     * @see PermissionsRegistry#getGroupsOfGroup(String)
     */
    public static List<String> getGroupsOfGroup(String groupName)
    {
        synchronized(registry)
        { return registry.getGroupsOfGroup(groupName); }
    }

    /**
     * Gets a list of the names of all groups assigned to the given player. Does not include the groups assigned to
     * those groups.
     * @param playerId The ID of the player to get groups of.
     * @return A list of all groups assigned directly to the specified player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     */
    public static List<String> getGroupsOfPlayer(UUID playerId)
    {
        synchronized(registry)
        { return registry.getGroupsOfUser(playerId); }
    }

    /**
     * Gets a list of the names of all groups assigned to the given player. Does not include the groups assigned to
     * those groups.
     * @param player The player to get groups of.
     * @return A list of all groups assigned directly to the player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     */
    public static List<String> getGroupsOfPlayer(PlayerEntity player)
    {
        synchronized(registry)
        { return registry.getGroupsOfUser(player.getUniqueID()); }
    }

    /**
     * Gets a list of the names of all groups assigned to the given player. Does not include the groups assigned to
     * those groups.
     * @param playerProfile The player to get groups of.
     * @return A list of all groups assigned directly to the player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     */
    public static List<String> getGroupsOfPlayer(GameProfile playerProfile)
    {
        synchronized(registry)
        { return registry.getGroupsOfUser(playerProfile.getId()); }
    }

    /**
     * Gets a list of the names of all groups assigned to the specified group, prefixed with "#", and all the
     * permissions assigned to the specified group. Does not include groups assigned to, or permissions of, groups
     * assigned to the specified group.
     * @param groupName The name of the group to get the group names and permissions of.
     * @return A list containing the names of all groups assigned directly to the specified group, prefixed with "#",
     *         and all permissions assigned directly to the specified group.
     * @see PermissionsRegistry#getGroupsOfGroup(String)
     * @see PermissionsRegistry#getGroupPermissions(String)
     */
    public static List<String> getGroupsAndPermissionsOfGroup(String groupName)
    {
        List<String> result = new ArrayList<>();
        List<String> inheritedGroups;
        List<String> perms;

        synchronized(registry)
        {
            inheritedGroups = new ArrayList<>(registry.getGroupsOfGroup(groupName));
            perms = registry.getGroupPermissions(groupName);
        }

        inheritedGroups.sort(Comparator.naturalOrder());

        for(String inheritedGroupName : inheritedGroups)
            result.add("#" + inheritedGroupName);

        result.addAll(perms);
        return result;
    }

    /**
     * Gets a list of the names of all groups assigned to the specified player, prefixed with "#", and all the
     * permissions assigned to the specified player. Does not include groups assigned to, or permissions of, groups
     * assigned to the player.
     * @param playerId The ID of the player to get the group names and permissions of.
     * @return A list containing the names of all groups assigned directly to the specified player, prefixed with "#",
     *         and all permissions assigned directly to the specified player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getGroupsAndPermissionsOfPlayer(UUID playerId)
    {
        List<String> result = new ArrayList<>();
        List<String> inheritedGroups;
        List<String> perms;

        synchronized(registry)
        {
            inheritedGroups = new ArrayList<>(registry.getGroupsOfUser(playerId));
            perms = registry.getUserPermissions(playerId);
        }

        inheritedGroups.sort(Comparator.naturalOrder());

        for(String groupName : inheritedGroups)
            result.add("#" + groupName);

        result.addAll(perms);
        return result;
    }

    /**
     * Gets a list of the names of all groups assigned to the given player, prefixed with "#", and all the permissions
     * assigned to the specified player. Does not include groups assigned to, or permissions of, groups assigned to the
     * player.
     * @param player The player to get the group names and permissions of.
     * @return A list containing the names of all groups assigned directly to the given player, prefixed with "#", and
     *         all permissions assigned directly to the given player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getGroupsAndPermissionsOfPlayer(PlayerEntity player)
    { return getGroupsAndPermissionsOfPlayer(player.getUniqueID()); }

    /**
     * Gets a list of the names of all groups assigned to the given player, prefixed with "#", and all the permissions
     * assigned to the specified player. Does not include groups assigned to, or permissions of, groups assigned to the
     * player.
     * @param playerProfile The player to get the group names and permissions of.
     * @return A list containing the names of all groups assigned directly to the given player, prefixed with "#", and
     *         all permissions assigned directly to the given player.
     * @see PermissionsRegistry#getGroupsOfUser(Comparable)
     * @see PermissionsRegistry#getUserPermissions(Comparable)
     */
    public static List<String> getGroupsAndPermissionsOfPlayer(GameProfile playerProfile)
    { return getGroupsAndPermissionsOfPlayer(playerProfile.getId()); }
    //endregion
    //endregion

    //region saving & loading

    /**
     * Saves the contents of the permissions registry to the permissions files. Does not save anything if no changes
     * have been made.
     * @see PermissionsRegistry#save()
     */
    public static void save()
    {
        synchronized(registry)
        {
            if(!registry.hasBeenDifferentiatedFromFiles())
                return;

            try
            { registry.save(); }
            catch(IOException e)
            { throw new RuntimeException("Error saving permissions files.", e); }
        }
    }

    /**
     * Loads the contents of the permissions registry from the permissions files.
     * @see PermissionsRegistry#load()
     */
    public static void load()
    {
        try
        {
            synchronized(registry)
            { registry.load(); }
        }
        catch(IOException e)
        { throw new RuntimeException("Error loading permissions files.", e); }
    }
    //endregion
    //endregion
}
