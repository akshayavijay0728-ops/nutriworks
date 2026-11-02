package com.nutriworks.model;

/**
 * Model representing a logged meal entry in SQLite.
 * Tracks portions, calories, macros, meal category, timestamp, optional image,
 * and entry origin (PHONE, UPLOAD, WEBCAM, MANUAL).
 */
public class MealEntry {

    public enum Source {
        PHONE,
        UPLOAD,
        WEBCAM,
        MANUAL
    }

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
    private Source source;

    public MealEntry() {
        this.source = Source.MANUAL;
        this.timestamp = System.currentTimeMillis();
    }

    public MealEntry(String foodName,
                     double grams,
                     int calories,
                     double protein,
                     double carbs,
                     double fat,
                     String mealType,
                     long timestamp,
                     String imagePath,
                     Source source) {
        this.foodName = foodName != null ? foodName : "";
        this.grams = Math.max(0.0, grams);
        this.calories = Math.max(0, calories);
        this.protein = Math.max(0.0, protein);
        this.carbs = Math.max(0.0, carbs);
        this.fat = Math.max(0.0, fat);
        this.mealType = mealType != null ? mealType : "Lunch";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.imagePath = imagePath != null ? imagePath : "";
        this.source = source != null ? source : Source.MANUAL;
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
        return mealType != null ? mealType : "Lunch";
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
        return imagePath != null ? imagePath : "";
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Source getSource() {
        return source != null ? source : Source.MANUAL;
    }

    public void setSource(Source source) {
        this.source = source;
    }
}
