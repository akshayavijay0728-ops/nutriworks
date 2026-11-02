package com.nutriworks.app.data.db;

/**
 * POJO for aggregate Room queries calculating sum of calories
 * and macronutrients for a given date.
 */
public class DailyMacroSummary {

    private int totalCalories;
    private double totalProtein;
    private double totalCarbs;
    private double totalFat;

    public DailyMacroSummary() {
    }

    public DailyMacroSummary(int totalCalories, double totalProtein, double totalCarbs, double totalFat) {
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.totalCarbs = totalCarbs;
        this.totalFat = totalFat;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public double getTotalProtein() {
        return totalProtein;
    }

    public void setTotalProtein(double totalProtein) {
        this.totalProtein = totalProtein;
    }

    public double getTotalCarbs() {
        return totalCarbs;
    }

    public void setTotalCarbs(double totalCarbs) {
        this.totalCarbs = totalCarbs;
    }

    public double getTotalFat() {
        return totalFat;
    }

    public void setTotalFat(double totalFat) {
        this.totalFat = totalFat;
    }
}
