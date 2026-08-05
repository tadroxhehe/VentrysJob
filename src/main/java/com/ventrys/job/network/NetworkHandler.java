package com.ventrys.job.network;

import com.ventrys.job.VentrysJob;
import com.ventrys.job.network.packet.CraftJobRecipePacket;
import com.ventrys.job.network.packet.ExtractionProgressPacket;
import com.ventrys.job.network.packet.SyncPlayerJobPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(VentrysJob.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.messageBuilder(CraftJobRecipePacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
            .decoder(CraftJobRecipePacket::new)
            .encoder(CraftJobRecipePacket::toBytes)
            .consumer(CraftJobRecipePacket::handle)
            .add();
        
        INSTANCE.messageBuilder(ExtractionProgressPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(ExtractionProgressPacket::new)
            .encoder(ExtractionProgressPacket::toBytes)
            .consumer(ExtractionProgressPacket::handle)
            .add();

        INSTANCE.messageBuilder(SyncPlayerJobPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
            .decoder(SyncPlayerJobPacket::new)
            .encoder(SyncPlayerJobPacket::toBytes)
            .consumer(SyncPlayerJobPacket::handle)
            .add();

        VentrysJob.LOGGER.debug("Réseau VentrysJob enregistré");
    }

    public static void syncPlayerJob(ServerPlayer player) {
        if (player == null) {
            return;
        }
        String job = com.ventrys.job.data.PlayerJobData.getPlayerJob(player);
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncPlayerJobPacket(job == null ? "" : job));
    }
}
