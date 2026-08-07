package com.ventrys.job.event;

import com.ventrys.job.data.BlockBreakRules;
import com.ventrys.job.data.BlockPlacementRules;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.FurnitureAccess;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobPermissionService;
import com.ventrys.job.data.MalletUsage;
import com.ventrys.job.data.ToolDurability;
import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Casse de blocs : règles métier uniquement (pas LuckPerms / permission-level).
 * Le créatif contourne ; la survie suit ouvrier / bâtisseur / terrain libre.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class BlockBreakEventHandler {
    
    /**
     * Filet de sécurité : vitesse 0 côté client ET serveur, après les mods qui boostent les outils.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakSpeedExtractable(PlayerEvent.BreakSpeed event) {
        if (event.getPlayer().isCreative()) {
            return;
        }
        Player p = event.getPlayer();
        BlockState st = event.getState();
        if (FurnitureAccess.isFurniture(st) || BlockBreakRules.isNarrationTextBlock(st)
                || (BlockPlacementRules.isOuvrierMineSupportBlock(st) && JobPermissionService.isOuvrier(p))) {
            return;
        }
        // Cultures : récolte multi-clics (pas de casse vanilla)
        if (CropGrowthConfig.isConfiguredCrop(st.getBlock())) {
            event.setNewSpeed(0.0f);
            return;
        }
        BlockPos pos = event.getPos();
        if (canBatisseurMalletBreak(p, st, pos)) {
            float speed = BlockBreakRules.requiresBatisseur(st)
                    ? BlockBreakRules.capDecorativeBreakSpeed(event.getNewSpeed())
                    : BlockBreakRules.malletBreakSpeed(event.getNewSpeed());
            event.setNewSpeed(speed);
            return;
        }
        // Granite (mines) : pioche — autoriser la vitesse même si le métier n'est pas sync client
        if (JobActions.canAttemptGranitePickaxeBreak(p, st)) {
            return;
        }
        if (JobActions.isMineGranite(st)) {
            event.setNewSpeed(0.0f);
            return;
        }
        if (JobActions.isAnyExtractableBlock(st, pos) || JobActions.isStoneProtectedFromMining(st)
                || BlockBreakRules.requiresBatisseur(st)) {
            event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        BlockPos pos = event.getPos();
        Level level = event.getWorld();
        BlockState state = level.getBlockState(pos);
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setCanceled(true);
            return;
        }

        if (FurnitureAccess.isFurniture(state) || BlockBreakRules.isNarrationTextBlock(state)
                || (BlockPlacementRules.isOuvrierMineSupportBlock(state) && JobPermissionService.isOuvrier(player))) {
            return;
        }

        // Cultures : multi-clics fourche (overlay), comme l'extraction ouvrier
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer && !level.isClientSide) {
                InteractionHand harvestHand = InteractionHand.MAIN_HAND;
                ItemStack main = player.getItemInHand(InteractionHand.MAIN_HAND);
                ItemStack off = player.getItemInHand(InteractionHand.OFF_HAND);
                if (!com.ventrys.job.data.ForkConfig.isFork(main.getItem())
                        && com.ventrys.job.data.ForkConfig.isFork(off.getItem())
                        && ToolDurability.isUsable(off)) {
                    harvestHand = InteractionHand.OFF_HAND;
                }
                JobActions.handleBlockInteraction(serverPlayer, level, pos, state, harvestHand);
                // Les messages d'erreur (pas paysan / pas fourche / immature) sont déjà
                // envoyés par ExtractionInteractionHandler — ne pas en renvoyer un second.
            }
            return;
        }

        if (canBatisseurMalletBreak(player, state, pos)) {
            return;
        }

        // Granite = pierre des mines : pioche (métier validé serveur au BreakEvent)
        if (JobActions.isMineGranite(state)) {
            if (JobActions.canAttemptGranitePickaxeBreak(player, state)) {
                return;
            }
            event.setCanceled(true);
            return;
        }

        if (!JobPermissionService.isUnrestrictedVentrysJobBlock(state, pos)) {
            if (JobActions.isAnyExtractableBlock(state, pos) || JobActions.isStoneProtectedFromMining(state)) {
                event.setCanceled(true);
                if (player instanceof ServerPlayer serverPlayer && !level.isClientSide) {
                    JobActions.handleBlockInteraction(serverPlayer, level, pos, state, InteractionHand.MAIN_HAND);
                }
                return;
            }
        }

        // Client + serveur : construction réservée bâtisseur (ne pas exiger ServerPlayer)
        if (BlockBreakRules.requiresBatisseur(state)) {
            if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                event.setCanceled(true);
            }
            return;
        }

        if (JobPermissionService.isUnrestrictedVentrysJobBlock(state, pos)) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (JobActions.isExtractedBlock(level, pos)) {
            event.setCanceled(true);
            return;
        }

        if (shouldPreventBreaking(state, heldItem, serverPlayer)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getPlayer();
        if (player.isCreative()) {
            return;
        }

        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setNewSpeed(0.0f);
            return;
        }

        if (FurnitureAccess.isFurniture(state) || BlockBreakRules.isNarrationTextBlock(state)
                || (BlockPlacementRules.isOuvrierMineSupportBlock(state) && JobPermissionService.isOuvrier(player))) {
            return;
        }

        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setNewSpeed(0.0f);
            return;
        }

        if (canBatisseurMalletBreak(player, state, pos)) {
            float speed = BlockBreakRules.requiresBatisseur(state)
                    ? BlockBreakRules.capDecorativeBreakSpeed(event.getNewSpeed())
                    : BlockBreakRules.malletBreakSpeed(event.getNewSpeed());
            event.setNewSpeed(speed);
            return;
        }

        if (JobActions.canAttemptGranitePickaxeBreak(player, state)) {
            return;
        }
        if (JobActions.isMineGranite(state)) {
            event.setNewSpeed(0.0f);
            return;
        }

        if (JobPermissionService.isUnrestrictedVentrysJobBlock(state, pos)) {
            return;
        }

        if (JobActions.isAnyExtractableBlock(state, pos) || JobActions.isStoneProtectedFromMining(state)) {
            event.setNewSpeed(0.0f);
            return;
        }

        // Client + serveur (plus d'early-return ServerPlayer qui laissait le client incohérent)
        if (BlockBreakRules.requiresBatisseur(state)) {
            if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                event.setNewSpeed(0.0f);
                return;
            }
            event.setNewSpeed(BlockBreakRules.capDecorativeBreakSpeed(event.getNewSpeed()));
            return;
        }

        if (player instanceof ServerPlayer serverPlayer
                && shouldPreventBreaking(state, heldItem, serverPlayer)) {
            event.setNewSpeed(0.0f);
        }
    }
    
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        ItemStack held = player.getMainHandItem();

        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            event.setCanceled(true);
            return;
        }

        // Granite (pierre des mines) : AUCUN drop
        if (JobActions.isMineGranite(state)) {
            boolean ouvrierPick = JobActions.canOuvrierVanillaBreakGranite(player, state);
            // Pas de récup bâtisseur sur granite
            if (!ouvrierPick) {
                event.setCanceled(true);
                return;
            }
            if (!JobEnergyHelper.consumeForAction(player, JobActionEnergyCosts.BREAK_NORMAL)) {
                event.setCanceled(true);
                return;
            }
            event.setCanceled(true);
            removeBlockNoDrop(player, pos, state);
            ToolDurability.hurtAndBreak(held, player, InteractionHand.MAIN_HAND);
            return;
        }

        // Texte HRP : casse libre à la main, aucun drop
        if (BlockBreakRules.isNarrationTextBlock(state)) {
            event.setCanceled(true);
            removeBlockNoDrop(player, pos, state);
            return;
        }

        // Meubles : casse libre + drop item-bloc (loot Westeros souvent vide)
        if (FurnitureAccess.isFurniture(state)) {
            forceDropBlockAsItem(event, player, state, pos);
            return;
        }

        // Supports de mine : ouvrier (libre) OU bâtisseur + maillet (récup)
        if (BlockPlacementRules.isOuvrierMineSupportBlock(state)) {
            if (JobPermissionService.isOuvrier(player)) {
                forceDropBlockAsItem(event, player, state, pos);
                return;
            }
            if (!canBatisseurMalletBreak(player, state, pos)) {
                event.setCanceled(true);
                return;
            }
            // Bâtisseur + maillet : suite dans le bloc canBatisseurMalletBreak ci-dessous
        }

        // Cultures : jamais de casse vanilla (récolte = LeftClick + fourche)
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            event.setCanceled(true);
            return;
        }

        // Bâtisseur + maillet : récupère le bloc cassé (sauf extraction / farm)
        if (canBatisseurMalletBreak(player, state, pos)) {
            float cost = BlockBreakRules.requiresBatisseur(state)
                    ? JobActionEnergyCosts.BREAK_DECORATIVE
                    : JobActionEnergyCosts.BREAK_NORMAL;
            if (!JobEnergyHelper.consumeForAction(player, cost)) {
                event.setCanceled(true);
                return;
            }
            MalletUsage.applyWearToHeldMallet(player);
            forceDropBlockAsItem(event, player, state, pos);
            return;
        }

        if (JobActions.isAnyExtractableBlock(state, pos) || JobActions.isStoneProtectedFromMining(state)) {
            event.setCanceled(true);
            return;
        }
        if (BlockBreakRules.requiresBatisseur(state)) {
            if (!BlockBreakRules.canPlayerBreakBlock(player, state)) {
                event.setCanceled(true);
                return;
            }
            if (!JobEnergyHelper.consumeForAction(player, JobActionEnergyCosts.BREAK_DECORATIVE)) {
                event.setCanceled(true);
                return;
            }
            MalletUsage.applyWearToHeldMallet(player);
            forceDropBlockAsItem(event, player, state, pos);
        }
    }

    /**
     * Bâtisseur + maillet : casse récupérable.
     * Exclut granite, filons / extraction, cultures (farm).
     */
    private static boolean canBatisseurMalletBreak(Player player, BlockState state, BlockPos pos) {
        if (player == null || state == null) {
            return false;
        }
        if (state.getBlock() == Blocks.BEDROCK) {
            return false;
        }
        if (BlockBreakRules.isStaffProtectedUnbreakable(state)) {
            return false;
        }
        if (isBatisseurReclaimExcluded(state, pos)) {
            return false;
        }
        if (!MalletUsage.hasUsableMallet(player)) {
            return false;
        }
        return JobPermissionService.isBatisseur(player);
    }

    /** Granite / extraction / farm : pas de récup maillet. */
    private static boolean isBatisseurReclaimExcluded(BlockState state, BlockPos pos) {
        if (JobActions.isMineGranite(state)) {
            return true;
        }
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            return true;
        }
        BlockPos p = pos != null ? pos : BlockPos.ZERO;
        return JobActions.isAnyExtractableBlock(state, p)
                || JobActions.isStoneProtectedFromMining(state);
    }

    private static void removeBlockNoDrop(ServerPlayer player, BlockPos pos, BlockState state) {
        Level level = player.getLevel();
        level.removeBlock(pos, false);
        level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(pos);
            player.connection.send(new ClientboundBlockUpdatePacket(pos, level.getBlockState(pos)));
        }
    }

    /**
     * Force le drop de l'item-bloc (Westeros / déco : loot souvent vide).
     * Coffres / tonneaux : laisse le vanilla (contenu + conteneur).
     * @return true si casse custom effectuée
     */
    private static boolean forceDropBlockAsItem(BlockEvent.BreakEvent event, ServerPlayer player,
                                               BlockState state, BlockPos pos) {
        var block = state.getBlock();
        if (block instanceof net.minecraft.world.level.block.ChestBlock
                || block instanceof net.minecraft.world.level.block.AbstractChestBlock
                || block instanceof net.minecraft.world.level.block.BarrelBlock) {
            return false;
        }
        ItemStack asItem = new ItemStack(block.asItem());
        if (asItem.isEmpty()) {
            return false;
        }
        event.setCanceled(true);
        removeBlockNoDrop(player, pos, state);
        net.minecraft.world.level.block.Block.popResource(player.getLevel(), pos, asItem);
        return true;
    }

    private static boolean shouldPreventBreaking(BlockState state, ItemStack tool, ServerPlayer player) {
        var blockRegistryName = state.getBlock().getRegistryName();
        if (blockRegistryName == null) return false;

        if (JobActions.isMineGranite(state)) {
            return !JobActions.isPickaxe(tool);
        }

        if (JobActions.isStoneProtectedFromMining(state)) {
            return !JobActions.isChiselTool(tool);
        }
        
        String playerJob = JobPermissionService.getJob(player);
        
        if ("ouvrier".equals(playerJob)) {
            if (JobActions.isExtractableLog(state, BlockPos.ZERO)) {
                return !JobActions.isAxe(tool);
            }
            if (JobActions.isExtractableStone(state)) {
                return !JobActions.isChiselTool(tool);
            }
            if (JobActions.isExtractableCalcite(state)) {
                return !JobActions.isChiselTool(tool);
            }
            if (JobActions.isExtractableSand(state, BlockPos.ZERO)) {
                return !JobActions.isShovel(tool);
            }
            if (JobActions.isExtractableOre(state, BlockPos.ZERO)) {
                return !JobActions.isPickaxe(tool);
            }
            if (JobActions.isExtractableClay(state, BlockPos.ZERO)) {
                return !JobActions.isShovel(tool);
            }
        }
        
        return false;
    }
}
