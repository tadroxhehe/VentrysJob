package com.ventrys.job.block;

import com.ventrys.job.block.entity.VaseApothicaireBlockEntity;
import com.ventrys.job.data.PlayerJobData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaseApothicaireBlock extends Block implements EntityBlock {
    
    /** 0 = modèle base ({@code vase_plantation.png}), 1 = stade final ({@code vase_plantation_texture.png}). */
    public static final IntegerProperty GROWTH_STAGE = IntegerProperty.create("growth_stage", 0, 1);
    
    // Collision box correspondant à la base du vase (75 % du modèle, pivot centre du bloc)
    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0.25390625, 0.0, 0.25390625, 0.74609375, 0.796875, 0.74609375);
    
    public VaseApothicaireBlock() {
        super(BlockBehaviour.Properties.of(Material.STONE)
                .strength(2.0f)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(GROWTH_STAGE, 0));
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GROWTH_STAGE);
    }
    
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // Retourne un bloc complet pour l'hitbox d'interaction (visible avec F3+B)
        // Le modèle JSON contrôle l'apparence visuelle réelle
        return Shapes.block();
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // Utilise la forme réduite pour la collision physique (les entités peuvent passer à côté)
        return COLLISION_SHAPE;
    }
    
    @Override
    public @NotNull VoxelShape getOcclusionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        // Bloc complet pour l'occlusion de lumière
        return Shapes.block();
    }
    
    @Override
    public @NotNull VoxelShape getVisualShape(@NotNull BlockState state, @NotNull BlockGetter reader, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        // Bloc complet pour éviter de voir à travers
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        // Empêcher les interactions en mode créatif (comme pour les tables de métier)
        if (player.isCreative()) {
            return InteractionResult.PASS;
        }

        // Vérifier si le joueur a le métier apothicaire
        if (!PlayerJobData.canAccessJobTable(player, "apothicaire")) {
            player.sendMessage(new TranslatableComponent("ventrysjob.message.vase.wrong_job"), player.getUUID());
            return InteractionResult.FAIL;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Créer le BlockEntity s'il n'existe pas
        if (blockEntity == null) {
            blockEntity = newBlockEntity(pos, state);
            if (blockEntity != null) {
                level.setBlockEntity(blockEntity);
            }
        }
        
        if (blockEntity instanceof VaseApothicaireBlockEntity vaseEntity) {
            // Récupérer l'item de la main utilisée
            ItemStack heldItem = player.getItemInHand(hand);
            
            // Si la main utilisée est vide, vérifier l'autre main
            if (heldItem.isEmpty()) {
                InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND) 
                    ? InteractionHand.OFF_HAND 
                    : InteractionHand.MAIN_HAND;
                ItemStack otherHandItem = player.getItemInHand(otherHand);
                
                // Si l'autre main a un item, l'utiliser
                if (!otherHandItem.isEmpty()) {
                    heldItem = otherHandItem;
                }
            }
            
            // Vérifier si c'est un seau d'eau
            if (heldItem.getItem() == Items.WATER_BUCKET) {
                return vaseEntity.handleWatering(player, heldItem);
            }
            
            // Sinon, traiter comme une tentative de plantation/récolte
            return vaseEntity.handlePlantingOrHarvest(player, heldItem);
        }

        return InteractionResult.PASS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaseApothicaireBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof VaseApothicaireBlockEntity vaseEntity) {
                VaseApothicaireBlockEntity.tick(level1, pos, state1, vaseEntity);
            }
        };
    }
}
