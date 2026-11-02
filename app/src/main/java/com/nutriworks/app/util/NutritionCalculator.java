package com.nutriworks.app.util;

import androidx.annotation.NonNull;

import com.nutriworks.app.model.NutritionInfo;

/**
 * Utility for computing dynamic calories and macronutrient breakdowns
 * based on portion grams or serving multiples.
 */
public final class NutritionCalculator {

    private NutritionCalculator() {
        // Prevent instantiation
    }

    /**
     * Value object containing scaled calories and macronutrient values.
     */
    public static class CalculatedNutrition {
        private final int calories;
        private final double proteinGrams;
        private final double carbsGrams;
        private final double fatGrams;
        private final double fiberGrams;
        private final double sugarGrams;
        private final double grams;
        private final double servings;

        public CalculatedNutrition(int calories,
                                   double proteinGrams,
                                   double carbsGrams,
                                   double fatGrams,
                                   double fiberGrams,
                                   double sugarGrams,
                                   double grams,
                                   double servings) {
            this.calories = calories;
            this.proteinGrams = proteinGrams;
            this.carbsGrams = carbsGrams;
            this.fatGrams = fatGrams;
            this.fiberGrams = fiberGrams;
            this.sugarGrams = sugarGrams;
            this.grams = grams;
            this.servings = servings;
        }

        public int getCalories() {
            return calories;
        }

        public double getProteinGrams() {
            return proteinGrams;
        }

        public double getCarbsGrams() {
            return carbsGrams;
        }

        public double getFatGrams() {
            return fatGrams;
        }

        public double getFiberGrams() {
            return fiberGrams;
        }

        public double getSugarGrams() {
            return sugarGrams;
        }

        public double getGrams() {
            return grams;
        }

        public double getServings() {
            return servings;
        }
    }

    /**
     * Computes nutrition for a specific weight in grams.
     *
     * @param info Base nutrition per 100g.
     * @param grams Weight in grams. Negative values are safely clamped to 0.0.
     * @return CalculatedNutrition result.
     */
    @NonNull
    public static CalculatedNutrition calculateFromGrams(@NonNull NutritionInfo info, double grams) {
        double safeGrams = Math.max(0.0, grams);
        double factor = safeGrams / 100.0;

        int calories = (int) Math.round(info.getKcalPer100g() * factor);
        double protein = roundToOneDecimal(info.getProteinPer100g() * factor);
        double carbs = roundToOneDecimal(info.getCarbsPer100g() * factor);
        double fat = roundToOneDecimal(info.getFatPer100g() * factor);
        double fiber = roundToOneDecimal(info.getFiberPer100g() * factor);
        double sugar = roundToOneDecimal(info.getSugarPer100g() * factor);

        double servingGrams = info.getDefaultServingGrams();
        double servings = (servingGrams > 0.0) ? roundToOneDecimal(safeGrams / servingGrams) : 1.0;

        return new CalculatedNutrition(
                calories,
                protein,
                carbs,
                fat,
                fiber,
                sugar,
                roundToOneDecimal(safeGrams),
                servings
        );
    }

    /**
     * Computes nutrition for a multiple of standard servings.
     *
     * @param info Base nutrition per 100g.
     * @param servings Multiplier (e.g. 1.5 servings). Negative values clamped to 0.0.
     * @return CalculatedNutrition result.
     */
    @NonNull
    public static CalculatedNutrition calculateFromServings(@NonNull NutritionInfo info, double servings) {
        double safeServings = Math.max(0.0, servings);
        double defaultGrams = info.getDefaultServingGrams() > 0.0 ? info.getDefaultServingGrams() : 100.0;
        double grams = safeServings * defaultGrams;
        return calculateFromGrams(info, grams);
    }

    /**
     * Rounds a double value to exactly 1 decimal place (e.g., 14.54 -> 14.5).
     */
    public static double roundToOneDecimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.round(value * 10.0) / 10.0;
    }
}
