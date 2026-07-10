package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomAnimal;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Renforce la persistance des animaux VentrysJob à chaque chargement de chunk / spawn.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class EntityPersistenceHandler {

    private EntityPersistenceHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof CustomAnimal animal) {
            animal.setPersistenceRequired();
        }
    }
}
