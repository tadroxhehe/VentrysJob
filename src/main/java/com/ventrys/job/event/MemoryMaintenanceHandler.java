package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.extraction.ExtractedPositionsStore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Entretien périodique des structures en mémoire qui peuvent grossir sans limite.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class MemoryMaintenanceHandler {

    private static final int MAINTENANCE_INTERVAL_TICKS = 1200;

    private static int tickCounter = 0;

    private MemoryMaintenanceHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;
        if (tickCounter < MAINTENANCE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isRunning()) {
            return;
        }

        int batch = ExtractedPositionsStore.STALE_PURGE_BATCH;
        int removed = 0;
        for (ServerLevel level : server.getAllLevels()) {
            removed += ExtractedPositionsStore.purgeStaleEntries(level, batch);
        }

        if (removed > 0) {
            VentrysJob.LOGGER.debug(
                "Purge positions extraites : {} entrée(s) retirée(s), {} restante(s)",
                removed,
                ExtractedPositionsStore.size()
            );
        }
    }
}
