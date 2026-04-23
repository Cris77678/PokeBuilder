package com.tuservidor.pokebuilder;

import com.tuservidor.pokebuilder.config.PokeBuilderConfig;
import com.tuservidor.pokebuilder.config.PokeBuilderLang;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.commands.PokeBuilderCommand;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("PokeBuilder-%d").build()
    );

    public static void reload() {
        config.init();
        lang.init();
    }

    public static void runAsync(Runnable task) {
        if (EXECUTOR.isShutdown()) { task.run(); return; }
        CompletableFuture.runAsync(task, EXECUTOR)
            .orTimeout(15, TimeUnit.SECONDS)
            .exceptionally(e -> { LOGGER.error("Async error", e); return null; });
    }

    @Override
    public void onInitialize() {
        LOGGER.info("PokeBuilder loading...");

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            PokeBuilderCommand.register(dispatcher));

        // Flush coin cache on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, svr) ->
            EconomyManager.flush(handler.player.getUuid()));

        // We grab the server reference via a mixin-free trick:
        // store it when the first command fires, or use the lifecycle event below
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            reload();
            LOGGER.info("PokeBuilder ready! Coin: {}", config.getCoinName());
        });

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(srv ->
            EXECUTOR.shutdown());
    }
}
