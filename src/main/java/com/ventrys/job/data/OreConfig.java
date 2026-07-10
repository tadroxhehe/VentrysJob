package com.ventrys.job.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration pour les blocs de minerais moddés
 */
public class OreConfig {
    
    private static final Gson GSON = new Gson();
    private static final Map<String, OreData> ORES = new HashMap<>();
    
    public static class OreData {
        private final String id;
        private final String name;
        private final String texture;
        private final float hardness;
        private final boolean requiresTool;
        private final String dropItemId;
        private final int dropCount;
        private final int experience;
        
        public OreData(String id, String name, String texture, float hardness, boolean requiresTool,
                      String dropItemId, int dropCount, int experience) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.hardness = hardness;
            this.requiresTool = requiresTool;
            this.dropItemId = dropItemId;
            this.dropCount = dropCount;
            this.experience = experience;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getTexture() { return texture; }
        public float getHardness() { return hardness; }
        public boolean requiresTool() { return requiresTool; }
        public String getDropItemId() { return dropItemId; }
        public int getDropCount() { return dropCount; }
        public int getExperience() { return experience; }
    }
    
    public static void loadConfig() {
        ORES.clear();
        
        try (InputStream stream = OreConfig.class.getResourceAsStream("/data/ventrysjob/ores_config.json")) {
            if (stream == null) {
                VentrysJob.LOGGER.warn("Configuration des minerais introuvable, utilisation des valeurs par défaut.");
                return;
            }
            
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                if (root == null || !root.has("ores")) {
                    VentrysJob.LOGGER.warn("Configuration des minerais vide.");
                    return;
                }
                
                JsonArray oresArray = root.getAsJsonArray("ores");
                for (JsonElement element : oresArray) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    
                    JsonObject oreObj = element.getAsJsonObject();
                    
                    String id = oreObj.get("id").getAsString();
                    String name = oreObj.has("name") ? oreObj.get("name").getAsString() : id;
                    String texture = oreObj.has("texture") ? oreObj.get("texture").getAsString() : id;
                    float hardness = oreObj.has("hardness") ? oreObj.get("hardness").getAsFloat() : 3.0f;
                    boolean requiresTool = !oreObj.has("requires_tool") || oreObj.get("requires_tool").getAsBoolean();
                    
                    String dropItemId = "";
                    int dropCount = 1;
                    int experience = 0;
                    
                    if (oreObj.has("drops") && oreObj.get("drops").isJsonObject()) {
                        JsonObject dropsObj = oreObj.getAsJsonObject("drops");
                        dropItemId = dropsObj.get("item_id").getAsString();
                        dropCount = dropsObj.has("count") ? dropsObj.get("count").getAsInt() : 1;
                        experience = dropsObj.has("experience") ? dropsObj.get("experience").getAsInt() : 0;
                    }
                    
                    ORES.put(id, new OreData(id, name, texture, hardness, requiresTool, dropItemId, dropCount, experience));
                    VentrysJob.LOGGER.debug("Minerai: {} ({})", id, name);
                }
                
                VentrysJob.LOGGER.debug("Minerais configurés: {}", ORES.size());
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des minerais", e);
        }
    }
    
    public static Map<String, OreData> getOres() {
        return ORES;
    }
    
    public static OreData getOre(String id) {
        return ORES.get(id);
    }
}
