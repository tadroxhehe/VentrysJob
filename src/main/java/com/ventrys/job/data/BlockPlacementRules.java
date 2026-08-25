package com.ventrys.job.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Set;

/**
 * Règles métier pour la pose de blocs (VentrysJob).
 * <p>
 * Joueurs : quasi uniquement les meubles. Bâtisseur + maillet : tout.
 */
public final class BlockPlacementRules {

    /**
     * Poutres / murets bois / supports de mine autorisés pour l'ouvrier
     * (en plus des planches chêne et des bûches extractibles).
     */
    private static final Set<String> OUVRIER_MINE_SUPPORT_BLOCKS = Set.of(
            // Bûches fines (= poutres)
            "westerosblocks:thin_oak_log",
            "westerosblocks:thin_spruce_log",
            "westerosblocks:thin_birch_log",
            // Murets bois
            "westerosblocks:oak_wall",
            "westerosblocks:spruce_wall",
            "westerosblocks:birch_wall",
            "westerosblocks:oak_vertical_planks_wall",
            "westerosblocks:spruce_vertical_planks_wall",
            "westerosblocks:birch_vertical_planks_wall",
            // Barrières bois
            "minecraft:oak_fence",
            "minecraft:spruce_fence",
            "minecraft:birch_fence",
            "westerosblocks:oak_vertical_planks_fence",
            "westerosblocks:spruce_vertical_planks_fence",
            "westerosblocks:birch_vertical_planks_fence",
            // Accès / lumière mine
            "minecraft:ladder",
            "westerosblocks:wood_ladder",
            "westerosblocks:rope_ladder",
            "minecraft:torch",
            "minecraft:wall_torch",
            "minecraft:soul_torch",
            "minecraft:soul_wall_torch",
            "minecraft:redstone_torch",
            "minecraft:redstone_wall_torch"
    );

    private BlockPlacementRules() {
    }

    /** Torches (sol / mur / soul / redstone) et variantes dont l'id contient "torch". */
    public static boolean isTorchLike(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return isTorchLike(state.getBlock());
    }

    public static boolean isTorchLike(Block block) {
        if (block == null) {
            return false;
        }
        if (block == Blocks.TORCH || block == Blocks.WALL_TORCH
                || block == Blocks.SOUL_TORCH || block == Blocks.SOUL_WALL_TORCH
                || block == Blocks.REDSTONE_TORCH || block == Blocks.REDSTONE_WALL_TORCH) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null && id.getPath().contains("torch");
    }

    public static boolean isTorchLikeItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.REDSTONE_TORCH)) {
            return true;
        }
        if (stack.getItem() instanceof BlockItem blockItem) {
            return isTorchLike(blockItem.getBlock());
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getPath().contains("torch");
    }

    public static boolean isOuvrierMineSupportBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        if (isTorchLike(state)) {
            return true;
        }
        Block block = state.getBlock();
        if (block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS || block == Blocks.BIRCH_PLANKS) {
            return true;
        }
        if (block == Blocks.OAK_FENCE || block == Blocks.SPRUCE_FENCE || block == Blocks.BIRCH_FENCE) {
            return true;
        }
        if (block == Blocks.LADDER) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            return false;
        }
        if (OUVRIER_MINE_SUPPORT_BLOCKS.contains(id.toString())) {
            return true;
        }
        // Westeros / variantes : planches & barrières oak/spruce/birch
        String path = id.getPath().toLowerCase(Locale.ROOT);
        boolean woodSpecies = path.contains("oak") || path.contains("spruce") || path.contains("birch")
                || path.contains("sapin") || path.contains("chene") || path.contains("bouleau");
        if (!woodSpecies) {
            return false;
        }
        return path.contains("plank") || path.contains("fence") || path.contains("barrier")
                || path.contains("barriere") || path.contains("wall") && path.contains("plank");
    }

    public static boolean canPlayerPlaceBlock(Player player, BlockState blockToPlace, BlockPos pos) {
        return canPlayerPlaceBlock(player, blockToPlace, pos, false);
    }

    /**
     * @param notify si true, envoie le message d'erreur au joueur en cas de refus
     * @deprecated plus de restriction de métier sur la pose (gérée côté plugin désormais) —
     * la pose de bloc suit maintenant purement les règles vanilla.
     */
    @Deprecated
    public static boolean canPlayerPlaceBlock(Player player, BlockState blockToPlace, BlockPos pos, boolean notify) {
        return true;
    }

    /**
     * Resync inventaire client après un refus de pose (évite un item « fantôme » disparu).
     */
    public static void syncInventory(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }

    /**
     * Filet si un chemin custom a consommé l'item alors que la pose a été annulée
     * (hors flux ForgeHooks normal, qui restaure déjà le stack).
     * <p>
     * Préférer {@link #syncInventory} après un simple cancel EntityPlaceEvent.
     */
    public static void ensurePlacementItemRestored(Player player, BlockState placedState) {
        if (player.getAbilities().instabuild || placedState == null) {
            return;
        }
        Item expected = placedState.getBlock().asItem();
        if (expected == null || expected == Items.AIR) {
            return;
        }

        // Si une main tient encore le BlockItem correspondant, rien à faire
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == placedState.getBlock()) {
                return;
            }
            if (stack.is(expected)) {
                return;
            }
        }

        // Stack totalement consommé à tort → rendre 1
        ItemStack refund = new ItemStack(expected, 1);
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
        syncInventory(player);
    }
}
