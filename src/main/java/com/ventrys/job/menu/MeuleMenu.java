package com.ventrys.job.menu;

import com.ventrys.job.init.ModBlocks;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class MeuleMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final ContainerData data;

    public MeuleMenu(int containerId, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MEULE.get(), containerId);
        Level level = blockEntity != null ? blockEntity.getLevel() : playerInventory.player.level;
        if (level == null) {
            level = playerInventory.player.level;
        }
        BlockPos pos = blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO;
        this.access = ContainerLevelAccess.create(level, pos);
        this.data = data;

        addDataSlots(data);

        if (blockEntity != null) {
            blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                // Slot input (gauche) - position ajustée pour être visible
                addSlot(new SlotItemHandler(handler, 0, 30, 40) {
                    @Override
                    public int getMaxStackSize(@NotNull ItemStack stack) {
                        return 16;
                    }
                });
                // Slot output (droite) - position ajustée pour être visible
                addSlot(new SlotItemHandler(handler, 1, 152, 40) {
                    @Override
                    public boolean mayPlace(@NotNull ItemStack stack) {
                        return false;
                    }
                });
            });
        }

        // Inventaire du joueur visible et interactif (position standard)
        addPlayerInventory(playerInventory, 8, 84);
    }

    public MeuleMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, playerInventory.player.level.getBlockEntity(pos), new ContainerData() {
            private final int[] values = new int[2];

            @Override
            public int get(int index) {
                return values[index];
            }

            @Override
            public void set(int index, int value) {
                values[index] = value;
            }

            @Override
            public int getCount() {
                return values.length;
            }
        });
    }

    private void addPlayerInventory(Inventory playerInventory, int leftCol, int topRow) {
        // Inventaire principal (3x9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, leftCol + col * 18, topRow + row * 18));
            }
        }
        // Hotbar (9 slots)
        for (int hotbarSlot = 0; hotbarSlot < 9; ++hotbarSlot) {
            addSlot(new Slot(playerInventory, hotbarSlot, leftCol + hotbarSlot * 18, topRow + 58));
        }
    }

    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();

            if (index < 2) {
                if (!moveItemStackTo(stackInSlot, 2, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
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

    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModBlocks.MEULE.get());
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }
}

