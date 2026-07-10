package com.ventrys.job.data;

import net.minecraft.world.entity.player.Player;
import com.ventrys.job.VentrysJob;
import com.ventrys.job.util.VentrysJobDataPaths;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerJobData {
    // Stockage thread-safe des métiers des joueurs avec persistance
    private static final Map<UUID, String> playerJobs = new ConcurrentHashMap<>();
    
    // Fichier de persistance pour les métiers
    private static final String JOBS_PERSISTENCE_FILE = "ventrysjob_player_jobs.dat";
    
    // Système de sauvegarde différée pour optimiser les performances
    private static final AtomicInteger saveCounter = new AtomicInteger(0);
    private static final int SAVE_INTERVAL = 20; // Sauvegarder toutes les 20 modifications (environ 1 seconde en jeu)
    private static final AtomicBoolean needsSave = new AtomicBoolean(false);
    private static final AtomicLong dirtySeq = new AtomicLong(0);
    private static final AtomicBoolean saveInFlight = new AtomicBoolean(false);
    private static final Object saveLock = new Object();
    
    // ExecutorService pour gérer les sauvegardes de manière thread-safe
    private static final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VentrysJob-SaveThread");
        t.setDaemon(true); // Thread daemon pour ne pas bloquer l'arrêt de la JVM
        return t;
    });
    
    /**
     * Obtient le métier actuel d'un joueur
     */
    public static String getPlayerJob(Player player) {
        if (player == null) return null;
        String job = playerJobs.get(player.getUUID());
        if ("tanneur".equals(job)) {
            playerJobs.put(player.getUUID(), "couturier");
            markForSave();
            return "couturier";
        }
        return job;
    }
    
    /**
     * Définit le métier d'un joueur
     */
    public static void setPlayerJob(Player player, String jobId) {
        if (player == null || jobId == null) return;
        playerJobs.put(player.getUUID(), jobId);
        markForSave(); // Marquer pour sauvegarde différée
    }
    
    /**
     * Retire le métier d'un joueur
     */
    public static void removePlayerJob(Player player) {
        if (player == null) return;
        playerJobs.remove(player.getUUID());
        markForSave(); // Marquer pour sauvegarde différée
    }
    
    /**
     * Marque les données comme nécessitant une sauvegarde
     */
    private static void markForSave() {
        needsSave.set(true);
        dirtySeq.incrementAndGet();
        int current = saveCounter.incrementAndGet();
        // Sauvegarder périodiquement ou si beaucoup de modifications
        if (current >= SAVE_INTERVAL) {
            savePlayerJobsPeriodically();
            saveCounter.set(0);
        }
    }
    
    /**
     * Sauvegarde périodique des métiers (optimisée)
     * Utilise un ExecutorService avec un seul thread pour éviter de bloquer le thread serveur
     * et éviter de créer trop de threads simultanés
     */
    private static void savePlayerJobsPeriodically() {
        if (!needsSave.get()) {
            return;
        }

        if (!saveInFlight.compareAndSet(false, true)) {
            return; // une sauvegarde est déjà en vol
        }

        long seqAtStart = dirtySeq.get();

        saveExecutor.submit(() -> {
            try {
                synchronized (saveLock) {
                    savePlayerJobs();
                }
            } catch (Exception e) {
                VentrysJob.LOGGER.error("Erreur lors de la sauvegarde des métiers: {}", e.getMessage());
            } finally {
                saveInFlight.set(false);
                // Ne clear la "dirty state" que si aucun changement n'a eu lieu pendant la sauvegarde.
                if (dirtySeq.get() == seqAtStart) {
                    needsSave.set(false);
                } else {
                    // Modifs pendant la save: on reprogramme pour ne pas perdre l'état.
                    savePlayerJobsPeriodically();
                }
            }
        });
    }
    
    /**
     * Arrête l'ExecutorService (appelé lors de l'arrêt du serveur)
     */
    public static void shutdown() {
        try {
            saveExecutor.shutdown();
            if (!saveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                saveExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            saveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Vérifie si un joueur peut accéder à une table de métier
     */
    public static boolean canAccessJobTable(Player player, String requiredJob) {
        String playerJob = getPlayerJob(player);
        return requiredJob.equals(playerJob);
    }
    
    /**
     * Sauvegarde les métiers des joueurs dans un fichier
     */
    public static void savePlayerJobs() {
        try {
            VentrysJobDataPaths.ensureDataDirectory();
            Path dataDir = VentrysJobDataPaths.getDataDirectory();
            
            Path file = dataDir.resolve(JOBS_PERSISTENCE_FILE);
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
                oos.writeObject(new ConcurrentHashMap<>(playerJobs));
            }
        } catch (IOException e) {
            VentrysJob.LOGGER.warn("Impossible de sauvegarder les métiers joueurs: {}", e.getMessage());
        }
    }
    
    /**
     * Charge les métiers des joueurs depuis un fichier
     */
    @SuppressWarnings("unchecked")
    public static void loadPlayerJobs() {
        try {
            VentrysJobDataPaths.migrateLegacyFilesIfNeeded(JOBS_PERSISTENCE_FILE);
            Path dataDir = VentrysJobDataPaths.getDataDirectory();
            Path file = dataDir.resolve(JOBS_PERSISTENCE_FILE);
            
            if (Files.exists(file)) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                    Map<UUID, String> loaded = (Map<UUID, String>) ois.readObject();
                    playerJobs.clear();
                    playerJobs.putAll(loaded);
                    // Fusion tanneur → couturier (même métier dans jobs.json)
                    boolean migratedTanneur = false;
                    for (Map.Entry<UUID, String> e : new ArrayList<>(playerJobs.entrySet())) {
                        if ("tanneur".equals(e.getValue())) {
                            playerJobs.put(e.getKey(), "couturier");
                            migratedTanneur = true;
                        }
                    }
                    if (migratedTanneur) {
                        VentrysJob.LOGGER.warn("Migration métiers joueurs : « tanneur » → « couturier »");
                        needsSave.set(true);
                    }
                    VentrysJob.LOGGER.debug("Métiers joueurs chargés: {} entrées", playerJobs.size());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            VentrysJob.LOGGER.warn("Impossible de charger les métiers joueurs: {}", e.getMessage());
        }
    }
    
    /**
     * Force une sauvegarde immédiate (utilisé lors de l'arrêt du serveur)
     * Cette méthode bloque car elle est appelée lors de l'arrêt du serveur
     */
    public static void forceSave() {
        // Sauvegarde synchrone lors de l'arrêt du serveur.
        synchronized (saveLock) {
            if (!needsSave.get()) {
                return;
            }
            savePlayerJobs();
            needsSave.set(false);
            saveCounter.set(0);
        }
    }
}