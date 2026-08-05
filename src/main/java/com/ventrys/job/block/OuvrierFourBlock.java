package com.ventrys.job.block;

import com.ventrys.job.block.entity.OuvrierFourBlockEntity;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.util.FurnaceLighterHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc Four pour le métier Ouvrier - Transforme les bûches en charbon de bois
 */
public class OuvrierFourBlock extends BaseEntityBlock {
    
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public OuvrierFourBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LIT, false)
                .setValue(FACING, Direction.NORTH));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public @NotNull BlockState mirror(@NotNull BlockState state, @NotNull Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, 
                                 @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            // Empêcher les interactions en mode créatif
            if (player.isCreative()) {
                return InteractionResult.PASS;
            }
            
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                // Créer le BlockEntity s'il n'existe pas
                blockEntity = newBlockEntity(pos, state);
                if (blockEntity != null) {
                    level.setBlockEntity(blockEntity);
                }
            }
            
            if (blockEntity instanceof OuvrierFourBlockEntity fourEntity) {
                net.minecraft.world.item.ItemStack heldItem = player.getItemInHand(hand);
                
                // Vérifier si le joueur utilise un briquet pour allumer le four (vanilla ou moddé)
                if (FurnaceLighterHelper.isLighter(heldItem)) {
                    if (!fourEntity.isLit()) {
                        fourEntity.lightWithFlintAndSteel();
                        level.setBlock(pos, state.setValue(LIT, true), 3);
                        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("ventrysjob.message.furnace.lit"), player.getUUID());
                        // Consommer la durabilité du briquet
                        if (!player.isCreative()) {
                            heldItem.hurt(1, level.random, player instanceof ServerPlayer ? (ServerPlayer) player : null);
                        }
                        return InteractionResult.SUCCESS;
                    } else {
                        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("ventrysjob.message.furnace.already_lit"), player.getUUID());
                        return InteractionResult.SUCCESS;
                    }
                }
                
                // Vérifier si le joueur est ouvrier avant d'ouvrir le GUI
                if (!PlayerJobData.canAccessJobTable(player, "ouvrier")) {
                    player.sendMessage(new TranslatableComponent("ventrysjob.message.furnace.must_be_worker"), player.getUUID());
                    return InteractionResult.FAIL;
                }
                
                // Ouvrir le GUI (même si le four n'est pas allumé)
                if (player instanceof ServerPlayer serverPlayer) {
                    NetworkHooks.openGui(serverPlayer, fourEntity, pos);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new OuvrierFourBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof OuvrierFourBlockEntity fourEntity) {
                fourEntity.tick();
            }
        };
    }
}
