package scot.massie.mc.ninti.core.exceptions;

import net.minecraftforge.common.UsernameCache;

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
    public UUID getPlayer()
    { return playerMissingPermission; }

    /**
     * Gets the permission the player is missing but requires.
     * @return The concerned permission.
     */
    public String getPermission()
    { return permissionMissing; }
}
