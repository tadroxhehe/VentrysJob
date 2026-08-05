package com.ventrys.job.data;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Usure du marteau de forgeron (craft sur l'enclume).
 */
public final class HammerUsage {

    private HammerUsage() {
    }

    public static boolean hasHammerInOffhand(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        return !offhand.isEmpty() && HammerConfig.isHammer(offhand.getItem()) && !isBroken(offhand);
    }

    public static boolean isBroken(ItemStack stack) {
        return ToolDurability.isBroken(stack);
    }

    public static void applyWear(ServerPlayer player, ItemStack hammerStack) {
        if (hammerStack.isEmpty() || !HammerConfig.isHammer(hammerStack.getItem())) {
            return;
        }

        if (hammerStack.isDamageableItem()) {
            hammerStack.hurt(1, player.level.random, player);
            if (hammerStack.getDamageValue() >= hammerStack.getMaxDamage()) {
                player.getOffhandItem().shrink(1);
                player.sendMessage(new TranslatableComponent("ventrysjob.message.craft.forgeron.hammer_broken"), player.getUUID());
            }
        }
    }
}
