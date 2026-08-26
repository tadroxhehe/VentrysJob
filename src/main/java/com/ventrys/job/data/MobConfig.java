package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Configuration pour les mobs personnalisés
 */
public final class MobConfig {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, AnimalConfig> ANIMALS = new HashMap<>();
    
    private static int minNutritionPercent = 30;
    private static int minHydrationPercent = 30;
    private static int requiredTimeMinutes = 15;
    private static long milkExtractionIntervalMs = 60_000L; // 1 minute
    private static double detectionRadiusBlocks = 16.0D; // Rayon de détection pour la reproduction
    
    private MobConfig() {}
    
    public static void load() {
        ANIMALS.clear();
        
        try (InputStream stream = MobConfig.class.getResourceAsStream("/data/ventrysjob/mobs_config.json")) {
            if (stream == null) {
                VentrysJob.LOGGER.warn("Configuration des mobs introuvable, utilisation des valeurs par défaut.");
                loadDefaults();
                return;
            }
            
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    VentrysJob.LOGGER.warn("Configuration des mobs vide, utilisation des valeurs par défaut.");
                    loadDefaults();
                    return;
                }
                
                // Charger les paramètres de reproduction
                if (root.has("reproduction")) {
                    JsonObject repro = root.getAsJsonObject("reproduction");
                    if (repro.has("min_nutrition_percent")) {
                        minNutritionPercent = Math.max(0, Math.min(100, repro.get("min_nutrition_percent").getAsInt()));
                    }
                    if (repro.has("min_hydration_percent")) {
                        minHydrationPercent = Math.max(0, Math.min(100, repro.get("min_hydration_percent").getAsInt()));
                    }
                    if (repro.has("required_time_minutes")) {
                        requiredTimeMinutes = Math.max(1, repro.get("required_time_minutes").getAsInt());
                    }
                    if (repro.has("detection_radius_blocks")) {
                        detectionRadiusBlocks = Math.max(1.0D, repro.get("detection_radius_blocks").getAsDouble());
                    }
                }
                
                // Charger l'intervalle d'extraction de lait
                if (root.has("milk_extraction_interval_ms")) {
                    milkExtractionIntervalMs = Math.max(1000L, root.get("milk_extraction_interval_ms").getAsLong());
                }
                
                // Charger les animaux
                JsonArray animalsArray = root.getAsJsonArray("animals");
                if (animalsArray != null) {
                    for (JsonElement element : animalsArray) {
                        if (!element.isJsonObject()) continue;
                        JsonObject obj = element.getAsJsonObject();
                        
                        if (!obj.has("id")) continue;
                        String animalId = obj.get("id").getAsString();
                        
                        List<DropConfig> drops = new ArrayList<>();
                        if (obj.has("drops") && obj.get("drops").isJsonArray()) {
                            JsonArray dropsArray = obj.getAsJsonArray("drops");
                            for (JsonElement dropElement : dropsArray) {
                                if (!dropElement.isJsonObject()) continue;
                                JsonObject dropObj = dropElement.getAsJsonObject();
                                
                                if (!dropObj.has("item_id")) continue;
                                String itemId = dropObj.get("item_id").getAsString();
                                
                                if (!isValidItem(itemId)) {
                                    VentrysJob.LOGGER.warn("Item invalide pour drop: {}", itemId);
                                    continue;
                                }
                                
                                int minCount = dropObj.has("min_count") ? Math.max(0, dropObj.get("min_count").getAsInt()) : 1;
                                int maxCount = dropObj.has("max_count") ? Math.max(minCount, dropObj.get("max_count").getAsInt()) : minCount;
                                
                                drops.add(new DropConfig(itemId, minCount, maxCount));
                            }
                        }
                        
                        ANIMALS.put(animalId, new AnimalConfig(animalId, drops));
                        VentrysJob.LOGGER.debug("Animal drops: {} — {} entrées", animalId, drops.size());
                    }
                }
            }
        } catch (Exception ex) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des mobs: {}", ex.getMessage());
            loadDefaults();
        }
        
        if (ANIMALS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucun animal configuré, utilisation des valeurs par défaut.");
            loadDefaults();
        }
    }
    
    private static boolean isValidItem(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            return item != null;
        } catch (Exception ex) {
            return false;
        }
    }
    
    private static void loadDefaults() {
        ANIMALS.clear();
        minNutritionPercent = 20;
        minHydrationPercent = 20;
        requiredTimeMinutes = 5760; // 96 h
        milkExtractionIntervalMs = 43_200_000L; // 12 h
        detectionRadiusBlocks = 16.0D;
    }
    
    public static Optional<AnimalConfig> getAnimalConfig(String animalId) {
        return Optional.ofNullable(ANIMALS.get(animalId));
    }
    
    public static int getMinNutritionPercent() {
        return minNutritionPercent;
    }
    
    public static int getMinHydrationPercent() {
        return minHydrationPercent;
    }
    
    public static int getRequiredTimeMinutes() {
        return requiredTimeMinutes;
    }
    
    public static long getMilkExtractionIntervalMs() {
        return milkExtractionIntervalMs;
    }
    
    public static double getDetectionRadiusBlocks() {
        return detectionRadiusBlocks;
    }
    
    public record AnimalConfig(String id, List<DropConfig> drops) {}
    
    public record DropConfig(String itemId, int minCount, int maxCount) {}
}

