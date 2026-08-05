package com.ventrys.job.data;

import com.ventrys.job.extraction.ExtractedPositionsStore;
import com.ventrys.job.extraction.ExtractionConfigRegistry;
import com.ventrys.job.extraction.ExtractionDropFactory;
import com.ventrys.job.extraction.ExtractionInteractionHandler;
import com.ventrys.job.extraction.ExtractionProgressManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class JobActions {

    public static class OakConfig extends ExtractionConfigRegistry.OakConfig {
        public OakConfig(String dropItem, int dropCount) {
            super(dropItem, dropCount);
        }
    }

    public static class SawConfig extends ExtractionConfigRegistry.SawConfig {
        public SawConfig(String dropItem, int dropCount) {
            super(dropItem, dropCount);
        }
    }

    public static class MiningConfig extends ExtractionConfigRegistry.MiningConfig {
        public MiningConfig(String dropItem, int dropCount) {
            super(dropItem, dropCount);
        }
    }

    public static class StoneConfig extends ExtractionConfigRegistry.StoneConfig {
        public StoneConfig(String dropItem, int dropCount) {
            super(dropItem, dropCount);
        }
    }

    public static boolean handleBlockInteraction(Player player, Level level, BlockPos pos,
                                               BlockState state, InteractionHand hand) {
        return ExtractionInteractionHandler.handleBlockInteraction(player, level, pos, state, hand);
    }

    public static ItemStack createDropItemPublic(String itemId, int count) {
        return ExtractionDropFactory.createDropItemPublic(itemId, count);
    }

    public static ItemStack createDropItemPublic(String itemId, int count, String displayName) {
        return ExtractionDropFactory.createDropItemPublic(itemId, count, displayName);
    }

    public static boolean isExtractableLog(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableLog(state, pos);
    }

    public static boolean isExtractableOre(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableOre(state, pos);
    }

    public static boolean isExtractableStone(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableStone(state, pos);
    }

    public static boolean isAxe(ItemStack item) {
        return ExtractionConfigRegistry.isAxe(item);
    }

    public static boolean isPickaxe(ItemStack item) {
        return ExtractionConfigRegistry.isPickaxe(item);
    }

    public static void forceCleanup() {
        ExtractionProgressManager.forceCleanup();
    }

    public static void cleanupProgress() {
        ExtractionProgressManager.cleanupProgress();
    }

    public static void resetPlayerProgress(Player player) {
        ExtractionProgressManager.resetPlayerProgress(player);
    }

    public static void clearPlayerProgressions(String playerUUID) {
        ExtractionProgressManager.clearPlayerProgressions(playerUUID);
    }

    public static void clearAllPlayerData(Player player) {
        ExtractionProgressManager.clearAllPlayerData(player);
    }

    public static void setOakExtractionClicks(int clicks) {
        ExtractionConfigRegistry.setOakExtractionClicks(clicks);
    }

    public static void setOakExtractionDrops(int drops) {
        ExtractionConfigRegistry.setOakExtractionDrops(drops);
    }

    public static void markPositionAsExtracted(Level level, BlockPos pos) {
        ExtractedPositionsStore.markPositionAsExtracted(level, pos);
    }

    public static void unmarkPositionAsExtracted(Level level, BlockPos pos) {
        ExtractedPositionsStore.unmarkPositionAsExtracted(level, pos);
    }

    public static boolean isExtractableCalcite(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableCalcite(state, pos);
    }

    public static boolean isExtractableSand(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableSand(state, pos);
    }

    public static boolean isExtractableClay(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isExtractableClay(state, pos);
    }

    public static boolean isShovel(ItemStack item) {
        return ExtractionConfigRegistry.isShovel(item);
    }

    public static boolean isChiselTool(ItemStack item) {
        return ExtractionConfigRegistry.isChiselTool(item);
    }

    public static boolean isAnyExtractableBlock(BlockState state, BlockPos pos) {
        return ExtractionConfigRegistry.isAnyExtractableBlock(state, pos);
    }

    public static boolean isExtractableSaw(BlockState state) {
        return ExtractionConfigRegistry.isExtractableSaw(state);
    }

    public static boolean isExtractedBlock(Level level, BlockPos pos) {
        return ExtractedPositionsStore.isExtractedBlock(level, pos);
    }

    public static boolean isExtractableStone(BlockState state) {
        return ExtractionConfigRegistry.isExtractableStone(state);
    }

    public static boolean isRoughNaturalStone(BlockState state) {
        return ExtractionConfigRegistry.isRoughNaturalStone(state);
    }

    public static boolean isStoneProtectedFromMining(BlockState state) {
        return ExtractionConfigRegistry.isStoneProtectedFromMining(state);
    }

    /** Pierre des mines : granite cassé à la pioche (casse vanilla, sans drop). */
    public static boolean isMineGranite(BlockState state) {
        return state != null && state.is(net.minecraft.world.level.block.Blocks.GRANITE);
    }

    /**
     * Client + serveur : granite + pioche usable → autoriser le début de casse.
     * Le métier n'est pas requis ici (souvent pas encore sync client) ;
     * le serveur valide ouvrier dans BreakEvent.
     */
    public static boolean canAttemptGranitePickaxeBreak(net.minecraft.world.entity.player.Player player, BlockState state) {
        return isMineGranite(state) && player != null && isPickaxe(player.getMainHandItem());
    }

    /**
     * Serveur (et client si métier sync) : ouvrier + pioche → casse granite sans drop.
     * Indépendant des perms LuckPerms / OP.
     */
    public static boolean canOuvrierVanillaBreakGranite(net.minecraft.world.entity.player.Player player, BlockState state) {
        if (!canAttemptGranitePickaxeBreak(player, state)) {
            return false;
        }
        return JobPermissionService.isOuvrier(player);
    }

    public static boolean isExtractableCalcite(BlockState state) {
        return ExtractionConfigRegistry.isExtractableCalcite(state);
    }

    public static boolean isExtractableOre(BlockState state) {
        return ExtractionConfigRegistry.isExtractableOre(state);
    }

    public static boolean giveCropDrops(ServerPlayer player, Level level, BlockPos pos,
                                     ForkConfig.CropConfig cropConfig) {
        return ExtractionDropFactory.giveCropDrops(player, level, pos, cropConfig);
    }

    public static void addSawTool(String itemId) {
        ExtractionConfigRegistry.addSawTool(itemId);
    }

    public static void removeSawTool(String itemId) {
        ExtractionConfigRegistry.removeSawTool(itemId);
    }

    public static void addSawConfig(String logId, String dropItem, int dropCount) {
        ExtractionConfigRegistry.addSawConfig(logId, dropItem, dropCount);
    }

    public static void removeSawConfig(String logId) {
        ExtractionConfigRegistry.removeSawConfig(logId);
    }

    public static void addMiningConfig(String oreId, String dropItem, int dropCount) {
        ExtractionConfigRegistry.addMiningConfig(oreId, dropItem, dropCount);
    }

    public static void removeMiningConfig(String oreId) {
        ExtractionConfigRegistry.removeMiningConfig(oreId);
    }

    public static void addStoneConfig(String stoneId, String dropItem, int dropCount) {
        ExtractionConfigRegistry.addStoneConfig(stoneId, dropItem, dropCount);
    }

    public static void removeStoneConfig(String stoneId) {
        ExtractionConfigRegistry.removeStoneConfig(stoneId);
    }

    public static SawConfig getSawConfig(String logId) {
        ExtractionConfigRegistry.SawConfig config = ExtractionConfigRegistry.getSawConfig(logId);
        return config == null ? null : new SawConfig(config.getDropItem(), config.getDropCount());
    }

    public static String[] listSawableLogs() {
        return ExtractionConfigRegistry.listSawableLogs();
    }

    public static String[] listSawTools() {
        return ExtractionConfigRegistry.listSawTools();
    }

    public static void setOakDropItem(String itemId) {
        ExtractionConfigRegistry.setOakDropItem(itemId);
    }

    public static boolean doesItemExist(String itemId) {
        return ExtractionConfigRegistry.doesItemExist(itemId);
    }

    public static void addOakConfig(String blockId, String dropItem, int dropCount) {
        ExtractionConfigRegistry.addOakConfig(blockId, dropItem, dropCount);
    }

    public static void removeOakConfig(String blockId) {
        ExtractionConfigRegistry.removeOakConfig(blockId);
    }

    public static OakConfig getOakConfig(String blockId) {
        ExtractionConfigRegistry.OakConfig config = ExtractionConfigRegistry.getOakConfig(blockId);
        return config == null ? null : new OakConfig(config.getDropItem(), config.getDropCount());
    }

    public static void listConfiguredBlocks(ServerPlayer player) {
        ExtractionConfigRegistry.listConfiguredBlocks(player);
    }

    public static void shutdownExtractedPositionsSaver() {
        ExtractedPositionsStore.shutdownExtractedPositionsSaver();
    }

    public static void saveExtractedPositions() {
        ExtractedPositionsStore.saveExtractedPositions();
    }

    public static void loadExtractedPositions() {
        ExtractedPositionsStore.loadExtractedPositions();
    }

    public static void loadExtractionToolsConfig() {
        ExtractionConfigRegistry.loadExtractionToolsConfig();
    }
}
