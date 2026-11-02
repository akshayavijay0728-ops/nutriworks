package com.nutriworks.util;

import com.nutriworks.model.NutritionInfo;

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

        public double getProteinPercentage() {
            double totalMacros = proteinGrams + carbsGrams + fatGrams;
            return totalMacros > 0.0 ? (proteinGrams / totalMacros) * 100.0 : 0.0;
        }

        public double getCarbsPercentage() {
            double totalMacros = proteinGrams + carbsGrams + fatGrams;
            return totalMacros > 0.0 ? (carbsGrams / totalMacros) * 100.0 : 0.0;
        }

        public double getFatPercentage() {
            double totalMacros = proteinGrams + carbsGrams + fatGrams;
            return totalMacros > 0.0 ? (fatGrams / totalMacros) * 100.0 : 0.0;
        }
    }

    public static CalculatedNutrition calculateFromGrams(NutritionInfo info, double grams) {
        if (info == null || grams <= 0.0) {
            return new CalculatedNutrition(0, 0.0, 0.0, 0.0, 0.0, 0.0, Math.max(0.0, grams), 0.0);
        }

        double ratio = grams / 100.0;
        int calories = (int) Math.round(info.getKcalPer100g() * ratio);
        double protein = roundToOneDecimal(info.getProteinPer100g() * ratio);
        double carbs = roundToOneDecimal(info.getCarbsPer100g() * ratio);
        double fat = roundToOneDecimal(info.getFatPer100g() * ratio);
        double fiber = roundToOneDecimal(info.getFiberPer100g() * ratio);
        double sugar = roundToOneDecimal(info.getSugarPer100g() * ratio);
        double servings = info.getDefaultServingGrams() > 0.0 ? grams / info.getDefaultServingGrams() : 1.0;

        return new CalculatedNutrition(calories, protein, carbs, fat, fiber, sugar, grams, roundToOneDecimal(servings));
    }

    public static CalculatedNutrition calculateFromServings(NutritionInfo info, double servings) {
        if (info == null || servings <= 0.0) {
            return new CalculatedNutrition(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Math.max(0.0, servings));
        }

        double grams = servings * info.getDefaultServingGrams();
        return calculateFromGrams(info, grams);
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
