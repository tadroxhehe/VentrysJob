package com.ventrys.job.data;

import com.ventrys.job.block.JobTableBlock;
import com.ventrys.job.block.MetierTisserBlock;
import com.ventrys.job.compat.VentrysSurvivalBridge;
import com.ventrys.job.energy.JobActionEnergyCosts;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
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
        return canPlayerPlaceBlock(player, blockToPlace, pos, false);
    }

    /**
     * @param notify si true, envoie le message d'erreur au joueur en cas de refus
     */
    public static boolean canPlayerPlaceBlock(Player player, BlockState blockToPlace, BlockPos pos, boolean notify) {
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
            boolean ok = "paysan".equals(playerJob);
            if (!ok && notify) {
                deny(player, "ventrysjob.message.block.placement.paysan.restricted");
            }
            return ok;
        }

        if (JobPermissionService.isPaysan(player)) {
            if (notify) {
                deny(player, "ventrysjob.message.block.placement.paysan.restricted");
            }
            return false;
        }
        if (JobPermissionService.isOuvrier(player)) {
            if (JobActions.isExtractableLog(blockToPlace, pos)) {
                return true;
            }
            boolean ok = blockToPlace.getBlock() == Blocks.OAK_PLANKS;
            if (!ok && notify) {
                deny(player, "ventrysjob.message.block.placement.ouvrier.restricted");
            }
            return ok;
        }
        if (JobPermissionService.isBatisseur(player)) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            if (!MalletUsage.hasMalletInOffhand(serverPlayer)) {
                if (notify) {
                    deny(player, "ventrysjob.message.block.placement.batisseur.no_mallet");
                }
                return false;
            }
            if (VentrysSurvivalBridge.isAvailable()
                    && VentrysSurvivalBridge.getJobEnergy(serverPlayer) < JobActionEnergyCosts.PLACE_BLOCK) {
                if (notify) {
                    VentrysSurvivalBridge.sendInsufficientEnergy(serverPlayer);
                }
                return false;
            }
            return true;
        }
        if (playerJob != null && !playerJob.isEmpty()) {
            if (notify) {
                deny(player, "ventrysjob.message.block.placement.restricted");
            }
            return false;
        }
        return true;
    }

    private static void deny(Player player, String translationKey) {
        player.sendMessage(new TranslatableComponent(translationKey), player.getUUID());
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
