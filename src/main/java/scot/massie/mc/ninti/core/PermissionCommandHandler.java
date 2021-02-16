package scot.massie.mc.ninti.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import joptsimple.internal.Strings;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.MessageArgument;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.UsernameCache;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static scot.massie.mc.ninti.core.StaticUtilFunctions.*;

public class PermissionCommandHandler
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
    permissions list [playername or group id]
    permissions listgroups
    permissions add [playername or group id] [permission as string]
    permissions remove [playername or group id] [permission as string]
    permissions has [playername or group id] [permission as string]
    permissions help
     */

    private static final SuggestionProvider<CommandSource> playerNameOrGroupIdSuggestionProvider
            = (context, builder) ->
    {
        for(String groupName : Permissions.getGroupNames())
            builder.suggest("#" + groupName);

        for(String userName : UsernameCache.getMap().values())
            builder.suggest(userName);

        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> permissionsCurrentlyHasSuggestionProvider
            = (context, builder) ->
    {
        TargetReferenced targ = new TargetReferenced(MessageArgument.getMessage(context, "target").getString());

        if(targ.isForGroup())
            for(String s : Permissions.getGroupsAndPermissionsOfGroup(targ.getGroupName()))
                builder.suggest(s);
        else if(targ.hasPlayerId())
            for(String s : Permissions.getGroupsAndPermissionsOfPlayer(targ.getPlayerId()))
                builder.suggest(s);

        return builder.buildFuture();
    };

    public static final LiteralArgumentBuilder<CommandSource> permissionCommand
            = Commands.literal("permission")
                      .then(Commands.literal("save").executes(PermissionCommandHandler::cmdSave))
                      .then(Commands.literal("load").executes(PermissionCommandHandler::cmdLoad))
                      .then(Commands.literal("list")
                                    .then(Commands.argument("target", MessageArgument.message())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .executes(PermissionCommandHandler::cmdList)))
                      .then(Commands.literal("listgroups").executes(PermissionCommandHandler::cmdListGroups))
                      .then(Commands.literal("add")
                                    .then(Commands.argument("target", MessageArgument.message())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to add", MessageArgument.message())
                                                                .executes(PermissionCommandHandler::cmdAdd))))
                      .then(Commands.literal("remove")
                                    .then(Commands.argument("target", MessageArgument.message())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to remove", MessageArgument.message())
                                                                .suggests(permissionsCurrentlyHasSuggestionProvider)
                                                                .executes(PermissionCommandHandler::cmdRemove))))
                      .then(Commands.literal("has")
                                    .then(Commands.argument("target", MessageArgument.message())
                                                  .suggests(playerNameOrGroupIdSuggestionProvider)
                                                  .then(Commands.argument("permission to check", MessageArgument.message())
                                                                .executes(PermissionCommandHandler::cmdHas))))
                      .then(Commands.literal("help").executes(PermissionCommandHandler::cmdHelp))
                      .executes(PermissionCommandHandler::cmdHelp);

    private static int cmdSave(CommandContext<CommandSource> commandContext)
    {
        Permissions.savePermissions();
        return 1;
    }

    private static int cmdLoad(CommandContext<CommandSource> commandContext)
    {
        Permissions.loadPermissions();
        return 1;
    }

    private static int cmdList(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(MessageArgument.getMessage(commandContext, "target").getString());

        if(targ.isForGroup())
        {
            String resultHeader = "Permissions for group " + targ.getGroupName() + ": ";
            String groupPerms = Strings.join(Permissions.getPermissionsOfGroup(targ.getGroupName()), "\n");
            sendMessage(commandContext, resultHeader + "\n" + groupPerms);
            return 1;
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
        sendMessage(commandContext, "Groups:\n" + Strings.join(Permissions.getGroupNames(), "\n"));
        return 1;
    }

    private static int cmdAdd(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(MessageArgument.getMessage(commandContext, "target").getString());
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to add").getString();

        if(targ.isForGroup())
        {
            Permissions.assignGroupPermission(targ.getGroupName(), permissionAsString);
            return 1;
        }

        if(targ.hasPlayerId())
        {
            Permissions.assignPlayerPermission(targ.getPlayerId(), permissionAsString);
            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdRemove(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(MessageArgument.getMessage(commandContext, "target").getString());
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to remove").getString();

        if(targ.isForGroup())
        {
            Permissions.revokeGroupPermission(targ.getGroupName(), permissionAsString);
            return 1;
        }

        if(targ.hasPlayerId())
        {
            Permissions.revokePlayerPermission(targ.getPlayerId(), permissionAsString);
            return 1;
        }

        sendMessage(commandContext, "No player with name: " + targ.getPlayerName());
        return 1;
    }

    private static int cmdHas(CommandContext<CommandSource> commandContext) throws CommandSyntaxException
    {
        TargetReferenced targ = new TargetReferenced(MessageArgument.getMessage(commandContext, "target").getString());
        String permissionAsString = MessageArgument.getMessage(commandContext, "permission to check").getString();

        if(targ.isForGroup())
        {
            boolean has = Permissions.groupHasPermission(targ.getGroupName(), permissionAsString);

            sendMessage(commandContext, "The group " + targ.getGroupName()
                                        + (has ? " has" : " does not have")
                                        + " the permission: " + permissionAsString);
            return 1;
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
        String helpUrl = "https://github.com/c-massie/PermissionsSystem";

        Style s = Style.EMPTY.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, helpUrl));
        ITextComponent msg = new StringTextComponent(clickHereText).setStyle(s);
        commandContext.getSource().sendFeedback(msg, true);
        return 1;
    }
}
