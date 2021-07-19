package scot.massie.mc.ninti.core.exceptions;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;
import scot.massie.lib.permissions.exceptions.UserMissingPermissionException;
import scot.massie.mc.ninti.core.PluginUtils;

import java.util.UUID;

/**
 * Thrown when a player needs a permission they do not have to do something.
 */
public class PlayerMissingPermissionException extends UserMissingPermissionException
{
    /**
     * Creates a new MissingPermissionException.
     * @param playerId The ID of the player missing the permission.
     * @param permission The permission the player is missing.
     */
    public PlayerMissingPermissionException(UUID playerId, String permission)
    {
        super(playerId, "The player " + getHowToReferToPlayer(playerId) + " does not have the required permission: "
                        + permission);
    }

    /**
     * Creates a new MissingPermissionException.
     * @param player The player missing the permission.
     * @param permission The permission the player is missing.
     */
    public PlayerMissingPermissionException(PlayerEntity player, String permission)
    { this(player.getUniqueID(), permission); }

    /**
     * Creates a new MissingPermissionException.
     * @param player The game profile of the player missing the permission.
     * @param permission The permission the player is missing.
     */
    public PlayerMissingPermissionException(GameProfile player, String permission)
    { this(player.getId(), permission); }

    private static String getHowToReferToPlayer(UUID playerId)
    {
        String name = UsernameCache.getLastKnownUsername(playerId);
        return name != null ? name : "with the ID " + playerId;
    }

    /**
     * Gets the ID of the player missing the permission.
     * @return The player ID.
     */
    public UUID getPlayerId()
    { return (UUID)this.getUserId(); }

    /**
     * Gets the player missing the permission.
     * @return The player, or null if the represented player is not online.
     */
    public PlayerEntity getPlayer()
    { return PluginUtils.getOnlinePlayer(getPlayerId()); }
}
