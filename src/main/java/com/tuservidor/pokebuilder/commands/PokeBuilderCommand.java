package com.tuservidor.pokebuilder.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.tuservidor.pokebuilder.PokeBuilder;
import com.tuservidor.pokebuilder.economy.EconomyManager;
import com.tuservidor.pokebuilder.ui.SelectPokemonMenu;
import com.tuservidor.pokebuilder.ui.SacrificeMenu;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PokeBuilderCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        var base = CommandManager.literal("pokebuilder")
            .requires(src -> {
                if (!src.isExecutedByPlayer()) return true;
                return hasPermission(src.getPlayer(), "pokebuilder.use");
            })
            .executes(ctx -> {
                if (!ctx.getSource().isExecutedByPlayer()) return 0;
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) return 0;

                // [FIX CRÍTICO] Prevenir crasheos del servidor durante batallas
                if (isInBattle(player)) {
                    sendMsg(player, "&cNo puedes usar PokeBuilder mientras estás en combate.");
                    return 0;
                }

                PokeBuilder.runAsync(() -> SelectPokemonMenu.open(player));
                return 1;
            });

        base.then(CommandManager.literal("balance")
            .executes(ctx -> {
                if (!ctx.getSource().isExecutedByPlayer()) return 0;
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                PokeBuilder.runAsync(() -> {
                    sendMsg(player, "&7Tu saldo: &e" + EconomyManager.getBalanceFormatted(player));
                });
                return 1;
            })
        );

        base.then(CommandManager.literal("givecoin")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(1))
                    .executes(ctx -> {
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "player");
                        List<ServerPlayerEntity> targetsCopy = new ArrayList<>(targets);
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");
                        ServerCommandSource source = ctx.getSource();

                        PokeBuilder.runAsync(() -> {
                            for (ServerPlayerEntity target : targetsCopy) {
                                EconomyManager.give(target, amount);
                                sendMsg(target, PokeBuilder.lang.format(
                                    "&a+&e%amount% %coin% &arecibidos!",
                                    "%amount%", String.format("%.0f", amount),
                                    "%coin%", PokeBuilder.config.getCoinName()));
                            }
                            source.sendMessage(AdventureTranslator.toNative("&aMonedas dadas correctamente."));
                        });
                        return 1;
                    })
                )
            )
        );

        base.then(CommandManager.literal("removecoin")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(1))
                    .executes(ctx -> {
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "player");
                        List<ServerPlayerEntity> targetsCopy = new ArrayList<>(targets);
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");
                        ServerCommandSource source = ctx.getSource();

                        PokeBuilder.runAsync(() -> {
                            for (ServerPlayerEntity target : targetsCopy) {
                                double current = EconomyManager.getBalance(target);
                                EconomyManager.set(target.getUuid(), Math.max(0, current - amount));
                                sendMsg(target, PokeBuilder.lang.format(
                                    "&c-&e%amount% %coin% &cremovidos.",
                                    "%amount%", String.format("%.0f", amount),
                                    "%coin%", PokeBuilder.config.getCoinName()));
                            }
                            source.sendMessage(AdventureTranslator.toNative("&aMonedas removidas correctamente."));
                        });
                        return 1;
                    })
                )
            )
        );

        base.then(CommandManager.literal("setcoin")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(0))
                    .executes(ctx -> {
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "player");
                        List<ServerPlayerEntity> targetsCopy = new ArrayList<>(targets);
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");
                        ServerCommandSource source = ctx.getSource();

                        PokeBuilder.runAsync(() -> {
                            for (ServerPlayerEntity target : targetsCopy) {
                                EconomyManager.set(target.getUuid(), amount);
                                sendMsg(target, PokeBuilder.lang.format(
                                    "&7Tus &e%coin% &7fueron establecidas en &e%amount%&7.",
                                    "%amount%", String.format("%.0f", amount),
                                    "%coin%", PokeBuilder.config.getCoinName()));
                            }
                            source.sendMessage(AdventureTranslator.toNative("&aMonedas establecidas correctamente."));
                        });
                        return 1;
                    })
                )
            )
        );

        base.then(CommandManager.literal("reload")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .executes(ctx -> {
                PokeBuilder.reload();
                ctx.getSource().sendMessage(
                    AdventureTranslator.toNative(PokeBuilder.lang.getMsgReload()));
                return 1;
            })
        );

        dispatcher.register(base);

        dispatcher.register(CommandManager.literal("pb")
            .requires(base.getRequirement())
            .redirect(dispatcher.getRoot().getChild("pokebuilder")));

        dispatcher.register(CommandManager.literal("sacrifice")
            .requires(src -> {
                if (!src.isExecutedByPlayer()) return false;
                return hasPermission(src.getPlayer(), "pokebuilder.use");
            })
            .executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) return 0;

                // [FIX CRÍTICO] Prevenir eliminación de Pokémon mientras están luchando
                if (isInBattle(player)) {
                    sendMsg(player, "&cNo puedes sacrificar Pokémon mientras estás en combate.");
                    return 0;
                }

                PokeBuilder.runAsync(() -> SacrificeMenu.open(player));
                return 1;
            })
        );
    }

    private static boolean isInBattle(ServerPlayerEntity player) {
        return Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
    }

    private static boolean hasPermission(ServerPlayerEntity player, String perm) {
        if (player == null) return false;
        try {
            var lp = net.luckperms.api.LuckPermsProvider.get()
                .getUserManager().getUser(player.getUuid());
            if (lp != null) return lp.getCachedData().getPermissionData()
                .checkPermission(perm).asBoolean();
        } catch (Exception ignored) {}
        return true; 
    }

    private static boolean isAdmin(ServerCommandSource src) {
        if (!src.isExecutedByPlayer()) return true;
        return hasPermission(src.getPlayer(), "pokebuilder.admin");
    }

    private static void sendMsg(ServerPlayerEntity player, String msg) {
        if (player != null)
            player.sendMessage(AdventureTranslator.toNative(msg));
    }
}
