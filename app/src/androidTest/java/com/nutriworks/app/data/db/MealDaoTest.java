package com.nutriworks.app.data.db;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nutriworks.app.util.DateTimeUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Room in-memory DAO tests validating date-range queries, daily macro summaries,
 * and meal category ordering.
 */
@RunWith(AndroidJUnit4.class)
public class MealDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private MealDao mealDao;
    private FoodDao foodDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        // Create an in-memory database instance for testing
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        mealDao = database.mealDao();
        foodDao = database.foodDao();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void insertAndRetrieveEntriesForDate_filtersByDayAndOrdersByCategory() {
        // Setup Day 1 base timestamp
        Calendar calDay1 = Calendar.getInstance();
        calDay1.set(2026, Calendar.OCTOBER, 15, 8, 30, 0); // 8:30 AM
        long day1BreakfastTime = calDay1.getTimeInMillis();

        calDay1.set(2026, Calendar.OCTOBER, 15, 13, 0, 0); // 1:00 PM
        long day1LunchTime = calDay1.getTimeInMillis();

        // Setup Day 2 base timestamp
        Calendar calDay2 = Calendar.getInstance();
        calDay2.set(2026, Calendar.OCTOBER, 16, 9, 0, 0);
        long day2BreakfastTime = calDay2.getTimeInMillis();

        // Insert items
        MealEntry entry1 = new MealEntry("Idli & Sambar", 150, 220, 8.0, 42.0, 2.0, "Breakfast", day1BreakfastTime, null);
        MealEntry entry2 = new MealEntry("Chicken Biryani", 350, 580, 32.0, 68.0, 18.0, "Lunch", day1LunchTime, null);
        MealEntry entry3 = new MealEntry("Oatmeal & Banana", 200, 260, 9.0, 50.0, 4.0, "Breakfast", day2BreakfastTime, null);

        mealDao.insert(entry1);
        mealDao.insert(entry2);
        mealDao.insert(entry3);

        // Query Day 1
        long startDay1 = DateTimeUtils.getStartOfDay(day1BreakfastTime);
        long endDay1 = DateTimeUtils.getEndOfDay(day1BreakfastTime);
        List<MealEntry> day1Entries = mealDao.getEntriesForDateSync(startDay1, endDay1);

        assertEquals("Day 1 must return exactly 2 logged meals", 2, day1Entries.size());
        assertEquals("First entry should be Breakfast", "Breakfast", day1Entries.get(0).getMealType());
        assertEquals("Second entry should be Lunch", "Lunch", day1Entries.get(1).getMealType());
        assertEquals("Idli & Sambar", day1Entries.get(0).getFoodName());

        // Query Day 2
        long startDay2 = DateTimeUtils.getStartOfDay(day2BreakfastTime);
        long endDay2 = DateTimeUtils.getEndOfDay(day2BreakfastTime);
        List<MealEntry> day2Entries = mealDao.getEntriesForDateSync(startDay2, endDay2);

        assertEquals("Day 2 must return exactly 1 meal", 1, day2Entries.size());
        assertEquals("Oatmeal & Banana", day2Entries.get(0).getFoodName());
    }

    @Test
    public void getSummaryForDate_correctlySumsCaloriesAndMacronutrients() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 15, 8, 0, 0);
        long timestamp = cal.getTimeInMillis();

        MealEntry meal1 = new MealEntry("Eggs & Toast", 120, 280, 16.0, 24.0, 12.0, "Breakfast", timestamp, null);
        MealEntry meal2 = new MealEntry("Salmon Salad", 250, 420, 36.0, 14.0, 22.0, "Lunch", timestamp + 14400000, null);

        mealDao.insert(meal1);
        mealDao.insert(meal2);

        long startOfDay = DateTimeUtils.getStartOfDay(timestamp);
        long endOfDay = DateTimeUtils.getEndOfDay(timestamp);
        DailyMacroSummary summary = mealDao.getSummaryForDateSync(startOfDay, endOfDay);

        assertNotNull("Daily macro summary must not be null", summary);
        assertEquals(700, summary.getTotalCalories());
        assertEquals(52.0, summary.getTotalProtein(), 0.001);
        assertEquals(38.0, summary.getTotalCarbs(), 0.001);
        assertEquals(34.0, summary.getTotalFat(), 0.001);
    }

    @Test
    public void getSummaryForDate_emptyDayReturnsZeroWithoutCrash() {
        long timestamp = System.currentTimeMillis();
        long startOfDay = DateTimeUtils.getStartOfDay(timestamp);
        long endOfDay = DateTimeUtils.getEndOfDay(timestamp);

        DailyMacroSummary summary = mealDao.getSummaryForDateSync(startOfDay, endOfDay);

        assertNotNull("Daily macro summary on empty date must return non-null object", summary);
        assertEquals(0, summary.getTotalCalories());
        assertEquals(0.0, summary.getTotalProtein(), 0.001);
        assertEquals(0.0, summary.getTotalCarbs(), 0.001);
        assertEquals(0.0, summary.getTotalFat(), 0.001);
    }

    @Test
    public void getEntriesBetweenDates_returnsRangeAcrossMultipleDays() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2026, Calendar.OCTOBER, 10, 12, 0, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2026, Calendar.OCTOBER, 12, 12, 0, 0);

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2026, Calendar.OCTOBER, 20, 12, 0, 0);

        mealDao.insert(new MealEntry("Meal 1", 100, 200, 10, 20, 5, "Lunch", cal1.getTimeInMillis(), null));
        mealDao.insert(new MealEntry("Meal 2", 150, 300, 15, 30, 8, "Lunch", cal2.getTimeInMillis(), null));
        mealDao.insert(new MealEntry("Meal 3", 200, 400, 20, 40, 10, "Lunch", cal3.getTimeInMillis(), null));

        long windowStart = DateTimeUtils.getStartOfDay(cal1.getTimeInMillis());
        long windowEnd = DateTimeUtils.getEndOfDay(cal2.getTimeInMillis());

        List<MealEntry> rangeResults = mealDao.getEntriesBetweenDatesSync(windowStart, windowEnd);
        assertEquals("Should retrieve exactly meals inside the Oct 10 - Oct 12 window", 2, rangeResults.size());
    }

    @Test
    public void foodDao_searchAndFavoritesFunctionality() {
        foodDao.insert(new FoodItem("Plain Dosa", 150, 3.5, 28.0, 2.5, false, 1000L));
        foodDao.insert(new FoodItem("Masala Dosa", 175, 4.0, 32.0, 4.0, true, 2000L));
        foodDao.insert(new FoodItem("Butter Chicken", 240, 16.0, 8.0, 16.0, false, 3000L));

        List<FoodItem> searchResults = foodDao.searchFoodsSync("Dosa");
        assertEquals(2, searchResults.size());
        assertEquals("Favorite food should appear first in search results", "Masala Dosa", searchResults.get(0).getName());
        assertTrue(searchResults.get(0).isFavorite());
    }
}
