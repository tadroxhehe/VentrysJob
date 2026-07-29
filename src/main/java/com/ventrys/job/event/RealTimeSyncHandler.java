package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.command.TimeDebugCommand;
import com.ventrys.job.util.RealTimeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

@Mod.EventBusSubscriber(modid = com.ventrys.job.VentrysJob.MOD_ID)
public class RealTimeSyncHandler {
    
    private static int tickCounter = 0;
    private static final int LOG_INTERVAL = 100; // Logs toutes les 5 secondes (100 ticks)
    private static final int FORCE_SYNC_INTERVAL_TICKS = 36000; // 30 minutes = 36000 ticks (30 * 60 * 20)
    private static final int MAX_DRIFT_TICKS = 2; // Tolérance maximale de dérive avant correction
    private static final int TIME_CACHE_TICKS = 20; // Cache le temps IRL pendant 1 seconde (20 ticks)
    
    // Cache pour éviter les calculs répétés (optimisation multijoueur)
    private static long lastTargetTicks = -1;
    private static int lastIRLMinute = -1;
    private static int lastIRLSecond = -1; // Cache aussi la seconde pour optimiser
    private static ServerLevel overworldLevel = null;
    private static long lastForceSyncTick = 0; // Dernier tick où on a forcé une synchronisation
    private static long lastSyncedWorldTime = -1; // Dernier temps du monde synchronisé
    private static int lastTimeCalculationTick = -1; // Dernier tick où on a calculé le temps IRL
    private static ZonedDateTime cachedParisTime = null; // Cache du temps Paris

    private static boolean daylightCycleRuleSaved = false;
    private static boolean daylightCycleOriginalValue = true;
    private static boolean daylightCycleDisabled = false;
    
    private static void tryDisableDaylightCycle(MinecraftServer server, ServerLevel overworld) {
        if (daylightCycleDisabled) {
            return;
        }
        if (server == null || overworld == null) {
            return;
        }
        
        try {
            GameRules gameRules = overworld.getGameRules();
            daylightCycleOriginalValue = gameRules.getRule(GameRules.RULE_DAYLIGHT).get();
            daylightCycleRuleSaved = true;
            gameRules.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
            daylightCycleDisabled = true;
            VentrysJob.LOGGER.debug("[TIME SYNC] doDaylightCycle désactivé (était {})", daylightCycleOriginalValue);
        } catch (Exception e) {
            VentrysJob.LOGGER.warn("[TIME SYNC] Failed to disable doDaylightCycle: {}", e.getMessage());
        }
    }
    
    public static void restoreDaylightCycle(MinecraftServer server) {
        if (!daylightCycleRuleSaved || server == null || daylightCycleDisabled == false) {
            return;
        }
        
        try {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylightCycleOriginalValue, server);
            daylightCycleDisabled = false;
            VentrysJob.LOGGER.debug("[TIME SYNC] doDaylightCycle restauré (→ {})", daylightCycleOriginalValue);
        } catch (Exception e) {
            VentrysJob.LOGGER.warn("[TIME SYNC] Failed to restore doDaylightCycle: {}", e.getMessage());
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Ne traiter que pour les ticks de fin (après que le temps vanilla ait avancé)
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        tickCounter++;
        
        // Utiliser ServerLifecycleHooks pour obtenir le serveur (fonctionne en solo et multi)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        // Optimisation : ne synchroniser que l'overworld (le temps est partagé entre dimensions)
        if (overworldLevel == null || overworldLevel.isClientSide()) {
            overworldLevel = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        }
        
        if (overworldLevel == null) {
            return;
        }

        // Désactiver le daylight cycle vanilla (1 seule fois) pour éviter le jitter.
        tryDisableDaylightCycle(server, overworldLevel);
        
        // Optimisation multijoueur : Cache le calcul du temps IRL
        // On ne recalcule que toutes les secondes (20 ticks) ou si nécessaire
        ZonedDateTime parisTime;
        int currentIRLMinute;
        int currentIRLSecond;
        long targetTicks;
        
        boolean needTimeRecalculation = (tickCounter - lastTimeCalculationTick) >= TIME_CACHE_TICKS;
        boolean didRecalc = needTimeRecalculation || cachedParisTime == null;
        if (didRecalc) {
            parisTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
            cachedParisTime = parisTime;
            currentIRLMinute = parisTime.getMinute();
            currentIRLSecond = parisTime.getSecond();
            targetTicks = RealTimeManager.getCurrentGameTime(parisTime);
            lastTimeCalculationTick = tickCounter;
        } else {
            // Utiliser le cache
            parisTime = cachedParisTime;
            currentIRLMinute = lastIRLMinute;
            currentIRLSecond = lastIRLSecond;
            targetTicks = lastTargetTicks;
        }
        
        // Obtenir le temps actuel du monde (opération légère)
        long currentWorldTime = overworldLevel.getDayTime();
        
        // Normaliser les deux temps dans le cycle de 24000 ticks
        long normalizedTarget = targetTicks % 24000;
        if (normalizedTarget < 0) {
            normalizedTarget += 24000;
        }
        
        long normalizedCurrent = currentWorldTime % 24000;
        if (normalizedCurrent < 0) {
            normalizedCurrent += 24000;
        }
        
        // Calculer la différence (chemin le plus court dans le cycle)
        long diff = normalizedTarget - normalizedCurrent;
        if (diff > 12000) {
            diff -= 24000;
        } else if (diff < -12000) {
            diff += 24000;
        }
        
        // Décider si on doit synchroniser
        // Capturer l'ancien état pour détecter un changement de cible
        long previousTargetTicks = lastTargetTicks;
        
        boolean forceSyncNeeded = (tickCounter - lastForceSyncTick) >= FORCE_SYNC_INTERVAL_TICKS;
        
        // Synchro majoritairement quand la cible change (minute/secondes)
        // ou quand la dérive dépasse une petite tolérance.
        boolean targetChanged = didRecalc && previousTargetTicks != targetTicks;
        boolean driftTooLarge = Math.abs(diff) > MAX_DRIFT_TICKS;
        boolean isFirstSync = lastSyncedWorldTime == -1;
        
        // Respect du mode "smooth_intervals": éviter de synchroniser à chaque tick
        boolean shouldSync = isFirstSync || targetChanged || driftTooLarge || forceSyncNeeded;
        
        // Synchroniser si nécessaire
        if (shouldSync) {
            long beforeTicks = currentWorldTime;
            
            // Vérifier si le temps a changé depuis notre dernière vérification (avancement vanilla)
            if (lastSyncedWorldTime != -1 && currentWorldTime != lastSyncedWorldTime) {
                // Le temps vanilla a avancé, on doit le corriger
                long vanillaAdvancement = currentWorldTime - lastSyncedWorldTime;
                if (vanillaAdvancement > 0 && vanillaAdvancement < 10) {
                    // Le temps a avancé normalement (1 tick), on le corrige
                    VentrysJob.LOGGER.debug("[TIME SYNC] Temps vanilla avancé de {} ticks, correction nécessaire", vanillaAdvancement);
                }
            }
            
            // Appliquer la synchronisation
            RealTimeManager.syncWorldTime(overworldLevel, parisTime);
            
            // Vérifier le résultat
            long afterTicks = overworldLevel.getDayTime();
            long normalizedAfter = afterTicks % 24000;
            if (normalizedAfter < 0) {
                normalizedAfter += 24000;
            }
            
            // Recalculer la différence après synchronisation
            long diffAfter = normalizedTarget - normalizedAfter;
            if (diffAfter > 12000) {
                diffAfter -= 24000;
            } else if (diffAfter < -12000) {
                diffAfter += 24000;
            }
            
            // Mettre à jour les caches
            lastSyncedWorldTime = afterTicks;
            
            if (forceSyncNeeded) {
                lastForceSyncTick = tickCounter;
            }
            
            // Logs détaillés : évite tout calcul si ni niveau DEBUG ni /ventrystime debug (perf tick serveur)
            if ((tickCounter % LOG_INTERVAL == 0 || TimeDebugCommand.isDebugEnabled() || Math.abs(diffAfter) > 5)
                && (VentrysJob.LOGGER.isDebugEnabled() || TimeDebugCommand.isDebugEnabled())) {
                // S'assurer d'avoir le temps à jour pour les logs
                if (!didRecalc) {
                    parisTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
                    cachedParisTime = parisTime;
                }
                int irlHour = parisTime.getHour();
                int gameHour = RealTimeManager.convertIRLToGameHour(irlHour);
                String windowState = RealTimeManager.isTimeRunning(parisTime) ? "actif" : "fige";
                
                // Calculer phase jour/nuit
                String phase = "jour";
                if (normalizedAfter >= 12000 && normalizedAfter < 18000) {
                    phase = "nuit";
                } else if (normalizedAfter >= 18000) {
                    phase = "aube";
                } else if (normalizedAfter >= 6000) {
                    phase = "soir";
                }
                
                VentrysJob.LOGGER.debug("[TIME SYNC] IRL: {}:{} ({}), cible jeu: {}h ({} ticks), actuel: {} ticks ({}h, {}), dérive: {} ticks, Δsync: {} ticks",
                    String.format("%02d", irlHour), String.format("%02d", currentIRLMinute), windowState,
                    gameHour, normalizedTarget, 
                    normalizedAfter, (normalizedAfter / 1000), phase,
                    diffAfter, afterTicks - beforeTicks);
            }
        } else {
            // Rien: on conserve lastSyncedWorldTime comme "dernier setDayTime".
        }
        
        // Mettre à jour les caches même si on ne synchronise pas,
        // pour que les ticks cibles ne restent pas à une valeur obsolète (-1 au démarrage).
        if (didRecalc) {
            lastIRLMinute = currentIRLMinute;
            lastIRLSecond = currentIRLSecond;
            lastTargetTicks = targetTicks;
        }
    }
}

