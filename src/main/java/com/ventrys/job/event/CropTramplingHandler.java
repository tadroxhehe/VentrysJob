package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.CropGrowthConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class CropTramplingHandler {
    
    /**
     * Empêche le trampling (piétinement) des cultures configurées
     * Quand un joueur saute sur une culture, cela ne doit rien faire
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        
        BlockPos farmlandPos = event.getPos();
        BlockPos cropPos = farmlandPos.above();
        BlockState cropState = level.getBlockState(cropPos);
        
        // Si c'est une culture configurée, empêcher le trampling
        if (CropGrowthConfig.isConfiguredCrop(cropState.getBlock())) {
            event.setCanceled(true);
            VentrysJob.LOGGER.debug("Trampling de culture configurée empêché à {}", cropPos);
        }
    }
}
