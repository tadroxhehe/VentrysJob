package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.JobActions;
import com.ventrys.job.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public class GameModeEventHandler {

    private static void syncJobSoon(ServerPlayer player, int... delaysTicks) {
        NetworkHandler.syncPlayerJob(player);
        if (player.getServer() == null) {
            return;
        }
        for (int delay : delaysTicks) {
            final int d = delay;
            player.getServer().tell(new net.minecraft.server.TickTask(
                    player.getServer().getTickCount() + d,
                    () -> {
                        if (player.isAlive() || player.connection != null) {
                            NetworkHandler.syncPlayerJob(player);
                        }
                    }));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur connecté: {} - Nettoyage des progressions",
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup();
            // Sync immédiat + retries (client parfois pas prêt / packet perdu)
            syncJobSoon(player, 5, 20, 60, 100);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur respawn: {} - Nettoyage des progressions",
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup();
            syncJobSoon(player, 5, 20);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur change de dimension: {} - Nettoyage des progressions",
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup();
            syncJobSoon(player, 5, 20);
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur déconnecté: {} - Nettoyage des progressions",
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup();
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangeGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            VentrysJob.LOGGER.debug("Joueur {} change de mode de jeu - Nettoyage des progressions",
                player.getName().getString());
            JobActions.resetPlayerProgress(player);
            JobActions.forceCleanup();
            NetworkHandler.syncPlayerJob(player);
        }
    }
}
