package org.tensorflow.demo;

import java.util.ArrayList;
import java.util.HashMap;

public class RecipeBase {
    private ArrayList<String> availableIngredients;
    private ArrayList<Recipe> recipeList;
    private ArrayList<String> lines;

    public RecipeBase(ArrayList<String> ingredients) {
        this.availableIngredients = ingredients;
        this.loadRecipes();
    }

    public addIngredient(String ingredient) {
        availableIngredients.add(ingredient);
    }

    private void loadRecipes() {
        try {
            for (String line : lines) {
                recipeList.add(new Recipe(line));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Recipe> getRecipes() {
        return getRecipes(this.availableIngredients);
    }

    private ArrayList<Recipe> getRecipes(ArrayList<String> ingredients) {
        ArrayList<Recipe> results = new ArrayList<>();
        for (Recipe recipe : recipeList) {
            Recipe tmp = recipe.compare(ingredients);
            if (tmp.availableIngredients.size() > tmp.unavailableIngredients.size())
                results.add(tmp);
        }
        return results;
    }

    public class Recipe {
        String recipe_name;
        ArrayList<String> availableIngredients = new ArrayList<String>();
        ArrayList<String> unavailableIngredients = new ArrayList<String>();
        String recipeUrl;

        public Recipe(String line) {
            String[] list = line.split(",");
            recipe_name = list[0];
            recipeUrl = list[1];
            for (int i = 2; i < line.length(); i++)
                availableIngredients.add(list[i]);
        }

        public Recipe() {
        }

        public Recipe compare(ArrayList<String> ingredients) {
            Recipe result = new Recipe();
            result.recipe_name = recipe_name;
            result.recipeUrl = recipeUrl;
            for (String a : availableIngredients) {
                if (ingredients.contains(a)) result.availableIngredients.add(a);
                else result.unavailableIngredients.add(a);
            }
            return result;
        }
    }
}