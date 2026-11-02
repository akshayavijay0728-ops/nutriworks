package com.nutriworks.ui.pages;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.SettingsDao;
import com.nutriworks.model.AppSettings;
import com.nutriworks.model.Prediction;
import com.nutriworks.server.BridgeTokenManager;
import com.nutriworks.server.PhoneBridgeServer;
import com.nutriworks.ui.NutriWorksApp;
import com.nutriworks.util.QrCodeGenerator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Phone Connect (QR Bridge) page with live server control,
 * token timer, troubleshooting tips, and real-time connection log.
 */
public class PhoneConnectController implements Initializable, PhoneBridgeServer.PhoneBridgeListener {

    @FXML private ImageView imgQrCode;
    @FXML private Label lblBridgeUrl;
    @FXML private Label lblTokenCountdown;
    @FXML private Circle circleServerStatus;
    @FXML private Label lblServerStatus;
    @FXML private Button btnToggleServer;
    @FXML private ListView<String> listConnectedDevices;
    @FXML private TextArea txtLiveLogs;

    @FXML private VBox troubleshootingBox;

    private Timeline countdownTimeline;
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server != null) {
            server.addListener(this);
            updateServerStatus(server.isRunning(), server.getCurrentIp(), server.getCurrentPort());
        }

        refreshQrCode();
        startCountdownTimer();
        logMessage("Phone Connect interface initialized.");
    }

    private void startCountdownTimer() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            BridgeTokenManager tokenMgr = NutriWorksApp.getTokenManager();
            if (tokenMgr != null) {
                BridgeTokenManager.TokenInfo info = tokenMgr.getCurrentToken();
                if (info != null) {
                    long remaining = info.getRemainingSeconds();
                    long mins = remaining / 60;
                    long secs = remaining % 60;
                    lblTokenCountdown.setText(String.format("Token expires in: %02d:%02d", mins, secs));

                    if (info.isExpired()) {
                        lblTokenCountdown.setText("Token EXPIRED! Click 'Regenerate QR'.");
                        lblTokenCountdown.setTextFill(Color.web("#DC2626"));
                    } else {
                        lblTokenCountdown.setTextFill(Color.web("#059669"));
                    }
                }
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    public void refreshQrCode() {
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server != null && server.isRunning()) {
            String url = server.getBridgeUrl();
            lblBridgeUrl.setText(url);
            Image qrImage = QrCodeGenerator.generateQrFxImage(url, 260, 260);
            if (qrImage != null) {
                imgQrCode.setImage(qrImage);
            }
        } else {
            lblBridgeUrl.setText("Server is stopped. Start server to generate QR code.");
            imgQrCode.setImage(null);
        }
    }

    @FXML
    private void onToggleServer() {
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server == null) return;

        if (server.isRunning()) {
            server.stopServer();
            logMessage("Stopped embedded web server.");
        } else {
            AppSettings settings = new SettingsDao(DatabaseManager.getInstance()).getSettings();
            server.startServer(settings.getLanIpOverride(), settings.getServerPort());
            logMessage("Started embedded web server on port " + server.getCurrentPort());
        }
        refreshQrCode();
    }

    @FXML
    private void onRegenerateToken() {
        BridgeTokenManager tokenMgr = NutriWorksApp.getTokenManager();
        if (tokenMgr != null) {
            tokenMgr.regenerateToken(15);
            logMessage("Regenerated new 15-minute token: " + tokenMgr.getCurrentToken().getToken());
            refreshQrCode();
        }
    }

    @FXML
    private void onCopyUrl() {
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server != null && server.isRunning()) {
            String url = server.getBridgeUrl();
            ClipboardContent content = new ClipboardContent();
            content.putString(url);
            Clipboard.getSystemClipboard().setContent(content);
            logMessage("Copied URL to system clipboard: " + url);
            new Alert(Alert.AlertType.INFORMATION, "Copied URL to clipboard:\n" + url).show();
        }
    }

    @FXML
    private void onTestConnection() {
        PhoneBridgeServer server = NutriWorksApp.getPhoneBridgeServer();
        if (server == null || !server.isRunning()) {
            new Alert(Alert.AlertType.WARNING, "Server is not running. Please start it first.").show();
            return;
        }

        new Thread(() -> {
            try {
                URL testUrl = new URL("http://127.0.0.1:" + server.getCurrentPort() + "/api/status/" + NutriWorksApp.getTokenManager().getCurrentToken().getToken());
                HttpURLConnection conn = (HttpURLConnection) testUrl.openConnection();
                conn.setConnectTimeout(3000);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                Platform.runLater(() -> {
                    if (code == 200) {
                        logMessage("Self-test connection successful (HTTP 200 OK).");
                        new Alert(Alert.AlertType.INFORMATION, "Local Server Test PASSED (HTTP 200 OK).\nServer is responding properly.").show();
                    } else {
                        logMessage("Self-test returned HTTP " + code);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    logMessage("Self-test error: " + e.getMessage());
                    new Alert(Alert.AlertType.ERROR, "Self-test failed: " + e.getMessage()).show();
                });
            }
        }).start();
    }

    private void updateServerStatus(boolean running, String ip, int port) {
        if (running) {
            circleServerStatus.setFill(Color.web("#2E7D32"));
            lblServerStatus.setText(String.format("Server Online &bull; http://%s:%d", ip, port));
            btnToggleServer.setText("Stop Server");
            btnToggleServer.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-weight: bold;");
        } else {
            circleServerStatus.setFill(Color.web("#94A3B8"));
            lblServerStatus.setText("Server Offline");
            btnToggleServer.setText("Start Server");
            btnToggleServer.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-weight: bold;");
        }
    }

    private void logMessage(String msg) {
        String entry = "[" + LocalTime.now().format(timeFmt) + "] " + msg;
        if (txtLiveLogs != null) {
            txtLiveLogs.appendText(entry + "\n");
        }
    }

    // Server Listener
    @Override
    public void onPhotoReceived(File savedFile, BufferedImage image, List<Prediction> predictions) {
        Platform.runLater(() -> {
            String label = !predictions.isEmpty() ? predictions.get(0).getLabel() : "Unknown";
            logMessage("Photo received from mobile bridge! Classified as: " + label);
        });
    }

    @Override
    public void onDeviceConnected(String userAgent, String clientIp) {
        Platform.runLater(() -> {
            logMessage("Device connected from " + clientIp + " (" + userAgent + ")");
            String item = clientIp + " - " + userAgent;
            if (!listConnectedDevices.getItems().contains(item)) {
                listConnectedDevices.getItems().add(0, item);
            }
        });
    }

    @Override
    public void onServerStatusChanged(boolean running, String hostIp, int port) {
        Platform.runLater(() -> {
            updateServerStatus(running, hostIp, port);
            refreshQrCode();
        });
    }

    @Override
    public void onServerError(String message) {
        Platform.runLater(() -> logMessage("SERVER ERROR: " + message));
    }
}
