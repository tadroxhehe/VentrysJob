package com.ventrys.job.data;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Données de monde persistantes pour la croissance des cultures.
 *
 * <p>On stocke, pour chaque culture suivie, l'identifiant du bloc, l'âge atteint et le
 * temps réel (wall-clock, {@link System#currentTimeMillis()}) auquel le stade courant a
 * commencé. Comme pour les animaux, la progression est donc basée sur le temps réel : elle
 * survit au déchargement des chunks et aux redémarrages du serveur (rattrapage au rechargement).</p>
 */
public final class CropGrowthSavedData extends SavedData {

    private static final String DATA_NAME = "ventrysjob_crops";

    /** Entrée persistée pour une culture (immuable). */
    public static final class CropEntry {
        public final String blockId;
        public final int lastAge;
        public final long stageStartRealMs;

        public CropEntry(String blockId, int lastAge, long stageStartRealMs) {
            this.blockId = blockId;
            this.lastAge = lastAge;
            this.stageStartRealMs = stageStartRealMs;
        }
    }

    private final Map<BlockPos, CropEntry> crops = new HashMap<>();

    public static CropGrowthSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            CropGrowthSavedData::load,
            CropGrowthSavedData::new,
            DATA_NAME
        );
    }

    public static CropGrowthSavedData load(CompoundTag tag) {
        CropGrowthSavedData data = new CropGrowthSavedData();
        ListTag list = tag.getList("crops", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            BlockPos pos = new BlockPos(c.getInt("x"), c.getInt("y"), c.getInt("z"));
            data.crops.put(pos, new CropEntry(
                c.getString("id"),
                c.getInt("age"),
                c.getLong("start")
            ));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, CropEntry> e : crops.entrySet()) {
            BlockPos pos = e.getKey();
            CropEntry entry = e.getValue();
            CompoundTag c = new CompoundTag();
            c.putInt("x", pos.getX());
            c.putInt("y", pos.getY());
            c.putInt("z", pos.getZ());
            c.putString("id", entry.blockId);
            c.putInt("age", entry.lastAge);
            c.putLong("start", entry.stageStartRealMs);
            list.add(c);
        }
        tag.put("crops", list);
        return tag;
    }

    public CropEntry getCrop(BlockPos pos) {
        return crops.get(pos);
    }

    public void putCrop(BlockPos pos, CropEntry entry) {
        crops.put(pos.immutable(), entry);
        setDirty();
    }

    public void removeCrop(BlockPos pos) {
        if (crops.remove(pos) != null) {
            setDirty();
        }
    }

    public int size() {
        return crops.size();
    }

    /** Retire les entrées persistées dont le bloc n'est plus une culture configurée (chunk chargé). */
    public int purgeOrphansInChunk(ServerLevel level, ChunkPos chunkPos) {
        int minX = chunkPos.getMinBlockX();
        int maxX = chunkPos.getMaxBlockX();
        int minZ = chunkPos.getMinBlockZ();
        int maxZ = chunkPos.getMaxBlockZ();
        int removed = 0;

        Iterator<Map.Entry<BlockPos, CropEntry>> it = crops.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, CropEntry> entry = it.next();
            BlockPos pos = entry.getKey();
            if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ) {
                continue;
            }
            if (!isOrphanCrop(level, pos)) {
                continue;
            }
            it.remove();
            removed++;
        }

        if (removed > 0) {
            setDirty();
            VentrysJob.LOGGER.debug("Purge cultures orphelines chunk {}: {} entrée(s)", chunkPos, removed);
        }
        return removed;
    }

    /** Parcourt un lot d'entrées persistées et retire celles invalides dans des chunks chargés. */
    public int purgeOrphanBatch(ServerLevel level, int maxChecks) {
        if (maxChecks <= 0 || crops.isEmpty()) {
            return 0;
        }

        int checked = 0;
        int removed = 0;
        Iterator<Map.Entry<BlockPos, CropEntry>> it = crops.entrySet().iterator();
        while (it.hasNext() && checked < maxChecks) {
            Map.Entry<BlockPos, CropEntry> entry = it.next();
            checked++;
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos) || !isOrphanCrop(level, pos)) {
                continue;
            }
            it.remove();
            removed++;
        }

        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    private static boolean isOrphanCrop(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !(state.getBlock() instanceof CropBlock)
            || !CropGrowthConfig.isConfiguredCrop(state.getBlock());
    }
}
