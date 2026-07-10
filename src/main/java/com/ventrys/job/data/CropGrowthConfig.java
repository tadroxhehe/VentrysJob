package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Gestion de la configuration des temps de croissance des cultures
 */
public final class CropGrowthConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, CropConfigEntry> CROP_CONFIG = new HashMap<>();
    private static long defaultStageDuration = 600L; // 30 secondes par défaut

    private CropGrowthConfig() {
    }

    public static void loadConfig() {
        CROP_CONFIG.clear();
        defaultStageDuration = 600L;

        try (InputStream resourceStream = CropGrowthConfig.class.getResourceAsStream("/data/ventrysjob/crop_growth.json")) {
            if (resourceStream == null) {
                VentrysJob.LOGGER.warn("Configuration des cultures introuvable, utilisation des valeurs par défaut");
                loadDefaultValues();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) {
                    VentrysJob.LOGGER.warn("Configuration des cultures vide, utilisation des valeurs par défaut");
                    loadDefaultValues();
                    return;
                }

                if (json.has("default_stage_duration_ticks")) {
                    defaultStageDuration = Math.max(1L, json.get("default_stage_duration_ticks").getAsLong());
                }

                JsonArray cropsArray = json.getAsJsonArray("crops");
                if (cropsArray != null) {
                    for (JsonElement element : cropsArray) {
                        if (!element.isJsonObject()) {
                            continue;
                        }

                        JsonObject cropObj = element.getAsJsonObject();
                        if (!cropObj.has("block_id")) {
                            continue;
                        }

                        String blockId = cropObj.get("block_id").getAsString();
                        ResourceLocation registryName;
                        try {
                            registryName = new ResourceLocation(blockId);
                        } catch (Exception ex) {
                            VentrysJob.LOGGER.warn("ID de bloc de culture invalide: {}", blockId);
                            continue;
                        }

                        Block block = ForgeRegistries.BLOCKS.getValue(registryName);
                        if (!(block instanceof CropBlock)) {
                            VentrysJob.LOGGER.warn("Bloc {} n'est pas une culture gérée (CropBlock). Ignoré.", blockId);
                            continue;
                        }

                        List<Long> stageDurations = new ArrayList<>();
                        if (cropObj.has("stage_durations_ticks")) {
                            JsonArray durationArray = cropObj.getAsJsonArray("stage_durations_ticks");
                            for (JsonElement durationEl : durationArray) {
                                long duration = Math.max(1L, durationEl.getAsLong());
                                stageDurations.add(duration);
                            }
                        }

                        if (stageDurations.isEmpty()) {
                            VentrysJob.LOGGER.warn("Aucune durée définie pour la culture {}. Utilisation des valeurs par défaut.", blockId);
                            stageDurations.add(defaultStageDuration);
                        }

                        CROP_CONFIG.put(blockId, new CropConfigEntry(blockId, stageDurations));
                        VentrysJob.LOGGER.debug("Culture: {} — {} étapes", blockId, stageDurations.size());
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des cultures: {}", e.getMessage());
            loadDefaultValues();
        }

        if (CROP_CONFIG.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucune culture configurée, utilisation des valeurs par défaut");
            loadDefaultValues();
        }
    }

    private static void loadDefaultValues() {
        defaultStageDuration = 600L;
        registerDefault("minecraft:wheat", 7);
        registerDefault("minecraft:carrots", 7);
        registerDefault("minecraft:potatoes", 7);
        registerDefault("minecraft:beetroots", 3);
    }

    private static void registerDefault(String blockId, int stages) {
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < stages; i++) {
            durations.add(defaultStageDuration);
        }
        CROP_CONFIG.put(blockId, new CropConfigEntry(blockId, durations));
    }

    public static boolean isConfiguredCrop(Block block) {
        if (!(block instanceof CropBlock)) {
            return false;
        }
        ResourceLocation registryName = block.getRegistryName();
        if (registryName == null) {
            return false;
        }
        return CROP_CONFIG.containsKey(registryName.toString());
    }

    public static long getStageDurationTicks(Block block, int currentAge) {
        if (block == null) {
            return defaultStageDuration;
        }
        ResourceLocation registryName = block.getRegistryName();
        if (registryName == null) {
            return defaultStageDuration;
        }
        CropConfigEntry entry = CROP_CONFIG.get(registryName.toString());
        if (entry == null) {
            return defaultStageDuration;
        }

        List<Long> durations = entry.stageDurations();
        if (durations.isEmpty()) {
            return defaultStageDuration;
        }
        if (currentAge < durations.size()) {
            return durations.get(currentAge);
        }
        return durations.get(durations.size() - 1);
    }

    private record CropConfigEntry(String blockId, List<Long> stageDurations) {
    }
}

