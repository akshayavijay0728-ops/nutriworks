package com.nutriworks.db;

import com.nutriworks.model.FoodItem;
import com.nutriworks.model.NutritionInfo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for FoodItem library search and management in SQLite.
 */
public class FoodLibraryDao {

    private final DatabaseManager dbManager;

    public FoodLibraryDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        ensurePrepopulated();
    }

    public synchronized void ensurePrepopulated() {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM food_items")) {
            if (rs.next() && rs.getInt(1) == 0) {
                prepopulateFromCsv(conn);
            }
        } catch (SQLException e) {
            System.err.println("Failed checking food library population: " + e.getMessage());
        }
    }

    private void prepopulateFromCsv(Connection conn) {
        InputStream is = getClass().getResourceAsStream("/nutrition.csv");
        if (is == null) {
            return;
        }

        String insertSql = "INSERT OR IGNORE INTO food_items (name, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, sugar_per_100g, default_serving_grams, serving_label) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement(insertSql)) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("name,")) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length >= 9) {
                    ps.setString(1, parts[0].trim().replace("_", " "));
                    ps.setDouble(2, Double.parseDouble(parts[1].trim()));
                    ps.setDouble(3, Double.parseDouble(parts[2].trim()));
                    ps.setDouble(4, Double.parseDouble(parts[3].trim()));
                    ps.setDouble(5, Double.parseDouble(parts[4].trim()));
                    ps.setDouble(6, Double.parseDouble(parts[5].trim()));
                    ps.setDouble(7, Double.parseDouble(parts[6].trim()));
                    ps.setDouble(8, Double.parseDouble(parts[7].trim()));
                    ps.setString(9, parts[8].trim());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        } catch (Exception e) {
            System.err.println("Error seeding food library from CSV: " + e.getMessage());
        }
    }

    public List<FoodItem> searchByName(String query, int limit) throws SQLException {
        String sql = "SELECT * FROM food_items WHERE name LIKE ? ORDER BY use_count DESC, name ASC LIMIT ?";
        List<FoodItem> list = new ArrayList<>();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + (query != null ? query.trim() : "") + "%");
            ps.setInt(2, limit > 0 ? limit : 20);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public FoodItem findExactByName(String name) throws SQLException {
        String sql = "SELECT * FROM food_items WHERE LOWER(name) = LOWER(?) LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name != null ? name.trim() : "");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<FoodItem> getAllFoods() throws SQLException {
        String sql = "SELECT * FROM food_items ORDER BY is_favorite DESC, name ASC";
        List<FoodItem> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void insert(FoodItem item) throws SQLException {
        String sql = "INSERT OR REPLACE INTO food_items (name, kcal_per_100g, protein_per_100g, carbs_per_100g, fat_per_100g, fiber_per_100g, sugar_per_100g, default_serving_grams, serving_label, is_favorite, use_count) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setDouble(2, item.getKcalPer100g());
            ps.setDouble(3, item.getProteinPer100g());
            ps.setDouble(4, item.getCarbsPer100g());
            ps.setDouble(5, item.getFatPer100g());
            ps.setDouble(6, item.getFiberPer100g());
            ps.setDouble(7, item.getSugarPer100g());
            ps.setDouble(8, item.getDefaultServingGrams());
            ps.setString(9, item.getServingLabel());
            ps.setInt(10, item.isFavorite() ? 1 : 0);
            ps.setInt(11, item.getUseCount());
            ps.executeUpdate();
        }
    }

    public void incrementUseCount(String name) {
        String sql = "UPDATE food_items SET use_count = use_count + 1 WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    public void toggleFavorite(long id, boolean isFavorite) throws SQLException {
        String sql = "UPDATE food_items SET is_favorite = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isFavorite ? 1 : 0);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private FoodItem mapRow(ResultSet rs) throws SQLException {
        FoodItem item = new FoodItem();
        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setKcalPer100g(rs.getDouble("kcal_per_100g"));
        item.setProteinPer100g(rs.getDouble("protein_per_100g"));
        item.setCarbsPer100g(rs.getDouble("carbs_per_100g"));
        item.setFatPer100g(rs.getDouble("fat_per_100g"));
        item.setFiberPer100g(rs.getDouble("fiber_per_100g"));
        item.setSugarPer100g(rs.getDouble("sugar_per_100g"));
        item.setDefaultServingGrams(rs.getDouble("default_serving_grams"));
        item.setServingLabel(rs.getString("serving_label"));
        item.setFavorite(rs.getInt("is_favorite") == 1);
        item.setUseCount(rs.getInt("use_count"));
        return item;
    }
}
