package com.ventrys.job.menu;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.util.JobTableInteractionHelper;
import com.ventrys.job.init.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Menu/Container pour les tables de métier
 */
public class JobTableMenu extends AbstractContainerMenu {
    private final BlockPos tablePos;
    private final String jobId;

    public JobTableMenu(int containerId, Inventory playerInventory, BlockPos tablePos, String jobId) {
        super(ModMenuTypes.JOB_TABLE.get(), containerId);
        this.tablePos = tablePos;
        this.jobId = jobId;
        
        // Debug
        if (jobId == null || jobId.isEmpty()) {
            VentrysJob.LOGGER.warn("JobTableMenu créé avec jobId vide ou null!");
        } else {
            VentrysJob.LOGGER.debug("JobTableMenu créé avec jobId: {}", jobId);
        }
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.distanceToSqr(tablePos.getX() + 0.5, tablePos.getY() + 0.5, tablePos.getZ() + 0.5) > 64.0) {
            return false;
        }

        BlockState state = player.level.getBlockState(tablePos);
        return JobTableInteractionHelper.isJobTableBlock(state, jobId);
    }
}

