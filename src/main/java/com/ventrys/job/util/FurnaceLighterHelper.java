package com.ventrys.job.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Briquets acceptés pour allumer les fours (vanilla + VentrysItem).
 */
public final class FurnaceLighterHelper {

    private static final ResourceLocation BRONZE_BRIQUET = new ResourceLocation("ventrysitem", "item_bronze_briquet");
    private static final ResourceLocation FER_BRIQUET = new ResourceLocation("ventrysitem", "item_fer_briquet");

    private FurnaceLighterHelper() {}

    public static boolean isLighter(ItemStack stack) {
        return !stack.isEmpty() && isLighter(stack.getItem());
    }

    public static boolean isLighter(Item item) {
        if (item == Items.FLINT_AND_STEEL) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return BRONZE_BRIQUET.equals(key) || FER_BRIQUET.equals(key);
    }
}
