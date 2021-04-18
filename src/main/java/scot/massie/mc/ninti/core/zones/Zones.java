package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;
import scot.massie.mc.ninti.core.NintiCore;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Static registry for registering areas of the server's worlds to be identifiable by name. Provides a static interface
 * to an instance of {@link ZoneRegistry}.
 */
public final class Zones
{
    private Zones()
    {}

    private static final Path ZoneRegistryFile = NintiCore.DATA_FOLDER.resolve("zones.txt");
    private static final ZoneRegistry reg = new ZoneRegistry(ZoneRegistryFile);

    /**
     * Registers a new zone. If the zone shares a name with a zone already present, overwrites that zone.
     * @param zone The zone to register.
     */
    public static void register(Zone zone)
    { synchronized(reg) { reg.register(zone); } }

    /**
     * Deregisters the zone by the given name.
     * @param zoneName The name of the zone to deregister.
     * @return The zone deregistered, or null if there was no zone by the given name.
     */
    public static Zone deregister(String zoneName)
    { synchronized(reg) { return reg.deregister(zoneName); } }

    /**
     * Renames a zone. If another zone exists with the given name, overwrites that.
     * @param zoneName The name of the zone to rename.
     * @param newZoneName The name to rename the zone to.
     * @return The zone renamed.
     */
    public static Zone rename(String zoneName, String newZoneName)
    { synchronized(reg) { return reg.rename(zoneName, newZoneName); } }

    /**
     * Adds the zone region to the zone by the given name, if a zone by the given name exists in the registry.
     * @param zoneName The name of the zone to add the zone region to.
     * @param region The zone region to add to the zone.
     * @return The zone added to.
     */
    public static Zone addToZoneIfThere(String zoneName, Zone.ZoneRegion region)
    { synchronized(reg) { return reg.addToZoneIfThere(zoneName, region); } }

    /**
     * Gets the zone by the given name.
     * @param zoneName The name of the zone to get.
     * @return The zone by the given name, or null if there is no zone by the given name.
     */
    public static Zone getZone(String zoneName)
    { synchronized(reg) { return reg.get(zoneName); } }

    /**
     * Gets all zones in the registry.
     * @return A list of the zones in the registry, ordered by name.
     */
    public static List<Zone> getZones()
    { synchronized(reg) { return reg.getZones(); } }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public static Collection<Zone> getZonesAt(String worldId, int x, int z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, z); } }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public static Collection<Zone> getZonesAt(String worldId, int x, int y, int z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, y, z); } }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public static Collection<Zone> getZonesAt(String worldId, double x, double z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, z); } }

    /**
     * Gets all zones in the registry covering the point represented by the given coördinates and world ID.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A collection of all zones that cover the given position.
     */
    public static Collection<Zone> getZonesAt(String worldId, double x, double y, double z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, y, z); } }

    /**
     * Gets all zones in the registry covering the given location object.
     * @param location The location to get the zones covering.
     * @return A collection of all zones that cover the given position.
     */
    public static Collection<Zone> getZonesAt(EntityLocation location)
    { synchronized(reg) { return reg.getZonesAt(location); } }

    /**
     * Gets all zones the given entity is in.
     * @param entity The entity to get the current zones of.
     * @return A collection of all zones that the given entity is in.
     */
    public static Collection<Zone> getZonesEntityIsIn(Entity entity)
    { synchronized(reg) { return reg.getZonesEntityIsIn(entity); } }

    /**
     * Gets a list of all zones in the registry.
     * @return A list of the names of all zones in the registry, in alphabetical order.
     */
    public static List<String> getZoneNames()
    { synchronized(reg) { return reg.getZoneNames(); } }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public static List<String> getZoneNamesAt(String worldId, int x, int z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, z); } }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public static List<String> getZoneNamesAt(String worldId, int x, int y, int z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, y, z); } }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public static List<String> getZoneNamesAt(String worldId, double x, double z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, z); } }

    /**
     * Gets the names of all zones covering the point represented by the given coördinates and world ID, in alphabetical
     * order.
     * @param worldId The ID of the world to get zones of.
     * @param x The X coördinate.
     * @param y The Y coördinate.
     * @param z The Z coördinate.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public static List<String> getZoneNamesAt(String worldId, double x, double y, double z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, y, z); } }

    /**
     * Gets the names of all zones covering the given location object.
     * @param location The location to get the names of zones at.
     * @return A list of the names of all zones that cover the given position, in alphabetical order.
     */
    public static List<String> getZoneNamesAt(EntityLocation location)
    { synchronized(reg) { return reg.getZoneNamesAt(location); } }

    /**
     * Gets the names of all zones the given entity is in.
     * @param entity The entity to get the names of zones it's currently in.
     * @return A list of the zones currently containing the given entity's location, in alphabetical order.
     */
    public static List<String> getZoneNamesEntityIsIn(Entity entity)
    { synchronized(reg) { return reg.getZoneNamesEntityIsIn(entity); } }

    /**
     * Saves the contents of the zones registry.
     */
    public static void save()
    { synchronized(reg) { reg.save(); } }

    /**
     * Loads the contents of the zones registry from the zones file.
     */
    public static void load()
    { synchronized(reg) { reg.load(); } }
}
