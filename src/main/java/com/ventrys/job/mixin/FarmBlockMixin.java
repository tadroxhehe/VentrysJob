package com.ventrys.job.mixin;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Désactive la logique vanilla du labour (perte d'humidité + retour en dirt) déclenchée par {@code randomTick}.
 * Le suivi {@link com.ventrys.job.util.FarmlandMoistureManager} continue de forcer l'humidité max au labour / chargement de chunk.
 */
@Mixin(FarmBlock.class)
public class FarmBlockMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void ventrysjob$skipFarmlandDrying(BlockState state, ServerLevel level, BlockPos pos, Random random, CallbackInfo ci) {
        ci.cancel();
    }
}
