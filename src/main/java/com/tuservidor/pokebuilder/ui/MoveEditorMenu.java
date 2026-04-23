package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
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

import java.util.ArrayList;
import java.util.List;

public class MoveEditorMenu {

    private static final int MOVES_PER_PAGE = 45;

    public static void open(ServerPlayerEntity player, Pokemon pokemon, int page) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double price = cfg.effectivePrice(cfg.getPriceMove(), player);

            var moveSet = pokemon.getMoveSet();
            int[] slots = {11, 12, 13, 14};
            for (int i = 0; i < 4; i++) {
                Move move = moveSet.get(i);
                int moveSlot = i;
                String moveName = move != null ? move.getTemplate().getName() : "&8(vacío)";

                List<String> lore = new ArrayList<>();
                if (move != null) lore.add("&7Tipo: &f" + move.getTemplate().getElementalType().getName());
                lore.add("&7Precio: &e" + EconomyManager.formatCost(price));
                lore.add("");
                lore.add("&aClick para cambiar");

                ItemStack stack = new ItemStack(Items.BOOK);
                stack.set(DataComponentTypes.CUSTOM_NAME,
                    AdventureTranslator.toNative("&6Slot " + (i + 1) + ": &f" + moveName));
                stack.set(DataComponentTypes.LORE,
                    new LoreComponent(AdventureTranslator.toNativeL(lore)));

                template.set(slots[i], GooeyButton.builder()
                    .display(stack)
                    .onClick(a -> PokeBuilder.runAsync(() ->
                        openMovePicker(player, pokemon, moveSlot, 0)))
                    .build());
            }

            template.set(25, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> UIManager.closeUI(player))));

            GuiUtils.fillEmpty(template, 3);

            GooeyPage gooeyPage = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Movimientos &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> UIManager.openUIForcefully(player, gooeyPage));
        });
    }

    private static void openMovePicker(ServerPlayerEntity player, Pokemon pokemon, int slot, int position) {
        PokeBuilder.runAsync(() -> {
            var cfg = PokeBuilder.config;
            double price = cfg.effectivePrice(cfg.getPriceMove(), player);

            List<MoveTemplate> learnable = new ArrayList<>();
            
            // [FIX CRÍTICO]: Recolección COMPLETA del Movepool biológico (Nivel, TMs, Huevo, Tutor)
            for (java.util.List<MoveTemplate> moveList : pokemon.getForm().getMoves().getLevelUpMoves().values()) {
                learnable.addAll(moveList);
            }
            learnable.addAll(pokemon.getForm().getMoves().getTmMoves());
            learnable.addAll(pokemon.getForm().getMoves().getEggMoves());
            learnable.addAll(pokemon.getForm().getMoves().getTutorMoves());

            // Filtra duplicados y los ordena alfabéticamente
            learnable = learnable.stream().distinct()
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toList();

            ChestTemplate template = ChestTemplate.builder(6).build();
            int start = position, end = position + MOVES_PER_PAGE;
            int idx = 0;
            int guiSlot = 0; 

            for (MoveTemplate move : learnable) {
                if (idx >= start && idx < end) {
                    final MoveTemplate finalMove = move;
                    List<String> lore = List.of(
                        "&7Tipo: &f" + move.getElementalType().getName(),
                        "&7Poder: &f" + move.getPower(),
                        "",
                        "&7Precio: &e" + EconomyManager.formatCost(price),
                        "&aClick para asignar al slot " + (slot + 1)
                    );
                    ItemStack stack = new ItemStack(Items.PAPER);
                    stack.set(DataComponentTypes.CUSTOM_NAME,
                        AdventureTranslator.toNative("&f" + move.getName()));
                    stack.set(DataComponentTypes.LORE,
                        new LoreComponent(AdventureTranslator.toNativeL(lore)));

                    template.set(guiSlot++, GooeyButton.builder()
                        .display(stack)
                        .onClick(a -> PokeBuilder.runAsync(() ->
                            applyMove(player, pokemon, slot, finalMove, price)))
                        .build());
                }
                idx++;
            }

            for (int i = 45; i < 54; i++) template.set(i, GuiUtils.filler(Items.BLACK_STAINED_GLASS_PANE));

            final int finalIdx = idx;

            if (position > 0) {
                template.set(45, GuiUtils.button(Items.ARROW, "&6← Anterior", List.of(),
                    () -> openMovePicker(player, pokemon, slot, position - MOVES_PER_PAGE)));
            }
            template.set(49, GuiUtils.button(Items.ARROW, "&7← Volver a slots", List.of(),
                () -> open(player, pokemon, 0)));
            
            if (finalIdx > end) {
                template.set(53, GuiUtils.button(Items.ARROW, "&6Siguiente →", List.of(),
                    () -> openMovePicker(player, pokemon, slot, position + MOVES_PER_PAGE)));
            }

            GooeyPage gooeyPage = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6Elige movimiento &8| Slot " + (slot + 1)))
                .build();

            PokeBuilder.server.execute(() -> UIManager.openUIForcefully(player, gooeyPage));
        });
    }

    private static void applyMove(ServerPlayerEntity player, Pokemon pokemon,
                                   int slot, MoveTemplate moveTemplate, double price) {
        
        boolean knowsMove = false;
        for (int i = 0; i < 4; i++) {
            Move currentMove = pokemon.getMoveSet().get(i);
            if (currentMove != null && currentMove.getTemplate().getName().equals(moveTemplate.getName())) {
                knowsMove = true;
                break;
            }
        }
        
        if (knowsMove) {
            sendMsg(player, "&cEl Pokémon ya conoce este movimiento.");
            return;
        }

        if (!EconomyManager.charge(player, price)) {
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgNotEnoughFunds(),
                "%cost%", EconomyManager.formatCost(price)));
            return;
        }
        
        PokeBuilder.server.execute(() -> {
            PokemonTags.markBuilt(pokemon);
            pokemon.getMoveSet().setMove(slot, moveTemplate.create());
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(price)));
            open(player, pokemon, 0);
        });
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> player.sendMessage(AdventureTranslator.toNative(msg)));
    }
}
