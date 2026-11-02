package com.nutriworks.app.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for logged meal entries.
 */
@Dao
public interface MealDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MealEntry entry);

    @Update
    void update(MealEntry entry);

    @Delete
    void delete(MealEntry entry);

    @Query("DELETE FROM meal_entries WHERE id = :id")
    void deleteById(long id);

    /**
     * Retrieves all entries for a specific date range, ordered by meal type
     * (Breakfast -> Lunch -> Dinner -> Snack) then chronological timestamp.
     */
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay " +
            "ORDER BY CASE mealType WHEN 'Breakfast' THEN 1 WHEN 'Lunch' THEN 2 WHEN 'Dinner' THEN 3 WHEN 'Snack' THEN 4 ELSE 5 END, timestamp ASC")
    LiveData<List<MealEntry>> getEntriesForDate(long startOfDay, long endOfDay);

    /**
     * Synchronous variant of getEntriesForDate for background tasks and in-memory unit tests.
     */
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay " +
            "ORDER BY CASE mealType WHEN 'Breakfast' THEN 1 WHEN 'Lunch' THEN 2 WHEN 'Dinner' THEN 3 WHEN 'Snack' THEN 4 ELSE 5 END, timestamp ASC")
    List<MealEntry> getEntriesForDateSync(long startOfDay, long endOfDay);

    /**
     * Calculates aggregate sums of calories, protein, carbs, and fat for a specific date range.
     */
    @Query("SELECT COALESCE(SUM(calories), 0) AS totalCalories, " +
            "COALESCE(SUM(protein), 0.0) AS totalProtein, " +
            "COALESCE(SUM(carbs), 0.0) AS totalCarbs, " +
            "COALESCE(SUM(fat), 0.0) AS totalFat " +
            "FROM meal_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    LiveData<DailyMacroSummary> getSummaryForDate(long startOfDay, long endOfDay);

    /**
     * Synchronous variant of getSummaryForDate for tests.
     */
    @Query("SELECT COALESCE(SUM(calories), 0) AS totalCalories, " +
            "COALESCE(SUM(protein), 0.0) AS totalProtein, " +
            "COALESCE(SUM(carbs), 0.0) AS totalCarbs, " +
            "COALESCE(SUM(fat), 0.0) AS totalFat " +
            "FROM meal_entries WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    DailyMacroSummary getSummaryForDateSync(long startOfDay, long endOfDay);

    /**
     * Retrieves all entries within a date window for weekly and monthly reports.
     */
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp ASC")
    LiveData<List<MealEntry>> getEntriesBetweenDates(long startDate, long endDate);

    /**
     * Synchronous variant for tests and reporting calculations.
     */
    @Query("SELECT * FROM meal_entries WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp ASC")
    List<MealEntry> getEntriesBetweenDatesSync(long startDate, long endDate);
}
