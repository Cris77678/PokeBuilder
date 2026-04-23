package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.util.GuiUtils;
import com.tuservidor.pokebuilder.util.PokemonTags;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class LevelEditorMenu {

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double pricePerLevel = cfg.effectivePrice(cfg.getPriceLevelUp(), player);
            double priceMax = cfg.effectivePrice(cfg.getPriceLevelMax(), player);
            int current = pokemon.getLevel();
            int max = cfg.getMaxLevel();

            List<String> evolutionWarning = List.of(
                "",
                "&8⚠ &nImportante:&8 La evolución y",
                "&8el aprendizaje de movimientos requieren",
                "&8subir 1 nivel extra manualmente."
            );

            template.set(10, GuiUtils.button(Items.EXPERIENCE_BOTTLE,
                "&a+1 Nivel &8(" + current + " → " + Math.min(current + 1, max) + ")",
                mergeLists(List.of("&7Precio: &e" + EconomyManager.formatCost(pricePerLevel)), evolutionWarning, List.of("", "&aClick para subir")),
                () -> PokeBuilder.runAsync(() -> levelUp(player, pokemon, 1, pricePerLevel))));

            template.set(12, GuiUtils.button(Items.EXPERIENCE_BOTTLE,
                "&a+5 Niveles &8(" + current + " → " + Math.min(current + 5, max) + ")",
                mergeLists(List.of("&7Precio estimado: &e" + EconomyManager.formatCost(pricePerLevel * 5)), evolutionWarning, List.of("", "&aClick para subir")),
                () -> PokeBuilder.runAsync(() -> levelUp(player, pokemon, 5, pricePerLevel))));

            template.set(14, GuiUtils.button(Items.EXPERIENCE_BOTTLE,
                "&a+10 Niveles &8(" + current + " → " + Math.min(current + 10, max) + ")",
                mergeLists(List.of("&7Precio estimado: &e" + EconomyManager.formatCost(pricePerLevel * 10)), evolutionWarning, List.of("", "&aClick para subir")),
                () -> PokeBuilder.runAsync(() -> levelUp(player, pokemon, 10, pricePerLevel))));

            template.set(16, GuiUtils.button(Items.NETHER_STAR,
                "&6Nivel Máximo &8(→ " + max + ")",
                mergeLists(List.of("&7Precio total: &e" + EconomyManager.formatCost(priceMax)), evolutionWarning, List.of("", "&aClick para subir")),
                () -> PokeBuilder.runAsync(() -> levelMax(player, pokemon, priceMax))));

            template.set(22, GuiUtils.button(Items.WRITTEN_BOOK,
                "&7Nivel actual: &f" + current,
                List.of("&7Nivel máximo configurado: &f" + max), () -> {}));

            template.set(25, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.closeUI(player);
                })));

            GuiUtils.fillEmpty(template, 3);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Nivel &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }

    private static void levelUp(ServerPlayerEntity player, Pokemon pokemon, int requestedAmount, double pricePerLevel) {
        int max = PokeBuilder.config.getMaxLevel();
        int current = pokemon.getLevel();
        int target = Math.min(current + requestedAmount, max);
        int actualGain = target - current;

        if (actualGain <= 0) {
            sendMsg(player, "&cEl Pokémon ya está en el nivel máximo."); return;
        }

        double finalPrice = actualGain * pricePerLevel;

        if (!EconomyManager.charge(player, finalPrice)) {
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgNotEnoughFunds(),
                "%cost%", EconomyManager.formatCost(finalPrice)));
            return;
        }
        
        PokeBuilder.server.execute(() -> {
            if (player.isRemoved()) return;
            if (Cobblemon.INSTANCE.getStorage().getParty(player).get(pokemon.getUuid()) == null) {
                sendMsg(player, "&cError: El Pokémon ya no está en tu equipo. Transacción cancelada.");
                return;
            }

            PokemonTags.markBuilt(pokemon);
            pokemon.setLevel(target);
            
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(finalPrice)));
            open(player, pokemon);
        });
    }

    private static void levelMax(ServerPlayerEntity player, Pokemon pokemon, double price) {
        int max = PokeBuilder.config.getMaxLevel();
        if (pokemon.getLevel() >= max) {
            sendMsg(player, "&cEl Pokémon ya está en el nivel máximo."); return;
        }
        
        if (!EconomyManager.charge(player, price)) {
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgNotEnoughFunds(),
                "%cost%", EconomyManager.formatCost(price)));
            return;
        }
        
        PokeBuilder.server.execute(() -> {
            if (player.isRemoved()) return;
            if (Cobblemon.INSTANCE.getStorage().getParty(player).get(pokemon.getUuid()) == null) {
                sendMsg(player, "&cError: El Pokémon ya no está en tu equipo. Transacción cancelada.");
                return;
            }

            PokemonTags.markBuilt(pokemon);
            pokemon.setLevel(max);
            
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(price)));
            open(player, pokemon);
        });
    }

    private static List<String> mergeLists(List<String> a, List<String> b, List<String> c) {
        java.util.ArrayList<String> result = new java.util.ArrayList<>(a);
        result.addAll(b);
        result.addAll(c);
        return result;
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> {
            if (!player.isRemoved()) {
                player.sendMessage(AdventureTranslator.toNative(msg));
            }
        });
    }
}