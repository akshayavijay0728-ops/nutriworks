package com.nutriworks.app.data.api;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/**
 * Top-level search response envelope from USDA FoodData Central.
 */
public class UsdaSearchResponse {

    @SerializedName("totalHits")
    private int totalHits;

    @SerializedName("foods")
    private List<UsdaFoodItem> foods;

    public int getTotalHits() {
        return totalHits;
    }

    public List<UsdaFoodItem> getFoods() {
        return foods != null ? foods : Collections.emptyList();
    }
}
