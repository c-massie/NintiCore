package scot.massie.mc.ninti.core;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.event.world.WorldEvent;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public final class PluginUtils
{
    private PluginUtils()
    {}

    static MinecraftServer minecraftServer;

    public static MinecraftServer getServer()
    { return minecraftServer; }

    public static Path getPluginDataFolder()
    { return Paths.get("plugindata"); }

    public static String getWorldId(ServerWorld world)
    { return world.getDimensionKey().getLocation().toString(); }

    public static String getWorldId(WorldEvent.Save worldSaveEvent)
    { return getWorldId((ServerWorld)(worldSaveEvent.getWorld())); }

    public static World getDefaultWorld()
    {
        // Assumes the default world is minecraft:overworld - It's not currently clear how to derive the actual default
        // world and it usually is minecraft:overworld

        for(ServerWorld w : minecraftServer.getWorlds())
            if(getWorldId(w).equals("minecraft:overworld"))
                return w;

        System.err.println("Could not provide a default server world instance.");
        throw new RuntimeException("Could not provide a default server world instance.");
    }

    public static String getDefaultWorldId()
    {
        // Assumes the default world is minecraft:overworld - It's not currently clear how to derive the actual default
        // world and it usually is minecraft:overworld
        return "minecraft:overworld";
    }

    public static void sendMessage(CommandContext<CommandSource> cmdContext, String msg)
    { cmdContext.getSource().sendFeedback(new StringTextComponent(msg), true); }

    public static UUID getLastKnownUUIDOfPlayer(String username)
    {
        for(Map.Entry<UUID, String> entry : UsernameCache.getMap().entrySet())
            if(entry.getValue().equals(username))
                return entry.getKey();

        return null;
    }

    public static PlayerEntity getOnlinePlayer(UUID playerId)
    { return minecraftServer.getPlayerList().getPlayerByUUID(playerId); }

    public static boolean playerIsOp(PlayerEntity player)
    { return minecraftServer.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null; }

    public static boolean playerIsOp(UUID playerId)
    {
        Collection<OpEntry> opEntries = minecraftServer.getPlayerList().getOppedPlayers().getEntries();

        for(OpEntry entry : opEntries)
        {
            if(entry.value.getId().equals(playerId))
                return true;
        }

        return false;
    }
}
