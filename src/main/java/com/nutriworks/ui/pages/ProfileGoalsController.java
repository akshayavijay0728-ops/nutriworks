package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.UserProfileDao;
import com.nutriworks.model.UserProfile;
import com.nutriworks.util.GoalCalculator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for personalized User Profile and Mifflin-St Jeor Goal Calculation engine.
 */
public class ProfileGoalsController implements Initializable {

    @FXML private TextField txtAge;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtHeightCm;
    @FXML private TextField txtWeightKg;
    @FXML private ComboBox<String> cmbActivityLevel;
    @FXML private ComboBox<String> cmbGoalType;
    @FXML private ComboBox<String> cmbMacroSplit;

    @FXML private Label lblCalculatedBmr;
    @FXML private Label lblCalculatedTdee;
    @FXML private Label lblTargetCalorieGoal;
    @FXML private Label lblTargetProtein;
    @FXML private Label lblTargetCarbs;
    @FXML private Label lblTargetFat;

    private UserProfileDao profileDao;
    private UserProfile currentProfile;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        profileDao = new UserProfileDao(DatabaseManager.getInstance());

        cmbGender.getItems().addAll("MALE", "FEMALE");
        cmbActivityLevel.getItems().addAll(
                "Sedentary (1.2x)",
                "Lightly Active (1.375x)",
                "Moderately Active (1.55x)",
                "Very Active (1.725x)"
        );
        cmbGoalType.getItems().addAll(
                "Lose Weight (-500 kcal/day)",
                "Maintain Weight (0 kcal/day)",
                "Gain Weight (+350 kcal/day)"
        );
        cmbMacroSplit.getItems().addAll(
                "Balanced (25% P, 50% C, 25% F)",
                "High Protein (35% P, 40% C, 25% F)",
                "Low Carb (30% P, 25% C, 45% F)"
        );

        loadProfileData();

        // Attach listeners for live recalculation
        txtAge.textProperty().addListener((obs, o, n) -> recalculateLive());
        txtHeightCm.textProperty().addListener((obs, o, n) -> recalculateLive());
        txtWeightKg.textProperty().addListener((obs, o, n) -> recalculateLive());
        cmbGender.valueProperty().addListener((obs, o, n) -> recalculateLive());
        cmbActivityLevel.valueProperty().addListener((obs, o, n) -> recalculateLive());
        cmbGoalType.valueProperty().addListener((obs, o, n) -> recalculateLive());
        cmbMacroSplit.valueProperty().addListener((obs, o, n) -> recalculateLive());
    }

    private void loadProfileData() {
        currentProfile = profileDao.getProfile();
        txtAge.setText(String.valueOf(currentProfile.getAge()));
        cmbGender.setValue(currentProfile.getGender());
        txtHeightCm.setText(String.valueOf(currentProfile.getHeightCm()));
        txtWeightKg.setText(String.valueOf(currentProfile.getWeightKg()));

        if (currentProfile.getActivityMultiplier() >= 1.7) {
            cmbActivityLevel.setValue("Very Active (1.725x)");
        } else if (currentProfile.getActivityMultiplier() >= 1.5) {
            cmbActivityLevel.setValue("Moderately Active (1.55x)");
        } else if (currentProfile.getActivityMultiplier() >= 1.3) {
            cmbActivityLevel.setValue("Lightly Active (1.375x)");
        } else {
            cmbActivityLevel.setValue("Sedentary (1.2x)");
        }

        if (UserProfile.GOAL_GAIN_WEIGHT.equals(currentProfile.getGoalType())) {
            cmbGoalType.setValue("Gain Weight (+350 kcal/day)");
        } else if (UserProfile.GOAL_MAINTAIN_WEIGHT.equals(currentProfile.getGoalType())) {
            cmbGoalType.setValue("Maintain Weight (0 kcal/day)");
        } else {
            cmbGoalType.setValue("Lose Weight (-500 kcal/day)");
        }

        if (UserProfile.MACRO_SPLIT_HIGH_PROTEIN.equals(currentProfile.getMacroSplit())) {
            cmbMacroSplit.setValue("High Protein (35% P, 40% C, 25% F)");
        } else if (UserProfile.MACRO_SPLIT_LOW_CARB.equals(currentProfile.getMacroSplit())) {
            cmbMacroSplit.setValue("Low Carb (30% P, 25% C, 45% F)");
        } else {
            cmbMacroSplit.setValue("Balanced (25% P, 50% C, 25% F)");
        }

        recalculateLive();
    }

    private void recalculateLive() {
        try {
            int age = Integer.parseInt(txtAge.getText().trim());
            double height = Double.parseDouble(txtHeightCm.getText().trim());
            double weight = Double.parseDouble(txtWeightKg.getText().trim());
            String gender = cmbGender.getValue();

            double multiplier = 1.375;
            String actStr = cmbActivityLevel.getValue();
            if (actStr != null) {
                if (actStr.contains("1.725")) multiplier = 1.725;
                else if (actStr.contains("1.55")) multiplier = 1.55;
                else if (actStr.contains("1.375")) multiplier = 1.375;
                else if (actStr.contains("1.2")) multiplier = 1.2;
            }

            String goalType = UserProfile.GOAL_LOSE_WEIGHT;
            String gStr = cmbGoalType.getValue();
            if (gStr != null) {
                if (gStr.contains("Gain")) goalType = UserProfile.GOAL_GAIN_WEIGHT;
                else if (gStr.contains("Maintain")) goalType = UserProfile.GOAL_MAINTAIN_WEIGHT;
            }

            String macroSplit = UserProfile.MACRO_SPLIT_BALANCED;
            String mStr = cmbMacroSplit.getValue();
            if (mStr != null) {
                if (mStr.contains("High Protein")) macroSplit = UserProfile.MACRO_SPLIT_HIGH_PROTEIN;
                else if (mStr.contains("Low Carb")) macroSplit = UserProfile.MACRO_SPLIT_LOW_CARB;
            }

            double bmr = GoalCalculator.calculateBmr(weight, height, age, gender);
            double tdee = GoalCalculator.calculateTdee(bmr, multiplier);
            int dailyGoal = GoalCalculator.calculateDailyCalorieGoal(tdee, goalType, 0.5, gender);
            GoalCalculator.MacroTargets macros = GoalCalculator.calculateMacroGrams(dailyGoal, macroSplit);

            lblCalculatedBmr.setText(String.format("%.0f kcal", bmr));
            lblCalculatedTdee.setText(String.format("%.0f kcal", tdee));
            lblTargetCalorieGoal.setText(String.format("%,d kcal/day", dailyGoal));

            lblTargetProtein.setText(String.format("%dg Protein", macros.getProteinGrams()));
            lblTargetCarbs.setText(String.format("%dg Carbs", macros.getCarbsGrams()));
            lblTargetFat.setText(String.format("%dg Fat", macros.getFatGrams()));

        } catch (Exception ignored) {
        }
    }

    @FXML
    private void onSaveProfile() {
        try {
            int age = Integer.parseInt(txtAge.getText().trim());
            double height = Double.parseDouble(txtHeightCm.getText().trim());
            double weight = Double.parseDouble(txtWeightKg.getText().trim());
            String gender = cmbGender.getValue();

            double multiplier = 1.375;
            String actStr = cmbActivityLevel.getValue();
            if (actStr.contains("1.725")) multiplier = 1.725;
            else if (actStr.contains("1.55")) multiplier = 1.55;
            else if (actStr.contains("1.375")) multiplier = 1.375;
            else if (actStr.contains("1.2")) multiplier = 1.2;

            String goalType = UserProfile.GOAL_LOSE_WEIGHT;
            String gStr = cmbGoalType.getValue();
            if (gStr.contains("Gain")) goalType = UserProfile.GOAL_GAIN_WEIGHT;
            else if (gStr.contains("Maintain")) goalType = UserProfile.GOAL_MAINTAIN_WEIGHT;

            String macroSplit = UserProfile.MACRO_SPLIT_BALANCED;
            String mStr = cmbMacroSplit.getValue();
            if (mStr.contains("High Protein")) macroSplit = UserProfile.MACRO_SPLIT_HIGH_PROTEIN;
            else if (mStr.contains("Low Carb")) macroSplit = UserProfile.MACRO_SPLIT_LOW_CARB;

            double bmr = GoalCalculator.calculateBmr(weight, height, age, gender);
            double tdee = GoalCalculator.calculateTdee(bmr, multiplier);
            int dailyGoal = GoalCalculator.calculateDailyCalorieGoal(tdee, goalType, 0.5, gender);
            GoalCalculator.MacroTargets macros = GoalCalculator.calculateMacroGrams(dailyGoal, macroSplit);

            currentProfile.setAge(age);
            currentProfile.setHeightCm(height);
            currentProfile.setWeightKg(weight);
            currentProfile.setGender(gender);
            currentProfile.setActivityMultiplier(multiplier);
            currentProfile.setGoalType(goalType);
            currentProfile.setMacroSplit(macroSplit);
            currentProfile.setDailyCalorieGoal(dailyGoal);
            currentProfile.setTargetProteinGrams(macros.getProteinGrams());
            currentProfile.setTargetCarbsGrams(macros.getCarbsGrams());
            currentProfile.setTargetFatGrams(macros.getFatGrams());

            profileDao.saveProfile(currentProfile);

            new Alert(Alert.AlertType.INFORMATION, "Profile and personalized calorie targets updated successfully!").show();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Invalid input: " + e.getMessage()).showAndWait();
        }
    }
}
