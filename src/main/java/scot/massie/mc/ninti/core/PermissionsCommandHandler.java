package scot.massie.mc.ninti.core;

import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import joptsimple.internal.Strings;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.UsernameCache;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static scot.massie.mc.ninti.core.PluginUtils.getLastKnownUUIDOfPlayer;
import static scot.massie.mc.ninti.core.PluginUtils.sendMessage;

public final class PermissionsCommandHandler
{
    private static final class TargetReferenced
    {
        public TargetReferenced(String targetAsString)
        {
            if(targetAsString.startsWith("#"))
            {
                groupName = targetAsString.substring(1);
                playerName = null;
                playerId = null;
                return;
            }

            groupName = null;
            UUID asUUID;

            try
            { asUUID = UUID.fromString(targetAsString); }
            catch(IllegalArgumentException e)
            { asUUID = null; }

            if(asUUID != null)
            {
                playerName = UsernameCache.getLastKnownUsername(asUUID);
                playerId = asUUID;
                return;
            }

            playerName = targetAsString;
            playerId = getLastKnownUUIDOfPlayer(playerName);
        }

        private final String groupName;
        private final String playerName;
        private final UUID playerId;

        public boolean isForPlayer()
        { return groupName == null; }

        public boolean hasPlayerName()
        { return playerName != null; }

        public boolean hasPlayerId()
        { return playerId != null; }

        public String getPlayerName()
        { return playerName; }

        public UUID getPlayerId()
        { return playerId; }

        public boolean isForGroup()
        { return groupName != null; }

        public String getGroupName()
        { return groupName; }
    }

    /*
    permissions save
    permissions load
    permissions initialise blank
    permissions initialise presets
    permissions list [playername or group id]
    permissions listgroups
    permissions add [playername or group id] [permission as string]
    permissions remove [playername or group id] [permission as string]
    permissions has [playername or group id] [permission as string]
    permissions help
     */

    private PermissionsCommandHandler()
    {}

    private static final int cacheTimeoutInSeconds = 15;
    private static final String dontHavePermission = "You don't have permission to do that.";
    private static final String noSuggestionsSuggestion = "(No suggestions)";

    //region Suggestion provider caches
    static LoadingCache<UUID, List<String>> cachedSuggestionsToAddToPlayers
            = CacheBuilder.newBuilder()
                          .maximumSize(10000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<UUID, List<String>>()
    {
        @Override @ParametersAreNonnullByDefault
        public List<String> load(UUID key)
        {
            List<String> suggestions = new ArrayList<>();

            for(String groupName : Permissions.getGroupNames())
                if(!Permissions.playerIsInGroup(key, groupName))
                    suggestions.add("#" + groupName);

            for(String suggestion : Permissions.Suggestions.get())
                if(!Permissions.playerHasPermission(key, suggestion))
                    suggestions.add(suggestion);

            return suggestions;
        }
    });

    static LoadingCache<String, List<String>> cachedSuggestionsToAddToGroups
            = CacheBuilder.newBuilder()
                          .maximumSize(10000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<String, List<String>>()
    {
        @Override @ParametersAreNonnullByDefault
        public List<String> load(String key) throws Exception
        {
            List<String> result = new ArrayList<>();

            for(String presetName : Permissions.Presets.getPresetNames())
                result.add("@" + presetName);

            for(String groupName : Permissions.getGroupNames())
                if(!Permissions.groupIsInGroup(key, groupName))
                    result.add("#" + groupName);

            for(String perm : Permissions.Suggestions.get())
                if(!Permissions.groupHasPermission(key, perm))
                    result.add(perm);

            return result;
        }
    });

    static LoadingCache<UUID, List<String>> cachedSuggestionsToRemoveFromPlayers
            = CacheBuilder.newBuilder()
                          .maximumSize(10000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<UUID, List<String>>()
    {
        @Override @ParametersAreNonnullByDefault
        public List<String> load(UUID key)
        { return Permissions.getGroupsAndPermissionsOfPlayer(key); }
    });

    static LoadingCache<String, List<String>> cachedSuggestionsToRemoveFromGroups
            = CacheBuilder.newBuilder()
                          .maximumSize(10000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<String, List<String>>()
    {
        @Override @ParametersAreNonnullByDefault
        public List<String> load(String key)
        { return Permissions.getGroupsAndPermissionsOfGroup(key); }
    });

    static LoadingCache<CommandContext<CommandSource>, Boolean> cachedHasReadPermissions
            = CacheBuilder.newBuilder()
                          .maximumSize(10000)
                          .expireAfterWrite(cacheTimeoutInSeconds, TimeUnit.SECONDS)
                          .build(new CacheLoader<CommandContext<CommandSource>, Boolean>()
    {
        @Override @ParametersAreNonnullByDefault
        public Boolean load(CommandContext<CommandSource> key) throws Exception
        {
            if(!(key.getSource().getEntity() instanceof PlayerEntity))
                return true;

            PlayerEntity player = key.getSource().asPlayer();

            return Permissions.playerHasPermission(player, NintiCore.PERMISSION_PERMISSIONS_READ_GROUPS)
                && Permissions.playerHasPermission(player, NintiCore.PERMISSION_PERMISSIONS_READ_PLAYERS);
        }
    });

    static Supplier<List<String>> cachedSuggestionsToSuggest
            = Suppliers.memoizeWithExpiration(Permissions::getGroupNamesAndSuggestedPermissions,
                                              cacheTimeoutInSeconds,
                                              TimeUnit.SECONDS);
    //endregion

    //region Suggestion providers
    private static final SuggestionProvider<CommandSource> playerNameOrGroupIdSuggestionProvider
            = (context, builder) ->
    {
        if(!cachedHasReadPermissions.getUnchecked(context))
            return builder.suggest(noSuggestionsSuggestion).buildFuture();

        boolean anySuggestions = false;

        for(String groupName : Permissions.getGroupNames())
        {
            builder.suggest("#" + groupName);
            anySuggestions = true;
        }

        for(String userName : UsernameCache.getMap().values())
        {
            anySuggestions = true;
            builder.suggest(userName);
        }

        if(!anySuggestions)
            builder.suggest(noSuggestionsSuggestion);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> suggestedPermissionsToAddProvider
            = (context, builder) ->
    {
        if(!cachedHasReadPermissions.getUnchecked(context))
            return builder.suggest(noSuggestionsSuggestion).buildFuture();

        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(context, "target"));
        boolean anySuggestions = false;

        if(targ.isForGroup())
        {
            for(String suggestion : cachedSuggestionsToAddToGroups.getUnchecked(targ.getGroupName()))
                if(suggestion.startsWith(builder.getRemaining()))
                {
                    anySuggestions = true;
                    builder.suggest(suggestion);
                }
        }
        else if(targ.hasPlayerId())
        {
            for(String suggestion : cachedSuggestionsToAddToPlayers.getUnchecked(targ.getPlayerId()))
                if(suggestion.startsWith(builder.getRemaining()))
                {
                    anySuggestions = true;
                    builder.suggest(suggestion);
                }
        }

        if(!anySuggestions)
            builder.suggest(noSuggestionsSuggestion);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> suggestedPermissionsToRemoveProvider
            = (context, builder) ->
    {
        if(!cachedHasReadPermissions.getUnchecked(context))
            return builder.suggest(noSuggestionsSuggestion).buildFuture();

        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(context, "target"));
        boolean anySuggestions = false;

        if(targ.isForGroup())
        {
            for(String suggestion : cachedSuggestionsToRemoveFromGroups.getUnchecked(targ.getGroupName()))
                if(suggestion.startsWith(builder.getRemaining()))
                {
                    anySuggestions = true;
                    builder.suggest(suggestion);
                }
        }
        else if(targ.hasPlayerId())
        {
            for(String suggestion : cachedSuggestionsToRemoveFromPlayers.getUnchecked(targ.getPlayerId()))
                if(suggestion.startsWith(builder.getRemaining()))
                {
                    anySuggestions = true;
                    builder.suggest(suggestion);
                }
        }

        if(!anySuggestions)
            builder.suggest(noSuggestionsSuggestion);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> suggestedPermissionsProvider
            = (context, builder) ->
    {
        if(!cachedHasReadPermissions.getUnchecked(context))
            return builder.suggest(noSuggestionsSuggestion).buildFuture();

        boolean anySuggestions = false;

        for(String suggestion : cachedSuggestionsToSuggest.get())
            if(suggestion.startsWith(builder.getRemaining()))
            {
                anySuggestions = true;
                builder.suggest(suggestion);
            }

        if(!anySuggestions)
            builder.suggest(noSuggestionsSuggestion);

        return builder.buildFuture();
    };
    //endregion

    //region public static final LiteralArgumentBuilder<CommandSource> permissionCommand = ...
    public static final LiteralArgumentBuilder<CommandSource> permissionCommand
            = Commands.literal("permissions")
                      .then(Commands.literal("save").executes(PermissionsCommandHandler::cmdSave))
                      .then(Commands.literal("load").executes(PermissionsCommandHandler::cmdLoad))
                      .then(Commands.literal("initialise")
                                    .then(Commands.literal("blank")
                                                  .executes(PermissionsCommandHandler::cmdInitialiseBlank))
                                    .then(Commands.literal("presets")
                                                  .executes(PermissionsCommandHandler::cmdInitialisePresets)))
                      .then(Commands.literal("list")
                                    .then(Commands.argument("target", StringArgumentType.word())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .executes(PermissionsCommandHandler::cmdList)))
                      .then(Commands.literal("listgroups").executes(PermissionsCommandHandler::cmdListGroups))
                      .then(Commands.literal("add")
                                    .then(Commands.argument("target", StringArgumentType.word())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to add", MessageArgument.message())
                                                                .suggests(suggestedPermissionsToAddProvider)
                                                                .executes(PermissionsCommandHandler::cmdAdd))))
                      .then(Commands.literal("remove")
                                    .then(Commands.argument("target", StringArgumentType.word())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to remove", MessageArgument.message())
                                                                .suggests(suggestedPermissionsToRemoveProvider)
                                                                .executes(PermissionsCommandHandler::cmdRemove))))
                      .then(Commands.literal("has")
                                    .then(Commands.argument("target", StringArgumentType.word())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to check", MessageArgument.message())
                                                                .suggests(suggestedPermissionsProvider)
                                                                .executes(PermissionsCommandHandler::cmdHas))))
                      .then(Commands.literal("help").executes(PermissionsCommandHandler::cmdHelp))
                      .executes(PermissionsCommandHandler::cmdHelp);
    //endregion
    
    private static boolean sourceHasPermission(CommandContext<CommandSource> commandContext, String... permissions)
    {
        Entity sourceEntity = commandContext.getSource().getEntity();

        if(!(sourceEntity instanceof PlayerEntity))
            return true;

        for(int i = 0; i < permissions.length; i++)
            if(!Permissions.playerHasPermission((PlayerEntity)sourceEntity, permissions[i]))
                return false;

        return true;
    }

    private static int cmdSave(CommandContext<CommandSource> commandContext)
    {
        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_FILEHANDLING_SAVE))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        Permissions.save();
        return 1;
    }

    private static int cmdLoad(CommandContext<CommandSource> commandContext)
    {
        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_FILEHANDLING_LOAD))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        Permissions.load();
        return 1;
    }

    private static int cmdInitialiseBlank(CommandContext<CommandSource> commandContext)
    {
        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_GROUPS, NintiCore.PERMISSION_PERMISSIONS_WRITE_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        Permissions.Write.clear();
        return 1;
    }

    private static int cmdInitialisePresets(CommandContext<CommandSource> commandContext)
    {
        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_GROUPS, NintiCore.PERMISSION_PERMISSIONS_WRITE_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        Permissions.Write.initialisePermissionsWithPresets();
        return 1;
    }

    private static int cmdList(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(commandContext, "target"));

        if(targ.isForGroup())
        {
            if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_READ_GROUPS))
            {
                sendMessage(commandContext, dontHavePermission);
                return 0;
            }

            String resultHeader = "Permissions for group " + targ.getGroupName() + ": ";
            String groupPerms = Strings.join(Permissions.getPermissionsOfGroup(targ.getGroupName()), "\n");
            sendMessage(commandContext, resultHeader + "\n" + groupPerms);
            return 1;
        }

        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_READ_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        if(targ.hasPlayerId())
        {
            String playerPerms = Strings.join(Permissions.getPermissionsOfPlayer(targ.getPlayerId()), "\n");
            String playerIdentification = (targ.hasPlayerName())
                                                  ? ("with the UUID " + targ.getPlayerId().toString())
                                                  : (targ.getPlayerName());

            sendMessage(commandContext, "Permissions for player " + playerIdentification + ":\n" + playerPerms);
            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdListGroups(CommandContext<CommandSource> commandContext)
    {
        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_READ_GROUPS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        sendMessage(commandContext, "Groups:\n" + Strings.join(Permissions.getGroupNames(), "\n"));
        return 1;
    }

    private static int cmdAdd(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(commandContext, "target"));
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to add").getString();

        if(targ.isForGroup())
        {
            if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_GROUPS))
            {
                sendMessage(commandContext, dontHavePermission);
                return 0;
            }

            Permissions.Write.assignGroupPermission(targ.getGroupName(), permissionAsString);
            return 1;
        }

        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        if(targ.hasPlayerId())
        {
            Permissions.Write.assignPlayerPermission(targ.getPlayerId(), permissionAsString);
            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdRemove(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(commandContext, "target"));
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to remove").getString();

        if(targ.isForGroup())
        {
            if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_GROUPS))
            {
                sendMessage(commandContext, dontHavePermission);
                return 0;
            }

            Permissions.Write.revokeGroupPermission(targ.getGroupName(), permissionAsString);
            return 1;
        }

        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_WRITE_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        if(targ.hasPlayerId())
        {
            Permissions.Write.revokePlayerPermission(targ.getPlayerId(), permissionAsString);
            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdHas(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(StringArgumentType.getString(commandContext, "target"));
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to check").getString();

        if(targ.isForGroup())
        {
            if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_READ_GROUPS))
            {
                sendMessage(commandContext, dontHavePermission);
                return 0;
            }

            boolean has = Permissions.groupHasPermission(targ.getGroupName(), permissionAsString);

            sendMessage(commandContext, "The group " + targ.getGroupName()
                                        + (has ? " has" : " does not have")
                                        + " the permission: " + permissionAsString);
            return 1;
        }

        if(!sourceHasPermission(commandContext, NintiCore.PERMISSION_PERMISSIONS_READ_PLAYERS))
        {
            sendMessage(commandContext, dontHavePermission);
            return 0;
        }

        if(targ.hasPlayerId())
        {
            boolean has = Permissions.playerHasPermission(targ.getPlayerId(), permissionAsString);

            if(targ.hasPlayerName())
            {
                sendMessage(commandContext, "The player " + targ.getPlayerName()
                                            + (has ? " has" : " does not have")
                                            + " the permission: " + permissionAsString);
            }
            else
            {
                sendMessage(commandContext, "The player with the ID " + targ.getPlayerId()
                                            + (has ? " has" : " does not have")
                                            + " the permission: " + permissionAsString);
            }

            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdHelp(CommandContext<CommandSource> commandContext)
    {
        String clickHereText = "Click here to go to the github page for this mod.";
        String helpUrl = "https://github.com/c-massie/NintiCore";

        Style s = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, helpUrl));
        ITextComponent msg = new StringTextComponent(clickHereText).setStyle(s);
        commandContext.getSource().sendFeedback(msg, true);
        return 1;
    }
}
