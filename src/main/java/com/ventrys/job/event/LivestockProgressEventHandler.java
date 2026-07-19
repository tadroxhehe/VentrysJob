package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.LivestockProgressManager;
import com.ventrys.job.entity.CustomAnimal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class LivestockProgressEventHandler {

    private LivestockProgressEventHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LivestockProgressManager.handleServerTick(ServerLifecycleHooks.getCurrentServer());
    }

    @SubscribeEvent
    public static void onAnimalDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof CustomAnimal animal)) {
            return;
        }
        if (animal.level instanceof ServerLevel level) {
            LivestockProgressManager.remove(animal.getUUID(), level);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (event.getChunk() instanceof LevelChunk chunk) {
            LivestockProgressManager.registerAnimalsInChunk(level, chunk);
        }
    }
}
