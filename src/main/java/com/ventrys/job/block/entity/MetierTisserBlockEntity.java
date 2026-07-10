package com.ventrys.job.block.entity;

import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity pour le Métier à tisser du métier Couturier
 * Utilise le même système que les autres tables de métier
 */
public class MetierTisserBlockEntity extends JobTableBlockEntity {
    
    public MetierTisserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METIER_TISSER.get(), pos, state, "couturier");
    }
}
