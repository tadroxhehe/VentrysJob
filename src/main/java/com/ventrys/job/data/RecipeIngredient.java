package com.ventrys.job.data;

public class RecipeIngredient {
    private String itemId; // Format: "modid:item_id"
    private int count;
    /** Si renseigné sur une sortie de recette, nom affiché sur la pile craftée (Component texte brut). */
    private String displayName;

    public RecipeIngredient() {
        this.count = 1;
    }

    public RecipeIngredient(String itemId, int count) {
        this.itemId = itemId;
        this.count = count;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

