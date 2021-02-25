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

    public static final class Presets
    {
        private Presets()
        {}

        Map<String, Set<String>> presets = new HashMap<>();

        public List<String> getPresetNames()
        {
            List<String> presetNames = new ArrayList<>(presets.keySet());
            presetNames.sort(Comparator.naturalOrder());
            return presetNames;
        }

        public Collection<String> getPresetPermissions(String presetName)
        {
            Set<String> preset = presets.getOrDefault(presetName, null);
            return preset != null ? Collections.unmodifiableCollection(preset) : Collections.emptySet();
        }

        public void addPermission(String presetName, String permission)
        { presets.computeIfAbsent(presetName, s -> new HashSet<>()).add(permission); }

        public void addPermissions(String presetName, String... permissions)
        { Collections.addAll(presets.computeIfAbsent(presetName, s -> new HashSet<>()), permissions); }

        public void addPermissions(String presetName, Collection<String> permissions)
        { presets.computeIfAbsent(presetName, s -> new HashSet<>()).addAll(permissions); }

        public void assignToPlayer(String presetName, UUID playerId)
        {
            Set<String> presetPerms = presets.get(presetName);

            for(String perm : presetPerms)
                assignPlayerPermission(playerId, perm);
        }

        public void assignToPlayer(String presetName, PlayerEntity player)
        { assignToPlayer(presetName, player.getUniqueID()); }

        public void assignToGroup(String presetName, String groupName)
        {
            Set<String> presetPerms = presets.get(presetName);

            for(String perm : presetPerms)
                assignGroupPermission(groupName, perm);
        }
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
            PermissionsAreBeingSuggestedEventArgs eventArgs
                    = new PermissionsAreBeingSuggestedEventArgs(permissionsToBeSuggested);

            beingSuggested.invoke(eventArgs);

            List<String> suggestions = new ArrayList<>(eventArgs.getSuggestedPermissions());
            suggestions.sort(Comparator.naturalOrder());
            return suggestions;
        }

        public static void add(String permission)
        { permissionsToBeSuggested.add(permission); }

        public static void add(String... permissions)
        { Collections.addAll(permissionsToBeSuggested, permissions); }

        public static void add(Collection<String> permissions)
        { permissionsToBeSuggested.addAll(permissions); }
    }

    private Permissions()
    {}

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

    static List<String> getGroupsAndSuggestedPermissions()
    {
        List<String> result = new ArrayList<>();

        for(String groupName : registry.getGroupNames())
            result.add("#" + groupName);

        result.addAll(Suggestions.get());
        return result;
    }

    public static boolean playerHasPermission(PlayerEntity player, String permission)
    { return playerHasPermission(player.getUniqueID(), permission); }

    public static boolean playerHasPermission(UUID playerId, String permission)
    {
        if(permission.startsWith("#"))
            return playerIsInGroup(playerId, permission.substring(1));

        return registry.userHasPermission(playerId, permission);
    }

    public static boolean groupHasPermission(String groupId, String permission)
    {
        if(permission.startsWith("#"))
            return groupIsInGroup(groupId, permission.substring(1));

        return registry.groupHasPermission(groupId, permission);
    }

    public static boolean playerIsInGroup(PlayerEntity player, String groupId)
    { return registry.userHasGroup(player.getUniqueID(), groupId); }

    public static boolean playerIsInGroup(UUID playerId, String groupId)
    { return registry.userHasGroup(playerId, groupId); }

    public static boolean groupIsInGroup(String groupId, String superGroupId)
    { return registry.groupExtendsFromGroup(groupId, superGroupId); }

    public static void assignPlayerPermission(PlayerEntity player, String permission)
    { assignPlayerPermission(player.getUniqueID(), permission); }

    public static void assignPlayerPermission(UUID playerId, String permission)
    {
        if(permission.startsWith("#"))
        {
            assignPlayerGroup(playerId, permission.substring(1));
            return;
        }

        registry.assignUserPermission(playerId, permission);
    }

    public static void assignGroupPermission(String groupId, String permission)
    {
        if(permission.startsWith("#"))
        {
            assignPermissionGroupGroup(groupId, permission.substring(1));
            return;
        }

        registry.assignGroupPermission(groupId, permission);
    }

    public static void assignPlayerGroup(PlayerEntity player, String groupId)
    { registry.assignGroupToUser(player.getUniqueID(), groupId); }

    public static void assignPlayerGroup(UUID playerId, String groupId)
    { registry.assignGroupToUser(playerId, groupId); }

    public static void assignPermissionGroupGroup(String groupIdBeingAssignedTo, String groupIdBeingAssigned)
    { registry.assignGroupToGroup(groupIdBeingAssignedTo, groupIdBeingAssigned); }

    public static void revokePlayerPermission(PlayerEntity player, String permission)
    { revokePlayerPermission(player.getUniqueID(), permission); }

    public static void revokePlayerPermission(UUID playerId, String permission)
    {
        if(permission.startsWith("#"))
        {
            removePlayerFromGroup(playerId, permission.substring(1));
            return;
        }

        registry.revokeUserPermission(playerId, permission);
    }

    public static void revokeGroupPermission(String groupId, String permission)
    {
        if(permission.startsWith("#"))
        {
            removeGroupFromGroup(groupId, permission.substring(1));
            return;
        }

        registry.revokeGroupPermission(groupId, permission);
    }

    public static void removePlayerFromGroup(PlayerEntity player, String groupId)
    { registry.revokeGroupFromUser(player.getUniqueID(), groupId); }

    public static void removePlayerFromGroup(UUID playerId, String groupId)
    { registry.revokeGroupFromUser(playerId, groupId); }

    public static void removeGroupFromGroup(String groupIdBeingDeassigned, String groupIdBeingRemovedFrom)
    { registry.revokeGroupFromGroup(groupIdBeingDeassigned, groupIdBeingRemovedFrom); }

    public static List<String> getGroupNames()
    { return registry.getGroupNames().stream().sorted().collect(Collectors.toList()); }

    public static List<String> getPermissionsOfGroup(String groupId)
    { return registry.getGroupPermissions(groupId); }

    public static List<String> getPermissionsOfPlayer(UUID playerId)
    { return registry.getUserPermissions(playerId); }

    public static List<String> getPermissionsOfPlayer(PlayerEntity player)
    { return registry.getUserPermissions(player.getUniqueID()); }

    public static List<String> getGroupsOfGroup(String groupId)
    { return registry.getGroupsOfGroup(groupId); }

    public static List<String> getGroupsOfPlayer(UUID playerId)
    { return registry.getGroupsOfUser(playerId); }

    public static List<String> getGroupsOfPlayer(PlayerEntity player)
    { return registry.getGroupsOfUser(player.getUniqueID()); }

    public static List<String> getGroupsAndPermissionsOfGroup(String groupId)
    {
        List<String> result = new ArrayList<>();

        for(String groupName : registry.getGroupsOfGroup(groupId))
            result.add("#" + groupName);

        result.addAll(registry.getGroupPermissions(groupId));
        return result;
    }

    public static List<String> getGroupsAndPermissionsOfPlayer(UUID playerId)
    {
        List<String> result = new ArrayList<>();

        for(String groupName : registry.getGroupsOfUser(playerId))
            result.add("#" + groupName);

        result.addAll(registry.getUserPermissions(playerId));
        return result;
    }

    public static List<String> getGroupsAndPermissionsOfPlayer(PlayerEntity player)
    { return getGroupsAndPermissionsOfPlayer(player.getUniqueID()); }

    public static void savePermissions()
    {
        if(!registry.hasBeenDifferentiatedFromFiles())
            return;

        try
        { registry.save(); }
        catch(IOException e)
        { throw new RuntimeException("Error saving permissions files.", e); }
    }

    public static void loadPermissions()
    {
        try
        { registry.load(); }
        catch(IOException e)
        { throw new RuntimeException("Error loading permissions files.", e); }
    }
}
