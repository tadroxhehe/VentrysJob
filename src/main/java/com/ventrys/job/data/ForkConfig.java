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
 * Gestionnaire de configuration pour les fourches et la récolte des cultures
 */
public class ForkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FORK_TOOLS = new HashSet<>();
    private static final Map<String, CropConfig> CROP_CONFIGS = new HashMap<>();
    private static int CLICKS_REQUIRED = 3;
    
    public record CropConfig(String blockId, String dropItem, int dropCount, String seedItem) {}
    
    /**
     * Charge la configuration des fourches depuis extraction_config.json
     */
    public static void loadConfig() {
        FORK_TOOLS.clear();
        CROP_CONFIGS.clear();
        
        try (InputStream resourceStream = ForkConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");
                    
                    if (extractionConfig != null) {
                        // Charger le nombre de clics requis pour la récolte
                        if (extractionConfig.has("crop_harvest_clicks")) {
                            CLICKS_REQUIRED = Math.max(1, extractionConfig.get("crop_harvest_clicks").getAsInt());
                        }
                        
                        // Charger les outils fourche
                        JsonArray forkToolsArray = extractionConfig.getAsJsonArray("fork_tools");
                        if (forkToolsArray != null) {
                            for (JsonElement element : forkToolsArray) {
                                String itemId = element.getAsString();
                                if (isValidItem(itemId)) {
                                    FORK_TOOLS.add(itemId);
                                    VentrysJob.LOGGER.debug("Fourche acceptée: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item fourche invalide ignoré: {}", itemId);
                                }
                            }
                        }
                        
                        // Charger les configurations de récolte
                        JsonArray cropConfigsArray = extractionConfig.getAsJsonArray("crop_configs");
                        if (cropConfigsArray != null) {
                            for (JsonElement element : cropConfigsArray) {
                                if (element.isJsonObject()) {
                                    JsonObject cropObj = element.getAsJsonObject();
                                    if (cropObj.has("block_id") && cropObj.has("drop_item")) {
                                        String blockId = cropObj.get("block_id").getAsString();
                                        String dropItem = cropObj.get("drop_item").getAsString();
                                        int dropCount = cropObj.has("drop_count") ? 
                                            Math.max(1, cropObj.get("drop_count").getAsInt()) : 1;
                                        String seedItem = cropObj.has("seed_item") ? 
                                            cropObj.get("seed_item").getAsString() : null;
                                        
                                        // Normaliser seedItem : null si vide
                                        if (seedItem != null && seedItem.isEmpty()) {
                                            seedItem = null;
                                        }
                                        
                                        if (isValidItem(dropItem)) {
                                            if (seedItem == null || isValidItem(seedItem)) {
                                                CROP_CONFIGS.put(blockId, new CropConfig(blockId, dropItem, dropCount, seedItem));
                                                VentrysJob.LOGGER.debug("Récolte: {} → {} x{} + {}", blockId, dropItem, dropCount, seedItem != null ? seedItem : "aucune");
                                            } else {
                                                VentrysJob.LOGGER.warn("Item graine invalide ignoré: {}", seedItem);
                                            }
                                        } else {
                                            VentrysJob.LOGGER.warn("Item drop invalide ignoré: {}", dropItem);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des fourches: {}", e.getMessage());
            loadDefaults();
        }
        
        if (FORK_TOOLS.isEmpty() && CROP_CONFIGS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucune configuration de fourche, utilisation des valeurs par défaut");
            loadDefaults();
        }
        
        VentrysJob.LOGGER.debug("Fourches — {} outils, {} cultures", FORK_TOOLS.size(), CROP_CONFIGS.size());
    }
    
    private static void loadDefaults() {
        FORK_TOOLS.clear();
        CROP_CONFIGS.clear();
        CLICKS_REQUIRED = 3;
        
        // Plus de valeurs hardcodées - tout est chargé depuis le JSON
    }
    
    /**
     * Vérifie si un item ID est valide (syntaxiquement)
     * Ne vérifie PAS l'existence dans le registre car les mods externes peuvent ne pas être chargés
     */
    private static boolean isValidItem(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            // Vérifier seulement la syntaxe, pas l'existence
            return rl != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Vérifie si un item est une fourche acceptée
     */
    public static boolean isFork(Item item) {
        if (item == null) return false;
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        if (registryName == null) {
            registryName = item.getRegistryName();
        }
        if (registryName == null) return false;
        String id = registryName.toString();
        if (FORK_TOOLS.contains(id)) {
            return true;
        }
        // Fallback : id ventrysitem contenant "fourche" (évite un JSON désync / alias)
        return "ventrysitem".equals(registryName.getNamespace())
                && registryName.getPath().contains("fourche");
    }
    
    /**
     * Obtient la configuration de récolte pour un bloc
     */
    public static Optional<CropConfig> getCropConfig(String blockId) {
        return Optional.ofNullable(CROP_CONFIGS.get(blockId));
    }
    
    /**
     * Obtient le nombre de clics requis pour récolter
     */
    public static int getClicksRequired() {
        return CLICKS_REQUIRED;
    }
    
    /**
     * Obtient la liste des fourches acceptées (pour debug)
     */
    public static Set<String> getForkTools() {
        return new HashSet<>(FORK_TOOLS);
    }
}

