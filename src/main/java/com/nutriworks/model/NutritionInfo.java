package com.nutriworks.model;

/**
 * Domain model representing nutritional breakdown per 100 grams for a food item.
 */
public class NutritionInfo {

    private final String name;
    private final double kcalPer100g;
    private final double proteinPer100g;
    private final double carbsPer100g;
    private final double fatPer100g;
    private final double fiberPer100g;
    private final double sugarPer100g;
    private final double defaultServingGrams;
    private final String servingLabel;

    public NutritionInfo(String name,
                         double kcalPer100g,
                         double proteinPer100g,
                         double carbsPer100g,
                         double fatPer100g,
                         double fiberPer100g,
                         double sugarPer100g,
                         double defaultServingGrams,
                         String servingLabel) {
        this.name = name != null ? name : "";
        this.kcalPer100g = Math.max(0.0, kcalPer100g);
        this.proteinPer100g = Math.max(0.0, proteinPer100g);
        this.carbsPer100g = Math.max(0.0, carbsPer100g);
        this.fatPer100g = Math.max(0.0, fatPer100g);
        this.fiberPer100g = Math.max(0.0, fiberPer100g);
        this.sugarPer100g = Math.max(0.0, sugarPer100g);
        this.defaultServingGrams = defaultServingGrams > 0.0 ? defaultServingGrams : 100.0;
        this.servingLabel = servingLabel != null && !servingLabel.trim().isEmpty() ? servingLabel : "1 serving";
    }

    public String getName() {
        return name;
    }

    public double getKcalPer100g() {
        return kcalPer100g;
    }

    public double getProteinPer100g() {
        return proteinPer100g;
    }

    public double getCarbsPer100g() {
        return carbsPer100g;
    }

    public double getFatPer100g() {
        return fatPer100g;
    }

    public double getFiberPer100g() {
        return fiberPer100g;
    }

    public double getSugarPer100g() {
        return sugarPer100g;
    }

    public double getDefaultServingGrams() {
        return defaultServingGrams;
    }

    public String getServingLabel() {
        return servingLabel;
    }

    @Override
    public String toString() {
        return "NutritionInfo{" +
                "name='" + name + '\'' +
                ", kcalPer100g=" + kcalPer100g +
                ", proteinPer100g=" + proteinPer100g +
                ", carbsPer100g=" + carbsPer100g +
                ", fatPer100g=" + fatPer100g +
                '}';
    }
}
