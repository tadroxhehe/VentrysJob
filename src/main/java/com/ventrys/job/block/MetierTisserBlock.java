package com.ventrys.job.block;

import com.ventrys.job.block.entity.MetierTisserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc Métier à tisser pour le métier Couturier.
 * Étend {@link JobTableBlock} pour que {@link com.ventrys.job.menu.JobTableMenu#stillValid}
 * reconnaisse le bloc et pour envoyer le jobId sur le réseau comme les autres tables.
 */
public class MetierTisserBlock extends JobTableBlock {

    public MetierTisserBlock(Properties properties) {
        super(properties, "couturier");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MetierTisserBlockEntity(pos, state);
    }
}
