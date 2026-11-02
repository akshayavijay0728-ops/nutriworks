package com.nutriworks.app.data.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Single food search result from USDA FoodData Central.
 */
public class UsdaFoodItem {

    @SerializedName("description")
    private String description;

    @SerializedName("foodNutrients")
    private List<UsdaFoodNutrient> foodNutrients;

    public String getDescription() {
        return description != null ? description : "";
    }

    public List<UsdaFoodNutrient> getFoodNutrients() {
        return foodNutrients;
    }
}
