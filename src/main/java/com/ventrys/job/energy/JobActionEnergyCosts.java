package com.ventrys.job.energy;

/**
 * Coûts d'énergie métier par action (complétion, pas par clic).
 */
public final class JobActionEnergyCosts {

    private JobActionEnergyCosts() {
    }

    // Ouvrier — extraction
    /** ~10 énergie pour 64 bûches (demande élevée / volume mineur-ouvrier). */
    public static final float EXTRACT_LOG = 0.15f;
    public static final float EXTRACT_SAW = 0.3f;
    public static final float EXTRACT_ORE = 0.7f;
    public static final float EXTRACT_VERDRAGON = 3.0f;
    public static final float EXTRACT_STONE = 0.5f;
    public static final float EXTRACT_CALCITE = 0.5f;
    public static final float EXTRACT_SAND = 0.6f;
    public static final float EXTRACT_CLAY = 0.6f;

    // Paysan — légèrement augmenté
    public static final float TILL_FARMLAND = 0.3f;
    public static final float PLANT_SEED = 0.25f;
    public static final float HARVEST_CROP = 0.5f;
    public static final float FEED_ANIMAL = 0.75f;
    public static final float HYDRATE_ANIMAL = 0.75f;
    public static final float MILK_COW = 4.2f;
    public static final float BREED_ANIMAL = 11.5f;

    // Pose / casse déco bâtisseur inchangé ; granite / casse volume plus légère
    public static final float PLACE_BLOCK = 0.35f;
    public static final float BREAK_DECORATIVE = 0.35f;
    public static final float BREAK_NORMAL = 0.10f;

    // Apothicaire — vase / meule (hors craft)
    public static final float VASE_PLANT = 0.5f;
    public static final float VASE_WATER = 0.4f;
    public static final float VASE_HARVEST = 0.9f;
    public static final float MEULE_GRIND = 1.4f;
}
