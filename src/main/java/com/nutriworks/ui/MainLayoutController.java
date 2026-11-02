package com.nutriworks.ui;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.SettingsDao;
import com.nutriworks.model.AppSettings;
import com.nutriworks.model.Prediction;
import com.nutriworks.server.PhoneBridgeServer;
import com.nutriworks.ui.pages.ScanFoodController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the main application shell containing the collapsible left sidebar,
 * top global status bar, and central content stack.
 */
public class MainLayoutController implements Initializable, PhoneBridgeServer.PhoneBridgeListener {

    @FXML private VBox sidebar;
    @FXML private Button btnSidebarToggle;
    @FXML private Button btnBack;
    @FXML private Button btnForward;
    @FXML private Label lblTodayDate;
    @FXML private HBox phoneStatusPill;
    @FXML private Circle circlePhoneStatus;
    @FXML private Label lblPhoneStatus;
    @FXML private StackPane mainContentArea;

    // Sidebar navigation buttons
    @FXML private Button navDashboard;
    @FXML private Button navScanFood;
    @FXML private Button navDailyTracker;
    @FXML private Button navFoodLibrary;
    @FXML private Button navReports;
    @FXML private Button navProfileGoals;
    @FXML private Button navPhoneConnect;
    @FXML private Button navSettings;
    @FXML private Button navAbout;

    private boolean isSidebarCollapsed = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        NavigationService nav = NavigationService.getInstance();
        nav.init(mainContentArea, this::updateActiveNavHighlight);

        // Date display
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        lblTodayDate.setText(LocalDate.now().format(dtf));

        // Register server listener
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server != null) {
            server.addListener(this);
            updateServerStatusUi(server.isRunning(), server.getCurrentIp(), server.getCurrentPort());
        }

        // Navigate to initial dashboard
        Platform.runLater(() -> nav.navigateTo(NavigationService.Page.DASHBOARD));
    }

    @FXML
    private void onToggleSidebar() {
        isSidebarCollapsed = !isSidebarCollapsed;
        sidebar.setPrefWidth(isSidebarCollapsed ? 64 : 220);
        btnSidebarToggle.setText(isSidebarCollapsed ? ">>" : "<<");

        // Hide or show text on buttons
        updateButtonText(navDashboard, "Dashboard", "DB", isSidebarCollapsed);
        updateButtonText(navScanFood, "Scan Food", "SF", isSidebarCollapsed);
        updateButtonText(navDailyTracker, "Daily Tracker", "DT", isSidebarCollapsed);
        updateButtonText(navFoodLibrary, "Food Library", "FL", isSidebarCollapsed);
        updateButtonText(navReports, "Reports", "RP", isSidebarCollapsed);
        updateButtonText(navProfileGoals, "Profile & Goals", "PG", isSidebarCollapsed);
        updateButtonText(navPhoneConnect, "Phone Connect", "QR", isSidebarCollapsed);
        updateButtonText(navSettings, "Settings", "ST", isSidebarCollapsed);
        updateButtonText(navAbout, "About", "AB", isSidebarCollapsed);
    }

    private void updateButtonText(Button btn, String full, String shortText, boolean collapsed) {
        if (btn != null) {
            btn.setText(collapsed ? shortText : full);
        }
    }

    @FXML private void onNavDashboard() { NavigationService.getInstance().navigateTo(NavigationService.Page.DASHBOARD); }
    @FXML private void onNavScanFood() { NavigationService.getInstance().navigateTo(NavigationService.Page.SCAN_FOOD); }
    @FXML private void onNavDailyTracker() { NavigationService.getInstance().navigateTo(NavigationService.Page.DAILY_TRACKER); }
    @FXML private void onNavFoodLibrary() { NavigationService.getInstance().navigateTo(NavigationService.Page.FOOD_LIBRARY); }
    @FXML private void onNavReports() { NavigationService.getInstance().navigateTo(NavigationService.Page.REPORTS); }
    @FXML private void onNavProfileGoals() { NavigationService.getInstance().navigateTo(NavigationService.Page.PROFILE_GOALS); }
    @FXML private void onNavPhoneConnect() { NavigationService.getInstance().navigateTo(NavigationService.Page.PHONE_CONNECT); }
    @FXML private void onNavSettings() { NavigationService.getInstance().navigateTo(NavigationService.Page.SETTINGS); }
    @FXML private void onNavAbout() { NavigationService.getInstance().navigateTo(NavigationService.Page.ABOUT); }

    @FXML
    private void onGoBack() {
        NavigationService.getInstance().goBack();
    }

    @FXML
    private void onGoForward() {
        NavigationService.getInstance().goForward();
    }

    private void updateActiveNavHighlight() {
        NavigationService nav = NavigationService.getInstance();
        btnBack.setDisable(!nav.canGoBack());
        btnForward.setDisable(!nav.canGoForward());

        NavigationService.Page current = nav.getCurrentPage();
        if (current == null) return;

        resetNavStyles();
        switch (current) {
            case DASHBOARD -> setActiveNavStyle(navDashboard);
            case SCAN_FOOD -> setActiveNavStyle(navScanFood);
            case DAILY_TRACKER -> setActiveNavStyle(navDailyTracker);
            case FOOD_LIBRARY -> setActiveNavStyle(navFoodLibrary);
            case REPORTS -> setActiveNavStyle(navReports);
            case PROFILE_GOALS -> setActiveNavStyle(navProfileGoals);
            case PHONE_CONNECT -> setActiveNavStyle(navPhoneConnect);
            case SETTINGS -> setActiveNavStyle(navSettings);
            case ABOUT -> setActiveNavStyle(navAbout);
        }
    }

    private void resetNavStyles() {
        Button[] buttons = {navDashboard, navScanFood, navDailyTracker, navFoodLibrary, navReports, navProfileGoals, navPhoneConnect, navSettings, navAbout};
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("nav-button-active");
            }
        }
    }

    private void setActiveNavStyle(Button btn) {
        if (btn != null && !btn.getStyleClass().contains("nav-button-active")) {
            btn.getStyleClass().add("nav-button-active");
        }
    }

    private void updateServerStatusUi(boolean running, String ip, int port) {
        if (running) {
            circlePhoneStatus.setFill(Color.web("#2E7D32"));
            lblPhoneStatus.setText("Phone Bridge: " + ip + ":" + port + " (Online)");
        } else {
            circlePhoneStatus.setFill(Color.web("#94A3B8"));
            lblPhoneStatus.setText("Phone Bridge: Offline");
        }
    }

    // PhoneBridgeListener callbacks
    @Override
    public void onPhotoReceived(File savedFile, BufferedImage image, List<Prediction> predictions) {
        Platform.runLater(() -> {
            AppSettings settings = new SettingsDao(DatabaseManager.getInstance()).getSettings();
            if (settings.isAutoNavigateOnUpload()) {
                NavigationService nav = NavigationService.getInstance();
                nav.navigateTo(NavigationService.Page.SCAN_FOOD);
                ScanFoodController scanCtrl = nav.getController(NavigationService.Page.SCAN_FOOD);
                if (scanCtrl != null) {
                    scanCtrl.displayPhoneCaptureResult(savedFile, image, predictions);
                }
            }
        });
    }

    @Override
    public void onDeviceConnected(String userAgent, String clientIp) {
        Platform.runLater(() -> {
            lblPhoneStatus.setText("Device Connected: " + clientIp);
            circlePhoneStatus.setFill(Color.web("#1E88E5"));
        });
    }

    @Override
    public void onServerStatusChanged(boolean running, String hostIp, int port) {
        Platform.runLater(() -> updateServerStatusUi(running, hostIp, port));
    }

    @Override
    public void onServerError(String message) {
        Platform.runLater(() -> {
            lblPhoneStatus.setText("Bridge Error: " + message);
            circlePhoneStatus.setFill(Color.web("#E53935"));
        });
    }
}
