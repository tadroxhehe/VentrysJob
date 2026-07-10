package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.CropGrowthManager;
import com.ventrys.job.data.PlayerJobData;
import com.ventrys.job.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Les graines VentrysItem sont des items « simples » (pas de {@code BlockItem}) : elles ne peuvent pas
 * se planter toutes seules. Ce handler reproduit le clic droit sur terre labourée comme le vanilla.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class VentrysSeedPlantingHandler {

    private static final Map<ResourceLocation, Supplier<Block>> SEED_ITEM_TO_CROP = new HashMap<>();

    static {
        putSeed("ventrysitem:item_graine_orge", ModBlocks.CROP_ORGE);
        putSeed("ventrysitem:item_graine_tomates", ModBlocks.CROP_TOMATES);
        putSeed("ventrysitem:item_graine_doignon", ModBlocks.CROP_OIGNON);
        putSeed("ventrysitem:item_graine_salade", ModBlocks.CROP_SALADE);
        putSeed("ventrysitem:item_graine_raisin", ModBlocks.CROP_RAISIN);
        putSeed("ventrysitem:item_graine_choux", ModBlocks.CROP_CHOUX);
        putSeed("ventrysitem:item_graine_carotte", ModBlocks.CROP_CAROTTE);
        putSeed("ventrysitem:item_graine_betrave", ModBlocks.CROP_BETRAVE);
    }

    private static void putSeed(String itemId, Supplier<Block> crop) {
        SEED_ITEM_TO_CROP.put(new ResourceLocation(itemId), crop);
    }

    private VentrysSeedPlantingHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickFarmlandWithSeed(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide() || !(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation itemId = stack.getItem().getRegistryName();
        if (itemId == null) {
            return;
        }
        Supplier<Block> cropSupplier = SEED_ITEM_TO_CROP.get(itemId);
        if (cropSupplier == null) {
            return;
        }
        Block cropBlock = cropSupplier.get();
        if (!(cropBlock instanceof CropBlock crop)) {
            return;
        }

        if (!player.isCreative()) {
            String job = PlayerJobData.getPlayerJob(player);
            if (!"paysan".equals(job)) {
                return;
            }
        }

        BlockPos farmlandPos = event.getPos();
        BlockState ground = level.getBlockState(farmlandPos);
        if (!(ground.getBlock() instanceof FarmBlock)) {
            return;
        }

        BlockPos cropPos = farmlandPos.above();
        BlockState above = level.getBlockState(cropPos);
        if (!canPlaceCrop(level, cropPos, above, crop)) {
            return;
        }

        BlockState planted = crop.defaultBlockState().setValue(crop.getAgeProperty(), 0);
        if (!planted.canSurvive(level, cropPos)) {
            return;
        }

        level.setBlock(cropPos, planted, Block.UPDATE_ALL_IMMEDIATE);
        CropGrowthManager.registerCrop(level, cropPos, level.getBlockState(cropPos));

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        player.swing(hand);
        level.playSound(null, cropPos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    private static boolean canPlaceCrop(ServerLevel level, BlockPos cropPos, BlockState above, CropBlock crop) {
        if (above.isAir()) {
            return true;
        }
        return above.getMaterial().isReplaceable() && crop.canSurvive(crop.defaultBlockState().setValue(crop.getAgeProperty(), 0), level, cropPos);
    }
}
