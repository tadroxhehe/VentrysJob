package com.ventrys.job.init;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.CreativeModeTab;

/**
 * Onglet créatif centralisé VentrysMobs ({@code itemGroup.tabventrysmobs}), sans dépendance compile-time.
 */
public final class VentrysMobCreativeTab {

    private static final String VENTRYS_MOBS_TAB_KEY = "itemGroup.tabventrysmobs";

    private VentrysMobCreativeTab() {
    }

    public static CreativeModeTab getOrFallback(CreativeModeTab fallback) {
        for (CreativeModeTab tab : CreativeModeTab.TABS) {
            if (tab != null && tab.getDisplayName() instanceof TranslatableComponent tc
                && VENTRYS_MOBS_TAB_KEY.equals(tc.getKey())) {
                return tab;
            }
        }
        return fallback;
    }
}
