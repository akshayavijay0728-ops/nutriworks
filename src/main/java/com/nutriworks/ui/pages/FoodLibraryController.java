package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.FoodLibraryDao;
import com.nutriworks.model.FoodItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for the Food Library page supporting search, favorites,
 * and custom food creation.
 */
public class FoodLibraryController implements Initializable {

    @FXML private TextField txtSearchFood;
    @FXML private CheckBox chkFavoritesOnly;
    @FXML private TableView<FoodItem> tableFoods;
    @FXML private TableColumn<FoodItem, String> colName;
    @FXML private TableColumn<FoodItem, Double> colKcal;
    @FXML private TableColumn<FoodItem, Double> colProtein;
    @FXML private TableColumn<FoodItem, Double> colCarbs;
    @FXML private TableColumn<FoodItem, Double> colFat;
    @FXML private TableColumn<FoodItem, Double> colFiber;
    @FXML private TableColumn<FoodItem, Double> colSugar;
    @FXML private TableColumn<FoodItem, String> colServing;
    @FXML private Label lblFoodCount;

    private final ObservableList<FoodItem> foodList = FXCollections.observableArrayList();
    private FoodLibraryDao foodLibraryDao;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        foodLibraryDao = new FoodLibraryDao(DatabaseManager.getInstance());

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colKcal.setCellValueFactory(new PropertyValueFactory<>("kcalPer100g"));
        colProtein.setCellValueFactory(new PropertyValueFactory<>("proteinPer100g"));
        colCarbs.setCellValueFactory(new PropertyValueFactory<>("carbsPer100g"));
        colFat.setCellValueFactory(new PropertyValueFactory<>("fatPer100g"));
        colFiber.setCellValueFactory(new PropertyValueFactory<>("fiberPer100g"));
        colSugar.setCellValueFactory(new PropertyValueFactory<>("sugarPer100g"));
        colServing.setCellValueFactory(new PropertyValueFactory<>("servingLabel"));

        tableFoods.setItems(foodList);
        loadFoods();
    }

    public void loadFoods() {
        try {
            List<FoodItem> all = foodLibraryDao.getAllFoods();
            foodList.clear();

            String q = txtSearchFood != null ? txtSearchFood.getText().toLowerCase().trim() : "";
            boolean favOnly = chkFavoritesOnly != null && chkFavoritesOnly.isSelected();

            for (FoodItem item : all) {
                if (favOnly && !item.isFavorite()) continue;
                if (!q.isEmpty() && !item.getName().toLowerCase().contains(q)) continue;
                foodList.add(item);
            }

            lblFoodCount.setText(foodList.size() + " foods listed");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onSearchChanged() {
        loadFoods();
    }

    @FXML
    private void onToggleFavoriteFilter() {
        loadFoods();
    }

    @FXML
    private void onAddCustomFood() {
        Dialog<FoodItem> dialog = new Dialog<>();
        dialog.setTitle("Add Custom Food");
        dialog.setHeaderText("Enter Nutritional Breakdown per 100 grams:");

        ButtonType saveBtnType = new ButtonType("Save Food", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Food Name (e.g., Protein Shake)");
        TextField kcalField = new TextField("150");
        TextField proteinField = new TextField("10");
        TextField carbsField = new TextField("20");
        TextField fatField = new TextField("3");

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Food Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Calories (kcal/100g):"), 0, 1); grid.add(kcalField, 1, 1);
        grid.add(new Label("Protein (g/100g):"), 0, 2); grid.add(proteinField, 1, 2);
        grid.add(new Label("Carbs (g/100g):"), 0, 3); grid.add(carbsField, 1, 3);
        grid.add(new Label("Fat (g/100g):"), 0, 4); grid.add(fatField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveBtnType) {
                try {
                    return new FoodItem(
                            nameField.getText().trim(),
                            Double.parseDouble(kcalField.getText().trim()),
                            Double.parseDouble(proteinField.getText().trim()),
                            Double.parseDouble(carbsField.getText().trim()),
                            Double.parseDouble(fatField.getText().trim()),
                            2.0, 1.0, 100.0, "1 serving"
                    );
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        });

        Optional<FoodItem> result = dialog.showAndWait();
        result.ifPresent(food -> {
            try {
                foodLibraryDao.insert(food);
                loadFoods();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Failed adding food: " + e.getMessage()).show();
            }
        });
    }
}
