package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.SettingsDao;
import com.nutriworks.ml.ClassifierFactory;
import com.nutriworks.model.AppSettings;
import com.nutriworks.server.PhoneBridgeServer;
import com.nutriworks.ui.NutriWorksApp;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for application preferences: Theme, Network ports, IP override,
 * ML Classifier Stub mode, and automatic navigation behaviors.
 */
public class SettingsController implements Initializable {

    @FXML private ComboBox<String> cmbTheme;
    @FXML private TextField txtServerPort;
    @FXML private TextField txtIpOverride;
    @FXML private CheckBox chkStubClassifier;
    @FXML private CheckBox chkAutoNavigate;
    @FXML private Label lblSaveStatus;

    private SettingsDao settingsDao;
    private AppSettings currentSettings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        settingsDao = new SettingsDao(DatabaseManager.getInstance());
        currentSettings = settingsDao.getSettings();

        cmbTheme.getItems().addAll("LIGHT", "DARK");
        cmbTheme.setValue(currentSettings.getTheme());

        txtServerPort.setText(String.valueOf(currentSettings.getServerPort()));
        txtIpOverride.setText(currentSettings.getLanIpOverride());
        chkStubClassifier.setSelected(currentSettings.isUseStubClassifier());
        chkAutoNavigate.setSelected(currentSettings.isAutoNavigateOnUpload());
    }

    @FXML
    private void onSaveSettings() {
        try {
            int port = Integer.parseInt(txtServerPort.getText().trim());
            String theme = cmbTheme.getValue();
            String ipOverride = txtIpOverride.getText().trim();
            boolean stub = chkStubClassifier.isSelected();
            boolean autoNav = chkAutoNavigate.isSelected();

            currentSettings.setTheme(theme);
            currentSettings.setServerPort(port);
            currentSettings.setLanIpOverride(ipOverride);
            currentSettings.setUseStubClassifier(stub);
            currentSettings.setAutoNavigateOnUpload(autoNav);

            settingsDao.saveSettings(currentSettings);

            // Apply theme dynamically
            if (NutriWorksApp.getPrimaryStage() != null && NutriWorksApp.getPrimaryStage().getScene() != null) {
                NutriWorksApp.applyTheme(NutriWorksApp.getPrimaryStage().getScene(), theme);
            }

            // Reset classifier cache if stub toggle changed
            ClassifierFactory.resetClassifier();

            // Restart bridge server if running
            PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
            if (server != null && server.isRunning()) {
                server.stopServer();
                server.startServer(ipOverride, port);
            }

            lblSaveStatus.setText("Settings saved successfully!");
            lblSaveStatus.setStyle("-fx-text-fill: #15803D;");

        } catch (Exception e) {
            lblSaveStatus.setText("Save failed: " + e.getMessage());
            lblSaveStatus.setStyle("-fx-text-fill: #DC2626;");
        }
    }
}
