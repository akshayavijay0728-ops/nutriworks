package com.nutriworks.app.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.data.repository.ProfileRepository;
import com.nutriworks.app.util.GoalCalculator;

/**
 * ViewModel managing the UserProfile state, goal recalculation, and persistence.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository profileRepository;
    private final LiveData<UserProfile> profileLiveData;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.profileRepository = new ProfileRepository(application);
        this.profileLiveData = profileRepository.getProfile();
    }

    @NonNull
    public LiveData<UserProfile> getProfile() {
        return profileLiveData;
    }

    public void saveAndRecalculateProfile(@NonNull UserProfile profile, @Nullable Runnable onComplete) {
        UserProfile updated = GoalCalculator.calculateAndApplyGoals(profile);
        updated.setOnboardingCompleted(true);
        profileRepository.saveProfile(updated, onComplete);
    }
}
