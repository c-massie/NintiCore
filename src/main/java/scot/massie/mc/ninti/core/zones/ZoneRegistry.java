package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Registry for registering areas of the server's worlds to be indentifiable by name.
 */
public final class ZoneRegistry
{
    /**
     * Creates a new zone registry.
     * @param filePath The file path to save the zone registry's file at.
     */
    public ZoneRegistry(String filePath)
    { this.filePath = Paths.get(filePath); }

    /**
     * Creates a new zone registry.
     * @param filePath The file path to save the zone registry's file at.
     */
    public ZoneRegistry(Path filePath)
    { this.filePath = filePath; }

    /**
     * Creates a new zone registry.
     * @param filePath The file path to save the zone registry's file at.
     */
    public ZoneRegistry(File filePath)
    { this.filePath = filePath.toPath(); }

    private final Map<String, Zone> zones = new HashMap<>();
    private final Path filePath;
    private boolean changedSinceLoad = false;

    /**
     * Marks the zone registry as having had its contents modified since the last time it was saved or loaded.
     */
    private void markAsChanged()
    { changedSinceLoad = true; }

    /**
     * Marks the zone registry as not having had its contents modified since the last time it was saved or loaded.
     */
    private void resetChangedFlag()
    { changedSinceLoad = false; }

    /**
     * Registers a new zone. If the zone shares a name with a zone already present, overwrites that zone.
     * @param zone The zone to register.
     */
    public void register(Zone zone)
    {
        zones.put(zone.getName(), zone.copy());
        markAsChanged();
    }

    /**
     * Deregisters the zone by the given name.
     * @param zoneName The name of the zone to deregister.
     * @return The zone deregistered, or null if there was no zone by the given name.
     */
    public Zone deregister(String zoneName)
    {
        Zone result = zones.remove(zoneName);

        if(result != null)
            markAsChanged();

        return result;
    }

    /**
     * Renames a zone. If another zone exists with the given name, overwrites that.
     * @param zoneName The name of the zone to rename.
     * @param newZoneName The name to rename the zone to.
     * @return The zone renamed.
     */
    public Zone rename(String zoneName, String newZoneName)
    {
        Zone oldZone = zones.remove(zoneName);

        if(oldZone == null)
            return null;

        Zone newZone = oldZone.copyWithNewName(newZoneName);
        zones.put(newZoneName, newZone);
        markAsChanged();
        return newZone.copy();
    }

    /**
     * Adds the zone region to the zone by the given name, if a zone by the given name exists in the registry.
     * @param zoneName The name of the zone to add the zone region to.
     * @param region The zone region to add to the zone.
     * @return The zone added to.
     */
    public Zone addToZoneIfThere(String zoneName, Zone.ZoneRegion region)
    {
        Zone zone = zones.get(zoneName);

        if(zone == null)
            return null;

        zone.addRegion(region);
        markAsChanged();
        return zone.copy();
    }

    /**
     * Gets the zone by the given name.
     * @param zoneName The name of the zone to get.
     * @return The zone by the given name, or null if there is no zone by the given name.
     */
    public Zone get(String zoneName)
    {
        Zone zone = zones.get(zoneName);

        if(zone == null)
            return null;

        return zone.copy();
    }

    /**
     * Gets all zones in the registry.
     * @return A list of the zones in the registry, ordered by name.
     */
    public List<Zone> getZones()
    {
        List<Zone> result = new ArrayList<>();

        for(Zone z : zones.values())
            result.add(z.copy());

        result.sort(Comparator.comparing(Zone::getName));
        return result;
    }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public Collection<Zone> getZonesAt(String worldId, int x, int z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, z))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public Collection<Zone> getZonesAt(String worldId, int x, int y, int z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, y, z))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public Collection<Zone> getZonesAt(String worldId, double x, double z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, z))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public Collection<Zone> getZonesAt(String worldId, double x, double y, double z)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, y, z))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets all zones in the registry covering the given location object.
     * @param location The location to get the zones covering.
     * @return A collection of all zones that cover the given position.
     */
    public Collection<Zone> getZonesAt(EntityLocation location)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(location))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets all zones the given entity is in.
     * @param entity The entity to get the current zones of.
     * @return A collection of all zones that the given entity is in.
     */
    public Collection<Zone> getZonesEntityIsIn(Entity entity)
    {
        Collection<Zone> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(entity))
                result.add(zone.copy());

        return result;
    }

    /**
     * Gets a list of all zones in the registry.
     * @return A list of the names of all zones in the registry, in alphabetical order.
     */
    public List<String> getZoneNames()
    {
        List<String> result = new ArrayList<>();

        for(Zone z : zones.values())
            result.add(z.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public List<String> getZoneNamesAt(String worldId, int x, int z)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, z))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public List<String> getZoneNamesAt(String worldId, int x, int y, int z)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, y, z))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public List<String> getZoneNamesAt(String worldId, double x, double z)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, z))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public List<String> getZoneNamesAt(String worldId, double x, double y, double z)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(worldId, x, y, z))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones covering the given location object.
     * @param location The location to get the names of zones at.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public List<String> getZoneNamesAt(EntityLocation location)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(location))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    /**
     * Gets the names of all zones the given entity is in.
     * @param entity The entity to get the names of zones it's currently in.
     * @return A list of the zones currently containing the given entity's location, in alphabetical order.
     */
    public List<String> getZoneNamesEntityIsIn(Entity entity)
    {
        List<String> result = new ArrayList<>();

        for(Zone zone : zones.values())
            if(zone.contains(entity))
                result.add(zone.getName());

        result.sort(Comparator.naturalOrder());
        return result;
    }

    //region Saving

    /**
     * Saves the contents of the zones registry to the registry's save file location.
     */
    public void save()
    {
        if(!changedSinceLoad)
            return;

        if(!filePath.getParent().toFile().mkdirs())
            throw new RuntimeException("Could not create the directory the zone file should be in.");

        try(BufferedWriter writer = Files.newBufferedWriter(filePath))
        { writeZones(writer); }
        catch(IOException e)
        { throw new RuntimeException("Could not save the zones file.", e); }

        resetChangedFlag();
    }

    /**
     * Saves the zones in the zones registry to the given writer. Zones are formatted as specified by
     * {@link #zoneToString(Zone)}.
     * @param writer The writer to write to.
     * @throws IOException If an IO exception is thrown by the given writer.
     */
    private void writeZones(Writer writer) throws IOException
    {
        List<Zone> zonesSorted = new ArrayList<>(zones.values());
        zonesSorted.sort(Comparator.comparing(Zone::getName));

        for(Zone z : zonesSorted)
            writer.write(zoneToString(z));
    }

    /**
     * Converts a zone into a parsable string representation for the purposes of saving. This produces a string where
     * the first line is the zone's name, followed by a colon, followed by the ID of the world the zone is in. Each
     * successive line is a region in the zone as provided by {@link #zoneRegionToString(Zone.ZoneRegion)}, in order
     * from the bottom layering region to the top.
     * @param zone The zone to get a string representation of.
     * @return A string representation of the given zone.
     */
    private static String zoneToString(Zone zone)
    {
        StringBuilder result = new StringBuilder(zone.getName() + ": " + zone.getWorldId());

        for(Zone.ZoneRegion region : zone.getRegions())
            result.append("\n    ").append(zoneRegionToString(region));

        return result + "\n\n";
    }

    /**
     * <p>Converts a zone region into a parsable string representation for the purposes of saving.</p>
     *
     * <p>As: ["NOT" if negating] [x], [y], [z] -> [x], [y], [z]</p>
     *
     * <p>e.g.: 5, 10, 12 -> 50, 20, 20</p>
     * @param region The region to get a string representation of.
     * @return A string representation of the given zone region.
     */
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

    /**
     * Replaces the contents of the zones registry with the interpreted contents of the zones registry file at the
     * registry's save file location.
     */
    public void load()
    {
        if((filePath == null) || (!Files.isReadable(filePath)) || (Files.isDirectory(filePath)))
            return;

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

    /**
     * Reads the zones from a reader into a list of zones. Reads zones in the format as specified by
     * {@link #zoneToString(Zone)}.
     * @param reader The reader to read zones from.
     * @return The list of zones read from the given reader.
     * @throws IOException If an IO exception is thrown by the given reader, or if the text read by the reader is not
     *                     parsable as zones.
     */
    private static List<Zone> readZones(BufferedReader reader) throws IOException
    {
        List<Zone> result = new ArrayList<>();
        Zone currentZone = null;

        for(String line; (line = reader.readLine()) != null;)
        {
            if(line.trim().isEmpty())
                continue;

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

    /**
     * Creates a zone with no regions from the first line in a zone string representation as specified by
     * {@link #zoneToString(Zone)}.
     * @param zoneHeader The first line of a string representation of a zone.
     * @return The represented zone, without any regions.
     * @throws IOException If the given string was not parsable as a zone header.
     */
    private static Zone readZoneFromHeader(String zoneHeader) throws IOException
    {
        String[] split = zoneHeader.split(":", 2);

        if(split.length < 2)
            throw new IOException("Zone header not made up of a zone name and world world split by a colon.");

        return new Zone(split[0].trim(), split[1].trim());
    }

    /**
     * Creates a zone region from a string representation of a zone region, as specified by
     * {@link #zoneRegionToString(Zone.ZoneRegion)}.
     * @param line The zone region representation to parse.
     * @return The zone region represented by the given representation.
     * @throws IOException If the line is not parsable as a zone region.
     */
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
