package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
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

public class AbilityEditorMenu {

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(3).build();
            var cfg = PokeBuilder.config;
            double priceNormal = cfg.effectivePrice(cfg.getPriceAbility(), player);
            double priceHA = cfg.effectivePrice(cfg.getPriceHiddenAbility(), player);

            List<PotentialAbility> abilities = new ArrayList<>();
            var abilityIterator = pokemon.getForm().getAbilities().iterator();
            while (abilityIterator.hasNext()) {
                abilities.add(abilityIterator.next());
            }

            String currentAbilityName = pokemon.getAbility().getName();

            int slot = 10;
            for (PotentialAbility potential : abilities) {
                boolean isHA = potential.getType() instanceof HiddenAbilityType;
                boolean isCurrent = potential.getTemplate().getName().equals(currentAbilityName);
                double price = isHA ? priceHA : priceNormal;

                List<String> lore = new ArrayList<>();
                lore.add(isHA ? "&6[Habilidad Oculta]" : "&7[Habilidad Normal]");
                lore.add("");
                if (isCurrent) {
                    lore.add("&aHabilidad actual");
                } else {
                    lore.add("&7Precio: &e" + EconomyManager.formatCost(price));
                    lore.add("&aClick para aplicar");
                }

                var item = isCurrent ? (isHA ? Items.GOLDEN_APPLE : Items.APPLE)
                    : (isHA ? Items.BLAZE_POWDER : Items.GUNPOWDER);

                ItemStack stack = new ItemStack(item);
                stack.set(DataComponentTypes.CUSTOM_NAME,
                    AdventureTranslator.toNative((isCurrent ? "&a" : isHA ? "&6" : "&f")
                        + potential.getTemplate().getDisplayName()));
                stack.set(DataComponentTypes.LORE,
                    new LoreComponent(AdventureTranslator.toNativeL(lore)));

                final PotentialAbility finalPotential = potential;
                final double finalPrice = price;
                template.set(slot++, GooeyButton.builder()
                    .display(stack)
                    .onClick(a -> {
                        if (isCurrent) return;
                        PokeBuilder.runAsync(() -> applyAbility(player, pokemon, finalPotential, finalPrice));
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
                .title(AdventureTranslator.toNative("&6Habilidad &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }

    private static void applyAbility(ServerPlayerEntity player, Pokemon pokemon,
                                     PotentialAbility potential, double price) {
        
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
            pokemon.updateAbility(potential.getTemplate().create(true, Priority.HIGHEST));
            
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