package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ApothicaireTableBlockEntity extends JobTableBlockEntity {
    public ApothicaireTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.APOTHICAIRE_TABLE.get(), pos, state, "apothicaire");
    }
}

