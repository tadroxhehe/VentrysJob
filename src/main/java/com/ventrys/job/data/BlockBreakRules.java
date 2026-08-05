package com.ventrys.job.data;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

/**
 * Règles métier pour la casse de blocs (miroir de {@link BlockPlacementRules}).
 * Indépendant des perms LuckPerms / OP — seul le métier VentrysJob compte.
 */
public final class BlockBreakRules {

    /** Vitesse max de casse sur construction (hors meubles) pour bâtisseur. */
    public static final float DECORATIVE_MAX_BREAK_SPEED = 4.0f;

    /** Vitesse mini garantie avec maillet (main gauche OK). */
    public static final float MALLET_MIN_BREAK_SPEED = 2.5f;

    private BlockBreakRules() {
    }

    /**
     * Construction / décor structurel (pas meuble, pas terrain naturel) — réservé bâtisseur + maillet.
     */
    public static boolean isDecorativeBuildingBlock(BlockState state) {
        return requiresBatisseur(state);
    }

    /**
     * True = bloc de construction / décor : bâtisseur + maillet requis.
     * False = terrain naturel, granite mines, meuble, air, ou protégé staff (autre règle).
     */
    public static boolean requiresBatisseur(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (FurnitureAccess.isFurniture(state)) {
            return false;
        }
        if (BlockPlacementRules.isOuvrierMineSupportBlock(state)) {
            return false;
        }
        if (isNarrationTextBlock(state)) {
            return false;
        }
        if (isStaffProtectedUnbreakable(state)) {
            return false;
        }
        // Granite mines : ouvrier + pioche (pas bâtisseur)
        if (JobActions.isMineGranite(state)) {
            return false;
        }
        // Cultures paysan : récolte fourche (pas bâtisseur)
        if (CropGrowthConfig.isConfiguredCrop(state.getBlock())) {
            return false;
        }
        if (isNaturalTerrain(state)) {
            return false;
        }
        return true;
    }

    /**
     * Terrain / nature cassable par tout le monde (pas réservé bâtisseur).
     * Les filons / granite mine restent gérés à part via {@link JobActions}.
     */
    public static boolean isNaturalTerrain(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();

        if (block == Blocks.DIRT
                || block == Blocks.GRASS_BLOCK
                || block == Blocks.COARSE_DIRT
                || block == Blocks.PODZOL
                || block == Blocks.MYCELIUM
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.DIRT_PATH
                || block == Blocks.FARMLAND
                || block == Blocks.SAND
                || block == Blocks.RED_SAND
                || block == Blocks.GRAVEL
                || block == Blocks.CLAY
                || block == Blocks.SNOW
                || block == Blocks.SNOW_BLOCK
                || block == Blocks.MOSS_BLOCK
                || block == Blocks.MOSS_CARPET
                || block == Blocks.SOUL_SAND
                || block == Blocks.SOUL_SOIL) {
            return true;
        }

        Material mat = state.getMaterial();
        // Soft terrain only — PAS Material.STONE (sinon murs / déco pierre cassables par tous)
        return mat == Material.DIRT
                || mat == Material.GRASS
                || mat == Material.SAND
                || mat == Material.CLAY
                || mat == Material.TOP_SNOW
                || mat == Material.SNOW
                || mat == Material.MOSS;
    }

    /**
     * Blocs staff (portails warp, etc.) : indestructibles hors créatif,
     * même pour un bâtisseur avec maillet.
     */
    public static boolean isStaffProtectedUnbreakable(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        var id = state.getBlock().getRegistryName();
        return id != null
                && "ventryschat".equals(id.getNamespace())
                && "warp_portal_block".equals(id.getPath());
    }

    /** Texte HRP VentrysChat : pose / casse libre, sans drop. */
    public static boolean isNarrationTextBlock(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        var id = state.getBlock().getRegistryName();
        return id != null
                && "ventryschat".equals(id.getNamespace())
                && "narration_text_block".equals(id.getPath());
    }

    /**
     * Client + serveur. Ne dépend pas de LuckPerms / permission-level.
     */
    public static boolean canPlayerBreakBlock(Player player, BlockState state) {
        if (player == null) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        if (isStaffProtectedUnbreakable(state)) {
            return false;
        }
        if (isNarrationTextBlock(state)) {
            return true;
        }
        if (FurnitureAccess.isFurniture(state)) {
            return true;
        }
        if (BlockPlacementRules.isOuvrierMineSupportBlock(state)) {
            if (JobPermissionService.isOuvrier(player)) {
                return true;
            }
            // Bâtisseur + maillet : planches / barrières / torches / poutres mine
            return JobPermissionService.isBatisseur(player) && MalletUsage.hasUsableMallet(player);
        }
        if (JobActions.isMineGranite(state)) {
            return JobActions.canOuvrierVanillaBreakGranite(player, state);
        }
        if (isNaturalTerrain(state)) {
            return true;
        }
        return JobPermissionService.isBatisseur(player) && MalletUsage.hasUsableMallet(player);
    }

    public static float capDecorativeBreakSpeed(float currentSpeed) {
        float boosted = Math.max(currentSpeed, MALLET_MIN_BREAK_SPEED);
        return Math.min(boosted, DECORATIVE_MAX_BREAK_SPEED);
    }

    /** Vitesse de casse bâtisseur + maillet (tous blocs non-meubles autorisés). */
    public static float malletBreakSpeed(float currentSpeed) {
        return Math.max(currentSpeed, MALLET_MIN_BREAK_SPEED);
    }
}
