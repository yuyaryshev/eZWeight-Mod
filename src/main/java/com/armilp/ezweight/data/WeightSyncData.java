package com.armilp.ezweight.data;

public class WeightSyncData {
    private static double maxWeight = 60.0; // valor inicial por defecto
    private static boolean updated = false;

    public static void setMaxWeight(double newMax) {
        if (Double.compare(maxWeight, newMax) != 0) {
            maxWeight = newMax;
            updated = true;  // marca que hubo actualización
        }
    }

    public static double getMaxWeight() {
        return maxWeight;
    }

    public static boolean consumeUpdatedFlag() {
        boolean flag = updated;
        updated = false;
        return flag;
    }
}


