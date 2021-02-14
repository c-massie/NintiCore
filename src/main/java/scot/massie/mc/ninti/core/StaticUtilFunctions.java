package scot.massie.mc.ninti.core;

import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.WorldEvent;

public final class StaticUtilFunctions
{
    private StaticUtilFunctions()
    {}

    public static String getWorldId(ServerWorld world)
    { return world.getDimensionKey().getLocation().toString(); }

    public static String getWorldId(WorldEvent.Save worldSaveEvent)
    { return getWorldId((ServerWorld)(worldSaveEvent.getWorld())); }
}
