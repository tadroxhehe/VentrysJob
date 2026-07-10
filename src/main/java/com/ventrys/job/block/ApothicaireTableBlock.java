package com.ventrys.job.block;

import com.ventrys.job.block.entity.ApothicaireTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Table d'Apothicaire
 */
public class ApothicaireTableBlock extends JobTableBlock {
    public ApothicaireTableBlock(Properties properties) {
        super(properties, "apothicaire");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ApothicaireTableBlockEntity(pos, state);
    }
}

