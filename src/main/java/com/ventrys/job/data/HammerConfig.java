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
 * Marteaux de forge acceptés sur l'enclume forgeron (main gauche).
 */
public final class HammerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> HAMMER_ITEMS = new HashSet<>();

    private HammerConfig() {
    }

    public static void loadConfig() {
        HAMMER_ITEMS.clear();

        try (InputStream resourceStream = HammerConfig.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    JsonObject extractionConfig = json.getAsJsonObject("extraction_config");

                    if (extractionConfig != null) {
                        JsonArray hammerToolsArray = extractionConfig.getAsJsonArray("hammer_tools");
                        if (hammerToolsArray != null) {
                            for (JsonElement element : hammerToolsArray) {
                                String itemId = element.getAsString();
                                if (isValidItemId(itemId)) {
                                    HAMMER_ITEMS.add(itemId);
                                    VentrysJob.LOGGER.debug("Marteau accepté: {}", itemId);
                                } else {
                                    VentrysJob.LOGGER.warn("Item marteau invalide ignoré: {}", itemId);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement des marteaux: {}", e.getMessage());
        }

        if (HAMMER_ITEMS.isEmpty()) {
            HAMMER_ITEMS.add("ventrysitem:item_marteau_en_bronze");
            HAMMER_ITEMS.add("ventrysitem:item_marteau_en_fer");
        }

        VentrysJob.LOGGER.debug("Marteaux chargés: {} items", HAMMER_ITEMS.size());
    }

    private static boolean isValidItemId(String itemId) {
        try {
            return itemId != null && !itemId.isBlank() && ResourceLocation.isValidResourceLocation(itemId);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHammer(Item item) {
        if (item == null) {
            return false;
        }
        ResourceLocation registryName = item.getRegistryName();
        return registryName != null && HAMMER_ITEMS.contains(registryName.toString());
    }

    public static Set<String> getHammerItems() {
        return new HashSet<>(HAMMER_ITEMS);
    }
}
