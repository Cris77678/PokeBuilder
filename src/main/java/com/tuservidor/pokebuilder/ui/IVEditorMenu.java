package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
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

import java.util.List;

public class IVEditorMenu {

    private static final Stats[] STAT_ORDER = {
        Stats.HP, Stats.ATTACK, Stats.DEFENCE,
        Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED
    };

    private static final String[] STAT_NAMES = {
        "&c❤ HP", "&e⚔ Ataque", "&b🛡 Defensa",
        "&9✦ At.Esp.", "&5✦ Def.Esp.", "&a⚡ Velocidad"
    };

    private static final int[] STAT_SLOTS = {11, 12, 13, 14, 15, 16};

    public static void open(ServerPlayerEntity player, Pokemon pokemon) {
        PokeBuilder.runAsync(() -> {
            ChestTemplate template = ChestTemplate.builder(5).build();
            var cfg = PokeBuilder.config;
            double pricePerStat = cfg.effectivePrice(cfg.getPriceIvPerStat(), player);
            double pricePerfectConfig = cfg.effectivePrice(cfg.getPriceIvPerfect(), player);

            int totalMissingIvs = 0;
            for (Stats s : STAT_ORDER) {
                totalMissingIvs += (31 - pokemon.getIvs().getOrDefault(s));
            }
            
            double dynamicPerfectPrice = Math.min(pricePerfectConfig, Math.ceil(totalMissingIvs / 5.0) * pricePerStat);

            for (int i = 0; i < STAT_ORDER.length; i++) {
                Stats stat = STAT_ORDER[i];
                int currentIv = pokemon.getIvs().getOrDefault(stat);
                String statName = STAT_NAMES[i];
                int slot = STAT_SLOTS[i];

                var item = currentIv == 31 ? Items.EMERALD
                    : currentIv >= 20 ? Items.GOLD_INGOT : Items.IRON_INGOT;

                ItemStack stack = new ItemStack(item);
                stack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(statName));
                stack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
                    "&7IV Actual: &f" + currentIv + " &8/ 31",
                    "",
                    "&7[Click izq] &f+5 → &e" + EconomyManager.formatCost(pricePerStat),
                    "&7[Click der] &fPoner a 31 → &e" + EconomyManager.formatCost(pricePerStat * Math.ceil((31 - currentIv)/5.0))
                ))));

                final Stats finalStat = stat;
                final int finalCurrent = currentIv;
                template.set(slot, GooeyButton.builder()
                    .display(stack)
                    .onClick(action -> {
                        boolean rightClick = action.getClickType().toString().contains("RIGHT");
                        PokeBuilder.runAsync(() -> editIv(player, pokemon, finalStat, finalCurrent, pricePerStat, rightClick));
                    })
                    .build());
            }

            template.set(40, GuiUtils.button(Items.NETHER_STAR, "&e⭐ Todos los IVs a 31",
                List.of(
                    "&7Pone los 6 IVs a 31.",
                    "&7Precio Inteligente: &e" + EconomyManager.formatCost(dynamicPerfectPrice),
                    "", "&aClick para aplicar"
                ),
                () -> PokeBuilder.runAsync(() -> setAllPerfect(player, pokemon, dynamicPerfectPrice))));

            template.set(36, GuiUtils.button(Items.ARROW, "&7← Volver", List.of(),
                () -> PokeBuilderMenu.open(player, pokemon)));
            template.set(44, GuiUtils.button(Items.BARRIER, "&cCerrar", List.of(),
                () -> PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.closeUI(player);
                })));

            GuiUtils.fillEmpty(template, 5);

            GooeyPage page = GooeyPage.builder()
                .template(template)
                .title(AdventureTranslator.toNative("&6IVs &8| &7" + pokemon.getSpecies().getName()))
                .build();

            PokeBuilder.server.execute(() -> {
                if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
            });
        });
    }

    private static void editIv(ServerPlayerEntity player, Pokemon pokemon, Stats stat,
                                int current, double pricePerTick, boolean setMax) {
        
        if (current >= 31) {
            sendMsg(player, "&cEste IV ya está al máximo (31).");
            return;
        }

        int newVal = setMax ? 31 : Math.min(31, current + 5);
        double finalPrice = setMax ? Math.ceil((31 - current) / 5.0) * pricePerTick : pricePerTick;

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
            pokemon.getIvs().set(stat, newVal);
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(finalPrice)));
            open(player, pokemon);
        });
    }

    private static void setAllPerfect(ServerPlayerEntity player, Pokemon pokemon, double finalPrice) {
        boolean allMax = true;
        for (Stats s : STAT_ORDER) {
            if (pokemon.getIvs().getOrDefault(s) < 31) {
                allMax = false;
                break;
            }
        }
        if (allMax) {
            sendMsg(player, "&cEl Pokémon ya tiene todos los IVs perfectos.");
            return;
        }

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
            for (Stats stat : STAT_ORDER) pokemon.getIvs().set(stat, 31);
            
            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSuccess(),
                "%pokemon%", pokemon.getSpecies().getName(),
                "%cost%", EconomyManager.formatCost(finalPrice)));
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