package com.ventrys.job.mixin;

import com.ventrys.job.data.WeatherConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Empêche l'accumulation météo au sol pendant les chutes de neige : intercepte les deux
 * tests effectués par {@code ServerLevel.tickChunk} avant de poser de la neige / de la glace.
 *
 * <p>On ne touche PAS aux méthodes {@code Biome.shouldSnow/shouldFreeze} en général (elles
 * servent ailleurs) : le {@link Redirect} ne s'applique qu'aux appels faits dans {@code tickChunk}.
 * L'effet visuel de neige qui tombe (côté client) reste donc intact.
 */
@Mixin(ServerLevel.class)
public class ServerLevelWeatherMixin {

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean ventrysjob$cancelSnowLayer(Biome biome, LevelReader level, BlockPos pos) {
        if (WeatherConfig.isSnowAccumulationDisabled()) {
            return false;
        }
        return biome.shouldSnow(level, pos);
    }

    @Redirect(
            method = "tickChunk",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome;shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"
            )
    )
    private boolean ventrysjob$cancelIce(Biome biome, LevelReader level, BlockPos pos) {
        if (WeatherConfig.isIceFormationDisabled()) {
            return false;
        }
        return biome.shouldFreeze(level, pos);
    }
}
