package com.ventrys.job.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.Set;

/**
 * Meubles / props déplaçables par tout le monde (vanilla + mods).
 * Le reste de la construction reste réservé au bâtisseur (+ maillet).
 */
public final class FurnitureAccess {

    /** Namespace VentrysDeco — presque tout est meuble / prop. */
    private static final String VENTRYS_BLOCS = "ventrys_blocs";

    /**
     * Établis métier VentrysDeco : pas des meubles libres
     * (casse / pose restent gérées comme construction / tables).
     */
    private static final Set<String> VENTRYS_BLOCS_JOB_STATIONS = Set.of(
            "menuisier",
            "couturier",
            "alambic",
            "marmite",
            "enclume"
    );

    /**
     * Mots dans l'ID (path) → meuble, sauf si un mot « structure » est aussi présent.
     */
    private static final String[] FURNITURE_TOKENS = {
            "chair", "chaise", "stool", "tabouret", "bench", "banc", "seat", "siege",
            "sofa", "couch", "throne", "trone", "pew",
            "table", "desk", "bureau", "counter",
            "bed", "lit", "cot",
            "cabinet", "cupboard", "armoire", "armoir", "wardrobe", "dresser", "comode",
            "shelf", "shelve", "etagere", "bookcase", "bookshelf",
            "drawer", "chest", "coffre", "crate", "caisse", "barrel", "tonneau",
            "carpet", "rug", "tapis", "mat", "curtain", "rideau", "tapestry", "tapisserie",
            "candle", "bougie", "candlestick", "lantern", "lanterne", "chandelier",
            "pillow", "coussin", "cushion",
            "vase", "urn", "poterie", "flowerpot", "flower_pot",
            "painting", "tableau", "mirror", "miroir", "frame", "cadre",
            "clock", "horloge", "banner", "drapeau",
            "lectern", "pupitre", "jukebox",
            "armor_stand", "mannequin", "rack", "stand", "statue", "statut",
            "meuble", "furniture", "decor", "ornament",
            "tankard", "mug", "cup", "bowl", "plate", "bottle", "goblet"
    };

    /** Si présent dans le path, ce n'est PAS un meuble (construction). */
    private static final String[] STRUCTURE_TOKENS = {
            "stair", "stairs", "slab", "wall", "fence", "gate", "pane",
            "roof", "toiture", "pillar", "column", "beam", "log", "plank", "planks",
            "brick", "bricks", "cobble", "cobblestone", "stonebrick", "deepslate",
            "ore",
            "door", "trapdoor", "window",
            "ramp", "tile", "tiles",
            "foundation", "support"
    };

    private FurnitureAccess() {
    }

    public static boolean isFurniture(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (isFurnitureBlockClass(block)) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) {
            return false;
        }
        String path = id.getPath().toLowerCase(Locale.ROOT);

        // VentrysDeco : meubles / statues / props — sauf établis métier
        if (VENTRYS_BLOCS.equals(id.getNamespace())) {
            return !VENTRYS_BLOCS_JOB_STATIONS.contains(path);
        }

        if (containsToken(path, STRUCTURE_TOKENS)) {
            // Escaliers / dalles / murs : jamais meubles, même si le nom contient "table" etc.
            return false;
        }
        if (containsToken(path, FURNITURE_TOKENS)) {
            return true;
        }

        // Props légers Westeros / déco (souvent Material.DECORATION)
        Material mat = state.getMaterial();
        return mat == Material.DECORATION || mat == Material.CLOTH_DECORATION;
    }

    private static boolean isFurnitureBlockClass(Block block) {
        return block instanceof BedBlock
                || block instanceof CarpetBlock
                || block instanceof FlowerPotBlock
                || block instanceof ChestBlock
                || block instanceof AbstractChestBlock
                || block instanceof BarrelBlock
                || block instanceof LecternBlock
                || block instanceof BannerBlock
                || block instanceof WallBannerBlock
                || block instanceof SignBlock
                || block instanceof WallSignBlock
                || block instanceof SkullBlock
                || block instanceof WallSkullBlock
                || block instanceof LadderBlock;
    }

    private static boolean containsToken(String path, String[] tokens) {
        for (String token : tokens) {
            if (path.equals(token) || path.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
