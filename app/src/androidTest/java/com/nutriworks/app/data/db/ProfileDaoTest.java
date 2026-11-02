package com.nutriworks.app.data.db;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Room in-memory DAO tests validating UserProfile persistence, singleton replacement,
 * and onboarding completion flag checks.
 */
@RunWith(AndroidJUnit4.class)
public class ProfileDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AppDatabase database;
    private ProfileDao profileDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        profileDao = database.profileDao();
    }

    @After
    public void tearDown() {
        if (database != null) {
            database.close();
        }
    }

    @Test
    public void initialDatabase_hasNoProfile() {
        assertNull(profileDao.getProfileSync());
        assertEquals(0, profileDao.isOnboardingCompleted());
    }

    @Test
    public void insertOrUpdate_storesProfileAndReportsOnboardingComplete() {
        UserProfile profile = new UserProfile();
        profile.setAge(28);
        profile.setGender(UserProfile.GENDER_MALE);
        profile.setHeightCm(178.0);
        profile.setWeightKg(76.5);
        profile.setActivityLevel(UserProfile.ACTIVITY_MODERATE);
        profile.setGoalType(UserProfile.GOAL_LOSE);
        profile.setWeeklyRateKg(0.5);
        profile.setMacroSplit(UserProfile.SPLIT_HIGH_PROTEIN);
        profile.setTargetCalories(2150);
        profile.setTargetProteinGrams(160);
        profile.setTargetCarbsGrams(200);
        profile.setTargetFatGrams(60);
        profile.setOnboardingCompleted(true);

        profileDao.insertOrUpdate(profile);

        UserProfile stored = profileDao.getProfileSync();
        assertNotNull(stored);
        assertEquals(1, stored.getId());
        assertEquals(28, stored.getAge());
        assertEquals(UserProfile.GENDER_MALE, stored.getGender());
        assertEquals(178.0, stored.getHeightCm(), 0.001);
        assertEquals(76.5, stored.getWeightKg(), 0.001);
        assertEquals(2150, stored.getTargetCalories());
        assertEquals(160, stored.getTargetProteinGrams());
        assertTrue(stored.isOnboardingCompleted());
        assertEquals(1, profileDao.isOnboardingCompleted());
    }

    @Test
    public void insertOrUpdate_overwritesExistingProfileSingleton() {
        UserProfile initial = UserProfile.createDefault();
        profileDao.insertOrUpdate(initial);

        UserProfile updated = UserProfile.createDefault();
        updated.setWeightKg(82.0);
        updated.setTargetCalories(2350);
        updated.setOnboardingCompleted(true);

        profileDao.insertOrUpdate(updated);

        UserProfile stored = profileDao.getProfileSync();
        assertNotNull(stored);
        assertEquals(82.0, stored.getWeightKg(), 0.001);
        assertEquals(2350, stored.getTargetCalories());
        assertEquals(1, profileDao.isOnboardingCompleted());
    }
}
