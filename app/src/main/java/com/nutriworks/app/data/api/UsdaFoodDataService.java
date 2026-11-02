package com.nutriworks.app.data.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit interface for USDA FoodData Central REST API.
 */
public interface UsdaFoodDataService {

    @GET("foods/search")
    Call<UsdaSearchResponse> searchFoods(
            @Query("query") String query,
            @Query("api_key") String apiKey,
            @Query("pageSize") int pageSize
    );
}
