package com.ventrys.job.util;

import com.ventrys.job.data.CropGrowthConfig;
import com.ventrys.job.data.CropGrowthManager;
import com.ventrys.job.data.CropGrowthSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Scan unique d'un chunk pour cultures + farmlands (évite deux passes complètes au chargement).
 */
public final class AgriChunkScanner {

    // Pre-filtre palette : ne balaye en detail que les sections dont la palette contient
    // potentiellement un farmland ou une culture configuree. Evite ~4096 getBlockState
    // par section vide de blocs pertinents (la grande majorite).
    private static final java.util.function.Predicate<BlockState> RELEVANT =
            state -> state.getBlock() instanceof FarmBlock
                    || (state.getBlock() instanceof CropBlock && CropGrowthConfig.isConfiguredCrop(state.getBlock()));

    private AgriChunkScanner() {
    }

    public static void scanChunk(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) {
            return;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        ChunkPos chunkPos = chunk.getPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        LevelChunkSection[] sections = chunk.getSections();
        if (sections == null || sections.length == 0) {
            return;
        }

        for (LevelChunkSection section : sections) {
            if (section == null || section.hasOnlyAir()) {
                continue;
            }
            // Skip rapide via la palette : aucune section ne contient de bloc pertinent -> pas de scan.
            if (!section.maybeHas(RELEVANT)) {
                continue;
            }

            int sectionBottomY = section.bottomBlockY();
            int sectionTopExclusive = sectionBottomY + 16;
            int fromY = Math.max(minY, sectionBottomY);
            int toYExclusive = Math.min(maxY, sectionTopExclusive);
            if (fromY >= toYExclusive) {
                continue;
            }

            for (int x = 0; x < 16; x++) {
                int worldX = chunkPos.getMinBlockX() + x;
                for (int z = 0; z < 16; z++) {
                    int worldZ = chunkPos.getMinBlockZ() + z;
                    for (int y = fromY; y < toYExclusive; y++) {
                        mutable.set(worldX, y, worldZ);
                        BlockState state = chunk.getBlockState(mutable);
                        if (state.getBlock() instanceof FarmBlock) {
                            FarmlandMoistureManager.trackFromChunkScan(level, mutable, state);
                        } else if (state.getBlock() instanceof CropBlock
                            && CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
                            CropGrowthManager.registerCrop(level, mutable, state);
                        }
                    }
                }
            }
        }

        CropGrowthSavedData.get(level).purgeOrphansInChunk(level, chunkPos);
    }

    public static void forgetChunk(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) {
            return;
        }
        CropGrowthManager.forgetChunk(level, chunkPos);
        FarmlandMoistureManager.forgetChunk(level, chunkPos);
        ChickenNestIndex.forgetChunk(level, chunkPos);
    }
}
