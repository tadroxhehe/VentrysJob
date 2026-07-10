package com.ventrys.job.block;

import com.ventrys.job.block.entity.CuisinierTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Table de Cuisinier
 */
public class CuisinierTableBlock extends JobTableBlock {
    public CuisinierTableBlock(Properties properties) {
        super(properties, "cuisinier");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CuisinierTableBlockEntity(pos, state);
    }
}

