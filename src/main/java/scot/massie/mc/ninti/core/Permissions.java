package scot.massie.mc.ninti.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.events.Event;
import scot.massie.lib.events.SetEvent;
import scot.massie.lib.events.args.EventArgs;
import scot.massie.lib.permissions.PermissionsRegistry;

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
import java.util.stream.Collectors;

public final class Permissions
{
    //region Inner classes
    //region Event args
    public static final class PermissionsAreBeingSuggestedEventArgs implements EventArgs
    {
        PermissionsAreBeingSuggestedEventArgs(Set<String> suggestions)
        { this.suggestedPermissions = suggestions; }

        Set<String> suggestedPermissions;
        boolean isUnmodified = true;

        public void suggestPermission(String permission)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            suggestedPermissions.add(permission);
        }

        public void suggestPermissions(List<String> permissions)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            suggestedPermissions.addAll(permissions);
        }

        public void suggestPermissions(String... permissions)
        {
            if(isUnmodified)
            {
                suggestedPermissions = new HashSet<>(suggestedPermissions);
                isUnmodified = false;
            }

            Collections.addAll(suggestedPermissions, permissions);
        }

        public Set<String> getSuggestedPermissions()
        { return Collections.unmodifiableSet(suggestedPermissions); }
    }
    //endregion

    //region Inner static function stores
    public static final class Presets
    {
        private Presets()
        {}

        private static final Map<String, Set<String>> presets = new HashMap<>();

        public static List<String> getPresetNames()
        {
            Collection<String> presetNames;

            synchronized(presets)
            { presetNames = presets.keySet(); }

            List<String> result = new ArrayList<>(presetNames);
            result.sort(Comparator.naturalOrder());
            return result;
        }

        public static Collection<String> getPresetPermissions(String presetName)
        {
            Set<String> preset;

            synchronized(presets)
            { preset = presets.get(presetName); }

            return preset != null ? Collections.unmodifiableCollection(preset) : Collections.emptySet();
        }

        public static Map<String, Set<String>> getPresets()
        {
            synchronized(presets)
            { return Collections.unmodifiableMap(new HashMap<>(presets)); }
        }

        public static void addPermission(String presetName, String permission)
        {
            synchronized(presets)
            { presets.computeIfAbsent(presetName, s -> new HashSet<>()).add(permission); }
        }

        public static void addPermissions(String presetName, String... permissions)
        {
            synchronized(presets)
            { Collections.addAll(presets.computeIfAbsent(presetName, s -> new HashSet<>()), permissions); }
        }

        public static void addPermissions(String presetName, Collection<String> permissions)
        {
            synchronized(presets)
            { presets.computeIfAbsent(presetName, s -> new HashSet<>()).addAll(permissions); }
        }

        public static void assignToPlayer(String presetName, UUID playerId)
        { Permissions.assignPlayerPreset(playerId, presetName); }

        public static void assignToPlayer(String presetName, PlayerEntity player)
        { assignToPlayer(presetName, player.getUniqueID()); }

        public static void assignToGroup(String presetName, String groupName)
        { Permissions.assignGroupPreset(groupName, presetName); }
    }

    public static final class Suggestions
    {
        private Suggestions()
        {}

        private static final Set<String> permissionsToBeSuggested = new HashSet<>();
        static
        {

        }

        public static final Event<PermissionsAreBeingSuggestedEventArgs> beingSuggested = new SetEvent<>();

        // not public for now, until I decide whether to pass arguments to pass into the event args.
        static List<String> get()
        {
            PermissionsAreBeingSuggestedEventArgs eventArgs;

            synchronized(permissionsToBeSuggested)
            { eventArgs = new PermissionsAreBeingSuggestedEventArgs(permissionsToBeSuggested); }

            beingSuggested.invoke(eventArgs);

            List<String> suggestions = new ArrayList<>(eventArgs.getSuggestedPermissions());
            suggestions.sort(Comparator.naturalOrder());
            return suggestions;
        }

        public static void add(String permission)
        {
            synchronized(permissionsToBeSuggested)
            { permissionsToBeSuggested.add(permission); }
        }

        public static void add(String... permissions)
        {
            synchronized(permissionsToBeSuggested)
            { Collections.addAll(permissionsToBeSuggested, permissions); }
        }

        public static void add(Collection<String> permissions)
        {
            synchronized(permissionsToBeSuggested)
            { permissionsToBeSuggested.addAll(permissions); }
        }
    }
    //endregion
    //endregion

    private Permissions()
    {}

    //region fields

    /**
     * This is defined in the UUID contract. See {@link UUID#toString}.
     */
    static final int uuidStringLength = 36;

    private static final PermissionsRegistry<UUID> registry = new PermissionsRegistry<>(
            uuid -> (UsernameCache.containsUUID(uuid))
                            ? (uuid.toString() + " - " + UsernameCache.getLastKnownUsername(uuid))
                            : (uuid.toString()),
            name -> UUID.fromString(name.substring(0, uuidStringLength)),
            Paths.get("permissions.txt"),
            Paths.get("permission_groups.txt"));
    //endregion

    //region methods
    //region accessors
    //region hasPermissions
    public static boolean playerHasPermission(UUID playerId, String permission)
    {
        if(permission.startsWith("#"))
            return playerIsInGroup(playerId, permission.substring(1));

        synchronized(registry)
        { return registry.userHasPermission(playerId, permission); }
    }

    public static boolean playerHasPermission(PlayerEntity player, String permission)
    { return playerHasPermission(player.getUniqueID(), permission); }

    public static boolean groupHasPermission(String groupId, String permission)
    {
        if(permission.startsWith("#"))
            return groupIsInGroup(groupId, permission.substring(1));

        synchronized(registry)
        { return registry.groupHasPermission(groupId, permission); }
    }
    //endregion
    //region isInGroup
    public static boolean playerIsInGroup(UUID playerId, String groupId)
    {
        synchronized(registry)
        { return registry.userHasGroup(playerId, groupId); }
    }

    public static boolean playerIsInGroup(PlayerEntity player, String groupId)
    {
        synchronized(registry)
        { return registry.userHasGroup(player.getUniqueID(), groupId); }
    }

    public static boolean groupIsInGroup(String groupId, String superGroupId)
    {
        synchronized(registry)
        { return registry.groupExtendsFromGroup(groupId, superGroupId); }
    }
    //endregion
    //region getters
    public static List<String> getGroupNames()
    {
        List<String> result;

        synchronized(registry)
        { result = new ArrayList<>(registry.getGroupNames()); }

        result.sort(Comparator.naturalOrder());
        return result;
    }

    static List<String> getGroupNamesAndSuggestedPermissions()
    {
        List<String> result = new ArrayList<>();

        for(String groupName : getGroupNames())
            result.add("#" + groupName);

        result.addAll(Suggestions.get());
        return result;
    }

    public static List<String> getPermissionsOfGroup(String groupId)
    {
        synchronized(registry)
        { return registry.getGroupPermissions(groupId); }
    }

    public static List<String> getPermissionsOfPlayer(UUID playerId)
    {
        synchronized(registry)
        { return registry.getUserPermissions(playerId); }
    }

    public static List<String> getPermissionsOfPlayer(PlayerEntity player)
    {
        synchronized(registry)
        { return registry.getUserPermissions(player.getUniqueID()); }
    }

    public static List<String> getGroupsOfGroup(String groupId)
    {
        synchronized(registry)
        { return registry.getGroupsOfGroup(groupId); }
    }

    public static List<String> getGroupsOfPlayer(UUID playerId)
    {
        synchronized(registry)
        { return registry.getGroupsOfUser(playerId); }
    }

    public static List<String> getGroupsOfPlayer(PlayerEntity player)
    {
        synchronized(registry)
        { return registry.getGroupsOfUser(player.getUniqueID()); }
    }

    public static List<String> getGroupsAndPermissionsOfGroup(String groupId)
    {
        List<String> result = new ArrayList<>();
        List<String> inheritedGroups;
        List<String> perms;

        synchronized(registry)
        {
            inheritedGroups = new ArrayList<>(registry.getGroupsOfGroup(groupId));
            perms = registry.getGroupPermissions(groupId);
        }

        inheritedGroups.sort(Comparator.naturalOrder());

        for(String groupName : inheritedGroups)
            result.add("#" + groupName);

        result.addAll(perms);
        return result;
    }

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

    public static List<String> getGroupsAndPermissionsOfPlayer(PlayerEntity player)
    { return getGroupsAndPermissionsOfPlayer(player.getUniqueID()); }
    //endregion
    //endregion
    //region mutators
    //region assign permissions/groups
    public static void assignPlayerPermission(PlayerEntity player, String permission)
    { assignPlayerPermission(player.getUniqueID(), permission); }

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

    public static void assignPlayerGroup(PlayerEntity player, String groupId)
    {
        synchronized(registry)
        { registry.assignGroupToUser(player.getUniqueID(), groupId); }
    }

    public static void assignPlayerGroup(UUID playerId, String groupId)
    {
        synchronized(registry)
        { registry.assignGroupToUser(playerId, groupId); }
    }

    public static void assignPermissionGroupGroup(String groupIdBeingAssignedTo, String groupIdBeingAssigned)
    {
        synchronized(registry)
        { registry.assignGroupToGroup(groupIdBeingAssignedTo, groupIdBeingAssigned); }
    }

    public static void assignPlayerPreset(UUID playerId, String presetName)
    {
        Collection<String> perms = Presets.getPresetPermissions(presetName);

        synchronized(registry)
        {
            for(String perm : perms)
                registry.assignUserPermission(playerId, perm);
        }
    }

    public static void assignPlayerPreset(PlayerEntity player, String presetName)
    { assignPlayerPreset(player.getUniqueID(), presetName); }

    public static void assignGroupPreset(String groupId, String presetName)
    {
        Collection<String> perms = Presets.getPresetPermissions(presetName);

        synchronized(registry)
        {
            for(String perm : perms)
                registry.assignGroupPermission(groupId, presetName);
        }
    }
    //endregion
    //region revoke permissions/groups
    public static void revokePlayerPermission(PlayerEntity player, String permission)
    { revokePlayerPermission(player.getUniqueID(), permission); }

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

    public static void removePlayerFromGroup(PlayerEntity player, String groupId)
    {
        synchronized(registry)
        { registry.revokeGroupFromUser(player.getUniqueID(), groupId); }
    }

    public static void removePlayerFromGroup(UUID playerId, String groupId)
    {
        synchronized(registry)
        { registry.revokeGroupFromUser(playerId, groupId); }
    }

    public static void removeGroupFromGroup(String groupIdBeingDeassigned, String groupIdBeingRemovedFrom)
    {
        synchronized(registry)
        { registry.revokeGroupFromGroup(groupIdBeingDeassigned, groupIdBeingRemovedFrom); }
    }
    //endregion
    //region clear
    static void clear()
    {
        synchronized(registry)
        { registry.clear(); }
    }
    //endregion
    //region saving/loading/initialising
    public static void savePermissions()
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

    public static void loadPermissions()
    {
        try
        {
            synchronized(registry)
            { registry.load(); }
        }
        catch(IOException e)
        { throw new RuntimeException("Error loading permissions files.", e); }
    }

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
    //endregion
    //endregion
}
