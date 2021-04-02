package scot.massie.mc.ninti.core.zones;

import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.server.ServerWorld;
import scot.massie.mc.ninti.core.NintiCore;
import scot.massie.mc.ninti.core.Permissions;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static net.minecraft.command.Commands.*;
import static scot.massie.mc.ninti.core.PluginUtils.*;

public class ZonesCommandHandler
{
    /*

    zones save
    zones load
    zones list
    zones list here
    zones list in [world id] at [x] [z]
    zones list in [world id] at [x] [y] [z]
    zones list at [x] [z]
    zones list at [x] [y] [z]
    zones create [zone name] [world id]
    zones createfromchunk [zone name]
    zones createfromchunk [zone name] [world id] [atX] [atZ]
    zones addto [zone name] chunk
    zones addto [zone name] chunk [atX] [atZ]
    zones addto [zone name] [fromX] [fromZ] [toX] [toZ]
    zones addto [zone name] [fromX] [fromY] [fromZ] [toX] [toY] [toZ]
    zones removefrom [zone name] chunk
    zones removefrom [zone name] chunk [atX] [atZ]
    zones removefrom [zone name] [fromX] [fromZ] [toX] [toZ]
    zones removefrom [zone name] [fromX] [fromY] [fromZ] [toX] [toY] [toZ]
    zones rename [old zone name] [new zone name]
    zones delete [zone name]
    zones help

     */

    private ZonesCommandHandler()
    {}

    private static final int cacheTimeoutInSeconds = 15;
    private static final String dontHavePermission = "You don't have permission to do that.";
    private static final String noSuggestionsSuggestion = "(No suggestions)";

    private static final Supplier<List<String>> cachedZoneNames
            = Suppliers.memoizeWithExpiration(Zones::getZoneNames,
                                              cacheTimeoutInSeconds,
                                              TimeUnit.SECONDS);

    private static final LoadingCache<CommandContext<CommandSource>, Boolean> cachedHasReadPermissions
            = CacheBuilder.newBuilder()
                          .maximumSize(1000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<CommandContext<CommandSource>, Boolean>()
    {
        @Override @ParametersAreNonnullByDefault
        public Boolean load(CommandContext<CommandSource> key)
        { return Permissions.commandSourceHasPermission(key, NintiCore.PERMISSION_ZONES_READ); }
    });

    private static final SuggestionProvider<CommandSource> worldIdSuggestionProvider
            = (context, builder) ->
    {
        for(ServerWorld world : getServer().getWorlds())
            builder.suggest(getWorldId(world));

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> existingZoneNameSuggestionProvider
            = (context, builder) ->
    {
        if(!cachedHasReadPermissions.getUnchecked(context))
        {
            builder.suggest(noSuggestionsSuggestion);
            return builder.buildFuture();
        }

        for(String zoneName : cachedZoneNames.get())
            builder.suggest(zoneName);

        return builder.buildFuture();
    };

    private static LiteralArgumentBuilder<CommandSource> getAtXYZSubcommand
    (
            Command<CommandSource> doWithXZ,
            Command<CommandSource> doWithXYZ
    )
    {
        return
        literal("at")
                .then(argument("coörd arg 1", IntegerArgumentType.integer())
                        .then(argument("coörd arg 2", IntegerArgumentType.integer())
                                .then(argument("coörd arg 3", IntegerArgumentType.integer())
                                        .executes(doWithXYZ))
                                .executes(doWithXZ)));
    }

    private static RequiredArgumentBuilder<CommandSource, String> getMutateZoneSubcommand
    (
            Command<CommandSource> doToArea2d,
            Command<CommandSource> doToArea3d,
            Command<CommandSource> doToDerivedChunk,
            Command<CommandSource> doToSpecifiedChunk
    )
    {
        return
        argument("zone name", StringArgumentType.word())
                .suggests(existingZoneNameSuggestionProvider)
                .then(literal("chunk")
                        .then(argument("at X", IntegerArgumentType.integer())
                                .then(argument("at Z", IntegerArgumentType.integer())
                                        .executes(doToSpecifiedChunk)))
                        .executes(doToDerivedChunk))
                .then(argument("coörd arg 1", IntegerArgumentType.integer())
                        .then(argument("coörd arg 2", IntegerArgumentType.integer())
                                .then(argument("coörd arg 3", IntegerArgumentType.integer())
                                        .then(argument("coörd arg 4", IntegerArgumentType.integer())
                                                .then(argument("coörd arg 5", IntegerArgumentType.integer())
                                                        .then(argument("coörd arg 6", IntegerArgumentType.integer())
                                                                .executes(doToArea3d)))
                                                .executes(doToArea2d)))));
    }

    private static boolean hasPerm(CommandSource src, String... perm)
    { return Permissions.commandSourceHasPermission(src, perm); }

    private static boolean hasAnyPermUnder(CommandSource src, String... perm)
    { return Permissions.commandSourceHasAnyPermissionUnder(src, perm); }

    public static final LiteralArgumentBuilder<CommandSource> zonesCommand
            = literal("zones")
                    .requires(src -> hasAnyPermUnder(src, NintiCore.PERMISSION_ZONES_ROOT))
                    .then(literal("save")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_FILEHANDLING_SAVE))
                            .executes(ZonesCommandHandler::cmdSave))
                    .then(literal("load")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_FILEHANDLING_LOAD))
                            .executes(ZonesCommandHandler::cmdLoad))
                    .then(literal("list")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_READ))
                            .then(literal("here")
                                    .executes(ZonesCommandHandler::cmdList_here))
                            .then(literal("in")
                                    .then(argument("world id", StringArgumentType.word())
                                            .suggests(worldIdSuggestionProvider)
                                            .then(getAtXYZSubcommand(ZonesCommandHandler::cmdList_inSpecifiedWorld_xz,
                                                                     ZonesCommandHandler::cmdList_inSpecifiedWorld_xyz))))
                            .then(getAtXYZSubcommand(ZonesCommandHandler::cmdList_inDerivedWorld_xz,
                                                     ZonesCommandHandler::cmdList_inDerivedWorld_xyz))
                            .executes(ZonesCommandHandler::cmdList))
                    .then(literal("create")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_CREATE))
                            .then(argument("zone name", StringArgumentType.word())
                                    .then(argument("world id", StringArgumentType.word())
                                            .suggests(worldIdSuggestionProvider)
                                            .executes(ZonesCommandHandler::cmdCreate_specifiedWorld))
                                    .executes(ZonesCommandHandler::cmdCreate_derivedWorld)))
                    .then(literal("createfromchunk")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_CREATE,
                                                          NintiCore.PERMISSION_ZONES_WRITE_MODIFY_ADDTO))
                            .then(argument("zone name", StringArgumentType.word())
                                    .then(argument("world id", StringArgumentType.word())
                                            .suggests(worldIdSuggestionProvider)
                                            .then(argument("at X", IntegerArgumentType.integer())
                                                    .then(argument("at Z", IntegerArgumentType.integer())
                                                            .executes(ZonesCommandHandler::cmdCreateFromChunk_specified))))
                                    .executes(ZonesCommandHandler::cmdCreateFromChunk_derived)))
                    .then(literal("addto")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_MODIFY_ADDTO))
                            .then(getMutateZoneSubcommand(
                                    ZonesCommandHandler::cmdAddTo_area_2d,
                                    ZonesCommandHandler::cmdAddTo_area_3d,
                                    ZonesCommandHandler::cmdAddTo_chunk_derived,
                                    ZonesCommandHandler::cmdAddTo_chunk_specified)))
                    .then(literal("removefrom")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_MODIFY_REMOVEFROM))
                            .then(getMutateZoneSubcommand(
                                    ZonesCommandHandler::cmdRemoveFrom_area_2d,
                                    ZonesCommandHandler::cmdRemoveFrom_area_3d,
                                    ZonesCommandHandler::cmdRemoveFrom_chunk_derived,
                                    ZonesCommandHandler::cmdRemoveFrom_chunk_specified)))
                    .then(literal("rename")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_RENAME))
                            .then(argument("zone name", StringArgumentType.word())
                                    .suggests(existingZoneNameSuggestionProvider)
                                    .then(argument("new zone name", StringArgumentType.word())
                                            .executes(ZonesCommandHandler::cmdRename))))
                    .then(literal("delete")
                            .requires(src -> hasPerm(src, NintiCore.PERMISSION_ZONES_WRITE_DELETE))
                            .then(argument("zone name", StringArgumentType.word())
                                    .suggests(existingZoneNameSuggestionProvider)
                                    .executes(ZonesCommandHandler::cmdDelete)))
                    .then(literal("help")
                            .executes(ZonesCommandHandler::cmdHelp))
                    .executes(ZonesCommandHandler::cmdHelp);

    private static int cmdSave(CommandContext<CommandSource> cmdContext)
    {
        Zones.save();
        return 1;
    }

    private static int cmdLoad(CommandContext<CommandSource> cmdContext)
    {
        Zones.load();
        return 1;
    }

    private static int cmdList(CommandContext<CommandSource> cmdContext)
    {
        sendMessage(cmdContext, "Zones: ");

        for(String zn : Zones.getZoneNames())
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdList_here(CommandContext<CommandSource> cmdContext)
    {
        Entity sourceEntity = cmdContext.getSource().getEntity();

        if(sourceEntity == null)
        {
            sendMessage(cmdContext, "Where is \"here\"?");
            return 1;
        }

        sendMessage(cmdContext, "Zones at your location: ");

        for(String zn : Zones.getZoneNamesEntityIsIn(sourceEntity))
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdList_inDerivedWorld_xz(CommandContext<CommandSource> cmdContext)
    {
        Entity sourceEntity = cmdContext.getSource().getEntity();
        String worldId = sourceEntity == null ? getDefaultWorldId()
                                              : getWorldId((ServerWorld)sourceEntity.getEntityWorld());
        int x = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int z = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");

        sendMessage(cmdContext, "Zones at " + x + ", " + z + " in " + worldId + ": ");

        for(String zn : Zones.getZoneNamesAt(x, z))
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdList_inDerivedWorld_xyz(CommandContext<CommandSource> cmdContext)
    {
        Entity sourceEntity = cmdContext.getSource().getEntity();
        String worldId = sourceEntity == null ? getDefaultWorldId()
                                              : getWorldId((ServerWorld)sourceEntity.getEntityWorld());
        int x = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int y = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int z = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");

        sendMessage(cmdContext, "Zones at " + x + ", " + y + ", " + z + " in " + worldId + ": ");

        for(String zn : Zones.getZoneNamesAt(x, y, z))
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdList_inSpecifiedWorld_xz(CommandContext<CommandSource> cmdContext)
    {
        String worldId = StringArgumentType.getString(cmdContext, "world id");
        int x = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int z = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");

        sendMessage(cmdContext, "Zones at " + x + ", " + z + " in " + worldId + ": ");

        for(String zn : Zones.getZoneNamesAt(x, z))
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdList_inSpecifiedWorld_xyz(CommandContext<CommandSource> cmdContext)
    {
        String worldId = StringArgumentType.getString(cmdContext, "world id");
        int x = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int y = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int z = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");

        sendMessage(cmdContext, "Zones at " + x + ", " + y + ", " + z + " in " + worldId + ": ");

        for(String zn : Zones.getZoneNamesAt(x, y, z))
            sendMessage(cmdContext, " - " + zn);

        return 1;
    }

    private static int cmdCreate_derivedWorld(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");

        Entity sourceEntity = cmdContext.getSource().getEntity();
        String impliedWorldId = sourceEntity != null
                                        ? getWorldId((ServerWorld)sourceEntity.getEntityWorld())
                                        : getDefaultWorldId();

        Zones.register(new Zone(zoneName, impliedWorldId));
        return 1;
    }

    private static int cmdCreate_specifiedWorld(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        String worldId = StringArgumentType.getString(cmdContext, "world id");
        Zones.register(new Zone(zoneName, worldId));
        return 1;
    }

    private static int cmdCreateFromChunk_derived(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        Entity sourceEntity = cmdContext.getSource().getEntity();

        if(sourceEntity == null)
        {
            sendMessage(cmdContext, "A chunk can only be derived from you if you're an entity in the world. Please "
                                    + "specify a chunk.");
            return 1;
        }

        String worldId = getWorldId((ServerWorld)(sourceEntity.getEntityWorld()));
        Zone zone = new Zone(zoneName, worldId);
        zone.addRegion(Zone.ZoneRegionRectangle.ofEntitysChunk(sourceEntity));
        Zones.register(zone);
        return 1;
    }

    private static int cmdCreateFromChunk_specified(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        String worldId = StringArgumentType.getString(cmdContext, "world id");
        int atX = IntegerArgumentType.getInteger(cmdContext, "at X");
        int atZ = IntegerArgumentType.getInteger(cmdContext, "at Z");
        Zone zone = new Zone(zoneName, worldId);
        zone.addRegion(Zone.ZoneRegionRectangle.ofChunkAt(atX, atZ));
        Zones.register(zone);
        return 1;
    }

    private static int cmdAddTo_area_2d(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int fromX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int fromZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int toX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");
        int toZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 4");
        Zone.ZoneRegionRectangle region = new Zone.ZoneRegionRectangle(fromX, fromZ, toX, toZ);

        if(!Zones.addToZoneIfThere(zoneName, region))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdAddTo_area_3d(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int fromX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int fromY = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int fromZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");
        int toX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 4");
        int toY = IntegerArgumentType.getInteger(cmdContext, "coörd arg 5");
        int toZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 6");
        Zone.ZoneRegionCuboid region = new Zone.ZoneRegionCuboid(fromX, fromY, fromZ, toX, toY, toZ);

        if(!Zones.addToZoneIfThere(zoneName, region))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdAddTo_chunk_derived(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        Entity sourceEntity = cmdContext.getSource().getEntity();

        if(sourceEntity == null)
        {
            sendMessage(cmdContext, "A chunk can only be derived from you if you're an entity in the world. Please "
                                    + "specify a chunk.");
            return 1;
        }

        Zone existingZone = Zones.get(zoneName);

        if(existingZone == null)
        {
            sendMessage(cmdContext, "No zone found by the name " + zoneName);
            return 1;
        }

        String worldId = getWorldId((ServerWorld)(sourceEntity.getEntityWorld()));

        if(!existingZone.getWorldId().equals(worldId))
        {
            sendMessage(cmdContext, "Chunk is not in the same world as the zone.");
            return 1;
        }

        if(!Zones.addToZoneIfThere(zoneName, Zone.ZoneRegionRectangle.ofEntitysChunk(sourceEntity)))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdAddTo_chunk_specified(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int atX = IntegerArgumentType.getInteger(cmdContext, "at X");
        int atZ = IntegerArgumentType.getInteger(cmdContext, "at Z");

        if(!Zones.addToZoneIfThere(zoneName, Zone.ZoneRegionRectangle.ofChunkAt(atX, atZ)))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdRemoveFrom_area_2d(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int fromX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int fromZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int toX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");
        int toZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 4");
        Zone.ZoneRegionRectangle region = new Zone.ZoneRegionRectangle(fromX, fromZ, toX, toZ).negating();

        if(!Zones.addToZoneIfThere(zoneName, region))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdRemoveFrom_area_3d(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int fromX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 1");
        int fromY = IntegerArgumentType.getInteger(cmdContext, "coörd arg 2");
        int fromZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 3");
        int toX = IntegerArgumentType.getInteger(cmdContext, "coörd arg 4");
        int toY = IntegerArgumentType.getInteger(cmdContext, "coörd arg 5");
        int toZ = IntegerArgumentType.getInteger(cmdContext, "coörd arg 6");
        Zone.ZoneRegionCuboid region = new Zone.ZoneRegionCuboid(fromX, fromY, fromZ, toX, toY, toZ).negating();

        if(!Zones.addToZoneIfThere(zoneName, region))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdRemoveFrom_chunk_derived(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        Entity sourceEntity = cmdContext.getSource().getEntity();

        if(sourceEntity == null)
        {
            sendMessage(cmdContext, "A chunk can only be derived from you if you're an entity in the world. Please "
                                    + "specify a chunk.");
            return 1;
        }

        Zone existingZone = Zones.get(zoneName);

        if(existingZone == null)
        {
            sendMessage(cmdContext, "No zone found by the name " + zoneName);
            return 1;
        }

        String worldId = getWorldId((ServerWorld)(sourceEntity.getEntityWorld()));

        if(!existingZone.getWorldId().equals(worldId))
        {
            sendMessage(cmdContext, "Chunk is not in the same world as the zone.");
            return 1;
        }

        if(!Zones.addToZoneIfThere(zoneName, Zone.ZoneRegionRectangle.ofEntitysChunk(sourceEntity).negating()))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdRemoveFrom_chunk_specified(CommandContext<CommandSource> cmdContext)
    {
        String zoneName = StringArgumentType.getString(cmdContext, "zone name");
        int atX = IntegerArgumentType.getInteger(cmdContext, "at X");
        int atZ = IntegerArgumentType.getInteger(cmdContext, "at Z");

        if(!Zones.addToZoneIfThere(zoneName, Zone.ZoneRegionRectangle.ofChunkAt(atX, atZ).negating()))
            sendMessage(cmdContext, "No zone found by the name " + zoneName);

        return 1;
    }

    private static int cmdRename(CommandContext<CommandSource> cmdContext)
    {
        String oldZoneName = StringArgumentType.getString(cmdContext, "zone name");
        String newZoneName = StringArgumentType.getString(cmdContext, "new zone name");

        if(!Zones.rename(oldZoneName, newZoneName))
            sendMessage(cmdContext, "No zone found by the name " + oldZoneName);

        return 1;
    }

    private static int cmdDelete(CommandContext<CommandSource> cmdContext)
    {
        String oldZoneName = StringArgumentType.getString(cmdContext, "zone name");

        if(!Zones.deregister(oldZoneName))
            sendMessage(cmdContext, "No zone found by the name " + oldZoneName);

        return 1;
    }

    private static int cmdHelp(CommandContext<CommandSource> cmdContext)
    {
        String clickHereText = "Click here to go to the github page for this mod.";
        String helpUrl = "https://github.com/c-massie/NintiCore";

        Style s = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, helpUrl));
        ITextComponent msg = new StringTextComponent(clickHereText).setStyle(s);
        cmdContext.getSource().sendFeedback(msg, true);
        return 1;
    }
}
