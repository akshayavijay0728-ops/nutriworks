package com.nutriworks.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.nutriworks.app.data.db.AppDatabase;
import com.nutriworks.app.data.db.DailyMacroSummary;
import com.nutriworks.app.data.db.FoodDao;
import com.nutriworks.app.data.db.FoodItem;
import com.nutriworks.app.data.db.MealDao;
import com.nutriworks.app.data.db.MealEntry;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository mediating database access for MealEntry records and FoodItem management.
 * Returns LiveData observable streams and executes all write operations on a background thread.
 */
public class MealRepository {

    public interface OnOperationCompleteCallback {
        void onComplete(long rowId);
    }

    private final MealDao mealDao;
    private final FoodDao foodDao;
    private final ExecutorService executor;

    public MealRepository(@NonNull Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.mealDao = database.mealDao();
        this.foodDao = database.foodDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Constructor for testing with mock or in-memory DAOs
    public MealRepository(@NonNull MealDao mealDao, @NonNull FoodDao foodDao) {
        this.mealDao = mealDao;
        this.foodDao = foodDao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    // --- Meal Operations ---

    @NonNull
    public LiveData<List<MealEntry>> getEntriesForDate(long startOfDay, long endOfDay) {
        return mealDao.getEntriesForDate(startOfDay, endOfDay);
    }

    @NonNull
    public LiveData<DailyMacroSummary> getSummaryForDate(long startOfDay, long endOfDay) {
        return mealDao.getSummaryForDate(startOfDay, endOfDay);
    }

    @NonNull
    public LiveData<List<MealEntry>> getEntriesBetweenDates(long startDate, long endDate) {
        return mealDao.getEntriesBetweenDates(startDate, endDate);
    }

    public void insertMeal(@NonNull MealEntry entry, @Nullable OnOperationCompleteCallback callback) {
        executor.execute(() -> {
            long id = mealDao.insert(entry);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void updateMeal(@NonNull MealEntry entry) {
        executor.execute(() -> mealDao.update(entry));
    }

    public void deleteMeal(@NonNull MealEntry entry) {
        executor.execute(() -> mealDao.delete(entry));
    }

    // --- Food Items (Manual Add, Search, Favorites) ---

    @NonNull
    public LiveData<List<FoodItem>> searchFoods(@NonNull String query) {
        return foodDao.searchFoods(query);
    }

    @NonNull
    public LiveData<List<FoodItem>> getFavorites() {
        return foodDao.getFavorites();
    }

    @NonNull
    public LiveData<List<FoodItem>> getRecentFoods(int limit) {
        return foodDao.getRecentFoods(limit);
    }

    public void insertFood(@NonNull FoodItem foodItem, @Nullable OnOperationCompleteCallback callback) {
        executor.execute(() -> {
            long id = foodDao.insert(foodItem);
            if (callback != null) {
                callback.onComplete(id);
            }
        });
    }

    public void toggleFavoriteFood(@NonNull FoodItem foodItem) {
        executor.execute(() -> {
            foodItem.setFavorite(!foodItem.isFavorite());
            foodDao.update(foodItem);
        });
    }

    public void updateFoodLastUsed(@NonNull FoodItem foodItem) {
        executor.execute(() -> {
            foodItem.setLastUsedTimestamp(System.currentTimeMillis());
            foodDao.update(foodItem);
        });
    }
}
