package com.ventrys.job.block.entity;

import com.ventrys.job.menu.JobTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Block Entity de base pour toutes les tables de métier
 */
public abstract class JobTableBlockEntity extends BlockEntity implements MenuProvider {
    private final String jobId;

    protected JobTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String jobId) {
        super(type, pos, state);
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public Component getDisplayName() {
        return titleForJob(jobId);
    }

    public static Component titleForJob(String jobId) {
        if ("forgeron".equals(jobId)) {
            return new TranslatableComponent("container.ventrysjob.forgeron_table");
        }
        if ("artisan".equals(jobId)) {
            return new TranslatableComponent("container.ventrysjob.artisan_table");
        }
        if ("apothicaire".equals(jobId)) {
            return new TranslatableComponent("container.ventrysjob.apothicaire_table");
        }
        if ("cuisinier".equals(jobId)) {
            return new TranslatableComponent("container.ventrysjob.cuisinier_table");
        }
        if ("couturier".equals(jobId)) {
            return new TranslatableComponent("container.ventrysjob.metier_tisser");
        }
        return new TranslatableComponent("container.ventrysjob.artisan_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new JobTableMenu(containerId, playerInventory, this.worldPosition, jobId);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("jobId", jobId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // Le jobId est défini dans le constructeur, pas besoin de le charger
    }
}

