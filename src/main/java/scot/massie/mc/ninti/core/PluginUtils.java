package scot.massie.mc.ninti.core;

import com.google.common.collect.Lists;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for miscellaneous minecraft-related convenience methods.
 */
public final class PluginUtils
{
    /**
     * Repository for miscellaneous minecraft-related comparators.
     */
    public static class Comparators
    {
        private Comparators()
        {}

        /**
         * Compares UUIDs according to the username of the player they represent if they represent a player. If they
         * don't represent a player, compares the UUIDs directly, where UUIDs representing a player come before. Nulls
         * come first.
         */
        public static final Comparator<UUID> PLAYER_ID_BY_NAME = (a, b) ->
        {
            if(a == null)
                return b == null ? 0 : -1;

            if(b == null)
                return 1;

            String aName = UsernameCache.getLastKnownUsername(a);
            String bName = UsernameCache.getLastKnownUsername(b);

            if(aName == null)
                return bName == null ? a.compareTo(b) : 1;

            if(bName == null)
                return -1;

            return aName.compareTo(bName);
        };
    }

    private PluginUtils()
    {}

    /**
     * The instance of the minecraft server. This is not arbitrarily accessible, so a reference is stored here when the
     * mod loads, as it can be accessed from the server startup event.
     */
    static MinecraftServer minecraftServer;

    /**
     * Gets the minecraft server.
     * @return The minecraft server instance.
     */
    public static MinecraftServer getServer()
    { return minecraftServer; }

    /**
     * Gets a path object representing the plugin data folder.
     * @return A path object representing the plugin data folder.
     */
    public static Path getPluginDataFolder()
    { return Paths.get("plugindata"); }

    /**
     * Gets the ID of a given world.
     * @param world The world to get the ID of.
     * @return The ID of the world, as a string, usually in the format of mod:world (where minecraft may be considered
     *         a mod.)
     */
    public static String getWorldId(World world)
    { return world.getDimensionKey().getLocation().toString(); }

    /**
     * Gets the ID of a world being saved by a given save event.
     * @param worldSaveEvent The save event of a world.
     * @return The ID of the world being saved, as a string, usually in the format of mod:world (where minecraft may be
     *         considered a mod.)
     */
    public static String getWorldId(WorldEvent.Save worldSaveEvent)
    { return getWorldId((ServerWorld)(worldSaveEvent.getWorld())); }

    /**
     * Gets The world represented by the given ID.
     * @param worldId The ID of the world to get.
     * @return The world object for the world matching the given world ID, or null if there is no world matching the
     *         given world ID.
     */
    public static ServerWorld getWorldById(String worldId)
    {
        for(ServerWorld world : minecraftServer.getWorlds())
            if(getWorldId(world).equals(worldId))
                return world;

        return null;
    }

    /**
     * Gets the default world.
     * @return The default world object.
     */
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

    /**
     * Gets the default world ID.
     * @return The default world ID, as a string.
     */
    public static String getDefaultWorldId()
    {
        // Assumes the default world is minecraft:overworld - It's not currently clear how to derive the actual default
        // world and it usually is minecraft:overworld
        return "minecraft:overworld";
    }

    /**
     * Sends a message to the given command context. If the command is called by a player, displays the text in that
     * player's chat. If the command is called by the console, displays the text int he console.
     * @param cmdContext The context of the command.
     * @param msg The message to send to whoever is sending the command.
     */
    public static void sendMessage(CommandContext<CommandSource> cmdContext, String msg)
    { cmdContext.getSource().sendFeedback(new StringTextComponent(msg), true); }

    /**
     * Gets the last known UUID of the player by the given username.
     * @param username The username of the player to get the UUID of.
     * @return the UUID of the player, or null if the player has not been recorded by this server.
     */
    public static UUID getLastKnownUUIDOfPlayer(String username)
    {
        for(Map.Entry<UUID, String> entry : UsernameCache.getMap().entrySet())
            if(entry.getValue().equals(username))
                return entry.getKey();

        return null;
    }

    /**
     * Gets the online player with the given ID.
     * @param playerId The player's ID.
     * @return The online player with the given ID, or null if not player is currently online with the given ID.
     */
    public static ServerPlayerEntity getOnlinePlayer(UUID playerId)
    { return minecraftServer.getPlayerList().getPlayerByUUID(playerId); }

    /**
     * Gets whether or not the given player has been marked as an operator.
     * @param player The player to check.
     * @return True if the given player is an operator. Otherwise, false.
     */
    public static boolean playerIsOp(PlayerEntity player)
    { return minecraftServer.getPlayerList().getOppedPlayers().getEntry(player.getGameProfile()) != null; }

    /**
     * Gets whether or not the given player has been marked as an operator.
     * @param playerId The ID of the player to check.
     * @return True if the given player is an operator, or false if there is no player with the given ID, or if there
     *         is, but the player by the given ID is not an operator.
     */
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
