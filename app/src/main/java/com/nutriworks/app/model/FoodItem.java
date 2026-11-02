package com.nutriworks.app.model;

/**
 * Model representing a recognized or logged food item with
 * nutritional breakdown and meal metadata.
 */
public class FoodItem {

    private long id;
    private String name;
    private int calories;
    private float proteinGrams;
    private float carbsGrams;
    private float fatGrams;
    private String mealType;
    private long timestamp;

    public FoodItem() {
    }

    public FoodItem(long id, String name, int calories, float proteinGrams, float carbsGrams, float fatGrams, String mealType, long timestamp) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
        this.mealType = mealType;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
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

    public float getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(float proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public float getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(float carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public float getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(float fatGrams) {
        this.fatGrams = fatGrams;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
