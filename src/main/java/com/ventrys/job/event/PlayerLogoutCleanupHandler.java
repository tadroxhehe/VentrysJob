package com.ventrys.job.event;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.data.JobActions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = VentrysJob.MOD_ID)
public final class PlayerLogoutCleanupHandler {

    private PlayerLogoutCleanupHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            JobActions.clearAllPlayerData(player);
        }
    }
}
