package com.nutriworks.ui;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.SettingsDao;
import com.nutriworks.ml.ClassifierFactory;
import com.nutriworks.model.AppSettings;
import com.nutriworks.server.BridgeTokenManager;
import com.nutriworks.server.PhoneBridgeServer;
import com.nutriworks.service.WebcamService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

/**
 * JavaFX Desktop Application entry point for NutriWorks.
 * Configures window lifecycle, global keyboard shortcuts (Ctrl+1..9),
 * and clean shutdown hooks for server and hardware resources.
 */
public class NutriWorksApp extends Application {

    private static PhoneBridgeServer phoneBridgeServer;
    private static BridgeTokenManager tokenManager;
    private static Stage primaryStage;

    public static PhoneBridgeServer getPhoneBridgeServer() {
        return phoneBridgeServer;
    }

    public static BridgeTokenManager getTokenManager() {
        return tokenManager;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void init() throws Exception {
        // Initialize SQLite schema
        DatabaseManager.getInstance();

        // Initialize Phone Bridge Server
        tokenManager = new BridgeTokenManager();
        phoneBridgeServer = new PhoneBridgeServer(tokenManager);

        AppSettings settings = new SettingsDao(DatabaseManager.getInstance()).getSettings();
        phoneBridgeServer.startServer(settings.getLanIpOverride(), settings.getServerPort());
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        stage.setTitle("NutriWorks - Daily Nutrition & Health Tracker (Java Desktop)");
        stage.setMinWidth(1080);
        stage.setMinHeight(700);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nutriworks/ui/main_layout.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1200, 780);

        // Apply theme CSS
        AppSettings settings = new SettingsDao(DatabaseManager.getInstance()).getSettings();
        applyTheme(scene, settings.getTheme());

        // Setup global keyboard shortcuts
        setupKeyShortcuts(scene);

        stage.setScene(scene);
        stage.show();
    }

    public static void applyTheme(Scene scene, String themeName) {
        if (scene == null) return;
        scene.getStylesheets().clear();
        String cssPath = "DARK".equalsIgnoreCase(themeName) ? "/css/dark_theme.css" : "/css/light_theme.css";
        var res = NutriWorksApp.class.getResource(cssPath);
        if (res != null) {
            scene.getStylesheets().add(res.toExternalForm());
        }
    }

    private void setupKeyShortcuts(Scene scene) {
        NavigationService nav = NavigationService.getInstance();

        // Ctrl + 1..9
        for (NavigationService.Page p : NavigationService.Page.values()) {
            KeyCode code = KeyCode.getKeyCode("DIGIT" + p.getShortcutIndex());
            if (code != null) {
                scene.getAccelerators().put(
                        new KeyCodeCombination(code, KeyCombination.CONTROL_DOWN),
                        () -> nav.navigateTo(p)
                );
            }
        }

        // Alt + Left (Back)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.LEFT, KeyCombination.ALT_DOWN),
                nav::goBack
        );

        // Alt + Right (Forward)
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.ALT_DOWN),
                nav::goForward
        );
    }

    @Override
    public void stop() throws Exception {
        // Safe shutdown
        if (phoneBridgeServer != null) {
            phoneBridgeServer.stopServer();
        }
        WebcamService.getInstance().stopWebcam();
        ClassifierFactory.resetClassifier();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
