package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon; // [FIX] Importación vital añadida
import com.cobblemon.mod.common.api.pokemon.Natures;
import com.cobblemon.mod.common.pokemon.Nature;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.util.GuiUtils;
import com.tuservidor.pokebuilder.util.PokemonTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class NatureEditorMenu {

    private static final String[] NATURES = {
        "hardy","lonely","brave","adamant","naughty",
        "bold","docile","relaxed","impish","lax",
        "timid","hasty","serious","jolly","naive",
        "modest","mild","quiet","bashful","rash",
        "calm","gentle","sassy","careful","quirky"
    };

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double price = cfg.effectivePrice(cfg.getPriceNature(), player);
            String currentNature = pokemon.getNature().getName().getPath();

            for (int i = 0; i < NATURES.length; i++) {
                String natureName = NATURES[i];
                Nature nature = Natures.INSTANCE.getNature(Identifier.of("cobblemon", natureName));
                if (nature == null) continue;

                boolean isCurrent = natureName.equals(currentNature);
                var item = isCurrent ? Items.LIME_DYE : Items.GRAY_DYE;

                List<String> lore = new ArrayList<>();
                if (nature.getIncreasedStat() != null) {
                    lore.add("&a+ " + nature.getIncreasedStat().getShowdownId().toUpperCase());
                    lore.add("&c- " + nature.getDecreasedStat().getShowdownId().toUpperCase());
                } else {
                    lore.add("&7Neutral");
                }
                lore.add("");
                
                if (isCurrent) {
                    lore.add("&aNaturaleza actual");
                } else {
                    lore.add("&7Precio: &e" + EconomyManager.formatCost(price));
                    lore.add("&aClick para aplicar");
                }

                ItemStack stack = new ItemStack(item);
                stack.set(DataComponentTypes.CUSTOM_NAME,
                    AdventureTranslator.toNative((isCurrent ? "&a" : "&f") + capitalize(natureName)));
                stack.set(DataComponentTypes.LORE,
                    new LoreComponent(AdventureTranslator.toNativeL(lore)));

                final Nature finalNature = nature;
                template.set(i, GooeyButton.builder()
                    .display(stack)
                    .onClick(a -> {
                        if (isCurrent) return;
                        PokeBuilder.runAsync(() -> applyNature(player, pokemon, finalNature, price));
                    })
                    .build());
            }

            template.set(25, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.closeUI(player);
                })));

            GuiUtils.fillEmpty(template, 3);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Naturaleza &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }

    private static void applyNature(ServerPlayerEntity player, Pokemon pokemon, Nature nature, double price) {
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
            pokemon.setNature(nature);
            
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(price)));
            open(player, pokemon);
        });
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> {
            if (!player.isRemoved()) {
                player.sendMessage(AdventureTranslator.toNative(msg));
            }
        });
    }
}