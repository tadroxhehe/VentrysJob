package com.ventrys.job.energy;

import com.ventrys.job.data.JobRecipe;
import com.ventrys.job.data.RecipeIngredient;

/**
 * Résout le coût énergie d'un craft table (valeur JSON ou calcul automatique).
 * Valeurs ~+20 % vs grille précédente.
 */
public final class JobEnergyCostCalculator {

    private JobEnergyCostCalculator() {
    }

    public static float resolveCraftCost(String jobId, JobRecipe recipe) {
        if (recipe == null) {
            return 0f;
        }
        float explicit = recipe.getEnergyCost();
        if (explicit >= 0f) {
            return explicit;
        }
        return computeAutoCost(jobId, recipe);
    }

    private static float computeAutoCost(String jobId, JobRecipe recipe) {
        String id = recipe.getId() != null ? recipe.getId().toLowerCase() : "";
        int inputTotal = sumInputs(recipe);
        String outputId = primaryOutputId(recipe);

        if (id.contains("statue")) {
            return 100f;
        }

        // Armures forgeron
        if (id.contains("platecorps3")) return 80f;
        if (id.contains("platecorps2")) return 58f;
        if (id.contains("platecorps1")) return 42f;
        if (id.contains("platebas3")) return 48f;
        if (id.contains("platebas2")) return 36f;
        if (id.contains("platebas1")) return 26f;
        if (id.contains("platetete3")) return 30f;
        if (id.contains("platetete2")) return 22f;
        if (id.contains("platetete1")) return 14f;
        if (id.contains("maillecorps")) return 46f;
        if (id.contains("maillebas")) return 36f;
        if (id.contains("mailletete")) return 26f;

        // Couturier armures (~−20 % vs grille précédente)
        if (id.contains("gambison_corp") || id.contains("gambisoncorps")) return 24f;
        if (id.contains("gambison_bas")) return 18f;
        if (id.contains("gambison_tete")) return 14f;
        if (id.contains("hdskin_corp") || (id.contains("cuir") && id.contains("corps"))) return 30f;
        if (id.contains("hdskin_bas") || id.contains("hdskin_haut")
                || (id.contains("cuir") && (id.contains("bas") || id.contains("haut")))) {
            return 22f;
        }

        // Armes
        if (id.contains("flamberge") || id.contains("espadon")) return 46f;
        if (id.contains("claymore")) return 34f;
        if (id.contains("dague")) return 22f;
        if (id.contains("crossbow") || id.contains("arbalete")) return 26f;
        if (id.contains("ecu")) return 24f;

        // Gemmes taillées
        if (id.contains("taille") || id.contains("taill")) {
            if (outputId.contains("diamant") || outputId.contains("emeraude")
                    || outputId.contains("saphir") || outputId.contains("rubis")) {
                return 54f;
            }
        }

        // Apothicaire infusions
        if (id.contains("infusion")) {
            if (id.contains("adrenaline") || id.contains("dadrenaline") || id.contains("lierre")) {
                return 50f;
            }
            return 34f;
        }
        if (id.contains("bandage") || id.contains("baume")) return 12f;

        // Stations métier
        if (id.contains("enclume") || id.contains("alambic") || id.contains("marmite")) {
            return 36f;
        }

        // Outils forgeron
        if (id.contains("_bronze") || id.contains("bronze_")) {
            if (isToolOutput(outputId)) return 10f;
        }
        if (isToolOutput(outputId) && (outputId.contains("fer") || outputId.contains("iron"))) {
            return 14f;
        }

        // Cuisinier
        if ("cuisinier".equals(jobId)) {
            if (isSimpleCook(id, recipe)) return 2.5f;
            if (id.contains("soupe") || id.contains("ragout") || id.contains("ragoût")) return 14f;
            if (id.contains("vin") || id.contains("gateau") || id.contains("gâteau") || id.contains("chope")) {
                return 31f;
            }
            if (id.contains("farine") || id.contains("pate") || id.contains("pâte") || id.contains("sucre")) {
                return 6f;
            }
        }

        // Artisan : construction volume (stairs/slabs/cobble…) très légère — grosses commandes.
        // Meubles / armes / gros crafts restent sur une grille adoucie.
        if ("artisan".equals(jobId)) {
            if (id.contains("batons")) {
                return 1f;
            }
            if (id.contains("thin_") || outputId.contains("thin_") || id.contains("poutre")) {
                return 0.4f;
            }
            if (id.contains("vase_apothicaire") || outputId.contains("vase_apothicaire")) {
                return 66f;
            }

            int outCount = sumOutputs(recipe);

            // Bois de construction : escaliers / dalles / planches / clôtures…
            // Ex. 16 escaliers ≈ 4 énergie (au lieu de ~24–100).
            if (isWoodConstruction(id, outputId)) {
                if (outputId.contains("timber") || id.contains("timber")) {
                    return 0.4f;
                }
                if (outCount >= 4) {
                    return 0.4f;
                }
                return 0.25f;
            }

            // Pierre de construction : ex. 5 crafts ×8 cobble (40) ≈ 2 énergie.
            if (isStoneConstruction(id, outputId)) {
                if (outCount >= 4) {
                    return 0.4f;
                }
                return 0.2f;
            }

            if (inputTotal >= 40) return 12f;
            if (inputTotal >= 20) return 8f;
            if (inputTotal >= 8) return 5f;
            return 3.5f;
        }

        // Couturier textile / base (~−30 % chaîne fil→tissu, fallbacks adoucis)
        if ("couturier".equals(jobId)) {
            if (id.contains("fil") || id.contains("tissu") || id.contains("corde")) return 2.5f;
            if (id.contains("gourde") || id.contains("laisse") || id.contains("sel")) return 8f;
            // Papier = intermédiaire bas (3 planches) ; ne plus le coller au bucket livre (10)
            if (id.contains("paper") || outputId.contains("paper")) return 2.5f;
            if (id.contains("book") || id.contains("livre") || outputId.contains("book")) return 10f;
            if (inputTotal >= 100) return 32f;
            if (inputTotal >= 60) return 22f;
            if (inputTotal >= 30) return 12f;
            if (inputTotal >= 10) return 6f;
            if (inputTotal >= 4) return 3.5f;
            return 2.5f;
        }

        // Forgeron : petits objets bas ; outils restent 10/14 ; gros crafts armures au fallback
        if ("forgeron".equals(jobId)) {
            if (id.contains("arrow") || id.contains("fleche")) {
                return 3f;
            }
            if (id.contains("clou") || id.contains("pepite") || id.contains("nugget")
                    || id.contains("lingot") || id.contains("ingot")) {
                return 2f;
            }
            if (id.contains("clef") || id.contains("cadenat") || id.contains("key") || id.contains("lock")) {
                return 5f;
            }
            // Anneaux / maille brute (pas les pièces d'armure déjà gérées plus haut)
            if (id.contains("maille") && !id.contains("corps") && !id.contains("bas") && !id.contains("tete")) {
                return 2f;
            }
            if (inputTotal >= 60) {
                return 30f;
            }
            if (inputTotal >= 24) {
                return 22f;
            }
            if (inputTotal <= 8) {
                return 3f;
            }
        }

        // Apothicaire divers
        if ("apothicaire".equals(jobId)) {
            if (id.contains("bougie") || id.contains("teinture")) return 6f;
            if (inputTotal >= 8) return 14f;
            return 10f;
        }

        // Ouvrier
        if ("ouvrier".equals(jobId) && id.contains("charbon")) {
            return 4f;
        }

        // Fallback générique
        if (inputTotal >= 100) return 42f;
        if (inputTotal >= 60) return 30f;
        if (inputTotal >= 30) return 18f;
        if (inputTotal >= 10) return 10f;
        if (inputTotal >= 4) return 6f;
        return 4f;
    }

    private static boolean isSimpleCook(String id, JobRecipe recipe) {
        if (!id.startsWith("cuis_")) {
            return false;
        }
        if (id.contains("soupe") || id.contains("vin") || id.contains("gateau") || id.contains("chope")) {
            return false;
        }
        return recipe.getInputs().size() <= 2 && sumInputs(recipe) <= 3;
    }

    private static boolean isToolOutput(String outputId) {
        return outputId.contains("pioche") || outputId.contains("hache")
                || outputId.contains("pelle") || outputId.contains("fourche")
                || outputId.contains("scie") || outputId.contains("burin")
                || outputId.contains("marteau");
    }

    private static int sumInputs(JobRecipe recipe) {
        int total = 0;
        for (RecipeIngredient in : recipe.getInputs()) {
            total += in.getCount();
        }
        return total;
    }

    private static int sumOutputs(JobRecipe recipe) {
        int total = 0;
        for (RecipeIngredient out : recipe.getOutputsForCraft()) {
            total += out.getCount();
        }
        return Math.max(1, total);
    }

    /** Blocs structure bois (pas meubles / armes / vaisselle). */
    private static boolean isWoodConstruction(String id, String outputId) {
        if (id.contains("assiette") || id.contains("chope") || id.contains("bol")
                || id.contains("epee") || id.contains("dague") || id.contains("hache")
                || id.contains("lance") || id.contains("masse") || id.contains("fleau")
                || id.contains("bouclier") || id.contains("bocle") || id.contains("ecu")
                || id.contains("pavois") || id.contains("table") || id.contains("chaise")
                || id.contains("banc") || id.contains("armoire") || id.contains("coffre")
                || id.contains("nid") || id.contains("statue") || id.contains("commode")
                || id.contains("fauteuil") || id.contains("tabouret") || id.contains("buffet")
                || id.contains("chariot") || id.contains("tonneau") || id.contains("cart")) {
            return false;
        }
        if (outputId.contains("plank") || outputId.contains("timber")
                || outputId.contains("thin_") || outputId.contains("wood_ladder")
                || outputId.contains("rope_ladder") || outputId.contains("panelling")
                || outputId.contains("wattle") || outputId.contains("thatch")) {
            return true;
        }
        boolean woodSpecies = outputId.contains("oak") || outputId.contains("spruce")
                || outputId.contains("birch") || outputId.contains("jungle")
                || outputId.contains("acacia") || outputId.contains("dark_oak")
                || outputId.contains("reach_oak") || outputId.contains("reach_spruce");
        boolean shape = outputId.contains("stairs") || outputId.contains("slab")
                || outputId.contains("wall") || outputId.contains("fence")
                || outputId.contains("tip") || outputId.contains("hopper")
                || outputId.contains("log") || outputId.contains("door")
                || outputId.contains("trapdoor") || outputId.contains("pressure_plate")
                || outputId.contains("button") || outputId.contains("sign")
                || outputId.contains("fence_gate");
        return woodSpecies && shape;
    }

    /** Blocs structure pierre (pas bancs / statues). */
    private static boolean isStoneConstruction(String id, String outputId) {
        if (id.contains("banc") || id.contains("statue") || id.contains("meuble")
                || id.contains("table") || id.contains("chaise") || id.contains("commode")
                || id.contains("armoire") || id.contains("coffre")) {
            return false;
        }
        return outputId.contains("cobble") || outputId.contains("stone")
                || outputId.contains("granite") || outputId.contains("slate")
                || outputId.contains("brick") || outputId.contains("sandstone")
                || outputId.contains("basalt") || outputId.contains("marble")
                || outputId.contains("limestone") || outputId.contains("andesite")
                || outputId.contains("diorite") || outputId.contains("calcaire")
                || outputId.contains("enduit") || outputId.contains("chaux")
                || outputId.contains("mud") || outputId.contains("daub")
                || outputId.contains("plaster") || outputId.contains("frame");
    }

    private static String primaryOutputId(JobRecipe recipe) {
        for (RecipeIngredient out : recipe.getOutputsForCraft()) {
            if (out.getItemId() != null) {
                return out.getItemId().toLowerCase();
            }
        }
        return "";
    }
}
