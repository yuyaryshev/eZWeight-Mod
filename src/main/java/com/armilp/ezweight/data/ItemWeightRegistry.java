package com.armilp.ezweight.data;

import com.armilp.ezweight.EZWeight;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class ItemWeightRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, Double> ITEM_WEIGHTS = new HashMap<>();

    private static final String FILE_NAME = "items.json";

    private static File configFile;

    public static void init(Path configDir) {
        configFile = configDir.resolve(FILE_NAME).toFile();
        if (configFile.exists()) {
            loadFromFile(configFile);
        } else {
            generateDefaultFile(configFile);
        }
    }



    private static void loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Map<String, Double>>>() {}.getType();
            Map<String, Map<String, Double>> categorizedMap = GSON.fromJson(reader, type);

            ITEM_WEIGHTS.clear();
            boolean updated = false;

            for (Map.Entry<String, Map<String, Double>> categoryEntry : categorizedMap.entrySet()) {
                for (Map.Entry<String, Double> entry : categoryEntry.getValue().entrySet()) {
                    try {
                        ResourceLocation id = new ResourceLocation(entry.getKey());
                        ITEM_WEIGHTS.put(id, entry.getValue());
                    } catch (Exception ex) {
                        EZWeight.LOGGER.warn("Invalid item ID in config: {}", entry.getKey());
                    }
                }
            }

            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && !ITEM_WEIGHTS.containsKey(id)) {
                    double weight = estimateWeight(item);
                    ITEM_WEIGHTS.put(id, weight);
                    categorizedMap
                            .computeIfAbsent(id.getNamespace(), k -> new LinkedHashMap<>())
                            .put(id.toString(), weight);
                    updated = true;
                    EZWeight.LOGGER.info("Added new item '{}' with estimated weight {}", id, weight);
                }
            }

            if (updated) {
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(categorizedMap, writer);
                    EZWeight.LOGGER.info("Updated item weights file with new items.");
                }
            }

            EZWeight.LOGGER.info("Loaded {} item weights from config.", ITEM_WEIGHTS.size());
        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to load item weights!", e);
        }
    }


    public static void saveToFile(File file) {
        try {
            Type type = new TypeToken<Map<String, Map<String, Double>>>() {}.getType();
            Map<String, Map<String, Double>> categorizedMap = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, Double> entry : ITEM_WEIGHTS.entrySet()) {
                ResourceLocation id = entry.getKey();
                String namespace = id.getNamespace();
                categorizedMap
                        .computeIfAbsent(namespace, k -> new LinkedHashMap<>())
                        .put(id.toString(), entry.getValue());
            }

            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(categorizedMap, writer);
            }

            EZWeight.LOGGER.info("Item weights saved to file.");
        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to save item weights!", e);
        }
    }


    private static void generateDefaultFile(File file) {
        Map<String, Map<String, Double>> categorizedWeights = new HashMap<>();
        ITEM_WEIGHTS.clear();

        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) {
                double weight = estimateWeight(item);
                ITEM_WEIGHTS.put(id, weight);

                String namespace = id.getNamespace();
                categorizedWeights
                        .computeIfAbsent(namespace, k -> new LinkedHashMap<>())
                        .put(id.toString(), weight);
            }
        }

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(categorizedWeights, writer);
                EZWeight.LOGGER.info("Generated default item weights with {} categories.", categorizedWeights.size());
            }
        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to write default item weights!", e);
        }
    }

    private static double estimateWeight(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        String itemName = id != null ? id.getPath().toLowerCase() : "";

        double baseWeight = 0.1;

        if (itemName.contains("sword") || itemName.contains("axe") || itemName.contains("pickaxe")) {
            baseWeight += 2.0;
        } else if (itemName.contains("shovel") || itemName.contains("hoe")) {
            baseWeight += 1.0;
        } else if (itemName.contains("helmet") || itemName.contains("chestplate") || itemName.contains("leggings") || itemName.contains("boots")) {
            baseWeight += 3.0;
        } else if (itemName.contains("block")) {
            baseWeight += 4.0;
        } else if (itemName.contains("nugget")) {
            baseWeight += 0.1;
        } else if (itemName.contains("ingot")) {
            baseWeight += 1.0;
        } else if (itemName.contains("stick") || itemName.contains("feather")) {
            baseWeight += 0.05;
        } else if (itemName.contains("food") || itemName.contains("bread") || itemName.contains("meat") || itemName.contains("apple")) {
            baseWeight += 0.2;
        } else if (itemName.contains("bucket")) {
            baseWeight += 1.5;
        } else if (itemName.contains("bow") || itemName.contains("crossbow")) {
            baseWeight += 1.0;
        }

        if (itemName.contains("wood")) {
            baseWeight *= 0.8;
        } else if (itemName.contains("stone")) {
            baseWeight *= 1.2;
        } else if (itemName.contains("iron")) {
            baseWeight *= 1.5;
        } else if (itemName.contains("gold")) {
            baseWeight *= 2.0;
        } else if (itemName.contains("diamond")) {
            baseWeight *= 2.5;
        } else if (itemName.contains("netherite")) {
            baseWeight *= 3.0;
        }

        int stackSize = item.getMaxStackSize();
        if (stackSize > 1) {
            baseWeight /= Math.sqrt(stackSize);
        }

        // Límite mínimo y redondeo
        baseWeight = Math.max(0.01, baseWeight);
        return Math.round(baseWeight * 100.0) / 100.0; // redondeo a 2 decimales
    }




    public static double getWeight(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return ITEM_WEIGHTS.getOrDefault(id, 1.0);
    }

    public static Map<ResourceLocation, Double> getAllWeights() {
        return Collections.unmodifiableMap(ITEM_WEIGHTS);
    }

    public static void setWeight(ResourceLocation id, double weight) {
        ITEM_WEIGHTS.put(id, weight);
    }

    public static File getConfigFile() {
        return configFile;
    }

    public static void updateMissingItems() {
        if (configFile == null || !configFile.exists()) {
            EZWeight.LOGGER.warn("Config file not found for updating items.");
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<Map<String, Map<String, Double>>>() {}.getType();
            Map<String, Map<String, Double>> categorizedMap = GSON.fromJson(reader, type);

            boolean updated = false;

            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && !ITEM_WEIGHTS.containsKey(id)) {
                    double weight = estimateWeight(item);
                    ITEM_WEIGHTS.put(id, weight);

                    categorizedMap
                            .computeIfAbsent(id.getNamespace(), k -> new LinkedHashMap<>())
                            .put(id.toString(), weight);

                    EZWeight.LOGGER.info("Added missing item '{}' with estimated weight {}", id, weight);
                    updated = true;
                }
            }

            if (updated) {
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(categorizedMap, writer);
                    EZWeight.LOGGER.info("Updated item weights file with new items.");
                }
            }

        } catch (Exception e) {
            EZWeight.LOGGER.error("Failed to update missing item weights!", e);
        }
    }


    public static void reloadFromFile() {
        if (configFile != null && configFile.exists()) {
            try {
                Type type = new TypeToken<Map<String, Map<String, Double>>>() {}.getType();
                Map<String, Map<String, Double>> categorizedMap = GSON.fromJson(new FileReader(configFile), type);

                ITEM_WEIGHTS.clear(); // Limpia el mapa actual para recargar todo

                for (Map.Entry<String, Map<String, Double>> categoryEntry : categorizedMap.entrySet()) {
                    for (Map.Entry<String, Double> entry : categoryEntry.getValue().entrySet()) {
                        try {
                            ResourceLocation id = new ResourceLocation(entry.getKey());
                            double weight = entry.getValue();
                            ITEM_WEIGHTS.put(id, weight);
                        } catch (Exception e) {
                            EZWeight.LOGGER.warn("Invalid entry in item weights config: {}", entry.getKey(), e);
                        }
                    }
                }

                updateMissingItems(); // Agrega ítems nuevos si faltan
                EZWeight.LOGGER.info("Reloaded {} item weights from config.", ITEM_WEIGHTS.size());

            } catch (Exception e) {
                EZWeight.LOGGER.error("Failed to reload item weights from file.", e);
            }
        } else {
            EZWeight.LOGGER.warn("No config file found to reload item weights.");
        }
    }

}
