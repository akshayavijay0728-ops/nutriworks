package com.nutriworks.db;

import com.nutriworks.model.UserProfile;

import java.sql.*;

/**
 * Data Access Object for UserProfile singleton persistence in SQLite.
 */
public class UserProfileDao {

    private final DatabaseManager dbManager;

    public UserProfileDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public UserProfile getProfile() {
        String sql = "SELECT * FROM user_profile WHERE id = 1 LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                UserProfile p = new UserProfile();
                p.setId(rs.getLong("id"));
                p.setAge(rs.getInt("age"));
                p.setGender(rs.getString("gender"));
                p.setHeightCm(rs.getDouble("height_cm"));
                p.setWeightKg(rs.getDouble("weight_kg"));
                p.setActivityMultiplier(rs.getDouble("activity_multiplier"));
                p.setGoalType(rs.getString("goal_type"));
                p.setWeeklyRateKg(rs.getDouble("weekly_rate_kg"));
                p.setMacroSplit(rs.getString("macro_split"));
                p.setDailyCalorieGoal(rs.getInt("daily_calorie_goal"));
                p.setTargetProteinGrams(rs.getInt("target_protein"));
                p.setTargetCarbsGrams(rs.getInt("target_carbs"));
                p.setTargetFatGrams(rs.getInt("target_fat"));
                return p;
            }
        } catch (SQLException e) {
            System.err.println("Error reading user profile: " + e.getMessage());
        }

        // Return default profile if table is empty
        UserProfile defaultProfile = new UserProfile();
        saveProfile(defaultProfile);
        return defaultProfile;
    }

    public void saveProfile(UserProfile p) {
        String sql = "INSERT OR REPLACE INTO user_profile (id, age, gender, height_cm, weight_kg, activity_multiplier, goal_type, weekly_rate_kg, macro_split, daily_calorie_goal, target_protein, target_carbs, target_fat) " +
                "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getAge());
            ps.setString(2, p.getGender());
            ps.setDouble(3, p.getHeightCm());
            ps.setDouble(4, p.getWeightKg());
            ps.setDouble(5, p.getActivityMultiplier());
            ps.setString(6, p.getGoalType());
            ps.setDouble(7, p.getWeeklyRateKg());
            ps.setString(8, p.getMacroSplit());
            ps.setInt(9, p.getDailyCalorieGoal());
            ps.setInt(10, p.getTargetProteinGrams());
            ps.setInt(11, p.getTargetCarbsGrams());
            ps.setInt(12, p.getTargetFatGrams());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving user profile: " + e.getMessage());
        }
    }
}
