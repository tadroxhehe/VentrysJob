package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CuisinierTableBlockEntity extends JobTableBlockEntity {
    public CuisinierTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUISINIER_TABLE.get(), pos, state, "cuisinier");
    }
}

