package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.util.FarmlandMoistureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class FarmlandProtectionHandler {

    private static final ConcurrentHashMap<Long, Long> MOISTURE_NOTIFY_COOLDOWN = new ConcurrentHashMap<>();
    private static final long MOISTURE_NOTIFY_COOLDOWN_TICKS = 10L;

    /**
     * Réagit aux mises à jour de voisinage (ex. eau) pour garder l'affichage / l'état cohérent sur farmlands suivis.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof FarmBlock) {
            if (FarmlandMoistureManager.isTracked(level, pos)) {
                long key = pos.asLong();
                long nowGameTicks = level.getGameTime();
                Long last = MOISTURE_NOTIFY_COOLDOWN.get(key);
                if (last != null && (nowGameTicks - last) < MOISTURE_NOTIFY_COOLDOWN_TICKS) {
                    return;
                }
                MOISTURE_NOTIFY_COOLDOWN.put(key, nowGameTicks);

                if (state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                    BlockState updated = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
                    level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        BlockState finalState = event.getFinalState();
        if (finalState != null && finalState.getBlock() instanceof FarmBlock && event.getToolAction() == ToolActions.HOE_TILL) {
            FarmlandMoistureManager.track(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onEntityPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        if (event.getPlacedBlock().getBlock() instanceof FarmBlock) {
            FarmlandMoistureManager.track(level, event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getWorld() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (state.getBlock() instanceof FarmBlock) {
            BlockPos farmlandPos = event.getPos();
            BlockPos cropPos = farmlandPos.above();
            BlockState cropState = level.getBlockState(cropPos);

            if (com.ventrys.job.data.CropGrowthConfig.isConfiguredCrop(cropState.getBlock())) {
                if (cropState.getBlock() instanceof net.minecraft.world.level.block.CropBlock cropBlock) {
                    int age = cropState.getValue(cropBlock.getAgeProperty());
                    int maxAge = cropBlock.getMaxAge();

                    if (age >= maxAge) {
                        net.minecraft.world.level.block.Block.getDrops(cropState, level, cropPos, null)
                            .forEach(itemStack -> {
                                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                    level, cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5, itemStack);
                                level.addFreshEntity(itemEntity);
                            });
                    } else {
                        net.minecraft.world.item.Item seedItem = cropBlock.getCloneItemStack(level, cropPos, cropState).getItem();
                        if (seedItem != null) {
                            net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                                level, cropPos.getX() + 0.5, cropPos.getY() + 0.5, cropPos.getZ() + 0.5,
                                new net.minecraft.world.item.ItemStack(seedItem));
                            level.addFreshEntity(itemEntity);
                        }
                    }

                    com.ventrys.job.data.CropGrowthManager.unregisterCrop(level, cropPos);
                    level.removeBlock(cropPos, false);
                }
            }

            FarmlandMoistureManager.untrack(level, event.getPos());
        }
    }

    public static void clearMoistureCooldown(BlockPos pos) {
        if (pos != null) {
            MOISTURE_NOTIFY_COOLDOWN.remove(pos.asLong());
        }
    }

    public static void clearMoistureCooldownForChunk(ChunkPos chunkPos) {
        if (chunkPos == null || MOISTURE_NOTIFY_COOLDOWN.isEmpty()) {
            return;
        }
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();
        MOISTURE_NOTIFY_COOLDOWN.keySet().removeIf(key -> {
            BlockPos pos = BlockPos.of(key);
            return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
        });
    }
}
