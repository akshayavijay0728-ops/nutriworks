package com.nutriworks.app.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing the user's demographic profile, physical metrics,
 * metabolic goals, and calculated macronutrient targets.
 */
@Entity(tableName = "user_profile")
public class UserProfile {

    public static final int SINGLETON_PROFILE_ID = 1;

    // Standard gender constants
    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";
    public static final String GENDER_OTHER = "OTHER";

    // Standard activity level constants
    public static final String ACTIVITY_SEDENTARY = "SEDENTARY";       // 1.2
    public static final String ACTIVITY_LIGHT = "LIGHT";               // 1.375
    public static final String ACTIVITY_MODERATE = "MODERATE";         // 1.55
    public static final String ACTIVITY_ACTIVE = "ACTIVE";             // 1.725
    public static final String ACTIVITY_VERY_ACTIVE = "VERY_ACTIVE";   // 1.9

    // Goal types
    public static final String GOAL_LOSE = "LOSE";
    public static final String GOAL_MAINTAIN = "MAINTAIN";
    public static final String GOAL_GAIN = "GAIN";

    // Macro split presets
    public static final String SPLIT_BALANCED = "BALANCED";         // 50% C, 25% P, 25% F
    public static final String SPLIT_HIGH_PROTEIN = "HIGH_PROTEIN"; // 40% C, 35% P, 25% F
    public static final String SPLIT_LOW_CARB = "LOW_CARB";         // 25% C, 40% P, 35% F

    @PrimaryKey
    private int id = SINGLETON_PROFILE_ID;

    private int age;
    private String gender;
    private double heightCm;
    private double weightKg;
    private String activityLevel;
    private String goalType;
    private double weeklyRateKg;

    private String macroSplit;
    private double carbRatio;
    private double proteinRatio;
    private double fatRatio;

    private int targetCalories;
    private int targetProteinGrams;
    private int targetCarbsGrams;
    private int targetFatGrams;

    private boolean isOnboardingCompleted;

    public UserProfile() {
        this.id = SINGLETON_PROFILE_ID;
        this.gender = GENDER_MALE;
        this.activityLevel = ACTIVITY_MODERATE;
        this.goalType = GOAL_MAINTAIN;
        this.macroSplit = SPLIT_BALANCED;
        this.carbRatio = 0.50;
        this.proteinRatio = 0.25;
        this.fatRatio = 0.25;
    }

    public static UserProfile createDefault() {
        UserProfile profile = new UserProfile();
        profile.setId(SINGLETON_PROFILE_ID);
        profile.setAge(28);
        profile.setGender(GENDER_MALE);
        profile.setHeightCm(175.0);
        profile.setWeightKg(75.0);
        profile.setActivityLevel(ACTIVITY_MODERATE);
        profile.setGoalType(GOAL_MAINTAIN);
        profile.setWeeklyRateKg(0.0);
        profile.setMacroSplit(SPLIT_BALANCED);
        profile.setCarbRatio(0.50);
        profile.setProteinRatio(0.25);
        profile.setFatRatio(0.25);
        profile.setTargetCalories(2150);
        profile.setTargetCarbsGrams(269);
        profile.setTargetProteinGrams(134);
        profile.setTargetFatGrams(60);
        profile.setOnboardingCompleted(false);
        return profile;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender != null ? gender : GENDER_MALE;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public double getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(double heightCm) {
        this.heightCm = heightCm;
    }

    public double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
    }

    public String getActivityLevel() {
        return activityLevel != null ? activityLevel : ACTIVITY_MODERATE;
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getGoalType() {
        return goalType != null ? goalType : GOAL_MAINTAIN;
    }

    public void setGoalType(String goalType) {
        this.goalType = goalType;
    }

    public double getWeeklyRateKg() {
        return weeklyRateKg;
    }

    public void setWeeklyRateKg(double weeklyRateKg) {
        this.weeklyRateKg = weeklyRateKg;
    }

    public String getMacroSplit() {
        return macroSplit != null ? macroSplit : SPLIT_BALANCED;
    }

    public void setMacroSplit(String macroSplit) {
        this.macroSplit = macroSplit;
    }

    public double getCarbRatio() {
        return carbRatio > 0 ? carbRatio : 0.50;
    }

    public void setCarbRatio(double carbRatio) {
        this.carbRatio = carbRatio;
    }

    public double getProteinRatio() {
        return proteinRatio > 0 ? proteinRatio : 0.25;
    }

    public void setProteinRatio(double proteinRatio) {
        this.proteinRatio = proteinRatio;
    }

    public double getFatRatio() {
        return fatRatio > 0 ? fatRatio : 0.25;
    }

    public void setFatRatio(double fatRatio) {
        this.fatRatio = fatRatio;
    }

    public int getTargetCalories() {
        return targetCalories > 0 ? targetCalories : 2000;
    }

    public void setTargetCalories(int targetCalories) {
        this.targetCalories = targetCalories;
    }

    public int getTargetProteinGrams() {
        return targetProteinGrams > 0 ? targetProteinGrams : 130;
    }

    public void setTargetProteinGrams(int targetProteinGrams) {
        this.targetProteinGrams = targetProteinGrams;
    }

    public int getTargetCarbsGrams() {
        return targetCarbsGrams > 0 ? targetCarbsGrams : 220;
    }

    public void setTargetCarbsGrams(int targetCarbsGrams) {
        this.targetCarbsGrams = targetCarbsGrams;
    }

    public int getTargetFatGrams() {
        return targetFatGrams > 0 ? targetFatGrams : 65;
    }

    public void setTargetFatGrams(int targetFatGrams) {
        this.targetFatGrams = targetFatGrams;
    }

    public boolean isOnboardingCompleted() {
        return isOnboardingCompleted;
    }

    public void setOnboardingCompleted(boolean onboardingCompleted) {
        isOnboardingCompleted = onboardingCompleted;
    }
}
