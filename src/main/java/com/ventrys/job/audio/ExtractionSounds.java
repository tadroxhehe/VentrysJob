package com.ventrys.job.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

/**
 * Sons vanilla joués à la fin d’une extraction / récolte réussie (position du bloc).
 */
public final class ExtractionSounds {

    private ExtractionSounds() {}

    public enum Kind {
        /** Hache — bûches */
        WOOD,
        /** Scie — planches à partir de bûches extraites */
        SAW,
        /** Pioche — minerais */
        ORE,
        /** Burin — pierre ou calcite */
        CHISEL,
        /** Pelle — sable */
        SAND,
        /** Pelle — argile */
        CLAY,
        /** Fourche — culture mature */
        CROP
    }

    /**
     * No-op si {@code level} n’est pas un serveur.
     */
    public static void play(Level level, BlockPos pos, Kind kind) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SoundEvent sound = soundFor(kind);
        PositionalSounds.playNearBlock(serverLevel, pos, sound,
            PositionalSounds.EXTRACTION_SOUND_MAX_DISTANCE, 0.95f, 1.05f);
    }

    private static SoundEvent soundFor(Kind k) {
        return switch (k) {
            case WOOD -> SoundEvents.WOOD_BREAK;
            case SAW -> SoundEvents.WOOD_HIT;
            case ORE -> SoundEvents.STONE_BREAK;
            case CHISEL -> SoundEvents.STONE_BREAK;
            case SAND -> SoundEvents.SAND_BREAK;
            case CLAY -> SoundEvents.GRAVEL_BREAK;
            case CROP -> SoundEvents.CROP_BREAK;
        };
    }
}
