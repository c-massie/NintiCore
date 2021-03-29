package scot.massie.mc.ninti.core.currencies;

import net.minecraft.entity.player.PlayerEntity;

import java.util.*;
import java.util.function.Function;

import static scot.massie.mc.ninti.core.PluginUtils.*;

public class Currencies
{
    public static final class UnrecognisedCurrencyException extends Exception
    {
        public UnrecognisedCurrencyException(String currencyName)
        {
            super("Unrecognised currency name: " + currencyName);
            this.currencyName = currencyName;
        }

        private final String currencyName;

        public String getCurrencyName()
        { return currencyName; }
    }

    private Currencies()
    {}

    private static final Map<String, Currency> currencies = new HashMap<>();

    public static Currency getCurrency(String name) throws UnrecognisedCurrencyException
    {
        Currency result;

        synchronized(currencies)
        { result = currencies.get(name); }

        if(result == null)
            throw new UnrecognisedCurrencyException(name);

        return result;
    }

    public static List<Currency> getCurrencies()
    {
        List<Currency> result;

        synchronized(currencies)
        { result = new ArrayList<>(currencies.values()); }

        result.sort(Comparator.comparing(Currency::getName));
        return result;
    }

    public static void register(Currency currency)
    {
        synchronized(currencies)
        { currencies.put(currency.getName(), currency); }
    }

    public static boolean playerCanAfford(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).playerCanAfford(playerId, amount); }

    public static boolean playerCanAfford(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).playerCanAfford(player, amount); }

    public static boolean chargePlayer(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).chargePlayer(playerId, amount); }

    public static boolean chargePlayer(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).chargePlayer(player, amount); }

    public static boolean givePlayer(UUID playerId, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).giveToPlayer(playerId, amount); }

    public static boolean givePlayer(PlayerEntity player, String currencyName, double amount)
            throws UnrecognisedCurrencyException
    { return getCurrency(currencyName).giveToPlayer(player, amount); }
}
