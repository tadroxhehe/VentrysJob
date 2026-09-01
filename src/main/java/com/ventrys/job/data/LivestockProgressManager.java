package com.ventrys.job.data;

import com.ventrys.job.entity.CustomAnimal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if (entry == null) {
            entry = createEntryFromEntity(animal, now);
            saved.put(uuid, entry);
        } else {
            // SavedData = vérité wall-clock pour nutri/hydro/repro.
            // Sexe : toujours depuis l'entité (NBT) — le champ local "male" client avait
            // corrompu l'affichage ; on resynchronise entry.isMale depuis l'entité ici.
            entry.x = animal.getX();
            entry.y = animal.getY();
            entry.z = animal.getZ();
            entry.isMale = animal.isMale();
            ensureEntityTypeId(entry, animal);
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
            entry.matingPartnerUuid = null;
            saved.put(uuid, entry);
        }
    }

    public static void resetPregnancy(UUID uuid, ServerLevel level) {
        if (uuid == null || level == null) {
            return;
        }
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        LivestockProgressSavedData.Entry entry = saved.get(uuid);
        if (entry != null) {
            entry.pregnant = false;
            entry.pregnancyProgressMs = 0L;
            entry.matingPartnerUuid = null;
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

        if (dynamicStateChanged(entry, animal)) {
            copyEntityDynamicState(entry, animal);
            saved.put(uuid, entry);
        }
        animal.applyLivestockEntry(entry);
    }

    /**
     * Écrit la nutrition/hydratation courante de l'entité dans le SavedData sans attendre le tick.
     * À appeler après feed / hydrate / coût de reproduction.
     */
    public static void persistFromEntity(CustomAnimal animal) {
        if (animal == null || animal.level.isClientSide || !(animal.level instanceof ServerLevel level)) {
            return;
        }
        LivestockProgressSavedData saved = LivestockProgressSavedData.get(level);
        UUID uuid = animal.getUUID();
        LivestockProgressSavedData.Entry entry = saved.get(uuid);
        long now = System.currentTimeMillis();
        if (entry == null) {
            entry = createEntryFromEntity(animal, now);
        } else {
            copyEntityDynamicState(entry, animal);
        }
        saved.put(uuid, entry);
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

        // Appariement exclusif 1 mâle ↔ 1 femelle avant d'avancer les jauges.
        Map<UUID, UUID> exclusivePairs = buildExclusiveMatingPairs(snapshot, saved);

        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> mapEntry : snapshot) {
            UUID uuid = mapEntry.getKey();
            LivestockProgressSavedData.Entry entry = mapEntry.getValue();

            Entity entity = level.getEntity(uuid);
            if (entity instanceof CustomAnimal animal && animal.isAlive()) {
                // Entité chargée : pos/sexe/nutri depuis l'entité (feed immédiat), puis decay.
                copyEntityDynamicState(entry, animal);
            }
            ensureEntityTypeId(entry, null);

            long deltaMs = 0L;
            if (entry.lastProcessedWallMs > 0L) {
                deltaMs = Math.max(0L, now - entry.lastProcessedWallMs);
            } else {
                entry.lastProcessedWallMs = now;
            }

            applyNutritionDecay(entry, now);
            applyHydrationDecay(entry, now);
            applyRegeneration(entry, now);

            UUID pairedMate = exclusivePairs.get(uuid);
            applyExclusiveMateAssignment(entry, pairedMate);

            long requiredMatingMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
            long requiredPregnancyMs = MobConfig.getPregnancyTimeMinutes() * 60_000L;

            if (entry.pregnant) {
                // Phase 2 : gestation — pause si nutrition < seuil sustain (soif = seuil min inchangé).
                entry.matingPartnerUuid = null;
                if (deltaMs > 0L && !entry.isMale && canSustainReproductionProgress(entry)) {
                    entry.pregnancyProgressMs += deltaMs;
                }
                if (entry.pregnancyProgressMs > requiredPregnancyMs) {
                    entry.pregnancyProgressMs = requiredPregnancyMs;
                }
                entry.reproductionProgressMs = 0L;
            } else if (deltaMs > 0L && pairedMate != null) {
                LivestockProgressSavedData.Entry mateEntry = saved.get(pairedMate);
                // Phase 1 : les deux partenaires doivent tenir le seuil sustain pour avancer.
                if (mateEntry != null
                        && canSustainReproductionProgress(entry)
                        && canSustainReproductionProgress(mateEntry)) {
                    entry.reproductionProgressMs += deltaMs;
                    if (entry.reproductionProgressMs > requiredMatingMs) {
                        entry.reproductionProgressMs = requiredMatingMs;
                    }
                }
            }

            entry.lastProcessedWallMs = now;
            saved.put(uuid, entry);

            if (entity instanceof CustomAnimal animal && animal.isAlive()) {
                animal.applyLivestockEntry(entry);
                boolean oppositeNearby = pairedMate != null || hasLoadedOppositeSexNearby(animal);
                animal.updateReproductionHud(entry, oppositeNearby);
            }
        }

        tryStartPregnancies(level, saved, snapshot, exclusivePairs);
        tryBirths(level, saved, snapshot);
    }

    /**
     * Met à jour le partenaire exclusif. Changement de partenaire → reset de la jauge
     * (évite qu'un mâle « porte » la progression de plusieurs femelles).
     */
    private static void applyExclusiveMateAssignment(
            LivestockProgressSavedData.Entry entry,
            @Nullable UUID pairedMate) {
        if (entry.pregnant) {
            entry.matingPartnerUuid = null;
            return;
        }
        if (pairedMate == null) {
            if (entry.matingPartnerUuid != null) {
                entry.matingPartnerUuid = null;
                entry.reproductionProgressMs = 0L;
            }
            return;
        }
        if (!pairedMate.equals(entry.matingPartnerUuid)) {
            entry.matingPartnerUuid = pairedMate;
            entry.reproductionProgressMs = 0L;
        }
    }

    /**
     * Construit des paires exclusives 1↔1 (même espèce, sexe opposé, éligibles).
     * Conserve d'abord les paires sticky encore valides, puis apparie le reste
     * par proximité (plus proche d'abord).
     */
    private static Map<UUID, UUID> buildExclusiveMatingPairs(
            List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> snapshot,
            LivestockProgressSavedData saved) {
        Map<UUID, UUID> pairs = new HashMap<>();
        Set<UUID> claimed = new HashSet<>();
        double radius = MobConfig.getDetectionRadiusBlocks();
        double radiusSq = radius * radius;

        // 1) Conserver les paires mutuelles encore valides
        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> mapEntry : snapshot) {
            UUID uuid = mapEntry.getKey();
            if (claimed.contains(uuid)) {
                continue;
            }
            LivestockProgressSavedData.Entry self = saved.get(uuid);
            if (self == null || self.matingPartnerUuid == null || !isMatingCandidate(self)) {
                continue;
            }
            UUID mateUuid = self.matingPartnerUuid;
            if (claimed.contains(mateUuid)) {
                continue;
            }
            LivestockProgressSavedData.Entry mate = saved.get(mateUuid);
            if (mate == null || !isMatingCandidate(mate)) {
                continue;
            }
            if (!areValidExclusiveMates(self, mate, radiusSq)) {
                continue;
            }
            // Sticky seulement si le partenaire nous désigne encore (ou n'a plus de cible)
            if (mate.matingPartnerUuid != null && !mate.matingPartnerUuid.equals(uuid)) {
                continue;
            }
            pairs.put(uuid, mateUuid);
            pairs.put(mateUuid, uuid);
            claimed.add(uuid);
            claimed.add(mateUuid);
        }

        // 2) Apparier le reste : arêtes mâle–femelle triées par distance
        List<MateEdge> edges = new ArrayList<>();
        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> aEntry : snapshot) {
            UUID aUuid = aEntry.getKey();
            if (claimed.contains(aUuid)) {
                continue;
            }
            LivestockProgressSavedData.Entry a = saved.get(aUuid);
            if (a == null || !isMatingCandidate(a) || !a.isMale) {
                continue;
            }
            String typeA = normalizedType(a.entityTypeId);
            if (typeA.isEmpty()) {
                continue;
            }
            for (Map.Entry<UUID, LivestockProgressSavedData.Entry> bEntry : snapshot) {
                UUID bUuid = bEntry.getKey();
                if (aUuid.equals(bUuid) || claimed.contains(bUuid)) {
                    continue;
                }
                LivestockProgressSavedData.Entry b = saved.get(bUuid);
                if (b == null || !isMatingCandidate(b) || b.isMale) {
                    continue;
                }
                if (!typeA.equals(normalizedType(b.entityTypeId))) {
                    continue;
                }
                double dSq = distSq(a, b);
                if (dSq > radiusSq) {
                    continue;
                }
                edges.add(new MateEdge(aUuid, bUuid, dSq));
            }
        }
        edges.sort(Comparator.comparingDouble(e -> e.distSq));
        for (MateEdge edge : edges) {
            if (claimed.contains(edge.male) || claimed.contains(edge.female)) {
                continue;
            }
            pairs.put(edge.male, edge.female);
            pairs.put(edge.female, edge.male);
            claimed.add(edge.male);
            claimed.add(edge.female);
        }
        return pairs;
    }

    private static final class MateEdge {
        final UUID male;
        final UUID female;
        final double distSq;

        MateEdge(UUID male, UUID female, double distSq) {
            this.male = male;
            this.female = female;
            this.distSq = distSq;
        }
    }

    private static boolean isMatingCandidate(LivestockProgressSavedData.Entry entry) {
        return entry != null && !entry.pregnant && canReproduce(entry);
    }

    private static boolean areValidExclusiveMates(
            LivestockProgressSavedData.Entry a,
            LivestockProgressSavedData.Entry b,
            double radiusSq) {
        if (a.isMale == b.isMale) {
            return false;
        }
        String typeA = normalizedType(a.entityTypeId);
        String typeB = normalizedType(b.entityTypeId);
        if (typeA.isEmpty() || !typeA.equals(typeB)) {
            return false;
        }
        return distSq(a, b) <= radiusSq;
    }

    private static double distSq(LivestockProgressSavedData.Entry a, LivestockProgressSavedData.Entry b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Sexe opposé, même espèce, entité chargée dans le rayon. */
    private static boolean hasLoadedOppositeSexNearby(CustomAnimal self) {
        double radius = MobConfig.getDetectionRadiusBlocks();
        return !self.level.getEntitiesOfClass(
            CustomAnimal.class,
            self.getBoundingBox().inflate(radius),
            other -> other != self
                && other.getClass() == self.getClass()
                && other.isAlive()
                && !other.isBaby()
                && other.isMale() != self.isMale()
        ).isEmpty();
    }

    private static String normalizedType(String typeId) {
        return typeId == null ? "" : typeId;
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

    /**
     * Accouplement / grossesse : nutrition ≥ sustain (50 % défaut), soif ≥ min (inchangé).
     * En dessous → pause du timer (pas de reset).
     */
    private static boolean canSustainReproductionProgress(LivestockProgressSavedData.Entry entry) {
        return entry.nutrition >= MobConfig.getSustainNutritionPercent()
                && entry.hydration >= MobConfig.getMinHydrationPercent();
    }

    private static boolean isMatingComplete(LivestockProgressSavedData.Entry entry) {
        long requiredMs = MobConfig.getRequiredTimeMinutes() * 60_000L;
        return entry.reproductionProgressMs >= requiredMs;
    }

    private static boolean isPregnancyComplete(LivestockProgressSavedData.Entry entry) {
        long requiredMs = MobConfig.getPregnancyTimeMinutes() * 60_000L;
        return entry.pregnant && entry.pregnancyProgressMs >= requiredMs;
    }

    /**
     * Fin du timer d'accouplement → démarre la grossesse 48 h (pas de spawn immédiat).
     * Conception uniquement avec le partenaire exclusif (1 mâle ↔ 1 femelle).
     */
    private static void tryStartPregnancies(
            ServerLevel level,
            LivestockProgressSavedData saved,
            List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> snapshot,
            Map<UUID, UUID> exclusivePairs) {
        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> a : snapshot) {
            LivestockProgressSavedData.Entry entryA = saved.get(a.getKey());
            if (entryA == null || entryA.isMale || entryA.pregnant
                    || !canReproduce(entryA) || !isMatingComplete(entryA)) {
                continue;
            }

            UUID mateUuid = exclusivePairs.get(a.getKey());
            if (mateUuid == null) {
                mateUuid = entryA.matingPartnerUuid;
            }
            if (mateUuid == null) {
                continue;
            }

            LivestockProgressSavedData.Entry entryB = saved.get(mateUuid);
            if (entryB == null || entryB.isMale == entryA.isMale || entryB.pregnant
                    || !canReproduce(entryB)) {
                continue;
            }
            // Doit être mutuel : le mâle ne peut pas « servir » plusieurs femelles
            UUID back = exclusivePairs.get(mateUuid);
            if (back == null) {
                back = entryB.matingPartnerUuid;
            }
            if (back == null || !back.equals(a.getKey())) {
                continue;
            }

            Entity entityA = level.getEntity(a.getKey());
            if (!(entityA instanceof CustomAnimal animalA) || !animalA.isAlive() || animalA.isBaby()) {
                continue;
            }
            Entity entityB = level.getEntity(mateUuid);
            if (!(entityB instanceof CustomAnimal animalB) || !animalB.isAlive() || animalB.isBaby()) {
                continue;
            }
            if (!animalA.isReproductionReadyWith(animalB)) {
                continue;
            }

            // Conception : reset jauge accouplement, démarre grossesse.
            entryA.reproductionProgressMs = 0L;
            entryB.reproductionProgressMs = 0L;
            entryA.matingPartnerUuid = null;
            entryB.matingPartnerUuid = null;
            entryA.pregnant = true;
            entryA.pregnancyProgressMs = 0L;
            animalA.resetReproductionTimer();
            animalB.resetReproductionTimer();
            animalA.setPregnant(true, 0L);
            saved.put(a.getKey(), entryA);
            saved.put(mateUuid, entryB);

            animalA.addNutrition(-40);
            animalA.addHydration(-40);
            animalB.addNutrition(-40);
            animalB.addHydration(-40);
            LivestockProgressManager.persistFromEntity(animalA);
            LivestockProgressManager.persistFromEntity(animalB);
        }
    }

    /** Fin des 48 h de grossesse → spawn du bébé. */
    private static void tryBirths(
            ServerLevel level,
            LivestockProgressSavedData saved,
            List<Map.Entry<UUID, LivestockProgressSavedData.Entry>> snapshot) {
        for (Map.Entry<UUID, LivestockProgressSavedData.Entry> a : snapshot) {
            LivestockProgressSavedData.Entry entryA = saved.get(a.getKey());
            if (entryA == null || entryA.isMale || !isPregnancyComplete(entryA)) {
                continue;
            }

            Entity entityA = level.getEntity(a.getKey());
            if (!(entityA instanceof CustomAnimal animalA) || !animalA.isAlive() || animalA.isBaby()) {
                continue;
            }

            // Claim atomique avant spawn
            entryA.pregnant = false;
            entryA.pregnancyProgressMs = 0L;
            entryA.reproductionProgressMs = 0L;
            animalA.setPregnant(false, 0L);
            animalA.resetReproductionTimer();
            saved.put(a.getKey(), entryA);

            CustomAnimal mate = findLoadedMateForBirth(animalA);
            AgeableMob offspring = null;
            if (mate != null) {
                // Bypass temporaire du check faim pour la mise bas (conception déjà validée)
                int nA = animalA.getNutrition();
                int hA = animalA.getHydration();
                int nB = mate.getNutrition();
                int hB = mate.getHydration();
                animalA.setNutrition(100);
                animalA.setHydration(100);
                mate.setNutrition(100);
                mate.setHydration(100);
                offspring = animalA.getBreedOffspring(level, mate);
                animalA.setNutrition(nA);
                animalA.setHydration(hA);
                mate.setNutrition(nB);
                mate.setHydration(hB);
            }
            if (offspring == null) {
                Entity created = animalA.getType().create(level);
                if (created instanceof CustomAnimal baby) {
                    offspring = baby;
                }
            }
            if (offspring != null) {
                offspring.setAge(-24000);
                offspring.moveTo(animalA.getX(), animalA.getY(), animalA.getZ(), 0.0F, 0.0F);
                level.addFreshEntity(offspring);
            }
        }
    }

    @Nullable
    private static CustomAnimal findLoadedMateForBirth(CustomAnimal mother) {
        double radius = MobConfig.getDetectionRadiusBlocks();
        List<CustomAnimal> males = mother.level.getEntitiesOfClass(
            CustomAnimal.class,
            mother.getBoundingBox().inflate(radius),
            other -> other != mother
                && other.getClass() == mother.getClass()
                && other.isAlive()
                && !other.isBaby()
                && other.isMale()
        );
        return males.isEmpty() ? null : males.get(0);
    }

    private static boolean isReproductionComplete(LivestockProgressSavedData.Entry entry) {
        return isMatingComplete(entry);
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
        entry.pregnancyProgressMs = animal.getPregnancyProgressMs();
        entry.pregnant = animal.isPregnant();
        entry.lastProcessedWallMs = now;
        copyEntityDynamicState(entry, animal);
        return entry;
    }

    private static void copyEntityDynamicState(LivestockProgressSavedData.Entry entry, CustomAnimal animal) {
        refreshPoseAndIdentity(entry, animal);
        ensureEntityTypeId(entry, animal);
        entry.nutrition = animal.getNutrition();
        entry.hydration = animal.getHydration();
    }

    private static void refreshPoseAndIdentity(LivestockProgressSavedData.Entry entry, CustomAnimal animal) {
        entry.x = animal.getX();
        entry.y = animal.getY();
        entry.z = animal.getZ();
        entry.isMale = animal.isMale();
        ensureEntityTypeId(entry, animal);
    }

    private static void ensureEntityTypeId(LivestockProgressSavedData.Entry entry, CustomAnimal animal) {
        if (animal != null) {
            ResourceLocation typeId = ForgeRegistries.ENTITIES.getKey(animal.getType());
            if (typeId != null) {
                entry.entityTypeId = typeId.toString();
                return;
            }
        }
        if (entry.entityTypeId == null) {
            entry.entityTypeId = "";
        }
    }
}
