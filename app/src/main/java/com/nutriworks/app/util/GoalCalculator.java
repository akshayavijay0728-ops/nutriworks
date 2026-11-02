package com.nutriworks.app.util;

import androidx.annotation.NonNull;

import com.nutriworks.app.data.db.UserProfile;

/**
 * Metabolic and nutritional goal calculation utility based on the Mifflin-St Jeor formula,
 * physical activity multipliers, clinical safe minimum floors, and macronutrient distributions.
 */
public final class GoalCalculator {

    // Clinical safe minimum calorie thresholds (ACSM guidelines)
    public static final int SAFE_MIN_CALORIES_MALE = 1500;
    public static final int SAFE_MIN_CALORIES_FEMALE = 1200;
    public static final int SAFE_MIN_CALORIES_DEFAULT = 1200;

    // Caloric equivalent of 1 kg of body fat = ~7,700 kcal (divided by 7 days = 1,100 kcal/day per 1 kg/week)
    public static final double CALORIES_PER_KG_WEEKLY = 1100.0;

    // Macronutrient caloric densities (Atwater values)
    public static final double CALORIES_PER_GRAM_PROTEIN = 4.0;
    public static final double CALORIES_PER_GRAM_CARBS = 4.0;
    public static final double CALORIES_PER_GRAM_FAT = 9.0;

    private GoalCalculator() {
        // Prevent instantiation
    }

    /**
     * POJO containing calculated macronutrient target weights in grams.
     */
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

    /**
     * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor equation.
     * Men: BMR = (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
     * Women: BMR = (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
     */
    public static double calculateBmr(int age, @NonNull String gender, double heightCm, double weightKg) {
        if (age <= 0 || heightCm <= 0 || weightKg <= 0) {
            return 0.0;
        }

        double base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age);

        if (UserProfile.GENDER_FEMALE.equalsIgnoreCase(gender)) {
            return base - 161.0;
        } else if (UserProfile.GENDER_MALE.equalsIgnoreCase(gender)) {
            return base + 5.0;
        } else {
            // Non-binary or unspecified gender uses median offset
            return base - 78.0;
        }
    }

    /**
     * Resolves the Total Daily Energy Expenditure (TDEE) activity multiplier.
     */
    public static double getActivityMultiplier(@NonNull String activityLevel) {
        switch (activityLevel.toUpperCase()) {
            case UserProfile.ACTIVITY_SEDENTARY:
                return 1.2;
            case UserProfile.ACTIVITY_LIGHT:
                return 1.375;
            case UserProfile.ACTIVITY_ACTIVE:
                return 1.725;
            case UserProfile.ACTIVITY_VERY_ACTIVE:
                return 1.9;
            case UserProfile.ACTIVITY_MODERATE:
            default:
                return 1.55;
        }
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) from BMR and activity level.
     */
    public static double calculateTdee(double bmr, @NonNull String activityLevel) {
        return bmr * getActivityMultiplier(activityLevel);
    }

    /**
     * Resolves the minimum safe daily calorie floor to prevent metabolic slowing or nutritional deficiency.
     */
    public static int getSafeMinimumFloor(@NonNull String gender) {
        if (UserProfile.GENDER_MALE.equalsIgnoreCase(gender)) {
            return SAFE_MIN_CALORIES_MALE;
        } else if (UserProfile.GENDER_FEMALE.equalsIgnoreCase(gender)) {
            return SAFE_MIN_CALORIES_FEMALE;
        } else {
            return SAFE_MIN_CALORIES_DEFAULT;
        }
    }

    /**
     * Calculates the daily target calorie goal adjusted for weight gain, loss, or maintenance,
     * clamped by the clinical safe minimum floor.
     */
    public static int calculateDailyCalories(double tdee,
                                             @NonNull String goalType,
                                             double weeklyRateKg,
                                             @NonNull String gender) {
        double adjustment = weeklyRateKg * CALORIES_PER_KG_WEEKLY;
        double targetCalories;

        if (UserProfile.GOAL_LOSE.equalsIgnoreCase(goalType)) {
            targetCalories = tdee - adjustment;
        } else if (UserProfile.GOAL_GAIN.equalsIgnoreCase(goalType)) {
            targetCalories = tdee + adjustment;
        } else {
            // MAINTAIN
            targetCalories = tdee;
        }

        int safeFloor = getSafeMinimumFloor(gender);
        return Math.max((int) Math.round(targetCalories), safeFloor);
    }

    /**
     * Calculates macronutrient targets in grams from daily calories and split ratios.
     */
    public static MacroTargets calculateMacroGrams(int totalCalories,
                                                   double carbRatio,
                                                   double proteinRatio,
                                                   double fatRatio) {
        // Normalize ratios if sum != 1.0
        double sum = carbRatio + proteinRatio + fatRatio;
        if (sum <= 0) {
            carbRatio = 0.50;
            proteinRatio = 0.25;
            fatRatio = 0.25;
            sum = 1.0;
        }

        double cRatio = carbRatio / sum;
        double pRatio = proteinRatio / sum;
        double fRatio = fatRatio / sum;

        int carbsGrams = (int) Math.round((totalCalories * cRatio) / CALORIES_PER_GRAM_CARBS);
        int proteinGrams = (int) Math.round((totalCalories * pRatio) / CALORIES_PER_GRAM_PROTEIN);
        int fatGrams = (int) Math.round((totalCalories * fRatio) / CALORIES_PER_GRAM_FAT);

        return new MacroTargets(proteinGrams, carbsGrams, fatGrams);
    }

    /**
     * Pipeline method that calculates and assigns all metabolic targets directly to a UserProfile.
     */
    @NonNull
    public static UserProfile calculateAndApplyGoals(@NonNull UserProfile profile) {
        double bmr = calculateBmr(
                profile.getAge(),
                profile.getGender(),
                profile.getHeightCm(),
                profile.getWeightKg()
        );

        double tdee = calculateTdee(bmr, profile.getActivityLevel());

        int dailyCalories = calculateDailyCalories(
                tdee,
                profile.getGoalType(),
                profile.getWeeklyRateKg(),
                profile.getGender()
        );

        // Apply ratio presets if specified
        if (UserProfile.SPLIT_HIGH_PROTEIN.equalsIgnoreCase(profile.getMacroSplit())) {
            profile.setCarbRatio(0.40);
            profile.setProteinRatio(0.35);
            profile.setFatRatio(0.25);
        } else if (UserProfile.SPLIT_LOW_CARB.equalsIgnoreCase(profile.getMacroSplit())) {
            profile.setCarbRatio(0.25);
            profile.setProteinRatio(0.40);
            profile.setFatRatio(0.35);
        } else if (UserProfile.SPLIT_BALANCED.equalsIgnoreCase(profile.getMacroSplit())) {
            profile.setCarbRatio(0.50);
            profile.setProteinRatio(0.25);
            profile.setFatRatio(0.25);
        }

        MacroTargets macros = calculateMacroGrams(
                dailyCalories,
                profile.getCarbRatio(),
                profile.getProteinRatio(),
                profile.getFatRatio()
        );

        profile.setTargetCalories(dailyCalories);
        profile.setTargetProteinGrams(macros.getProteinGrams());
        profile.setTargetCarbsGrams(macros.getCarbsGrams());
        profile.setTargetFatGrams(macros.getFatGrams());

        return profile;
    }
}
