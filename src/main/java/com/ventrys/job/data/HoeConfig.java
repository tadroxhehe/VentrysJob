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
 * Gestionnaire de configuration pour les houes et le labour
 */
public class HoeConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> HOE_TOOLS = new HashSet<>();
    
    /**
     * Charge la configuration des houes depuis extraction_config.json
     */
    public static void loadConfig() {
        HOE_TOOLS.clear();
        
        try (InputStream resourceStream = HoeConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");
                    
                    if (extractionConfig != null) {
                        // Charger les outils houe
                        JsonArray hoeToolsArray = extractionConfig.getAsJsonArray("hoe_tools");
                        if (hoeToolsArray != null) {
                            for (JsonElement element : hoeToolsArray) {
                                String itemId = element.getAsString();
                                if (isValidItem(itemId)) {
                                    HOE_TOOLS.add(itemId);
                                    VentrysJob.LOGGER.debug("Houe acceptée: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item houe invalide ignoré: {}", itemId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des houes: {}", e.getMessage());
            loadDefaults();
        }
        
        if (HOE_TOOLS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucune configuration de houe, utilisation des valeurs par défaut");
            loadDefaults();
        }
        
        VentrysJob.LOGGER.debug("Houes chargées: {} outils", HOE_TOOLS.size());
    }
    
    private static void loadDefaults() {
        HOE_TOOLS.clear();
        
        // Ajouter les houes vanilla par défaut
        HOE_TOOLS.add("minecraft:wooden_hoe");
        HOE_TOOLS.add("minecraft:stone_hoe");
        HOE_TOOLS.add("minecraft:iron_hoe");
        HOE_TOOLS.add("minecraft:golden_hoe");
        HOE_TOOLS.add("minecraft:diamond_hoe");
        HOE_TOOLS.add("minecraft:netherite_hoe");
    }
    
    /**
     * Vérifie si un item ID est valide
     */
    private static boolean isValidItem(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            return item != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Vérifie si un item est une houe acceptée
     */
    public static boolean isHoe(Item item) {
        if (item == null) return false;
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) return false;
        return HOE_TOOLS.contains(registryName.toString());
    }
    
    /**
     * Obtient la liste des houes acceptées (pour debug)
     */
    public static Set<String> getHoeTools() {
        return new HashSet<>(HOE_TOOLS);
    }
}

