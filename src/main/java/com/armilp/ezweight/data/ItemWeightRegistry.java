package com.armilp.ezweight.data;

import com.armilp.ezweight.EZWeight;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.item.AmmoItem;
import com.tacz.guns.item.AttachmentItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ItemWeightRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<ResourceLocation, Double> ITEM_WEIGHTS = new HashMap<>();
    private static final String FILE_NAME = "items.json";
    private static final String DEFAULT_ASSET_PATH = "/assets/ezweight/items.json"; // ajusta el namespace si tu mod es otro
    private static File configFile;
    private static boolean taczLoaded = false;

    public static void init(Path configDir) {
        taczLoaded = ModList.get().isLoaded(GunMod.MOD_ID);
        configFile = configDir.resolve(FILE_NAME).toFile();

        if (!configFile.exists()) {
            // Copiar el archivo por defecto desde los assets/resources al config si no existe
            copyDefaultFromAssets(configFile);
        }

        if (configFile.exists()) {
            loadFromFile(configFile);
        } else {
            // Si la copia falló o no hay archivo, generar uno desde cero
            generateDefaultFile(configFile);
        }
    }

    /**
     * Intenta copiar el archivo por defecto desde assets a la carpeta de configuración
     */
    private static void copyDefaultFromAssets(File destFile) {
        try (InputStream in = ItemWeightRegistry.class.getResourceAsStream(DEFAULT_ASSET_PATH)) {
            if (in == null) {
                EZWeight.LOGGER.warn("No se encontró el items.json por defecto en assets: {}", DEFAULT_ASSET_PATH);
                return;
            }
            destFile.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
            EZWeight.LOGGER.info("Copiado items.json por defecto desde assets a config.");
        } catch (Exception e) {
            EZWeight.LOGGER.error("No se pudo copiar items.json de assets a config", e);
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

            // Añadir items de Forge y de TACZ que falten
            updated |= addMissingItemsAndTACZGuns(categorizedMap);

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

    /**
     * Solo se usa si no se puede cargar ni copiar ningún archivo (caso muy raro)
     */
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
        if (taczLoaded) {
            addTACZGunsToMap(categorizedWeights, ITEM_WEIGHTS);
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
        if (taczLoaded && item instanceof AbstractGunItem) {
            return 5.0;
        }
        if (taczLoaded && item instanceof AttachmentItem) {
            return 0.8;
        }
        if (taczLoaded && item instanceof AmmoItem) {
            return 0.2;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        String itemName = id != null ? id.getPath().toLowerCase() : "";

        double baseWeight = 0.3;

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

        baseWeight = Math.max(0.01, baseWeight);
        return Math.round(baseWeight * 100.0) / 100.0;
    }

    private static void addTACZGunsToMap(Map<String, Map<String, Double>> categorizedWeights, Map<ResourceLocation, Double> weightsMap) {
        if (!taczLoaded) return;
        Set<Map.Entry<ResourceLocation, CommonGunIndex>> entries = com.tacz.guns.api.TimelessAPI.getAllCommonGunIndex();
        for (Map.Entry<ResourceLocation, CommonGunIndex> entry : entries) {
            ResourceLocation gunId = entry.getKey();
            if (gunId != null && !weightsMap.containsKey(gunId)) {
                double weight = estimateTACZGunWeight(gunId);
                weightsMap.put(gunId, weight);
                categorizedWeights
                        .computeIfAbsent(gunId.getNamespace(), k -> new LinkedHashMap<>())
                        .put(gunId.toString(), weight);
                EZWeight.LOGGER.info("Added TACZ gun '{}' with estimated weight {}", gunId, weight);
            }
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof AttachmentItem) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && !weightsMap.containsKey(id)) {
                    double weight = estimateWeight(item);
                    weightsMap.put(id, weight);
                    categorizedWeights
                            .computeIfAbsent(id.getNamespace(), k -> new LinkedHashMap<>())
                            .put(id.toString(), weight);
                    EZWeight.LOGGER.info("Added TACZ attachment '{}' with estimated weight {}", id, weight);
                }
            }
        }
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item instanceof AmmoItem) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null && !weightsMap.containsKey(id)) {
                    double weight = estimateWeight(item);
                    weightsMap.put(id, weight);
                    categorizedWeights
                            .computeIfAbsent(id.getNamespace(), k -> new LinkedHashMap<>())
                            .put(id.toString(), weight);
                    EZWeight.LOGGER.info("Added TACZ ammo '{}' with estimated weight {}", id, weight);
                }
            }
        }
    }

    private static double estimateTACZGunWeight(ResourceLocation gunId) {
        String name = gunId.getPath().toLowerCase();
        if (name.contains("pistol")) return 2.0;
        if (name.contains("sniper")) return 7.0;
        if (name.contains("shotgun")) return 6.5;
        if (name.contains("smg")) return 4.0;
        if (name.contains("rifle")) return 5.5;
        return 5.0;
    }

    private static boolean addMissingItemsAndTACZGuns(Map<String, Map<String, Double>> categorizedMap) {
        boolean updated = false;
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
        if (taczLoaded) {
            Map<ResourceLocation, Double> oldMap = new HashMap<>(ITEM_WEIGHTS);
            addTACZGunsToMap(categorizedMap, ITEM_WEIGHTS);
            updated |= ITEM_WEIGHTS.size() != oldMap.size();
        }
        return updated;
    }

    public static double getWeight(ItemStack stack) {
        Item item = stack.getItem();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);

        if (taczLoaded && item instanceof AbstractGunItem) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {
                ResourceLocation gunId = new ResourceLocation(tag.getString("GunId"));
                if (ITEM_WEIGHTS.containsKey(gunId)) {
                    return ITEM_WEIGHTS.get(gunId);
                }
            }
        }
        return ITEM_WEIGHTS.getOrDefault(itemId, 1.0);
    }

    public static Map<ResourceLocation, Double> getAllWeights() {
        return Collections.unmodifiableMap(ITEM_WEIGHTS);
    }

    public static void setWeight(ResourceLocation id, double weight) {
        ITEM_WEIGHTS.put(id, weight);
    }

    public static void setWeight(ItemStack stack, double weight) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (taczLoaded && stack.getItem() instanceof AbstractGunItem) {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("GunId", Tag.TAG_STRING)) {
                id = new ResourceLocation(tag.getString("GunId"));
            }
        }
        setWeight(id, weight);
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
            updated |= addMissingItemsAndTACZGuns(categorizedMap);

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

                ITEM_WEIGHTS.clear();

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

                updateMissingItems();
                EZWeight.LOGGER.info("Reloaded {} item weights from config.", ITEM_WEIGHTS.size());
            } catch (Exception e) {
                EZWeight.LOGGER.error("Failed to reload item weights from file.", e);
            }
        } else {
            EZWeight.LOGGER.warn("No config file found to reload item weights.");
        }
    }
}