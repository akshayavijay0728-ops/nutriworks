package com.nutriworks.app;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.nutriworks.app.data.repository.ProfileRepository;
import com.nutriworks.app.databinding.ActivityMainBinding;

import java.util.concurrent.Executors;

/**
 * Main host activity for NutriWorks.
 * Configures SplashScreen API, ViewBinding, top app bar, BottomNavigationView,
 * and handles first-launch onboarding routing with Jetpack Navigation Component.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ProfileRepository profileRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Install modern Android 12+ SplashScreen API before super.onCreate()
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileRepository = new ProfileRepository(this);

        setupNavigation();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Treat all 5 primary bottom nav destinations as top-level
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home,
                    R.id.navigation_capture,
                    R.id.navigation_history,
                    R.id.navigation_reports,
                    R.id.navigation_profile
            ).build();

            NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

            // Hide bottom navigation & toolbar on fullscreen/onboarding destinations
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.navigation_onboarding) {
                    binding.bottomNavigation.setVisibility(View.GONE);
                    binding.toolbar.setVisibility(View.GONE);
                } else if (id == R.id.navigation_confirm_image || id == R.id.navigation_result) {
                    binding.bottomNavigation.setVisibility(View.GONE);
                    binding.toolbar.setVisibility(View.VISIBLE);
                } else {
                    binding.bottomNavigation.setVisibility(View.VISIBLE);
                    binding.toolbar.setVisibility(View.VISIBLE);
                }
            });

            // Check if onboarding is completed; if not, route to OnboardingFragment
            Executors.newSingleThreadExecutor().execute(() -> {
                boolean completed = profileRepository.isOnboardingCompletedSync();
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed() && !completed) {
                        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
                        graph.setStartDestination(R.id.navigation_onboarding);
                        navController.setGraph(graph);
                    }
                });
            });
        }
    }
}
