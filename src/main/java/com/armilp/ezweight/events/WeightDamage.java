package com.armilp.ezweight.events;

import com.armilp.ezweight.commands.WeightCommands;
import com.armilp.ezweight.config.WeightConfig;
import com.armilp.ezweight.player.DynamicMaxWeightCalculator;
import com.armilp.ezweight.player.PlayerWeightHandler;
import com.armilp.ezweight.registry.WeightDamageSources;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class WeightDamage {

    private static final String TICK_COUNTER_TAG = "ezweight_damage_tick_counter";
    private static final int TICKS_PER_DAMAGE = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!WeightCommands.isWeightEnabledFor(serverPlayer)) return;

        double currentWeight = PlayerWeightHandler.getTotalWeight(player);
        double maxWeight = DynamicMaxWeightCalculator.calculate(player);

        if (!WeightConfig.COMMON.DAMAGE_OVERWEIGHT_ENABLED.get() || maxWeight <= 0.0) {
            resetTickCounter(player);
            return;
        }

        double overweightRatio = currentWeight / maxWeight;
        if (!isOverThreshold(overweightRatio)) {
            resetTickCounter(player);
            return;
        }

        CompoundTag data = player.getPersistentData();
        int ticksOverweight = data.getInt(TICK_COUNTER_TAG) + 1;

        if (ticksOverweight >= TICKS_PER_DAMAGE) {
            ticksOverweight = 0;
            float damage = calculateDamage(currentWeight, maxWeight);
            if (damage > 0.0f) {
                DamageSource source = WeightDamageSources.overweight(player.level().registryAccess());
                player.hurt(source, damage);
                serverPlayer.displayClientMessage(
                        Component.translatable("message.ezweight.overweight_damage").withStyle(ChatFormatting.DARK_RED),
                        true
                );
            }
        }

        data.putInt(TICK_COUNTER_TAG, ticksOverweight);
    }

    private static void resetTickCounter(Player player) {
        player.getPersistentData().putInt(TICK_COUNTER_TAG, 0);
    }

    private static boolean isOverThreshold(double overweightRatio) {
        List<? extends String> thresholds = WeightConfig.COMMON.DAMAGE_OVERWEIGHT_THRESHOLDS.get();
        for (String str : thresholds) {
            try {
                double threshold = Double.parseDouble(str);
                if (overweightRatio >= threshold) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    private static float calculateDamage(double currentWeight, double maxWeight) {
        if (!WeightConfig.COMMON.DAMAGE_OVERWEIGHT_ENABLED.get()) {
            return 0.0f;
        }

        return WeightConfig.COMMON.DAMAGE_PER_SECOND.get().floatValue();
    }
}
