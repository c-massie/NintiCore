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
import java.util.UUID;
import java.util.function.Function;

public final class Permissions
{
    private Permissions()
    {}

    private static final PermissionsRegistry<UUID> registry = new PermissionsRegistry<>(
            UUID::toString, UUID::fromString, Paths.get("permissions.txt"), Paths.get("permission_groups.txt"));

    public static class PermissionsEventArgs implements EventArgs
    { }

    public static boolean playerHasPermission(PlayerEntity player, String permission)
    { return registry.userHasPermission(player.getUniqueID(), permission); }

    public static boolean playerHasPermission(UUID playerId, String permission)
    { return registry.userHasPermission(playerId, permission); }

    public static void assignPlayerPermission(PlayerEntity player, String permission)
    { registry.assignUserPermission(player.getUniqueID(), permission); }

    public static void assignPlayerPermission(UUID playerId, String permission)
    { registry.assignUserPermission(playerId, permission); }

    public static void assignGroupPermission(String groupId, String permission)
    { registry.assignGroupPermission(groupId, permission); }

    public static void assignPlayerGroup(PlayerEntity player, String groupId)
    { registry.assignGroupToUser(player.getUniqueID(), groupId); }

    public static void assignPlayerGroup(UUID playerId, String groupId)
    { registry.assignGroupToUser(playerId, groupId); }

    public static void assignPermissionGroupGroup(String groupIdBeingAssignedTo, String groupIdBeingAssigned)
    { registry.assignGroupToGroup(groupIdBeingAssignedTo, groupIdBeingAssigned); }

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
