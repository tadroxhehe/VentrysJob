package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.BlockPlacementRules;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.CropGrowthManager;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobPermissionService;
import com.ventrys.job.data.MalletUsage;
import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ventrysjob")
public class BlockPlacementEvent {

    /**
     * Annule le clic avant {@link BlockItem#useOn} (évite la consommation côté client).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(event.getHand());
        if (!(heldItem.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockState blockToPlace = blockItem.getBlock().defaultBlockState();
        if (BlockPlacementRules.canPlayerPlaceBlock(player, blockToPlace, event.getPos())) {
            return;
        }

        event.setCanceled(true);
        VentrysJob.LOGGER.debug("Placement de bloc bloqué (RightClickBlock) pour {}.", player.getName().getString());
    }

    /**
     * Filet serveur : si la pose passe malgré tout, annuler l'événement et rendre le bloc.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityPlaceDenied(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }

        BlockState placedState = event.getPlacedBlock();
        if (BlockPlacementRules.canPlayerPlaceBlock(player, placedState, event.getPos())) {
            return;
        }

        event.setCanceled(true);
        BlockPlacementRules.refundPlacedBlockItem(player, placedState);
        VentrysJob.LOGGER.debug("Placement de bloc bloqué (EntityPlaceEvent) pour {} — item rendu.",
            player.getName().getString());
    }

    /**
     * Gère le placement effectif du bloc (tag extracted, maillet, cultures).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.isCreative()) {
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.hasTag()) {
                var tag = heldItem.getTag();
                if (tag != null && tag.getBoolean("ventrysjob:extracted")) {
                    BlockPos pos = event.getPos();
                    JobActions.markPositionAsExtracted(player.level, pos);
                    VentrysJob.LOGGER.debug("Position marquée comme extraite: {},{},{}", pos.getX(), pos.getY(), pos.getZ());
                }
            }
            return;
        }

        BlockPos pos = event.getPos();

        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.hasTag()) {
            var tag = heldItem.getTag();
            if (tag != null && tag.getBoolean("ventrysjob:extracted")) {
                JobActions.markPositionAsExtracted(player.level, pos);
            }
        }

        ItemStack offhandItem = player.getOffhandItem();
        if (offhandItem.hasTag()) {
            var tag = offhandItem.getTag();
            if (tag != null && tag.getBoolean("ventrysjob:extracted")) {
                JobActions.markPositionAsExtracted(player.level, pos);
            }
        }

        if (JobPermissionService.isBatisseur(player) && player instanceof ServerPlayer serverPlayer) {
            if (MalletUsage.hasMalletInOffhand(serverPlayer)) {
                JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.PLACE_BLOCK);
                MalletUsage.applyWear(serverPlayer, offhandItem);
            }
        }

        if (event.getWorld() instanceof ServerLevel serverLevel) {
            BlockState placedState = event.getPlacedBlock();
            if (CropGrowthConfig.isConfiguredCrop(placedState.getBlock())) {
                CropGrowthManager.registerCrop(serverLevel, pos, placedState);
            }
        }
    }
}
