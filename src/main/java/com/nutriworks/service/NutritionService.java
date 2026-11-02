package com.nutriworks.service;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.FoodLibraryDao;
import com.nutriworks.model.FoodItem;
import com.nutriworks.model.NutritionInfo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service managing food nutritional lookups and food library caching.
 */
public class NutritionService {

    private static NutritionService instance;
    private final Map<String, NutritionInfo> localCache = new HashMap<>();
    private final FoodLibraryDao foodLibraryDao;

    public static synchronized NutritionService getInstance() {
        if (instance == null) {
            instance = new NutritionService(new FoodLibraryDao(DatabaseManager.getInstance()));
        }
        return instance;
    }

    public NutritionService(FoodLibraryDao foodLibraryDao) {
        this.foodLibraryDao = foodLibraryDao;
        loadBundledCsv();
    }

    private void loadBundledCsv() {
        InputStream is = getClass().getResourceAsStream("/nutrition.csv");
        if (is == null) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("name,")) {
                    continue;
                }

                String[] p = line.split(",");
                if (p.length >= 9) {
                    String name = p[0].trim().replace("_", " ");
                    double kcal = Double.parseDouble(p[1].trim());
                    double protein = Double.parseDouble(p[2].trim());
                    double carbs = Double.parseDouble(p[3].trim());
                    double fat = Double.parseDouble(p[4].trim());
                    double fiber = Double.parseDouble(p[5].trim());
                    double sugar = Double.parseDouble(p[6].trim());
                    double servingGrams = Double.parseDouble(p[7].trim());
                    String label = p[8].trim();

                    NutritionInfo info = new NutritionInfo(name, kcal, protein, carbs, fat, fiber, sugar, servingGrams, label);
                    localCache.put(name.toLowerCase(), info);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading nutrition.csv: " + e.getMessage());
        }
    }

    public NutritionInfo findByName(String foodName) {
        if (foodName == null || foodName.trim().isEmpty()) {
            return new NutritionInfo("Custom Food", 150.0, 5.0, 20.0, 5.0, 2.0, 1.0, 100.0, "100g");
        }

        String key = foodName.trim().toLowerCase().replace("_", " ");
        NutritionInfo hit = localCache.get(key);
        if (hit != null) {
            return hit;
        }

        // Fuzzy match
        for (Map.Entry<String, NutritionInfo> entry : localCache.entrySet()) {
            if (entry.getKey().contains(key) || key.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Check SQLite food library
        try {
            FoodItem dbItem = foodLibraryDao.findExactByName(foodName);
            if (dbItem != null) {
                NutritionInfo info = new NutritionInfo(
                        dbItem.getName(),
                        dbItem.getKcalPer100g(),
                        dbItem.getProteinPer100g(),
                        dbItem.getCarbsPer100g(),
                        dbItem.getFatPer100g(),
                        dbItem.getFiberPer100g(),
                        dbItem.getSugarPer100g(),
                        dbItem.getDefaultServingGrams(),
                        dbItem.getServingLabel()
                );
                localCache.put(key, info);
                return info;
            }
        } catch (Exception ignored) {
        }

        // Fallback default nutrition info
        return new NutritionInfo(foodName, 180.0, 8.0, 22.0, 6.0, 2.0, 1.5, 120.0, "1 serving");
    }

    public List<NutritionInfo> getAllCached() {
        return new ArrayList<>(localCache.values());
    }
}
