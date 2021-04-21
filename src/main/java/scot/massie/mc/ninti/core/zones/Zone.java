package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;
import scot.massie.mc.ninti.core.PluginUtils;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A representation of a 3D area in a specific world, made by layering regions.
 */
public final class Zone
{
    /**
     * A simple representation of a 3D area.
     */
    public interface ZoneRegion
    {
        /**
         * Gets the minimum X value in this region.
         * @return The minimum X value in this region.
         */
        int getMinX();

        /**
         * Gets the minimum Y value in this region.
         * @return The minimum Y value in this region.
         */
        int getMinY();

        /**
         * Gets the minimum Z value in this region.
         * @return The minimum Z value in this region.
         */
        int getMinZ();

        /**
         * Gets the maximum X value in this region.
         * @return The maximum X value in this region.
         */
        int getMaxX();

        /**
         * Gets the maximum Y value in this region.
         * @return The maximum Y value in this region.
         */
        int getMaxY();

        /**
         * Gets the maximum Z value in this region.
         * @return The maximum Z value in this region.
         */
        int getMaxZ();

        /**
         * Gets whether or not this region cuts away the area it represents from the zone, rather than add to it.
         * @return True if this region cuts away the area it represents. Otherwise, false.
         */
        boolean isNegating();

        /**
         * Gets whether or not this region contains a given XZ coördinate.
         * @param x The X coördinate.
         * @param z The Z coördinate.
         * @return True if this region contains the given XZ coördinate. Otherwise, false.
         */
        boolean contains(int x, int z);

        /**
         * Gets whether or not this region contains a given XYZ coördinate.
         * @param x The X coördinate.
         * @param y The Y coördinate.
         * @param z The Z coördinate.
         * @return True if this region contains the given XYZ coördinate. Otherwise, false.
         */
        boolean contains(int x, int y, int z);

        /**
         * Gets whether or not this region contains a given XZ coördinate.
         * @param x The X coördinate.
         * @param z The Z coördinate.
         * @return True if this region contains the given XZ coördinate. Otherwise, false.
         */
        boolean contains(double x, double z);

        /**
         * Gets whether or not this region contains a given XYZ coördinate.
         * @param x The X coördinate.
         * @param y The Y coördinate.
         * @param z The Z coördinate.
         * @return True if this region contains the given XYZ coördinate. Otherwise, false.
         */
        boolean contains(double x, double y, double z);

        /**
         * Gets whether or not a given entity is within the area represented by this region.
         * @param entity The entity to check.
         * @return True if the given entity's location is within this region. Otherwise, false. Does not consider the
         *         entity's world, only its position.
         */
        boolean contains(Entity entity);

        /**
         * Gets a copy of this region, but cutting away from the zone it's in rather than adding to it.
         * @return A new ZoneRegion object that returns true for {@link #isNegating()}.
         */
        ZoneRegion negating();
    }

    /**
     * A region representing a cuboid area.
     */
    public static class ZoneRegionCuboid implements ZoneRegion
    {
        /**
         * Creates a new cuboid region.
         * @param fromX One of the edges on the X coördinate.
         * @param fromY One of the edges on the Y coördinate.
         * @param fromZ One of the edges on the Z coördinate.
         * @param toX The opposite edge on the X coördinate.
         * @param toY The opposite edge on the Y coördinate.
         * @param toZ The opposite edge on the Z coördinate.
         * @param isNegating Whether or not this region should be removing itself from the zone it's in rather than
         *                   adding to it.
         */
        public ZoneRegionCuboid(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, boolean isNegating)
        {
            if(toX < fromX)
            {
                int temp = toX;
                toX = fromX;
                fromX = temp;
            }

            if(toY < fromY)
            {
                int temp = toY;
                toY = fromY;
                fromY = temp;
            }

            if(toZ < fromZ)
            {
                int temp = toZ;
                toZ = fromZ;
                fromZ = temp;
            }

            this.minX = fromX;
            this.minY = fromY;
            this.minZ = fromZ;
            this.maxX = toX;
            this.maxY = toY;
            this.maxZ = toZ;
            this.isNegating = isNegating;
        }

        /**
         * Creates a new cuboid region.
         * @param fromX One of the edges on the X coördinate.
         * @param fromY One of the edges on the Y coördinate.
         * @param fromZ One of the edges on the Z coördinate.
         * @param toX The opposite edge on the X coördinate.
         * @param toY The opposite edge on the Y coördinate.
         * @param toZ The opposite edge on the Z coördinate.
         */
        public ZoneRegionCuboid(int fromX, int fromY, int fromZ, int toX, int toY, int toZ)
        { this(fromX, fromY, fromZ, toX, toY, toZ, false); }

        protected final int minX, minY, minZ, maxX, maxY, maxZ;
        protected final boolean isNegating;

        @Override
        public int getMinX()
        { return minX; }

        @Override
        public int getMinY()
        { return minY; }

        @Override
        public int getMinZ()
        { return minZ; }

        @Override
        public int getMaxX()
        { return maxX; }

        @Override
        public int getMaxY()
        { return maxY; }

        @Override
        public int getMaxZ()
        { return maxZ; }

        @Override
        public boolean isNegating()
        { return isNegating; }

        @Override
        public boolean contains(int x, int y)
        { return minX <= x && x <= maxX && minY <= y && y <= maxY; }

        @Override
        public boolean contains(int x, int y, int z)
        { return minX <= x && x <= maxX && minY <= y && y <= maxY && minZ <= z && z <= maxZ; }

        @Override
        public boolean contains(double x, double y)
        { return minX <= x && x < maxX + 1 && minY <= y && y < maxY + 1; }

        @Override
        public boolean contains(double x, double y, double z)
        { return minX <= x && x < maxX + 1 && minY <= y && y < maxY + 1 && minZ <= z && z < maxZ + 1; }

        @Override
        public boolean contains(Entity entity)
        { return contains(entity.getPosX(), entity.getPosY(), entity.getPosZ()); }

        @Override
        public ZoneRegionCuboid negating()
        { return new ZoneRegionCuboid(minX, minY, minZ, maxX, maxY, maxZ, true); }

        @Override
        public String toString()
        { return "(" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ") "; }

        @Override
        public boolean equals(Object o)
        {
            if(this == o)
                return true;

            if(o == null || getClass() != o.getClass())
                return false;

            ZoneRegionCuboid zoneRegionCuboid = (ZoneRegionCuboid)o;

            return minX == zoneRegionCuboid.minX
                && minY == zoneRegionCuboid.minY
                && minZ == zoneRegionCuboid.minZ
                && maxX == zoneRegionCuboid.maxX
                && maxY == zoneRegionCuboid.maxY
                && maxZ == zoneRegionCuboid.maxZ
                && isNegating == zoneRegionCuboid.isNegating;
        }

        @Override
        public int hashCode()
        { return Objects.hash(minX, minY, minZ, maxX, maxY, maxZ, isNegating); }
    }

    /**
     * A region representing a rectangular 2D area along the X and Z coördinates, extending up and down the Y
     * coördinate.
     */
    public static class ZoneRegionRectangle implements ZoneRegion
    {
        /**
         * Creates a new rectangular zone region.
         * @param fromX One of the edges on the X coördinate.
         * @param fromZ One of the edges on the Z coördinate.
         * @param toX The opposite edge on the X coördinate.
         * @param toZ The opposite edge on the Z coördinate.
         * @param negates Whether or not this region should be removing itself from the zone it's in rather than adding
         *                to it.
         */
        ZoneRegionRectangle(int fromX, int fromZ, int toX, int toZ, boolean negates)
        {
            if(toX < fromX)
            {
                int temp = toX;
                toX = fromX;
                fromX = temp;
            }

            if(toZ < fromZ)
            {
                int temp = toZ;
                toZ = fromZ;
                fromZ = temp;
            }

            this.minX = fromX;
            this.minZ = fromZ;
            this.maxX = toX;
            this.maxZ = toZ;
            this.isNegating = negates;
        }

        /**
         * Creates a new rectangular zone region.
         * @param fromX One of the edges on the X coördinate.
         * @param fromZ One of the edges on the Z coördinate.
         * @param toX The opposite edge on the X coördinate.
         * @param toZ The opposite edge on the Z coördinate.
         */
        public ZoneRegionRectangle(int fromX, int fromZ, int toX, int toZ)
        { this(fromX, fromZ, toX, toZ, false); }

        /**
         * Creates a new rectangular zone region corresponding to the chunk at a given XZ coördinate.
         * @param x The X coördinate.
         * @param z The Z coördinate.
         * @return A new zone region corresponding to the chunk at the given XZ coördinate.
         */
        static ZoneRegionRectangle ofChunkAt(int x, int z)
        {
            int minX = (x - (x % 16));
            int minZ = (z - (z % 16));
            if(x < 0) minX -= 16;
            if(z < 0) minZ -= 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            return new ZoneRegionRectangle(minX, minZ, maxX, maxZ);
        }

        /**
         * Creates a new rectangular zone region corresponding to the chunk with a given XZ chunk coördinate.
         * @param chunkX The chunk's X coördinate.
         * @param chunkZ The chunk's Z coördinate.
         * @return A new zone region corresponding to the chunk with the given XZ chunk coördinate.
         */
        static ZoneRegionRectangle ofChunk(int chunkX, int chunkZ)
        {
            int minX = chunkX * 16;
            int minZ = chunkZ * 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            return new ZoneRegionRectangle(minX, minZ, maxX, maxZ);
        }

        /**
         * Creates a new rectangular zone region corresponding to the chunk a given entity is currently in.
         * @param entity The entity to derive the chunk from.
         * @return A new zone region corresponding to the chunk the given entity is in.
         */
        static ZoneRegionRectangle ofEntitysChunk(Entity entity)
        { return ofChunk(entity.chunkCoordX, entity.chunkCoordZ); }

        protected final int minX, minZ, maxX, maxZ;
        protected final boolean isNegating;

        @Override
        public int getMinX()
        { return minX; }

        @Override
        public int getMinY()
        { return Integer.MIN_VALUE; }

        @Override
        public int getMinZ()
        { return minZ; }

        @Override
        public int getMaxX()
        { return maxX; }

        @Override
        public int getMaxY()
        { return Integer.MAX_VALUE; }

        @Override
        public int getMaxZ()
        { return maxZ; }

        @Override
        public boolean isNegating()
        { return isNegating; }

        @Override
        public boolean contains(int x, int y)
        { return minX <= x && x <= maxX && minZ <= y && y <= maxZ; }

        @Override
        public boolean contains(int x, int y, int z)
        { return contains(x, z); }

        @Override
        public boolean contains(double x, double y)
        { return minX <= x && x < maxX + 1 && minZ <= y && y < maxZ + 1; }

        @Override
        public boolean contains(double x, double y, double z)
        { return contains(x, z); }

        @Override
        public boolean contains(Entity entity)
        { return contains(entity.getPosX(), entity.getPosY()); }

        @Override
        public ZoneRegionRectangle negating()
        { return new ZoneRegionRectangle(minX, minZ, maxX, maxZ, true); }

        @Override
        public String toString()
        { return (isNegating ? "-(" : "(") + minX + ", " + minZ + ") to (" + maxX + ", " + maxZ + ") "; }

        @Override
        public boolean equals(Object o)
        {
            if(this == o)
                return true;

            if(o == null || getClass() != o.getClass())
                return false;

            ZoneRegionRectangle zoneRegionRectangle = (ZoneRegionRectangle)o;

            return minX == zoneRegionRectangle.minX
                && minZ == zoneRegionRectangle.minZ
                && maxX == zoneRegionRectangle.maxX
                && maxZ == zoneRegionRectangle.maxZ
                && isNegating == zoneRegionRectangle.isNegating;
        }

        @Override
        public int hashCode()
        { return Objects.hash(minX, minZ, maxX, maxZ, isNegating); }
    }

    /**
     * Creates a new zone.
     * @param name The unique name of the zone.
     * @param worldId The ID of the world the zone represents an area in.
     */
    public Zone(String name, String worldId)
    {
        this.name = name;
        this.worldId = worldId;
    }

    /**
     * The name of this zone. This is expected to be used as a unique identifier among zones.
     */
    private final String name;

    /**
     * The world id this zone is in, in the format of "mod:world_name". (wq)
     */
    private final String worldId;

    /**
     * The layers of the zone. These are in order from bottom layer to top layer, so the first item in the list may be
     * overridden by everything else, and the last item isn't overridden by anything.
     */
    private final List<ZoneRegion> regions = new ArrayList<>();

    /**
     * Gets the name of this zone.
     * @return The name of this zone.
     */
    public String getName()
    { return name; }

    /**
     * Gets the world ID of the world this represents an area in.
     * @return The ID of the world this zone is in.
     */
    public String getWorldId()
    { return worldId; }

    /**
     * Gets all regions in the zone, in order from lowest level layer to highest level. As in, later regions override
     * earlier ones, where some may be removing area from the zone rather than adding to it.
     * @return A list of the regions in this zone in order from lowest level layer to highest level.
     */
    public List<ZoneRegion> getRegions()
    {
        synchronized(regions)
        { return new ArrayList<>(regions); }
    }

    /**
     * Gets whether or not this zone contains the given XZ coördinate, disregarding world.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains a point at the given XZ coördinate, disregarding the world. Otherwise, false.
     */
    private boolean contains(double x, double z)
    {
        synchronized(regions)
        {
            for(int i = regions.size() - 1; i >= 0; i--)
            {
                ZoneRegion iregion = regions.get(i);

                if(iregion.contains(x, z))
                    return !iregion.isNegating();
            }
        }

        return false;
    }

    /**
     * Gets whether or not this zone contains the given XYZ coördinate, disregarding world.
     * @param x The X coördinate.
     * @param x The Y coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains a point at the given XYZ coördinate, disregarding the world. Otherwise, false.
     */
    private boolean contains(double x, double y, double z)
    {
        synchronized(regions)
        {
            for(int i = regions.size() - 1; i >= 0; i--)
            {
                ZoneRegion iregion = regions.get(i);

                if(iregion.contains(x, y, z))
                    return !iregion.isNegating();
            }
        }

        return false;
    }

    /**
     * Gets whether or not this zone contains the given XZ coördinate.
     * @param worldId The ID of the world the given coördinate is in.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains the given XZ coördinate. Otherwise, false.
     */
    public boolean contains(String worldId, int x, int z)
    { return this.worldId.equals(worldId) && contains(x, z); }

    /**
     * Gets whether or not this zone contains the given XYZ coördinate.
     * @param worldId The ID of the world the given coördinate is in.
     * @param x The X coördinate.
     * @param x The Y coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains the given XYZ coördinate. Otherwise, false.
     */
    public boolean contains(String worldId, int x, int y, int z)
    { return this.worldId.equals(worldId) && contains(x, y, z); }

    /**
     * Gets whether or not this zone contains the given XZ coördinate.
     * @param worldId The ID of the world the given coördinate is in.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains the given XZ coördinate. Otherwise, false.
     */
    public boolean contains(String worldId, double x, double z)
    { return this.worldId.equals(worldId) && contains(x, z); }

    /**
     * Gets whether or not this zone contains the given XYZ coördinate.
     * @param worldId The ID of the world the given coördinate is in.
     * @param x The X coördinate.
     * @param x The Y coördinate.
     * @param z The Z coördinate.
     * @return True if this zone contains the given XYZ coördinate. Otherwise, false.
     */
    public boolean contains(String worldId, double x, double y, double z)
    { return this.worldId.equals(worldId) && contains(x, y, z); }

    /**
     * Gets whether or not the given entity is within this zone.
     * @param entity The entity to check.
     * @return True if the given entity is within this zone. Otherwise, false.
     */
    public boolean contains(Entity entity)
    {
        return contains(PluginUtils.getWorldId(entity.getEntityWorld()),
                        entity.getPosX(),
                        entity.getPosY(),
                        entity.getPosZ());
    }

    /**
     * Gets whether or not the given location is within this zone.
     * @param location The location to check.
     * @return True if the given location is within this zone. Otherwise, false.
     */
    public boolean contains(EntityLocation location)
    { return contains(location.getWorldId(), location.getX(), location.getY(), location.getZ()); }

    /**
     * Adds a zone region to this zone.
     * @param region The region to add
     */
    void addRegion(ZoneRegion region)
    {
        synchronized(regions)
        { regions.add(region); }
    }

    /**
     * Removes all zone regions from this zone.
     */
    void clear()
    {
        synchronized(regions)
        { regions.clear(); }
    }

    /**
     * Makes a copy of this zone.
     * @return A new zone object, which is a shallow copy of this one.
     */
    public Zone copy()
    {
        Zone zone = new Zone(name, worldId);
        zone.regions.addAll(regions);
        return zone;
    }

    /**
     * Makes a copy of this zone, giving the copy a different name.
     * @param newName The name to give to the new copy.
     * @return A new zone object, which is a shallow copy of this one, with a different unique name.
     */
    public Zone copyWithNewName(String newName)
    {
        Zone zone = new Zone(newName, worldId);
        zone.regions.addAll(regions);
        return zone;
    }
}
