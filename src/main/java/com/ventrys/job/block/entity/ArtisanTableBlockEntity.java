package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ArtisanTableBlockEntity extends JobTableBlockEntity {
    public ArtisanTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARTISAN_TABLE.get(), pos, state, "artisan");
    }
}

