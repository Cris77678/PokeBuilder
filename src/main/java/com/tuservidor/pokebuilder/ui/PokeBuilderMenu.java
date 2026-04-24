package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.util.GuiUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class PokeBuilderMenu {

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(4).build();

            // MOSTRAR LA FIGURA DEL POKÉMON COMO OBJETO EN EL SLOT 4
            var pokeStack = PokemonItem.from(pokemon);
            pokeStack.set(DataComponentTypes.CUSTOM_NAME, 
                AdventureTranslator.toNative("&6&l" + pokemon.getSpecies().getName()));
            
            template.set(4, ca.landonjw.gooeylibs2.api.button.GooeyButton.builder()
                .display(pokeStack).onClick(a -> {}).build());

            // Botones de edición
            template.set(10, GuiUtils.button(Items.PAPER, "&aNaturaleza", List.of("&7Cambiar la naturaleza"), () -> NatureEditorMenu.open(player, pokemon)));
            template.set(11, GuiUtils.button(Items.BLAZE_POWDER, "&6Habilidad", List.of("&7Cambiar la habilidad"), () -> AbilityEditorMenu.open(player, pokemon)));
            template.set(12, GuiUtils.button(Items.DIAMOND, "&bIVs", List.of("&7Mejorar los IVs"), () -> IVEditorMenu.open(player, pokemon)));
            template.set(13, GuiUtils.button(Items.EXPERIENCE_BOTTLE, "&eNivel", List.of("&7Subir de nivel"), () -> LevelEditorMenu.open(player, pokemon)));
            template.set(14, GuiUtils.button(Items.BOOK, "&9Movimientos", List.of("&7Enseñar movimientos"), () -> MoveEditorMenu.open(player, pokemon, 0)));
            template.set(15, GuiUtils.button(Items.SLIME_BALL, "&cPokéball", List.of("&7Cambiar Pokéball"), () -> PokeballEditorMenu.open(player, pokemon)));
            template.set(16, GuiUtils.button(Items.PINK_DYE, "&dSexo", List.of("&7Cambiar género"), () -> GenderEditorMenu.open(player, pokemon)));
            
            template.set(20, GuiUtils.button(Items.GOLD_NUGGET, "&e✦ Shiny", List.of("&7Convertir a Shiny"), () -> ShinyEditorMenu.open(player, pokemon)));
            template.set(21, GuiUtils.button(Items.SLIME_BLOCK, "&2Tamaño", List.of("&7Cambiar tamaño"), () -> SizeEditorMenu.open(player, pokemon)));

            template.set(31, GuiUtils.button(Items.ARROW, "&7← Volver a Selección", List.of(), () -> SelectPokemonMenu.open(player)));
            template.set(35, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(), () -> PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.closeUI(player);
            })));

            GuiUtils.fillEmpty(template, 4);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6PokeBuilder &8| &7Menú Principal"))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }
}
