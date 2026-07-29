package com.ventrys.job.util;

import net.minecraft.server.level.ServerLevel;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Temps de jeu synchronisé sur l'horloge de Paris, avec freeze hors session.
 *
 * <p>Fenêtre active (défreeze) : {@code 17:00} → {@code 02:00} (9 heures IRL).
 * Dans cette fenêtre, exactement {@code 24 h} de jeu (24000 ticks) s'écoulent.
 *
 * <pre>
 * Ratio = 24 h jeu / 9 h IRL = 8/3 ≈ 2,667 h jeu par heure IRL
 *       = 24000 ticks / 32400 s IRL ≈ 0,7407 tick / seconde IRL
 *
 * Progression t ∈ [0, 1] = secondes_depuis_17h / 32400
 * dayTime               = floor(t × 24000)  (0 = aube Minecraft)
 * </pre>
 *
 * <p>Hors fenêtre ({@code 02:00} → {@code 17:00}) : le temps reste figé en fin de cycle (aube).
 */
public class RealTimeManager {
    public static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");

    /** Début de la fenêtre (inclus) — 17h00 Paris. */
    public static final int WINDOW_START_HOUR = 17;
    /** Fin de la fenêtre (exclus) — 02h00 Paris. */
    public static final int WINDOW_END_HOUR = 2;

    /** 17h → 2h = 9 heures IRL. */
    public static final int WINDOW_HOURS = 9;
    public static final long WINDOW_SECONDS = WINDOW_HOURS * 3600L;

    /** Un jour Minecraft complet. */
    public static final long TICKS_PER_GAME_DAY = 24000L;
    public static final long TICKS_PER_GAME_HOUR = 1000L;

    private RealTimeManager() {
    }

    public static int getParisHour() {
        return ZonedDateTime.now(PARIS_ZONE).getHour();
    }

    /**
     * {@code true} si l'horloge Paris est dans {@code [17:00, 02:00)} (wrap minuit).
     */
    public static boolean isTimeRunning(ZonedDateTime parisTime) {
        if (parisTime == null) {
            parisTime = ZonedDateTime.now(PARIS_ZONE);
        }
        int hour = parisTime.getHour();
        return hour >= WINDOW_START_HOUR || hour < WINDOW_END_HOUR;
    }

    /**
     * Secondes écoulées depuis 17:00 dans la fenêtre (0 … 32400).
     * Hors fenêtre : -1.
     */
    public static long secondsIntoWindow(ZonedDateTime parisTime) {
        if (parisTime == null) {
            parisTime = ZonedDateTime.now(PARIS_ZONE);
        }
        if (!isTimeRunning(parisTime)) {
            return -1L;
        }
        int hour = parisTime.getHour();
        int minute = parisTime.getMinute();
        int second = parisTime.getSecond();
        long secondsOfHour = minute * 60L + second;
        if (hour >= WINDOW_START_HOUR) {
            // 17:00 → 24:00
            return (hour - WINDOW_START_HOUR) * 3600L + secondsOfHour;
        }
        // 00:00 → 02:00 : 7 h déjà écoulées (17→00) + heure actuelle
        return (hour + (24 - WINDOW_START_HOUR)) * 3600L + secondsOfHour;
    }

    /**
     * Progression [0, 1] dans la fenêtre ; 1.0 si hors fenêtre (figé en fin de jour).
     */
    public static double windowProgress(ZonedDateTime parisTime) {
        long seconds = secondsIntoWindow(parisTime);
        if (seconds < 0L) {
            return 1.0D;
        }
        if (seconds >= WINDOW_SECONDS) {
            return 1.0D;
        }
        return seconds / (double) WINDOW_SECONDS;
    }

    /**
     * Heure de jeu « horloge » 0–23 dérivée de la progression (00:00 au début de fenêtre).
     */
    public static int convertIRLToGameHour(int irlHour) {
        // Conservé pour les logs /ventrystime : approximation à l'heure pleine.
        ZonedDateTime sample = ZonedDateTime.now(PARIS_ZONE).withHour(irlHour).withMinute(0).withSecond(0).withNano(0);
        long ticks = getCurrentGameTime(sample) % TICKS_PER_GAME_DAY;
        if (ticks < 0L) {
            ticks += TICKS_PER_GAME_DAY;
        }
        // 0 ticks MC = 6h horloge ; on expose l'heure depuis minuit
        return (int) (((ticks / TICKS_PER_GAME_HOUR) + 6) % 24);
    }

    public static long gameHourToTicks(int gameHour) {
        int hoursSinceDawn = (gameHour - 6 + 24) % 24;
        return (long) hoursSinceDawn * TICKS_PER_GAME_HOUR;
    }

    public static long getCurrentGameTime() {
        return getCurrentGameTime(ZonedDateTime.now(PARIS_ZONE));
    }

    /**
     * Temps cible (ticks dans le cycle 24000).
     * <ul>
     *   <li>Dans la fenêtre : linéaire 0 → 24000 sur 9 h IRL (départ = aube MC).</li>
     *   <li>Hors fenêtre : figé à 0 (fin de cycle / aube — jour terminé).</li>
     * </ul>
     */
    public static long getCurrentGameTime(ZonedDateTime parisTime) {
        if (parisTime == null) {
            parisTime = ZonedDateTime.now(PARIS_ZONE);
        }
        long seconds = secondsIntoWindow(parisTime);
        if (seconds < 0L) {
            // Freeze hors session : fin du jour (= aube, 0 tick)
            return 0L;
        }
        if (seconds >= WINDOW_SECONDS) {
            return 0L;
        }
        // (s * 24000) / 32400 — exact en arithmétique entière
        return (seconds * TICKS_PER_GAME_DAY) / WINDOW_SECONDS;
    }

    /** Heures de jeu qui passent pour 1 heure IRL dans la fenêtre (24/9 = 8/3). */
    public static double gameHoursPerIrlHour() {
        return 24.0D / (double) WINDOW_HOURS;
    }

    public static void syncWorldTime(ServerLevel level) {
        syncWorldTime(level, ZonedDateTime.now(PARIS_ZONE));
    }

    public static void syncWorldTime(ServerLevel level, ZonedDateTime parisTime) {
        if (level == null) {
            return;
        }

        long targetGameTime = getCurrentGameTime(parisTime);
        long currentWorldTime = level.getDayTime();

        long normalizedTarget = targetGameTime % TICKS_PER_GAME_DAY;
        if (normalizedTarget < 0L) {
            normalizedTarget += TICKS_PER_GAME_DAY;
        }

        long normalizedCurrent = currentWorldTime % TICKS_PER_GAME_DAY;
        if (normalizedCurrent < 0L) {
            normalizedCurrent += TICKS_PER_GAME_DAY;
        }

        long difference = normalizedTarget - normalizedCurrent;
        if (difference > 12000L) {
            difference -= TICKS_PER_GAME_DAY;
        } else if (difference < -12000L) {
            difference += TICKS_PER_GAME_DAY;
        }

        if (difference != 0L) {
            long worldDays = (currentWorldTime / TICKS_PER_GAME_DAY) * TICKS_PER_GAME_DAY;
            long newTime = worldDays + normalizedTarget;
            if (normalizedTarget < normalizedCurrent && difference < -1000L) {
                newTime = worldDays + TICKS_PER_GAME_DAY + normalizedTarget;
            }
            level.setDayTime(newTime);
        }
    }

    public static String getGameTimeString() {
        ZonedDateTime paris = ZonedDateTime.now(PARIS_ZONE);
        long ticks = getCurrentGameTime(paris) % TICKS_PER_GAME_DAY;
        int hoursSinceDawn = (int) (ticks / TICKS_PER_GAME_HOUR);
        int gameHour = (hoursSinceDawn + 6) % 24;
        String state = isTimeRunning(paris) ? "actif" : "figé";
        return String.format("%02d:00 (%s) IRL %02d:%02d Paris", gameHour, state, paris.getHour(), paris.getMinute());
    }
}
