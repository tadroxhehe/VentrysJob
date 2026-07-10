package com.ventrys.job.extraction;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.registries.ForgeRegistries;

public final class ExtractionDropFactory {
    private ExtractionDropFactory() {
    }

    public static ItemStack createDropItemPublic(String itemId, int count) {
        return createDropItem(itemId, count, null);
    }

    public static ItemStack createDropItemPublic(String itemId, int count, String displayName) {
        return createDropItem(itemId, count, displayName);
    }

    static ItemStack createDropItem(String itemId, int count, String displayName) {
        try {
            ResourceLocation resourceLocation = new ResourceLocation(itemId);
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            if (item != null) {
                ItemStack stack = findCreativeStack(item);
                if (!stack.isEmpty()) {
                    stack.setCount(count);
                    applyDisplayName(stack, displayName);
                    return stack;
                }
                stack = item.getDefaultInstance();
                if (!stack.isEmpty()) {
                    stack.setCount(count);
                    applyDisplayName(stack, displayName);
                    return stack;
                }
                stack = new ItemStack(item, count);
                applyDisplayName(stack, displayName);
                return stack;
            }
        } catch (Exception e) {
            // L'item n'existe pas ou erreur de parsing
        }
        return ItemStack.EMPTY;
    }

    static void spawnDrop(Level level, BlockPos pos, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemEntity entity = new ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            stack
        );
        level.addFreshEntity(entity);
    }

    public static boolean giveCropDrops(ServerPlayer player, Level level, BlockPos pos,
                                     com.ventrys.job.data.ForkConfig.CropConfig cropConfig) {
        ItemStack dropItem = createDropItem(cropConfig.dropItem(), cropConfig.dropCount(), null);
        if (dropItem.isEmpty()) {
            VentrysJob.LOGGER.warn("Item de récolte invalide: {}", cropConfig.dropItem());
            return false;
        }

        spawnDrop(level, pos, dropItem);

        if (cropConfig.seedItem() != null && !cropConfig.seedItem().isEmpty()) {
            int seedCount = level.random.nextBoolean() ? 1 : 2;
            ItemStack seedItem = createDropItem(cropConfig.seedItem(), seedCount, null);
            if (!seedItem.isEmpty()) {
                spawnDrop(level, pos, seedItem);
            } else {
                VentrysJob.LOGGER.warn("Item de graine invalide: {}", cropConfig.seedItem());
            }
        }

        player.sendMessage(new TranslatableComponent("ventrysjob.message.crop.harvest.success"), player.getUUID());
        return true;
    }

    private static void applyDisplayName(ItemStack stack, String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            stack.setHoverName(new TextComponent(displayName));
        }
    }

    private static ItemStack findCreativeStack(Item item) {
        try {
            for (CreativeModeTab tab : CreativeModeTab.TABS) {
                if (tab == null) {
                    continue;
                }
                NonNullList<ItemStack> stacks = NonNullList.create();
                item.fillItemCategory(tab, stacks);
                for (ItemStack stack : stacks) {
                    if (!stack.isEmpty() && stack.getItem() == item) {
                        return stack.copy();
                    }
                }
            }
        } catch (Exception e) {
            // Ignorer les erreurs liées aux onglets créatifs côté serveur
        }
        return ItemStack.EMPTY;
    }
}
