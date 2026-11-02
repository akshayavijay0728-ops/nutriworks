package com.nutriworks.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.nutriworks.app.data.db.AppDatabase;
import com.nutriworks.app.data.db.ProfileDao;
import com.nutriworks.app.data.db.UserProfile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository mediating access to UserProfile data in Room.
 * Provides LiveData streams for UI observation and executes persistence on a background thread.
 */
public class ProfileRepository {

    private final ProfileDao profileDao;
    private final ExecutorService executor;

    public ProfileRepository(@NonNull Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.profileDao = database.profileDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public ProfileRepository(@NonNull ProfileDao profileDao) {
        this.profileDao = profileDao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @NonNull
    public LiveData<UserProfile> getProfile() {
        return profileDao.getProfile();
    }

    @Nullable
    public UserProfile getProfileSync() {
        return profileDao.getProfileSync();
    }

    public boolean isOnboardingCompletedSync() {
        return profileDao.isOnboardingCompleted() > 0;
    }

    public void saveProfile(@NonNull UserProfile profile, @Nullable Runnable onComplete) {
        executor.execute(() -> {
            profileDao.insertOrUpdate(profile);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }
}
