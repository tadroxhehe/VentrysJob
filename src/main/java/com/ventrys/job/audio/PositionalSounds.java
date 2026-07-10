package com.ventrys.job.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;

/**
 * Sons positionnels : uniquement les joueurs dans {@code maxDistance} (sphère) reçoivent le paquet.
 * <p>
 * <strong>Important :</strong> {@link ServerLevel#playSound(net.minecraft.world.entity.player.Player, double, double, double, SoundEvent, SoundSource, float, float)}
 * utilise le joueur passé comme <em>exception</em> du broadcast : ne pas l’appeler une fois par destinataire
 * avec ce joueur en premier argument, sinon personne n’entend le son. On envoie donc un
 * {@link ClientboundSoundPacket} explicite aux joueurs à portée.
 * <p>
 * Les joueurs sont d’abord filtrés par AABB puis par distance au carré.
 */
public final class PositionalSounds {

    private PositionalSounds() {}

    /**
     * Table de métier : zone utile ~5–10 blocs — on cappe à 10 blocs pour éviter d’entendre trop loin.
     */
    public static final double CRAFT_SOUND_MAX_DISTANCE = 10.0;

    /** Vase : interaction locale un peu plus courte qu’une table. */
    public static final double VASE_SOUND_MAX_DISTANCE = 8.0;

    /** Extractions (mine, bûcheron, scie, burin, pelle, récolte, etc.). */
    public static final double EXTRACTION_SOUND_MAX_DISTANCE = 12.0;

    public static void playNearBlock(ServerLevel level, BlockPos pos, SoundEvent sound,
                                     double maxDistance, float volume, float pitch) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        double maxSq = maxDistance * maxDistance;
        double r = maxDistance;
        AABB box = new AABB(cx - r, cy - r, cz - r, cx + r, cy + r, cz + r);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (p.distanceToSqr(cx, cy, cz) <= maxSq) {
                p.connection.send(new ClientboundSoundPacket(sound, SoundSource.BLOCKS, cx, cy, cz, volume, pitch));
            }
        }
    }
}
