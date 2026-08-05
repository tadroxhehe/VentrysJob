package com.ventrys.job.network.packet;

import com.ventrys.job.data.PlayerJobData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sync métier joueur serveur → client (casse / pose bâtisseur côté client).
 */
public class SyncPlayerJobPacket {
    private final String jobId;

    public SyncPlayerJobPacket(String jobId) {
        this.jobId = jobId == null ? "" : jobId;
    }

    public SyncPlayerJobPacket(FriendlyByteBuf buf) {
        this.jobId = buf.readUtf(64);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(jobId, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                PlayerJobData.applySyncedJob(player.getUUID(), jobId.isEmpty() ? null : jobId);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
