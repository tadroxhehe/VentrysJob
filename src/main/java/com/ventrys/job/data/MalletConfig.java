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
import java.util.HashSet;
import java.util.Set;

/**
 * Gestionnaire de configuration pour les maillets acceptés
 */
public class MalletConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> MALLET_ITEMS = new HashSet<>();
    
    /**
     * Charge la configuration des maillets depuis extraction_config.json
     */
    public static void loadConfig() {
        MALLET_ITEMS.clear();
        
        try (InputStream resourceStream = MalletConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");
                    
                    if (extractionConfig != null) {
                        JsonArray malletToolsArray = extractionConfig.getAsJsonArray("mallet_tools");
                        if (malletToolsArray != null) {
                            for (JsonElement element : malletToolsArray) {
                                String itemId = element.getAsString();
                                if (isValidItem(itemId)) {
                                    MALLET_ITEMS.add(itemId);
                                    VentrysJob.LOGGER.debug("Maillet accepté: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item maillet invalide ignoré: {}", itemId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des maillets: {}", e.getMessage());
            loadDefaults();
        }
        
        if (MALLET_ITEMS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucun maillet configuré, utilisation des valeurs par défaut");
            loadDefaults();
        }
        
        VentrysJob.LOGGER.debug("Maillets chargés: {} items", MALLET_ITEMS.size());
    }
    
    private static void loadDefaults() {
        MALLET_ITEMS.clear();
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
     * Vérifie si un item est un maillet accepté
     */
    public static boolean isMallet(Item item) {
        if (item == null) return false;
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) return false;
        return MALLET_ITEMS.contains(registryName.toString());
    }
    
    /**
     * Obtient la liste des maillets acceptés (pour debug)
     */
    public static Set<String> getMalletItems() {
        return new HashSet<>(MALLET_ITEMS);
    }
}

