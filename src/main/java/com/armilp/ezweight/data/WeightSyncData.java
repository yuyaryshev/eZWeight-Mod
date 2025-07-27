package com.armilp.ezweight.data;

import com.armilp.ezweight.player.DynamicMaxWeightCalculator;
import net.minecraft.world.entity.player.Player;

public class WeightSyncData {
    private static double lastSyncedMaxWeight = 60.0;
    private static boolean updated = false;

    public static void updateDynamicMaxWeight(Player player) {
        double newMax = DynamicMaxWeightCalculator.calculate(player);
        if (Double.compare(lastSyncedMaxWeight, newMax) != 0) {
            lastSyncedMaxWeight = newMax;
            updated = true;
        }
    }

    public static void setMaxWeight(double newMax) {
        if (Double.compare(lastSyncedMaxWeight, newMax) != 0) {
            lastSyncedMaxWeight = newMax;
            updated = true;
        }
    }

    public static double getMaxWeight() {
        return lastSyncedMaxWeight;
    }

    public static boolean consumeUpdatedFlag() {
        boolean flag = updated;
        updated = false;
        return flag;
    }
}
