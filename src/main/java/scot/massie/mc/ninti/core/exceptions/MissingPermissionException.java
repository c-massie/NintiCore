package scot.massie.mc.ninti.core.exceptions;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.mc.ninti.core.PluginUtils;

import java.util.UUID;

/**
 * Thrown when a player needs a permission they do not have to do something.
 */
public class MissingPermissionException extends Exception
{
    /**
     * Creates a new MissingPermissionException.
     * @param playerId The ID of the player missing the permission.
     * @param permission The permission the player is missing.
     */
    public MissingPermissionException(UUID playerId, String permission)
    {
        super("The player " + getHowToReferToPlayer(playerId) + " does not have the required permission: "
              + permission);

        this.playerMissingPermission = playerId;
        this.permissionMissing = permission;
    }

    /**
     * Creates a new MissingPermissionException.
     * @param player The player missing the permission.
     * @param permission The permission the player is missing.
     */
    public MissingPermissionException(PlayerEntity player, String permission)
    {
        super("The player " + player.getGameProfile().getName() + " does not have the required permission: "
              + permission);

        this.playerMissingPermission = player.getUniqueID();
        this.permissionMissing = permission;
    }

    /**
     * Creates a new MissingPermissionException.
     * @param player The game profile of the player missing the permission.
     * @param permission The permission the player is missing.
     */
    public MissingPermissionException(GameProfile player, String permission)
    {
        super("The player " + player.getName() + " does not have the required permission: "
              + permission);

        this.playerMissingPermission = player.getId();
        this.permissionMissing = permission;
    }

    private static String getHowToReferToPlayer(UUID playerId)
    {
        String name = UsernameCache.getLastKnownUsername(playerId);
        return name != null ? name : "with the ID " + playerId;
    }

    protected final UUID playerMissingPermission;
    protected final String permissionMissing;

    /**
     * Gets the ID of the player missing the permission.
     * @return The player ID.
     */
    public UUID getPlayerId()
    { return playerMissingPermission; }

    /**
     * Gets the player missing the permission.
     * @return The player, or null if the represented player is not online.
     */
    public PlayerEntity getPlayer()
    { return PluginUtils.getOnlinePlayer(playerMissingPermission); }

    /**
     * Gets the permission the player is missing but requires.
     * @return The concerned permission.
     */
    public String getPermission()
    { return permissionMissing; }
}
