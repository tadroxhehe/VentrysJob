package com.ventrys.job.block.entity;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.MeuleConfig;
import com.ventrys.job.data.MeuleConfig.MeuleRecipe;
import com.ventrys.job.init.ModBlockEntities;
import com.ventrys.job.menu.MeuleMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class MeuleBlockEntity extends BlockEntity implements Container, MenuProvider {

    private static final int SLOT_INPUT = 0;
    private static final int SLOT_OUTPUT = 1;

    private final MeuleItemHandler itemHandler = new MeuleItemHandler(this);

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private long processStart = 0L;
    private boolean grinding = false;
    private long cachedDuration = MeuleConfig.getProcessDurationMs();

    // La meule est minutee a l'horloge murale (process ~20 s) : inutile d'evaluer recette/sortie
    // a chaque tick. On traite tous les TICK_INTERVAL ticks (0,25 s), imperceptible pour le joueur,
    // et on evite les allocations par tick (ResourceLocation/ItemStack dans createResultStack).
    private static final int TICK_INTERVAL = 5;
    private int tickThrottle = 0;

    public final MeuleContainerData data = new MeuleContainerData(this);

    public MeuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MEULE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.ventrysjob.meule");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new MeuleMenu(containerId, inventory, this, data);
    }

    public void tick() {
        var currentLevel = this.level;
        if (currentLevel == null || currentLevel.isClientSide()) {
            return;
        }

        if (++tickThrottle < TICK_INTERVAL) {
            return;
        }
        tickThrottle = 0;

        cachedDuration = MeuleConfig.getProcessDurationMs();

        Optional<MeuleRecipe> recipeOpt = getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            stopGrinding();
            return;
        }

        MeuleRecipe recipe = recipeOpt.get();
        if (!canOutput(recipe)) {
            stopGrinding();
            return;
        }

        if (!grinding) {
            startGrinding();
        }

        if (grinding && isComplete()) {
            finishRecipe(recipe);
            stopGrinding();
        }
    }

    private void startGrinding() {
        grinding = true;
        processStart = System.currentTimeMillis();
        setChanged();
    }

    private void stopGrinding() {
        if (grinding) {
            grinding = false;
            processStart = 0L;
            setChanged();
        }
    }

    private boolean isComplete() {
        if (!grinding || processStart == 0L) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - processStart;
        return elapsed >= cachedDuration;
    }

    private int getProgress() {
        if (!grinding || processStart == 0L) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - processStart;
        if (elapsed >= cachedDuration) {
            return 100;
        }
        return (int) ((elapsed * 100L) / cachedDuration);
    }

    private Optional<MeuleRecipe> getCurrentRecipe() {
        ItemStack input = itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        Optional<MeuleRecipe> recipe = MeuleConfig.getRecipeFor(input.getItem());
        if (recipe.isEmpty()) {
            return Optional.empty();
        }
        if (input.getCount() < recipe.get().inputCount()) {
            return Optional.empty();
        }
        return recipe;
    }

    private boolean canOutput(MeuleRecipe recipe) {
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack resultStack = createResultStack(recipe);
        if (resultStack.isEmpty()) {
            return false;
        }

        if (outputStack.isEmpty()) {
            return true;
        }

        if (!ItemStack.isSameItemSameTags(outputStack, resultStack)) {
            return false;
        }

        int total = outputStack.getCount() + resultStack.getCount();
        return total <= outputStack.getMaxStackSize();
    }

    private ItemStack createResultStack(MeuleRecipe recipe) {
        ItemStack result;
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(recipe.outputId()));
        if (item != null) {
            result = new ItemStack(item, recipe.outputCount());
        } else {
            VentrysJob.LOGGER.warn("Item de sortie introuvable pour la meule: {}", recipe.outputId());
            result = ItemStack.EMPTY;
        }
        return result;
    }

    private void finishRecipe(MeuleRecipe recipe) {
        ItemStack inputStack = itemHandler.getStackInSlot(SLOT_INPUT);
        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT);
        ItemStack resultStack = createResultStack(recipe);
        if (resultStack.isEmpty()) {
            return;
        }

        inputStack.shrink(recipe.inputCount());
        if (outputStack.isEmpty()) {
            itemHandler.setStackInSlot(SLOT_OUTPUT, resultStack.copy());
        } else {
            outputStack.grow(resultStack.getCount());
        }

        setChanged();
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        processStart = tag.getLong("processStart");
        grinding = tag.getBoolean("grinding");
        
        // Protection contre les timestamps invalides après crash/redémarrage
        long currentTime = System.currentTimeMillis();
        if (processStart > 0) {
            // Si le timestamp est dans le futur ou trop ancien (plus de 10x la durée de traitement), réinitialiser
            if (processStart > currentTime || processStart < currentTime - (cachedDuration * 10)) {
                processStart = 0;
                grinding = false;
            }
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putLong("processStart", processStart);
        tag.putBoolean("grinding", grinding);
        super.saveAdditional(tag);
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

    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            container.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (level != null) {
            Containers.dropContents(level, worldPosition, container);
        }
    }

    // Container impl
    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public @NotNull ItemStack getItem(int index) {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count) {
        ItemStack stack = itemHandler.extractItem(index, count, false);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        ItemStack stack = itemHandler.getStackInSlot(index);
        itemHandler.setStackInSlot(index, ItemStack.EMPTY);
        if (!stack.isEmpty()) {
            setChanged();
        }
        return stack;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        itemHandler.setStackInSlot(index, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (level == null) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    private static final class MeuleItemHandler extends ItemStackHandler {
        private final MeuleBlockEntity owner;

        MeuleItemHandler(MeuleBlockEntity owner) {
            super(2);
            this.owner = owner;
        }

        @Override
        protected void onContentsChanged(int slot) {
            owner.setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_OUTPUT) {
                return false;
            }
            return MeuleConfig.getRecipeFor(stack.getItem()).isPresent();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == SLOT_INPUT) {
                return 16;
            }
            return super.getSlotLimit(slot);
        }
    }

    public static final class MeuleContainerData implements ContainerData {
        private final MeuleBlockEntity owner;

        MeuleContainerData(MeuleBlockEntity owner) {
            this.owner = owner;
        }

        @Override
        public int get(int index) {
            if (index == 0) {
                return owner.getProgress();
            }
            if (index == 1) {
                return 100;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            // Pas de set nécessaire
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}

