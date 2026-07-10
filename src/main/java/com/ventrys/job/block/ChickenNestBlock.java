package com.ventrys.job.block;

import com.ventrys.job.block.entity.ChickenNestBlockEntity;
import com.ventrys.job.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChickenNestBlock extends BaseEntityBlock {
    
    public static final BooleanProperty HAS_EGG = BooleanProperty.create("has_egg");
    
    // Collision box correspondant à la base du nid (de 1,0.1,1 à 15,0.35,15 en coordonnées de bloc)
    private static final VoxelShape SHAPE = Shapes.box(0.0625, 0.00625, 0.0625, 0.9375, 0.021875, 0.9375);
    
    public ChickenNestBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_EGG, false));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(HAS_EGG);
    }
    
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
    
    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChickenNestBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CHICKEN_NEST.get(), ChickenNestBlockEntity::tick);
    }
    
    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            // Empêcher les interactions en mode créatif
            if (player.isCreative()) {
                return InteractionResult.PASS;
            }
            
            // Vérifier si le joueur est paysan
            if (!com.ventrys.job.data.PlayerJobData.canAccessJobTable(player, "paysan")) {
                return InteractionResult.FAIL;
            }
            
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ChickenNestBlockEntity nest) {
                    NetworkHooks.openGui(serverPlayer, nest, pos);
                    return InteractionResult.SUCCESS;
                } else {
                    return InteractionResult.FAIL;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

