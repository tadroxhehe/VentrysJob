package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contrôle l'accumulation météo au sol pendant les chutes de neige (biomes froids).
 * Fichier éditable : {@code config/ventrysjob-weather.json}.
 *
 * <p>L'effet visuel de neige qui tombe reste inchangé ; seul le dépôt de blocs au sol
 * (couches de neige / gel de l'eau en glace) est concerné — voir
 * {@code com.ventrys.job.mixin.ServerLevelWeatherMixin}.
 */
public final class WeatherConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ventrysjob-weather.json";

    private static volatile boolean disableSnowAccumulation = true;
    private static volatile boolean disableIceFormation = true;

    private WeatherConfig() {
    }

    public static void load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        try {
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonObject json = GSON.fromJson(reader, JsonObject.class);
                    if (json != null) {
                        if (json.has("disableSnowAccumulation")) {
                            disableSnowAccumulation = json.get("disableSnowAccumulation").getAsBoolean();
                        }
                        if (json.has("disableIceFormation")) {
                            disableIceFormation = json.get("disableIceFormation").getAsBoolean();
                        }
                    }
                }
            } else {
                save(path);
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lecture {} (valeurs par défaut conservées): {}", FILE_NAME, e.getMessage());
        }
        VentrysJob.LOGGER.info("Météo VentrysJob — neige au sol désactivée: {}, glace désactivée: {}",
                disableSnowAccumulation, disableIceFormation);
    }

    private static void save(Path path) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("disableSnowAccumulation", disableSnowAccumulation);
            json.addProperty("disableIceFormation", disableIceFormation);
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Impossible d'écrire {}: {}", FILE_NAME, e.getMessage());
        }
    }

    public static boolean isSnowAccumulationDisabled() {
        return disableSnowAccumulation;
    }

    public static boolean isIceFormationDisabled() {
        return disableIceFormation;
    }
}
