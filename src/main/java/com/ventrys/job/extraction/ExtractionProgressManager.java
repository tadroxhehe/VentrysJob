package com.ventrys.job.extraction;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtractionProgressManager {
    private static final Map<String, Integer> clickProgress = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastClickTime = new ConcurrentHashMap<>();
    /** Délai avant reset si le joueur change de cible / pause trop longue (réseau inclus). */
    private static final long PROGRESSION_RESET_DELAY = 10000;
    private static final Map<String, Map<String, String>> activeProgressionsByType = new ConcurrentHashMap<>();

    private ExtractionProgressManager() {
    }

    public static Map<String, Integer> getClickProgress() {
        return clickProgress;
    }

    public static Map<String, Long> getLastClickTime() {
        return lastClickTime;
    }

    public static Map<String, Map<String, String>> getActiveProgressionsByType() {
        return activeProgressionsByType;
    }

    public static String buildProgressKey(String playerUUID, String extractionType, BlockPos pos) {
        return playerUUID + "_" + extractionType + "_" + pos;
    }

    public static String getExtractionType(String key) {
        int firstUnderscore = key.indexOf('_');
        if (firstUnderscore >= 0) {
            int secondUnderscore = key.indexOf('_', firstUnderscore + 1);
            if (secondUnderscore > firstUnderscore + 1) {
                String extractedType = key.substring(firstUnderscore + 1, secondUnderscore);
                if (extractedType.equals("oak") || extractedType.equals("birch") ||
                    extractedType.equals("spruce") || extractedType.equals("acacia") ||
                    extractedType.equals("dark_oak") || extractedType.equals("mangrove") ||
                    extractedType.equals("cherry") || extractedType.equals("log")) {
                    return "oak";
                }
                if (extractedType.equals("calcite")) {
                    return "stone";
                }
                if (extractedType.equals("clay")) {
                    return "sand";
                }
                return extractedType;
            }
        }
        if (key.contains("_saw_")) return "saw";
        if (key.contains("_mine_")) return "mine";
        if (key.contains("_stone_") || key.contains("_calcite_")) return "stone";
        if (key.contains("_sand_") || key.contains("_clay_")) return "sand";
        if (key.contains("_cropharvest_") || key.contains("_crop_harvest_")) return "cropharvest";
        return "oak";
    }

    public static boolean checkAndResetProgress(String playerUUID, String key, String typeToUse) {
        String extractionType = typeToUse;
        Map<String, String> typeMap = activeProgressionsByType.get(playerUUID);

        if (typeMap == null) {
            return true;
        }

        String existingKey = typeMap.get(extractionType);

        if (existingKey != null) {
            if (existingKey.equals(key)) {
                Long lastTime = lastClickTime.get(key);
                if (lastTime != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime > PROGRESSION_RESET_DELAY) {
                        clearProgressEntry(playerUUID, extractionType, key, typeMap);
                    }
                }
                return true;
            } else {
                Long lastTime = lastClickTime.get(existingKey);
                if (lastTime != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime > PROGRESSION_RESET_DELAY) {
                        clearProgressEntry(playerUUID, extractionType, existingKey, typeMap);
                        return true;
                    } else {
                        return false;
                    }
                }
                return false;
            }
        }

        return true;
    }

    static void clearProgressEntry(String playerUUID, String extractionType, String key, Map<String, String> typeMap) {
        clickProgress.remove(key);
        lastClickTime.remove(key);
        typeMap.remove(extractionType);
        if (typeMap.isEmpty()) {
            activeProgressionsByType.remove(playerUUID);
        }
    }

    static void cleanupTemporaryData() {
        long currentTime = System.currentTimeMillis();
        final int[] cleanedCount = {0};

        clickProgress.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            Long lastTime = lastClickTime.get(key);
            boolean expired = lastTime != null && (currentTime - lastTime) > 60000;
            if (expired) {
                cleanedCount[0]++;
            }
            return expired;
        });

        lastClickTime.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            return !clickProgress.containsKey(key);
        });

        activeProgressionsByType.entrySet().removeIf(entry -> {
            Map<String, String> typeMap = entry.getValue();
            typeMap.entrySet().removeIf(typeEntry -> !clickProgress.containsKey(typeEntry.getValue()));
            return typeMap.isEmpty();
        });

        activeProgressionsByType.entrySet().removeIf(entry -> {
            String playerUUID = entry.getKey();
            Map<String, String> typeMap = entry.getValue();
            Iterator<Map.Entry<String, String>> iterator = typeMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> typeEntry = iterator.next();
                String activeKey = typeEntry.getValue();
                Long lastTime = lastClickTime.get(activeKey);
                if (lastTime != null && (currentTime - lastTime) > 30000) {
                    clickProgress.remove(activeKey);
                    lastClickTime.remove(activeKey);
                    iterator.remove();
                    VentrysJob.LOGGER.debug("Progression coincée nettoyée pour le joueur {}", playerUUID);
                }
            }
            return typeMap.isEmpty();
        });

        VentrysJob.LOGGER.debug("Nettoyage des données temporaires terminé - {} entrées nettoyées", cleanedCount[0]);
    }

    public static void forceCleanup() {
        VentrysJob.LOGGER.debug("Nettoyage forcé des données temporaires");
        cleanupTemporaryData();
    }

    public static void cleanupProgress() {
        cleanupTemporaryData();
    }

    public static void resetPlayerProgress(Player player) {
        String playerUuid = player.getUUID().toString();
        clickProgress.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuid));
        lastClickTime.entrySet().removeIf(entry -> entry.getKey().startsWith(playerUuid));
        activeProgressionsByType.remove(playerUuid);
        VentrysJob.LOGGER.debug("Progressions nettoyées pour le joueur: {}", player.getName().getString());
    }

    public static void clearPlayerProgressions(String playerUUID) {
        Map<String, String> typeMap = activeProgressionsByType.get(playerUUID);
        if (typeMap != null) {
            for (String activeKey : typeMap.values()) {
                clickProgress.remove(activeKey);
                lastClickTime.remove(activeKey);
            }
            activeProgressionsByType.remove(playerUUID);
            VentrysJob.LOGGER.debug("Progressions nettoyées pour le joueur UUID: {}", playerUUID);
        }
    }

    public static void clearAllPlayerData(Player player) {
        resetPlayerProgress(player);
        VentrysJob.LOGGER.debug("Toutes les données nettoyées pour le joueur: {}",
            player.getName().getString());
    }
}
