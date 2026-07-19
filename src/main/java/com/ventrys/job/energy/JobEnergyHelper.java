package com.ventrys.job.energy;

import com.ventrys.job.compat.VentrysSurvivalBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class JobEnergyHelper {

    private JobEnergyHelper() {
    }

    public static boolean consumeForAction(Player player, float cost) {
        if (player == null || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        if (VentrysSurvivalBridge.tryConsumeJobEnergy(player, cost)) {
            return true;
        }
        VentrysSurvivalBridge.sendInsufficientEnergy(serverPlayer);
        return false;
    }
}
