package com.tuservidor.pokebuilder.economy;

import com.tuservidor.pokebuilder.PokeBuilder;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.kyori.adventure.key.Key;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private static final ConcurrentHashMap<UUID, Long> TRANSACTION_LOCK = new ConcurrentHashMap<>();

    private static EconomyService getService() {
        try {
            // Acceso directo a la API de Impactor v5
            return Impactor.instance().services().provide(EconomyService.class);
        } catch (Throwable e) {
            PokeBuilder.LOGGER.error("Fallo crítico: No se encontró el servicio de economía de Impactor.");
            return null;
        }
    }

    private static Currency getCurrency() {
        var service = getService();
        if (service == null) return null;

        // Buscamos la moneda específica usando su Key en lugar de primary()
        return service.currencies().currency(Key.key("pokebuilder", "pokecoins")).orElseGet(() -> {
            PokeBuilder.LOGGER.error("No se encontró la moneda 'pokebuilder:pokecoins'.");
            return null;
        });
    }

    public static double getBalance(ServerPlayerEntity player) {
        var service = getService();
        var currency = getCurrency();
        if (service == null || currency == null) return 0.0;
        
        // Obtener la cuenta y el balance de forma síncrona para la GUI
        Account account = service.account(currency, player.getUuid()).join();
        return account.balance().doubleValue();
    }

    public static String getBalanceFormatted(ServerPlayerEntity player) {
        return String.format("%.0f %s", getBalance(player), PokeBuilder.config.getCoinName());
    }

    public static boolean charge(ServerPlayerEntity player, double amount) {
        var service = getService();
        var currency = getCurrency();
        if (service == null || currency == null) return false;

        Account account = service.account(currency, player.getUuid()).join();
        if (account.balance().doubleValue() < amount) return false;

        EconomyTransaction transaction = account.withdraw(BigDecimal.valueOf(amount));
        return transaction.successful();
    }

    public static void give(ServerPlayerEntity player, double amount) {
        var service = getService();
        var currency = getCurrency();
        if (service != null && currency != null) {
            Account account = service.account(currency, player.getUuid()).join();
            account.deposit(BigDecimal.valueOf(amount));
        }
    }

    public static String formatCost(double amount) {
        return String.format("%.0f %s", amount, PokeBuilder.config.getCoinName());
    }
}
