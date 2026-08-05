package com.ventrys.job.data;

import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Usure du maillet bâtisseur (pose et casse de blocs).
 */
public final class MalletUsage {

    private static final String NBT_DURA = "ventrysjob:mallet_durability";
    private static final int NBT_DEFAULT_DURA = 230;

    private MalletUsage() {
    }

    public static boolean isMallet(ItemStack stack) {
        return ToolDurability.isUsable(stack) && MalletConfig.isMallet(stack.getItem());
    }

    public static boolean hasMalletInOffhand(ServerPlayer player) {
        return isMallet(player.getOffhandItem());
    }

    /** Maillet usable en main principale ou secondaire. */
    public static boolean hasUsableMallet(Player player) {
        if (player == null) {
            return false;
        }
        return isMallet(player.getMainHandItem()) || isMallet(player.getOffhandItem());
    }

    public static InteractionHand findMalletHand(Player player) {
        if (isMallet(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (isMallet(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static void applyWearToHeldMallet(ServerPlayer player) {
        InteractionHand hand = findMalletHand(player);
        if (hand == null) {
            return;
        }
        applyWear(player, player.getItemInHand(hand), hand);
    }

    public static void applyWear(ServerPlayer player, ItemStack malletStack) {
        applyWear(player, malletStack, InteractionHand.OFF_HAND);
    }

    public static void applyWear(ServerPlayer player, ItemStack malletStack, InteractionHand hand) {
        if (malletStack.isEmpty() || !MalletConfig.isMallet(malletStack.getItem())) {
            return;
        }

        if (malletStack.isDamageableItem()) {
            malletStack.hurtAndBreak(1, player, broken ->
                    broken.broadcastBreakEvent(hand));
            if (player.getItemInHand(hand).isEmpty()) {
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
            malletStack.shrink(1);
            player.sendMessage(
                    new TranslatableComponent("ventrysjob.message.block.placement.mallet.broken"),
                    player.getUUID());
        } else {
            tag.putInt(NBT_DURA, durability);
        }
    }
}
