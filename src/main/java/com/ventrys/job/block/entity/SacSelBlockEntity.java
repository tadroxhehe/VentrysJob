package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity pour le Sac à Sel
 */
public class SacSelBlockEntity extends BlockEntity implements MenuProvider {
    
    private final ItemStackHandler itemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateBlockState();
        }
    };
    
    private void updateBlockState() {
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide) {
            BlockState state = getBlockState();
            boolean hasItems = !isEmpty();
            if (state.getValue(com.ventrys.job.block.SacSelBlock.HAS_ITEMS) != hasItems) {
                currentLevel.setBlock(worldPosition, state.setValue(com.ventrys.job.block.SacSelBlock.HAS_ITEMS, hasItems), 3);
            }
        }
    }
    
    private boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public SacSelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAC_SEL.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.ventrysjob.sac_sel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.ventrys.job.menu.SacSelMenu(containerId, playerInventory, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        if (level != null && !level.isClientSide) {
            updateBlockState();
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Tick du Sac à Sel - Simple stockage sans système de péremption
     */
    public void tick() {
        // Pas de logique spéciale nécessaire - juste un stockage simple
    }
}
