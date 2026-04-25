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

import java.util.Collection;

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

                if (isInBattle(player)) {
                    sendMsg(player, "&cNo puedes usar PokeBuilder mientras estás en combate.");
                    return 0;
                }

                // CORRECCIÓN: Se abre el menú directamente en el hilo principal
                SelectPokemonMenu.open(player);
                return 1;
            });

        // Subcomando: balance
        base.then(CommandManager.literal("balance")
            .executes(ctx -> {
                if (!ctx.getSource().isExecutedByPlayer()) return 0;
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                // Los mensajes sí pueden ir en asíncrono
                PokeBuilder.runAsync(() -> {
                    sendMsg(player, "&7Tu saldo: &e" + EconomyManager.getBalanceFormatted(player));
                });
                return 1;
            })
        );

        // Subcomando: givecoin
        base.then(CommandManager.literal("givecoin")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .then(CommandManager.argument("player", EntityArgumentType.players())
                .then(CommandManager.argument("amount", DoubleArgumentType.doubleArg(1))
                    .executes(ctx -> {
                        Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "player");
                        double amount = DoubleArgumentType.getDouble(ctx, "amount");
                        ServerCommandSource source = ctx.getSource();

                        PokeBuilder.runAsync(() -> {
                            for (ServerPlayerEntity target : targets) {
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

        // Subcomando: reload
        base.then(CommandManager.literal("reload")
            .requires(src -> src.hasPermissionLevel(2) || isAdmin(src))
            .executes(ctx -> {
                PokeBuilder.reload();
                ctx.getSource().sendMessage(AdventureTranslator.toNative(PokeBuilder.lang.getMsgReload()));
                return 1;
            })
        );

        dispatcher.register(base);

        // Alias /pb
        dispatcher.register(CommandManager.literal("pb")
            .requires(base.getRequirement())
            .executes(base.getCommand())
            .redirect(dispatcher.getRoot().getChild("pokebuilder")));

        // Comando /sacrifice
        dispatcher.register(CommandManager.literal("sacrifice")
            .requires(src -> {
                if (!src.isExecutedByPlayer()) return false;
                return hasPermission(src.getPlayer(), "pokebuilder.use");
            })
            .executes(ctx -> {
                ServerPlayerEntity player = ctx.getSource().getPlayer();
                if (player == null) return 0;

                if (isInBattle(player)) {
                    sendMsg(player, "&cNo puedes sacrificar Pokémon mientras estás en combate.");
                    return 0;
                }

                // CORRECCIÓN: Se abre el menú directamente en el hilo principal
                SacrificeMenu.open(player);
                return 1;
            })
        );
    }

    private static boolean isInBattle(ServerPlayerEntity player) {
        return Cobblemon.INSTANCE.getBattleRegistry().getBattleByParticipatingPlayer(player) != null;
    }

    private static boolean hasPermission(ServerPlayerEntity player, String perm) {
        if (player == null) return false;
        if (player.hasPermissionLevel(2)) return true; // Fix para que el OP siempre pueda usarlo

        try {
            var lp = net.luckperms.api.LuckPermsProvider.get()
                .getUserManager().getUser(player.getUuid());
            if (lp != null) return lp.getCachedData().getPermissionData()
                .checkPermission(perm).asBoolean();
        } catch (Throwable ignored) {} 
        
        return player.hasPermissionLevel(0);
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
