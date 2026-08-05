package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.CropGrowthManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import com.ventrys.job.util.AgriChunkScanner;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class CropGrowthEventHandler {

    private CropGrowthEventHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        CropGrowthManager.handleServerTick(server);
    }

    @SubscribeEvent
    public static void onCropGrowPre(BlockEvent.CropGrowEvent.Pre event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }

        if (CropGrowthConfig.isConfiguredCrop(event.getState().getBlock())) {
            // Empêcher complètement la croissance vanilla
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            // Enregistrer la culture pour notre système
            CropGrowthManager.registerCrop(level, event.getPos(), event.getState());
        }
    }
    
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        
        // Enregistrer les nouvelles cultures plantées
        if (CropGrowthConfig.isConfiguredCrop(event.getPlacedBlock().getBlock())) {
            CropGrowthManager.registerCrop(level, event.getPos(), event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }

        if (!CropGrowthConfig.isConfiguredCrop(event.getState().getBlock())) {
            return;
        }

        // Créatif : laisser casser, juste désenregistrer
        if (event.getPlayer() != null && event.getPlayer().isCreative()) {
            CropGrowthManager.unregisterCrop(level, event.getPos());
            return;
        }

        // Survie : empêcher la casse vanilla. La récolte multi-clics est gérée par
        // BlockBreakEventHandler (LeftClick) → ExtractionInteractionHandler.
        // Ne pas spammer require_fork ici : faux positif pour paysan + fourche.
        event.setCanceled(true);
    }
    
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk) {
            AgriChunkScanner.scanChunk(level, chunk);
        }
    }
    
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        // À l'arrêt : ne pas toucher aux index (classloader ModLauncher peut déjà refuser
        // de résoudre des classes jamais chargées → Failed to save chunk).
        if (com.ventrys.job.VentrysJob.isShuttingDown()) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isRunning()) {
            return;
        }
        try {
            AgriChunkScanner.forgetChunk(level, event.getChunk().getPos());
        } catch (NoClassDefFoundError | ExceptionInInitializerError t) {
            // Classloader en fermeture / classe absente : ignorer pour ne pas bloquer la save chunk
        }
    }
}

