package com.ventrys.job.extraction;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ExtractedPositionsStore {
    public static final String EXTRACTED_TAG = "ventrysjob:extracted";

    private static final String PERSISTENCE_FILE = "ventrysjob_extracted_positions.dat";
    private static final String LEGACY_POSITION_SEPARATOR = ",";

    private static final Map<String, Boolean> extractedBlockPositions = new ConcurrentHashMap<>();

    private static final ExecutorService extractedPositionsSaveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VentrysJob-ExtractedPositionsSaveThread");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean extractedPositionsSaveRequested = new AtomicBoolean(false);
    private static final AtomicBoolean extractedPositionsSaveInFlight = new AtomicBoolean(false);

    private static int saveCounter = 0;
    private static final int SAVE_INTERVAL = 10;

    private static int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100;
    public static final int STALE_PURGE_BATCH = 200;

    private static int stalePurgeCursor = 0;

    private ExtractedPositionsStore() {
    }

    public static void markPositionAsExtracted(Level level, BlockPos pos) {
        String posKey = getPositionKey(level, pos);
        extractedBlockPositions.put(posKey, true);
        saveExtractedPositionsPeriodically();
        if (extractedBlockPositions.size() % 5 == 0) {
            requestExtractedPositionsSave();
        }
    }

    public static void unmarkPositionAsExtracted(Level level, BlockPos pos) {
        String posKey = getPositionKey(level, pos);
        extractedBlockPositions.remove(posKey);
        extractedBlockPositions.remove(getLegacyPositionKey(pos));
        saveExtractedPositionsPeriodically();
    }

    public static boolean isExtractedBlock(Level level, BlockPos pos) {
        return isExtractedPosition(level, pos);
    }

    static boolean isExtractedPosition(Level level, BlockPos pos) {
        String positionKey = getPositionKey(level, pos);
        if (extractedBlockPositions.containsKey(positionKey)) {
            return true;
        }
        return extractedBlockPositions.containsKey(getLegacyPositionKey(pos));
    }

    static void saveExtractedPositionsPeriodically() {
        saveCounter++;
        if (saveCounter >= SAVE_INTERVAL) {
            requestExtractedPositionsSave();
            saveCounter = 0;
        }

        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            ExtractionProgressManager.cleanupTemporaryData();
            cleanupCounter = 0;
        }
    }

    public static void saveExtractedPositions() {
        try {
            ExtractedPositionsPersistence.save(extractedBlockPositions);
        } catch (IOException e) {
            VentrysJob.LOGGER.warn("Impossible de sauvegarder les positions extraites: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadExtractedPositions() {
        try {
            com.ventrys.job.util.VentrysJobDataPaths.migrateLegacyFilesIfNeeded(PERSISTENCE_FILE);
            Map<String, Boolean> loaded = ExtractedPositionsPersistence.load();
            extractedBlockPositions.clear();
            extractedBlockPositions.putAll(loaded);
            int legacyRemoved = purgeLegacyKeys();
            if (legacyRemoved > 0) {
                VentrysJob.LOGGER.info("Positions extraites : {} clé(s) legacy retirée(s)", legacyRemoved);
                requestExtractedPositionsSave();
            }
            VentrysJob.LOGGER.debug("Positions de bûches extraites chargées: {}", extractedBlockPositions.size());
        } catch (IOException | ClassNotFoundException e) {
            VentrysJob.LOGGER.warn("Impossible de charger les positions extraites: {}", e.getMessage());
        }
    }

    public static void shutdownExtractedPositionsSaver() {
        extractedPositionsSaveRequested.set(false);
        extractedPositionsSaveExecutor.shutdown();
        try {
            if (!extractedPositionsSaveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                extractedPositionsSaveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            extractedPositionsSaveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /** Retire les marqueurs obsolètes (air, clés legacy) dans les chunks chargés. */
    public static int purgeStaleEntries(ServerLevel level, int maxChecks) {
        if (maxChecks <= 0 || extractedBlockPositions.isEmpty()) {
            return 0;
        }

        String dimensionPrefix = level.dimension().location().toString() + "|";
        String[] keys = extractedBlockPositions.keySet().toArray(new String[0]);
        if (keys.length == 0) {
            return 0;
        }

        int removed = 0;
        int checked = 0;
        int start = Math.floorMod(stalePurgeCursor, keys.length);

        for (int i = 0; i < keys.length && checked < maxChecks; i++) {
            String key = keys[(start + i) % keys.length];
            checked++;

            if (!key.startsWith(dimensionPrefix)) {
                if (!key.contains("|")) {
                    extractedBlockPositions.remove(key);
                    removed++;
                }
                continue;
            }

            BlockPos pos = parsePositionKey(key);
            if (pos == null || !level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                extractedBlockPositions.remove(key);
                removed++;
            }
        }

        stalePurgeCursor = (start + checked) % Math.max(keys.length, 1);
        if (removed > 0) {
            requestExtractedPositionsSave();
        }
        return removed;
    }

    public static int size() {
        return extractedBlockPositions.size();
    }

    private static int purgeLegacyKeys() {
        int removed = 0;
        Iterator<String> it = extractedBlockPositions.keySet().iterator();
        while (it.hasNext()) {
            if (!it.next().contains("|")) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }

    private static BlockPos parsePositionKey(String key) {
        int separator = key.indexOf('|');
        if (separator < 0 || separator >= key.length() - 1) {
            return null;
        }
        String posPart = key.substring(separator + 1);
        String[] parts = posPart.split(LEGACY_POSITION_SEPARATOR);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static String getPositionKey(Level level, BlockPos pos) {
        String dimensionId = level.dimension().location().toString();
        return dimensionId + "|" + pos.getX() + LEGACY_POSITION_SEPARATOR + pos.getY() + LEGACY_POSITION_SEPARATOR + pos.getZ();
    }

    private static String getLegacyPositionKey(BlockPos pos) {
        return pos.getX() + LEGACY_POSITION_SEPARATOR + pos.getY() + LEGACY_POSITION_SEPARATOR + pos.getZ();
    }

    private static void requestExtractedPositionsSave() {
        extractedPositionsSaveRequested.set(true);
        if (!extractedPositionsSaveInFlight.compareAndSet(false, true)) {
            return;
        }

        extractedPositionsSaveExecutor.submit(() -> {
            try {
                while (extractedPositionsSaveRequested.getAndSet(false)) {
                    saveExtractedPositions();
                }
            } finally {
                extractedPositionsSaveInFlight.set(false);
                if (extractedPositionsSaveRequested.get()) {
                    requestExtractedPositionsSave();
                }
            }
        });
    }

    private static final class ExtractedPositionsPersistence {
        private ExtractedPositionsPersistence() {
        }

        private static void save(Map<String, Boolean> positions) throws IOException {
            com.ventrys.job.util.VentrysJobDataPaths.ensureDataDirectory();
            Path dataDir = com.ventrys.job.util.VentrysJobDataPaths.getDataDirectory();

            Path file = dataDir.resolve(PERSISTENCE_FILE);
            Path tempFile = dataDir.resolve(PERSISTENCE_FILE + ".tmp");
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                oos.writeObject(new HashMap<>(positions));
            }
            if (Files.exists(file)) {
                Files.delete(file);
            }
            Files.move(tempFile, file);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Boolean> load() throws IOException, ClassNotFoundException {
            Path file = com.ventrys.job.util.VentrysJobDataPaths.getDataDirectory().resolve(PERSISTENCE_FILE);
            if (!Files.exists(file)) {
                return new HashMap<>();
            }
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                return (Map<String, Boolean>) ois.readObject();
            }
        }
    }
}
