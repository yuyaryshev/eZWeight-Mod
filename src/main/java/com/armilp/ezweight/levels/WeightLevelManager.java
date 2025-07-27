package com.armilp.ezweight.levels;

import com.armilp.ezweight.EZWeight;
import com.armilp.ezweight.config.WeightConfig;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.io.*;
        import java.nio.file.Path;
import java.util.*;

public class WeightLevelManager {

    private static final String FILE_NAME = "levels.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<WeightLevel> LEVELS = new ArrayList<>();

    public static void init(Path configDir) {
        File file = configDir.resolve(FILE_NAME).toFile();
        if (!file.exists()) {
            generateDefaultFile(file);
        }
        loadFromFile(file);
    }

    private static void loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray jsonLevels = root.getAsJsonArray("levels");
            LEVELS.clear();

            for (JsonElement element : jsonLevels) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.has("name") ? obj.get("name").getAsString() : "Unnamed";
                double minWeight = obj.has("min_weight") ? obj.get("min_weight").getAsDouble() : 0.0;
                double maxWeight = obj.has("max_weight") ? obj.get("max_weight").getAsDouble() : Double.MAX_VALUE;

                List<MobEffectInstance> effects = new ArrayList<>();
                JsonArray jsonEffects = obj.getAsJsonArray("effects");
                if (jsonEffects != null) {
                    for (JsonElement e : jsonEffects) {
                        JsonObject effObj = e.getAsJsonObject();
                        String id = effObj.get("effect").getAsString();
                        MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(id));
                        if (effect != null) {
                            int amplifier = effObj.has("amplifier") ? effObj.get("amplifier").getAsInt() : 0;
                            int duration = effObj.has("duration") ? effObj.get("duration").getAsInt() : 200;
                            effects.add(new MobEffectInstance(effect, duration, amplifier, true, true));
                        } else {
                            EZWeight.LOGGER.warn("Unknown effect '{}' in weight level '{}'", id, name);
                        }
                    }
                }

                LEVELS.add(new WeightLevel(name, minWeight, maxWeight, effects));
            }

            LEVELS.sort(Comparator.comparingDouble(WeightLevel::minWeight));
            EZWeight.LOGGER.info("Loaded {} weight levels from config.", LEVELS.size());

        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to load weight levels!", e);
        }
    }

    private static void generateDefaultFile(File file) {
        JsonObject root = new JsonObject();
        JsonArray levels = new JsonArray();

        double base = WeightConfig.COMMON.BASE_WEIGHT.get();
        double max = WeightConfig.COMMON.MAX_WEIGHT.get();

        double range = max - base;
        double step = range / 6.0;

        levels.add(createLevel("Unburdened", base, base + step - 0.01));
        levels.add(createLevel("Light Load", base + step, base + step * 2 - 0.01));
        levels.add(createLevel("Moderate Load", base + step * 2, base + step * 3 - 0.01, effect("minecraft:slowness", 0)));
        levels.add(createLevel("Heavy Load", base + step * 3, base + step * 4 - 0.01,
                effect("minecraft:slowness", 1), effect("minecraft:mining_fatigue", 0)));
        levels.add(createLevel("Very Heavy Load", base + step * 4, base + step * 5 - 0.01,
                effect("minecraft:slowness", 2), effect("minecraft:mining_fatigue", 1)));
        levels.add(createLevel("Overburdened", base + step * 5, max - 0.01,
                effect("minecraft:slowness", 3), effect("minecraft:weakness", 0)));
        levels.add(createLevel("Crushed", max, max,
                effect("minecraft:slowness", 4), effect("minecraft:weakness", 1)));

        root.add("levels", levels);
        root.addProperty("version", 1);

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(root, writer);
                EZWeight.LOGGER.info("Generated balanced weight levels based on base_weight = {} and max_weight = {}", base, max);
            }
        } catch (IOException e) {
            EZWeight.LOGGER.error("Failed to write default levels config!", e);
        }
    }




    private static JsonObject createLevel(String name, double minWeight, double maxWeight, JsonObject... effects) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("min_weight", minWeight);
        obj.addProperty("max_weight", maxWeight);
        JsonArray effArray = new JsonArray();
        for (JsonObject eff : effects) {
            effArray.add(eff);
        }
        obj.add("effects", effArray);
        return obj;
    }

    private static JsonObject effect(String id, int amplifier) {
        JsonObject obj = new JsonObject();
        obj.addProperty("effect", id);
        obj.addProperty("amplifier", amplifier);
        obj.addProperty("duration", 200); // ticks por defecto
        return obj;
    }

    public static WeightLevel getLevelForWeight(double weight) {
        return LEVELS.stream()
                .filter(level -> level.isInRange(weight))
                .findFirst()
                .orElse(null);
    }

    public static List<WeightLevel> getLevels() {
        return Collections.unmodifiableList(LEVELS);
    }

    public static void loadFromJsonString(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray jsonLevels = root.getAsJsonArray("levels");
            LEVELS.clear();

            for (JsonElement element : jsonLevels) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                double min = obj.get("min_weight").getAsDouble();
                double max = obj.get("max_weight").getAsDouble();

                List<MobEffectInstance> effects = new ArrayList<>();
                JsonArray effs = obj.getAsJsonArray("effects");
                for (JsonElement effElement : effs) {
                    JsonObject effObj = effElement.getAsJsonObject();
                    MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(new ResourceLocation(effObj.get("effect").getAsString()));
                    int amplifier = effObj.get("amplifier").getAsInt();
                    int duration = effObj.get("duration").getAsInt();
                    effects.add(new MobEffectInstance(effect, duration, amplifier, true, true));
                }

                LEVELS.add(new WeightLevel(name, min, max, effects));
            }

            LEVELS.sort(Comparator.comparingDouble(WeightLevel::minWeight));
        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to parse synced weight levels", e);
        }
    }

    public static String toJsonString(List<WeightLevel> levels) {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();

        for (WeightLevel level : levels) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", level.name());
            obj.addProperty("min_weight", level.minWeight());
            obj.addProperty("max_weight", level.maxWeight());

            JsonArray effs = new JsonArray();
            for (MobEffectInstance eff : level.effects()) {
                JsonObject effObj = new JsonObject();
                effObj.addProperty("effect", BuiltInRegistries.MOB_EFFECT.getKey(eff.getEffect()).toString());
                effObj.addProperty("amplifier", eff.getAmplifier());
                effObj.addProperty("duration", eff.getDuration());
                effs.add(effObj);
            }

            obj.add("effects", effs);
            array.add(obj);
        }

        root.add("levels", array);
        root.addProperty("version", 1);

        return GSON.toJson(root);
    }
}
