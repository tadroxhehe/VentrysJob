package com.ventrys.job.energy;

import com.ventrys.job.data.JobRecipe;
import com.ventrys.job.data.RecipeIngredient;

/**
 * Résout le coût énergie d'un craft table (valeur JSON ou calcul automatique).
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
            return 85f;
        }

        // Armures forgeron
        if (id.contains("platecorps3")) return 65f;
        if (id.contains("platecorps2")) return 48f;
        if (id.contains("platecorps1")) return 35f;
        if (id.contains("platebas3")) return 40f;
        if (id.contains("platebas2")) return 30f;
        if (id.contains("platebas1")) return 22f;
        if (id.contains("platetete3")) return 25f;
        if (id.contains("platetete2")) return 18f;
        if (id.contains("platetete1")) return 12f;
        if (id.contains("maillecorps")) return 38f;
        if (id.contains("maillebas")) return 30f;
        if (id.contains("mailletete")) return 22f;

        // Couturier armures
        if (id.contains("gambison_corp") || id.contains("gambisoncorps")) return 25f;
        if (id.contains("gambison_bas")) return 18f;
        if (id.contains("gambison_tete")) return 15f;
        if (id.contains("cuir") && id.contains("corps")) return 32f;
        if (id.contains("cuir") && (id.contains("bas") || id.contains("haut"))) return 24f;

        // Armes
        if (id.contains("flamberge") || id.contains("espadon")) return 38f;
        if (id.contains("claymore")) return 28f;
        if (id.contains("dague")) return 18f;
        if (id.contains("crossbow") || id.contains("arbalete")) return 22f;
        if (id.contains("ecu")) return 20f;

        // Gemmes taillées
        if (id.contains("taille") || id.contains("taill")) {
            if (outputId.contains("diamant") || outputId.contains("emeraude")
                    || outputId.contains("saphir") || outputId.contains("rubis")) {
                return 45f;
            }
        }

        // Apothicaire infusions
        if (id.contains("infusion")) {
            if (id.contains("adrenaline") || id.contains("dadrenaline") || id.contains("lierre")) {
                return 42f;
            }
            return 28f;
        }
        if (id.contains("bandage") || id.contains("baume")) return 10f;

        // Stations métier
        if (id.contains("enclume") || id.contains("alambic") || id.contains("marmite")) {
            return 30f;
        }

        // Outils forgeron
        if (id.contains("_bronze") || id.contains("bronze_")) {
            if (isToolOutput(outputId)) return 8f;
        }
        if (isToolOutput(outputId) && (outputId.contains("fer") || outputId.contains("iron"))) {
            return 12f;
        }

        // Cuisinier
        if ("cuisinier".equals(jobId)) {
            if (isSimpleCook(id, recipe)) return 2f;
            if (id.contains("soupe") || id.contains("ragout") || id.contains("ragoût")) return 12f;
            if (id.contains("vin") || id.contains("gateau") || id.contains("gâteau") || id.contains("chope")) {
                return 26f;
            }
            if (id.contains("farine") || id.contains("pate") || id.contains("pâte") || id.contains("sucre")) {
                return 5f;
            }
        }

        // Artisan meubles / blocs
        if ("artisan".equals(jobId)) {
            if (inputTotal >= 40) return 18f;
            if (inputTotal >= 20) return 12f;
            if (inputTotal >= 8) return 8f;
            return 5f;
        }

        // Couturier textile
        if ("couturier".equals(jobId)) {
            if (id.contains("fil") || id.contains("tissu") || id.contains("corde")) return 3f;
            if (id.contains("gourde") || id.contains("laisse") || id.contains("sel")) return 10f;
        }

        // Forgeron composants
        if ("forgeron".equals(jobId)) {
            if (id.contains("maille") || id.contains("clou")) return 4f;
            if (id.contains("bronze_lingot") || id.contains("lingot")) return 5f;
            if (id.contains("clef") || id.contains("cadenat")) return 15f;
            if (inputTotal >= 60) return 25f;
            if (inputTotal >= 24) return 18f;
        }

        // Apothicaire divers
        if ("apothicaire".equals(jobId)) {
            if (id.contains("bougie") || id.contains("teinture")) return 5f;
            if (inputTotal >= 8) return 12f;
            return 8f;
        }

        // Ouvrier
        if ("ouvrier".equals(jobId) && id.contains("charbon")) {
            return 3f;
        }

        // Fallback générique
        if (inputTotal >= 100) return 35f;
        if (inputTotal >= 60) return 25f;
        if (inputTotal >= 30) return 15f;
        if (inputTotal >= 10) return 8f;
        if (inputTotal >= 4) return 5f;
        return 3f;
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

    private static String primaryOutputId(JobRecipe recipe) {
        for (RecipeIngredient out : recipe.getOutputsForCraft()) {
            if (out.getItemId() != null) {
                return out.getItemId().toLowerCase();
            }
        }
        return "";
    }
}
