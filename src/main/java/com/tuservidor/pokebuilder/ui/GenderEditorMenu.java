package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.util.GuiUtils;
import com.tuservidor.pokebuilder.util.PokemonTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

public class GenderEditorMenu {

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double cost = cfg.effectivePrice(cfg.getPriceGender(), player);

            Gender currentGender = pokemon.getGender();
            float maleRatio = pokemon.getForm().getMaleRatio(); // Obtiene la distribución de género natural

            var pokeStack = PokemonItem.from(pokemon);
            pokeStack.set(DataComponentTypes.CUSTOM_NAME,
                AdventureTranslator.toNative("&6" + pokemon.getSpecies().getName()
                    + (pokemon.getShiny() ? " &e✦" : "")));
            pokeStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
                "&7Sexo actual: " + formatGender(currentGender),
                "",
                "&7Saldo: &e" + EconomyManager.getBalanceFormatted(player)
            ))));
            
            template.set(4, ca.landonjw.gooeylibs2.api.button.GooeyButton.builder()
                .display(pokeStack).onClick(a -> {}).build());

            // [FIX CRÍTICO]: Validaciones de género bloqueado biológicamente.
            // ratio < 0 = GENDERLESS, ratio == 1.0 = 100% Macho, ratio == 0.0 = 100% Hembra
            if (maleRatio < 0) {
                template.set(13, GuiUtils.button(Items.BARRIER, "&cSin género",
                    List.of("&7Esta especie no tiene sexo biológico."), () -> {}));
                    
            } else if (maleRatio >= 1.0f) {
                template.set(13, GuiUtils.button(Items.LIGHT_BLUE_WOOL, "&b♂ Macho (100%)",
                    List.of("&7Esta especie está limitada solo a Machos.", "&cNo se puede cambiar."), () -> {}));
                    
            } else if (maleRatio <= 0.0f) {
                template.set(13, GuiUtils.button(Items.PINK_WOOL, "&d♀ Hembra (100%)",
                    List.of("&7Esta especie está limitada solo a Hembras.", "&cNo se puede cambiar."), () -> {}));
                    
            } else {
                // Si la especie admite ambos géneros (la gran mayoría)
                boolean isMale = currentGender == Gender.MALE;
                template.set(11, GuiUtils.button(
                    isMale ? Items.LIGHT_BLUE_WOOL : Items.LIGHT_BLUE_STAINED_GLASS_PANE,
                    isMale ? "&b♂ Macho &7(actual)" : "&b♂ Cambiar a Macho",
                    List.of(
                        isMale ? "&7Ya es macho." : "&7Precio: &e" + EconomyManager.formatCost(cost),
                        "", isMale ? "&7Sin costo" : "&aClick para cambiar"
                    ),
                    () -> {
                        if (!isMale) PokeBuilder.runAsync(() -> setGender(player, pokemon, Gender.MALE, cost));
                    }
                ));

                boolean isFemale = currentGender == Gender.FEMALE;
                template.set(15, GuiUtils.button(
                    isFemale ? Items.PINK_WOOL : Items.PINK_STAINED_GLASS_PANE,
                    isFemale ? "&d♀ Hembra &7(actual)" : "&d♀ Cambiar a Hembra",
                    List.of(
                        isFemale ? "&7Ya es hembra." : "&7Precio: &e" + EconomyManager.formatCost(cost),
                        "", isFemale ? "&7Sin costo" : "&aClick para cambiar"
                    ),
                    () -> {
                        if (!isFemale) PokeBuilder.runAsync(() -> setGender(player, pokemon, Gender.FEMALE, cost));
                    }
                ));
            }

            template.set(18, GuiUtils.button(Items.ARROW, "&7← Volver",
                List.of(), () -> PokeBuilderMenu.open(player, pokemon)));

            template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar",
                List.of(), () -> PokeBuilder.server.execute(() -> UIManager.closeUI(player))));

            GuiUtils.fillEmpty(template, 3);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&5Cambiar Sexo &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> UIManager.openUIForcefully(player, page));
        });
    }

    private static void setGender(ServerPlayerEntity player, Pokemon pokemon, Gender gender, double cost) {
        if (!EconomyManager.charge(player, cost)) {
            PokeBuilder.server.execute(() -> player.sendMessage(
                AdventureTranslator.toNative(PokeBuilder.lang.format(
                    PokeBuilder.lang.getMsgNotEnoughFunds(), "%cost%", EconomyManager.formatCost(cost)))));
            return;
        }
        
        PokeBuilder.server.execute(() -> {
            PokemonTags.markBuilt(pokemon);
            pokemon.setGender(gender);
            String genderName = gender == Gender.MALE ? "♂ Macho" : "♀ Hembra";
            player.sendMessage(AdventureTranslator.toNative("&aEl sexo de &6" + pokemon.getSpecies().getName()
                    + " &aha sido cambiado a &f" + genderName));
            open(player, pokemon);
        });
    }

    private static String formatGender(Gender gender) {
        return switch (gender) {
            case MALE -> "&b♂ Macho";
            case FEMALE -> "&d♀ Hembra";
            case GENDERLESS -> "&7Sin género";
        };
    }
}
