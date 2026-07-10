package com.ventrys.job.data;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Usure du maillet bâtisseur (pose et casse de blocs décoratifs).
 */
public final class MalletUsage {

    private MalletUsage() {
    }

    public static boolean hasMalletInOffhand(ServerPlayer player) {
        ItemStack offhand = player.getOffhandItem();
        return !offhand.isEmpty() && MalletConfig.isMallet(offhand.getItem());
    }

    public static void applyWear(ServerPlayer player, ItemStack malletStack) {
        if (malletStack.isEmpty() || !MalletConfig.isMallet(malletStack.getItem())) {
            return;
        }

        if (malletStack.isDamageableItem()) {
            malletStack.hurt(1, player.level.random, player);
            if (malletStack.getDamageValue() >= malletStack.getMaxDamage()) {
                player.getOffhandItem().shrink(1);
                player.sendMessage(new TranslatableComponent("ventrysjob.message.block.placement.mallet.broken"), player.getUUID());
            }
            return;
        }

        if (!malletStack.hasTag()) {
            malletStack.getOrCreateTag().putInt("ventrysjob:mallet_durability", 100);
        }
        var tag = malletStack.getTag();
        if (tag == null) {
            return;
        }
        int durability = tag.getInt("ventrysjob:mallet_durability") - 1;
        if (durability <= 0) {
            player.getOffhandItem().shrink(1);
            player.sendMessage(new TranslatableComponent("ventrysjob.message.block.placement.mallet.broken"), player.getUUID());
        } else {
            tag.putInt("ventrysjob:mallet_durability", durability);
        }
    }
}
