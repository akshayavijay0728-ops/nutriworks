package com.nutriworks.app.ui.home;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.nutriworks.app.data.db.DailyMacroSummary;
import com.nutriworks.app.data.db.MealEntry;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.data.repository.MealRepository;
import com.nutriworks.app.data.repository.ProfileRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating HomeViewModel date navigation, meal mutations,
 * and live repository delegation using Mockito.
 */
public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Application mockApplication;

    @Mock
    private MealRepository mockMealRepository;

    @Mock
    private ProfileRepository mockProfileRepository;

    private HomeViewModel viewModel;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        MutableLiveData<List<MealEntry>> liveEntries = new MutableLiveData<>(Collections.emptyList());
        MutableLiveData<DailyMacroSummary> liveSummary = new MutableLiveData<>(new DailyMacroSummary(0, 0, 0, 0));
        MutableLiveData<UserProfile> liveProfile = new MutableLiveData<>(UserProfile.createDefault());

        when(mockMealRepository.getEntriesForDate(anyLong(), anyLong())).thenReturn(liveEntries);
        when(mockMealRepository.getSummaryForDate(anyLong(), anyLong())).thenReturn(liveSummary);
        when(mockProfileRepository.getProfile()).thenReturn(liveProfile);

        viewModel = new HomeViewModel(mockApplication);

        // Inject mocks via reflection
        Field mealRepoField = HomeViewModel.class.getDeclaredField("mealRepository");
        mealRepoField.setAccessible(true);
        mealRepoField.set(viewModel, mockMealRepository);

        Field profileRepoField = HomeViewModel.class.getDeclaredField("profileRepository");
        profileRepoField.setAccessible(true);
        profileRepoField.set(viewModel, mockProfileRepository);
    }

    @Test
    public void initialSelectedDate_isNearCurrentTime() {
        Long initialDate = viewModel.getSelectedDateMillis().getValue();
        assertNotNull(initialDate);
        long diff = Math.abs(System.currentTimeMillis() - initialDate);
        assertTrue("Initial timestamp should be within seconds of now", diff < 5000);
    }

    @Test
    public void previousDay_shiftsSelectedDateBackwardByOneDay() {
        long baseTime = 1760000000000L;
        viewModel.setDate(baseTime);

        viewModel.previousDay();

        Long newDate = viewModel.getSelectedDateMillis().getValue();
        assertNotNull(newDate);
        // Exactly 24 hours difference
        long expected = baseTime - (24 * 60 * 60 * 1000);
        assertEquals(expected, newDate.longValue());
    }

    @Test
    public void nextDay_shiftsSelectedDateForwardByOneDay() {
        long baseTime = 1760000000000L;
        viewModel.setDate(baseTime);

        viewModel.nextDay();

        Long newDate = viewModel.getSelectedDateMillis().getValue();
        assertNotNull(newDate);
        long expected = baseTime + (24 * 60 * 60 * 1000);
        assertEquals(expected, newDate.longValue());
    }

    @Test
    public void deleteMeal_delegatesToRepository() {
        MealEntry entry = new MealEntry("Test Dosa", 150, 200, 5, 30, 6, "Breakfast", System.currentTimeMillis(), null);
        viewModel.deleteMeal(entry);
        verify(mockMealRepository).deleteMeal(entry);
    }

    @Test
    public void undoDelete_insertsMealBackToRepository() {
        MealEntry entry = new MealEntry("Test Dosa", 150, 200, 5, 30, 6, "Breakfast", System.currentTimeMillis(), null);
        viewModel.undoDelete(entry);
        verify(mockMealRepository).insertMeal(entry, null);
    }

    @Test
    public void updateMeal_delegatesToRepository() {
        MealEntry entry = new MealEntry("Test Dosa", 200, 260, 6, 40, 8, "Breakfast", System.currentTimeMillis(), null);
        viewModel.updateMeal(entry);
        verify(mockMealRepository).updateMeal(entry);
    }
}
