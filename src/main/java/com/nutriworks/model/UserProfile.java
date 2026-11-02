package com.nutriworks.model;

/**
 * Domain model representing user demographic profile, activity multipliers,
 * personalized goal selections, and calculated daily intake targets.
 */
public class UserProfile {

    public static final String GENDER_MALE = "MALE";
    public static final String GENDER_FEMALE = "FEMALE";

    public static final String GOAL_LOSE_WEIGHT = "LOSE_WEIGHT";
    public static final String GOAL_MAINTAIN_WEIGHT = "MAINTAIN_WEIGHT";
    public static final String GOAL_GAIN_WEIGHT = "GAIN_WEIGHT";

    public static final String MACRO_SPLIT_BALANCED = "BALANCED";
    public static final String MACRO_SPLIT_HIGH_PROTEIN = "HIGH_PROTEIN";
    public static final String MACRO_SPLIT_LOW_CARB = "LOW_CARB";

    private long id = 1;
    private int age = 28;
    private String gender = GENDER_MALE;
    private double heightCm = 175.0;
    private double weightKg = 72.0;
    private double activityMultiplier = 1.375;
    private String goalType = GOAL_LOSE_WEIGHT;
    private double weeklyRateKg = 0.5;
    private String macroSplit = MACRO_SPLIT_BALANCED;

    private int dailyCalorieGoal = 1850;
    private int targetProteinGrams = 115;
    private int targetCarbsGrams = 208;
    private int targetFatGrams = 62;

    public UserProfile() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
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

    public double getActivityMultiplier() {
        return activityMultiplier;
    }

    public void setActivityMultiplier(double activityMultiplier) {
        this.activityMultiplier = activityMultiplier;
    }

    public String getGoalType() {
        return goalType;
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
        return macroSplit;
    }

    public void setMacroSplit(String macroSplit) {
        this.macroSplit = macroSplit;
    }

    public int getDailyCalorieGoal() {
        return dailyCalorieGoal;
    }

    public void setDailyCalorieGoal(int dailyCalorieGoal) {
        this.dailyCalorieGoal = dailyCalorieGoal;
    }

    public int getTargetProteinGrams() {
        return targetProteinGrams;
    }

    public void setTargetProteinGrams(int targetProteinGrams) {
        this.targetProteinGrams = targetProteinGrams;
    }

    public int getTargetCarbsGrams() {
        return targetCarbsGrams;
    }

    public void setTargetCarbsGrams(int targetCarbsGrams) {
        this.targetCarbsGrams = targetCarbsGrams;
    }

    public int getTargetFatGrams() {
        return targetFatGrams;
    }

    public void setTargetFatGrams(int targetFatGrams) {
        this.targetFatGrams = targetFatGrams;
    }
}
