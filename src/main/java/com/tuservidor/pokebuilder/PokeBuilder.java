package com.tuservidor.pokebuilder;

import com.tuservidor.pokebuilder.config.PokeBuilderConfig;
import com.tuservidor.pokebuilder.config.PokeBuilderLang;
import com.tuservidor.pokebuilder.commands.PokeBuilderCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Importaciones para registrar la moneda en Impactor
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import java.math.BigDecimal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PokeBuilder implements ModInitializer {

    public static final String MOD_ID = "pokebuilder";
    public static final String PATH = "config/pokebuilder/";
    public static final String PATH_LANG = PATH + "lang/"; 
    
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MinecraftServer server;
    public static PokeBuilderConfig config = new PokeBuilderConfig();
    public static PokeBuilderLang lang = new PokeBuilderLang();

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void reload() {
        config.init();
        lang.init();
    }

    public static void runAsync(Runnable task) {
        CompletableFuture.runAsync(task, EXECUTOR)
            .orTimeout(15, TimeUnit.SECONDS)
            .exceptionally(e -> { LOGGER.error("Error en tarea asíncrona", e); return null; });
    }

    @Override
    public void onInitialize() {
        LOGGER.info("PokeBuilder cargando...");

        // Evento para registrar la moneda cuando el servidor empieza a cargar
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(srv -> {
            try {
                EconomyService service = Impactor.instance().services().provide(EconomyService.class);
                Key currencyKey = Key.key("pokebuilder", "pokecoins");
                
                // Si la moneda no existe en Impactor, la creamos
                if (!service.currencies().has(currencyKey)) {
                    Currency pokeCoins = Currency.builder()
                            .key(currencyKey)
                            .name(Component.text(config.getCoinNameSingular()))
                            .plural(Component.text(config.getCoinName()))
                            .symbol(Component.text("PC"))
                            .defaultBalance(BigDecimal.ZERO)
                            .build();
                    service.currencies().register(pokeCoins);
                    LOGGER.info("Moneda separada (PokéCoins) registrada exitosamente en Impactor.");
                }
            } catch (Throwable e) {
                LOGGER.warn("Impactor no detectado o error al registrar la moneda: " + e.getMessage());
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            PokeBuilderCommand.register(dispatcher));

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            reload();
            LOGGER.info("PokeBuilder listo!");
        });
    }
}
