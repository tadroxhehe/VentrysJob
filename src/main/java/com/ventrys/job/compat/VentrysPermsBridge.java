package com.ventrys.job.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

public final class VentrysPermsBridge {

    private static final Method CAN;
    private static final boolean LOADED;

    static {
        Method method = null;
        boolean modPresent = ModList.get().isLoaded("ventryspermissions");
        if (modPresent) {
            try {
                method = Class.forName("com.ventrys.permissions.api.VentrysPerms")
                        .getMethod("can", CommandSourceStack.class, String.class);
            } catch (ReflectiveOperationException ignored) {
                modPresent = false;
            }
        }
        CAN = method;
        LOADED = modPresent && method != null;
    }

    private VentrysPermsBridge() {
    }

    public static boolean staff(CommandSourceStack source, String permissionId) {
        if (LOADED) {
            try {
                return (boolean) CAN.invoke(null, source, permissionId);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return source.hasPermission(2);
    }
}
