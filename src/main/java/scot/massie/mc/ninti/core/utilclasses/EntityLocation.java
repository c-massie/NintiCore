package scot.massie.mc.ninti.core.utilclasses;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.exceptions.NoSuchWorldException;

import java.util.Objects;

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
     * @throws NullPointerException if the world ID given is null.
     */
    public EntityLocation(String worldId, double x, double y, double z, double pitch, double yaw)
    {
        if(worldId == null)
            throw new NullPointerException("worldId cannot be null.");

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

    /**
     * Creates a new EntityLocation from a string representation of it. Should always accept the output from
     * EntityLocation's {@link #toString()} method.
     * @param asString A string representation of an entity location.
     * @throws NullPointerException if asString is null.
     * @throws IllegalArgumentException if asString cannot be parsed into an EntityLocation instance.
     */
    public EntityLocation(String asString)
    {
        if(asString == null)
            throw new NullPointerException();

        String[] split = asString.split(", ");

        if(split.length < 6)
            throw new IllegalArgumentException("String passed was not parsable as an entity location.");

        worldId = split[0];

        try
        {
            x       = Double.parseDouble(split[1]);
            y       = Double.parseDouble(split[2]);
            z       = Double.parseDouble(split[3]);
            pitch   = Double.parseDouble(split[4]);
            yaw     = Double.parseDouble(split[5]);
        }
        catch(NumberFormatException e)
        { throw new IllegalArgumentException("String passed was not parsable as an entity location."); }
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

    /**
     * Gets the distance between this location and another.
     * @param other The location to get the distance of from this to it.
     * @return The distance between this location and the one passed.
     */
    public double getDistanceTo(EntityLocation other)
    { return Math.sqrt((x * x - other.x * other.x) + (y * y - other.y * other.y) + (z * z - other.z * other.z)); }

    /**
     * Gets the distance squared between this location and another. If you just need to compare distances, this may be
     * more performant than {@link #getDistanceTo(EntityLocation)}, as this will not call Math.sqrt.
     * @param other The location to get the distance squared of from this to it.
     * @return The distance squared between this location and the one passed.
     */
    public double getDistanceSqTo(EntityLocation other)
    { return (x * x - other.x * other.x) + (y * y - other.y * other.y) + (z * z - other.z * other.z); }

    /**
     * Teleports a given player to the location on the server represented by this EntityLocation.
     * @param player The player to teleport.
     * @throws NoSuchWorldException If there is no world by the ID stored in this EntityLocation.
     */
    public void tpPlayerToHere(ServerPlayerEntity player) throws NoSuchWorldException
    {
        ServerWorld world = PluginUtils.getWorldById(worldId);

        if(world == null)
            throw new NoSuchWorldException(worldId);

        player.teleport(world, x, y, z, (float)yaw, (float)pitch);
    }

    @Override
    public String toString()
    { return worldId + ", " + x + ", " + y + ", " + z + ", " + pitch + ", " + yaw; }

    @Override
    public boolean equals(Object o)
    {
        if(this == o)
            return true;

        if(o == null || getClass() != o.getClass())
            return false;

        EntityLocation other = (EntityLocation)o;

        return Double.compare(other.x, x) == 0
            && Double.compare(other.y, y) == 0
            && Double.compare(other.z, z) == 0
            && Double.compare(other.pitch, pitch) == 0
            && Double.compare(other.yaw, yaw) == 0
            && worldId.equals(other.worldId);
    }

    @Override
    public int hashCode()
    { return Objects.hash(worldId, x, y, z, pitch, yaw); }
}
