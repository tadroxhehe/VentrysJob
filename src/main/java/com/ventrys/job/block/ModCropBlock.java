package com.ventrys.job.block;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.function.Supplier;

/**
 * Culture enregistrée par VentrysJob : 4 stades (0–3). Les graines (items VentrysItem) ne font que déclencher
 * le placement ; le bloc au sol est celui-ci, avec textures et croissance gérées par VentrysJob.
 */
public class ModCropBlock extends CropBlock {

    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private final Supplier<ItemLike> seedItem;

    public ModCropBlock(Supplier<ItemLike> seedItem, Properties properties) {
        super(properties);
        this.seedItem = seedItem;
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seedItem.get();
    }
}
