package com.nutriworks.app.data.api;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nutriworks.app.model.NutritionInfo;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Fallback API client interfacing with USDA FoodData Central.
 * Configured with strict 5-second timeouts so offline and connection failures
 * fail fast without degrading user experience.
 */
public class UsdaApiClient {

    private static final String TAG = "UsdaApiClient";
    private static final String BASE_URL = "https://api.nal.usda.gov/fdc/v1/";

    // Standard USDA Nutrient IDs (per 100g)
    private static final int NUTRIENT_ENERGY_KCAL = 1008;
    private static final int NUTRIENT_PROTEIN = 1003;
    private static final int NUTRIENT_FAT = 1004;
    private static final int NUTRIENT_CARBOHYDRATE = 1005;
    private static final int NUTRIENT_FIBER = 1079;
    private static final int NUTRIENT_SUGARS = 2000;

    private static UsdaFoodDataService service;

    private static synchronized UsdaFoodDataService getService() {
        if (service == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(UsdaFoodDataService.class);
        }
        return service;
    }

    /**
     * Synchronously searches USDA FoodData Central for a food term and parses
     * macronutrients into a NutritionInfo object.
     *
     * @param query Food name (e.g. "Dosa", "Apple Pie").
     * @param apiKey USDA API key from BuildConfig.
     * @return Extracted NutritionInfo or null if not found or network is offline.
     */
    @Nullable
    public static NutritionInfo fetchFoodNutrition(@NonNull String query, @NonNull String apiKey) {
        if (apiKey.trim().isEmpty() || "DEMO_KEY".equalsIgnoreCase(apiKey)) {
            Log.d(TAG, "No custom USDA API Key set in local.properties; using demo endpoint limits.");
        }

        try {
            Response<UsdaSearchResponse> response = getService()
                    .searchFoods(query, apiKey, 1)
                    .execute();

            if (!response.isSuccessful() || response.body() == null) {
                Log.w(TAG, "USDA API request unsuccessful, code: " + response.code());
                return null;
            }

            List<UsdaFoodItem> foods = response.body().getFoods();
            if (foods.isEmpty()) {
                return null;
            }

            UsdaFoodItem item = foods.get(0);
            double kcal = 0.0;
            double protein = 0.0;
            double fat = 0.0;
            double carbs = 0.0;
            double fiber = 0.0;
            double sugar = 0.0;

            if (item.getFoodNutrients() != null) {
                for (UsdaFoodNutrient nutrient : item.getFoodNutrients()) {
                    switch (nutrient.getNutrientId()) {
                        case NUTRIENT_ENERGY_KCAL:
                            kcal = nutrient.getValue();
                            break;
                        case NUTRIENT_PROTEIN:
                            protein = nutrient.getValue();
                            break;
                        case NUTRIENT_FAT:
                            fat = nutrient.getValue();
                            break;
                        case NUTRIENT_CARBOHYDRATE:
                            carbs = nutrient.getValue();
                            break;
                        case NUTRIENT_FIBER:
                            fiber = nutrient.getValue();
                            break;
                        case NUTRIENT_SUGARS:
                            sugar = nutrient.getValue();
                            break;
                    }
                }
            }

            return new NutritionInfo(
                    query,
                    kcal,
                    protein,
                    carbs,
                    fat,
                    fiber,
                    sugar,
                    100.0,
                    "1 serving (100g)"
            );

        } catch (IOException e) {
            Log.w(TAG, "USDA API network error (expected if offline): " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error querying USDA API: " + e.getMessage(), e);
            return null;
        }
    }
}
