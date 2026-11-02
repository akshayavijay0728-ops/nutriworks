package com.nutriworks.service;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.MealDao;
import com.nutriworks.model.MealEntry;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Service coordinating meal tracking, day aggregation, and Undo stack operations.
 */
public class MealTrackingService {

    public static class DailySummary {
        private final int totalCalories;
        private final double totalProtein;
        private final double totalCarbs;
        private final double totalFat;
        private final int mealCount;

        public DailySummary(int totalCalories, double totalProtein, double totalCarbs, double totalFat, int mealCount) {
            this.totalCalories = totalCalories;
            this.totalProtein = totalProtein;
            this.totalCarbs = totalCarbs;
            this.totalFat = totalFat;
            this.mealCount = mealCount;
        }

        public int getTotalCalories() {
            return totalCalories;
        }

        public double getTotalProtein() {
            return totalProtein;
        }

        public double getTotalCarbs() {
            return totalCarbs;
        }

        public double getTotalFat() {
            return totalFat;
        }

        public int getMealCount() {
            return mealCount;
        }
    }

    private static MealTrackingService instance;
    private final MealDao mealDao;
    private final Deque<MealEntry> undoDeleteStack = new ArrayDeque<>();

    public static synchronized MealTrackingService getInstance() {
        if (instance == null) {
            instance = new MealTrackingService(new MealDao(DatabaseManager.getInstance()));
        }
        return instance;
    }

    public MealTrackingService(MealDao mealDao) {
        this.mealDao = mealDao;
    }

    public MealEntry logMeal(MealEntry entry) throws SQLException {
        return mealDao.insert(entry);
    }

    public boolean updateMeal(MealEntry entry) throws SQLException {
        return mealDao.update(entry);
    }

    public boolean moveMealType(long mealId, String newMealType) throws SQLException {
        return mealDao.updateMealType(mealId, newMealType);
    }

    public boolean deleteMealWithUndo(MealEntry entry) throws SQLException {
        if (entry == null) return false;
        boolean deleted = mealDao.delete(entry.getId());
        if (deleted) {
            undoDeleteStack.push(entry);
        }
        return deleted;
    }

    public MealEntry undoLastDelete() throws SQLException {
        if (undoDeleteStack.isEmpty()) {
            return null;
        }
        MealEntry restored = undoDeleteStack.pop();
        return mealDao.insert(restored);
    }

    public boolean hasUndoableDelete() {
        return !undoDeleteStack.isEmpty();
    }

    public List<MealEntry> getMealsForDate(LocalDate date) throws SQLException {
        return mealDao.getMealsForDate(date);
    }

    public DailySummary getSummaryForDate(LocalDate date) throws SQLException {
        List<MealEntry> meals = getMealsForDate(date);
        int cals = 0;
        double p = 0.0, c = 0.0, f = 0.0;
        for (MealEntry m : meals) {
            cals += m.getCalories();
            p += m.getProtein();
            c += m.getCarbs();
            f += m.getFat();
        }
        return new DailySummary(cals, p, c, f, meals.size());
    }

    public int copyYesterdayMeals(LocalDate targetDate) throws SQLException {
        LocalDate yesterday = targetDate.minusDays(1);
        return mealDao.copyMeals(yesterday, targetDate);
    }
}
