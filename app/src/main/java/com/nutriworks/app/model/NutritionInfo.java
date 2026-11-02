package com.nutriworks.app.model;

import androidx.annotation.NonNull;

/**
 * Model representing baseline nutritional values per 100 grams
 * alongside serving size metadata.
 */
public class NutritionInfo {

    private String name;
    private double kcalPer100g;
    private double proteinPer100g;
    private double carbsPer100g;
    private double fatPer100g;
    private double fiberPer100g;
    private double sugarPer100g;
    private double defaultServingGrams;
    private String servingLabel;

    public NutritionInfo() {
    }

    public NutritionInfo(@NonNull String name,
                         double kcalPer100g,
                         double proteinPer100g,
                         double carbsPer100g,
                         double fatPer100g,
                         double fiberPer100g,
                         double sugarPer100g,
                         double defaultServingGrams,
                         @NonNull String servingLabel) {
        this.name = name;
        this.kcalPer100g = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.fiberPer100g = fiberPer100g;
        this.sugarPer100g = sugarPer100g;
        this.defaultServingGrams = defaultServingGrams;
        this.servingLabel = servingLabel;
    }

    @NonNull
    public String getName() {
        return name != null ? name : "";
    }

    public void setName(@NonNull String name) {
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

    @NonNull
    public String getServingLabel() {
        return servingLabel != null ? servingLabel : "1 serving";
    }

    public void setServingLabel(@NonNull String servingLabel) {
        this.servingLabel = servingLabel;
    }
}
