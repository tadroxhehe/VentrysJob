package com.ventrys.job.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * Bloc de minerai simple - utilisé pour créer des blocs de minerais moddés
 */
public class OreBlock extends Block {
    
    public OreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
    
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, 
                                        @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return Shapes.block();
    }
}
