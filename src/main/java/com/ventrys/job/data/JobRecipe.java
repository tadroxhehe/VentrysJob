package com.ventrys.job.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JobRecipe {
    private String id;
    private String name;
    private String description;
    private List<RecipeIngredient> inputs;
    /** Sortie unique (recettes existantes). */
    private RecipeIngredient output;
    /** Plusieurs sorties optionnelles ; si renseigné, utilisé pour le craft à la place de {@link #output} seul. */
    private List<RecipeIngredient> outputs;
    // private int energyCost; // Desactive temporairement
    private int craftTime;

    public JobRecipe() {
        this.inputs = new ArrayList<>();
        // this.energyCost = 0; // Desactive temporairement
        this.craftTime = 20; // 1 seconde par défaut
    }

    public JobRecipe(String id, String name) {
        this.id = id;
        this.name = name;
        this.inputs = new ArrayList<>();
        // this.energyCost = 0; // Desactive temporairement
        this.craftTime = 20;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RecipeIngredient> getInputs() {
        return inputs;
    }

    public void setInputs(List<RecipeIngredient> inputs) {
        this.inputs = inputs;
    }

    public RecipeIngredient getOutput() {
        return output;
    }

    public void setOutput(RecipeIngredient output) {
        this.output = output;
    }

    public List<RecipeIngredient> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<RecipeIngredient> outputs) {
        this.outputs = outputs;
    }

    /**
     * Sorties effectives au craft : {@code outputs} si non vide, sinon une seule entrée {@code output}.
     */
    public List<RecipeIngredient> getOutputsForCraft() {
        if (outputs != null && !outputs.isEmpty()) {
            return outputs;
        }
        if (output != null) {
            return Collections.singletonList(output);
        }
        return Collections.emptyList();
    }

    // public int getEnergyCost() {
    //     return energyCost;
    // }

    // public void setEnergyCost(int energyCost) {
    //     this.energyCost = energyCost;
    // }

    public int getCraftTime() {
        return craftTime;
    }

    public void setCraftTime(int craftTime) {
        this.craftTime = craftTime;
    }

    public void addInput(RecipeIngredient ingredient) {
        this.inputs.add(ingredient);
    }
}

