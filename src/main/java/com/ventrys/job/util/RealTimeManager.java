package com.ventrys.job.util;

import net.minecraft.server.level.ServerLevel;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Gestionnaire de temps réel synchronisé avec l'heure de Paris.
 * Convertit l'heure IRL en heure de jeu selon les règles :
 * - 12h IRL = 6h en jeu
 * - 1h IRL = 0h en jeu
 */
public class RealTimeManager {
    private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
    private static final long TICKS_PER_GAME_HOUR = 1000L;
    private static final long SECONDS_PER_IRL_HOUR = 3600L;
    
    /**
     * Obtient l'heure actuelle de Paris en heures (0-23)
     */
    public static int getParisHour() {
        ZonedDateTime parisTime = ZonedDateTime.now(PARIS_ZONE);
        return parisTime.getHour();
    }
    
    /**
     * Convertit l'heure IRL de Paris en heure de jeu (0-23)
     * Règles :
     * - 12h IRL = 6h en jeu
     * - 1h IRL = 0h en jeu
     * 
     * Formule : heure_en_jeu = (heure_IRL - 1) % 24
     * Mais avec un décalage pour que 12h IRL = 6h en jeu
     */
    public static int convertIRLToGameHour(int irlHour) {
        // Conversion : 1h IRL = 0h jeu, 12h IRL = 6h jeu
        // À 1h28 IRL, on devrait être à environ minuit (0h) en jeu
        // Formule simple : (irlHour - 1) % 24 pour que 1h = 0h, 2h = 1h, etc.
        // Mais on veut que 12h IRL = 6h jeu, donc on ajuste
        
        // Si irlHour est 0 (minuit), on le traite comme 24h
        int adjustedHour = irlHour == 0 ? 24 : irlHour;
        
        // Conversion directe : 1h IRL = 0h jeu
        // Donc : gameHour = (irlHour - 1) % 24
        // Mais pour que 12h IRL = 6h jeu, on fait :
        // De 1h à 12h IRL : 0h à 6h en jeu (11h IRL pour 6h jeu)
        // Ratio : 6 heures jeu / 11 heures IRL = 6/11
        if (adjustedHour >= 1 && adjustedHour <= 12) {
            return (int) Math.round((adjustedHour - 1) * 6.0 / 11.0);
        } else {
            // De 13h à 24h IRL : 6h à 24h puis 0h en jeu
            // On a 12 heures IRL (13-24) pour 18 heures jeu (6-24, 0)
            // Ratio : 18 heures jeu / 12 heures IRL = 1.5
            int gameHour = 6 + (int) Math.round((adjustedHour - 12) * 1.5);
            return gameHour % 24;
        }
    }
    
    /**
     * Convertit l'heure de jeu en ticks Minecraft
     * @param gameHour L'heure de jeu (0-23) - heure depuis minuit
     * @return Le nombre de ticks correspondant
     * 
     * En Minecraft, le cycle commence à 6h (0 ticks) :
     * - 0 ticks = 6h (lever du soleil)
     * - 6000 ticks = 12h (midi)
     * - 12000 ticks = 18h (coucher)
     * - 18000 ticks = 0h (minuit)
     * 
     * Pour avoir gameHour visuellement (heure depuis minuit) :
     * - gameHour depuis minuit = (gameHour - 6 + 24) % 24 heures depuis le lever
     * - ticks = heures_depuis_lever * 1000
     * 
     * Exemples :
     * - 0h en jeu → 18h depuis le lever → 18000 ticks (minuit) ✓
     * - 6h en jeu → 0h depuis le lever → 0 ticks (lever) ✓
     * - 12h en jeu → 6h depuis le lever → 6000 ticks (midi) ✓
     * - 14h en jeu → 8h depuis le lever → 8000 ticks (14h après-midi) ✓
     * - 18h en jeu → 12h depuis le lever → 12000 ticks (coucher) ✓
     */
    public static long gameHourToTicks(int gameHour) {
        // Convertir l'heure depuis minuit en heures depuis le lever (6h)
        int hoursSinceDawn = (gameHour - 6 + 24) % 24;
        return (long) hoursSinceDawn * 1000L;
    }
    
    /**
     * Obtient le temps de jeu actuel basé sur l'heure IRL de Paris
     * @return Le temps en ticks Minecraft
     */
    public static long getCurrentGameTime() {
        return getCurrentGameTime(ZonedDateTime.now(PARIS_ZONE));
    }
    
    /**
     * Obtient le temps de jeu actuel basé sur une date/heure Paris déjà calculée.
     * Permet d'éviter de refaire des now() multiples pendant la boucle tick.
     */
    public static long getCurrentGameTime(ZonedDateTime parisTime) {
        if (parisTime == null) {
            parisTime = ZonedDateTime.now(PARIS_ZONE);
        }
        
        int parisHour = parisTime.getHour();
        int gameHour = convertIRLToGameHour(parisHour);
        int parisMinute = parisTime.getMinute();
        int parisSecond = parisTime.getSecond();
        
        // Convertir la progression IRL (minutes + secondes) en progression dans l'heure de jeu.
        // 3600 secondes IRL = 1000 ticks (1 heure de jeu)
        long secondsIntoHour = parisMinute * 60L + parisSecond;
        long ticksInCurrentHour = (secondsIntoHour * TICKS_PER_GAME_HOUR) / SECONDS_PER_IRL_HOUR;
        
        return gameHourToTicks(gameHour) + ticksInCurrentHour;
    }
    
    /**
     * Met à jour le temps du monde pour qu'il corresponde à l'heure IRL de Paris
     * @param level Le niveau serveur à mettre à jour
     */
    public static void syncWorldTime(ServerLevel level) {
        syncWorldTime(level, ZonedDateTime.now(PARIS_ZONE));
    }

    /**
     * Met à jour le temps du monde avec une heure Paris déjà calculée (évite un second {@code now()}).
     */
    public static void syncWorldTime(ServerLevel level, ZonedDateTime parisTime) {
        if (level == null) {
            return;
        }
        
        long targetGameTime = getCurrentGameTime(parisTime);
        long currentWorldTime = level.getDayTime();
        
        // Normaliser le temps cible dans le cycle de 24000 ticks
        long normalizedTarget = targetGameTime % 24000;
        if (normalizedTarget < 0) {
            normalizedTarget += 24000;
        }
        
        long normalizedCurrent = currentWorldTime % 24000;
        if (normalizedCurrent < 0) {
            normalizedCurrent += 24000;
        }
        
        // Calculer la différence (chemin le plus court dans le cycle)
        long difference = normalizedTarget - normalizedCurrent;
        if (difference > 12000) {
            difference -= 24000;
        } else if (difference < -12000) {
            difference += 24000;
        }
        
        // Toujours synchroniser si la différence est non nulle
        // Cela empêche l'avancement vanilla et garantit la stabilité
        if (difference != 0) {
            // Utiliser le temps absolu du monde mais avec le cycle normalisé
            // On préserve la partie haute du temps (jours écoulés) et on remplace seulement le cycle
            long worldDays = (currentWorldTime / 24000) * 24000;
            long newTime = worldDays + normalizedTarget;
            
            // Si on passe à un nouveau cycle, ajuster
            if (normalizedTarget < normalizedCurrent && difference < -1000) {
                // On recule dans le cycle, donc on est probablement passé à un nouveau jour
                newTime = worldDays + 24000 + normalizedTarget;
            }
            
            level.setDayTime(newTime);
        }
    }
    
    /**
     * Obtient une représentation textuelle de l'heure de jeu actuelle
     */
    public static String getGameTimeString() {
        int parisHour = getParisHour();
        int parisMinute = ZonedDateTime.now(PARIS_ZONE).getMinute();
        int gameHour = convertIRLToGameHour(parisHour);
        
        return String.format("%02d:%02d (IRL: %02d:%02d Paris)", gameHour, 0, parisHour, parisMinute);
    }
}

