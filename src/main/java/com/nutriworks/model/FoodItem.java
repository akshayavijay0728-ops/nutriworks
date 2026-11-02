package com.nutriworks.model;

/**
 * Model representing an entry in the Food Library table.
 */
public class FoodItem {

    private long id;
    private String name;
    private double kcalPer100g;
    private double proteinPer100g;
    private double carbsPer100g;
    private double fatPer100g;
    private double fiberPer100g;
    private double sugarPer100g;
    private double defaultServingGrams;
    private String servingLabel;
    private boolean isFavorite;
    private int useCount;

    public FoodItem() {
    }

    public FoodItem(String name,
                    double kcalPer100g,
                    double proteinPer100g,
                    double carbsPer100g,
                    double fatPer100g,
                    double fiberPer100g,
                    double sugarPer100g,
                    double defaultServingGrams,
                    String servingLabel) {
        this.name = name != null ? name : "";
        this.kcalPer100g = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
        this.sugarPer100g = sugarPer100g;
        this.defaultServingGrams = defaultServingGrams > 0 ? defaultServingGrams : 100.0;
        this.servingLabel = servingLabel != null ? servingLabel : "1 serving";
        this.isFavorite = false;
        this.useCount = 0;
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

    public double getKcalPer100g() {
        return kcalPer100g;
    }

    public void setKcalPer100g(double kcalPer100g) {
        this.kcalPer100g = kcalPer100g;
    }

    public double getProteinPer100g() {
        return proteinPer100g;
    }

    public void setProteinPer100g(double proteinPer100g) {
        this.proteinPer100g = proteinPer100g;
    }

    public double getCarbsPer100g() {
        return carbsPer100g;
    }

    public void setCarbsPer100g(double carbsPer100g) {
        this.carbsPer100g = carbsPer100g;
    }

    public double getFatPer100g() {
        return fatPer100g;
    }

    public void setFatPer100g(double fatPer100g) {
        this.fatPer100g = fatPer100g;
    }

    public double getFiberPer100g() {
        return fiberPer100g;
    }

    public void setFiberPer100g(double fiberPer100g) {
        this.fiberPer100g = fiberPer100g;
    }

    public double getSugarPer100g() {
        return sugarPer100g;
    }

    public void setSugarPer100g(double sugarPer100g) {
        this.sugarPer100g = sugarPer100g;
    }

    public double getDefaultServingGrams() {
        return defaultServingGrams;
    }

    public void setDefaultServingGrams(double defaultServingGrams) {
        this.defaultServingGrams = defaultServingGrams;
    }

    public String getServingLabel() {
        return servingLabel;
    }

    public void setServingLabel(String servingLabel) {
        this.servingLabel = servingLabel;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }
}
