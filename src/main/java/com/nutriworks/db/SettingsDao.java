package com.nutriworks.db;

import com.google.gson.Gson;
import com.nutriworks.model.AppSettings;

import java.sql.*;

/**
 * Data Access Object for persistent key-value configuration settings in SQLite.
 */
public class SettingsDao {

    private final DatabaseManager dbManager;
    private final Gson gson = new Gson();

    public SettingsDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public AppSettings getSettings() {
        String sql = "SELECT value FROM app_settings WHERE key = 'app_config' LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String json = rs.getString("value");
                AppSettings s = gson.fromJson(json, AppSettings.class);
                if (s != null) {
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading settings: " + e.getMessage());
        }

        AppSettings defaultSettings = new AppSettings();
        saveSettings(defaultSettings);
        return defaultSettings;
    }

    public void saveSettings(AppSettings settings) {
        String json = gson.toJson(settings != null ? settings : new AppSettings());
        String sql = "INSERT OR REPLACE INTO app_settings (key, value) VALUES ('app_config', ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, json);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }
}
