package com.nutriworks.db;

import com.nutriworks.model.MealEntry;

import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for MealEntry persistence in SQLite.
 */
public class MealDao {

    private final DatabaseManager dbManager;

    public MealDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public MealEntry insert(MealEntry entry) throws SQLException {
        String sql = "INSERT INTO meal_entries (food_name, grams, calories, protein, carbs, fat, meal_type, timestamp, image_path, source) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entry.getFoodName());
            ps.setDouble(2, entry.getGrams());
            ps.setInt(3, entry.getCalories());
            ps.setDouble(4, entry.getProtein());
            ps.setDouble(5, entry.getCarbs());
            ps.setDouble(6, entry.getFat());
            ps.setString(7, entry.getMealType());
            ps.setLong(8, entry.getTimestamp());
            ps.setString(9, entry.getImagePath());
            ps.setString(10, entry.getSource().name());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entry.setId(rs.getLong(1));
                }
            }
        }
        return entry;
    }

    public boolean update(MealEntry entry) throws SQLException {
        String sql = "UPDATE meal_entries SET food_name=?, grams=?, calories=?, protein=?, carbs=?, fat=?, meal_type=?, timestamp=?, image_path=?, source=? WHERE id=?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entry.getFoodName());
            ps.setDouble(2, entry.getGrams());
            ps.setInt(3, entry.getCalories());
            ps.setDouble(4, entry.getProtein());
            ps.setDouble(5, entry.getCarbs());
            ps.setDouble(6, entry.getFat());
            ps.setString(7, entry.getMealType());
            ps.setLong(8, entry.getTimestamp());
            ps.setString(9, entry.getImagePath());
            ps.setString(10, entry.getSource().name());
            ps.setLong(11, entry.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateMealType(long mealId, String newMealType) throws SQLException {
        String sql = "UPDATE meal_entries SET meal_type=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newMealType);
            ps.setLong(2, mealId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(long id) throws SQLException {
        String sql = "DELETE FROM meal_entries WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<MealEntry> getMealsForDate(LocalDate date) throws SQLException {
        long startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;

        return getMealsBetween(startOfDay, endOfDay);
    }

    public List<MealEntry> getMealsBetween(long startTimestamp, long endTimestamp) throws SQLException {
        String sql = "SELECT id, food_name, grams, calories, protein, carbs, fat, meal_type, timestamp, image_path, source " +
                "FROM meal_entries WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC";

        List<MealEntry> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, startTimestamp);
            ps.setLong(2, endTimestamp);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MealEntry entry = new MealEntry();
                    entry.setId(rs.getLong("id"));
                    entry.setFoodName(rs.getString("food_name"));
                    entry.setGrams(rs.getDouble("grams"));
                    entry.setCalories(rs.getInt("calories"));
                    entry.setProtein(rs.getDouble("protein"));
                    entry.setCarbs(rs.getDouble("carbs"));
                    entry.setFat(rs.getDouble("fat"));
                    entry.setMealType(rs.getString("meal_type"));
                    entry.setTimestamp(rs.getLong("timestamp"));
                    entry.setImagePath(rs.getString("image_path"));

                    String srcStr = rs.getString("source");
                    try {
                        entry.setSource(MealEntry.Source.valueOf(srcStr));
                    } catch (Exception e) {
                        entry.setSource(MealEntry.Source.MANUAL);
                    }
                    list.add(entry);
                }
            }
        }
        return list;
    }

    public int copyMeals(LocalDate sourceDate, LocalDate targetDate) throws SQLException {
        List<MealEntry> sourceMeals = getMealsForDate(sourceDate);
        if (sourceMeals.isEmpty()) {
            return 0;
        }

        long targetStartOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        int count = 0;

        for (MealEntry old : sourceMeals) {
            MealEntry clone = new MealEntry(
                    old.getFoodName(),
                    old.getGrams(),
                    old.getCalories(),
                    old.getProtein(),
                    old.getCarbs(),
                    old.getFat(),
                    old.getMealType(),
                    targetStartOfDay + (count * 60000L), // spaced by minutes
                    old.getImagePath(),
                    MealEntry.Source.MANUAL
            );
            insert(clone);
            count++;
        }
        return count;
    }
}
