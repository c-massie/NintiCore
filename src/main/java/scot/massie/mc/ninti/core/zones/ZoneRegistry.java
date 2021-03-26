package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

public final class ZoneRegistry
{
    public ZoneRegistry(String filePath)
    { this.filePath = Paths.get(filePath); }

    final Map<String, Zone> zones = new HashMap<>();
    private final Path filePath;
    private boolean changedSinceLoad = false;

    private void markAsChanged()
    { changedSinceLoad = true; }

    private void resetChangedFlag()
    { changedSinceLoad = false; }

    public void register(Zone zone)
    {
        zones.put(zone.getName(), zone.copy());
        markAsChanged();
    }

    public boolean deregister(String zoneName)
    {
        zones.remove(zoneName);

        markAsChanged();

        if(zones.remove(zoneName) != null)
        {
            markAsChanged();
            return true;
        }
        else
            return false;
    }

    public boolean rename(String zoneName, String newZoneName)
    {
        Zone oldZone = zones.remove(zoneName);

        if(oldZone == null)
            return false;

        zones.put(newZoneName, oldZone.copyWithNewName(newZoneName));
        return true;
    }

    public boolean addToZoneIfThere(String zoneName, Zone.ZoneRegion region)
    {
        Zone zone = zones.get(zoneName);

        if(zone == null)
            return false;

        zone.addRegion(region);
        return true;
    }

    public Zone get(String zoneName)
    {
        Zone zone = zones.get(zoneName);

        if(zone == null)
            return null;

        return zone.copy();
    }

    public List<Zone> getZones()
    {
        List<Zone> result = new ArrayList<>();

        for(Zone z : zones.values())
            result.add(z.copy());

        result.sort(Comparator.comparing(Zone::getName));
        return result;
    }

    public List<String> getZoneNames()
    {
        List<String> result = new ArrayList<>();

        for(Zone z : zones.values())
            result.add(z.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    public Collection<Zone> getZonesAt(int x, int y)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(x, y))
                result.add(zone.copy());

        return result;
    }

    public Collection<Zone> getZonesAt(int x, int y, int z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(x, y, z))
                result.add(zone.copy());

        return result;
    }

    public Collection<Zone> getZonesAt(double x, double y)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(x, y))
                result.add(zone.copy());

        return result;
    }

    public Collection<Zone> getZonesAt(double x, double y, double z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(x, y, z))
                result.add(zone.copy());

        return result;
    }

    public Collection<Zone> getZonesEntityIsIn(Entity entity)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(entity))
                result.add(zone.copy());

        return result;
    }

    //region Saving
    public void save()
    {
        if(!changedSinceLoad)
            return;

        try
        { writeZones(Files.newBufferedWriter(filePath)); }
        catch(IOException e)
        { throw new RuntimeException("Could not save the zones file.", e); }

        resetChangedFlag();
    }

    private void writeZones(Writer writer) throws IOException
    {
        List<Zone> zonesSorted = new ArrayList<>(zones.values());
        zonesSorted.sort(Comparator.comparing(Zone::getName));

        for(Zone z : zonesSorted)
            writer.write(zoneToString(z));
    }

    private static String zoneToString(Zone zone)
    {
        String result = zone.getName() + ": " + zone.getWorldId();

        for(Zone.ZoneRegion region : zone.getRegions())
            result += "\n    " + zoneRegionToString(region);

        return result;
    }

    private static String zoneRegionToString(Zone.ZoneRegion region)
    {
        String result;

        if(region instanceof Zone.ZoneRegionRectangle)
            result = region.getMinX() + ", " + region.getMinZ() + " -> " + region.getMaxX() + ", " + region.getMaxZ();
        else
            result =          region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ()
                              + " -> " + region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ();

        if(region.isNegating())
            result = "NOT " + result;

        return result;
    }
    //endregion

    //region Loading
    public void load()
    {
        List<Zone> zonesRead;

        try
        { zonesRead = readZones(Files.newBufferedReader(filePath)); }
        catch(IOException e)
        { throw new RuntimeException("Could not load the zones file.", e); }

        zones.clear();

        for(Zone zone : zonesRead)
            zones.put(zone.getName(), zone);

        resetChangedFlag();
    }

    private static List<Zone> readZones(BufferedReader reader) throws IOException
    {
        List<Zone> result = new ArrayList<>();
        Zone currentZone = null;

        for(String line; (line = reader.readLine()) != null;)
        {
            if(line.startsWith("    "))
            {
                if(currentZone == null)
                    continue;

                currentZone.addRegion(readZoneRegionFromLine(line.substring(4).trim()));
            }
            else
            {
                if(currentZone != null)
                    result.add(currentZone);

                currentZone = readZoneFromHeader(line);
            }
        }

        if(currentZone != null)
            result.add(currentZone);

        return result;
    }

    private static Zone readZoneFromHeader(String zoneHeader) throws IOException
    {
        String[] split = zoneHeader.split(":", 2);

        if(split.length < 2)
            throw new IOException("Zone header not made up of a zone name and world world split by a colon.");

        return new Zone(split[0].trim(), split[1].trim());
    }

    private static Zone.ZoneRegion readZoneRegionFromLine(String line) throws IOException
    {
        boolean negates = line.startsWith("NOT ");

        if(negates)
            line = line.substring(4);

        String[] split = line.split("->", 2);

        if(split.length != 2)
            throw new IOException("Zone region isn't formatted as (x, z) -> (x, z) or (x, y, z) -> (x, y, z)");

        String[] fromSplit = split[0].trim().split(", ");
        String[] toSplit = split[1].trim().split(", ");

        if(fromSplit.length < 2 || fromSplit.length > 3 || toSplit.length < 2 || toSplit.length > 3)
            throw new IOException("Zone region isn't formatted as (x, z) -> (x, z) or (x, y, z) -> (x, y, z)");

        if(fromSplit.length == 3 || toSplit.length == 3)
        {
            int fromX, fromY, fromZ, toX, toY, toZ;

            try
            {
                fromX = Integer.parseInt(fromSplit[0]);
                toX   = Integer.parseInt(toSplit[0]);

                if(fromSplit.length == 2)
                {
                    fromY = Integer.MIN_VALUE;
                    toY   = Integer.parseInt(toSplit  [1]);
                    fromZ = Integer.parseInt(fromSplit[1]);
                    toZ   = Integer.parseInt(toSplit  [2]);
                }
                else if(toSplit.length == 2)
                {
                    fromY = Integer.parseInt(fromSplit[1]);
                    toY   = Integer.MAX_VALUE;
                    fromZ = Integer.parseInt(fromSplit[2]);
                    toZ   = Integer.parseInt(toSplit  [1]);
                }
                else // both 3
                {
                    fromY = Integer.parseInt(fromSplit[1]);
                    toY   = Integer.parseInt(toSplit  [1]);
                    fromZ = Integer.parseInt(fromSplit[2]);
                    toZ   = Integer.parseInt(toSplit  [2]);
                }
            }
            catch(NumberFormatException e)
            { throw new IOException("Coördinate not parsable as number.", e); }

            return new Zone.ZoneRegionCuboid(fromX, fromY, fromZ, toX, toY, toZ, negates);
        }
        else
        {
            int fromX, fromZ, toX, toZ;

            try
            {
                fromX = Integer.parseInt(fromSplit[0]);
                fromZ = Integer.parseInt(fromSplit[1]);
                toX   = Integer.parseInt(toSplit  [0]);
                toZ   = Integer.parseInt(toSplit  [1]);
            }
            catch(NumberFormatException e)
            { throw new IOException("Coördinate not parsable as number.", e); }

            return new Zone.ZoneRegionRectangle(fromX, fromZ, toX, toZ, negates);
        }
    }
    //endregion
}
