package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.extraction.ExtractedPositionsStore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Retire le marqueur « extrait » dès qu'un bloc est cassé à cette coordonnée,
 * pour éviter qu'un tronc replacé hérite d'un sciage sans nouvel abattage.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class ExtractedPositionCleanupHandler {

    private ExtractedPositionCleanupHandler() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (ExtractedPositionsStore.isExtractedBlock(level, event.getPos())) {
            ExtractedPositionsStore.unmarkPositionAsExtracted(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (ExtractedPositionsStore.isExtractedBlock(level, pos)) {
            ExtractedPositionsStore.unmarkPositionAsExtracted(level, pos);
        }
    }
}
