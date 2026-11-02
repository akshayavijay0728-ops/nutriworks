package com.nutriworks.app.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.nutriworks.app.BuildConfig;
import com.nutriworks.app.data.api.UsdaApiClient;
import com.nutriworks.app.model.NutritionInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository orchestrating nutrition data resolution.
 * Prioritizes the local bundled offline nutrition.csv, falling back
 * to the USDA FoodData Central REST API when an entry is not found locally.
 */
public class NutritionRepository {

    private static final String TAG = "NutritionRepository";
    private static final String CSV_FILE_NAME = "nutrition.csv";

    public interface NutritionCallback {
        void onLoaded(@NonNull NutritionInfo info);
        void onNotFound(@NonNull String foodName);
    }

    private final Context context;
    private final ExecutorService executor;
    private final Map<String, NutritionInfo> localCache = new ConcurrentHashMap<>();
    private volatile boolean isDatabaseLoaded = false;

    public NutritionRepository(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        loadDatabaseAsync();
    }

    private void loadDatabaseAsync() {
        executor.execute(() -> {
            loadCsvDatabase();
            isDatabaseLoaded = true;
        });
    }

    private void loadCsvDatabase() {
        try (InputStream is = context.getAssets().open(CSV_FILE_NAME);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comment lines
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("name,")) {
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 9) {
                    try {
                        String name = tokens[0].trim();
                        double kcal = Double.parseDouble(tokens[1].trim());
                        double protein = Double.parseDouble(tokens[2].trim());
                        double carbs = Double.parseDouble(tokens[3].trim());
                        double fat = Double.parseDouble(tokens[4].trim());
                        double fiber = Double.parseDouble(tokens[5].trim());
                        double sugar = Double.parseDouble(tokens[6].trim());
                        double servingGrams = Double.parseDouble(tokens[7].trim());
                        String servingLabel = tokens[8].trim();

                        NutritionInfo info = new NutritionInfo(
                                name,
                                kcal,
                                protein,
                                carbs,
                                fat,
                                fiber,
                                sugar,
                                servingGrams,
                                servingLabel
                        );

                        localCache.put(normalizeKey(name), info);

                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Skipping malformed CSV row: " + line);
                    }
                }
            }
            Log.i(TAG, "Loaded " + localCache.size() + " foods into offline nutrition database.");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load " + CSV_FILE_NAME + " from assets", e);
        }
    }

    /**
     * Resolves nutrition data for a given food term asynchronously.
     *
     * @param foodName Food or dish label.
     * @param callback Callback delivering resolved NutritionInfo or not-found status.
     */
    public void getNutritionInfo(@NonNull String foodName, @NonNull NutritionCallback callback) {
        executor.execute(() -> {
            if (!isDatabaseLoaded) {
                loadCsvDatabase();
                isDatabaseLoaded = true;
            }

            String normalized = normalizeKey(foodName);
            NutritionInfo localMatch = localCache.get(normalized);

            if (localMatch != null) {
                callback.onLoaded(localMatch);
                return;
            }

            // Fallback: Query USDA FoodData Central API
            String apiKey = BuildConfig.USDA_API_KEY;
            NutritionInfo usdaMatch = UsdaApiClient.fetchFoodNutrition(foodName, apiKey);

            if (usdaMatch != null) {
                localCache.put(normalized, usdaMatch);
                callback.onLoaded(usdaMatch);
            } else {
                callback.onNotFound(foodName);
            }
        });
    }

    /**
     * Adds or updates a custom user-defined food item in memory.
     */
    public void saveCustomFood(@NonNull NutritionInfo info) {
        localCache.put(normalizeKey(info.getName()), info);
    }

    /**
     * Normalizes query string for robust map matching (e.g. "Paneer Tikka" -> "paneer_tikka").
     */
    @NonNull
    public static String normalizeKey(@NonNull String raw) {
        return raw.trim().toLowerCase(Locale.US)
                .replace(" ", "_")
                .replace("-", "_");
    }
}
