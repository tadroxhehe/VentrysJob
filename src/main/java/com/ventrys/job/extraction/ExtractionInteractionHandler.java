package com.ventrys.job.extraction;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.audio.ExtractionSounds;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.network.NetworkHandler;
import com.ventrys.job.network.packet.ExtractionProgressPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class ExtractionInteractionHandler {
    private static final int OAK_EXTRACTION_CLICKS = 10;
    private static final int SAW_CLICKS = 5;
    private static final int MINING_CLICKS = 10;
    private static final int STONE_EXTRACTION_CLICKS = 10;
    private static final int CLAY_EXTRACTION_CLICKS = 5;

    private ExtractionInteractionHandler() {
    }

    public static boolean handleBlockInteraction(Player player, Level level, BlockPos pos,
                                               BlockState state, InteractionHand hand) {
        if (level.isClientSide) return false;

        if (player.isCreative()) return false;

        String playerJob = PlayerJobData.getPlayerJob(player);
        if (playerJob == null) return false;

        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.gameMode.getGameModeForPlayer().isCreative()) {
                return false;
            }
        }

        if ("ouvrier".equals(playerJob)) {
            if (ExtractionConfigRegistry.isExtractableLog(state, pos) && ExtractionConfigRegistry.isAxe(player.getItemInHand(hand))) {
                return handleOakExtraction((ServerPlayer) player, level, pos, hand);
            } else if (isSawableLog(level, state, pos) && ExtractionConfigRegistry.isSaw(player.getItemInHand(hand))) {
                return handleSawing((ServerPlayer) player, level, pos, hand);
            } else if (ExtractionConfigRegistry.isExtractableOre(state, pos) && ExtractionConfigRegistry.isPickaxe(player.getItemInHand(hand))) {
                return handleMining((ServerPlayer) player, level, pos, hand);
            } else if (ExtractionConfigRegistry.isExtractableStone(state, pos) && isChisel(player.getItemInHand(hand))) {
                return handleStoneExtraction((ServerPlayer) player, level, pos, hand);
            } else if (ExtractionConfigRegistry.isExtractableCalcite(state, pos) && isChisel(player.getItemInHand(hand))) {
                return handleCalciteExtraction((ServerPlayer) player, level, pos, hand);
            } else if (ExtractionConfigRegistry.isExtractableSand(state, pos) && ExtractionConfigRegistry.isShovel(player.getItemInHand(hand))) {
                return handleSandExtraction((ServerPlayer) player, level, pos, hand);
            } else if (ExtractionConfigRegistry.isExtractableClay(state, pos) && ExtractionConfigRegistry.isShovel(player.getItemInHand(hand))) {
                return handleClayExtraction((ServerPlayer) player, level, pos, hand);
            }
        }

        return false;
    }

    private static boolean handleExtraction(
            ServerPlayer player, Level level, BlockPos pos, InteractionHand hand,
            String extractionType, int requiredClicks, String progressMessage,
            BiPredicate<ItemStack, InteractionHand> toolChecker,
            Consumer<ExtractionContext> extractionAction) {

        ItemStack heldItem = player.getItemInHand(hand);

        if (!toolChecker.test(heldItem, hand)) {
            return false;
        }

        String playerUUID = player.getUUID().toString();
        String key = ExtractionProgressManager.buildProgressKey(playerUUID, extractionType, pos);
        long currentTime = System.currentTimeMillis();

        String normalizedType = ExtractionProgressManager.getExtractionType(key);
        String typeToUse = normalizedType;

        if (!ExtractionProgressManager.checkAndResetProgress(playerUUID, key, typeToUse)) {
            return true;
        }

        Map<String, Long> lastClickTime = ExtractionProgressManager.getLastClickTime();
        Long lastTime = lastClickTime.get(key);
        if (lastTime != null && (currentTime - lastTime) < 1000) {
            return true;
        }

        lastClickTime.put(key, currentTime);
        ExtractionProgressManager.getActiveProgressionsByType()
            .computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(typeToUse, key);

        final String finalTypeToUse = typeToUse;

        Map<String, Integer> clickProgress = ExtractionProgressManager.getClickProgress();
        int currentClicks = clickProgress.getOrDefault(key, 0);
        currentClicks++;

        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new ExtractionProgressPacket(key, currentClicks, requiredClicks, progressMessage)
        );

        if (currentClicks >= requiredClicks) {
            ExtractionContext context = new ExtractionContext(player, level, pos, hand, heldItem);
            extractionAction.accept(context);
            clickProgress.remove(key);
            lastClickTime.remove(key);
            Map<String, String> typeMap = ExtractionProgressManager.getActiveProgressionsByType().get(playerUUID);
            if (typeMap != null) {
                typeMap.remove(finalTypeToUse);
                if (typeMap.isEmpty()) {
                    ExtractionProgressManager.getActiveProgressionsByType().remove(playerUUID);
                }
            }
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ExtractionProgressPacket(key, requiredClicks, requiredClicks, "")
            );
        } else {
            clickProgress.put(key, currentClicks);
        }

        return true;
    }

    private static class ExtractionContext {
        final ServerPlayer player;
        final Level level;
        final BlockPos pos;
        final InteractionHand hand;
        @SuppressWarnings("unused")
        final ItemStack tool;

        ExtractionContext(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand, ItemStack tool) {
            this.player = player;
            this.level = level;
            this.pos = pos;
            this.hand = hand;
            this.tool = tool;
        }
    }

    private static boolean handleOakExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "oak", OAK_EXTRACTION_CLICKS, "Progression",
            (item, h) -> ExtractionConfigRegistry.isAxe(item),
            context -> extractOakLog(context.player, context.level, context.pos)
        );
    }

    private static void extractOakLog(ServerPlayer player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.OakConfig config = ExtractionConfigRegistry.getOakConfigInternal(blockId);

        if (config == null) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), 1, null);
        if (!dropItem.isEmpty()) {
            dropItem.getOrCreateTag().putBoolean(ExtractedPositionsStore.EXTRACTED_TAG, true);
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.extraction.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.WOOD);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (ExtractionConfigRegistry.isAxe(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean isSawableLog(Level level, BlockState state, BlockPos pos) {
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        boolean isConfigured = ExtractionConfigRegistry.isSawableBlockConfigured(blockId);
        boolean hasTag = hasExtractedTag(level, pos);
        return isConfigured && hasTag;
    }

    private static boolean hasExtractedTag(Level level, BlockPos pos) {
        return ExtractedPositionsStore.isExtractedPosition(level, pos);
    }

    private static boolean isChisel(ItemStack item) {
        return ExtractionConfigRegistry.isChiselTool(item);
    }

    private static boolean handleSawing(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "saw", SAW_CLICKS, "Progression",
            (item, h) -> ExtractionConfigRegistry.isSaw(item),
            context -> sawLog(context.player, context.level, context.pos)
        );
    }

    private static void sawLog(ServerPlayer player, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.SawConfig config = ExtractionConfigRegistry.getSawConfigInternal(blockId);

        if (config == null) {
            return;
        }

        if (!hasExtractedTag(level, pos)) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ExtractedPositionsStore.unmarkPositionAsExtracted(level, pos);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.sawing.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.SAW);
        } else {
            level.setBlock(pos, state, 3);
            ExtractedPositionsStore.markPositionAsExtracted(level, pos);
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (ExtractionConfigRegistry.isSaw(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean handleMining(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.MiningConfig miningConfig = ExtractionConfigRegistry.getMiningConfigInternal(blockId);
        int requiredClicks = miningConfig != null
            ? miningConfig.resolveClicksRequired(MINING_CLICKS)
            : MINING_CLICKS;

        return handleExtraction(
            player, level, pos, hand,
            "mine", requiredClicks, "Progression",
            (item, h) -> ExtractionConfigRegistry.isPickaxe(item),
            context -> mineOre(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void mineOre(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.MiningConfig config = ExtractionConfigRegistry.getMiningConfigInternal(blockId);

        if (config == null) {
            VentrysJob.LOGGER.warn("Configuration introuvable pour minerai: {}", blockId);
            return;
        }

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (dropItem.isEmpty()) {
            VentrysJob.LOGGER.warn("Item de drop introuvable pour minerai {}: {} - le bloc n'a pas été supprimé", blockId, config.getDropItem());
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        ExtractionDropFactory.spawnDrop(level, pos, dropItem);
        player.sendMessage(new TranslatableComponent("ventrysjob.message.extraction.success"), player.getUUID());
        ExtractionSounds.play(level, pos, ExtractionSounds.Kind.ORE);

        ItemStack heldItem = player.getItemInHand(hand);
        if (ExtractionConfigRegistry.isPickaxe(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean handleStoneExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "stone", STONE_EXTRACTION_CLICKS, "Progression",
            (item, h) -> isChisel(item),
            context -> extractStone(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void extractStone(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.StoneConfig config = ExtractionConfigRegistry.getStoneConfigInternal(blockId);

        if (config == null) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.stone_extraction.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.CHISEL);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (isChisel(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean handleCalciteExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "calcite", STONE_EXTRACTION_CLICKS, "Progression",
            (item, h) -> isChisel(item),
            context -> extractCalcite(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void extractCalcite(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.StoneConfig config = ExtractionConfigRegistry.getCalciteConfigInternal(blockId);

        if (config == null) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.calcite_extraction.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.CHISEL);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (isChisel(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean handleSandExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "sand", STONE_EXTRACTION_CLICKS, "Progression",
            (item, h) -> ExtractionConfigRegistry.isShovel(item),
            context -> extractSand(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void extractSand(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.StoneConfig config = ExtractionConfigRegistry.getSandConfigInternal(blockId);

        if (config == null) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.sand_extraction.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.SAND);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (ExtractionConfigRegistry.isShovel(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }

    private static boolean handleClayExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "clay", CLAY_EXTRACTION_CLICKS, "Progression",
            (item, h) -> ExtractionConfigRegistry.isShovel(item),
            context -> extractClay(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void extractClay(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.StoneConfig config = ExtractionConfigRegistry.getClayConfigInternal(blockId);

        if (config == null) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.clay_extraction.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.CLAY);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        if (ExtractionConfigRegistry.isShovel(heldItem)) {
            heldItem.hurt(1, level.random, player);
        }
    }
}
