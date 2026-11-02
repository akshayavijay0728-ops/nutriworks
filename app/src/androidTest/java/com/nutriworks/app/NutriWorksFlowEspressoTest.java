package com.nutriworks.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.nutriworks.app.data.db.AppDatabase;
import com.nutriworks.app.data.db.UserProfile;
import com.nutriworks.app.ui.capture.ResultFragment;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * End-to-End Espresso UI integration test executing the complete meal logging flow:
 * Mock food image -> ResultFragment recognition -> Add to Log -> Verify entry on Home.
 */
@RunWith(AndroidJUnit4.class)
public class NutriWorksFlowEspressoTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    private String mockImagePath;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Ensure Onboarding is completed so MainActivity boots directly to Home
        UserProfile profile = UserProfile.createDefault();
        profile.setOnboardingCompleted(true);
        profile.setTargetCalories(2150);
        AppDatabase.getInstance(context).profileDao().insertOrUpdate(profile);

        // Generate synthetic mock food photo for inference test
        File cacheDir = context.getCacheDir();
        File testImageFile = new File(cacheDir, "espresso_mock_food.jpg");

        Bitmap bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.rgb(200, 140, 50)); // Food-like golden brown
        canvas.drawRect(0, 0, 224, 224, paint);

        try (FileOutputStream fos = new FileOutputStream(testImageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
        bitmap.recycle();

        mockImagePath = testImageFile.getAbsolutePath();
    }

    @Test
    public void executeMealLoggingFlow_fromResultScreen_addsMealAndUpdatesHomeScreen() throws Exception {
        // Navigate to ResultFragment with the mock image path argument
        activityRule.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
            Bundle args = new Bundle();
            args.putString(ResultFragment.ARG_IMAGE_PATH, mockImagePath);
            navController.navigate(R.id.navigation_result, args);
        });

        // Allow background classifier / stub to resolve
        Thread.sleep(1200);

        // Verify food result card components are visible
        Espresso.onView(withId(R.id.card_nutrition_details))
                .check(ViewAssertions.matches(isDisplayed()));
        Espresso.onView(withId(R.id.text_nutrition_food_name))
                .check(ViewAssertions.matches(isDisplayed()));

        // Tap "Add to Daily Log"
        Espresso.onView(withId(R.id.btn_add_to_log))
                .perform(ViewActions.click());

        // Wait for asynchronous Room insert and navigation
        Thread.sleep(1000);

        // Verify user is back on HomeFragment
        Espresso.onView(withId(R.id.card_calorie_progress))
                .check(ViewAssertions.matches(isDisplayed()));

        // Verify recycler is displayed and empty state is hidden
        Espresso.onView(withId(R.id.recycler_todays_meals))
                .check(ViewAssertions.matches(isDisplayed()));
        Espresso.onView(withId(R.id.layout_empty_state))
                .check(ViewAssertions.matches(not(isDisplayed())));
    }
}
