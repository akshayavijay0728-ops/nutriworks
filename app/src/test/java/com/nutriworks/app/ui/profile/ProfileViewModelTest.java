package com.nutriworks.app.ui.profile;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.data.repository.ProfileRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests validating ProfileViewModel logic using Mockito and InstantTaskExecutorRule.
 */
public class ProfileViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private Application mockApplication;

    @Mock
    private ProfileRepository mockProfileRepository;

    private ProfileViewModel viewModel;
    private MutableLiveData<UserProfile> liveDataProfile;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        liveDataProfile = new MutableLiveData<>();
        when(mockProfileRepository.getProfile()).thenReturn(liveDataProfile);

        // Instantiate ViewModel with mock Application
        viewModel = new ProfileViewModel(mockApplication);

        // Inject mockProfileRepository via reflection
        Field repositoryField = ProfileViewModel.class.getDeclaredField("profileRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(viewModel, mockProfileRepository);
    }

    @Test
    public void saveAndRecalculateProfile_recalculatesGoalsAndPersistsToRepository() {
        UserProfile inputProfile = new UserProfile();
        inputProfile.setAge(25);
        inputProfile.setGender(UserProfile.GENDER_MALE);
        inputProfile.setHeightCm(180.0);
        inputProfile.setWeightKg(80.0);
        inputProfile.setActivityLevel(UserProfile.ACTIVITY_MODERATE);
        inputProfile.setGoalType(UserProfile.GOAL_LOSE);
        inputProfile.setWeeklyRateKg(0.5);
        inputProfile.setMacroSplit(UserProfile.SPLIT_HIGH_PROTEIN);

        Runnable onComplete = () -> {};

        viewModel.saveAndRecalculateProfile(inputProfile, onComplete);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(mockProfileRepository).saveProfile(captor.capture(), eq(onComplete));

        UserProfile savedProfile = captor.getValue();
        assertTrue("Onboarding must be marked as completed", savedProfile.isOnboardingCompleted());
        assertTrue("Target calories must be calculated and non-zero", savedProfile.getTargetCalories() > 1500);
        assertTrue("Target protein must be calculated", savedProfile.getTargetProteinGrams() > 0);
        assertEquals(0.35, savedProfile.getProteinRatio(), 0.001);
    }
}
