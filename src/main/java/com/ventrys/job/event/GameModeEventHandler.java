package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.JobActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class GameModeEventHandler {

    // Nettoyer les données quand un joueur se connecte
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur connecté: {} - Nettoyage des progressions", 
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup(); // Nettoyage forcé global
        }
    }
    
    // Nettoyer les données quand un joueur respawn
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur respawn: {} - Nettoyage des progressions", 
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup(); // Nettoyage forcé global
        }
    }
    
    // Nettoyer les données quand un joueur change de dimension
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur change de dimension: {} - Nettoyage des progressions", 
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup(); // Nettoyage forcé global
        }
    }
    
    // Nettoyer les données quand un joueur se déconnecte
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur déconnecté: {} - Nettoyage des progressions", 
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup(); // Nettoyage forcé global
        }
    }
    
    // Nettoyer les données quand un joueur change de mode de jeu
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur {} change de mode de jeu - Nettoyage des progressions", 
                player.getName().getString());
            
            // Toujours nettoyer les progressions lors d'un changement de mode
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup(); // Nettoyage forcé global
        }
    }
}
