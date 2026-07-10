package com.ventrys.job.block;

import com.ventrys.job.block.entity.ForgeronFourBlockEntity;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.util.FurnaceLighterHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Four de Forgeron - Permet de cuire les minerais bruts en minerais fondus
 */
public class ForgeronFourBlock extends Block implements EntityBlock {
    
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public ForgeronFourBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE)
                .strength(3.5f)
                .requiresCorrectToolForDrops());
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ForgeronFourBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof ForgeronFourBlockEntity fourEntity) {
                fourEntity.tick();
            }
        };
    }

    @Override
    public @NotNull InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
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
            
            if (blockEntity instanceof ForgeronFourBlockEntity fourEntity) {
                ItemStack heldItem = player.getItemInHand(hand);
                
                // Vérifier si le joueur utilise un briquet (vanilla ou moddé)
                if (FurnaceLighterHelper.isLighter(heldItem)) {
                    if (!fourEntity.isLit()) {
                        fourEntity.lightWithFlintAndSteel();
                        level.setBlock(pos, state.setValue(LIT, true), 3);
                        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("ventrysjob.message.furnace.forgeron.lit"), player.getUUID());
                        
                        // Son d'allumage
                        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.5f, 1.0f);
                        
                        // Consommer la durabilité du briquet
                        if (!player.isCreative()) {
                            heldItem.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
                        }
                    } else {
                        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("ventrysjob.message.furnace.forgeron.already_lit"), player.getUUID());
                    }
                    return InteractionResult.SUCCESS;
                }
                
                // Ouvrir l'interface si le joueur est forgeron (peu importe si le four est allumé ou non)
                if (player instanceof ServerPlayer serverPlayer) {
                    // Verifier si le joueur a le metier forgeron
                    boolean canAccess = PlayerJobData.canAccessJobTable(player, "forgeron");
                    if (!canAccess) {
                        player.sendMessage(new net.minecraft.network.chat.TranslatableComponent("ventrysjob.message.furnace.forgeron.must_be_blacksmith"), player.getUUID());
                        return InteractionResult.FAIL;
                    }
                    
                    NetworkHooks.openGui(serverPlayer, fourEntity, pos);
                    return InteractionResult.SUCCESS;
                } else {
                    com.ventrys.job.VentrysJob.LOGGER.warn("Four de forge: joueur n'est pas ServerPlayer: {}", player.getClass().getName());
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
