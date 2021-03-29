package scot.massie.mc.ninti.core.currencies;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;

import java.util.UUID;

import static scot.massie.mc.ninti.core.PluginUtils.*;

public class Currency
{
    @FunctionalInterface
    public interface CurrencyChecker
    { boolean canAfford(UUID playerId, PlayerEntity player, double amount); }

    @FunctionalInterface
    public interface CurrencyCharger
    { boolean chargePlayer(UUID playerId, PlayerEntity player, double amount); }

    @FunctionalInterface
    public interface CurrencyGiver
    { boolean giveToPlayer(UUID playerId, PlayerEntity player, double amount); }

    public static class PlayerNotOnlineForCurrencyInteractionException extends RuntimeException
    {
        public PlayerNotOnlineForCurrencyInteractionException(UUID playerId, Currency currency)
        { super(getErrorMsg(playerId, currency.getName())); }

        private static String getErrorMsg(UUID playerId, String currencyName)
        {
            String playerName = UsernameCache.getLastKnownUsername(playerId);

            if(playerName == null)
                return "The player with the ID " + playerId.toString() + " was not online for the currency \""
                       + currencyName + "\" to interact with.";

            return "The player " + playerName + " was not online for the currency \"" + currencyName + "\" to interact "
                   + "with.";
        }

        UUID playerId;
        Currency currency;

        public UUID getPlayerId()
        { return playerId; }

        public Currency getCurrency()
        { return currency; }
    }

    public Currency(String          name,
                    String          displayName,
                    String          displayNamePlural,
                    boolean         requiresPlayerBeOnline,
                    CurrencyChecker checker,
                    CurrencyCharger charger,
                    CurrencyGiver   giver)
    {
        this.name                   = name;
        this.displayName            = displayName;
        this.displayNamePlural      = displayNamePlural;
        this.requiresPlayerBeOnline = requiresPlayerBeOnline;
        this.checker                = checker;
        this.charger                = charger;
        this.giver                  = giver;
    }

    public static final Currency EXPERIENCE
            = new Currency("xp", "experience", "experience", true,
                           (id, p, amount) -> p.experienceTotal >= amount,
                           (id, p, amount) -> { p.giveExperiencePoints(-(int)amount); return true; },
                           (id, p, amount) -> { p.giveExperiencePoints((int)amount); return true; });

    public static final Currency LEVELS
            = new Currency("lvls", "level", "levels", true,
                           (playerId, player, amount) -> player.experienceLevel >= amount,
                           (playerId, player, amount) -> { player.addExperienceLevel(-(int)amount); return true; },
                           (playerId, player, amount) -> { player.addExperienceLevel((int)amount); return true; });

    private final String name;
    private final String displayName;
    private final String displayNamePlural;
    private final boolean requiresPlayerBeOnline;
    private final CurrencyChecker checker;
    private final CurrencyCharger charger;
    private final CurrencyGiver giver;

    public String getName()
    { return name; }

    public String getDisplayName()
    { return displayName; }

    public String getDisplayNamePlural()
    { return displayNamePlural; }

    public boolean requiresPlayerBeOnline()
    { return requiresPlayerBeOnline; }

    private boolean playerCanAfford(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        return checker.canAfford(playerId, player, amount);
    }

    public boolean playerCanAfford(UUID playerId, double amount)
    { return playerCanAfford(playerId, getOnlinePlayer(playerId), amount); }

    public boolean playerCanAfford(PlayerEntity player, double amount)
    { return playerCanAfford(player.getUniqueID(), player, amount); }

    private boolean chargePlayer(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        if(!checker.canAfford(playerId, player, amount))
            return false;

        return charger.chargePlayer(playerId, player, amount);
    }

    public boolean chargePlayer(UUID playerId, double amount)
    { return chargePlayer(playerId, getOnlinePlayer(playerId), amount); }

    public boolean chargePlayer(PlayerEntity player, double amount)
    { return chargePlayer(player.getUniqueID(), player, amount); }

    private boolean giveToPlayer(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        return giver.giveToPlayer(playerId, player, amount);
    }

    public boolean giveToPlayer(UUID playerId, double amount)
    { return giveToPlayer(playerId, getOnlinePlayer(playerId), amount); }

    public boolean giveToPlayer(PlayerEntity player, double amount)
    { return giveToPlayer(player.getUniqueID(), player, amount); }
}
