package scot.massie.mc.ninti.core.zones;

import net.minecraft.entity.Entity;
import scot.massie.mc.ninti.core.utilclasses.EntityLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public final class Zones
{
    private Zones()
    {}

    private static final ZoneRegistry reg = new ZoneRegistry("plugindata/ninti/zones.txt");

    public static void register(Zone zone)
    { synchronized(reg) { reg.register(zone); } }

    public static Zone deregister(String zoneName)
    { synchronized(reg) { return reg.deregister(zoneName); } }

    public static Zone rename(String zoneName, String newZoneName)
    { synchronized(reg) { return reg.rename(zoneName, newZoneName); } }

    public static Zone addToZoneIfThere(String zoneName, Zone.ZoneRegion region)
    { synchronized(reg) { return reg.addToZoneIfThere(zoneName, region); } }

    public static Zone getZone(String zoneName)
    { synchronized(reg) { return reg.get(zoneName); } }

    public static List<Zone> getZones()
    { synchronized(reg) { return reg.getZones(); } }

    public static Collection<Zone> getZonesAt(String worldId, int x, int z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, z); } }

    public static Collection<Zone> getZonesAt(String worldId, int x, int y, int z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, y, z); } }

    public static Collection<Zone> getZonesAt(String worldId, double x, double z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, z); } }

    public static Collection<Zone> getZonesAt(String worldId, double x, double y, double z)
    { synchronized(reg) { return reg.getZonesAt(worldId, x, y, z); } }

    public static Collection<Zone> getZonesAt(EntityLocation location)
    { synchronized(reg) { return reg.getZonesAt(location); } }

    public static Collection<Zone> getZonesEntityIsIn(Entity entity)
    { synchronized(reg) { return reg.getZonesEntityIsIn(entity); } }

    public static List<String> getZoneNames()
    { synchronized(reg) { return reg.getZoneNames(); } }

    public static List<String> getZoneNamesAt(String worldId, int x, int z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, z); } }

    public static List<String> getZoneNamesAt(String worldId, int x, int y, int z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, y, z); } }

    public static List<String> getZoneNamesAt(String worldId, double x, double z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, z); } }

    public static List<String> getZoneNamesAt(String worldId, double x, double y, double z)
    { synchronized(reg) { return reg.getZoneNamesAt(worldId, x, y, z); } }

    public static List<String> getZoneNamesAt(EntityLocation location)
    { synchronized(reg) { return reg.getZoneNamesAt(location); } }

    public static List<String> getZoneNamesEntityIsIn(Entity entity)
    { synchronized(reg) { return reg.getZoneNamesEntityIsIn(entity); } }

    public static void save()
    { synchronized(reg) { reg.save(); } }

    public static void load()
    { synchronized(reg) { reg.load(); } }
}
