package com.ventrys.job.data;

import com.ventrys.job.block.JobTableBlock;
import com.ventrys.job.block.MetierTisserBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Règles métier pour la pose de blocs (VentrysJob).
 */
public final class BlockPlacementRules {

    private BlockPlacementRules() {
    }

    public static boolean canPlayerPlaceBlock(Player player, BlockState blockToPlace, BlockPos pos) {
        if (player.isCreative()) {
            return true;
        }

        Block placedBlock = blockToPlace.getBlock();
        if (placedBlock instanceof JobTableBlock || placedBlock instanceof MetierTisserBlock) {
            return true;
        }
        if (placedBlock == Blocks.DIRT) {
            return true;
        }
        if (JobPermissionService.isUnrestrictedVentrysJobBlock(blockToPlace, pos)) {
            return true;
        }

        String playerJob = JobPermissionService.getJob(player);
        if (CropGrowthConfig.isConfiguredCrop(placedBlock)) {
            return "paysan".equals(playerJob);
        }

        if (JobPermissionService.isPaysan(player)) {
            return false;
        }
        if (JobPermissionService.isOuvrier(player)) {
            if (JobActions.isExtractableLog(blockToPlace, pos)) {
                return true;
            }
            return blockToPlace.getBlock() == Blocks.OAK_PLANKS;
        }
        if (JobPermissionService.isBatisseur(player)) {
            ItemStack offhandItem = player.getOffhandItem();
            return !offhandItem.isEmpty() && MalletConfig.isMallet(offhandItem.getItem());
        }
        if (playerJob != null && !playerJob.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Rend un bloc consommé alors que la pose a été refusée (filet de sécurité côté serveur).
     */
    public static void refundPlacedBlockItem(Player player, BlockState placedState) {
        if (player.getAbilities().instabuild) {
            return;
        }
        Item blockItem = placedState.getBlock().asItem();
        if (blockItem == Items.AIR) {
            return;
        }
        ItemStack refund = new ItemStack(blockItem, 1);
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
    }
}
