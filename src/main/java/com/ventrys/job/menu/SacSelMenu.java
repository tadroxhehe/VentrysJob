package com.ventrys.job.menu;

import com.ventrys.job.block.entity.SacSelBlockEntity;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Menu pour le Sac à Sel
 */
public class SacSelMenu extends AbstractContainerMenu {
    @SuppressWarnings("unused")
    private final SacSelBlockEntity blockEntity;
    private final ItemStackHandler itemHandler;

    public SacSelMenu(int containerId, Inventory playerInventory, SacSelBlockEntity blockEntity) {
        super(ModMenuTypes.SAC_SEL.get(), containerId);
        this.blockEntity = blockEntity;
        this.itemHandler = blockEntity != null ? blockEntity.getItemHandler() : new ItemStackHandler(9);

        // Slots du Sac à Sel (3x3)
        if (blockEntity != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    this.addSlot(new SlotItemHandler(itemHandler, row * 3 + col, 
                        62 + col * 18, 17 + row * 18));
                }
            }
        }

        // Inventaire du joueur
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                    8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar du joueur
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public SacSelMenu(int containerId, Inventory playerInventory, FriendlyByteBuf data) {
        this(containerId, playerInventory, 
            playerInventory.player.level.getBlockEntity(data.readBlockPos()) instanceof SacSelBlockEntity entity ? entity : null);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();
            
            if (index < 9) {
                // Depuis le Sac à Sel vers l'inventaire du joueur
                if (!this.moveItemStackTo(slotStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 45) {
                // Depuis l'inventaire du joueur vers le Sac à Sel
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Depuis la hotbar vers le Sac à Sel
                if (!this.moveItemStackTo(slotStack, 0, 9, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            
            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            
            slot.onTake(player, slotStack);
        }
        
        return itemstack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true; // Le Sac à Sel est accessible à tous
    }
}
