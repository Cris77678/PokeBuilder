package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.util.GuiUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class SelectPokemonMenu {

    public static void open(ServerPlayerEntity player) {
        PokeBuilder.server.execute(() -> {
            if (player.isRemoved()) return;
            
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            List<Pokemon> snapshot = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                snapshot.add(party.get(i));
            }

            PokeBuilder.runAsync(() -> {
                ChestTemplate template = ChestTemplate.builder(4).build();

                GooeyButton darkBorder = GuiUtils.filler(Items.BLACK_STAINED_GLASS_PANE);
                GooeyButton goldBorder = GuiUtils.filler(Items.YELLOW_STAINED_GLASS_PANE);

                for (int i = 0; i < 9; i++) template.set(i, i == 4 ? goldBorder : darkBorder);
                for (int i = 27; i < 36; i++) template.set(i, i == 31 ? goldBorder : darkBorder);

                for (int row = 1; row <= 2; row++) {
                    template.set(row * 9,     darkBorder);
                    template.set(row * 9 + 8, darkBorder);
                }

                int[] displaySlots = {10, 11, 12, 13, 14, 15};

                for (int slot = 0; slot < 6; slot++) {
                    Pokemon poke = snapshot.get(slot); 
                    int guiSlot = displaySlots[slot];

                    if (poke == null) {
                        template.set(guiSlot, GuiUtils.filler(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
                        continue;
                    }

                    if (poke.getSpecies().showdownId().equals("egg")) {
                        var eggStack = PokemonItem.from(poke);
                        eggStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative("&eMisterioso Huevo"));
                        eggStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
                            "&8━━━━━━━━━━━━━━━",
                            "&c✗ No puedes editar un huevo",
                            "&8Incúbalo primero para revelar al Pokémon.",
                            "&8━━━━━━━━━━━━━━━"
                        ))));
                        template.set(guiSlot, GooeyButton.builder().display(eggStack).onClick(a -> {}).build());
                        continue;
                    }

                    boolean isFainted = poke.getCurrentHealth() <= 0;
                    List<String> blacklist = PokeBuilder.config.getBlacklistedSpecies();
                    boolean blacklisted = blacklist.contains(poke.getSpecies().showdownId()) || 
                                          blacklist.contains(poke.getSpecies().getName().toLowerCase());

                    var stack = PokemonItem.from(poke);
                    List<String> lore = new ArrayList<>();
                    lore.add("&8━━━━━━━━━━━━━━━");
                    lore.add("&7Nivel:      &f" + poke.getLevel());
                    lore.add("&7Especie:    &f" + poke.getSpecies().getName());
                    lore.add("&7Naturaleza: &f" + poke.getNature().getName().getPath());
                    lore.add("&7Habilidad:  &f" + poke.getAbility().getName());
                    lore.add("&7Shiny:      " + (poke.getShiny() ? "&e✦ Sí" : "&7No"));
                    lore.add("&7Sexo:       " + formatGender(poke.getGender()));
                    lore.add("&8━━━━━━━━━━━━━━━");
                    
                    if (isFainted) {
                        lore.add("&c✗ El Pokémon está debilitado.");
                        lore.add("&c  ¡Cúralo antes de editarlo!");
                    } else if (blacklisted) {
                        lore.add("&c✗ No puede ser editado.");
                    } else {
                        lore.add("&a▶ Click para editar");
                    }

                    stack.set(DataComponentTypes.CUSTOM_NAME,
                        AdventureTranslator.toNative(
                            (isFainted ? "&c&l" : "&6&l") + poke.getSpecies().getName()
                            + (poke.getShiny() ? " &e✦" : "")
                        ));
                    stack.set(DataComponentTypes.LORE,
                        new LoreComponent(AdventureTranslator.toNativeL(lore)));

                    if (blacklisted || isFainted) {
                        template.set(guiSlot, GooeyButton.builder().display(stack).onClick(a -> {}).build());
                    } else {
                        final Pokemon finalPoke = poke;
                        template.set(guiSlot, GooeyButton.builder()
                            .display(stack)
                            .onClick(a -> PokeBuilder.runAsync(() ->
                                PokeBuilderMenu.open(a.getPlayer(), finalPoke)))
                            .build());
                    }
                }

                String balance = EconomyManager.getBalanceFormatted(player);
                template.set(22, GuiUtils.button(Items.SUNFLOWER,
                    "&6Saldo: &e" + balance,
                    List.of("&7" + PokeBuilder.config.getCoinName() + " para editar Pokémon"), () -> {}));

                template.set(26, GuiUtils.button(Items.BARRIER, "&cCerrar",
                    List.of("&7Cerrar el menú"),
                    () -> PokeBuilder.server.execute(() -> {
                        if (!player.isRemoved()) UIManager.closeUI(player);
                    })));

                GuiUtils.fillEmpty(template, 4);

                GooeyPage page = GooeyPage.builder()
                    .template(template)
                    .title(AdventureTranslator.toNative("&6&lPokeBuilder &8» &7Selecciona un Pokémon"))
                    .build();

                PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
                });
            });
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