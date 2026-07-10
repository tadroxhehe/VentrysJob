package com.ventrys.job.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Résolution de l'œuf métier VentrysItem ({@code res_oeuf}) — seul type accepté par les nids / la ponte.
 */
public final class VentrysItemRefs {

    public static final ResourceLocation RES_OEUF_ID = new ResourceLocation("ventrysitem", "res_oeuf");

    private VentrysItemRefs() {}

    /** {@code null} si l'item n'est pas enregistré (mod absent). */
    public static Item resOeufItemOrNull() {
        return ForgeRegistries.ITEMS.getValue(RES_OEUF_ID);
    }

    public static boolean isResOeuf(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item expected = resOeufItemOrNull();
        return expected != null && stack.getItem() == expected;
    }
}
