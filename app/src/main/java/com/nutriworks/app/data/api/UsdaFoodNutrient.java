package com.nutriworks.app.data.api;

import com.google.gson.annotations.SerializedName;

/**
 * Nutrient entry in USDA FoodData Central search item.
 */
public class UsdaFoodNutrient {

    @SerializedName("nutrientId")
    private int nutrientId;

    @SerializedName("nutrientName")
    private String nutrientName;

    @SerializedName("value")
    private double value;

    @SerializedName("unitName")
    private String unitName;

    public int getNutrientId() {
        return nutrientId;
    }

    public String getNutrientName() {
        return nutrientName;
    }

    public double getValue() {
        return value;
    }

    public String getUnitName() {
        return unitName;
    }
}
