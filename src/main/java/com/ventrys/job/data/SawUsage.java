package com.ventrys.job.data;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Usure de la scie (craft sur l'établi artisan).
 */
public final class SawUsage {

    private SawUsage() {
    }

    public static boolean hasSawInOffhand(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        return !offhand.isEmpty() && SawConfig.isSaw(offhand.getItem()) && !isBroken(offhand);
    }

    public static boolean isBroken(ItemStack stack) {
        return ToolDurability.isBroken(stack);
    }

    public static void applyWear(ServerPlayer player, ItemStack sawStack) {
        if (sawStack.isEmpty() || !SawConfig.isSaw(sawStack.getItem())) {
            return;
        }

        if (sawStack.isDamageableItem()) {
            sawStack.hurt(1, player.level.random, player);
            if (sawStack.getDamageValue() >= sawStack.getMaxDamage()) {
                player.getOffhandItem().shrink(1);
                player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.artisan.saw_broken"), player.getUUID());
            }
        }
    }
}
