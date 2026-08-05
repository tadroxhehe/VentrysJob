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
 * Gestionnaire de configuration pour les burins acceptés
 */
public class ChiselConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> CHISEL_ITEMS = new HashSet<>();
    
    /**
     * Normalise un ID d'item pour accepter les formats avec et sans namespace
     * Si l'ID n'a pas de namespace (pas de ':'), ajoute 'ventrysitem:' par défaut
     */
    private static String normalizeItemId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return itemId;
        }
        // Si l'ID contient déjà un namespace (contient ':'), le retourner tel quel
        if (itemId.contains(":")) {
            return itemId;
        }
        // Sinon, ajouter le namespace par défaut 'ventrysitem:'
        return "ventrysitem:" + itemId;
    }
    
    /**
     * Charge la configuration des burins depuis extraction_config.json
     */
    public static void loadConfig() {
        CHISEL_ITEMS.clear();
        
        try (InputStream resourceStream = ChiselConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");
                    
                    if (extractionConfig != null) {
                        JsonArray chiselToolsArray = extractionConfig.getAsJsonArray("chisel_tools");
                        if (chiselToolsArray != null) {
                            for (JsonElement element : chiselToolsArray) {
                                String itemId = normalizeItemId(element.getAsString());
                                if (isValidItem(itemId)) {
                                    CHISEL_ITEMS.add(itemId);
                                    VentrysJob.LOGGER.debug("Burin accepté: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item burin invalide ignoré: {}", itemId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des burins: {}", e.getMessage());
            loadDefaults();
        }
        
        if (CHISEL_ITEMS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucun burin configuré, utilisation des valeurs par défaut");
            loadDefaults();
        }
        
        VentrysJob.LOGGER.debug("Burins chargés: {} items", CHISEL_ITEMS.size());
    }
    
    private static void loadDefaults() {
        CHISEL_ITEMS.clear();
        CHISEL_ITEMS.add("ventrysitem:item_burin");
        CHISEL_ITEMS.add("ventrysitem:item_burin_en_bronze");
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
     * Vérifie si un item est un burin accepté
     */
    public static boolean isChisel(Item item) {
        if (item == null) return false;
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) return false;
        return CHISEL_ITEMS.contains(registryName.toString());
    }
    
    /**
     * Obtient la liste des burins acceptés (pour debug)
     */
    public static Set<String> getChiselItems() {
        return new HashSet<>(CHISEL_ITEMS);
    }
}
