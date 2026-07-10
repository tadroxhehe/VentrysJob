package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.entity.CustomAnimal;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Enregistrement des attributs d'entités custom — hors {@link com.ventrys.job.VentrysJob}
 * pour éviter les soucis d'analyse IDE sur le bus MOD.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributes {

    private ModEntityAttributes() {}

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        VentrysJob.LOGGER.debug("VentrysJob — attributs d'entités enregistrés");
        event.put(ModEntities.CUSTOM_PIG.get(), CustomAnimal.createAttributes().build());
        event.put(ModEntities.CUSTOM_COW.get(), CustomAnimal.createAttributes().build());
        event.put(ModEntities.CUSTOM_CHICKEN.get(), CustomAnimal.createAttributes().build());
        event.put(ModEntities.CUSTOM_SHEEP.get(), CustomAnimal.createAttributes().build());
    }
}
