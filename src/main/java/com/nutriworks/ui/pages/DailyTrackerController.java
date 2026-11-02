package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.FoodLibraryDao;
import com.nutriworks.db.UserProfileDao;
import com.nutriworks.model.FoodItem;
import com.nutriworks.model.MealEntry;
import com.nutriworks.model.NutritionInfo;
import com.nutriworks.model.UserProfile;
import com.nutriworks.service.MealTrackingService;
import com.nutriworks.service.NutritionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for the Daily Tracker page with grouped meals, calorie subtotals,
 * undoable deletions, meal category moves, and quick-add autocomplete.
 */
public class DailyTrackerController implements Initializable {

    @FXML private DatePicker datePicker;
    @FXML private Label lblDayTotals;
    @FXML private ProgressBar pbDayGoal;
    @FXML private Label lblGoalRemaining;

    // Group containers
    @FXML private VBox boxBreakfast;
    @FXML private Label lblBreakfastSubtotal;
    @FXML private VBox boxLunch;
    @FXML private Label lblLunchSubtotal;
    @FXML private VBox boxSnack;
    @FXML private Label lblSnackSubtotal;
    @FXML private VBox boxDinner;
    @FXML private Label lblDinnerSubtotal;

    // Quick add
    @FXML private TextField txtQuickAdd;
    @FXML private TextField txtQuickGrams;
    @FXML private ComboBox<String> cmbQuickMealType;
    @FXML private Button btnUndo;

    private LocalDate selectedDate = LocalDate.now();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        datePicker.setValue(selectedDate);
        datePicker.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedDate = newV;
                loadDayMeals();
            }
        });

        cmbQuickMealType.getItems().addAll("Breakfast", "Lunch", "Snack", "Dinner");
        cmbQuickMealType.setValue("Lunch");

        loadDayMeals();
    }

    @FXML
    private void onPrevDay() {
        selectedDate = selectedDate.minusDays(1);
        datePicker.setValue(selectedDate);
    }

    @FXML
    private void onNextDay() {
        selectedDate = selectedDate.plusDays(1);
        datePicker.setValue(selectedDate);
    }

    @FXML
    private void onToday() {
        selectedDate = LocalDate.now();
        datePicker.setValue(selectedDate);
    }

    public void loadDayMeals() {
        MealTrackingService service = MealTrackingService.getInstance();
        UserProfile profile = new UserProfileDao(DatabaseManager.getInstance()).getProfile();

        try {
            List<MealEntry> meals = service.getMealsForDate(selectedDate);
            MealTrackingService.DailySummary summary = service.getSummaryForDate(selectedDate);

            // Update Header Totals
            int goal = profile.getDailyCalorieGoal();
            int consumed = summary.getTotalCalories();
            int left = Math.max(0, goal - consumed);

            lblDayTotals.setText(String.format("%,d / %,d kcal | P: %.1fg  C: %.1fg  F: %.1fg",
                    consumed, goal, summary.getTotalProtein(), summary.getTotalCarbs(), summary.getTotalFat()));
            lblGoalRemaining.setText(String.format("%d kcal remaining", left));
            pbDayGoal.setProgress(goal > 0 ? Math.min(1.0, (double) consumed / goal) : 0);

            // Clear groups
            boxBreakfast.getChildren().clear();
            boxLunch.getChildren().clear();
            boxSnack.getChildren().clear();
            boxDinner.getChildren().clear();

            int bCals = 0, lCals = 0, sCals = 0, dCals = 0;

            for (MealEntry m : meals) {
                HBox row = createMealRow(m);
                String type = m.getMealType().toLowerCase();
                if (type.contains("breakfast")) {
                    boxBreakfast.getChildren().add(row);
                    bCals += m.getCalories();
                } else if (type.contains("lunch")) {
                    boxLunch.getChildren().add(row);
                    lCals += m.getCalories();
                } else if (type.contains("snack")) {
                    boxSnack.getChildren().add(row);
                    sCals += m.getCalories();
                } else {
                    boxDinner.getChildren().add(row);
                    dCals += m.getCalories();
                }
            }

            lblBreakfastSubtotal.setText(bCals + " kcal");
            lblLunchSubtotal.setText(lCals + " kcal");
            lblSnackSubtotal.setText(sCals + " kcal");
            lblDinnerSubtotal.setText(dCals + " kcal");

            btnUndo.setDisable(!service.hasUndoableDelete());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private HBox createMealRow(MealEntry m) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 8 12; -fx-background-radius: 8; -fx-border-color: #E2E8F0; -fx-border-radius: 8;");

        Label name = new Label(m.getFoodName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        HBox.setHgrow(name, Priority.ALWAYS);

        Label details = new Label(String.format("%.0fg | %d kcal (P:%.1f C:%.1f F:%.1f)",
                m.getGrams(), m.getCalories(), m.getProtein(), m.getCarbs(), m.getFat()));
        details.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");

        Label sourceBadge = new Label(m.getSource().name());
        sourceBadge.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 2 6; -fx-background-radius: 6; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #475569;");

        // Move category menu
        MenuButton moveBtn = new MenuButton("Move");
        moveBtn.setStyle("-fx-font-size: 10px;");
        String[] types = {"Breakfast", "Lunch", "Snack", "Dinner"};
        for (String t : types) {
            if (!t.equalsIgnoreCase(m.getMealType())) {
                MenuItem item = new MenuItem("To " + t);
                item.setOnAction(e -> {
                    try {
                        MealTrackingService.getInstance().moveMealType(m.getId(), t);
                        loadDayMeals();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });
                moveBtn.getItems().add(item);
            }
        }

        // Delete button
        Button delBtn = new Button("Delete");
        delBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 10px; -fx-font-weight: bold;");
        delBtn.setOnAction(e -> {
            try {
                MealTrackingService.getInstance().deleteMealWithUndo(m);
                loadDayMeals();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });

        row.getChildren().addAll(name, details, sourceBadge, moveBtn, delBtn);
        return row;
    }

    @FXML
    private void onQuickAddFood() {
        String foodName = txtQuickAdd.getText();
        if (foodName == null || foodName.trim().isEmpty()) {
            return;
        }

        double grams = 100.0;
        try {
            grams = Double.parseDouble(txtQuickGrams.getText().trim());
        } catch (Exception ignored) {
        }

        NutritionService svc = NutritionService.getInstance();
        NutritionInfo info = svc.findByName(foodName);

        double ratio = grams / 100.0;
        int kcal = (int) Math.round(info.getKcalPer100g() * ratio);
        double p = info.getProteinPer100g() * ratio;
        double c = info.getCarbsPer100g() * ratio;
        double f = info.getFatPer100g() * ratio;

        MealEntry entry = new MealEntry(
                info.getName(),
                grams,
                kcal,
                p,
                c,
                f,
                cmbQuickMealType.getValue(),
                selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + System.currentTimeMillis() % 86400000L,
                "",
                MealEntry.Source.MANUAL
        );

        try {
            MealTrackingService.getInstance().logMeal(entry);
            txtQuickAdd.clear();
            txtQuickGrams.setText("100");
            loadDayMeals();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onUndoDelete() {
        try {
            MealTrackingService.getInstance().undoLastDelete();
            loadDayMeals();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onCopyYesterdayMeals() {
        try {
            int copied = MealTrackingService.getInstance().copyYesterdayMeals(selectedDate);
            new Alert(Alert.AlertType.INFORMATION, "Copied " + copied + " meals from yesterday!").show();
            loadDayMeals();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to copy meals: " + e.getMessage()).showAndWait();
        }
    }
}
