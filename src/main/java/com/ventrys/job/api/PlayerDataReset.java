package com.ventrys.job.api;

import com.ventrys.job.data.JobActions;
import com.ventrys.job.data.PlayerJobData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Point d'entrée pour réinitialiser les données VentrysJob d'un joueur (API modpack).
 */
public final class PlayerDataReset {

    private PlayerDataReset() {
    }

    public static void reset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerJobData.removePlayerJob(player);
        JobActions.clearAllPlayerData(player);
        JobActions.clearPlayerProgressions(player.getUUID().toString());
        PlayerJobData.savePlayerJobs();
    }
}
