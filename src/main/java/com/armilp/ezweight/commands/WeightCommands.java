package com.armilp.ezweight.commands;

import com.armilp.ezweight.EZWeight;
import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.player.PlayerWeightHandler;
import com.armilp.ezweight.network.EZWeightNetwork;
import com.armilp.ezweight.network.gui.OpenWeightGuiPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EZWeight.MODID)
public class WeightCommands {

    private static final Set<UUID> disabledPlayers = new HashSet<>();
    private static boolean allDisabled = false;

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("ezweight")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("gui")
                                .executes(context -> {
                                    CommandSourceStack src = context.getSource();
                                    ServerPlayer player = src.getPlayerOrException();
                                    EZWeightNetwork.sendToPlayer(new OpenWeightGuiPacket(), player);
                                    src.sendSuccess(() -> Component.translatable("message.ezweight.gui_open"), true);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("toggle")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> {
                                            CommandSourceStack src = context.getSource();
                                            String playerName = StringArgumentType.getString(context, "player");

                                            ServerPlayer player = src.getServer().getPlayerList().getPlayerByName(playerName);
                                            if (player == null) {
                                                src.sendFailure(Component.translatable("message.ezweight.player_not_found", playerName));
                                                return 0;
                                            }

                                            UUID uuid = player.getUUID();

                                            if (disabledPlayers.contains(uuid)) {
                                                disabledPlayers.remove(uuid);
                                                src.sendSuccess(() -> Component.translatable("message.ezweight.toggle_enabled", playerName), true);
                                            } else {
                                                disabledPlayers.add(uuid);
                                                src.sendSuccess(() -> Component.translatable("message.ezweight.toggle_disabled", playerName), true);
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                        .then(Commands.literal("toggleall")
                                .executes(context -> {
                                    CommandSourceStack src = context.getSource();

                                    allDisabled = !allDisabled;

                                    src.sendSuccess(() -> Component.translatable(allDisabled ?
                                            "message.ezweight.toggle_all_disabled" :
                                            "message.ezweight.toggle_all_enabled"), true);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("info")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(context -> {
                                            CommandSourceStack src = context.getSource();
                                            String playerName = StringArgumentType.getString(context, "player");

                                            ServerPlayer player = src.getServer().getPlayerList().getPlayerByName(playerName);
                                            if (player == null) {
                                                src.sendFailure(Component.translatable("message.ezweight.player_not_found", playerName));
                                                return 0;
                                            }

                                            double weight = PlayerWeightHandler.getTotalWeight(player);
                                            src.sendSuccess(() -> Component.translatable("message.ezweight.player_info",
                                                    playerName,
                                                    String.format("%.1f", weight),
                                                    String.format("%.1f", WeightConfig.COMMON.MAX_WEIGHT.get())
                                            ), true);
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
        );
    }

    public static boolean isWeightEnabledFor(ServerPlayer player) {
        if (!WeightConfig.COMMON.NO_JUMP_WEIGHT_ENABLED.get()
                && !WeightConfig.COMMON.DAMAGE_OVERWEIGHT_ENABLED.get()
                && !WeightConfig.COMMON.FORCE_SNEAK_ENABLED.get()) {
            return false;
        }
        if (allDisabled) return false;

        return !disabledPlayers.contains(player.getUUID());
    }
}

