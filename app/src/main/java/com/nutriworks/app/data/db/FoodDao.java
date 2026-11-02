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
 * Data Access Object for baseline and custom user-created FoodItems.
 */
@Dao
public interface FoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FoodItem item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FoodItem> items);

    @Update
    void update(FoodItem item);

    @Delete
    void delete(FoodItem item);

    /**
     * Searches foods by partial name match using SQL LIKE operator.
     * Orders favorites and recently used items first.
     */
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY isFavorite DESC, lastUsedTimestamp DESC, name ASC")
    LiveData<List<FoodItem>> searchFoods(String query);

    /**
     * Synchronous search query for background processors and tests.
     */
    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' " +
            "ORDER BY isFavorite DESC, lastUsedTimestamp DESC, name ASC")
    List<FoodItem> searchFoodsSync(String query);

    /**
     * Retrieves all starred/favorite foods.
     */
    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    LiveData<List<FoodItem>> getFavorites();

    /**
     * Retrieves recently logged or accessed foods.
     */
    @Query("SELECT * FROM food_items ORDER BY lastUsedTimestamp DESC LIMIT :limit")
    LiveData<List<FoodItem>> getRecentFoods(int limit);

    /**
     * Returns total number of foods in the database.
     */
    @Query("SELECT COUNT(*) FROM food_items")
    int getCount();
}
