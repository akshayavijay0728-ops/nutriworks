package com.nutriworks.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a food item with baseline nutritional values
 * and favorite/recent tracking metadata.
 */
@Entity(tableName = "food_items")
public class FoodItem {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private int calories; // kcal per 100g
    private double proteinGrams;
    private double carbsGrams;
    private double fatGrams;
    private boolean isFavorite;
    private long lastUsedTimestamp;

    public FoodItem() {
    }

    public FoodItem(String name,
                    int calories,
                    double proteinGrams,
                    double carbsGrams,
                    double fatGrams,
                    boolean isFavorite,
                    long lastUsedTimestamp) {
        this.name = name;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.isFavorite = isFavorite;
        this.lastUsedTimestamp = lastUsedTimestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(double proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public double getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(double carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public double getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(double fatGrams) {
        this.fatGrams = fatGrams;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }

    public void setLastUsedTimestamp(long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }
}
