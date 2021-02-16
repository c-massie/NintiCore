package scot.massie.mc.ninti.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModLoader;
import scot.massie.lib.events.Event;
import scot.massie.lib.events.SetEvent;
import scot.massie.lib.events.args.EventArgs;
import scot.massie.lib.permissions.PermissionsRegistry;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Permissions
{
    private Permissions()
    {}

    private static final PermissionsRegistry<UUID> registry = new PermissionsRegistry<>(
            UUID::toString, UUID::fromString, Paths.get("permissions.txt"), Paths.get("permission_groups.txt"));

    public static boolean playerHasPermission(PlayerEntity player, String permission)
    { return registry.userHasPermission(player.getUniqueID(), permission); }

    public static boolean playerHasPermission(UUID playerId, String permission)
    { return registry.userHasPermission(playerId, permission); }

    public static boolean groupHasPermission(String groupId, String permission)
    { return registry.groupHasPermission(groupId, permission); }

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
