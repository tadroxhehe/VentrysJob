package com.ventrys.job.data;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Planches ressources interchangeables (chêne / sapin / bouleau) pour certains crafts
 * (outils, armes, bols, meubles génériques). Les recettes liées à un bois précis
 * (Westeros oak_*, tablechene, etc.) restent strictes.
 */
public final class PlankCraftAccess {

    public static final Set<String> PLANCHE_IDS = Set.of(
            "ventrysitem:res_planche_chene",
            "ventrysitem:res_planche_sapin",
            "ventrysitem:res_planche_bouleau"
    );

    private PlankCraftAccess() {
    }

    public static boolean isPlancheResource(String itemId) {
        return itemId != null && PLANCHE_IDS.contains(itemId);
    }

    /**
     * True si cette ligne d'ingrédient planche peut être payée avec n'importe laquelle des 3.
     */
    public static boolean allowsAnyPlanche(JobRecipe recipe, String ingredientItemId) {
        if (!isPlancheResource(ingredientItemId) || recipe == null) {
            return false;
        }
        String out = primaryOutputId(recipe).toLowerCase(Locale.ROOT);
        String recipeId = recipe.getId() != null ? recipe.getId().toLowerCase(Locale.ROOT) : "";
        if (out.isEmpty()) {
            return false;
        }
        // Colombage / cadres bois : planches interchangeables (chêne, sapin, bouleau)
        if (isTimberFrameCraft(recipeId, out)) {
            return true;
        }
        // Ruche domestique et autres props sans essence imposée
        if ("minecraft:beehive".equals(out) || recipeId.contains("ruche")) {
            return true;
        }
        // Bois imposé par le résultat (bloc / meuble d'essence)
        if (hasSpecificWoodToken(out)) {
            return false;
        }
        // Blocs Westeros / vanilla d'essence : toujours strict
        if (out.startsWith("westerosblocks:") || out.startsWith("minecraft:")) {
            return false;
        }
        // Outils / armes / boucliers / bols
        if (isToolWeaponOrBowl(out)) {
            return true;
        }
        // Meubles / props sans essence dans l'id (chandelier, pantin, maillet, etc.)
        if (out.startsWith("ventrys_blocs:") || out.startsWith("hdskinmod:") || out.startsWith("ventrysitem:")) {
            return true;
        }
        return false;
    }

    public static boolean hasEnough(Player player, JobRecipe recipe, String itemId, int count) {
        if (allowsAnyPlanche(recipe, itemId)) {
            return countMatchingPlanks(player) >= count;
        }
        return countExact(player, itemId) >= count;
    }

    public static boolean consume(Player player, JobRecipe recipe, String itemId, int count) {
        if (!hasEnough(player, recipe, itemId, count)) {
            return false;
        }
        if (allowsAnyPlanche(recipe, itemId)) {
            return consumeAnyPlank(player, count);
        }
        return consumeExact(player, itemId, count);
    }

    private static int countMatchingPlanks(Player player) {
        int found = 0;
        for (String id : PLANCHE_IDS) {
            found += countExact(player, id);
        }
        return found;
    }

    private static boolean consumeAnyPlank(Player player, int count) {
        int remaining = count;
        // Ordre : chêne puis sapin puis bouleau (stable, prévisible)
        for (String id : List.of(
                "ventrysitem:res_planche_chene",
                "ventrysitem:res_planche_sapin",
                "ventrysitem:res_planche_bouleau")) {
            if (remaining <= 0) {
                break;
            }
            int have = countExact(player, id);
            if (have <= 0) {
                continue;
            }
            int take = Math.min(remaining, have);
            if (!consumeExact(player, id, take)) {
                return false;
            }
            remaining -= take;
        }
        return remaining <= 0;
    }

    private static int countExact(Player player, String itemId) {
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (item == null || player == null) {
            return 0;
        }
        int found = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                found += stack.getCount();
            }
        }
        return found;
    }

    private static boolean consumeExact(Player player, String itemId, int count) {
        if (countExact(player, itemId) < count) {
            return false;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(itemId));
        if (item == null) {
            return false;
        }
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        return remaining <= 0;
    }

    private static boolean isTimberFrameCraft(String recipeId, String outputId) {
        if (outputId.contains("timber")) {
            return true;
        }
        return recipeId.contains("timber") && (recipeId.startsWith("art_") || recipeId.contains("artisan"));
    }

    private static boolean hasSpecificWoodToken(String outputId) {
        // Tokens d'essence dans le path (meubles / blocs dédiés)
        return contains(outputId, "chene") || contains(outputId, "oak")
                || contains(outputId, "sapin") || contains(outputId, "spruce")
                || contains(outputId, "bouleau") || contains(outputId, "birch")
                || contains(outputId, "bouelau"); // typo id existant
    }

    private static boolean isToolWeaponOrBowl(String outputId) {
        return contains(outputId, "pioche") || contains(outputId, "hache")
                || contains(outputId, "pelle") || contains(outputId, "fourche")
                || contains(outputId, "scie") || contains(outputId, "burin")
                || contains(outputId, "marteau") || contains(outputId, "maillet")
                || contains(outputId, "canne") || contains(outputId, "epee")
                || contains(outputId, "dague") || contains(outputId, "lance")
                || contains(outputId, "hallebarde") || contains(outputId, "claymore")
                || contains(outputId, "flamberge") || contains(outputId, "espadon")
                || contains(outputId, "hache_combat") || contains(outputId, "masse")
                || contains(outputId, "fleau") || contains(outputId, "etoile")
                || contains(outputId, "guisarme") || contains(outputId, "barbiche")
                || contains(outputId, "bec_de_corbin") || contains(outputId, "bec_corbin")
                || contains(outputId, "pavois") || contains(outputId, "bocle")
                || contains(outputId, "ecu") || contains(outputId, "arc")
                || contains(outputId, "bow") || contains(outputId, "crossbow")
                || contains(outputId, "bol") || contains(outputId, "bowl")
                || contains(outputId, "assiette") || contains(outputId, "chope");
    }

    private static boolean contains(String hay, String needle) {
        return hay != null && hay.contains(needle);
    }

    private static String primaryOutputId(JobRecipe recipe) {
        for (RecipeIngredient out : recipe.getOutputsForCraft()) {
            if (out != null && out.getItemId() != null) {
                return out.getItemId();
            }
        }
        return "";
    }
}
