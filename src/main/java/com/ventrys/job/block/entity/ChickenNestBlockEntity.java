package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import com.ventrys.job.menu.ChickenNestMenu;
import com.ventrys.job.util.ChickenNestIndex;
import com.ventrys.job.util.VentrysItemRefs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChickenNestBlockEntity extends BlockEntity implements MenuProvider, Container {
    
    private static final int MAX_EGGS = 4;
    private final NonNullList<ItemStack> eggs = NonNullList.withSize(MAX_EGGS, ItemStack.EMPTY);
    
    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return eggs.get(index).getCount();
        }
        
        @Override
        public void set(int index, int value) {
            eggs.get(index).setCount(value);
        }
        
        @Override
        public int getCount() {
            return MAX_EGGS;
        }
    };
    
    public ChickenNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHICKEN_NEST.get(), pos, state);
    }
    
    public boolean canAddEgg() {
        for (ItemStack egg : eggs) {
            if (egg.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public void addEgg(ItemStack eggStack) {
        if (eggStack.isEmpty() || !VentrysItemRefs.isResOeuf(eggStack)) {
            return;
        }
        for (int i = 0; i < eggs.size(); i++) {
            ItemStack existing = eggs.get(i);
            if (existing.isEmpty()) {
                // Limiter à 1 œuf par slot
                ItemStack singleEgg = eggStack.copy();
                singleEgg.setCount(1);
                eggs.set(i, singleEgg);
                updateBlockState();
                setChanged();
                return;
            }
        }
    }
    
    public ItemStack removeEgg() {
        for (int i = eggs.size() - 1; i >= 0; i--) {
            if (!eggs.get(i).isEmpty()) {
                ItemStack egg = eggs.get(i).copy();
                eggs.set(i, ItemStack.EMPTY);
                updateBlockState();
                setChanged();
                return egg;
            }
        }
        return ItemStack.EMPTY;
    }
    
    private void updateBlockState() {
        Level currentLevel = this.level;
        if (currentLevel != null && !currentLevel.isClientSide) {
            BlockState state = getBlockState();
            boolean hasEgg = !isEmpty();
            if (state.getValue(com.ventrys.job.block.ChickenNestBlock.HAS_EGG) != hasEgg) {
                currentLevel.setBlock(worldPosition, state.setValue(com.ventrys.job.block.ChickenNestBlock.HAS_EGG, hasEgg), 3);
            }
        }
    }
    
    public NonNullList<ItemStack> getEggs() {
        return eggs;
    }
    
    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.ventrysjob.chicken_nest");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChickenNestMenu(containerId, inventory, this, data);
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, eggs);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, eggs);
        for (int i = 0; i < eggs.size(); i++) {
            ItemStack s = eggs.get(i);
            if (!s.isEmpty() && !VentrysItemRefs.isResOeuf(s)) {
                eggs.set(i, ItemStack.EMPTY);
            }
        }
        if (level != null && !level.isClientSide) {
            updateBlockState();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel && !level.isClientSide) {
            ChickenNestIndex.register(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel && !level.isClientSide) {
            ChickenNestIndex.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }
    
    public static void tick(Level level, BlockPos pos, BlockState state, ChickenNestBlockEntity be) {
        // Logique de tick si nécessaire
    }
    
    // Container implementation
    @Override
    public int getContainerSize() {
        return eggs.size();
    }
    
    @Override
    public boolean isEmpty() {
        return eggs.stream().allMatch(ItemStack::isEmpty);
    }
    
    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < eggs.size() ? eggs.get(index) : ItemStack.EMPTY;
    }
    
    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack stack = ContainerHelper.removeItem(eggs, index, count);
        if (!stack.isEmpty()) {
            updateBlockState();
            setChanged();
        }
        return stack;
    }
    
    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ContainerHelper.takeItem(eggs, index);
    }
    
    @Override
    public void setItem(int index, ItemStack stack) {
        if (!stack.isEmpty() && !VentrysItemRefs.isResOeuf(stack)) {
            return;
        }
        ItemStack toStore = stack;
        if (!stack.isEmpty() && stack.getCount() > 1) {
            toStore = stack.copy();
            toStore.setCount(1);
        }
        eggs.set(index, toStore);
        updateBlockState();
        setChanged();
    }
    
    @Override
    public int getMaxStackSize() {
        // Limiter à 1 pour les œufs
        return 1;
    }
    
    @Override
    public boolean stillValid(Player player) {
        Level currentLevel = this.level;
        if (currentLevel == null || currentLevel.getBlockEntity(worldPosition) != this) {
            return false;
        }
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }
    
    @Override
    public void clearContent() {
        eggs.clear();
        updateBlockState();
        setChanged();
    }
}

