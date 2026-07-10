package com.ventrys.job.menu;

import com.ventrys.job.block.entity.ChickenNestBlockEntity;
import com.ventrys.job.init.ModBlocks;
import com.ventrys.job.util.VentrysItemRefs;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChickenNestMenu extends AbstractContainerMenu {
    
    private final ContainerLevelAccess access;
    @SuppressWarnings("unused")
    private final ContainerData data;
    
    public ChickenNestMenu(int containerId, Inventory playerInventory, ChickenNestBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CHICKEN_NEST.get(), containerId);
        this.access = blockEntity != null ? ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()) : ContainerLevelAccess.NULL;
        this.data = data;
        
        addDataSlots(data);
        
        if (blockEntity != null) {
            // Slots pour les œufs (4 slots centrés)
            // Les positions doivent correspondre aux textures dans l'écran
            // L'écran dessine les textures à leftPos + i * slotSize, topPos
            // Donc les slots doivent être à i * slotSize, 0 (relatif à leftPos/topPos)
            int slotSize = 18;
            
            for (int i = 0; i < 4; ++i) {
                int slotX = i * slotSize; // Position relative (sera offset par leftPos dans l'écran)
                int slotY = 0; // Position relative (sera offset par topPos dans l'écran)
                addSlot(new Slot(blockEntity, i, slotX, slotY) {
                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        if (VentrysItemRefs.isResOeuf(stack)) {
                            return 1;
                        }
                        return super.getMaxStackSize(stack);
                    }
                    
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return VentrysItemRefs.isResOeuf(stack) && stack.getCount() <= 1;
                    }
                });
            }
        }
        
        // Inventaire du joueur (descendu pour correspondre à imageHeight=236)
        // Dans un GUI standard (166), inventaire est à y=84, hotbar à y=142
        // Avec imageHeight=236, on ajoute 70 pixels : inventaire à y=154, hotbar à y=212
        int inventoryOffsetY = 70; // Décalage pour descendre l'inventaire
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + inventoryOffsetY));
            }
        }
        
        // Hotbar (descendue)
        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            addSlot(new Slot(playerInventory, hotbarSlot, 8 + hotbarSlot * 18, 142 + inventoryOffsetY));
        }
    }
    
    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            
            if (index < 4) {
                if (!moveItemStackTo(stackInSlot, 4, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 4, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return stack;
    }
    
    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.CHICKEN_NEST.get());
    }
}

