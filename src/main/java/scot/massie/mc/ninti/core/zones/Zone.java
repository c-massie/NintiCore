package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Zone
{
    public interface ZoneRegion
    {
        int getMinX();
        int getMinY();
        int getMinZ();
        int getMaxX();
        int getMaxY();
        int getMaxZ();
        boolean isNegating();
        boolean contains(int x, int y);
        boolean contains(int x, int y, int z);
        boolean contains(double x, double y);
        boolean contains(double x, double y, double z);
        boolean contains(Entity entity);
        ZoneRegion negating();
    }

    public static class ZoneRegionCuboid implements ZoneRegion
    {
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

    public static class ZoneRegionRectangle implements ZoneRegion
    {
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

        public ZoneRegionRectangle(int fromX, int fromZ, int toX, int toZ)
        { this(fromX, fromZ, toX, toZ, false); }

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

        static ZoneRegionRectangle ofChunk(int chunkX, int chunkZ)
        {
            int minX = chunkX * 16;
            int minZ = chunkZ * 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            return new ZoneRegionRectangle(minX, minZ, maxX, maxZ);
        }

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

    public Zone(String name, String worldId)
    {
        this.name = name;
        this.worldId = worldId;
    }

    private final String name;
    private final String worldId;
    private final List<ZoneRegion> regions = new ArrayList<>();

    public String getName()
    { return name; }

    public String getWorldId()
    { return worldId; }

    public List<ZoneRegion> getRegions()
    {
        synchronized(regions)
        { return new ArrayList<>(regions); }
    }

    public boolean contains(int x, int y)
    {
        synchronized(regions)
        {
            for(int i = regions.size() - 1; i >= 0; i--)
            {
                ZoneRegion iregion = regions.get(i);

                if(iregion.contains(x, y))
                    return !iregion.isNegating();
            }
        }

        return false;
    }

    public boolean contains(int x, int y, int z)
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

    public boolean contains(double x, double y)
    {
        synchronized(regions)
        {
            for(int i = regions.size() - 1; i >= 0; i--)
            {
                ZoneRegion iregion = regions.get(i);

                if(iregion.contains(x, y))
                    return !iregion.isNegating();
            }
        }

        return false;
    }

    public boolean contains(double x, double y, double z)
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

    public boolean contains(Entity entity)
    { return contains(entity.getPosX(), entity.getPosY(), entity.getPosZ()); }

    void addRegion(ZoneRegion region)
    {
        synchronized(regions)
        { regions.add(region); }
    }

    void clear()
    {
        synchronized(regions)
        { regions.clear(); }
    }

    public Zone copy()
    {
        Zone zone = new Zone(name, worldId);
        zone.regions.addAll(regions);
        return zone;
    }

    public Zone copyWithNewName(String newName)
    {
        Zone zone = new Zone(newName, worldId);
        zone.regions.addAll(regions);
        return zone;
    }
}
