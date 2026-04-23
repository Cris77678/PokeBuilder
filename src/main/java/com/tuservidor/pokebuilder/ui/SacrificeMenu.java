package com.tuservidor.pokebuilder.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SacrificeMenu {

    private static final ConcurrentHashMap<UUID, UUID> pendingConfirm = new ConcurrentHashMap<>();

    public static void open(ServerPlayerEntity player) {
        PokeBuilder.server.execute(() -> {
            if (player.isRemoved()) return;
            
            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            List<Pokemon> snapshot = new ArrayList<>();
            for (int i = 0; i < 6; i++) snapshot.add(party.get(i));

            PokeBuilder.runAsync(() -> {
                ChestTemplate template = ChestTemplate.builder(4).build();

                GooeyButton fillerBlack = GuiUtils.filler(Items.BLACK_STAINED_GLASS_PANE);
                for (int i = 0; i < 9; i++) template.set(i, fillerBlack);
                for (int i = 27; i < 36; i++) template.set(i, fillerBlack);

                for (int slot = 0; slot < 6; slot++) {
                    Pokemon poke = snapshot.get(slot);
                    int guiSlot = 10 + slot;

                    if (poke == null) {
                        template.set(guiSlot, GuiUtils.filler(Items.LIGHT_GRAY_STAINED_GLASS_PANE));
                        continue;
                    }

                    boolean isFainted = poke.getCurrentHealth() <= 0;
                    List<String> blacklist = PokeBuilder.config.getBlacklistedSpecies();
                    boolean isBlacklisted = blacklist.contains(poke.getSpecies().showdownId()) || 
                                            blacklist.contains(poke.getSpecies().getName().toLowerCase());

                    if (poke.getSpecies().showdownId().equals("egg") || isBlacklisted || isFainted) {
                        var lockStack = PokemonItem.from(poke);
                        String displayName = poke.getSpecies().showdownId().equals("egg") ? "&eMisterioso Huevo" : "&c" + poke.getSpecies().getName();
                        
                        String denyReason = "&8No cumple los requisitos.";
                        if (poke.getSpecies().showdownId().equals("egg")) denyReason = "&8No puedes sacrificar un huevo.";
                        else if (isBlacklisted) denyReason = "&8Esta especie está prohibida en el sistema.";
                        else if (isFainted) denyReason = "&8Debes curarlo antes de sacrificarlo.";
                        
                        lockStack.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(displayName));
                        lockStack.set(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(List.of(
                            "&c✗ No elegible", denyReason
                        ))));
                        template.set(guiSlot, GooeyButton.builder().display(lockStack).onClick(a -> {
                            sendMsg(player, "&cEste Pokémon no puede ser sacrificado en este momento.");
                        }).build());
                        continue;
                    }

                    boolean isBuilt     = PokemonTags.isBuilt(poke);
                    boolean isShiny     = poke.getShiny();
                    boolean isLegendary = isLegendary(poke);
                    boolean eligible    = (isShiny || isLegendary) && !isBuilt;

                    double reward = calculateReward(isShiny, isLegendary);
                    boolean isPending = pendingConfirm.containsKey(player.getUuid())
                        && pendingConfirm.get(player.getUuid()).equals(poke.getUuid());

                    var stack = PokemonItem.from(poke);
                    List<String> lore = buildLore(poke, isBuilt, isShiny, isLegendary, eligible, reward, isPending);

                    String nameColor = isPending ? "&c" : eligible ? "&6" : "&7";
                    stack.set(DataComponentTypes.CUSTOM_NAME,
                        AdventureTranslator.toNative(nameColor + poke.getSpecies().getName()
                            + (isShiny ? " &e✦" : "")
                            + (isLegendary ? " &d★" : "")));
                    stack.set(DataComponentTypes.LORE,
                        new LoreComponent(AdventureTranslator.toNativeL(lore)));

                    if (!eligible) {
                        template.set(guiSlot, GooeyButton.builder().display(stack).onClick(a -> {
                            String msg = isBuilt
                                ? PokeBuilder.lang.getMsgSacrificeBuilt()
                                : PokeBuilder.lang.getMsgSacrificeNotEligible();
                            sendMsg(player, PokeBuilder.lang.format(msg));
                        }).build());
                    } else {
                        final Pokemon finalPoke = poke;
                        final double finalReward = reward;
                        template.set(guiSlot, GooeyButton.builder()
                            .display(stack)
                            .onClick(a -> PokeBuilder.runAsync(() -> {
                                if (isPending) {
                                    pendingConfirm.remove(player.getUuid());
                                    executeSacrifice(player, finalPoke, finalReward);
                                } else {
                                    pendingConfirm.put(player.getUuid(), finalPoke.getUuid());
                                    sendMsg(player, PokeBuilder.lang.format(
                                        PokeBuilder.lang.getMsgSacrificeConfirm(),
                                        "%pokemon%", finalPoke.getSpecies().getName()));
                                    open(player);
                                }
                            }))
                            .build());
                    }
                }

                if (pendingConfirm.containsKey(player.getUuid())) {
                    template.set(31, GuiUtils.button(Items.BARRIER, "&cCancelar sacrificio",
                        List.of("&7Cancela la confirmación pendiente."),
                        () -> {
                            pendingConfirm.remove(player.getUuid());
                            sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSacrificeCancelled()));
                            open(player);
                        }));
                } else {
                    var cfg = PokeBuilder.config;
                    template.set(31, GuiUtils.button(Items.BOOK, "&6Recompensas",
                        List.of(
                            "&e✦ Shiny: &f" + EconomyManager.formatCost(cfg.getSacrificeRewardShiny()),
                            "&d★ Legendario: &f" + EconomyManager.formatCost(cfg.getSacrificeRewardLegendary()),
                            "&e✦&d★ Shiny Legendario: &f" + EconomyManager.formatCost(
                                cfg.getSacrificeRewardShiny() + cfg.getSacrificeRewardLegendary() + cfg.getSacrificeRewardShinyLegendaryBonus()),
                            "",
                            "&8Los Pokémon editados con PokeBuilder",
                            "&8no son elegibles para sacrificio."
                        ), () -> {}));
                }

                template.set(35, GuiUtils.button(Items.ARROW, "&7← Cerrar",
                    List.of(), () -> PokeBuilder.server.execute(() -> {
                        if (!player.isRemoved()) UIManager.closeUI(player);
                    })));

                GuiUtils.fillEmpty(template, 4);

                GooeyPage page = GooeyPage.builder()
                    .template(template)
                    .title(AdventureTranslator.toNative("&4⚡ Sacrificio &8| &7Selecciona un Pokémon"))
                    .onClose(action -> pendingConfirm.remove(player.getUuid()))
                    .build();

                PokeBuilder.server.execute(() -> {
                    if (!player.isRemoved()) UIManager.openUIForcefully(player, page);
                });
            });
        });
    }

    private static void executeSacrifice(ServerPlayerEntity player, Pokemon pokemon, double reward) {
        PokeBuilder.server.execute(() -> {
            if (player.isRemoved()) return;

            if (PokemonTags.isBuilt(pokemon)) {
                sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSacrificeBuilt()));
                return;
            }

            var party = Cobblemon.INSTANCE.getStorage().getParty(player);
            boolean removed = party.remove(pokemon);
            
            if (!removed) {
                sendMsg(player, "&cError: El Pokémon ya no está en tu equipo (¿Lo moviste?). Transacción cancelada.");
                return;
            }

            PokeBuilder.runAsync(() -> {
                EconomyManager.give(player, reward);
                sendMsg(player, PokeBuilder.lang.format(PokeBuilder.lang.getMsgSacrificeSuccess(),
                    "%pokemon%", pokemon.getSpecies().getName(),
                    "%amount%", EconomyManager.formatCost(reward)));
                open(player);
            });
        });
    }

    private static double calculateReward(boolean isShiny, boolean isLegendary) {
        var cfg = PokeBuilder.config;
        double reward = 0;
        if (isShiny)     reward += cfg.getSacrificeRewardShiny();
        if (isLegendary) reward += cfg.getSacrificeRewardLegendary();
        if (isShiny && isLegendary) reward += cfg.getSacrificeRewardShinyLegendaryBonus();
        return reward;
    }

    private static boolean isLegendary(Pokemon pokemon) {
        return pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED)
            && pokemon.getSpecies().getLabels().contains("legendary");
    }

    private static List<String> buildLore(Pokemon pokemon, boolean isBuilt, boolean isShiny,
                                           boolean isLegendary, boolean eligible,
                                           double reward, boolean isPending) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Nivel: &f" + pokemon.getLevel());

        if (isShiny)     lore.add("&e✦ Shiny");
        if (isLegendary) lore.add("&d★ Legendario");

        lore.add("");

        if (isBuilt) {
            lore.add("&c✗ Modificado con PokeBuilder");
            lore.add("&8No elegible para sacrificio");
        } else if (!eligible) {
            lore.add("&8No es shiny ni legendario");
        } else if (isPending) {
            lore.add("&c⚠ Click de nuevo para CONFIRMAR");
            lore.add("&7Recibirás: &e" + EconomyManager.formatCost(reward));
            lore.add("&8Esta acción no se puede deshacer");
        } else {
            lore.add("&7Recompensa: &e" + EconomyManager.formatCost(reward));
            lore.add("&aClick para sacrificar");
        }
        return lore;
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        PokeBuilder.server.execute(() -> {
            if (!player.isRemoved()) {
                player.sendMessage(AdventureTranslator.toNative(msg));
            }
        });
    }
}