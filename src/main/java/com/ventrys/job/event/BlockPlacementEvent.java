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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ventrysjob")
public class BlockPlacementEvent {

    /**
     * Empêche la pose d'un {@link BlockItem} non autorisé, sans bloquer manger / boire
     * (ni l'autre main) quand un bloc est dans le viseur.
     * <p>
     * Important : tourner côté client aussi (sinon le client « mange » le clic en tentative
     * de pose), et renvoyer {@link InteractionResult#PASS} plutôt que {@code FAIL}
     * pour laisser Minecraft retomber sur {@code Item#use} (nourriture).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }

        ItemStack heldItem = player.getItemInHand(event.getHand());

        // Nourriture / boisson dans cette main : ne jamais interférer
        if (heldItem.isEdible()) {
            return;
        }

        if (!(heldItem.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        boolean client = event.getWorld().isClientSide();
        BlockState blockToPlace = blockItem.getBlock().defaultBlockState();
        if (BlockPlacementRules.canPlayerPlaceBlock(player, blockToPlace, event.getPos(), !client)) {
            return;
        }

        // Interdire pose + activation du bloc visé, mais laisser la chaîne d'interaction
        // continuer (autre main, Item#use, etc.). L'item n'est pas consommé ici.
        event.setUseItem(Event.Result.DENY);
        event.setUseBlock(Event.Result.DENY);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.PASS);
        if (!client) {
            BlockPlacementRules.syncInventory(player);
            VentrysJob.LOGGER.debug("Placement de bloc bloqué (RightClickBlock) pour {}.", player.getName().getString());
        }
    }

    /**
     * Filet serveur : si la pose passe malgré le RightClickBlock, annuler.
     * <p>
     * Ne pas « refund » : ForgeHooks restaure déjà le stack à l'annulation de
     * {@link BlockEvent.EntityPlaceEvent} (sinon duplication). On resync le client.
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
        // notify=false : le message a souvent déjà été envoyé au RightClickBlock
        if (BlockPlacementRules.canPlayerPlaceBlock(player, placedState, event.getPos(), false)) {
            return;
        }

        event.setCanceled(true);
        BlockPlacementRules.syncInventory(player);
        VentrysJob.LOGGER.debug("Placement de bloc bloqué (EntityPlaceEvent) pour {} — item conservé via Forge.",
            player.getName().getString());
    }

    /**
     * Gère le placement effectif du bloc (tag extracted, maillet, cultures, énergie).
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
            // Re-check (filet) : sans maillet utilisable → annuler (Forge restaure l'item)
            if (!MalletUsage.hasUsableMallet(serverPlayer)) {
                event.setCanceled(true);
                BlockPlacementRules.syncInventory(player);
                return;
            }
            if (!JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.PLACE_BLOCK)) {
                event.setCanceled(true);
                BlockPlacementRules.syncInventory(player);
                return;
            }
            MalletUsage.applyWearToHeldMallet(serverPlayer);
        }

        if (event.getWorld() instanceof ServerLevel serverLevel) {
            BlockState placedState = event.getPlacedBlock();
            if (CropGrowthConfig.isConfiguredCrop(placedState.getBlock())) {
                CropGrowthManager.registerCrop(serverLevel, pos, placedState);
            }
        }
    }
}
