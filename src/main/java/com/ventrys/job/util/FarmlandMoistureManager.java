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
import net.minecraft.world.level.chunk.LevelChunk;

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
        // Labour / pose : setBlock normal OK (chunk déjà chargé, pas pendant ChunkEvent.Load).
        ensureMoistureViaLevel(level, pos, state);
    }

    /**
     * Scan au {@code ChunkEvent.Load} : ne jamais appeler {@link Level#setBlock} ici.
     * Ça déclenche neighbor updates + getChunk → deadlock / ServerHangWatchdog (60s).
     */
    public static void trackFromChunkScan(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || !(state.getBlock() instanceof FarmBlock)) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = TRACKED.computeIfAbsent(level.dimension(), dim -> new HashMap<>());
        long chunkKey = chunkKey(pos);
        perLevel.computeIfAbsent(chunkKey, key -> new HashSet<>()).add(pos.immutable());

        if (state.getValue(FarmBlock.MOISTURE) >= FarmBlock.MAX_MOISTURE) {
            return;
        }
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            return;
        }
        BlockState updated = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        chunk.setBlockState(pos, updated, false);
        level.getChunkSource().blockChanged(pos);
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

    private static void ensureMoistureViaLevel(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
            BlockState updated = state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
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
