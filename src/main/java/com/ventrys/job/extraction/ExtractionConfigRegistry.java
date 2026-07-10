package com.ventrys.job.extraction;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;
import com.ventrys.job.data.ChiselConfig;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ExtractionConfigRegistry {
    private static final Map<String, OakConfig> OAK_CONFIGS = new HashMap<>();
    private static final Map<String, SawConfig> SAW_CONFIGS = new HashMap<>();
    private static final Map<String, MiningConfig> MINING_CONFIGS = new HashMap<>();
    private static final Map<String, StoneConfig> STONE_CONFIGS = new HashMap<>();
    private static final Map<String, StoneConfig> CALCITE_CONFIGS = new HashMap<>();
    private static final Map<String, StoneConfig> SAND_CONFIGS = new HashMap<>();
    private static final Map<String, StoneConfig> CLAY_CONFIGS = new HashMap<>();

    private static final Map<String, Boolean> SAW_TOOLS = new HashMap<>();
    private static final Map<String, Boolean> PICKAXE_TOOLS = new HashMap<>();
    private static final Map<String, Boolean> AXE_TOOLS = new HashMap<>();
    private static final Map<String, Boolean> SHOVEL_TOOLS = new HashMap<>();

    private ExtractionConfigRegistry() {
    }

    public static class OakConfig {
        private final String dropItem;
        private final int dropCount;

        public OakConfig(String dropItem, int dropCount) {
            this.dropItem = dropItem;
            this.dropCount = dropCount;
        }

        public String getDropItem() { return dropItem; }
        public int getDropCount() { return dropCount; }
    }

    public static class SawConfig {
        private final String dropItem;
        private final int dropCount;

        public SawConfig(String dropItem, int dropCount) {
            this.dropItem = dropItem;
            this.dropCount = dropCount;
        }

        public String getDropItem() { return dropItem; }
        public int getDropCount() { return dropCount; }
    }

    public static class MiningConfig {
        private final String dropItem;
        private final int dropCount;
        private final int clicksRequired;

        public MiningConfig(String dropItem, int dropCount) {
            this(dropItem, dropCount, -1);
        }

        public MiningConfig(String dropItem, int dropCount, int clicksRequired) {
            this.dropItem = dropItem;
            this.dropCount = dropCount;
            this.clicksRequired = clicksRequired;
        }

        public String getDropItem() { return dropItem; }
        public int getDropCount() { return dropCount; }

        public int resolveClicksRequired(int defaultClicks) {
            return clicksRequired > 0 ? clicksRequired : defaultClicks;
        }
    }

    public static class StoneConfig {
        private final String dropItem;
        private final int dropCount;

        public StoneConfig(String dropItem, int dropCount) {
            this.dropItem = dropItem;
            this.dropCount = dropCount;
        }

        public String getDropItem() { return dropItem; }
        public int getDropCount() { return dropCount; }
    }

    static OakConfig getOakConfigInternal(String blockId) {
        return OAK_CONFIGS.get(blockId);
    }

    static SawConfig getSawConfigInternal(String logId) {
        return SAW_CONFIGS.get(logId);
    }

    static MiningConfig getMiningConfigInternal(String oreId) {
        return MINING_CONFIGS.get(oreId);
    }

    static StoneConfig getStoneConfigInternal(String stoneId) {
        return STONE_CONFIGS.get(stoneId);
    }

    static StoneConfig getCalciteConfigInternal(String blockId) {
        return CALCITE_CONFIGS.get(blockId);
    }

    static StoneConfig getSandConfigInternal(String blockId) {
        return SAND_CONFIGS.get(blockId);
    }

    static StoneConfig getClayConfigInternal(String blockId) {
        return CLAY_CONFIGS.get(blockId);
    }

    static boolean isSawableBlockConfigured(String blockId) {
        return SAW_CONFIGS.containsKey(blockId);
    }

    static boolean isSaw(ItemStack itemStack) {
        if (itemStack.isEmpty()) return false;

        String itemId = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
        if (SAW_TOOLS.containsKey(itemId)) {
            return true;
        }
        return false;
    }

    public static boolean isExtractableLog(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        boolean isConfigured = OAK_CONFIGS.containsKey(blockId);
        if (isConfigured) {
            VentrysJob.LOGGER.debug("Bloc {} - Configuré: {}", blockId, isConfigured);
        }
        return isConfigured;
    }

    public static boolean isExtractableOre(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        boolean isConfigured = MINING_CONFIGS.containsKey(blockId);
        if (isConfigured) {
            VentrysJob.LOGGER.debug("Minerai {} - Configuré: {}", blockId, isConfigured);
        }
        return isConfigured;
    }

    public static boolean isExtractableStone(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        boolean isConfigured = STONE_CONFIGS.containsKey(blockId);
        if (isConfigured) {
            VentrysJob.LOGGER.debug("Pierre {} - Configuré: {}", blockId, isConfigured);
        }
        return isConfigured;
    }

    public static boolean isExtractableCalcite(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return CALCITE_CONFIGS.containsKey(blockId);
    }

    public static boolean isExtractableSand(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return SAND_CONFIGS.containsKey(blockId);
    }

    public static boolean isExtractableClay(BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return CLAY_CONFIGS.containsKey(blockId);
    }

    public static boolean isAxe(ItemStack item) {
        if (item.isEmpty()) return false;

        String itemId = ForgeRegistries.ITEMS.getKey(item.getItem()).toString();

        if (AXE_TOOLS.containsKey(itemId)) {
            return true;
        }

        if (PICKAXE_TOOLS.containsKey(itemId)) {
            return false;
        }

        if (item.canPerformAction(ToolActions.AXE_DIG)) {
            Item itemType = item.getItem();
            if (itemType != Items.WOODEN_PICKAXE &&
                itemType != Items.STONE_PICKAXE &&
                itemType != Items.IRON_PICKAXE &&
                itemType != Items.GOLDEN_PICKAXE &&
                itemType != Items.DIAMOND_PICKAXE &&
                itemType != Items.NETHERITE_PICKAXE) {
                return true;
            }
        }

        Item itemType = item.getItem();
        return itemType == Items.WOODEN_AXE ||
               itemType == Items.STONE_AXE ||
               itemType == Items.IRON_AXE ||
               itemType == Items.GOLDEN_AXE ||
               itemType == Items.DIAMOND_AXE ||
               itemType == Items.NETHERITE_AXE;
    }

    public static boolean isPickaxe(ItemStack item) {
        if (item.isEmpty()) return false;

        String itemId = ForgeRegistries.ITEMS.getKey(item.getItem()).toString();

        if (PICKAXE_TOOLS.containsKey(itemId)) {
            return true;
        }

        if (AXE_TOOLS.containsKey(itemId)) {
            return false;
        }

        if (item.canPerformAction(ToolActions.PICKAXE_DIG)) {
            Item itemType = item.getItem();
            if (itemType != Items.WOODEN_AXE &&
                itemType != Items.STONE_AXE &&
                itemType != Items.IRON_AXE &&
                itemType != Items.GOLDEN_AXE &&
                itemType != Items.DIAMOND_AXE &&
                itemType != Items.NETHERITE_AXE) {
                return true;
            }
        }

        Item itemType = item.getItem();
        return itemType == Items.WOODEN_PICKAXE ||
               itemType == Items.STONE_PICKAXE ||
               itemType == Items.IRON_PICKAXE ||
               itemType == Items.GOLDEN_PICKAXE ||
               itemType == Items.DIAMOND_PICKAXE ||
               itemType == Items.NETHERITE_PICKAXE;
    }

    public static boolean isShovel(ItemStack item) {
        if (item.isEmpty()) return false;

        String itemId = ForgeRegistries.ITEMS.getKey(item.getItem()).toString();
        if (SHOVEL_TOOLS.containsKey(itemId)) {
            return true;
        }

        if (item.canPerformAction(ToolActions.SHOVEL_DIG)) {
            return true;
        }

        Item itemType = item.getItem();
        return itemType == Items.WOODEN_SHOVEL ||
               itemType == Items.STONE_SHOVEL ||
               itemType == Items.IRON_SHOVEL ||
               itemType == Items.GOLDEN_SHOVEL ||
               itemType == Items.DIAMOND_SHOVEL ||
               itemType == Items.NETHERITE_SHOVEL;
    }

    public static boolean isChiselTool(ItemStack item) {
        if (item.isEmpty()) return false;
        return ChiselConfig.isChisel(item.getItem());
    }

    public static boolean isAnyExtractableBlock(BlockState state, BlockPos pos) {
        return isExtractableLog(state, pos) ||
               isExtractableOre(state, pos) ||
               isExtractableStone(state, pos) ||
               isExtractableCalcite(state, pos) ||
               isExtractableSand(state, pos) ||
               isExtractableClay(state, pos) ||
               isExtractableSaw(state);
    }

    public static boolean isExtractableSaw(BlockState state) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return SAW_CONFIGS.containsKey(blockId);
    }

    public static boolean isExtractableStone(BlockState state) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return STONE_CONFIGS.containsKey(blockId);
    }

    /** Pierre brute vanilla non encore listée dans stone_configs (filet de sécurité). */
    public static boolean isRoughNaturalStone(BlockState state) {
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            return true;
        }
        Block block = state.getBlock();
        return block == Blocks.DEEPSLATE
                || block == Blocks.COBBLED_DEEPSLATE
                || block == Blocks.COBBLESTONE
                || block == Blocks.TUFF;
    }

    /** Pierre minée uniquement via extraction au burin — jamais à la main. */
    public static boolean isStoneProtectedFromMining(BlockState state) {
        return isExtractableStone(state) || isRoughNaturalStone(state);
    }

    public static boolean isExtractableCalcite(BlockState state) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return CALCITE_CONFIGS.containsKey(blockId);
    }

    public static boolean isExtractableOre(BlockState state) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return MINING_CONFIGS.containsKey(blockId);
    }

    public static void setOakExtractionClicks(int clicks) {
        // Note: Cette méthode nécessiterait de rendre la variable non-final
        // Pour l'instant, changez directement la constante OAK_EXTRACTION_CLICKS
    }

    public static void setOakExtractionDrops(int drops) {
        // Note: Cette méthode nécessiterait de rendre la variable non-final
        // Pour l'instant, changez directement la constante OAK_EXTRACTION_DROPS
    }

    public static void addSawTool(String itemId) {
        SAW_TOOLS.put(itemId, true);
    }

    public static void removeSawTool(String itemId) {
        SAW_TOOLS.remove(itemId);
    }

    public static void addSawConfig(String logId, String dropItem, int dropCount) {
        SAW_CONFIGS.put(logId, new SawConfig(dropItem, dropCount));
    }

    public static void removeSawConfig(String logId) {
        SAW_CONFIGS.remove(logId);
    }

    public static void addMiningConfig(String oreId, String dropItem, int dropCount) {
        MINING_CONFIGS.put(oreId, new MiningConfig(dropItem, dropCount));
    }

    public static void addMiningConfig(String oreId, String dropItem, int dropCount, int clicksRequired) {
        MINING_CONFIGS.put(oreId, new MiningConfig(dropItem, dropCount, clicksRequired));
    }

    public static void removeMiningConfig(String oreId) {
        MINING_CONFIGS.remove(oreId);
    }

    public static void addStoneConfig(String stoneId, String dropItem, int dropCount) {
        STONE_CONFIGS.put(stoneId, new StoneConfig(dropItem, dropCount));
    }

    public static void removeStoneConfig(String stoneId) {
        STONE_CONFIGS.remove(stoneId);
    }

    public static SawConfig getSawConfig(String logId) {
        return SAW_CONFIGS.get(logId);
    }

    public static String[] listSawableLogs() {
        return SAW_CONFIGS.keySet().toArray(new String[0]);
    }

    public static String[] listSawTools() {
        return SAW_TOOLS.keySet().toArray(new String[0]);
    }

    public static void setOakDropItem(String itemId) {
        // Note: Cette méthode nécessiterait de rendre la variable non-final
        // Pour l'instant, changez directement la constante OAK_DROP_ITEM
    }

    public static boolean doesItemExist(String itemId) {
        try {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            return item != null;
        } catch (Exception e) {
            return false;
        }
    }

    public static void addOakConfig(String blockId, String dropItem, int dropCount) {
        OAK_CONFIGS.put(blockId, new OakConfig(dropItem, dropCount));
    }

    public static void removeOakConfig(String blockId) {
        OAK_CONFIGS.remove(blockId);
    }

    public static OakConfig getOakConfig(String blockId) {
        return OAK_CONFIGS.get(blockId);
    }

    public static void listConfiguredBlocks(ServerPlayer player) {
        player.sendMessage(new TranslatableComponent("ventrysjob.command.job.config.extractable"), player.getUUID());
        for (Map.Entry<String, OakConfig> entry : OAK_CONFIGS.entrySet()) {
            OakConfig config = entry.getValue();
            player.sendMessage(new TranslatableComponent("ventrysjob.command.job.config.item", entry.getKey(), config.getDropCount(), config.getDropItem()), player.getUUID());
        }
    }

    public static void loadExtractionToolsConfig() {
        SAW_TOOLS.clear();
        PICKAXE_TOOLS.clear();
        AXE_TOOLS.clear();
        SHOVEL_TOOLS.clear();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject extractionConfig = null;

        try (InputStream resourceStream = ExtractionConfigRegistry.class.getResourceAsStream("/data/ventrysjob/extraction_config.json")) {
            if (resourceStream != null) {
                try (InputStreamReader reader = new InputStreamReader(resourceStream, StandardCharsets.UTF_8)) {
                    JsonObject json = gson.fromJson(reader, JsonObject.class);
                    extractionConfig = json.getAsJsonObject("extraction_config");

                    if (extractionConfig != null) {
                        loadToolIds(extractionConfig, "saw_tools", SAW_TOOLS, "scie");
                        loadToolIds(extractionConfig, "pickaxe_tools", PICKAXE_TOOLS, "pioche");
                        loadToolIds(extractionConfig, "axe_tools", AXE_TOOLS, "hache");
                        loadToolIds(extractionConfig, "shovel_tools", SHOVEL_TOOLS, "pelle");
                    }
                }
            }
        } catch (Exception e) {
            VentrysJob.LOGGER.error("Erreur lors du chargement de la configuration des outils d'extraction: {}", e.getMessage());
            loadDefaultTools();
        }

        if (SAW_TOOLS.isEmpty() && PICKAXE_TOOLS.isEmpty() && AXE_TOOLS.isEmpty() && SHOVEL_TOOLS.isEmpty()) {
            VentrysJob.LOGGER.warn("Aucune configuration d'outils d'extraction, utilisation des valeurs par défaut");
            loadDefaultTools();
        }

        VentrysJob.LOGGER.debug("Outils d'extraction — scies: {}, pioches: {}, haches: {}, pelles: {}",
            SAW_TOOLS.size(), PICKAXE_TOOLS.size(), AXE_TOOLS.size(), SHOVEL_TOOLS.size());

        OAK_CONFIGS.clear();
        SAW_CONFIGS.clear();
        MINING_CONFIGS.clear();
        STONE_CONFIGS.clear();
        CALCITE_CONFIGS.clear();
        SAND_CONFIGS.clear();
        CLAY_CONFIGS.clear();

        loadOakConfigs(extractionConfig);
        loadSawConfigs(extractionConfig);
        loadMiningConfigs(extractionConfig);
        loadStoneConfigs(extractionConfig);
        loadCalciteConfigs(extractionConfig);
        loadSandConfigs(extractionConfig);
        loadClayConfigs(extractionConfig);
    }

    private static void loadToolIds(JsonObject extractionConfig, String arrayKey, Map<String, Boolean> targetTools, String label) {
        JsonArray toolsArray = extractionConfig.getAsJsonArray(arrayKey);
        if (toolsArray == null) {
            return;
        }

        for (JsonElement element : toolsArray) {
            String itemId = element.getAsString();
            if (isValidItem(itemId)) {
                targetTools.put(itemId, true);
                VentrysJob.LOGGER.debug("{} acceptée: {}", label, itemId);
            } else {
                VentrysJob.LOGGER.warn("Item {} invalide ignoré: {}", label, itemId);
            }
        }
    }

    @FunctionalInterface
    private interface ConfigLoader {
        void put(String blockId, String dropItem, int dropCount);
    }

    private static void loadBlockDropConfigs(JsonObject extractionConfig, String arrayKey, String label, ConfigLoader loader) {
        if (extractionConfig == null) {
            return;
        }

        JsonArray configsArray = extractionConfig.getAsJsonArray(arrayKey);
        if (configsArray == null) {
            return;
        }

        for (JsonElement element : configsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject configObj = element.getAsJsonObject();
            if (!configObj.has("block_id") || !configObj.has("drop_item")) {
                continue;
            }

            String blockId = configObj.get("block_id").getAsString();
            String dropItem = configObj.get("drop_item").getAsString();
            int dropCount = configObj.has("drop_count") ? configObj.get("drop_count").getAsInt() : 1;
            loader.put(blockId, dropItem, dropCount);
            VentrysJob.LOGGER.debug("Configuration de {} chargée: {} -> {} x{}", label, blockId, dropItem, dropCount);
        }
    }

    private static void loadOakConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "oak_configs", "oak",
            (blockId, dropItem, dropCount) -> OAK_CONFIGS.put(blockId, new OakConfig(dropItem, dropCount)));
    }

    private static void loadSawConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "saw_configs", "saw",
            (blockId, dropItem, dropCount) -> SAW_CONFIGS.put(blockId, new SawConfig(dropItem, dropCount)));
    }

    private static void loadMiningConfigs(JsonObject extractionConfig) {
        if (extractionConfig == null) {
            return;
        }

        JsonArray configsArray = extractionConfig.getAsJsonArray("mining_configs");
        if (configsArray == null) {
            return;
        }

        for (JsonElement element : configsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject configObj = element.getAsJsonObject();
            if (!configObj.has("block_id") || !configObj.has("drop_item")) {
                continue;
            }

            String blockId = configObj.get("block_id").getAsString();
            String dropItem = configObj.get("drop_item").getAsString();
            int dropCount = configObj.has("drop_count") ? configObj.get("drop_count").getAsInt() : 1;
            int clicksRequired = configObj.has("clicks_required") ? configObj.get("clicks_required").getAsInt() : -1;
            MINING_CONFIGS.put(blockId, new MiningConfig(dropItem, dropCount, clicksRequired));
            VentrysJob.LOGGER.debug("Configuration de mining chargée: {} -> {} x{} ({} clics)",
                blockId, dropItem, dropCount, clicksRequired > 0 ? clicksRequired : "défaut");
        }
    }

    private static void loadStoneConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "stone_configs", "stone",
            (blockId, dropItem, dropCount) -> STONE_CONFIGS.put(blockId, new StoneConfig(dropItem, dropCount)));
    }

    private static void loadCalciteConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "calcite_configs", "calcite",
            (blockId, dropItem, dropCount) -> CALCITE_CONFIGS.put(blockId, new StoneConfig(dropItem, dropCount)));
    }

    private static void loadSandConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "sand_configs", "sand",
            (blockId, dropItem, dropCount) -> SAND_CONFIGS.put(blockId, new StoneConfig(dropItem, dropCount)));
    }

    private static void loadClayConfigs(JsonObject extractionConfig) {
        loadBlockDropConfigs(extractionConfig, "clay_configs", "clay",
            (blockId, dropItem, dropCount) -> CLAY_CONFIGS.put(blockId, new StoneConfig(dropItem, dropCount)));
    }

    private static void loadDefaultTools() {
        SAW_TOOLS.clear();
        PICKAXE_TOOLS.clear();
        AXE_TOOLS.clear();
        SHOVEL_TOOLS.clear();
    }

    private static boolean isValidItem(String itemId) {
        try {
            ResourceLocation rl = new ResourceLocation(itemId);
            return rl != null;
        } catch (Exception e) {
            return false;
        }
    }
}
