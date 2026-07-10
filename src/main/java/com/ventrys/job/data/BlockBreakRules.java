package com.ventrys.job.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Règles métier pour la casse de blocs (miroir de {@link BlockPlacementRules}).
 */
public final class BlockBreakRules {

    /** Vitesse max de casse sur blocs décoratifs (Westeros, etc.) — ~3–5 s selon dureté. */
    public static final float DECORATIVE_MAX_BREAK_SPEED = 0.08f;

    private BlockBreakRules() {
    }

    public static boolean isDecorativeBuildingBlock(BlockState state) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null) {
            return false;
        }
        String namespace = blockId.getNamespace();
        return "westerosblocks".equals(namespace) || "ventrys_blocs".equals(namespace);
    }

    public static boolean canPlayerBreakBlock(ServerPlayer player, BlockState state) {
        if (player.isCreative()) {
            return true;
        }
        if (!isDecorativeBuildingBlock(state)) {
            return true;
        }
        return JobPermissionService.isBatisseur(player) && MalletUsage.hasMalletInOffhand(player);
    }

    public static float capDecorativeBreakSpeed(float currentSpeed) {
        return Math.min(currentSpeed, DECORATIVE_MAX_BREAK_SPEED);
    }
}
