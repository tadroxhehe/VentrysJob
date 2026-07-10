package com.ventrys.job.audio;

import com.ventrys.job.init.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Son de craft à la table : packs fournis sous {@code ventrysjobsfx/Nouveau dossier/} (variations .ogg au build).
 */
public final class JobCraftSounds {

    private JobCraftSounds() {}

    public static SoundEvent forJobTable(String jobId) {
        if (jobId == null) {
            return SoundEvents.UI_STONECUTTER_SELECT_RECIPE;
        }
        return switch (jobId) {
            case "couturier" -> ModSounds.CRAFT_COUTURIER.get();
            case "cuisinier" -> ModSounds.CRAFT_CUISINIER.get();
            case "forgeron" -> ModSounds.CRAFT_FORGERON.get();
            case "apothicaire" -> ModSounds.CRAFT_APOTHICAIRE.get();
            case "artisan" -> ModSounds.CRAFT_ARTISAN.get();
            default -> SoundEvents.UI_STONECUTTER_SELECT_RECIPE;
        };
    }
}
