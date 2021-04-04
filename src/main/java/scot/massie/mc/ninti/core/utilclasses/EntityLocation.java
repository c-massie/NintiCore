package scot.massie.mc.ninti.core.utilclasses;

import net.minecraft.entity.Entity;
import scot.massie.mc.ninti.core.PluginUtils;

/**
 * Single class representing an entity's location on the server.
 */
public final class EntityLocation
{
    /**
     * Creates a new EntityLocation instance.
     * @param worldId The world ID.
     * @param x The entity's X coördinate.
     * @param y The entity's Y coördinate.
     * @param z The entity's Z coördinate.
     * @param pitch The entity's pitch. This goes from -90 to 90, where -90 is straight up, 90 is straight down, and 0
     *              is level.
     * @param yaw The entity's yaw. This goes from 0-360, and represents how far left or right the entity is facing.
     */
    public EntityLocation(String worldId, double x, double y, double z, double pitch, double yaw)
    {
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    /**
     * Creates a new EntityLocation instance from the location of the given entity.
     * @param player The entity to get the location of.
     */
    public EntityLocation(Entity player)
    {
        this(PluginUtils.getWorldId(player.getEntityWorld()),
             player.getPosX(),
             player.getPosY(),
             player.getPosZ(),
             player.getPitchYaw().x,
             player.getPitchYaw().y);
    }

    private final String worldId;
    private final double x, y, z, pitch, yaw;

    /**
     * Gets the ID of the world the location is in.
     * @return The ID of the world the location is in. e.g. "minecraft:overworld"
     */
    public String getWorldId()
    { return worldId; }

    /**
     * Gets the X coördinate of the location.
     * @return The X coördinate of the location.
     */
    public double getX()
    { return x; }

    /**
     * Gets the Y coördinate of the location. This is the vertical coördinate.
     * @return The Y coördinate of the location.
     */
    public double getY()
    { return y; }

    /**
     * Gets the Z coördinate of the location.
     * @return The Z coördinate of the location.
     */
    public double getZ()
    { return z; }

    /**
     * Gets the pitch of the location. This is how far up or down the location is facing. This goes from -90 (looking
     * straight up) to 90, (looking straight down) where 0 is level.
     * @return The pitch of the location, from -90 to 90.
     */
    public double getPitch()
    { return pitch; }

    /**
     * Gets the yaw of the location. This is the location's horizontal rotation, i.e. the rotation the location is
     * facing. This goes from 0 to 360.
     * @return The yaw of the location, from 0 to 360.
     */
    public double getYaw()
    { return yaw; }
}
