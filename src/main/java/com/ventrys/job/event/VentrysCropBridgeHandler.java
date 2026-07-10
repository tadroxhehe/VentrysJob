package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.CropGrowthManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

/**
 * Remplace les blocs de culture encore enregistrés sous l’ancien id VentrysItem par les blocs
 * {@code ventrysjob:crop_*} (textures + croissance gérées par VentrysJob). Les items graine restent
 * des items VentrysItem ; seul le bloc au sol appartient à VentrysJob après pose.
 */
@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class VentrysCropBridgeHandler {

    private static final Map<ResourceLocation, ResourceLocation> LEGACY_VENTRYSITEM_BLOCK_TO_CROP = Map.ofEntries(
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_orge"), new ResourceLocation(VentrysJob.MOD_ID, "crop_orge")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_tomates"), new ResourceLocation(VentrysJob.MOD_ID, "crop_tomates")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_doignon"), new ResourceLocation(VentrysJob.MOD_ID, "crop_oignon")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_salade"), new ResourceLocation(VentrysJob.MOD_ID, "crop_salade")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_raisin"), new ResourceLocation(VentrysJob.MOD_ID, "crop_raisin")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_choux"), new ResourceLocation(VentrysJob.MOD_ID, "crop_choux")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_carotte"), new ResourceLocation(VentrysJob.MOD_ID, "crop_carotte")),
        Map.entry(new ResourceLocation("ventrysitem", "item_graine_betrave"), new ResourceLocation(VentrysJob.MOD_ID, "crop_betrave"))
    );

    private VentrysCropBridgeHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onCropPlacedFromVentrysItemSeed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level) || event.getPlacedBlock().isAir()) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        Block block = placed.getBlock();
        ResourceLocation id = block.getRegistryName();
        if (id == null) {
            return;
        }
        ResourceLocation replacementId = LEGACY_VENTRYSITEM_BLOCK_TO_CROP.get(id);
        if (replacementId == null) {
            return;
        }
        Block replacement = ForgeRegistries.BLOCKS.getValue(replacementId);
        if (!(replacement instanceof CropBlock crop)) {
            return;
        }
        BlockPos pos = event.getPos();
        int age = 0;
        try {
            if (placed.hasProperty(crop.getAgeProperty())) {
                age = placed.getValue(crop.getAgeProperty());
            }
        } catch (Exception ignored) {
            age = 0;
        }
        age = Math.min(age, crop.getMaxAge());
        BlockState next = replacement.defaultBlockState().setValue(crop.getAgeProperty(), age);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        CropGrowthManager.registerCrop(level, pos, level.getBlockState(pos));
    }
}
