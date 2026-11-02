package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.UserProfileDao;
import com.nutriworks.model.MealEntry;
import com.nutriworks.model.UserProfile;
import com.nutriworks.service.MealTrackingService;
import com.nutriworks.ui.NavigationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the main executive desktop dashboard.
 */
public class DashboardController implements Initializable {

    @FXML private Label lblKpiConsumed;
    @FXML private Label lblKpiTarget;
    @FXML private Label lblKpiBurned;
    @FXML private Label lblKpiNet;
    @FXML private Label lblKpiWater;
    @FXML private Label lblKpiFasting;

    @FXML private Label lblRingCalories;
    @FXML private Label lblRingRemaining;
    @FXML private Arc arcCalorieProgress;

    @FXML private Label lblProteinProgress;
    @FXML private ProgressBar pbProtein;

    @FXML private Label lblCarbsProgress;
    @FXML private ProgressBar pbCarbs;

    @FXML private Label lblFatProgress;
    @FXML private ProgressBar pbFat;

    @FXML private VBox recentMealsContainer;

    private int burnedCalories = 340;
    private int waterConsumedMl = 1750;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        refreshDashboard();
    }

    public void refreshDashboard() {
        UserProfile profile = new UserProfileDao(DatabaseManager.getInstance()).getProfile();
        LocalDate today = LocalDate.now();

        try {
            MealTrackingService trackingService = MealTrackingService.getInstance();
            MealTrackingService.DailySummary summary = trackingService.getSummaryForDate(today);
            List<MealEntry> todayMeals = trackingService.getMealsForDate(today);

            int goal = profile.getDailyCalorieGoal();
            int consumed = summary.getTotalCalories();
            int remaining = Math.max(0, goal - consumed);
            int net = Math.max(0, consumed - burnedCalories);

            // Update KPI cards
            lblKpiConsumed.setText(String.format("%,d kcal", consumed));
            lblKpiTarget.setText(String.format("Target: %,d kcal (%d left)", goal, remaining));
            lblKpiBurned.setText(String.format("%,d kcal", burnedCalories));
            lblKpiNet.setText(String.format("%,d kcal", net));
            lblKpiWater.setText(String.format("%,d / 2,500 ml", waterConsumedMl));
            lblKpiFasting.setText("14h 22m remaining");

            // Calorie Ring
            lblRingCalories.setText(String.format("%,d", consumed));
            lblRingRemaining.setText(String.format("%,d kcal remaining", remaining));

            double ratio = goal > 0 ? (double) consumed / goal : 0.0;
            double angle = Math.min(360.0, ratio * 360.0);
            arcCalorieProgress.setLength(-angle);

            // Macros
            double p = summary.getTotalProtein();
            double c = summary.getTotalCarbs();
            double f = summary.getTotalFat();

            int targetP = profile.getTargetProteinGrams();
            int targetC = profile.getTargetCarbsGrams();
            int targetF = profile.getTargetFatGrams();

            lblProteinProgress.setText(String.format("%.1f / %dg (%.0f%%)", p, targetP, targetP > 0 ? (p / targetP) * 100.0 : 0));
            pbProtein.setProgress(targetP > 0 ? Math.min(1.0, p / targetP) : 0);

            lblCarbsProgress.setText(String.format("%.1f / %dg (%.0f%%)", c, targetC, targetC > 0 ? (c / targetC) * 100.0 : 0));
            pbCarbs.setProgress(targetC > 0 ? Math.min(1.0, c / targetC) : 0);

            lblFatProgress.setText(String.format("%.1f / %dg (%.0f%%)", f, targetF, targetF > 0 ? (f / targetF) * 100.0 : 0));
            pbFat.setProgress(targetF > 0 ? Math.min(1.0, f / targetF) : 0);

            // Populate recent meals summary list
            recentMealsContainer.getChildren().clear();
            if (todayMeals.isEmpty()) {
                Label emptyLbl = new Label("No meals logged yet today. Use Scan Food or Daily Tracker to log a meal.");
                emptyLbl.getStyleClass().add("text-muted");
                recentMealsContainer.getChildren().add(emptyLbl);
            } else {
                int displayCount = Math.min(5, todayMeals.size());
                for (int i = 0; i < displayCount; i++) {
                    MealEntry m = todayMeals.get(todayMeals.size() - 1 - i); // most recent first
                    Label row = new Label(String.format("[%s] %s (%.0fg) - %,d kcal [Source: %s]",
                            m.getMealType(), m.getFoodName(), m.getGrams(), m.getCalories(), m.getSource()));
                    row.setStyle("-fx-font-size: 13px; -fx-padding: 4px 0;");
                    recentMealsContainer.getChildren().add(row);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error refreshing dashboard metrics: " + e.getMessage());
        }
    }

    @FXML
    private void onQuickScan() {
        NavigationService.getInstance().navigateTo(NavigationService.Page.SCAN_FOOD);
    }

    @FXML
    private void onQuickTracker() {
        NavigationService.getInstance().navigateTo(NavigationService.Page.DAILY_TRACKER);
    }

    @FXML
    private void onQuickPhoneConnect() {
        NavigationService.getInstance().navigateTo(NavigationService.Page.PHONE_CONNECT);
    }
}
