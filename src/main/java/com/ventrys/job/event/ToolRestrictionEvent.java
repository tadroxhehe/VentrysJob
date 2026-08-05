package com.ventrys.job.event;

import com.ventrys.job.data.ForkConfig;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobPermissionService;
import com.ventrys.job.data.ToolDurability;
import com.ventrys.job.energy.JobActionEnergyCosts;
import com.ventrys.job.energy.JobEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "ventrysjob")
public class ToolRestrictionEvent {
    
    /**
     * Désactive le stripping vanilla des bûches pour éviter les conflits avec notre système d'extraction
     */
    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        // Vérifier si c'est une action de stripping avec une hache
        if (event.getToolAction() == ToolActions.AXE_STRIP) {
            BlockState state = event.getState();
            BlockPos pos = event.getPos();
            
            // Si c'est une bûche extractible, désactiver le stripping vanilla
            if (JobActions.isExtractableLog(state, pos)) {
                event.setCanceled(true);
            }
        }
        
        // RESTRICTION : Seul le métier paysan peut labourer, et uniquement avec une fourche
        // Bloquer toutes les houes vanilla pour le métier paysan
        if (event.getToolAction() == ToolActions.HOE_TILL) {
            if (event.getPlayer() != null && event.getHeldItemStack() != null) {
                // Si c'est un paysan, bloquer les houes vanilla (seules les fourches sont autorisées)
                if (JobPermissionService.isPaysan(event.getPlayer())) {
                    // Vérifier si c'est une fourche configurée
                    if (ForkConfig.isFork(event.getHeldItemStack().getItem())
                            && ToolDurability.isUsable(event.getHeldItemStack())) {
                        // Permettre le labour avec une fourche - ne pas bloquer l'événement
                        return;
                    } else {
                        // Bloquer les houes vanilla pour le métier paysan
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        ItemStack heldItem = player.getItemInHand(event.getHand());
        BlockState state = event.getWorld().getBlockState(event.getPos());

        // Permettre l'utilisation normale en mode créatif
        if (player.isCreative()) {
            return;
        }

        // Pose de torche : ne jamais bloquer (ouvrier / tout métier)
        if (com.ventrys.job.data.BlockPlacementRules.isTorchLikeItem(heldItem)) {
            return;
        }

        // RESTRICTION : Seul le job paysan peut labourer (till farmland)
        // Utiliser la liste configurable des fourches au lieu des houes
        if (ForkConfig.isFork(heldItem.getItem()) && ToolDurability.isUsable(heldItem)) {
            // Vérifier si c'est de la terre ou de l'herbe (peut être labouré)
            if (state.getBlock() == net.minecraft.world.level.block.Blocks.DIRT || 
                state.getBlock() == net.minecraft.world.level.block.Blocks.GRASS_BLOCK) {
                if (!JobPermissionService.isPaysan(player)) {
                    event.setCanceled(true);
                    return;
                }
                // Si c'est un paysan avec une fourche, déclencher le labour manuellement
                // Les fourches moddées n'ont pas ToolActions.HOE_TILL, donc on doit le faire manuellement
                if (!event.getWorld().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    BlockPos pos = event.getPos();
                    Level level = event.getWorld();
                    BlockState currentState = level.getBlockState(pos);
                    
                    // Déclencher le labour manuellement
                    BlockState farmlandState = null;
                    if (currentState.getBlock() == net.minecraft.world.level.block.Blocks.DIRT) {
                        farmlandState = net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState();
                    } else if (currentState.getBlock() == net.minecraft.world.level.block.Blocks.GRASS_BLOCK) {
                        farmlandState = net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState();
                    }
                    
                    if (farmlandState != null) {
                        if (!JobEnergyHelper.consumeForAction(serverPlayer, JobActionEnergyCosts.TILL_FARMLAND)) {
                            event.setCanceled(true);
                            return;
                        }
                        level.setBlock(pos, farmlandState, 11);
                        ToolDurability.hurtAndBreak(heldItem, serverPlayer,
                                net.minecraft.world.InteractionHand.MAIN_HAND);
                        event.setCanceled(true); // Empêcher l'interaction normale
                    }
                }
            }
        }

        // Vérifier si le joueur tient un outil
        if (isRestrictedTool(heldItem)) {
            // Si le joueur n'a pas le métier "ouvrier", empêcher l'utilisation
            if (!JobPermissionService.isOuvrier(player)) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Vérifie si l'item est un outil restreint (hache, pelle, pioche, houe)
     */
    private static boolean isRestrictedTool(ItemStack itemStack) {
        return itemStack.canPerformAction(ToolActions.AXE_DIG) ||      // Haches
               itemStack.canPerformAction(ToolActions.SHOVEL_DIG) ||   // Pelles
               itemStack.canPerformAction(ToolActions.PICKAXE_DIG) ||  // Pioches
               ForkConfig.isFork(itemStack.getItem()); // Fourches (liste configurable)
    }
    
}
