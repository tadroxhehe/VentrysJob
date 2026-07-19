package com.ventrys.job.menu;

import com.ventrys.job.block.entity.OuvrierFourBlockEntity;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Menu pour le Four Ouvrier - Interface simple avec un slot central
 */
public class OuvrierFourMenu extends AbstractContainerMenu {
    private final OuvrierFourBlockEntity blockEntity;
    @SuppressWarnings("unused")
    private final Level level;
    private final ContainerData data;

    public OuvrierFourMenu(int containerId, Inventory inv, OuvrierFourBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.OUVRIER_FOUR.get(), containerId);
        checkContainerSize(inv, 1);
        this.blockEntity = entity;
        this.level = inv.player.level;
        this.data = data;

        // Ajouter le slot unique du four (entrée/sortie)
        if (this.blockEntity != null) {
            this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler ->
                // Limite 1 / filtre via ItemStackHandler (évite duplication avec shift-clic si logique manual en menu)
                this.addSlot(new SlotItemHandler(handler, 0, 80, 35))
            );
        }

        // Ajouter l'inventaire du joueur (positions vanilla)
        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Ajouter les données de synchronisation
        addDataSlots(data);
    }

    // Méthodes utilitaires pour vérifier l'état du four
    public boolean isTransforming() {
        return data.get(0) > 0;
    }

    public boolean isLit() {
        return data.get(1) > 0;
    }

    public int getProgressionScaled(int scale) {
        int progress = data.get(0);
        int maxProgress = data.get(2);
        if (maxProgress == 0) return 0;
        return progress * scale / maxProgress;
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

    @Override
    public ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack before = sourceStack.copy();

        if (index == 0) {
            if (!moveItemStackTo(sourceStack, 1, 37, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return before;
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

    public OuvrierFourBlockEntity getBlockEntity() {
        return this.blockEntity;
    }
}
