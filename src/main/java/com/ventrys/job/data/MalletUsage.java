package com.ventrys.job.data;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Usure du maillet bâtisseur (pose et casse de blocs).
 */
public final class MalletUsage {

    private static final String NBT_DURA = "ventrysjob:mallet_durability";
    private static final int NBT_DEFAULT_DURA = 230;

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
            malletStack.hurtAndBreak(1, player, broken ->
                    broken.broadcastBreakEvent(InteractionHand.OFF_HAND));
            if (player.getOffhandItem().isEmpty()) {
                player.sendMessage(
                        new TranslatableComponent("ventrysjob.message.block.placement.mallet.broken"),
                        player.getUUID());
            }
            return;
        }

        // Filet NBT pour d'anciens stacks sans Properties.durability
        var tag = malletStack.getOrCreateTag();
        if (!tag.contains(NBT_DURA)) {
            tag.putInt(NBT_DURA, NBT_DEFAULT_DURA);
        }
        int durability = tag.getInt(NBT_DURA) - 1;
        if (durability <= 0) {
            player.getOffhandItem().shrink(1);
            player.sendMessage(
                    new TranslatableComponent("ventrysjob.message.block.placement.mallet.broken"),
                    player.getUUID());
        } else {
            tag.putInt(NBT_DURA, durability);
        }
    }
}
