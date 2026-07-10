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
 * Configuration pour la meule (recettes de mouture)
 */
public final class MeuleConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, MeuleRecipe> RECIPES = new HashMap<>();
    private static long processDurationMs = 15_000L;

    private MeuleConfig() {}

    public static void load() {
        RECIPES.clear();
        processDurationMs = 15_000L;

        try (InputStream stream = MeuleConfig.class.getResourceAsStream("/data/ventrysjob/meule_recipes.json")) {
            if (stream == null) {
                VentrysJob.LOGGER.warn("Configuration de la meule introuvable, utilisation des valeurs par défaut.");
                loadDefaults();
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    VentrysJob.LOGGER.warn("Configuration de la meule vide, utilisation des valeurs par défaut.");
                    loadDefaults();
                    return;
                }

                if (root.has("default_process_duration_ms")) {
                    processDurationMs = Math.max(1L, root.get("default_process_duration_ms").getAsLong());
                }

                JsonArray recipesArray = root.getAsJsonArray("recipes");
                if (recipesArray != null) {
                    for (JsonElement element : recipesArray) {
                        if (!element.isJsonObject()) continue;
                        JsonObject obj = element.getAsJsonObject();
                        if (!validateRecipeObject(obj)) continue;

                        String inputId = obj.get("input").getAsString();
                        int inputCount = Math.max(1, obj.get("input_count").getAsInt());
                        String outputId = obj.get("output").getAsString();
                        int outputCount = Math.max(1, obj.get("output_count").getAsInt());

                        // L'entrée doit exister : c'est elle qui autorise le slot. La sortie peut ne pas être
                        // enregistrée au même moment (ordre de chargement des mods, mod optionnel absent au boot).
                        if (!isValidItem(inputId)) {
                            continue;
                        }
                        if (!isValidItem(outputId)) {
                            VentrysJob.LOGGER.warn(
                                "Meule : sortie '{}' introuvable au chargement — recette quand même enregistrée (vérifiez ventrysitem / l'id). Le craft échouera tant que l'item n'existe pas.",
                                outputId);
                        }

                        MeuleRecipe recipe = new MeuleRecipe(inputId, inputCount, outputId, outputCount);
                        RECIPES.put(inputId, recipe);
                        VentrysJob.LOGGER.debug("Meule: {} x{} → {} x{}", inputId, inputCount, outputId, outputCount);
                    }
                }
            }
        } catch (Exception ex) {
            VentrysJob.LOGGER.error("Erreur lors du chargement des recettes de meule: {}", ex.getMessage());
            loadDefaults();
        }

        if (RECIPES.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucune recette de meule définie, utilisation des valeurs par défaut.");
            loadDefaults();
        }
    }

    private static boolean validateRecipeObject(JsonObject obj) {
        if (!obj.has("input") || !obj.has("output")) {
            VentrysJob.LOGGER.warn("Recette de meule invalide: champs manquants");
            return false;
        }
        if (!obj.has("input_count") || !obj.has("output_count")) {
            VentrysJob.LOGGER.warn("Recette de meule invalide: compte manquant");
            return false;
        }
        return true;
    }

    private static boolean isValidItem(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item == null) {
                VentrysJob.LOGGER.warn("Item introuvable pour la meule: {}", itemId);
                return false;
            }
            return true;
        } catch (Exception ex) {
            VentrysJob.LOGGER.warn("ID d'item invalide pour la meule: {}", itemId);
            return false;
        }
    }

    private static void loadDefaults() {
        RECIPES.clear();
        processDurationMs = 15_000L;
        // Aucune recette par défaut
    }

    public static Optional<MeuleRecipe> getRecipeFor(Item item) {
        if (item == null) return Optional.empty();
        ResourceLocation rl = item.getRegistryName();
        if (rl == null) return Optional.empty();
        return Optional.ofNullable(RECIPES.get(rl.toString()));
    }

    public static long getProcessDurationMs() {
        return processDurationMs;
    }

    public record MeuleRecipe(String inputId, int inputCount, String outputId, int outputCount) {}
}

