package scot.massie.mc.ninti.core.currencies;

import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Static registry for currencies, allowing {@link Currency currencies} to be registered and accessed by name.
 */
public final class Currencies
{
    /**
     * Thrown if a currency is referenced by name, but no currency exists by that name.
     */
    public static final class UnrecognisedCurrencyException extends Exception
    {
        /**
         * Creates a new exception.
         * @param currencyName The name of the currency which does not exist.
         */
        public UnrecognisedCurrencyException(String currencyName)
        {
            super("Unrecognised currency name: " + currencyName);
            this.currencyName = currencyName;
        }

        private final String currencyName;

        /**
         * Gets the name of the currency that does not exist.
         * @return The name of the currency that does not exist.
         */
        public String getCurrencyName()
        { return currencyName; }
    }

    private Currencies()
    {}

    private static final Map<String, Currency> currencies = new HashMap<>();

    /**
     * Gets the currency registered by the given name.
     * @param name The name of the currency to get.
     * @return The currency registered under the given name, or null if no such currency exists.
     */
    public static Currency getCurrency(String name)
    {
        Currency result;

        synchronized(currencies)
        { result = currencies.get(name); }

        return result;
    }

    /**
     * Gets the currency registered by the given name, throwing an exception if there is no currency by the given name.
     * @param name The name of the currency to get.
     * @return The currency registered under the given name.
     * @throws UnrecognisedCurrencyException If there is no currency by the given name.
     */
    private static Currency getCurrencyUnsafe(String name) throws UnrecognisedCurrencyException
    {
        Currency result;

        synchronized(currencies)
        { result = currencies.get(name); }

        if(result == null)
            throw new UnrecognisedCurrencyException(name);

        return result;
    }

    /**
     * Gets all registered currencies.
     * @return A list of registered currencies, ordered by their names alphabetically.
     */
    public static List<Currency> getCurrencies()
    {
        List<Currency> result;

        synchronized(currencies)
        { result = new ArrayList<>(currencies.values()); }

        result.sort(Comparator.comparing(Currency::getName));
        return result;
    }

    /**
     * Registers a currency. The registered currency may then be referred to by name. If another currency exists by the
     * given name, overwrites it.
     * @param currency The currency to register.
     */
    public static void register(Currency currency)
    {
        synchronized(currencies)
        { currencies.put(currency.getName(), currency); }
    }

    /**
     * Gets whether or not a player can afford the given amount of the specified currency.
     * @param playerId The ID of the player to check if they can afford the amount.
     * @param currencyName The name of the currency.
     * @param amount The amount of the specified currency to check if the player can afford.
     * @return True if the player can afford the given amount of the specified currency. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean playerCanAfford(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).playerCanAfford(playerId, amount); }

    /**
     * Gets whether or not a player can afford the given amount of the specified currency.
     * @param player The player to check if they can afford the amount.
     * @param currencyName The name of the currency.
     * @param amount The amount of the specified currency to check if the player can afford.
     * @return True if the player can afford the given amount of the specified currency. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean playerCanAfford(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).playerCanAfford(player, amount); }

    /**
     * Gets whether or not a player can afford given amounts of possibly multiple specified currencies.
     * @param playerId The ID of the player to check if they can afford the given amounts of the specified currencies.
     * @param currencyAmounts A map containing the names of currencies as the keys and the amounts of those currencies
     *                        as values, to check if they player can afford them.
     * @return True if the player can afford the given amount of all currencies in the given map.
     * @throws UnrecognisedCurrencyException If any of the keys in the given map are not the names of registered
     *                                       currencies.
     */
    public static boolean playerCanAfford(UUID playerId, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                if(!c.playerCanAfford(playerId, entry.getValue()))
                    return false;
            }
        }

        return true;
    }

    /**
     * Gets whether or not a player can afford given amounts of possibly multiple specified currencies.
     * @param player The player to check if they can afford the given amounts of the specified currencies.
     * @param currencyAmounts A map containing the names of currencies as the keys and the amounts of those currencies
     *                        as values, to check if they player can afford them.
     * @return True if the player can afford the given amount of all currencies in the given map.
     * @throws UnrecognisedCurrencyException If any of the keys in the given map are not the names of registered
     *                                       currencies.
     */
    public static boolean playerCanAfford(PlayerEntity player, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                if(!c.playerCanAfford(player, entry.getValue()))
                    return false;
            }
        }

        return true;
    }

    /**
     * Charges a player a given amount of a specified currency.
     * @param playerId The ID of the player to charge.
     * @param currencyName The name of the currency to charge the player.
     * @param amount The amount of the currency to charge the player.
     * @return True if the player could afford to be, and was, successfully charged. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean chargePlayer(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).chargePlayer(playerId, amount); }

    /**
     * Charges a player a given amount of a specified currency.
     * @param player The player to charge.
     * @param currencyName The name of the currency to charge the player.
     * @param amount The amount of the currency to charge the player.
     * @return True if the player could afford to be, and was, successfully charged. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean chargePlayer(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).chargePlayer(player, amount); }

    /**
     * Charges a player given amounts of multiple specified currencies.
     * @param playerId The ID of the player to charge.
     * @param currencyAmounts A map containing the names of currencies to charge as the keys and the amounts of those
     *                        currencies to charge as values.
     * @return True if the player could afford, and was successfully charged, the given amounts of all specified
     *         currencies in the given map. Otherwise, false.
     * @throws UnrecognisedCurrencyException If any of the currency names in the given map does not correspond to a
     *                                       registered currency.
     */
    public static boolean chargePlayer(UUID playerId, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                if(!c.playerCanAfford(playerId, entry.getValue()))
                    return false;
            }

            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
                currencies.get(entry.getKey()).chargePlayer(playerId, entry.getValue());
        }

        return true;
    }

    /**
     * Charges a player given amounts of multiple specified currencies.
     * @param player The player to charge.
     * @param currencyAmounts A map containing the names of currencies to charge as the keys and the amounts of those
     *                        currencies to charge as values.
     * @return True if the player could afford, and was successfully charged, the given amounts of all specified
     *         currencies in the given map. Otherwise, false.
     * @throws UnrecognisedCurrencyException If any of the currency names in the given map does not correspond to a
     *                                       registered currency.
     */
    public static boolean chargePlayer(PlayerEntity player, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                if(!c.playerCanAfford(player, entry.getValue()))
                    return false;
            }

            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
                currencies.get(entry.getKey()).chargePlayer(player, entry.getValue());
        }

        return true;
    }

    /**
     * Gives a player a given amount of a specified currency.
     * @param playerId The ID of the player to give an amount of currency to.
     * @param currencyName The name of the currency to give.
     * @param amount The amount of the currency to give.
     * @return True if the player was successfully given the currency. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean giveToPlayer(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).giveToPlayer(playerId, amount); }

    /**
     * Gives a player a given amount of a specified currency.
     * @param player The player to give an amount of currency to.
     * @param currencyName The name of the currency to give.
     * @param amount The amount of the currency to give.
     * @return True if the player was successfully given the currency. Otherwise, false.
     * @throws UnrecognisedCurrencyException If currencyName is not the name of a registered currency.
     */
    public static boolean giveToPlayer(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrencyUnsafe(currencyName).giveToPlayer(player, amount); }

    /**
     * Gives a player given amounts of multiple specified currencies.
     * @param playerId The ID of the player to give amounts of currencies to.
     * @param currencyAmounts A map containing the names of currencies to give as the keys and the amounts of those
     *                        currencies to give as values.
     * @return A map containing the names of currencies passed as the keys and whether or not the player was
     *         successfully given the given amount of that currency as a boolean.
     * @throws UnrecognisedCurrencyException If any of the currency names in the given map does not correspond to a
     *                                       registered currency.
     */
    public static Map<String, Boolean> giveToPlayer(UUID playerId, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        Map<String, Boolean> result = new HashMap<>();

        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                result.put(entry.getKey(), c.giveToPlayer(playerId, entry.getValue()));
            }
        }

        return result;
    }

    /**
     * Gives a player given amounts of multiple specified currencies.
     * @param player The player to give amounts of currencies to.
     * @param currencyAmounts A map containing the names of currencies to give as the keys and the amounts of those
     *                        currencies to give as values.
     * @return A map containing the names of currencies passed as the keys and whether or not the player was
     *         successfully given the given amount of that currency as a boolean.
     * @throws UnrecognisedCurrencyException If any of the currency names in the given map does not correspond to a
     *                                       registered currency.
     */
    public static Map<String, Boolean> giveToPlayer(PlayerEntity player, Map<String, Double> currencyAmounts)
            throws UnrecognisedCurrencyException
    {
        Map<String, Boolean> result = new HashMap<>();

        synchronized(currencies)
        {
            for(Map.Entry<String, Double> entry : currencyAmounts.entrySet())
            {
                Currency c = currencies.get(entry.getKey());

                if(c == null)
                    throw new UnrecognisedCurrencyException(entry.getKey());

                result.put(entry.getKey(), c.giveToPlayer(player, entry.getValue()));
            }
        }

        return result;
    }
}
