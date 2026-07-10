package com.ventrys.job.util;

import com.ventrys.job.event.FarmlandProtectionHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enregistre les farmlands du mod et force l'humidité max au labour / au chargement de chunk.
 * La baisse d'humidité et le passage en dirt vanilla sont désactivés via {@link com.ventrys.job.mixin.FarmBlockMixin}.
 */
public final class FarmlandMoistureManager {

    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> TRACKED = new HashMap<>();

    private FarmlandMoistureManager() {
    }

    public static void track(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof FarmBlock)) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.computeIfAbsent(level.dimension(), dim -> new HashMap<>());
        long chunkKey = chunkKey(pos);
        perLevel.computeIfAbsent(chunkKey, key -> new HashSet<>()).add(pos.immutable());
        ensureMoisture(level, pos, state, true);
    }

    /** Enregistrement au scan de chunk : pas de propagation voisins (évite les cascades NeighborNotify). */
    public static void trackFromChunkScan(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || !(state.getBlock() instanceof FarmBlock)) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.computeIfAbsent(level.dimension(), dim -> new HashMap<>());
        long chunkKey = chunkKey(pos);
        perLevel.computeIfAbsent(chunkKey, key -> new HashSet<>()).add(pos.immutable());
        ensureMoisture(level, pos, state, false);
    }

    public static void untrack(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.get(level.dimension());
        if (perLevel == null || perLevel.isEmpty()) {
            return;
        }
        long chunkKey = chunkKey(pos);
        Set<BlockPos> positions = perLevel.get(chunkKey);
        if (positions != null) {
            positions.remove(pos);
            if (positions.isEmpty()) {
                perLevel.remove(chunkKey);
            }
        }
        FarmlandProtectionHandler.clearMoistureCooldown(pos);
    }

    public static void forgetChunk(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.get(level.dimension());
        if (perLevel != null) {
            perLevel.remove(chunkPos.toLong());
        }
        FarmlandProtectionHandler.clearMoistureCooldownForChunk(chunkPos);
    }

    private static void ensureMoisture(ServerLevel level, BlockPos pos, BlockState state, boolean notifyNeighbors) {
        if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
            BlockState updated = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
            int flags = Block.UPDATE_CLIENTS | (notifyNeighbors ? Block.UPDATE_NEIGHBORS : 0);
            level.setBlock(pos, updated, flags);
        }
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static boolean isTracked(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.get(level.dimension());
        if (perLevel == null || perLevel.isEmpty()) {
            return false;
        }
        long chunkKey = chunkKey(pos);
        Set<BlockPos> positions = perLevel.get(chunkKey);
        return positions != null && positions.contains(pos.immutable());
    }
}
