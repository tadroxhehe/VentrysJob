package com.ventrys.job.energy;

/**
 * Coûts d'énergie métier par action (complétion, pas par clic).
 */
public final class JobActionEnergyCosts {

    private JobActionEnergyCosts() {
    }

    // Ouvrier — extraction
    public static final float EXTRACT_LOG = 0.4f;
    public static final float EXTRACT_SAW = 0.25f;
    public static final float EXTRACT_ORE = 0.6f;
    public static final float EXTRACT_VERDRAGON = 2.5f;
    public static final float EXTRACT_STONE = 0.4f;
    public static final float EXTRACT_CALCITE = 0.4f;
    public static final float EXTRACT_SAND = 0.5f;
    public static final float EXTRACT_CLAY = 0.5f;

    // Paysan
    public static final float TILL_FARMLAND = 0.2f;
    public static final float PLANT_SEED = 0.15f;
    public static final float HARVEST_CROP = 0.35f;
    public static final float FEED_ANIMAL = 0.5f;
    public static final float HYDRATE_ANIMAL = 0.5f;
    public static final float MILK_COW = 3.0f;
    public static final float BREED_ANIMAL = 8.0f;

    // Bâtisseur — ~0.3 % de la barre (0–100) par bloc posé / cassé
    public static final float PLACE_BLOCK = 0.3f;
    public static final float BREAK_DECORATIVE = 0.3f;
    public static final float BREAK_NORMAL = 0.3f;

    // Apothicaire — vase
    public static final float VASE_PLANT = 0.3f;
    public static final float VASE_WATER = 0.2f;
    public static final float VASE_HARVEST = 0.5f;

    // Meule
    public static final float MEULE_GRIND = 0.8f;
}
