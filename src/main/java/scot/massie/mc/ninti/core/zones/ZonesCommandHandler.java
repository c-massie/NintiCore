package scot.massie.mc.ninti.core.zones;

import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.server.ServerWorld;
import scot.massie.mc.ninti.core.NintiCore;
import scot.massie.mc.ninti.core.Permissions;
import scot.massie.mc.ninti.core.StaticUtilFunctions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static net.minecraft.command.Commands.*;

public class ZonesCommandHandler
{
    /*

    zones save
    zones load
    zones list
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
        @Override
        public Boolean load(CommandContext<CommandSource> key) throws Exception
        { return Permissions.commandSourceHasPermission(key, NintiCore.PERMISSION_ZONES_READ); }
    });

    private static final SuggestionProvider<CommandSource> worldIdSuggestionProvider
            = (context, builder) ->
    {
        for(ServerWorld world : StaticUtilFunctions.getServer().getWorlds())
            builder.suggest(StaticUtilFunctions.getWorldId(world));

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

    private static RequiredArgumentBuilder<CommandSource, String> getMutateZoneSubCommand
    (
            ToIntFunction<CommandContext<CommandSource>> doToArea2d,
            ToIntFunction<CommandContext<CommandSource>> doToArea3d,
            ToIntFunction<CommandContext<CommandSource>> doToDerivedChunk,
            ToIntFunction<CommandContext<CommandSource>> doToSpecifiedChunk
    )
    {
        return
        argument("zone name", StringArgumentType.word())
                .suggests(existingZoneNameSuggestionProvider)
                .then(literal("chunk")
                        .then(argument("at X", IntegerArgumentType.integer())
                                .then(argument("at Z", IntegerArgumentType.integer())
                                        .executes(doToSpecifiedChunk::applyAsInt)))
                        .executes(doToDerivedChunk::applyAsInt))
                .then(argument("coörd arg 1", IntegerArgumentType.integer())
                        .then(argument("coörd arg 2", IntegerArgumentType.integer())
                                .then(argument("coörd arg 3", IntegerArgumentType.integer())
                                        .then(argument("coörd arg 4", IntegerArgumentType.integer())
                                                .then(argument("coörd arg 5", IntegerArgumentType.integer())
                                                        .then(argument("coörd arg 6", IntegerArgumentType.integer())
                                                                .executes(doToArea3d::applyAsInt)))
                                                .executes(doToArea2d::applyAsInt)))));
    }

    public static final LiteralArgumentBuilder<CommandSource> zonesCommand
            = literal("zones")
                    .then(literal("save").executes(ZonesCommandHandler::cmdSave))
                    .then(literal("load").executes(ZonesCommandHandler::cmdLoad))
                    .then(literal("create")
                            .then(argument("zone name", StringArgumentType.word())
                                    .then(argument("world id", StringArgumentType.word())
                                            .suggests(worldIdSuggestionProvider)
                                            .executes(ZonesCommandHandler::cmdCreate_specifiedWorld))
                                    .executes(ZonesCommandHandler::cmdCreate_derivedWorld)))
                    .then(literal("createfromchunk")
                            .then(argument("zone name", StringArgumentType.word())
                                    .then(argument("world id", StringArgumentType.word())
                                            .suggests(worldIdSuggestionProvider)
                                            .then(argument("at X", IntegerArgumentType.integer())
                                                    .then(argument("at Z", IntegerArgumentType.integer())
                                                            .executes(ZonesCommandHandler::cmdCreateFromChunk_specified))))
                                    .executes(ZonesCommandHandler::cmdCreateFromChunk_derived)))
                    .then(literal("addto")
                            .then(getMutateZoneSubCommand(
                                    ZonesCommandHandler::cmdAddTo_area_2d,
                                    ZonesCommandHandler::cmdAddTo_area_3d,
                                    ZonesCommandHandler::cmdAddTo_chunk_derived,
                                    ZonesCommandHandler::cmdAddTo_chunk_specified)))
                    .then(literal("removefrom")
                            .then(getMutateZoneSubCommand(
                                    ZonesCommandHandler::cmdRemoveFrom_area_2d,
                                    ZonesCommandHandler::cmdRemoveFrom_area_3d,
                                    ZonesCommandHandler::cmdRemoveFrom_chunk_derived,
                                    ZonesCommandHandler::cmdRemoveFrom_chunk_specified)))
                    .then(literal("rename")
                            .then(argument("zone name", StringArgumentType.word())
                                    .suggests(existingZoneNameSuggestionProvider)
                                    .then(argument("new zone name", StringArgumentType.word())
                                            .executes(ZonesCommandHandler::cmdRename))))
                    .then(literal("delete")
                            .then(argument("zone name", StringArgumentType.word())
                                    .suggests(existingZoneNameSuggestionProvider)
                                    .executes(ZonesCommandHandler::cmdDelete)))
                    .then(literal("help")
                            .executes(ZonesCommandHandler::cmdHelp))
                    .executes(ZonesCommandHandler::cmdHelp);

    private static int cmdSave(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdLoad(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdList(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdCreate_derivedWorld(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdCreate_specifiedWorld(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdCreateFromChunk_derived(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdCreateFromChunk_specified(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdAddTo_area_2d(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdAddTo_area_3d(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdAddTo_chunk_derived(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdAddTo_chunk_specified(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdRemoveFrom_area_2d(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdRemoveFrom_area_3d(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdRemoveFrom_chunk_derived(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdRemoveFrom_chunk_specified(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdRename(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdDelete(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }

    private static int cmdHelp(CommandContext<CommandSource> cmdContext)
    { throw new UnsupportedOperationException("Not implemented yet."); }
}
