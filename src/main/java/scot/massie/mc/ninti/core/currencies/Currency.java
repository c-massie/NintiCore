package scot.massie.mc.ninti.core.currencies;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.UsernameCache;

import java.util.UUID;

import static scot.massie.mc.ninti.core.PluginUtils.*;

/**
 * A currency, which may be registered in {@link Currencies} and allows a player to be given or charged any specified
 * unit.
 */
public class Currency
{
    /**
     * Functional interface for checking if a given player can afford an amount of a currency.
     */
    @FunctionalInterface
    public interface CurrencyChecker
    {
        /**
         * Checks whether or not a player can afford a given amount of a currency.
         * @param playerId The ID of the player concerned.
         * @param player The player as an entity. If the player is offline, this is null.
         * @param amount The amount of the currency to check if the player can afford.
         * @return True if the player can afford the given amount of the currency. Otherwise, false.
         */
        boolean canAfford(UUID playerId, PlayerEntity player, double amount);
    }

    /**
     * Functional interface for charging a given player a specified amount of a currency.
     */
    @FunctionalInterface
    public interface CurrencyCharger
    {
        /**
         * Charges a player an amount of a currency.
         * @param playerId The ID of the player to charge.
         * @param player The player as an entity. If the player is offline, this is null.
         * @param amount The amount of the currency to charge.
         * @return True if the player could afford to be charged and was. Otherwise, false.
         */
        boolean chargePlayer(UUID playerId, PlayerEntity player, double amount);
    }

    /**
     * Functional interface for giving a given player a specified amount of a currency.
     */
    @FunctionalInterface
    public interface CurrencyGiver
    {
        /**
         * Gives a player an amount of a currency.
         * @param playerId The ID of the player to give currency to.
         * @param player The player as an entity. If the player is offline, this is null.
         * @param amount The amount of the currency to give.
         * @return True if the player was successfully given the amount specified of the currency. Otherwise, false.
         */
        boolean giveToPlayer(UUID playerId, PlayerEntity player, double amount);
    }

    /**
     * Thrown when an offline player's currency is interacted with where the currency requires the player to be online.
     */
    public static class PlayerNotOnlineForCurrencyInteractionException extends RuntimeException
    {
        /**
         * Creates a new instance of the exception.
         * @param playerId The ID of the player that was offline.
         * @param currency The currency that requires a player to be online to be interacted with.
         */
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

        /**
         * Gets the ID of the player that was offline.
         * @return The ID of the player that was offline.
         */
        public UUID getPlayerId()
        { return playerId; }

        /**
         * Gets the currency that required the player to be online in order to be interacted with.
         * @return The currency that required the player to be online to be interacted with.
         */
        public Currency getCurrency()
        { return currency; }
    }

    /**
     * Creates a new currency.
     * @param name The (unique) name of the currency, as it may be referred to in code or in configuration.
     * @param displayName The display name (i.e. full english name) of the currency.
     * @param displayNamePlural The pluralised display name (i.e. full english name) of the currency.
     * @param requiresPlayerBeOnline Whether or not a player must be online in order for them to be interacted with with
     *                               respect to this currency.
     * @param checker Function for checking whether or not a player can afford a given amount of this currency.
     * @param charger Function for charging a player a given amount of this currency.
     * @param giver Fucntion for giving a player a given amount of this currency.
     */
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

    /**
     * The default currency representing experience points.
     */
    public static final Currency EXPERIENCE
            = new Currency("xp", "experience", "experience", true,
                           (id, p, amount) -> p.experienceTotal >= amount,
                           (id, p, amount) -> { p.giveExperiencePoints(-(int)amount); return true; },
                           (id, p, amount) -> { p.giveExperiencePoints((int)amount); return true; });

    /**
     * The default currency representing experience levels.
     */
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

    /**
     * Gets the unique name of this currency, as it might be referred to in code or configuration.
     * @return The unique name of this currency.
     */
    public String getName()
    { return name; }

    /**
     * Gets the fullform name of this currency.
     * @return This currency's display name.
     */
    public String getDisplayName()
    { return displayName; }

    /**
     * Gets the pluralised fullform name of this currency.
     * @return The plural form of this currency's display name.
     */
    public String getDisplayNamePlural()
    { return displayNamePlural; }

    /**
     * Gets whether or not this currency requires a player to be online in order to be interacted with.
     * @return True if the currency requires a player to be online in order to be interacted with, otherwise false.
     */
    public boolean requiresPlayerBeOnline()
    { return requiresPlayerBeOnline; }

    /**
     * Gets whether or not a player can afford an amount of this currency.
     * @param playerId The ID of the player to check.
     * @param player The player to check, as an entity. Null if the player is offline.
     * @param amount The amount of the currency to check if the player can afford.
     * @return True if the player can afford the given amount of the currency. Otherwise, false.
     */
    private boolean playerCanAfford(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        return checker.canAfford(playerId, player, amount);
    }

    /**
     * Gets whether or not a player can afford an amount of this currency.
     * @param playerId The ID of the player to check.
     * @param amount The amount of the currency to check if the player can afford.
     * @return True if the player can afford the given amount of the currency. Otherwise, false.
     */
    public boolean playerCanAfford(UUID playerId, double amount)
    { return playerCanAfford(playerId, getOnlinePlayer(playerId), amount); }

    /**
     * Gets whether or not a player can afford an amount of this currency.
     * @param player The player to check, as an entity.
     * @param amount The amount of the currency to check if the player can afford.
     * @return True if the player can afford the given amount of the currency. Otherwise, false.
     */
    public boolean playerCanAfford(PlayerEntity player, double amount)
    { return playerCanAfford(player.getUniqueID(), player, amount); }

    /**
     * Charges a player an amount of this currency.
     * @param playerId The ID of the player to charge.
     * @param player The player to charge, as an entity. Null if the player is offline.
     * @param amount The amount of the currency to charge the player.
     * @return True if the player could afford the amount of this currency and was successfully charged. Otherwise,
     *         false.
     */
    private boolean chargePlayer(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        if(!checker.canAfford(playerId, player, amount))
            return false;

        return charger.chargePlayer(playerId, player, amount);
    }

    /**
     * Charges a player an amount of this currency.
     * @param playerId The ID of the player to charge.
     * @param amount The amount of the currency to charge the player.
     * @return True if the player could afford the amount of this currency and was successfully charged. Otherwise,
     *         false.
     */
    public boolean chargePlayer(UUID playerId, double amount)
    { return chargePlayer(playerId, getOnlinePlayer(playerId), amount); }

    /**
     * Charges a player an amount of this currency.
     * @param player The player to charge, as an entity.
     * @param amount The amount of the currency to charge the player.
     * @return True if the player could afford the amount of this currency and was successfully charged. Otherwise,
     *         false.
     */
    public boolean chargePlayer(PlayerEntity player, double amount)
    { return chargePlayer(player.getUniqueID(), player, amount); }

    /**
     * Gives a player an amount of this currency.
     * @param playerId The ID of the player to give to.
     * @param player The player to give to, as an entity. Null if the player is offline.
     * @param amount The amount of the currency to give to the player.
     * @return True if the player was successfully given the amount of currency. Otherwise, false.
     */
    private boolean giveToPlayer(UUID playerId, PlayerEntity player, double amount)
    {
        if(requiresPlayerBeOnline && player == null)
            throw new PlayerNotOnlineForCurrencyInteractionException(playerId, this);

        return giver.giveToPlayer(playerId, player, amount);
    }

    /**
     * Gives a player an amount of this currency.
     * @param playerId The ID of the player to give to.
     * @param amount The amount of the currency to give to the player.
     * @return True if the player was successfully given the amount of currency. Otherwise, false.
     */
    public boolean giveToPlayer(UUID playerId, double amount)
    { return giveToPlayer(playerId, getOnlinePlayer(playerId), amount); }

    /**
     * Gives a player an amount of this currency.
     * @param player The player to give to, as an entity.
     * @param amount The amount of the currency to give to the player.
     * @return True if the player was successfully given the amount of currency. Otherwise, false.
     */
    public boolean giveToPlayer(PlayerEntity player, double amount)
    { return giveToPlayer(player.getUniqueID(), player, amount); }
}
