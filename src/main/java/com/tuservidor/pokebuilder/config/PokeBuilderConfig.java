package com.tuservidor.pokebuilder.config;

import com.google.gson.GsonBuilder;
import com.tuservidor.pokebuilder.PokeBuilder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Getter
@Setter
public class PokeBuilderConfig {

    private String coinName = "PokéCoins";
    private String coinNameSingular = "PokéCoin";

    // ── Prices ──────────────────────────────────────────────────────────────
    private double priceIvPerStat    = 5000.0;
    private double priceIvPerfect    = 50000.0;
    private double priceNature       = 8000.0;
    private double priceAbility      = 10000.0;
    private double priceHiddenAbility= 25000.0;
    private double priceShiny        = 100000.0;
    private double pricePokeball     = 3000.0;
    private double priceLevelUp      = 500.0;
    private double priceLevelMax     = 25000.0;
    private double priceSize         = 5000.0;
    private double priceMove         = 2000.0;
    private double priceGender       = 15000.0;

    // ── Limits ──────────────────────────────────────────────────────────────
    private int minLevel   = 1;
    private int maxLevel   = 100;
    private float minSize  = 0.5f;
    private float maxSize  = 5.0f;

    // ── Blacklist ────────────────────────────────────────────────────────────
    private List<String> blacklistedSpecies = new ArrayList<>(List.of("egg", "ditto"));

    // ── Permission discounts ─────────────────────────────────────────────────
    private Map<String, Double> priceMultiplierByPermission = new LinkedHashMap<>(Map.of(
        "pokebuilder.discount.vip", 0.75,
        "pokebuilder.discount.mvp", 0.50
    ));

    // ── Sacrifice system ─────────────────────────────────────────────────────
    private double sacrificeRewardShiny       = 50000.0;
    private double sacrificeRewardLegendary   = 75000.0;
    private double sacrificeRewardShinyLegendaryBonus = 25000.0;

    // ─────────────────────────────────────────────────────────────────────────

    public void init() {
        Path path = Path.of(PokeBuilder.PATH + "config.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                PokeBuilderConfig loaded = gson.fromJson(Files.readString(path), PokeBuilderConfig.class);
                if (loaded != null) {
                    PokeBuilder.config = loaded;
                    
                    // [FIX] Defensa contra listas/mapas nulos si el usuario los borra en el JSON
                    if (PokeBuilder.config.blacklistedSpecies == null) 
                        PokeBuilder.config.blacklistedSpecies = new ArrayList<>();
                    if (PokeBuilder.config.priceMultiplierByPermission == null) 
                        PokeBuilder.config.priceMultiplierByPermission = new LinkedHashMap<>();
                }
            }
            Files.writeString(path, gson.toJson(PokeBuilder.config));
        } catch (IOException e) {
            PokeBuilder.LOGGER.error("Failed to load config", e);
        }
    }

    /** Returns the effective price applying the best discount the player has. */
    public double effectivePrice(double base, ServerPlayerEntity player) {
        double best = 1.0;
        
        // Verificación de seguridad por si el mapa está vacío o es nulo
        if (priceMultiplierByPermission == null || priceMultiplierByPermission.isEmpty()) {
            return base;
        }

        try {
            // Evaluamos la integración con LuckPerms
            var lp = net.luckperms.api.LuckPermsProvider.get().getUserManager().getUser(player.getUuid());
            if (lp != null) {
                for (Map.Entry<String, Double> entry : priceMultiplierByPermission.entrySet()) {
                    if (lp.getCachedData().getPermissionData()
                            .checkPermission(entry.getKey()).asBoolean()) {
                        if (entry.getValue() < best) best = entry.getValue();
                    }
                }
            }
        } catch (Throwable ignored) {
            // [FIX CRÍTICO]: Se debe atrapar Throwable, no Exception. 
            // Esto evita que el mod explote con "NoClassDefFoundError" en servidores sin LuckPerms.
        }
        return base * best;
    }
}
