package com.ventrys.job.block;

import com.ventrys.job.block.entity.ArtisanTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table d'Artisan - prend 2 blocs de largeur
 */
public class ArtisanTableBlock extends JobTableBlock {
    
    // Hitbox qui s'étend sur 2 blocs de largeur (1 bloc actuel + 1 bloc à droite)
    // Le bloc actuel : de 0,0,0 à 1,1,1
    // Le bloc à droite : de 1,0,0 à 2,1,1
    private static final VoxelShape SHAPE = Shapes.or(
        Shapes.block(), // Bloc actuel (0,0,0 à 1,1,1)
        Shapes.box(1.0, 0.0, 0.0, 2.0, 1.0, 1.0) // Bloc à droite (1,0,0 à 2,1,1)
    );
    
    public ArtisanTableBlock(Properties properties) {
        super(properties, "artisan");
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArtisanTableBlockEntity(pos, state);
    }
    
    /**
     * Surcharge pour étendre la hitbox sur 2 blocs de largeur
     */
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
    
    /**
     * Surcharge pour étendre la hitbox de collision sur 2 blocs de largeur
     */
    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
}

