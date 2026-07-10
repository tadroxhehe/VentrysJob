package com.ventrys.job.init;

import com.ventrys.job.VentrysJob;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Événements sonores définis dans {@code sounds.json} (généré au build depuis {@code ventrysjobsfx/Nouveau dossier/sfx_*}).
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, VentrysJob.MOD_ID);

    public static final RegistryObject<SoundEvent> CRAFT_COUTURIER =
        SOUNDS.register("craft_couturier",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "craft_couturier")));

    public static final RegistryObject<SoundEvent> CRAFT_CUISINIER =
        SOUNDS.register("craft_cuisinier",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "craft_cuisinier")));

    public static final RegistryObject<SoundEvent> CRAFT_FORGERON =
        SOUNDS.register("craft_forgeron",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "craft_forgeron")));

    public static final RegistryObject<SoundEvent> CRAFT_APOTHICAIRE =
        SOUNDS.register("craft_apothicaire",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "craft_apothicaire")));

    public static final RegistryObject<SoundEvent> CRAFT_ARTISAN =
        SOUNDS.register("craft_artisan",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "craft_artisan")));

    public static final RegistryObject<SoundEvent> VASE_PLANTE =
        SOUNDS.register("vase_plante",
            () -> new SoundEvent(new ResourceLocation(VentrysJob.MOD_ID, "vase_plante")));

    private ModSounds() {}
}
