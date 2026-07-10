package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ForgeronTableBlockEntity extends JobTableBlockEntity {
    public ForgeronTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FORGERON_TABLE.get(), pos, state, "forgeron");
    }
}

