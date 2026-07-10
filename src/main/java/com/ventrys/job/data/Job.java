package com.ventrys.job.data;

import java.util.ArrayList;
import java.util.List;

public class Job {
    private String id;
    private String name;
    private String description;
    private List<JobRecipe> recipes;

    public Job() {
        this.recipes = new ArrayList<>();
    }

    public Job(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.recipes = new ArrayList<>();
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

    public List<JobRecipe> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<JobRecipe> recipes) {
        this.recipes = recipes;
    }

    public void addRecipe(JobRecipe recipe) {
        this.recipes.add(recipe);
    }
}

