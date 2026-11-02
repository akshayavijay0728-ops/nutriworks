package com.nutriworks.app.data.db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Main Room Database singleton managing FoodItem, MealEntry, and UserProfile entities.
 * Includes explicit schema migrations and initial database pre-population from nutrition.csv.
 */
@Database(entities = {FoodItem.class, MealEntry.class, UserProfile.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DATABASE_NAME = "nutriworks_db";

    public abstract MealDao mealDao();
    public abstract FoodDao foodDao();
    public abstract ProfileDao profileDao();

    private static volatile AppDatabase INSTANCE;

    /**
     * Migration 1 -> 2: Adds user_profile table for Phase 6 personalization.
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`age` INTEGER NOT NULL, " +
                    "`gender` TEXT, " +
                    "`heightCm` REAL NOT NULL, " +
                    "`weightKg` REAL NOT NULL, " +
                    "`activityLevel` TEXT, " +
                    "`goalType` TEXT, " +
                    "`weeklyRateKg` REAL NOT NULL, " +
                    "`macroSplit` TEXT, " +
                    "`carbRatio` REAL NOT NULL, " +
                    "`proteinRatio` REAL NOT NULL, " +
                    "`fatRatio` REAL NOT NULL, " +
                    "`targetCalories` INTEGER NOT NULL, " +
                    "`targetProteinGrams` INTEGER NOT NULL, " +
                    "`targetCarbsGrams` INTEGER NOT NULL, " +
                    "`targetFatGrams` INTEGER NOT NULL, " +
                    "`isOnboardingCompleted` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))");
            Log.i(TAG, "Applied Room Database Migration 1 -> 2 (user_profile created).");
        }
    };

    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        prepopulateFoods(context.getApplicationContext());
                                    });
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Prepopulates food items into Room database from the assets nutrition.csv
     * and seeds a default UserProfile if none exists.
     */
    private static void prepopulateFoods(@NonNull Context context) {
        AppDatabase database = getInstance(context);
        try (InputStream is = context.getAssets().open("nutrition.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            List<FoodItem> items = new ArrayList<>();
            String line;
            long now = System.currentTimeMillis();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("name,")) {
                    continue;
                }

                String[] tokens = line.split(",");
                if (tokens.length >= 9) {
                    String name = tokens[0].trim();
                    int calories = (int) Math.round(Double.parseDouble(tokens[1].trim()));
                    double protein = Double.parseDouble(tokens[2].trim());
                    double carbs = Double.parseDouble(tokens[3].trim());
                    double fat = Double.parseDouble(tokens[4].trim());

                    items.add(new FoodItem(
                            name,
                            calories,
                            protein,
                            carbs,
                            fat,
                            false,
                            now
                    ));
                }
            }

            if (!items.isEmpty()) {
                database.foodDao().insertAll(items);
                Log.i(TAG, "Prepopulated " + items.size() + " baseline foods into Room database.");
            }

            // Seed default profile if not present
            if (database.profileDao().getProfileSync() == null) {
                database.profileDao().insertOrUpdate(UserProfile.createDefault());
                Log.i(TAG, "Seeded default UserProfile into Room database.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to prepopulate baseline foods: " + e.getMessage(), e);
        }
    }
}
