package com.ventrys.job.block;

import com.ventrys.job.block.entity.ForgeronTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Table de Forgeron
 */
public class ForgeronTableBlock extends JobTableBlock {
    public ForgeronTableBlock(Properties properties) {
        super(properties, "forgeron");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ForgeronTableBlockEntity(pos, state);
    }
}

