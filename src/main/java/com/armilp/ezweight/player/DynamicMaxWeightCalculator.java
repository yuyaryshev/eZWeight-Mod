package com.armilp.ezweight.player;

import com.armilp.ezweight.config.WeightConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;

public class DynamicMaxWeightCalculator {

    public static double calculate(Player player) {
        if (!WeightConfig.COMMON.USE_DYNAMIC_WEIGHT.get()) {
            return WeightConfig.COMMON.MAX_WEIGHT.get();
        }

        double baseMaxWeight = WeightConfig.COMMON.MAX_WEIGHT.get();
        double baseWeight = WeightConfig.COMMON.BASE_WEIGHT.get();

        int foodLevel = player.getFoodData().getFoodLevel();
        double foodFactor = foodLevel / 20.0;
        double weightFromFood = baseWeight + (baseMaxWeight - baseWeight) * foodFactor;

        boolean hasStrength = player.hasEffect(MobEffects.DAMAGE_BOOST);
        double strengthBonus = hasStrength ? 10.0 : 0.0;

        double crouchBonus = player.isCrouching() ? 5.0 : 0.0;

        double armorWeightPenalty = 0.0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    armorWeightPenalty += 2.5;
                }
            }
        }

        double dynamicWeight = weightFromFood + strengthBonus + crouchBonus - armorWeightPenalty;

        return Math.max(baseWeight, Math.min(dynamicWeight, baseMaxWeight));
    }
}

