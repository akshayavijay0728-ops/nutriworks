package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.FoodLibraryDao;
import com.nutriworks.ml.ClassifierFactory;
import com.nutriworks.ml.FoodClassifier;
import com.nutriworks.model.FoodItem;
import com.nutriworks.model.MealEntry;
import com.nutriworks.model.NutritionInfo;
import com.nutriworks.model.Prediction;
import com.nutriworks.service.MealTrackingService;
import com.nutriworks.service.NutritionService;
import com.nutriworks.service.WebcamService;
import com.nutriworks.ui.NavigationService;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Scan Food Studio supporting 4 input streams:
 * Phone camera (QR), Phone gallery (QR), Desktop File Upload / Drag-and-Drop,
 * and Laptop Webcam capture.
 */
public class ScanFoodController implements Initializable {

    @FXML private ImageView imgScanPreview;
    @FXML private VBox dropZoneBox;
    @FXML private VBox webcamStreamBox;
    @FXML private ImageView imgWebcamLive;
    @FXML private Button btnWebcamCapture;
    @FXML private Label lblScanSourceBadge;

    // Result card
    @FXML private VBox resultCard;
    @FXML private Label lblRecognizedTitle;
    @FXML private Label lblInferenceTime;
    @FXML private HBox predictionChipsBox;
    @FXML private VBox lowConfidenceWarningBox;
    @FXML private TextField txtManualSearch;
    @FXML private ListView<String> listSearchResults;

    // Portion & Macros
    @FXML private Slider sliderPortion;
    @FXML private Label lblPortionGrams;
    @FXML private Label lblCalcKcal;
    @FXML private Label lblCalcProtein;
    @FXML private Label lblCalcCarbs;
    @FXML private Label lblCalcFat;

    // Meal type
    @FXML private ComboBox<String> cmbMealType;

    private BufferedImage currentImage;
    private File currentImageFile;
    private MealEntry.Source currentSource = MealEntry.Source.UPLOAD;
    private NutritionInfo currentNutritionInfo;
    private int currentPortion = 150;
    private boolean isWebcamActive = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cmbMealType.getItems().addAll("Breakfast", "Lunch", "Snack", "Dinner");
        cmbMealType.setValue("Lunch");

        sliderPortion.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentPortion = newVal.intValue();
            lblPortionGrams.setText(currentPortion + "g");
            updateCalculations();
        });

        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        dropZoneBox.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZoneBox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZoneBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                processLocalImageFile(file, MealEntry.Source.UPLOAD);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    @FXML
    private void onChooseLocalFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Food Photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.webp")
        );
        File file = chooser.showOpenDialog(dropZoneBox.getScene().getWindow());
        if (file != null) {
            processLocalImageFile(file, MealEntry.Source.UPLOAD);
        }
    }

    @FXML
    private void onToggleWebcam() {
        WebcamService webcamService = WebcamService.getInstance();
        if (!isWebcamActive) {
            boolean opened = webcamService.startWebcam();
            if (opened) {
                isWebcamActive = true;
                webcamStreamBox.setVisible(true);
                webcamStreamBox.setManaged(true);
                btnWebcamCapture.setText("Capture Photo");

                // Start snapshot loop in background thread
                new Thread(() -> {
                    while (isWebcamActive && webcamService.isStreaming()) {
                        BufferedImage frame = webcamService.captureFrame();
                        if (frame != null) {
                            Image fxImg = SwingFXUtils.toFXImage(frame, null);
                            Platform.runLater(() -> imgWebcamLive.setImage(fxImg));
                        }
                        try {
                            Thread.sleep(66); // ~15 FPS preview
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                }).start();
            } else {
                new Alert(Alert.AlertType.WARNING, "No webcam hardware detected or permission denied.").showAndWait();
            }
        } else {
            // Take snapshot
            BufferedImage snapshot = webcamService.captureFrame();
            webcamService.stopWebcam();
            isWebcamActive = false;
            webcamStreamBox.setVisible(false);
            webcamStreamBox.setManaged(false);

            if (snapshot != null) {
                processBufferedImage(snapshot, null, MealEntry.Source.WEBCAM);
            }
        }
    }

    @FXML
    private void onOpenPhoneConnectPage() {
        NavigationService.getInstance().navigateTo(NavigationService.Page.PHONE_CONNECT);
    }

    public void displayPhoneCaptureResult(File savedFile, BufferedImage image, List<Prediction> predictions) {
        processBufferedImage(image, savedFile, MealEntry.Source.PHONE);
    }

    private void processLocalImageFile(File file, MealEntry.Source source) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img != null) {
                processBufferedImage(img, file, source);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to read image: " + e.getMessage()).showAndWait();
        }
    }

    private void processBufferedImage(BufferedImage img, File file, MealEntry.Source source) {
        this.currentImage = img;
        this.currentImageFile = file;
        this.currentSource = source;

        imgScanPreview.setImage(SwingFXUtils.toFXImage(img, null));
        lblScanSourceBadge.setText("Source: " + source.name());

        long start = System.currentTimeMillis();
        FoodClassifier classifier = ClassifierFactory.getClassifier();
        List<Prediction> predictions = classifier.classify(img, 3);
        long elapsed = System.currentTimeMillis() - start;

        lblInferenceTime.setText(String.format("Inference Time: %d ms", elapsed));
        displayPredictions(predictions);
    }

    private void displayPredictions(List<Prediction> predictions) {
        resultCard.setVisible(true);
        resultCard.setManaged(true);
        predictionChipsBox.getChildren().clear();

        if (predictions.isEmpty()) {
            lblRecognizedTitle.setText("Unrecognized Food");
            showLowConfidenceWarning(true);
            return;
        }

        Prediction top = predictions.get(0);
        lblRecognizedTitle.setText(capitalize(top.getLabel()));
        setTargetFood(top.getLabel());

        boolean isLowConfidence = top.getConfidence() < 0.60f;
        showLowConfidenceWarning(isLowConfidence);

        for (Prediction p : predictions) {
            Button chip = new Button(String.format("%s (%s)", capitalize(p.getLabel()), p.getFormattedConfidence()));
            chip.getStyleClass().add("chip-button");
            chip.setOnAction(e -> {
                lblRecognizedTitle.setText(capitalize(p.getLabel()));
                setTargetFood(p.getLabel());
            });
            predictionChipsBox.getChildren().add(chip);
        }
    }

    private void showLowConfidenceWarning(boolean show) {
        lowConfidenceWarningBox.setVisible(show);
        lowConfidenceWarningBox.setManaged(show);
    }

    @FXML
    private void onManualSearchQuery() {
        String query = txtManualSearch.getText();
        if (query == null || query.trim().isEmpty()) {
            listSearchResults.getItems().clear();
            return;
        }

        try {
            FoodLibraryDao dao = new FoodLibraryDao(DatabaseManager.getInstance());
            List<FoodItem> items = dao.searchByName(query, 10);
            listSearchResults.getItems().clear();
            for (FoodItem item : items) {
                listSearchResults.getItems().add(item.getName() + " (" + (int) item.getKcalPer100g() + " kcal/100g)");
            }
        } catch (SQLException ignored) {
        }
    }

    @FXML
    private void onSelectSearchResult() {
        String selected = listSearchResults.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String foodName = selected.split(" \\(")[0];
            lblRecognizedTitle.setText(foodName);
            setTargetFood(foodName);
            showLowConfidenceWarning(false);
        }
    }

    private void setTargetFood(String label) {
        NutritionService svc = NutritionService.getInstance();
        this.currentNutritionInfo = svc.findByName(label);
        updateCalculations();
    }

    private void updateCalculations() {
        if (currentNutritionInfo == null) return;
        double ratio = currentPortion / 100.0;
        int kcal = (int) Math.round(currentNutritionInfo.getKcalPer100g() * ratio);
        double p = currentNutritionInfo.getProteinPer100g() * ratio;
        double c = currentNutritionInfo.getCarbsPer100g() * ratio;
        double f = currentNutritionInfo.getFatPer100g() * ratio;

        lblCalcKcal.setText(String.format("%d kcal", kcal));
        lblCalcProtein.setText(String.format("%.1fg", p));
        lblCalcCarbs.setText(String.format("%.1fg", c));
        lblCalcFat.setText(String.format("%.1fg", f));
    }

    @FXML
    private void onAddToMealLog() {
        if (currentNutritionInfo == null) return;

        double ratio = currentPortion / 100.0;
        int kcal = (int) Math.round(currentNutritionInfo.getKcalPer100g() * ratio);
        double p = currentNutritionInfo.getProteinPer100g() * ratio;
        double c = currentNutritionInfo.getCarbsPer100g() * ratio;
        double f = currentNutritionInfo.getFatPer100g() * ratio;

        MealEntry entry = new MealEntry(
                lblRecognizedTitle.getText(),
                currentPortion,
                kcal,
                p,
                c,
                f,
                cmbMealType.getValue(),
                System.currentTimeMillis(),
                currentImageFile != null ? currentImageFile.getAbsolutePath() : "",
                currentSource
        );

        try {
            MealTrackingService.getInstance().logMeal(entry);
            new Alert(Alert.AlertType.INFORMATION, "Added " + entry.getFoodName() + " (" + kcal + " kcal) to Meal Log!").show();
            NavigationService.getInstance().navigateTo(NavigationService.Page.DAILY_TRACKER);
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Database save failed: " + e.getMessage()).showAndWait();
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
