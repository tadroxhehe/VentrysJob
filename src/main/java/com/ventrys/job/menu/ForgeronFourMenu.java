package com.ventrys.job.menu;

import com.ventrys.job.block.entity.ForgeronFourBlockEntity;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Menu pour le Four de Forgeron
 * Interface : 2 slots à gauche (combustible + minerai), 1 slot à droite (résultat)
 */
public class ForgeronFourMenu extends AbstractContainerMenu {

    private final ForgeronFourBlockEntity blockEntity;
    @SuppressWarnings("unused")
    private final Level level;
    private final ContainerData data;

    public ForgeronFourMenu(int id, Inventory inv, ForgeronFourBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FORGERON_FOUR.get(), id);
        checkContainerSize(inv, 3);
        this.blockEntity = entity;
        this.level = inv.player.level;
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        if (this.blockEntity != null) {
            this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
            // Slot combustible
            this.addSlot(new SlotItemHandler(handler, 0, 56, 53) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (stack.isEmpty()) return false;
                    var registryName = stack.getItem().getRegistryName();
                    if (registryName == null) return false;
                    
                    String itemId = registryName.toString();
                    return ForgeronFourBlockEntity.isValidFuel(itemId);
                }
            });

            // Slot minerai brut
            this.addSlot(new SlotItemHandler(handler, 1, 56, 17) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    if (stack.isEmpty()) return false;
                    var registryName = stack.getItem().getRegistryName();
                    if (registryName == null) return false;
                    
                    String itemId = registryName.toString();
                    return ForgeronFourBlockEntity.isValidOre(itemId);
                }
            });

            // Slot résultat (à droite)
            this.addSlot(new SlotItemHandler(handler, 2, 116, 35) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false; // Slot de sortie uniquement
                }
            });
            });
        }

        addDataSlots(data);
    }

    public ForgeronFourMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, 
            inv.player.level.getBlockEntity(extraData.readBlockPos()) instanceof ForgeronFourBlockEntity entity ? entity : null, 
            new SimpleContainerData(5));
    }

    public boolean isLit() {
        return data.get(2) == 1;
    }

    public boolean isCooking() {
        return data.get(0) > 0;
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        int progressArrowSize = 24; // Largeur de la flèche de progression

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }
    
    public int getScaledFuelTime() {
        int fuelTime = this.data.get(3);
        int maxFuelTime = this.data.get(4);
        int fuelArrowSize = 14; // Hauteur de la flèche de combustible (comme vanilla)

        if (maxFuelTime == 0) return 0;
        return fuelTime * fuelArrowSize / maxFuelTime;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Slots 0-2 sont les slots du four (combustible, minerai, résultat)
        if (index < 3) {
            if (!this.moveItemStackTo(sourceStack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else if (index < 30) { // Inventaire du joueur
            if (!this.moveItemStackTo(sourceStack, 0, 3, false)) {
                if (!this.moveItemStackTo(sourceStack, 30, 39, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index < 39 && !this.moveItemStackTo(sourceStack, 0, 3, false)) { // Barre d'action du joueur
            if (!this.moveItemStackTo(sourceStack, 3, 30, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (blockEntity == null || blockEntity.isRemoved() || blockEntity.getLevel() == null) {
            return false;
        }
        if (blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) != blockEntity) {
            return false;
        }
        var pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
