package scot.massie.mc.ninti.core;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.OpEntry;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.event.world.WorldEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public final class StaticUtilFunctions
{
    private StaticUtilFunctions()
    {}

    static MinecraftServer minecraftServer;

    public static MinecraftServer getServer()
    { return minecraftServer; }

    public static String getWorldId(ServerWorld world)
    { return world.getDimensionKey().getLocation().toString(); }

    public static String getWorldId(WorldEvent.Save worldSaveEvent)
    { return getWorldId((ServerWorld)(worldSaveEvent.getWorld())); }

    public static void sendMessage(CommandContext<CommandSource> cmdContext, String msg)
    { cmdContext.getSource().sendFeedback(new StringTextComponent(msg), true); }

    public static UUID getLastKnownUUIDOfPlayer(String username)
    {
        for(Map.Entry<UUID, String> entry : UsernameCache.getMap().entrySet())
            if(entry.getValue().equals(username))
                return entry.getKey();

        return null;
    }

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
