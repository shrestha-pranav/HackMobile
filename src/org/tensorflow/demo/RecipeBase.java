package org.tensorflow.demo;

import java.util.ArrayList;

public class RecipeBase {
    private ArrayList<String> myIngredients;
    ArrayList<Recipe> recipeList;
    private String[] lines = {
            "Strawberry Banana Smoothie,https://www.readyseteat.com/recipes-Strawberry-Banana-Smoothie-3519,ice cream,strawberry,banana",
            "Lemonade,http://allrecipes.com/recipe/32385/best-lemonade-ever/,lemon,water bottle,sugar",
            "Orange Sherbet,http://www.foodnetwork.com/recipes/alton-brown/orange-sherbet-recipe-1945337,orange,lemon,ice cream,milk",
            "Pinapple Rice,https://www.littlebroken.com/2015/06/29/pineapple-rice/,pineapple,lemon,rice,water bottle",
            "Indian Street Corn,http://www.foodnetwork.com/recipes/aarti-sequeira/indian-street-corn-salad-recipe-2121054,corn,lemon,spices"};

    public RecipeBase(ArrayList<String> ingredients) {
        this.myIngredients = ingredients;
        recipeList = new ArrayList<Recipe>();
        this.loadRecipes();
    }

    public RecipeBase() {
        this(new ArrayList<String>());
    }

    public void addIngredient(String ingredient) {
        myIngredients.add(ingredient);
    }

    private void loadRecipes() {
        for (String line : lines) {
            System.out.println(line);
            recipeList.add(new Recipe(line));
        }
    }

    public ArrayList<Recipe> getRecipes() {
        return getRecipes(this.myIngredients);
    }

    private ArrayList<Recipe> getRecipes(ArrayList<String> ingredients) {
        ArrayList<Recipe> results = new ArrayList<>();
        for (Recipe recipe : recipeList) {
            Recipe t = recipe.compare(ingredients);
            if (t.availableIngredients.size() > t.unavailableIngredients.size())
                results.add(t);
        }
        return results;
    }

    public class Recipe {
        String recipeName;
        ArrayList<String> availableIngredients = new ArrayList<String>();
        ArrayList<String> unavailableIngredients = new ArrayList<String>();
        String recipeUrl;

        public Recipe(String line) {
            String[] list = line.split(",");
            recipeName = list[0];
            recipeUrl = list[1];
            for (int i = 2; i < list.length; i++) {
                availableIngredients.add(list[i]);
            }
        }

        public Recipe(String name, String url) {
            recipeName = name;
            recipeUrl = url;
        }

        public Recipe compare(ArrayList<String> ingredients) {
            Recipe result = new Recipe(recipeName, recipeUrl);
            for (String a : availableIngredients) {
                if (ingredients.contains(a))
                    result.availableIngredients.add(a);
                else
                    result.unavailableIngredients.add(a);
            }
            return result;
        }
    }
}