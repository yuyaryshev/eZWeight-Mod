package com.armilp.ezweight.config;
import com.armilp.ezweight.player.DynamicMaxWeightCalculator;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

public class WeightConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;

    static {
        // --- COMMON ---
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        COMMON = new Common(commonBuilder);
        COMMON_SPEC = commonBuilder.build();

        // --- CLIENT ---
        final Pair<Client, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = pair.getLeft();
        CLIENT_SPEC = pair.getRight();
    }

    public static class Common {
        public final ForgeConfigSpec.DoubleValue BASE_WEIGHT;
        public final ForgeConfigSpec.DoubleValue MAX_WEIGHT;
        public final ForgeConfigSpec.BooleanValue USE_DYNAMIC_WEIGHT;
        public final ForgeConfigSpec.BooleanValue DYN_FOOD_ENABLED;
        public final ForgeConfigSpec.DoubleValue DYN_FOOD_INFLUENCE_MULTIPLIER;
        public final ForgeConfigSpec.BooleanValue DYN_STRENGTH_ENABLED;
        public final ForgeConfigSpec.DoubleValue DYN_STRENGTH_BONUS;
        public final ForgeConfigSpec.BooleanValue DYN_CROUCH_ENABLED;
        public final ForgeConfigSpec.DoubleValue DYN_CROUCH_BONUS;
        public final ForgeConfigSpec.BooleanValue DYN_ARMOR_PENALTY_ENABLED;
        public final ForgeConfigSpec.DoubleValue DYN_ARMOR_PENALTY_PER_PIECE;

        public final ForgeConfigSpec.BooleanValue NO_JUMP_WEIGHT_ENABLED;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> NO_JUMP_WEIGHT_RANGES;
        public final ForgeConfigSpec.BooleanValue DAMAGE_OVERWEIGHT_ENABLED;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> DAMAGE_OVERWEIGHT_THRESHOLDS;
        public final ForgeConfigSpec.DoubleValue DAMAGE_PER_SECOND;
        public final ForgeConfigSpec.BooleanValue FORCE_SNEAK_ENABLED;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCE_SNEAK_WEIGHT_RANGES;

        public final ForgeConfigSpec.BooleanValue ATTACHMENT_WEIGHT_ENABLED;
        public final ForgeConfigSpec.DoubleValue ATTACHMENT_WEIGHT_MULTIPLIER;
        public final ForgeConfigSpec.BooleanValue AMMO_WEIGHT_ENABLED;
        public final ForgeConfigSpec.DoubleValue AMMO_WEIGHT_PER_ROUND;
        public final ForgeConfigSpec.DoubleValue AMMO_WEIGHT_MULTIPLIER;

        public final ForgeConfigSpec.BooleanValue ITEM_TOOLTIP_ENABLED;
        public final ForgeConfigSpec.ConfigValue<String> ITEM_TOOLTIP_COLOR_BASE;
        public final ForgeConfigSpec.ConfigValue<String> ITEM_TOOLTIP_COLOR_TOTAL;


        public Common(ForgeConfigSpec.Builder builder) {
            builder.push("General");
            BASE_WEIGHT = builder
                    .comment("Default player body weight (kilograms).")
                    .defineInRange("base_weight", 60.0, 30.0, 200.0);

            MAX_WEIGHT = builder
                    .comment("The base maximum weight a player can carry. (Kilograms).")
                    .defineInRange("max_weight", 140.0, 0.0, 1000.0);

            USE_DYNAMIC_WEIGHT = builder
                    .comment("If true, uses dynamic weight calculations (base weight, food, strength, etc.). If false, only uses max_weight.")
                    .define("use_dynamic_weight", false);

            ITEM_TOOLTIP_ENABLED = builder
                    .comment("Enable or disable displaying item weight tooltips.")
                    .define("item_tooltip_enabled", true);
            ITEM_TOOLTIP_COLOR_BASE = builder
                    .comment("Color for base weight in item tooltip.")
                    .define("item_tooltip_color_base", "#FFD700");  // Color dorado por defecto
            ITEM_TOOLTIP_COLOR_TOTAL = builder
                    .comment("Color for total weight in item tooltip.")
                    .define("item_tooltip_color_total", "#FFFF00"); // Color amarillo por defecto

            builder.pop();

            builder.push("DynamicWeight");
            DYN_FOOD_ENABLED = builder
                    .comment("If true, food level affects max carry weight.")
                    .define("food_enabled", true);
            DYN_FOOD_INFLUENCE_MULTIPLIER = builder
                    .comment("Multiplier for food influence (0.0 disables its effect while enabled). 1.0 = default.")
                    .defineInRange("food_influence_multiplier", 1.0, 0.0, 5.0);

            DYN_STRENGTH_ENABLED = builder
                    .comment("If true, Strength effect grants a flat bonus.")
                    .define("strength_enabled", true);
            DYN_STRENGTH_BONUS = builder
                    .comment("Flat bonus applied when Strength effect is active.")
                    .defineInRange("strength_bonus", 10.0, -1000.0, 1000.0);

            DYN_CROUCH_ENABLED = builder
                    .comment("If true, crouching grants a flat bonus.")
                    .define("crouch_enabled", true);
            DYN_CROUCH_BONUS = builder
                    .comment("Flat bonus applied while crouching.")
                    .defineInRange("crouch_bonus", 5.0, -1000.0, 1000.0);

            DYN_ARMOR_PENALTY_ENABLED = builder
                    .comment("If true, each worn armor piece applies a penalty.")
                    .define("armor_penalty_enabled", true);
            DYN_ARMOR_PENALTY_PER_PIECE = builder
                    .comment("Penalty per worn armor piece.")
                    .defineInRange("armor_penalty_per_piece", 2.5, -1000.0, 1000.0);
            builder.pop();

            builder.push("NoJump");
            NO_JUMP_WEIGHT_ENABLED = builder
                    .define("no_jump_weight_enabled", true);
            NO_JUMP_WEIGHT_RANGES = builder
                    .comment("Ranges [min, max]; use 'max' for player max weight.")
                    .defineList("no_jump_weight_ranges",
                            Arrays.asList("95", "max"),
                            val -> isValidDoubleOrMax(val));
            builder.pop();

            builder.push("DamageOverweight");
            DAMAGE_OVERWEIGHT_ENABLED = builder
                    .define("damage_overweight_enabled", true);
            DAMAGE_OVERWEIGHT_THRESHOLDS = builder
                    .comment("List of weight percent thresholds (0.0 - 1.0) where damage should start being applied. Example: ['0.8']")
                    .defineList("damage_overweight_thresholds",
                            Arrays.asList("0.8"),
                            val -> {
                                if (!(val instanceof String s)) return false;
                                try {
                                    double d = Double.parseDouble(s);
                                    return d >= 0.0 && d <= 1.0;
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            });

            DAMAGE_PER_SECOND = builder
                    .comment("Damage per second while overweight.")
                    .defineInRange("damage_per_second", 1.4, 0.0, 100.0);
            builder.pop();

            builder.push("ForceSneak");
            FORCE_SNEAK_ENABLED = builder
                    .define("force_sneak_enabled", false);
            FORCE_SNEAK_WEIGHT_RANGES = builder
                    .comment("Ranges [min, max]; use 'max' for player max weight.")
                    .defineList("force_sneak_weight_ranges",
                            Arrays.asList("115", "max"),
                            val -> isValidDoubleOrMax(val));
            builder.pop();

            builder.push("TACZGunWeight");
            ATTACHMENT_WEIGHT_ENABLED = builder
                    .comment("If true, includes attachments' configured weight in gun total weight.")
                    .define("attachment_weight_enabled", true);
            ATTACHMENT_WEIGHT_MULTIPLIER = builder
                    .comment("Multiplier applied to summed attachment weight.")
                    .defineInRange("attachment_weight_multiplier", 1.0, 0.0, 20.0);

            AMMO_WEIGHT_ENABLED = builder
                    .comment("If true, includes ammo weight in gun total weight.")
                    .define("ammo_weight_enabled", true);
            AMMO_WEIGHT_PER_ROUND = builder
                    .comment("Default weight per single bullet/round.")
                    .defineInRange("ammo_weight_per_round", 0.02, 0.0, 1.0);
            AMMO_WEIGHT_MULTIPLIER = builder
                    .comment("Multiplier applied to computed ammo weight (rounds * per_round).")
                    .defineInRange("ammo_weight_multiplier", 1.0, 0.0, 20.0);
            builder.pop();
        }

        private static boolean isValidDoubleOrMax(Object val) {
            if (val instanceof String s) {
                if (s.equalsIgnoreCase("max")) return true;
                try {
                    Double.parseDouble(s);
                    return true;
                } catch (NumberFormatException ignored) {}
            }
            return false;
        }
    }

    public static double parseWeightValue(String s) {
        if (s.equalsIgnoreCase("max")) {
            return COMMON.MAX_WEIGHT.get();
        }

        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static double parseWeightValue(String s, Player player) {
        if (s.equalsIgnoreCase("max")) {
            if (player != null) {
                return DynamicMaxWeightCalculator.calculate(player);
            }
            return COMMON.MAX_WEIGHT.get();
        }

        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public static double getProgressiveStartWeight() {
        List<? extends String> ranges = COMMON.DAMAGE_OVERWEIGHT_THRESHOLDS.get();
        if (!ranges.isEmpty()) {
            return parseWeightValue(ranges.get(0));
        }
        return 0.0;
    }


    public static class Client {
        public final ForgeConfigSpec.EnumValue<InventoryAnchor> MAIN_HUD_ANCHOR;
        public final ForgeConfigSpec.IntValue MAIN_HUD_OFFSET_X;
        public final ForgeConfigSpec.IntValue MAIN_HUD_OFFSET_Y;

        public final ForgeConfigSpec.IntValue MINI_HUD_X;
        public final ForgeConfigSpec.IntValue MINI_HUD_Y;
        public final ForgeConfigSpec.IntValue MINI_HUD_ICON_SIZE;

        public Client(ForgeConfigSpec.Builder builder) {
            builder.push("MainHUD Position");
            MAIN_HUD_ANCHOR = builder
                    .comment("Anchor position of the main weight HUD box relative to the inventory screen. Options: TOP, LEFT, RIGHT, BOTTOM")
                    .defineEnum("mainHudAnchor", InventoryAnchor.LEFT);
            MAIN_HUD_OFFSET_X = builder
                    .comment("X offset from the anchored position")
                    .defineInRange("mainHudOffsetX", -10, -500, 500);
            MAIN_HUD_OFFSET_Y = builder
                    .comment("Y offset from the anchored position")
                    .defineInRange("mainHudOffsetY", 0, -500, 500);
            builder.pop();

            builder.push("MiniHUD Positions");
            MINI_HUD_ICON_SIZE = builder
                    .comment("Size (width and height) of the mini-HUD icon in pixels")
                    .defineInRange("miniHudIconSize", 18, 4, 256);
            MINI_HUD_X = builder
                    .comment("X position of the mini-HUD icon")
                    .defineInRange("miniHudX", 15, 0, 500);
            MINI_HUD_Y = builder
                    .comment("Y position of the mini-HUD icon")
                    .defineInRange("miniHudY", 65, 0, 500);
            builder.pop();
        }

        public enum InventoryAnchor {
            TOP, LEFT, RIGHT, BOTTOM
        }
    }


}
