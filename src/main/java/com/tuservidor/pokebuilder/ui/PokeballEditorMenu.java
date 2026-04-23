package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.pokeball.PokeBalls;
import com.cobblemon.mod.common.pokeball.PokeBall;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.util.GuiUtils;
import com.tuservidor.pokebuilder.util.PokemonTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class PokeballEditorMenu {

    private static final String[] POKEBALLS = {
        "poke_ball", "great_ball", "ultra_ball", "master_ball",
        "safari_ball", "fast_ball", "level_ball", "lure_ball",
        "heavy_ball", "love_ball", "friend_ball", "moon_ball",
        "sport_ball", "net_ball", "dive_ball", "nest_ball",
        "repeat_ball", "timer_ball", "luxury_ball", "premier_ball",
        "dusk_ball", "heal_ball", "quick_ball", "cherish_ball",
        "dream_ball", "beast_ball", "park_ball"
    };

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(4).build();
            var cfg = PokeBuilder.config;
            double price = cfg.effectivePrice(cfg.getPricePokeball(), player);
            String current = pokemon.getCaughtBall().getName().getPath();

            int slot = 0;
            for (String ballId : POKEBALLS) {
                if (slot >= 27) break;
                PokeBall ball = PokeBalls.INSTANCE.getPokeBall(Identifier.of("cobblemon", ballId));
                
                // [FIX ESTÉTICO]: Si la ball es null, hacemos 'continue' SIN sumar al slot.
                // De esta manera no quedan huecos vacíos en la interfaz.
                if (ball == null) continue;

                boolean isCurrent = ballId.equals(current);

                Item ballItem = Registries.ITEM.getOrEmpty(Identifier.of("cobblemon", ballId))
                    .orElse(Items.SLIME_BALL);

                List<String> lore = new ArrayList<>();
                if (isCurrent) {
                    lore.add("&aPokéball actual");
                } else {
                    lore.add("&7Precio: &e" + EconomyManager.formatCost(price));
                    lore.add("&aClick para aplicar");
                }

                ItemStack stack = new ItemStack(ballItem);
                stack.set(DataComponentTypes.CUSTOM_NAME,
                    AdventureTranslator.toNative((isCurrent ? "&a" : "&f") + formatName(ballId)));
                stack.set(DataComponentTypes.LORE,
                    new LoreComponent(AdventureTranslator.toNativeL(lore)));

                final PokeBall finalBall = ball;
                template.set(slot++, GooeyButton.builder()
                    .display(stack)
                    .onClick(a -> {
                        if (isCurrent) return;
                        PokeBuilder.runAsync(() -> applyBall(player, pokemon, finalBall, price));
                    })
                    .build());
            }

            template.set(34, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(35, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> UIManager.closeUI(player))));

            GuiUtils.fillEmpty(template, 4);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Pokéball &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> UIManager.openUIForcefully(player, page));
        });
    }

    private static void applyBall(ServerPlayerEntity player, Pokemon pokemon, PokeBall ball, double price) {
        if (!EconomyManager.charge(player, price)) {
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgNotEnoughFunds(),
                "%cost%", EconomyManager.formatCost(price)));
            return;
        }
        
        PokeBuilder.server.execute(() -> {
            PokemonTags.markBuilt(pokemon);
            pokemon.setCaughtBall(ball);
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(price)));
            open(player, pokemon);
        });
    }

    private static String formatName(String id) {
        String[] parts = id.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> player.sendMessage(AdventureTranslator.toNative(msg)));
    }
}
