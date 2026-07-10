package com.ventrys.job.data;

import com.ventrys.job.VentrysJob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Service centralisant les règles simples de permissions métiers.
 */
public final class JobPermissionService {

    private JobPermissionService() {
    }

    public static String getJob(Player player) {
        return PlayerJobData.getPlayerJob(player);
    }

    public static boolean isJob(Player player, String jobId) {
        return jobId.equals(getJob(player));
    }

    public static boolean isPaysan(Player player) {
        return isJob(player, "paysan");
    }

    public static boolean isOuvrier(Player player) {
        return isJob(player, "ouvrier");
    }

    public static boolean isBatisseur(Player player) {
        return isJob(player, "batisseur");
    }

    public static boolean isVentrysJobBlock(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && VentrysJob.MOD_ID.equals(key.getNamespace());
    }

    /**
     * Blocs VentrysJob exemptés des règles métier de casse/pose (tables, fours, meule…).
     * Exclut les cultures et blocs extractibles (minerais, filons, etc.).
     */
    public static boolean isUnrestrictedVentrysJobBlock(BlockState state, BlockPos pos) {
        if (!isVentrysJobBlock(state)) {
            return false;
        }
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            return false;
        }
        return !JobActions.isAnyExtractableBlock(state, pos);
    }
}
