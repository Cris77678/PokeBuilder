package com.tuservidor.pokebuilder;

import com.tuservidor.pokebuilder.config.PokeBuilderConfig;
import com.tuservidor.pokebuilder.config.PokeBuilderLang;
import com.tuservidor.pokebuilder.commands.PokeBuilderCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
    // ESTA ES LA LÍNEA QUE FALTABA:
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

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            PokeBuilderCommand.register(dispatcher));

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(srv -> {
            server = srv;
            reload();
            LOGGER.info("PokeBuilder listo!");
        });
    }
}
