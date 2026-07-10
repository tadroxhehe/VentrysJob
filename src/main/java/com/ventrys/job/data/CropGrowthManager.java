package com.ventrys.job.data;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ChunkPos;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire runtime des cultures suivies. Stockage par chunk pour déchargement O(1) et tick amorti.
 *
 * <p>La progression est basée sur le <b>temps réel</b> (wall-clock, {@link System#currentTimeMillis()})
 * comme pour les animaux, et persistée via {@link CropGrowthSavedData}. La map en mémoire ne sert qu'à
 * itérer efficacement sur les cultures des chunks chargés ; la source de vérité du chrono est la
 * sauvegarde de monde, ce qui rend la croissance fiable même après déchargement de chunk ou
 * redémarrage du serveur (rattrapage au rechargement).</p>
 */
public final class CropGrowthManager {

    /** Conversion ticks (config) → millisecondes réelles : 20 ticks/seconde. */
    private static final long MS_PER_TICK = 50L;

    /** Dimension → chunk (long) → positions dans le chunk */
    private static final Map<ResourceKey<Level>, ConcurrentHashMap<Long, ConcurrentHashMap<BlockPos, CropGrowthData>>> ACTIVE_CROPS = new ConcurrentHashMap<>();

    private static final int GROWTH_CHECK_INTERVAL = 5;
    private static final int SURVEILLANCE_CHECK_INTERVAL = 20;
    private static final int MAX_CROPS_PER_TICK = 100;
    private static final int SAVED_DATA_PURGE_INTERVAL = 1200;
    private static final int SAVED_DATA_PURGE_BATCH = 250;
    private static int tickCounter = 0;
    /** Rotation sur la liste des chunks (indices), pas sur chaque culture */
    private static final Map<ResourceKey<Level>, Integer> CHUNK_ROTATION_INDEX = new ConcurrentHashMap<>();

    private CropGrowthManager() {
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static long stageDurationMs(Block block, int age) {
        return CropGrowthConfig.getStageDurationTicks(block, age) * MS_PER_TICK;
    }

    private static ConcurrentHashMap<BlockPos, CropGrowthData> cropsInChunk(ServerLevel level, BlockPos pos) {
        return ACTIVE_CROPS
            .computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey(pos), c -> new ConcurrentHashMap<>());
    }

    public static void registerCrop(ServerLevel level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null) {
            return;
        }

        Block block = state.getBlock();
        if (!(block instanceof CropBlock cropBlock)) {
            return;
        }

        if (!CropGrowthConfig.isConfiguredCrop(block)) {
            return;
        }

        ResourceLocation blockId = block.getRegistryName();
        if (blockId == null) {
            return;
        }

        String blockIdStr = blockId.toString();

        int age = state.getValue(cropBlock.getAgeProperty());
        ConcurrentHashMap<BlockPos, CropGrowthData> crops = cropsInChunk(level, pos);
        BlockPos immutablePos = pos.immutable();
        CropGrowthData existing = crops.get(immutablePos);
        if (existing != null) {
            if (existing.lastAge() != age || !existing.blockId().equals(blockIdStr)) {
                long startMs = System.currentTimeMillis();
                crops.put(immutablePos, new CropGrowthData(blockIdStr, block, age, startMs));
                persist(level, immutablePos, blockIdStr, age, startMs);
            }
            return;
        }

        // Absent de la mémoire : on tente de reprendre le chrono persistant (rechargement de chunk,
        // redémarrage du serveur). Sinon, nouvelle culture → départ au temps réel courant.
        CropGrowthSavedData saved = CropGrowthSavedData.get(level);
        CropGrowthSavedData.CropEntry savedEntry = saved.getCrop(immutablePos);
        long startMs;
        if (savedEntry != null && savedEntry.blockId.equals(blockIdStr) && savedEntry.lastAge == age) {
            startMs = savedEntry.stageStartRealMs;
        } else {
            startMs = System.currentTimeMillis();
            saved.putCrop(immutablePos, new CropGrowthSavedData.CropEntry(blockIdStr, age, startMs));
        }

        crops.put(immutablePos, new CropGrowthData(blockIdStr, block, age, startMs));

        VentrysJob.LOGGER.debug("Culture enregistrée: {} à {} (âge {})", blockId, pos, age);
    }

    /** Met à jour l'entrée persistante d'une culture (chrono temps réel). */
    private static void persist(ServerLevel level, BlockPos pos, String blockId, int age, long startMs) {
        CropGrowthSavedData.get(level).putCrop(pos, new CropGrowthSavedData.CropEntry(blockId, age, startMs));
    }

    public static void forgetChunk(ServerLevel level, ChunkPos chunkPos) {
        if (level == null || chunkPos == null) {
            return;
        }
        // On retire uniquement de la mémoire : la sauvegarde persistante conserve le chrono
        // pour que la culture continue de progresser pendant que le chunk est déchargé.
        ConcurrentHashMap<Long, ConcurrentHashMap<BlockPos, CropGrowthData>> dim = ACTIVE_CROPS.get(level.dimension());
        if (dim == null || dim.isEmpty()) {
            return;
        }
        dim.remove(chunkPos.toLong());
        if (dim.isEmpty()) {
            ACTIVE_CROPS.remove(level.dimension());
        }
    }

    public static void unregisterCrop(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        // Suppression définitive (récolte/casse) : mémoire ET sauvegarde persistante.
        if (level instanceof ServerLevel serverLevel) {
            CropGrowthSavedData.get(serverLevel).removeCrop(pos);
        }

        ConcurrentHashMap<Long, ConcurrentHashMap<BlockPos, CropGrowthData>> dim = ACTIVE_CROPS.get(level.dimension());
        if (dim == null) {
            return;
        }
        long ck = chunkKey(pos);
        ConcurrentHashMap<BlockPos, CropGrowthData> chunk = dim.get(ck);
        if (chunk != null) {
            chunk.remove(pos);
            if (chunk.isEmpty()) {
                dim.remove(ck);
                if (dim.isEmpty()) {
                    ACTIVE_CROPS.remove(level.dimension());
                }
            }
        }
    }

    public static void handleServerTick(MinecraftServer server) {
        if (server == null) {
            return;
        }

        tickCounter++;
        boolean shouldCheckGrowth = (tickCounter % GROWTH_CHECK_INTERVAL == 0);
        boolean shouldCheckSurveillance = (tickCounter % SURVEILLANCE_CHECK_INTERVAL == 0);
        boolean shouldPurgeSavedData = (tickCounter % SAVED_DATA_PURGE_INTERVAL == 0);

        // Croissance tous les 5 ticks ; surveillance tous les 20 (toujours alignée sur un tick de croissance).
        if (!shouldCheckGrowth && !shouldCheckSurveillance && !shouldPurgeSavedData) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            if (shouldPurgeSavedData) {
                int removed = CropGrowthSavedData.get(level).purgeOrphanBatch(level, SAVED_DATA_PURGE_BATCH);
                if (removed > 0) {
                    VentrysJob.LOGGER.debug("Purge cultures persistées {}: {} orphelin(s)", level.dimension().location(), removed);
                }
            }
        }

        if (!shouldCheckGrowth && !shouldCheckSurveillance) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            ConcurrentHashMap<Long, ConcurrentHashMap<BlockPos, CropGrowthData>> byChunk = ACTIVE_CROPS.get(level.dimension());
            if (byChunk == null || byChunk.isEmpty()) {
                continue;
            }

            CropGrowthSavedData saved = CropGrowthSavedData.get(level);
            long now = System.currentTimeMillis();

            Long[] chunkKeys = byChunk.keySet().toArray(new Long[0]);
            if (chunkKeys.length == 0) {
                continue;
            }

            int numChunks = chunkKeys.length;
            int startChunk = Math.floorMod(CHUNK_ROTATION_INDEX.getOrDefault(level.dimension(), 0), numChunks);

            int processedCount = 0;

            chunkLoop:
            for (int step = 0; step < numChunks && processedCount < MAX_CROPS_PER_TICK; step++) {
                long ck = chunkKeys[(startChunk + step) % numChunks];
                ConcurrentHashMap<BlockPos, CropGrowthData> crops = byChunk.get(ck);
                if (crops == null || crops.isEmpty()) {
                    continue;
                }

                for (Iterator<Map.Entry<BlockPos, CropGrowthData>> it = crops.entrySet().iterator();
                     it.hasNext() && processedCount < MAX_CROPS_PER_TICK; ) {
                    Map.Entry<BlockPos, CropGrowthData> entry = it.next();
                    BlockPos pos = entry.getKey();
                    CropGrowthData data = entry.getValue();

                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof CropBlock cropBlock)) {
                        it.remove();
                        saved.removeCrop(pos);
                        if (crops.isEmpty()) {
                            byChunk.remove(ck);
                            if (byChunk.isEmpty()) {
                                ACTIVE_CROPS.remove(level.dimension());
                            }
                        }
                        processedCount++;
                        continue;
                    }

                    ResourceLocation blockId = state.getBlock().getRegistryName();
                    if (state.getBlock() != data.block()) {
                        it.remove();
                        saved.removeCrop(pos);
                        if (crops.isEmpty()) {
                            byChunk.remove(ck);
                            if (byChunk.isEmpty()) {
                                ACTIVE_CROPS.remove(level.dimension());
                            }
                        }
                        processedCount++;
                        continue;
                    }

                    int currentAge = state.getValue(cropBlock.getAgeProperty());

                    if (shouldCheckSurveillance && currentAge != data.lastAge()) {
                        long durationMs = stageDurationMs(state.getBlock(), data.lastAge());
                        boolean shouldHaveGrown = (now - data.stageStartRealMs()) >= durationMs;
                        boolean isAuthorizedGrowth = shouldHaveGrown && currentAge == data.lastAge() + 1;

                        if (!isAuthorizedGrowth) {
                            BlockState restoredState = state.setValue(cropBlock.getAgeProperty(), data.lastAge());
                            level.setBlock(pos, restoredState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                            VentrysJob.LOGGER.debug(
                                "Changement vanilla annulé: {} à {} (âge {} -> {} restauré)",
                                blockId, pos, currentAge, data.lastAge()
                            );
                            processedCount++;
                            continue;
                        }

                        crops.put(pos, new CropGrowthData(data.blockId(), state.getBlock(), currentAge, now));
                        persist(level, pos, data.blockId(), currentAge, now);
                        VentrysJob.LOGGER.debug(
                            "Changement vanilla autorisé (rattrapage): {} à {} (âge {})",
                            blockId, pos, currentAge
                        );
                        processedCount++;
                        continue;
                    }

                    if (!shouldCheckGrowth) {
                        continue;
                    }

                    if (currentAge >= cropBlock.getMaxAge()) {
                        it.remove();
                        saved.removeCrop(pos);
                        if (crops.isEmpty()) {
                            byChunk.remove(ck);
                            if (byChunk.isEmpty()) {
                                ACTIVE_CROPS.remove(level.dimension());
                                break chunkLoop;
                            }
                        }
                        processedCount++;
                        continue;
                    }

                    long durationMs = stageDurationMs(state.getBlock(), currentAge);
                    if (now - data.stageStartRealMs() < durationMs) {
                        continue;
                    }

                    int nextAge = Math.min(cropBlock.getMaxAge(), currentAge + 1);
                    BlockState newState = state.setValue(cropBlock.getAgeProperty(), nextAge);
                    level.setBlock(pos, newState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);

                    // Report du reliquat (start += durée du stade) plutôt que reset à "maintenant" :
                    // garantit un rattrapage multi-stades exact après une longue absence/déchargement.
                    long nextStartMs = data.stageStartRealMs() + durationMs;

                    if (nextAge >= cropBlock.getMaxAge()) {
                        it.remove();
                        saved.removeCrop(pos);
                        if (crops.isEmpty()) {
                            byChunk.remove(ck);
                            if (byChunk.isEmpty()) {
                                ACTIVE_CROPS.remove(level.dimension());
                                break chunkLoop;
                            }
                        }
                    } else {
                        crops.put(pos, new CropGrowthData(data.blockId(), state.getBlock(), nextAge, nextStartMs));
                        persist(level, pos, data.blockId(), nextAge, nextStartMs);
                    }

                    VentrysJob.LOGGER.debug("Culture avancée: {} -> âge {} (pos: {})", data.blockId(), nextAge, pos);

                    processedCount++;
                }
            }

            CHUNK_ROTATION_INDEX.put(level.dimension(), (startChunk + 1) % numChunks);
        }
    }

    // block : reference du bloc (singleton enregistre) pour comparer par identite dans le tick chaud
    // au lieu de reconstruire/comparer des String (getRegistryName().toString()) a chaque culture.
    private record CropGrowthData(String blockId, Block block, int lastAge, long stageStartRealMs) {
    }
}
