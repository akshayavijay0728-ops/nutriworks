package com.nutriworks.app.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.nutriworks.app.data.db.DailyMacroSummary;
import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.data.repository.MealRepository;
import com.nutriworks.app.data.repository.ProfileRepository;
import com.nutriworks.app.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel managing the daily log state, date navigation, live nutrition aggregates,
 * personalized calorie targets, and meal persistence operations for HomeFragment.
 */
public class HomeViewModel extends AndroidViewModel {

    private final MealRepository mealRepository;
    private final ProfileRepository profileRepository;

    private final MutableLiveData<Long> selectedDateMillis = new MutableLiveData<>();

    private final LiveData<List<MealEntry>> entriesForDate;
    private final LiveData<DailyMacroSummary> summaryForDate;
    private final LiveData<List<GroupedMealItem>> groupedMealsForDate;
    private final LiveData<UserProfile> userProfile;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.mealRepository = new MealRepository(application);
        this.profileRepository = new ProfileRepository(application);

        // Default to today
        this.selectedDateMillis.setValue(System.currentTimeMillis());

        this.entriesForDate = Transformations.switchMap(selectedDateMillis, date -> {
            long start = DateTimeUtils.getStartOfDay(date);
            long end = DateTimeUtils.getEndOfDay(date);
            return mealRepository.getEntriesForDate(start, end);
        });

        this.summaryForDate = Transformations.switchMap(selectedDateMillis, date -> {
            long start = DateTimeUtils.getStartOfDay(date);
            long end = DateTimeUtils.getEndOfDay(date);
            return mealRepository.getSummaryForDate(start, end);
        });

        this.groupedMealsForDate = Transformations.map(entriesForDate, this::groupMealEntries);
        this.userProfile = profileRepository.getProfile();
    }

    @NonNull
    public LiveData<Long> getSelectedDateMillis() {
        return selectedDateMillis;
    }

    @NonNull
    public LiveData<List<MealEntry>> getEntriesForDate() {
        return entriesForDate;
    }

    @NonNull
    public LiveData<DailyMacroSummary> getSummaryForDate() {
        return summaryForDate;
    }

    @NonNull
    public LiveData<List<GroupedMealItem>> getGroupedMealsForDate() {
        return groupedMealsForDate;
    }

    @NonNull
    public LiveData<UserProfile> getUserProfile() {
        return userProfile;
    }

    public void previousDay() {
        Long current = selectedDateMillis.getValue();
        if (current == null) {
            current = System.currentTimeMillis();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(current);
        cal.add(Calendar.DAY_OF_YEAR, -1);
        selectedDateMillis.setValue(cal.getTimeInMillis());
    }

    public void nextDay() {
        Long current = selectedDateMillis.getValue();
        if (current == null) {
            current = System.currentTimeMillis();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(current);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        selectedDateMillis.setValue(cal.getTimeInMillis());
    }

    public void setDate(long millis) {
        selectedDateMillis.setValue(millis);
    }

    public void goToToday() {
        selectedDateMillis.setValue(System.currentTimeMillis());
    }

    public void deleteMeal(@NonNull MealEntry entry) {
        mealRepository.deleteMeal(entry);
    }

    public void undoDelete(@NonNull MealEntry entry) {
        mealRepository.insertMeal(entry, null);
    }

    public void updateMeal(@NonNull MealEntry entry) {
        mealRepository.updateMeal(entry);
    }

    /**
     * Transforms raw flat list of MealEntry into a sectioned list with header cards
     * grouped by meal category (Breakfast, Lunch, Dinner, Snack, etc.).
     */
    private List<GroupedMealItem> groupMealEntries(List<MealEntry> rawEntries) {
        List<GroupedMealItem> groupedList = new ArrayList<>();
        if (rawEntries == null || rawEntries.isEmpty()) {
            return groupedList;
        }

        // Standard ordered categories
        String[] standardCategories = {"Breakfast", "Lunch", "Dinner", "Snack"};
        Map<String, List<MealEntry>> categoryMap = new LinkedHashMap<>();

        for (String cat : standardCategories) {
            categoryMap.put(cat, new ArrayList<>());
        }

        for (MealEntry entry : rawEntries) {
            String type = entry.getMealType();
            if (!categoryMap.containsKey(type)) {
                categoryMap.put(type, new ArrayList<>());
            }
            categoryMap.get(type).add(entry);
        }

        for (Map.Entry<String, List<MealEntry>> group : categoryMap.entrySet()) {
            List<MealEntry> entriesInGroup = group.getValue();
            if (!entriesInGroup.isEmpty()) {
                int totalGroupCalories = 0;
                for (MealEntry e : entriesInGroup) {
                    totalGroupCalories += e.getCalories();
                }

                // Add Section Header
                groupedList.add(GroupedMealItem.createHeader(group.getKey(), totalGroupCalories));

                // Add Meal Entries
                for (MealEntry e : entriesInGroup) {
                    groupedList.add(GroupedMealItem.createMeal(e));
                }
            }
        }

        return groupedList;
    }
}
