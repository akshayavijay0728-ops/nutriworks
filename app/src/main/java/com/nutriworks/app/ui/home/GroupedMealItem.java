package com.nutriworks.app.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nutriworks.app.data.db.MealEntry;

/**
 * Adapter item model representing either a meal type section header
 * (e.g. "Breakfast • 450 kcal") or an individual logged MealEntry.
 */
public class GroupedMealItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_MEAL = 1;

    private final int type;
    private final String headerTitle;
    private final int headerCalories;
    private final MealEntry mealEntry;

    public static GroupedMealItem createHeader(@NonNull String title, int calories) {
        return new GroupedMealItem(TYPE_HEADER, title, calories, null);
    }

    public static GroupedMealItem createMeal(@NonNull MealEntry entry) {
        return new GroupedMealItem(TYPE_MEAL, null, 0, entry);
    }

    private GroupedMealItem(int type,
                            @Nullable String headerTitle,
                            int headerCalories,
                            @Nullable MealEntry mealEntry) {
        this.type = type;
        this.headerTitle = headerTitle;
        this.headerCalories = headerCalories;
        this.mealEntry = mealEntry;
    }

    public int getType() {
        return type;
    }

    @Nullable
    public String getHeaderTitle() {
        return headerTitle;
    }

    public int getHeaderCalories() {
        return headerCalories;
    }

    @Nullable
    public MealEntry getMealEntry() {
        return mealEntry;
    }
}
