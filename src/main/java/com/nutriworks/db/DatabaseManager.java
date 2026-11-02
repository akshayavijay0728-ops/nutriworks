package com.nutriworks.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite Database Manager with automated schema migrations and versioning.
 * Stores data locally in user app data directory: .nutriworks/nutriworks.db
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private final String dbUrl;

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, ".nutriworks");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            File dbFile = new File(appDir, "nutriworks.db");
            instance = new DatabaseManager("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        return instance;
    }

    public static synchronized DatabaseManager createInMemory() {
        return new DatabaseManager("jdbc:sqlite::memory:");
    }

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        initializeSchema();
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        }
        return conn;
    }

    private void initializeSchema() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Create schema_version table
            stmt.execute("CREATE TABLE IF NOT EXISTS schema_version (" +
                    "version INTEGER PRIMARY KEY, " +
                    "applied_at INTEGER NOT NULL" +
                    ");");

            int currentVersion = getCurrentVersion(conn);

            // Migration V1: Core tables
            if (currentVersion < 1) {
                stmt.execute("CREATE TABLE IF NOT EXISTS food_items (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL UNIQUE, " +
                        "kcal_per_100g REAL NOT NULL, " +
                        "protein_per_100g REAL NOT NULL, " +
                        "carbs_per_100g REAL NOT NULL, " +
                        "fat_per_100g REAL NOT NULL, " +
                        "fiber_per_100g REAL NOT NULL, " +
                        "sugar_per_100g REAL NOT NULL, " +
                        "default_serving_grams REAL NOT NULL, " +
                        "serving_label TEXT NOT NULL, " +
                        "is_favorite INTEGER DEFAULT 0, " +
                        "use_count INTEGER DEFAULT 0" +
                        ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS meal_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "food_name TEXT NOT NULL, " +
                        "grams REAL NOT NULL, " +
                        "calories INTEGER NOT NULL, " +
                        "protein REAL NOT NULL, " +
                        "carbs REAL NOT NULL, " +
                        "fat REAL NOT NULL, " +
                        "meal_type TEXT NOT NULL, " +
                        "timestamp INTEGER NOT NULL, " +
                        "image_path TEXT, " +
                        "source TEXT DEFAULT 'MANUAL'" +
                        ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS user_profile (" +
                        "id INTEGER PRIMARY KEY, " +
                        "age INTEGER NOT NULL, " +
                        "gender TEXT NOT NULL, " +
                        "height_cm REAL NOT NULL, " +
                        "weight_kg REAL NOT NULL, " +
                        "activity_multiplier REAL NOT NULL, " +
                        "goal_type TEXT NOT NULL, " +
                        "weekly_rate_kg REAL NOT NULL, " +
                        "macro_split TEXT NOT NULL, " +
                        "daily_calorie_goal INTEGER NOT NULL, " +
                        "target_protein INTEGER NOT NULL, " +
                        "target_carbs INTEGER NOT NULL, " +
                        "target_fat INTEGER NOT NULL" +
                        ");");

                stmt.execute("CREATE TABLE IF NOT EXISTS app_settings (" +
                        "key TEXT PRIMARY KEY, " +
                        "value TEXT NOT NULL" +
                        ");");

                stmt.execute("INSERT OR REPLACE INTO schema_version (version, applied_at) VALUES (1, " + System.currentTimeMillis() + ");");
            }

            // Migration V2: Add indexes for fast date queries
            if (currentVersion < 2) {
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_meal_timestamp ON meal_entries(timestamp);");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_food_name ON food_items(name);");
                stmt.execute("INSERT OR REPLACE INTO schema_version (version, applied_at) VALUES (2, " + System.currentTimeMillis() + ");");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database schema", e);
        }
    }

    private int getCurrentVersion(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
}
