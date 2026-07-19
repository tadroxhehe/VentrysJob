package com.ventrys.job.compat;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/**
 * Pont (soft dependency) vers VentrysSurvival pour l'énergie métier.
 */
public final class VentrysSurvivalBridge {

    private static final Method TRY_CONSUME;
    private static final Method GET_ENERGY;
    private static final Method CATCH_UP_REGEN;
    private static final boolean LOADED;

    static {
        Method tryConsume = null;
        Method getEnergy = null;
        Method catchUp = null;
        boolean modPresent = ModList.get().isLoaded("ventryssurvival");
        if (modPresent) {
            try {
                Class<?> api = Class.forName("com.tadrox.ventryssurvival.api.JobEnergyAPI");
                tryConsume = api.getMethod("tryConsume", Player.class, float.class);
                getEnergy = api.getMethod("getJobEnergy", Player.class);
                catchUp = api.getMethod("catchUpPassiveRegen", Player.class);
            } catch (ReflectiveOperationException ignored) {
                modPresent = false;
            }
        }
        TRY_CONSUME = tryConsume;
        GET_ENERGY = getEnergy;
        CATCH_UP_REGEN = catchUp;
        LOADED = modPresent && tryConsume != null;
    }

    private VentrysSurvivalBridge() {
    }

    public static boolean isAvailable() {
        return LOADED;
    }

    public static float getJobEnergy(Player player) {
        if (!LOADED || player == null) {
            return 100f;
        }
        try {
            return (float) GET_ENERGY.invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
            return 100f;
        }
    }

    public static boolean tryConsumeJobEnergy(Player player, float cost) {
        if (!LOADED || player == null || cost <= 0f) {
            return true;
        }
        if (player.isCreative() || player.isSpectator()) {
            return true;
        }
        try {
            return (boolean) TRY_CONSUME.invoke(null, player, cost);
        } catch (ReflectiveOperationException ignored) {
            return true;
        }
    }

    public static void catchUpPassiveRegen(Player player) {
        if (!LOADED || player == null || CATCH_UP_REGEN == null) {
            return;
        }
        try {
            CATCH_UP_REGEN.invoke(null, player);
        } catch (ReflectiveOperationException ignored) {
            // ignore
        }
    }

    public static void sendInsufficientEnergy(ServerPlayer player) {
        if (player != null) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.energy.insufficient"), player.getUUID());
        }
    }
}
