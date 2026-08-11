package com.ventrys.job.data;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Outils à durabilité : un stack à {@code damage >= maxDamage} est cassé
 * et ne doit plus être utilisable (le {@code hurt} vanilla ne le détruit pas toujours).
 */
public final class ToolDurability {

    private ToolDurability() {
    }

    public static boolean isBroken(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return true;
        }
        int max = stack.getItem().getMaxDamage(stack);
        if (max <= 0) {
            return false;
        }
        // Ne pas s'appuyer sur isDamageableItem() : avec Unbreakable ou états limites
        // un outil peut rester en inventaire à damage >= max et rester « utilisable ».
        if (stack.hasTag() && stack.getTag().getBoolean("Unbreakable")) {
            return false;
        }
        return stack.getDamageValue() >= max;
    }

    public static boolean isUsable(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !isBroken(stack);
    }

    public static void hurtAndBreak(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        hurtAndBreak(stack, player, hand, 1);
    }

    public static void hurtAndBreak(ItemStack stack, ServerPlayer player, InteractionHand hand, int amount) {
        if (stack == null || stack.isEmpty() || player == null || amount <= 0) {
            return;
        }
        if (isBroken(stack)) {
            destroyBroken(stack, player, hand);
            return;
        }
        if (!stack.getItem().canBeDepleted() && stack.getItem().getMaxDamage(stack) <= 0) {
            return;
        }
        stack.hurtAndBreak(amount, player, broken -> broken.broadcastBreakEvent(hand));
        ItemStack after = player.getItemInHand(hand);
        if (isBroken(after)) {
            destroyBroken(after, player, hand);
        }
    }

    private static void destroyBroken(ItemStack stack, ServerPlayer player, InteractionHand hand) {
        if (stack.isEmpty()) {
            return;
        }
        stack.setCount(0);
        player.broadcastBreakEvent(hand);
        player.setItemInHand(hand, ItemStack.EMPTY);
    }
}
