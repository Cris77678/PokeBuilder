package com.tuservidor.pokebuilder.economy;

import com.tuservidor.pokebuilder.PokeBuilder;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class EconomyManager {

    private static final ConcurrentHashMap<UUID, Long> TRANSACTION_LOCK = new ConcurrentHashMap<>();

    private static EconomyService getService() {
        try {
            Impactor impactor = Impactor.instance();
            Object registry;
            try {
                registry = impactor.getClass().getMethod("registry").invoke(impactor);
            } catch (Exception e) {
                registry = impactor.getClass().getMethod("getRegistry").invoke(impactor);
            }
            return (EconomyService) registry.getClass().getMethod("get", Class.class).invoke(registry, EconomyService.class);
        } catch (Throwable e) { // FIX: Capturar Throwable
            PokeBuilder.LOGGER.error("Fallo crítico: Impactor no detectado.");
            return null;
        }
    }

    private static Currency getCurrency() {
        var service = getService();
        return (service != null) ? service.currencies().primary() : null;
    }

    public static double getBalance(ServerPlayerEntity player) {
        var service = getService();
        var currency = getCurrency();
        if (service == null || currency == null) return 0.0;
        
        Account account = service.account(currency, player.getUuid()).join();
        return account.balance().doubleValue();
    }

    public static String getBalanceFormatted(ServerPlayerEntity player) {
        double balance = getBalance(player);
        return String.format("%.0f %s", balance, PokeBuilder.config.getCoinName());
    }

    public static boolean charge(ServerPlayerEntity player, double amount) {
        var service = getService();
        var currency = getCurrency();
        if (service == null || currency == null) return false;

        // Anti-spam de transacciones
        long now = System.currentTimeMillis();
        AtomicBoolean isSpam = new AtomicBoolean(false);
        TRANSACTION_LOCK.compute(player.getUuid(), (uuid, lastTx) -> {
            if (lastTx != null && (now - lastTx < 500)) {
                isSpam.set(true);
                return lastTx; 
            }
            return now; 
        });

        if (isSpam.get()) return false;

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
    
    public static void flush(UUID uuid) {}
    public static void set(UUID uuid, double amount) {}
}
