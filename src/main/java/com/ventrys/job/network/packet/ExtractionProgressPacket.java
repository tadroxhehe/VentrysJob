package com.ventrys.job.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Packet pour synchroniser la progression d'extraction côté client
 */
public class ExtractionProgressPacket {
    private final String key;
    private final int current;
    private final int max;
    private final String message;
    
    public ExtractionProgressPacket(String key, int current, int max, String message) {
        this.key = key;
        this.current = current;
        this.max = max;
        this.message = message;
    }
    
    public ExtractionProgressPacket(FriendlyByteBuf buf) {
        this.key = buf.readUtf(32767);
        this.current = buf.readInt();
        this.max = buf.readInt();
        this.message = buf.readUtf(32767);
    }
    
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.key);
        buf.writeInt(this.current);
        buf.writeInt(this.max);
        buf.writeUtf(this.message);
    }
    
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            // Mettre à jour l'overlay côté client uniquement
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // Utiliser le nom complet pour éviter le chargement côté serveur
                if (message == null || message.isEmpty()) {
                    // Supprimer la progression
                    com.ventrys.job.client.render.ExtractionProgressOverlay.removeProgress(key);
                } else {
                    // Mettre à jour la progression
                    com.ventrys.job.client.render.ExtractionProgressOverlay.updateProgress(key, current, max, message);
                }
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}

