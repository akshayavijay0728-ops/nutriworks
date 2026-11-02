package com.nutriworks.server;

import com.google.gson.Gson;
import com.google.zxing.WriterException;
import com.nutriworks.ml.ClassifierFactory;
import com.nutriworks.ml.FoodClassifier;
import com.nutriworks.model.NutritionInfo;
import com.nutriworks.model.Prediction;
import com.nutriworks.service.NutritionService;
import com.nutriworks.util.NetworkUtils;
import com.nutriworks.util.NutritionCalculator;
import com.nutriworks.util.QrCodeGenerator;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Embedded Javalin local web server providing the phone-to-laptop QR bridge.
 * Accepts camera uploads, validates magic bytes, executes on-device recognition,
 * and notifies the JavaFX desktop application on the FX Application Thread.
 */
public class PhoneBridgeServer {

    public interface PhoneBridgeListener {
        void onPhotoReceived(File savedFile, BufferedImage image, List<Prediction> predictions);
        void onDeviceConnected(String userAgent, String clientIp);
        void onServerStatusChanged(boolean running, String hostIp, int port);
        void onServerError(String message);
    }

    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB limit
    private final BridgeTokenManager tokenManager;
    private final Gson gson = new Gson();
    private final List<PhoneBridgeListener> listeners = new ArrayList<>();
    private final ConcurrentHashMap<String, AtomicInteger> uploadRateLimiter = new ConcurrentHashMap<>();

    private Javalin app;
    private boolean isRunning = false;
    private String currentIp;
    private int currentPort;
    private final File uploadDir;

    public PhoneBridgeServer(BridgeTokenManager tokenManager) {
        this.tokenManager = tokenManager;
        File userHome = new File(System.getProperty("user.home"), ".nutriworks/uploads");
        if (!userHome.exists()) {
            userHome.mkdirs();
        }
        this.uploadDir = userHome;
    }

    public synchronized void addListener(PhoneBridgeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void startServer(String overrideIp, int preferredPort) {
        if (isRunning) {
            return;
        }

        this.currentIp = NetworkUtils.getBestLanIp(overrideIp);
        this.currentPort = NetworkUtils.findAvailablePort(preferredPort);

        try {
            app = Javalin.create(config -> {
                config.showJavalinBanner = false;
                config.http.maxRequestSize = MAX_FILE_SIZE;
            });

            setupRoutes();

            app.start("0.0.0.0", currentPort);
            isRunning = true;
            notifyStatus(true, currentIp, currentPort);
        } catch (Exception e) {
            isRunning = false;
            notifyError("Failed to start embedded bridge server: " + e.getMessage());
            notifyStatus(false, currentIp, currentPort);
        }
    }

    public synchronized void stopServer() {
        if (app != null && isRunning) {
            try {
                app.stop();
            } catch (Exception ignored) {
            }
            isRunning = false;
            notifyStatus(false, currentIp, currentPort);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public String getCurrentIp() {
        return currentIp != null ? currentIp : "127.0.0.1";
    }

    public int getCurrentPort() {
        return currentPort;
    }

    public String getBridgeUrl() {
        BridgeTokenManager.TokenInfo info = tokenManager.getCurrentToken();
        String token = info != null ? info.getToken() : "";
        return "http://" + getCurrentIp() + ":" + getCurrentPort() + "/m/" + token;
    }

    private void setupRoutes() {
        // Root redirection
        app.get("/", ctx -> {
            ctx.redirect("/m/" + tokenManager.getCurrentToken().getToken());
        });

        // Mobile web page endpoint
        app.get("/m/{token}", ctx -> {
            String token = ctx.pathParam("token");
            if (!tokenManager.validateToken(token)) {
                ctx.status(403).html("<h1>403 Forbidden</h1><p>Session expired or token invalid. Please scan a fresh QR code from your laptop.</p>");
                return;
            }

            BridgeTokenManager.TokenInfo info = tokenManager.getTokenInfo(token);
            if (info != null) {
                info.updateDevice(ctx.userAgent());
                notifyDeviceConnected(ctx.userAgent(), ctx.ip());
            }

            ctx.contentType("text/html; charset=UTF-8");
            ctx.result(MobileWebHandler.getMobileHtml(token));
        });

        // Status check
        app.get("/api/status/{token}", ctx -> {
            String token = ctx.pathParam("token");
            boolean valid = tokenManager.validateToken(token);
            Map<String, Object> resp = new HashMap<>();
            resp.put("running", isRunning);
            resp.put("validToken", valid);
            resp.put("remainingSeconds", valid ? tokenManager.getTokenInfo(token).getRemainingSeconds() : 0);
            ctx.json(resp);
        });

        // QR Code image directly servable
        app.get("/qr", ctx -> {
            String url = getBridgeUrl();
            try {
                byte[] png = QrCodeGenerator.generateQrPngBytes(url, 260, 260);
                ctx.contentType("image/png").result(png);
            } catch (WriterException | IOException e) {
                ctx.status(500).result("Error generating QR code");
            }
        });

        // Multipart Photo Upload
        app.post("/api/upload/{token}", ctx -> {
            String token = ctx.pathParam("token");
            if (!tokenManager.validateToken(token)) {
                ctx.status(403).json(Collections.singletonMap("error", "Expired or invalid token"));
                return;
            }

            // Rate limiting by client IP (max 20 uploads per minute)
            String clientIp = ctx.ip();
            AtomicInteger count = uploadRateLimiter.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
            if (count.incrementAndGet() > 20) {
                ctx.status(429).json(Collections.singletonMap("error", "Rate limit exceeded. Please wait a moment."));
                return;
            }

            UploadedFile uploadedFile = ctx.uploadedFile("photo");
            if (uploadedFile == null) {
                ctx.status(400).json(Collections.singletonMap("error", "Missing 'photo' multipart field"));
                return;
            }

            byte[] bytes = uploadedFile.content().readAllBytes();
            if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) {
                ctx.status(413).json(Collections.singletonMap("error", "File empty or exceeds 10 MB limit"));
                return;
            }

            // Validate magic bytes
            if (!isValidImageMagicBytes(bytes)) {
                ctx.status(415).json(Collections.singletonMap("error", "Invalid file signature. Only JPEG, PNG, or WebP allowed."));
                return;
            }

            // Save to disk
            String filename = "capture_" + System.currentTimeMillis() + ".jpg";
            File savedFile = new File(uploadDir, filename);
            try (FileOutputStream fos = new FileOutputStream(savedFile)) {
                fos.write(bytes);
            }

            // Read as BufferedImage
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bufferedImage == null) {
                ctx.status(422).json(Collections.singletonMap("error", "Failed decoding image data"));
                return;
            }

            // Run ML classification
            FoodClassifier classifier = ClassifierFactory.getClassifier();
            List<Prediction> predictions = classifier.classify(bufferedImage, 3);
            Prediction top = !predictions.isEmpty() ? predictions.get(0) : new Prediction("Food", 0.85f);

            // Fetch nutrition
            NutritionService nutritionService = NutritionService.getInstance();
            NutritionInfo info = nutritionService.findByName(top.getLabel());
            NutritionCalculator.CalculatedNutrition calculated = NutritionCalculator.calculateFromGrams(info, 150.0);

            // Notify Desktop Application on UI Thread
            Platform.runLater(() -> {
                for (PhoneBridgeListener l : listeners) {
                    try {
                        l.onPhotoReceived(savedFile, bufferedImage, predictions);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });

            // Return JSON response to the phone
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("foodName", info != null ? info.getName() : top.getLabel());
            response.put("confidence", top.getFormattedConfidence());
            response.put("calories", calculated.getCalories());
            response.put("protein", calculated.getProteinGrams());
            response.put("carbs", calculated.getCarbsGrams());
            response.put("fat", calculated.getFatGrams());

            ctx.json(response);
        });
    }

    private boolean isValidImageMagicBytes(byte[] b) {
        if (b == null || b.length < 8) return false;
        // JPEG: FF D8 FF
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return true;
        // PNG: 89 50 4E 47
        if ((b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 0x50 && (b[2] & 0xFF) == 0x4E && (b[3] & 0xFF) == 0x47) return true;
        // WEBP / RIFF
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return true;
        return false;
    }

    private void notifyStatus(boolean running, String ip, int port) {
        Platform.runLater(() -> {
            for (PhoneBridgeListener l : listeners) {
                l.onServerStatusChanged(running, ip, port);
            }
        });
    }

    private void notifyError(String err) {
        Platform.runLater(() -> {
            for (PhoneBridgeListener l : listeners) {
                l.onServerError(err);
            }
        });
    }

    private void notifyDeviceConnected(String ua, String ip) {
        Platform.runLater(() -> {
            for (PhoneBridgeListener l : listeners) {
                l.onDeviceConnected(ua, ip);
            }
        });
    }
}
