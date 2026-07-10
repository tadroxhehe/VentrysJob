package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Scies acceptées sur l'établi artisan (main gauche).
 */
public final class SawConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> SAW_ITEMS = new HashSet<>();

    private SawConfig() {
    }

    public static void loadConfig() {
        SAW_ITEMS.clear();

        try (InputStream resourceStream = SawConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");

                    if (extractionConfig != null) {
                        JsonArray sawToolsArray = extractionConfig.getAsJsonArray("saw_tools");
                        if (sawToolsArray != null) {
                            for (JsonElement element : sawToolsArray) {
                                String itemId = element.getAsString();
                                if (isValidItemId(itemId)) {
                                    SAW_ITEMS.add(itemId);
                                    VentrysJob.LOGGER.debug("Scie acceptée: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item scie invalide ignoré: {}", itemId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement des scies: {}", e.getMessage());
        }

        if (SAW_ITEMS.isEmpty()) {
            SAW_ITEMS.add("ventrysitem:item_scie_en_bronze");
            SAW_ITEMS.add("ventrysitem:item_scie");
        }

        VentrysJob.LOGGER.debug("Scies chargées: {} items", SAW_ITEMS.size());
    }

    private static boolean isValidItemId(String itemId) {
        try {
            return itemId != null && !itemId.isBlank() && ResourceLocation.isValidResourceLocation(itemId);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isSaw(Item item) {
        if (item == null) {
            return false;
        }
        ResourceLocation registryName = item.getRegistryName();
        return registryName != null && SAW_ITEMS.contains(registryName.toString());
    }

    public static Set<String> getSawItems() {
        return new HashSet<>(SAW_ITEMS);
    }
}
