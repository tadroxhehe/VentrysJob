package com.ventrys.job.extraction;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import com.ventrys.job.audio.ExtractionSounds;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.data.ToolDurability;
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
    private static final int OAK_EXTRACTION_CLICKS = 5;
    private static final int SAW_CLICKS = 3;
    private static final int MINING_CLICKS = 5;
    private static final int STONE_EXTRACTION_CLICKS = 5;
    private static final int CLAY_EXTRACTION_CLICKS = 3;

    private ExtractionInteractionHandler() {
    }

    public static boolean handleBlockInteraction(Player player, Level level, BlockPos pos,
                                               BlockState state, InteractionHand hand) {
        if (level.isClientSide) return false;

        if (player.isCreative()) return false;

        String playerJob = PlayerJobData.getPlayerJob(player);
        if (playerJob == null) {
            // Ne pas échouer en silence sur une culture (casse déjà annulée côté LeftClick).
            if (com.ventrys.job.data.CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
                player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.require_fork"),
                        player.getUUID());
                return true;
            }
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (serverPlayer.gameMode.getGameModeForPlayer().isCreative()) {
                return false;
            }
        }

        // Scie : seule interaction "extraction" encore utilisée (le reste casse en vanilla,
        // voir BlockBreakEventHandler) — plus de restriction de métier, juste l'outil.
        if (isSawableLog(level, state, pos) && ExtractionConfigRegistry.isSaw(player.getItemInHand(hand))) {
            return handleSawing((ServerPlayer) player, level, pos, hand);
        }

        if ("paysan".equals(playerJob)) {
            if (com.ventrys.job.data.CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
                return handleCropHarvest((ServerPlayer) player, level, pos, state, hand);
            }
        } else if (com.ventrys.job.data.CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.require_fork"),
                    player.getUUID());
            return true;
        }

        return false;
    }

    /**
     * Récolte multi-clics (overlay) : paysan + fourche sur culture mature.
     */
    private static boolean handleCropHarvest(ServerPlayer player, Level level, BlockPos pos,
                                            BlockState state, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (!com.ventrys.job.data.ForkConfig.isFork(heldItem.getItem())
                || !ToolDurability.isUsable(heldItem)) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.require_fork"),
                    player.getUUID());
            return true;
        }

        if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock cropBlock)) {
            return false;
        }

        int currentAge = state.getValue(cropBlock.getAgeProperty());
        if (currentAge < cropBlock.getMaxAge()) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.not_mature"),
                    player.getUUID());
            return true;
        }

        var blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return false;
        }
        var cropConfigOpt = com.ventrys.job.data.ForkConfig.getCropConfig(blockId.toString());
        if (cropConfigOpt.isEmpty()) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.invalid_config"),
                    player.getUUID());
            return true;
        }

        final com.ventrys.job.data.ForkConfig.CropConfig cropConfig = cropConfigOpt.get();
        return handleExtraction(
                player, level, pos, hand,
                "cropharvest",
                com.ventrys.job.data.ForkConfig.getClicksRequired(),
                JobActionEnergyCosts.HARVEST_CROP,
                "Récolte",
                (item, h) -> com.ventrys.job.data.ForkConfig.isFork(item.getItem())
                        && ToolDurability.isUsable(item),
                context -> completeCropHarvest(context.player, context.level, context.pos, context.hand, cropConfig)
        );
    }

    private static void completeCropHarvest(ServerPlayer player, Level level, BlockPos pos,
                                            InteractionHand hand,
                                            com.ventrys.job.data.ForkConfig.CropConfig cropConfig) {
        BlockState state = level.getBlockState(pos);
        if (!com.ventrys.job.data.CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            return;
        }
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.CropBlock cropBlock)) {
            return;
        }
        if (state.getValue(cropBlock.getAgeProperty()) < cropBlock.getMaxAge()) {
            return;
        }

        boolean success = ExtractionDropFactory.giveCropDrops(player, level, pos, cropConfig);
        if (!success) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.invalid_config"),
                    player.getUUID());
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        com.ventrys.job.data.CropGrowthManager.unregisterCrop(level, pos);
        ExtractionSounds.play(level, pos, ExtractionSounds.Kind.CROP);

        ItemStack heldItem = player.getItemInHand(hand);
        if (com.ventrys.job.data.ForkConfig.isFork(heldItem.getItem())) {
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }

    private static boolean handleExtraction(
            ServerPlayer player, Level level, BlockPos pos, InteractionHand hand,
            String extractionType, int requiredClicks, float energyCost, String progressMessage,
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
        if (lastTime != null && (currentTime - lastTime) < 500) {
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
            if (!JobEnergyHelper.consumeForAction(player, energyCost)) {
                clickProgress.remove(key);
                lastClickTime.remove(key);
                Map<String, String> typeMapFail = ExtractionProgressManager.getActiveProgressionsByType().get(playerUUID);
                if (typeMapFail != null) {
                    typeMapFail.remove(finalTypeToUse);
                    if (typeMapFail.isEmpty()) {
                        ExtractionProgressManager.getActiveProgressionsByType().remove(playerUUID);
                    }
                }
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new ExtractionProgressPacket(key, 0, requiredClicks, "")
                );
                return true;
            }
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
            "oak", OAK_EXTRACTION_CLICKS, JobActionEnergyCosts.EXTRACT_LOG, "Progression",
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
            ToolDurability.hurtAndBreak(heldItem, player, InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isSawableLog(Level level, BlockState state, BlockPos pos) {
        // L'ancien garde-fou "hasExtractedTag" exigeait d'être passé par l'extraction à la hache
        // (qui posait le tag). Cette extraction n'existe plus (bûches cassées en vanilla), donc
        // plus rien ne pose jamais ce tag — la scie ne matchait plus aucune bûche. On ne garde
        // que la vérification "ce type de bloc est configuré comme sciable".
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        return ExtractionConfigRegistry.isSawableBlockConfigured(blockId);
    }

    private static boolean isChisel(ItemStack item) {
        return ExtractionConfigRegistry.isChiselTool(item);
    }

    private static boolean handleSawing(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "saw", SAW_CLICKS, JobActionEnergyCosts.EXTRACT_SAW, "Progression",
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

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(config.getDropItem(), config.getDropCount(), null);
        if (!dropItem.isEmpty()) {
            ExtractionDropFactory.spawnDrop(level, pos, dropItem);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.sawing.success"), player.getUUID());
            ExtractionSounds.play(level, pos, ExtractionSounds.Kind.SAW);
        } else {
            level.setBlock(pos, state, 3);
            return;
        }

        ItemStack heldItem = player.getMainHandItem();
        if (ExtractionConfigRegistry.isSaw(heldItem)) {
            ToolDurability.hurtAndBreak(heldItem, player, InteractionHand.MAIN_HAND);
        }
    }

    private static boolean handleMining(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.MiningConfig miningConfig = ExtractionConfigRegistry.getMiningConfigInternal(blockId);
        int requiredClicks = miningConfig != null
            ? miningConfig.resolveClicksRequired(MINING_CLICKS)
            : MINING_CLICKS;
        float energyCost = blockId.contains("verdragon")
            ? JobActionEnergyCosts.EXTRACT_VERDRAGON
            : JobActionEnergyCosts.EXTRACT_ORE;

        return handleExtraction(
            player, level, pos, hand,
            "mine", requiredClicks, energyCost, "Progression",
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
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }

    private static boolean handleStoneExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "stone", STONE_EXTRACTION_CLICKS, JobActionEnergyCosts.EXTRACT_STONE, "Progression",
            (item, h) -> isChisel(item),
            context -> extractStone(context.player, context.level, context.pos, context.hand)
        );
    }

    private static void extractStone(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        BlockState state = level.getBlockState(pos);
        String blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString();
        ExtractionConfigRegistry.StoneConfig config = ExtractionConfigRegistry.getStoneConfigInternal(blockId);

        String dropId = config != null ? config.getDropItem() : "ventrysitem:res_pierre_fragmente";
        int dropCount = config != null ? config.getDropCount() : 1;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        ItemStack dropItem = ExtractionDropFactory.createDropItem(dropId, dropCount, null);
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
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }

    private static boolean handleCalciteExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "calcite", STONE_EXTRACTION_CLICKS, JobActionEnergyCosts.EXTRACT_CALCITE, "Progression",
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
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }

    private static boolean handleSandExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "sand", STONE_EXTRACTION_CLICKS, JobActionEnergyCosts.EXTRACT_SAND, "Progression",
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
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }

    private static boolean handleClayExtraction(ServerPlayer player, Level level, BlockPos pos, InteractionHand hand) {
        return handleExtraction(
            player, level, pos, hand,
            "clay", CLAY_EXTRACTION_CLICKS, JobActionEnergyCosts.EXTRACT_CLAY, "Progression",
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
            ToolDurability.hurtAndBreak(heldItem, player, hand);
        }
    }
}
