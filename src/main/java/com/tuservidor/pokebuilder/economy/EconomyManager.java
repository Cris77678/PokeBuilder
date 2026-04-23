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

    // [FIX CRÍTICO]: Puente universal para sortear los cambios de nombre en los métodos de Impactor
    private static EconomyService getService() {
        try {
            Impactor impactor = Impactor.instance();
            Object registry;
            try {
                // Intenta método de versiones más nuevas
                registry = impactor.getClass().getMethod("registry").invoke(impactor);
            } catch (Exception e) {
                // Intenta método de versiones más antiguas
                registry = impactor.getClass().getMethod("getRegistry").invoke(impactor);
            }
            return (EconomyService) registry.getClass().getMethod("get", Class.class).invoke(registry, EconomyService.class);
        } catch (Exception e) {
            PokeBuilder.LOGGER.error("Fallo crítico obteniendo EconomyService de Impactor", e);
            throw new RuntimeException(e);
        }
    }

    private static Currency getCurrency() {
        return getService().currencies().primary();
    }

    public static double getBalance(ServerPlayerEntity player) {
        Account account = getService().account(getCurrency(), player.getUuid()).join();
        return account.balance().doubleValue();
    }

    public static String getBalanceFormatted(ServerPlayerEntity player) {
        double balance = getBalance(player);
        return String.format("%.0f %s", balance, PokeBuilder.config.getCoinName());
    }

    public static boolean charge(ServerPlayerEntity player, double amount) {
        long now = System.currentTimeMillis();
        AtomicBoolean isSpam = new AtomicBoolean(false);

        TRANSACTION_LOCK.compute(player.getUuid(), (uuid, lastTx) -> {
            if (lastTx != null && (now - lastTx < 500)) {
                isSpam.set(true);
                return lastTx; 
            }
            return now; 
        });

        if (isSpam.get()) {
            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) {
                    player.sendMessage(Text.literal("§cProcesando transacción, por favor espera un momento..."), true);
                }
            });
            return false;
        }

        Account account = getService().account(getCurrency(), player.getUuid()).join();
        
        if (account.balance().doubleValue() < amount) {
            return false;
        }

        EconomyTransaction transaction = account.withdraw(BigDecimal.valueOf(amount));
        return transaction.successful();
    }

    public static void give(ServerPlayerEntity player, double amount) {
        Account account = getService().account(getCurrency(), player.getUuid()).join();
        account.deposit(BigDecimal.valueOf(amount));
    }

    public static void set(UUID uuid, double amount) {
        Account account = getService().account(getCurrency(), uuid).join();
        account.set(BigDecimal.valueOf(amount));
    }

    public static void flush(UUID uuid) {}

    public static String formatCost(double amount) {
        return String.format("%.0f %s", amount, PokeBuilder.config.getCoinName());
    }
}