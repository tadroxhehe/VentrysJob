package com.ventrys.job.data;

import com.ventrys.job.entity.CustomAnimal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Progression temps réel (wall-clock) des animaux d'élevage, y compris chunks déchargés.
 */
public final class LivestockProgressManager {

    private static final long NUTRITION_DECREASE_INTERVAL_MS = 1_200_000L;
    private static final long HYDRATION_DECREASE_INTERVAL_MS = 600_000L;
    private static final long REGENERATION_INTERVAL_MS = 3_600_000L;
    /** Aligné sur la vérification reproduction (intervalle technique). */
    private static final int ADVANCE_INTERVAL_MS = 5_000;
    private static final double POS_EPS = 0.05D;

    private static int tickCounter = 0;

    private LivestockProgressManager() {
    }

    public static void registerFromEntity(CustomAnimal animal) {
        if (animal.level.isClientSide || !(animal.level instanceof ServerLevel level)) {
            return;
        }
        UUID uuid = animal.getUUID();
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        long now = System.currentTimeMillis();

        LivestockProgressSavedData.Entry entry = saved.get(uuid);
        if (entry != null && (entry.nutrition <= 0 || entry.hydration <= 0)) {
            // Mort différée pendant le chunk était déchargé.
            if (animal.isAlive() && !animal.isDeadOrDying()) {
                animal.hurt(net.minecraft.world.damagesource.DamageSource.STARVE, Float.MAX_VALUE);
            }
            saved.remove(uuid);
            return;
        }

        if (entry == null) {
            entry = createEntryFromEntity(animal, now);
            saved.put(uuid, entry);
        } else {
            copyEntityDynamicState(entry, animal);
            animal.applyLivestockEntry(entry);
            saved.put(uuid, entry);
        }
    }

    public static void registerAnimalsInChunk(ServerLevel level, LevelChunk chunk) {
        if (level == null || chunk == null) {
            return;
        }
        ChunkPos chunkPos = chunk.getPos();
        AABB box = new AABB(
            chunkPos.getMinBlockX(), level.getMinBuildHeight(), chunkPos.getMinBlockZ(),
            chunkPos.getMaxBlockX() + 1, level.getMaxBuildHeight(), chunkPos.getMaxBlockZ() + 1
        );
        for (CustomAnimal animal : level.getEntitiesOfClass(CustomAnimal.class, box, EntitySelector.ENTITY_STILL_ALIVE)) {
            registerFromEntity(animal);
        }
    }

    public static void resetReproduction(UUID uuid, ServerLevel level) {
        if (uuid == null || level == null) {
            return;
        }
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        LivestockProgressSavedData.Entry entry = saved.get(uuid);
        if (entry != null) {
            entry.reproductionProgressMs = 0L;
            saved.put(uuid, entry);
        }
    }

    public static void remove(UUID uuid, ServerLevel level) {
        if (uuid == null || level == null) {
            return;
        }
        LivestockProgressSavedData.get(level).remove(uuid);
    }

    public static void syncEntity(CustomAnimal animal) {
        if (animal.level.isClientSide || !(animal.level instanceof ServerLevel level)) {
            return;
        }
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        UUID uuid = animal.getUUID();
        LivestockProgressSavedData.Entry entry = saved.get(uuid);
        if (entry == null) {
            registerFromEntity(animal);
            return;
        }

        if (entry.nutrition <= 0 || entry.hydration <= 0) {
            if (animal.isAlive() && !animal.isDeadOrDying()) {
                animal.hurt(net.minecraft.world.damagesource.DamageSource.STARVE, Float.MAX_VALUE);
            }
            saved.remove(uuid);
            return;
        }

        if (dynamicStateChanged(entry, animal)) {
            copyEntityDynamicState(entry, animal);
            saved.put(uuid, entry);
        }
        animal.applyLivestockEntry(entry);
    }

    public static void handleServerTick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        tickCounter++;
        if (tickCounter % (ADVANCE_INTERVAL_MS / 50) != 0) {
            return;
        }

        long now = System.currentTimeMillis();
        for (ServerLevel level : server.getAllLevels()) {
            advanceDimension(level, now);
        }
    }

    private static void advanceDimension(ServerLevel level, long now) {
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        if (saved.size() == 0) {
            return;
        }

        List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> snapshot =
            new ArrayList<>(saved.allEntries().entrySet());
        Map<Long, List<Map.Entry<UUID, LivestockProgressSavedData.Entry>>> spatial =
            buildSpatialIndex(snapshot, MobConfig.getDetectionRadiusBlocks());

        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> mapEntry : snapshot) {
            UUID uuid = mapEntry.getKey();
            LivestockProgressSavedData.Entry entry = mapEntry.getValue();

            Entity entity = level.getEntity(uuid);
            if (entity instanceof CustomAnimal animal && animal.isAlive()) {
                copyEntityDynamicState(entry, animal);
            }

            long deltaMs = 0L;
            if (entry.lastProcessedWallMs > 0L) {
                deltaMs = Math.max(0L, now - entry.lastProcessedWallMs);
            } else {
                entry.lastProcessedWallMs = now;
            }

            applyNutritionDecay(entry, now);
            applyHydrationDecay(entry, now);
            applyRegeneration(entry, now);

            if (deltaMs > 0L && canReproduce(entry) && hasMateNearby(uuid, entry, spatial)) {
                entry.reproductionProgressMs += deltaMs;
            }

            entry.lastProcessedWallMs = now;
            saved.put(uuid, entry);

            if (entry.nutrition <= 0 || entry.hydration <= 0) {
                if (entity instanceof CustomAnimal animal && animal.isAlive()) {
                    animal.hurt(net.minecraft.world.damagesource.DamageSource.STARVE, Float.MAX_VALUE);
                    saved.remove(uuid);
                }
                // Chunk déchargé : garder l'entrée à 0 pour tuer au rechargement.
            } else if (entity instanceof CustomAnimal animal && animal.isAlive()) {
                animal.applyLivestockEntry(entry);
            }
        }

        tryAutomaticBreeding(level, saved, snapshot, spatial);
    }

    private static Map<Long, List<Map.Entry<UUID, LivestockProgressSavedData.Entry>>> buildSpatialIndex(
            List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> all,
            double radius) {
        int cell = Math.max(1, (int) Math.ceil(radius));
        Map<Long, List<Map.Entry<UUID, LivestockProgressSavedData.Entry>>> index = new HashMap<>();
        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> e : all) {
            LivestockProgressSavedData.Entry entry = e.getValue();
            long key = cellKey(entry.x, entry.z, cell);
            index.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        return index;
    }

    private static long cellKey(double x, double z, int cell) {
        int cx = floorDiv((int) Math.floor(x), cell);
        int cz = floorDiv((int) Math.floor(z), cell);
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    private static int floorDiv(int a, int b) {
        int q = a / b;
        if ((a ^ b) < 0 && q * b != a) {
            q--;
        }
        return q;
    }

    private static boolean hasMateNearby(
            UUID selfUuid,
            LivestockProgressSavedData.Entry self,
            Map<Long, List<Map.Entry<UUID, LivestockProgressSavedData.Entry>>> spatial) {
        double radius = MobConfig.getDetectionRadiusBlocks();
        double radiusSq = radius * radius;
        int cell = Math.max(1, (int) Math.ceil(radius));
        int cx = floorDiv((int) Math.floor(self.x), cell);
        int cz = floorDiv((int) Math.floor(self.z), cell);
        Vec3 posSelf = new Vec3(self.x, self.y, self.z);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = (((long) (cx + dx)) << 32) ^ ((cz + dz) & 0xffffffffL);
                List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> bucket = spatial.get(key);
                if (bucket == null) {
                    continue;
                }
                for (Map.Entry<UUID, LivestockProgressSavedData.Entry> other : bucket) {
                    if (other.getKey().equals(selfUuid)) {
                        continue;
                    }
                    LivestockProgressSavedData.Entry mate = other.getValue();
                    if (!self.entityTypeId.equals(mate.entityTypeId)) {
                        continue;
                    }
                    if (self.isMale == mate.isMale) {
                        continue;
                    }
                    if (!canReproduce(mate)) {
                        continue;
                    }
                    Vec3 posMate = new Vec3(mate.x, mate.y, mate.z);
                    if (posSelf.distanceToSqr(posMate) <= radiusSq) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void applyNutritionDecay(LivestockProgressSavedData.Entry entry, long now) {
        long elapsed = now - entry.lastNutritionDecreaseMs;
        if (elapsed < 0) {
            entry.lastNutritionDecreaseMs = now;
            return;
        }
        if (elapsed < NUTRITION_DECREASE_INTERVAL_MS) {
            return;
        }
        long maxAllowed = NUTRITION_DECREASE_INTERVAL_MS * 10L;
        if (elapsed > maxAllowed) {
            elapsed = maxAllowed;
            entry.lastNutritionDecreaseMs = now - maxAllowed;
        }
        int decreases = (int) (elapsed / NUTRITION_DECREASE_INTERVAL_MS);
        entry.nutrition = Math.max(0, entry.nutrition - decreases);
        entry.lastNutritionDecreaseMs = now - (elapsed % NUTRITION_DECREASE_INTERVAL_MS);
    }

    private static void applyHydrationDecay(LivestockProgressSavedData.Entry entry, long now) {
        long elapsed = now - entry.lastHydrationDecreaseMs;
        if (elapsed < 0) {
            entry.lastHydrationDecreaseMs = now;
            return;
        }
        if (elapsed < HYDRATION_DECREASE_INTERVAL_MS) {
            return;
        }
        long maxAllowed = HYDRATION_DECREASE_INTERVAL_MS * 10L;
        if (elapsed > maxAllowed) {
            elapsed = maxAllowed;
            entry.lastHydrationDecreaseMs = now - maxAllowed;
        }
        int decreases = (int) (elapsed / HYDRATION_DECREASE_INTERVAL_MS);
        entry.hydration = Math.max(0, entry.hydration - decreases);
        entry.lastHydrationDecreaseMs = now - (elapsed % HYDRATION_DECREASE_INTERVAL_MS);
    }

    private static void applyRegeneration(LivestockProgressSavedData.Entry entry, long now) {
        if (entry.nutrition < 50 || entry.hydration < 50) {
            entry.lastRegenerationMs = now;
            return;
        }
        long elapsed = now - entry.lastRegenerationMs;
        if (elapsed < 0) {
            entry.lastRegenerationMs = now;
            return;
        }
        if (elapsed < REGENERATION_INTERVAL_MS) {
            return;
        }
        int hearts = (int) (elapsed / REGENERATION_INTERVAL_MS);
        entry.pendingHealHearts += hearts;
        entry.lastRegenerationMs = now - (elapsed % REGENERATION_INTERVAL_MS);
    }

    private static boolean canReproduce(LivestockProgressSavedData.Entry entry) {
        int minNutrition = MobConfig.getMinNutritionPercent();
        int minHydration = MobConfig.getMinHydrationPercent();
        return entry.nutrition >= minNutrition && entry.hydration >= minHydration;
    }

    private static boolean isReproductionComplete(LivestockProgressSavedData.Entry entry) {
        long requiredMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
        return entry.reproductionProgressMs >= requiredMs;
    }

    private static void tryAutomaticBreeding(
            ServerLevel level,
            LivestockProgressSavedData saved,
            List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> snapshot,
            Map<Long, List<Map.Entry<UUID, LivestockProgressSavedData.Entry>>> spatial) {
        double radius = MobConfig.getDetectionRadiusBlocks();
        double radiusSq = radius * radius;
        int cell = Math.max(1, (int) Math.ceil(radius));

        for (int i = 0; i < snapshot.size(); i++) {
            Map.Entry<UUID, LivestockProgressSavedData.Entry> a = snapshot.get(i);
            LivestockProgressSavedData.Entry entryA = saved.get(a.getKey());
            if (entryA == null || !canReproduce(entryA) || !isReproductionComplete(entryA)) {
                continue;
            }

            Entity entityA = level.getEntity(a.getKey());
            if (!(entityA instanceof CustomAnimal animalA) || !animalA.isAlive()) {
                continue;
            }

            int cx = floorDiv((int) Math.floor(entryA.x), cell);
            int cz = floorDiv((int) Math.floor(entryA.z), cell);
            Vec3 posA = new Vec3(entryA.x, entryA.y, entryA.z);
            boolean bred = false;

            for (int dx = -1; dx <= 1 && !bred; dx++) {
                for (int dz = -1; dz <= 1 && !bred; dz++) {
                    long key = (((long) (cx + dx)) << 32) ^ ((cz + dz) & 0xffffffffL);
                    List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> bucket = spatial.get(key);
                    if (bucket == null) {
                        continue;
                    }
                    for (Map.Entry<UUID, LivestockProgressSavedData.Entry> b : bucket) {
                        if (b.getKey().equals(a.getKey())) {
                            continue;
                        }
                        LivestockProgressSavedData.Entry entryB = saved.get(b.getKey());
                        if (entryB == null) {
                            continue;
                        }
                        if (!entryA.entityTypeId.equals(entryB.entityTypeId)) {
                            continue;
                        }
                        if (entryA.isMale == entryB.isMale) {
                            continue;
                        }
                        if (!canReproduce(entryB) || !isReproductionComplete(entryB)) {
                            continue;
                        }

                        Vec3 posB = new Vec3(entryB.x, entryB.y, entryB.z);
                        if (posA.distanceToSqr(posB) > radiusSq) {
                            continue;
                        }

                        Entity entityB = level.getEntity(b.getKey());
                        if (!(entityB instanceof CustomAnimal animalB) || !animalB.isAlive()) {
                            continue;
                        }

                        if (!animalA.isReproductionReadyWith(animalB)) {
                            continue;
                        }

                        var offspring = animalA.getBreedOffspring(level, animalB);
                        if (offspring != null) {
                            offspring.setAge(-24000);
                            double midX = (entryA.x + entryB.x) / 2.0;
                            double midY = Math.max(entryA.y, entryB.y);
                            double midZ = (entryA.z + entryB.z) / 2.0;
                            offspring.moveTo(midX, midY, midZ, 0.0F, 0.0F);
                            level.addFreshEntity(offspring);

                            entryA.reproductionProgressMs = 0L;
                            entryB.reproductionProgressMs = 0L;
                            saved.put(a.getKey(), entryA);
                            saved.put(b.getKey(), entryB);
                            animalA.resetReproductionTimer();
                            animalB.resetReproductionTimer();
                            bred = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean dynamicStateChanged(LivestockProgressSavedData.Entry entry, CustomAnimal animal) {
        if (entry.nutrition != animal.getNutrition() || entry.hydration != animal.getHydration()) {
            return true;
        }
        if (entry.isMale != animal.isMale()) {
            return true;
        }
        if (Math.abs(entry.x - animal.getX()) > POS_EPS
                || Math.abs(entry.y - animal.getY()) > POS_EPS
                || Math.abs(entry.z - animal.getZ()) > POS_EPS) {
            return true;
        }
        ResourceLocation typeId = ForgeRegistries.ENTITIES.getKey(animal.getType());
        String type = typeId != null ? typeId.toString() : "";
        return !type.equals(entry.entityTypeId);
    }

    private static LivestockProgressSavedData.Entry createEntryFromEntity(CustomAnimal animal, long now) {
        LivestockProgressSavedData.Entry entry = new LivestockProgressSavedData.Entry();
        entry.lastNutritionDecreaseMs = animal.getLastNutritionDecreaseMs();
        entry.lastHydrationDecreaseMs = animal.getLastHydrationDecreaseMs();
        entry.lastRegenerationMs = animal.getLastRegenerationTimeMs();
        entry.reproductionProgressMs = animal.getReproductionProgressMs();
        entry.lastProcessedWallMs = now;
        copyEntityDynamicState(entry, animal);
        return entry;
    }

    private static void copyEntityDynamicState(LivestockProgressSavedData.Entry entry, CustomAnimal animal) {
        ResourceLocation typeId = ForgeRegistries.ENTITIES.getKey(animal.getType());
        if (typeId != null) {
            entry.entityTypeId = typeId.toString();
        }
        entry.x = animal.getX();
        entry.y = animal.getY();
        entry.z = animal.getZ();
        entry.isMale = animal.isMale();
        entry.nutrition = animal.getNutrition();
        entry.hydration = animal.getHydration();
    }
}
