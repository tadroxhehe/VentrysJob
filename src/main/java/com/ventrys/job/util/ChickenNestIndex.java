package com.ventrys.job.util;

import com.ventrys.job.block.entity.ChickenNestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Index des nids de poule par chunk pour éviter les scans de blocs O(n³) à la ponte.
 */
public final class ChickenNestIndex {

    private static final Map<ResourceKey<Level>, Map<Long, Set<BlockPos>>> NESTS = new HashMap<>();

    private ChickenNestIndex() {
    }

    public static void register(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = NESTS.computeIfAbsent(level.dimension(), dim -> new HashMap<>());
        perLevel.computeIfAbsent(chunkKey(pos), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = NESTS.get(level.dimension());
        if (perLevel == null || perLevel.isEmpty()) {
            return;
        }
        long ck = chunkKey(pos);
        Set<BlockPos> positions = perLevel.get(ck);
        if (positions != null) {
            positions.remove(pos);
            if (positions.isEmpty()) {
                perLevel.remove(ck);
            }
        }
    }

    public static void forgetChunk(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) {
            return;
        }
        Map<Long, Set<BlockPos>> perLevel = NESTS.get(level.dimension());
        if (perLevel != null) {
            perLevel.remove(chunkPos.toLong());
        }
    }

    /**
     * Retourne le nid le plus proche avec de la place, ou {@code null}.
     */
    @Nullable
    public static BlockPos findNestWithSpace(ServerLevel level, BlockPos origin, int radius, int yRange) {
        Map<Long, Set<BlockPos>> perLevel = NESTS.get(level.dimension());
        if (perLevel == null || perLevel.isEmpty()) {
            return null;
        }

        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int chunkRadius = (radius + 15) >> 4;
        long radiusSq = (long) radius * radius;

        BlockPos closest = null;
        double closestDistSq = Double.MAX_VALUE;

        for (int cx = originChunkX - chunkRadius; cx <= originChunkX + chunkRadius; cx++) {
            for (int cz = originChunkZ - chunkRadius; cz <= originChunkZ + chunkRadius; cz++) {
                Set<BlockPos> nests = perLevel.get(ChunkPos.asLong(cx, cz));
                if (nests == null || nests.isEmpty()) {
                    continue;
                }

                for (BlockPos nestPos : nests) {
                    if (Math.abs(nestPos.getY() - origin.getY()) > yRange) {
                        continue;
                    }
                    double distSq = origin.distSqr(nestPos);
                    if (distSq > radiusSq) {
                        continue;
                    }

                    BlockEntity be = level.getBlockEntity(nestPos);
                    if (!(be instanceof ChickenNestBlockEntity nest)) {
                        continue;
                    }
                    if (!nest.canAddEgg()) {
                        continue;
                    }

                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = nestPos;
                    }
                }
            }
        }

        return closest;
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }
}
