package com.nutriworks.app;

import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.util.GoalCalculator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests validating Mifflin-St Jeor BMR calculations, TDEE multipliers,
 * caloric goal adjustments, safe minimum clamping, and macro conversions.
 */
public class GoalCalculatorTest {

    private static final double DELTA = 0.01;

    @Test
    public void calculateBmr_maleReferenceSubject_returnsAccurateMetabolicRate() {
        // 30 years old, 80 kg, 180 cm
        // BMR = (10 * 80) + (6.25 * 180) - (5 * 30) + 5
        //     = 800 + 1125 - 150 + 5 = 1780.0
        double bmr = GoalCalculator.calculateBmr(30, UserProfile.GENDER_MALE, 180.0, 80.0);
        assertEquals(1780.0, bmr, DELTA);
    }

    @Test
    public void calculateBmr_femaleReferenceSubject_returnsAccurateMetabolicRate() {
        // 25 years old, 60 kg, 165 cm
        // BMR = (10 * 60) + (6.25 * 165) - (5 * 25) - 161
        //     = 600 + 1031.25 - 125 - 161 = 1345.25
        double bmr = GoalCalculator.calculateBmr(25, UserProfile.GENDER_FEMALE, 165.0, 60.0);
        assertEquals(1345.25, bmr, DELTA);
    }

    @Test
    public void calculateTdee_variousActivityLevels_appliesCorrectMultipliers() {
        double bmr = 1780.0;

        assertEquals(2136.0, GoalCalculator.calculateTdee(bmr, UserProfile.ACTIVITY_SEDENTARY), DELTA);
        assertEquals(2447.5, GoalCalculator.calculateTdee(bmr, UserProfile.ACTIVITY_LIGHT), DELTA);
        assertEquals(2759.0, GoalCalculator.calculateTdee(bmr, UserProfile.ACTIVITY_MODERATE), DELTA);
        assertEquals(3070.5, GoalCalculator.calculateTdee(bmr, UserProfile.ACTIVITY_ACTIVE), DELTA);
        assertEquals(3382.0, GoalCalculator.calculateTdee(bmr, UserProfile.ACTIVITY_VERY_ACTIVE), DELTA);
    }

    @Test
    public void calculateDailyCalories_weightLossDeficit_subtractsExpectedDeficit() {
        double tdee = 2759.0;
        // 0.5 kg per week loss = 550 kcal/day deficit
        int calories = GoalCalculator.calculateDailyCalories(tdee, UserProfile.GOAL_LOSE, 0.5, UserProfile.GENDER_MALE);
        assertEquals(2209, calories);
    }

    @Test
    public void calculateDailyCalories_weightGainSurplus_addsExpectedSurplus() {
        double tdee = 2759.0;
        // 0.25 kg per week gain = 275 kcal/day surplus
        int calories = GoalCalculator.calculateDailyCalories(tdee, UserProfile.GOAL_GAIN, 0.25, UserProfile.GENDER_MALE);
        assertEquals(3034, calories);
    }

    @Test
    public void calculateDailyCalories_extremeDeficit_clampsToSafeMinimumFloor() {
        // Aggressive 1.5 kg/week deficit on low TDEE
        double femaleTdee = 1400.0;
        int femaleCalories = GoalCalculator.calculateDailyCalories(femaleTdee, UserProfile.GOAL_LOSE, 1.0, UserProfile.GENDER_FEMALE);
        assertEquals("Female daily calories must be clamped to safe floor of 1200 kcal",
                GoalCalculator.SAFE_MIN_CALORIES_FEMALE, femaleCalories);

        double maleTdee = 1800.0;
        int maleCalories = GoalCalculator.calculateDailyCalories(maleTdee, UserProfile.GOAL_LOSE, 1.0, UserProfile.GENDER_MALE);
        assertEquals("Male daily calories must be clamped to safe floor of 1500 kcal",
                GoalCalculator.SAFE_MIN_CALORIES_MALE, maleCalories);
    }

    @Test
    public void calculateMacroGrams_balancedSplit_convertsAccurately() {
        // 2000 kcal with Balanced split: 50% Carbs, 25% Protein, 25% Fat
        GoalCalculator.MacroTargets targets = GoalCalculator.calculateMacroGrams(2000, 0.50, 0.25, 0.25);

        // Carbs: 1000 kcal / 4 = 250g
        assertEquals(250, targets.getCarbsGrams());
        // Protein: 500 kcal / 4 = 125g
        assertEquals(125, targets.getProteinGrams());
        // Fat: 500 kcal / 9 = 56g (55.56 rounded)
        assertEquals(56, targets.getFatGrams());
    }

    @Test
    public void calculateAndApplyGoals_fullProfilePipeline_populatesAllTargetFields() {
        UserProfile profile = new UserProfile();
        profile.setAge(30);
        profile.setGender(UserProfile.GENDER_MALE);
        profile.setHeightCm(180.0);
        profile.setWeightKg(80.0);
        profile.setActivityLevel(UserProfile.ACTIVITY_MODERATE);
        profile.setGoalType(UserProfile.GOAL_LOSE);
        profile.setWeeklyRateKg(0.5);
        profile.setMacroSplit(UserProfile.SPLIT_HIGH_PROTEIN);

        UserProfile result = GoalCalculator.calculateAndApplyGoals(profile);

        assertEquals(2209, result.getTargetCalories());
        assertTrue("Target protein must be positive", result.getTargetProteinGrams() > 0);
        assertTrue("Target carbs must be positive", result.getTargetCarbsGrams() > 0);
        assertTrue("Target fat must be positive", result.getTargetFatGrams() > 0);
        assertEquals(0.35, result.getProteinRatio(), DELTA);
        assertEquals(0.40, result.getCarbRatio(), DELTA);
    }
}
