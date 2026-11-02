package com.nutriworks.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a logged meal entry.
 * Contains portion size, calculated calories/macros, meal category,
 * timestamp, and optional photo reference.
 */
@Entity(tableName = "meal_entries")
public class MealEntry {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String foodName;
    private double grams;
    private int calories;
    private double protein;
    private double carbs;
    private double fat;
    private String mealType; // "Breakfast", "Lunch", "Dinner", "Snack"
    private long timestamp;
    private String imagePath;

    public MealEntry() {
    }

    public MealEntry(String foodName,
                     double grams,
                     int calories,
                     double protein,
                     double carbs,
                     double fat,
                     String mealType,
                     long timestamp,
                     String imagePath) {
        this.foodName = foodName;
        this.grams = grams;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.mealType = mealType;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFoodName() {
        return foodName != null ? foodName : "";
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public double getGrams() {
        return grams;
    }

    public void setGrams(double grams) {
        this.grams = grams;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public double getProtein() {
        return protein;
    }

    public void setProtein(double protein) {
        this.protein = protein;
    }

    public double getCarbs() {
        return carbs;
    }

    public void setCarbs(double carbs) {
        this.carbs = carbs;
    }

    public double getFat() {
        return fat;
    }

    public void setFat(double fat) {
        this.fat = fat;
    }

    public String getMealType() {
        return mealType != null ? mealType : "Meal";
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
