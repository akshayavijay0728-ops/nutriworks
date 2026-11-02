package com.nutriworks.ui;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

/**
 * Service managing JavaFX desktop page routing, forward/back navigation history,
 * and smooth fade transitions.
 */
public class NavigationService {

    public enum Page {
        DASHBOARD("Dashboard", "/com/nutriworks/ui/pages/dashboard.fxml", 1),
        SCAN_FOOD("Scan Food", "/com/nutriworks/ui/pages/scan_food.fxml", 2),
        DAILY_TRACKER("Daily Tracker", "/com/nutriworks/ui/pages/daily_tracker.fxml", 3),
        FOOD_LIBRARY("Food Library", "/com/nutriworks/ui/pages/food_library.fxml", 4),
        REPORTS("Reports & Analytics", "/com/nutriworks/ui/pages/reports.fxml", 5),
        PROFILE_GOALS("Profile & Goals", "/com/nutriworks/ui/pages/profile_goals.fxml", 6),
        PHONE_CONNECT("Phone Connect (QR)", "/com/nutriworks/ui/pages/phone_connect.fxml", 7),
        SETTINGS("Settings", "/com/nutriworks/ui/pages/settings.fxml", 8),
        ABOUT("About", "/com/nutriworks/ui/pages/about.fxml", 9);

        private final String title;
        private final String fxmlPath;
        private final int shortcutIndex;

        Page(String title, String fxmlPath, int shortcutIndex) {
            this.title = title;
            this.fxmlPath = fxmlPath;
            this.shortcutIndex = shortcutIndex;
        }

        public String getTitle() {
            return title;
        }

        public String getFxmlPath() {
            return fxmlPath;
        }

        public int getShortcutIndex() {
            return shortcutIndex;
        }
    }

    private static NavigationService instance;

    private StackPane contentArea;
    private final Deque<Page> backStack = new ArrayDeque<>();
    private final Deque<Page> forwardStack = new ArrayDeque<>();
    private Page currentPage;
    private final Map<Page, Object> controllerCache = new EnumMap<>(Page.class);
    private final Map<Page, Node> viewCache = new EnumMap<>(Page.class);
    private Runnable pageChangeListener;

    public static synchronized NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    public void init(StackPane contentArea, Runnable pageChangeListener) {
        this.contentArea = contentArea;
        this.pageChangeListener = pageChangeListener;
    }

    public void navigateTo(Page page) {
        if (page == null || page == currentPage) {
            return;
        }

        if (currentPage != null) {
            backStack.push(currentPage);
            forwardStack.clear();
        }

        loadAndDisplayPage(page);
    }

    public boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public boolean canGoForward() {
        return !forwardStack.isEmpty();
    }

    public void goBack() {
        if (canGoBack()) {
            forwardStack.push(currentPage);
            Page prev = backStack.pop();
            loadAndDisplayPage(prev);
        }
    }

    public void goForward() {
        if (canGoForward()) {
            backStack.push(currentPage);
            Page next = forwardStack.pop();
            loadAndDisplayPage(next);
        }
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    public <T> T getController(Page page) {
        return (T) controllerCache.get(page);
    }

    private void loadAndDisplayPage(Page page) {
        try {
            Node node = viewCache.get(page);
            if (node == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(page.getFxmlPath()));
                node = loader.load();
                viewCache.put(page, node);
                controllerCache.put(page, loader.getController());
            }

            this.currentPage = page;

            // Fade transition
            node.setOpacity(0.0);
            contentArea.getChildren().setAll(node);

            FadeTransition ft = new FadeTransition(Duration.millis(160), node);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            if (pageChangeListener != null) {
                pageChangeListener.run();
            }
        } catch (IOException e) {
            System.err.println("Failed loading FXML for page " + page + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
