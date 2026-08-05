package com.ventrys.job.event;

import com.ventrys.job.data.BlockPlacementRules;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.JobPermissionService;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class ModEvents {
    
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        Level level = event.getWorld();
        
        if (level.isClientSide) return;

        // Torche dans la main qui clique : laisser la pose (pas d'extraction)
        if (BlockPlacementRules.isTorchLikeItem(player.getItemInHand(event.getHand()))) {
            return;
        }

        // Ouvrier : torche en offhand → ne pas consommer le clic main (sinon la pose offhand est annulée)
        if (event.getHand() == InteractionHand.MAIN_HAND
                && JobPermissionService.isOuvrier(player)
                && BlockPlacementRules.isTorchLikeItem(player.getOffhandItem())) {
            return;
        }
        
        // Intercepter l'interaction avec les blocs pour les actions spéciales
        boolean handled = JobActions.handleBlockInteraction(
            player, 
            level, 
            event.getPos(), 
            event.getWorld().getBlockState(event.getPos()), 
            event.getHand()
        );
        
        if (handled) {
            event.setCanceled(true);
        }
    }
}
