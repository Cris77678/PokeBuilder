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

public class SizeEditorMenu {

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double price = cfg.effectivePrice(cfg.getPriceSize(), player);
            float current = pokemon.getScaleModifier();
            float min = cfg.getMinSize();
            float max = cfg.getMaxSize();

            float minusHalf = Math.max(Math.round((current - 0.5f) * 10f) / 10f, min);
            float minusPointOne = Math.max(Math.round((current - 0.1f) * 10f) / 10f, min);
            float plusPointOne = Math.min(Math.round((current + 0.1f) * 10f) / 10f, max);
            float plusHalf = Math.min(Math.round((current + 0.5f) * 10f) / 10f, max);

            template.set(10, GuiUtils.button(Items.RED_DYE,
                "&c-0.5 Tamaño &8(" + String.format("%.1f", minusHalf) + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, minusHalf, price))));

            template.set(11, GuiUtils.button(Items.ORANGE_DYE,
                "&c-0.1 Tamaño &8(" + String.format("%.1f", minusPointOne) + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, minusPointOne, price))));

            template.set(13, GuiUtils.button(Items.PISTON,
                "&7Tamaño actual: &f" + String.format("%.2f", current),
                List.of(
                    "&7Mínimo: &f" + min,
                    "&7Máximo: &f" + max
                ), () -> {}));

            template.set(12, GuiUtils.button(Items.LIME_DYE,
                "&aReset &8(1.0)",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, 1.0f, price))));

            template.set(14, GuiUtils.button(Items.LIGHT_BLUE_DYE,
                "&a+0.1 Tamaño &8(" + String.format("%.1f", plusPointOne) + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, plusPointOne, price))));

            template.set(15, GuiUtils.button(Items.BLUE_DYE,
                "&a+0.5 Tamaño &8(" + String.format("%.1f", plusHalf) + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, plusHalf, price))));

            template.set(19, GuiUtils.button(Items.RED_CONCRETE,
                "&cTamaño mínimo &8(" + min + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, min, price))));

            template.set(25, GuiUtils.button(Items.BLUE_CONCRETE,
                "&9Tamaño máximo &8(" + max + ")",
                List.of("&7Precio: &e" + EconomyManager.formatCost(price), "", "&aClick para aplicar"),
                () -> PokeBuilder.runAsync(() -> applySize(player, pokemon, max, price))));

            template.set(23, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.closeUI(player);
                })));

            GuiUtils.fillEmpty(template, 3);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Tamaño &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }

    private static void applySize(ServerPlayerEntity player, Pokemon pokemon, float size, double price) {
        
        float clamped = Math.round(Math.max(PokeBuilder.config.getMinSize(),
            Math.min(PokeBuilder.config.getMaxSize(), size)) * 10f) / 10f;

        if (Math.abs(clamped - pokemon.getScaleModifier()) < 0.001f) {
            sendMsg(player, "&cEl Pokémon ya tiene ese tamaño."); return;
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
            pokemon.setScaleModifier(clamped);
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(price)));
            open(player, pokemon);
        });
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> {
            if (!player.isRemoved()) {
                player.sendMessage(AdventureTranslator.toNative(msg));
            }
        });
    }
}