package com.nutriworks.util;

import com.nutriworks.model.UserProfile;

/**
 * Metabolic and nutritional goal calculation utility based on the Mifflin-St Jeor formula,
 * physical activity multipliers, clinical safe minimum floors, and macronutrient distributions.
 */
public final class GoalCalculator {

    public static final int SAFE_MIN_CALORIES_MALE = 1500;
    public static final int SAFE_MIN_CALORIES_FEMALE = 1200;
    public static final int SAFE_MIN_CALORIES_DEFAULT = 1200;

    public static final double CALORIES_PER_KG_WEEKLY = 1100.0;
    public static final double CALORIES_PER_GRAM_PROTEIN = 4.0;
    public static final double CALORIES_PER_GRAM_CARBS = 4.0;
    public static final double CALORIES_PER_GRAM_FAT = 9.0;

    private GoalCalculator() {
        // Prevent instantiation
    }

    public static class MacroTargets {
        private final int proteinGrams;
        private final int carbsGrams;
        private final int fatGrams;

        public MacroTargets(int proteinGrams, int carbsGrams, int fatGrams) {
            this.proteinGrams = proteinGrams;
            this.carbsGrams = carbsGrams;
            this.fatGrams = fatGrams;
        }

        public int getProteinGrams() {
            return proteinGrams;
        }

        public int getCarbsGrams() {
            return carbsGrams;
        }

        public int getFatGrams() {
            return fatGrams;
        }
    }

    public static double calculateBmr(double weightKg, double heightCm, int age, String gender) {
        if (weightKg <= 0.0 || heightCm <= 0.0 || age <= 0) {
            return 0.0;
        }

        double base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age);
        if (UserProfile.GENDER_FEMALE.equalsIgnoreCase(gender)) {
            return Math.max(0.0, base - 161.0);
        } else {
            return Math.max(0.0, base + 5.0);
        }
    }

    public static double calculateTdee(double bmr, double activityMultiplier) {
        double multiplier = activityMultiplier > 0.0 ? activityMultiplier : 1.2;
        return Math.max(0.0, bmr * multiplier);
    }

    public static int calculateDailyCalorieGoal(double tdee, String goalType, double weeklyRateKg, String gender) {
        if (tdee <= 0.0) {
            return getSafeFloorForGender(gender);
        }

        double dailyAdjustment = weeklyRateKg * CALORIES_PER_KG_WEEKLY;
        double unadjustedGoal;

        if (UserProfile.GOAL_LOSE_WEIGHT.equalsIgnoreCase(goalType)) {
            unadjustedGoal = tdee - dailyAdjustment;
        } else if (UserProfile.GOAL_GAIN_WEIGHT.equalsIgnoreCase(goalType)) {
            unadjustedGoal = tdee + dailyAdjustment;
        } else {
            unadjustedGoal = tdee;
        }

        int targetGoal = (int) Math.round(unadjustedGoal);
        int floor = getSafeFloorForGender(gender);
        return Math.max(floor, targetGoal);
    }

    public static MacroTargets calculateMacroGrams(int totalCalories, String macroSplit) {
        if (totalCalories <= 0) {
            return new MacroTargets(0, 0, 0);
        }

        double proteinRatio;
        double carbsRatio;
        double fatRatio;

        if (UserProfile.MACRO_SPLIT_HIGH_PROTEIN.equalsIgnoreCase(macroSplit)) {
            proteinRatio = 0.35;
            carbsRatio = 0.40;
            fatRatio = 0.25;
        } else if (UserProfile.MACRO_SPLIT_LOW_CARB.equalsIgnoreCase(macroSplit)) {
            proteinRatio = 0.30;
            carbsRatio = 0.25;
            fatRatio = 0.45;
        } else {
            proteinRatio = 0.25;
            carbsRatio = 0.50;
            fatRatio = 0.25;
        }

        int proteinGrams = (int) Math.round((totalCalories * proteinRatio) / CALORIES_PER_GRAM_PROTEIN);
        int carbsGrams = (int) Math.round((totalCalories * carbsRatio) / CALORIES_PER_GRAM_CARBS);
        int fatGrams = (int) Math.round((totalCalories * fatRatio) / CALORIES_PER_GRAM_FAT);

        return new MacroTargets(proteinGrams, carbsGrams, fatGrams);
    }

    public static int getSafeFloorForGender(String gender) {
        if (UserProfile.GENDER_FEMALE.equalsIgnoreCase(gender)) {
            return SAFE_MIN_CALORIES_FEMALE;
        } else if (UserProfile.GENDER_MALE.equalsIgnoreCase(gender)) {
            return SAFE_MIN_CALORIES_MALE;
        }
        return SAFE_MIN_CALORIES_DEFAULT;
    }
}
