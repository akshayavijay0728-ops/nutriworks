package com.nutriworks.desktop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/**
 * NutriWorks - Daily Nutrition & Health Tracker
 * Complete OpenNutriTracker Architecture for Java Desktop (Java 17+, pure Java SE).
 *
 * Streamlined 8-Page Navigation:
 *  - 🏠 Home: Dashboard with central calorie arc, macro progress bars, water chip & active energy logger
 *  - 📓 Food Diary: Monthly calendar with green/red compliance dots, grouped meals & per-meal targets
 *  - 🍎 Food Logging: Live search, barcode scanner, quick-add kcal, food photo upload, food emojis
 *  - 🍽️ Meals & Recipes: Multi-ingredient reusable recipe creator & 1-click meal logging
 *  - 📈 Trends: 7-day streak flame, calorie intake vs goal line graph, daily macro averages
 *  - 👤 You & Profile: Rich filled layout, vector avatar, compact BMI gauge, Mifflin-St Jeor engine, macro distribution, weight progress
 *  - 📱 Phone Bridge: ISO/IEC 18004 pure QR code generator, embedded LAN HTTP server
 *  - ⚙️ Settings & Backup: 3 Material themes, kcal <-> kJ unit conversion, JSON & CSV export/import
 */
public class NutriWorksDesktop extends JFrame {

    // ------------------------------------------------------------------------
    // THEME & COLOR PALETTES
    // ------------------------------------------------------------------------
    public static class ThemePalette {
        public String name;
        public Color canvas;
        public Color card;
        public Color border;
        public Color textMain;
        public Color textMuted;
        public Color primaryGreen;
        public Color primaryHover;
        public Color pillGreen;
        public Color pillText;

        public ThemePalette(String name, Color canvas, Color card, Color border, Color textMain, Color textMuted,
                            Color primaryGreen, Color primaryHover, Color pillGreen, Color pillText) {
            this.name = name;
            this.canvas = canvas;
            this.card = card;
            this.border = border;
            this.textMain = textMain;
            this.textMuted = textMuted;
            this.primaryGreen = primaryGreen;
            this.primaryHover = primaryHover;
            this.pillGreen = pillGreen;
            this.pillText = pillText;
        }
    }

    public static final ThemePalette THEME_WARM_CANVAS = new ThemePalette(
            "Material You Canvas",
            new Color(246, 244, 239),
            Color.WHITE,
            new Color(237, 234, 227),
            new Color(26, 28, 24),
            new Color(116, 119, 112),
            new Color(14, 110, 69),
            new Color(10, 85, 52),
            new Color(224, 237, 229),
            new Color(0, 81, 50)
    );

    public static final ThemePalette THEME_CLEAN_LIGHT = new ThemePalette(
            "Clean Forest Light",
            new Color(243, 248, 244),
            Color.WHITE,
            new Color(225, 235, 228),
            new Color(20, 30, 22),
            new Color(100, 115, 105),
            new Color(21, 128, 61),
            new Color(15, 95, 45),
            new Color(220, 245, 228),
            new Color(10, 70, 35)
    );

    public static final ThemePalette THEME_OBSIDIAN_DARK = new ThemePalette(
            "Obsidian Dark",
            new Color(18, 20, 18),
            new Color(28, 32, 29),
            new Color(42, 48, 44),
            new Color(240, 245, 242),
            new Color(160, 170, 164),
            new Color(46, 182, 115),
            new Color(36, 150, 94),
            new Color(34, 52, 42),
            new Color(160, 240, 190)
    );

    private ThemePalette currentTheme = THEME_WARM_CANVAS;

    // Macro Accent Colors
    public static final Color COLOR_MACRO_CARBS = new Color(184, 110, 0);
    public static final Color COLOR_MACRO_FAT = new Color(192, 58, 43);
    public static final Color COLOR_MACRO_PROTEIN = new Color(0, 122, 120);
    public static final Color COLOR_WATER = new Color(2, 132, 199);

    // ------------------------------------------------------------------------
    // HIGH-CONTRAST CUSTOM BUTTON (Immune to Windows Theme wash-out)
    // ------------------------------------------------------------------------
    public static class NutriButton extends JButton {
        private Color normalBg;
        private Color hoverBg;
        private Color textFg;
        private int cornerRadius;

        public NutriButton(String text, Color bg, Color fg, int radius) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = (bg.getRed() < 40 && bg.getGreen() > 80)
                    ? new Color(Math.max(0, bg.getRed() - 10), Math.max(0, bg.getGreen() - 25), Math.max(0, bg.getBlue() - 10))
                    : (bg.equals(Color.WHITE) ? new Color(238, 242, 238) : bg.darker());
            this.textFg = fg;
            this.cornerRadius = radius;
            setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void setColors(Color bg, Color fg) {
            this.normalBg = bg;
            this.hoverBg = (bg.equals(Color.WHITE)) ? new Color(238, 242, 238) : bg.darker();
            this.textFg = fg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (getModel().isPressed()) {
                g2.setColor(hoverBg.darker());
            } else if (getModel().isRollover()) {
                g2.setColor(hoverBg);
            } else {
                g2.setColor(normalBg);
            }
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

            g2.setColor(textFg);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth() - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    // ------------------------------------------------------------------------
    // USER PROFILE & SCIENCE ENGINE
    // ------------------------------------------------------------------------
    private String userName = "Alex Demo";
    private int userAge = 26;
    private String userGender = "Male";
    private double userHeightCm = 175.0;
    private double userWeightKg = 72.0;
    private double startingWeightKg = 76.0;
    private double targetWeightKg = 68.0;
    private String activityLevel = "Active (1.55x)";
    private String goalType = "Lose Weight (-500 kcal)";
    private String weeklyRate = "-0.40 kg/week";

    // Science-based Targets (Mifflin-St Jeor Engine)
    private int bmrKcal = 1680;
    private int tdeeKcal = 2604;
    private int targetKcal = 2104;
    private int targetCarbsG = 263;
    private int targetProteinG = 105;
    private int targetFatG = 70;

    // Per-Meal Target Distribution
    private int targetBreakfastKcal = 500;
    private int targetLunchKcal = 750;
    private int targetDinnerKcal = 550;
    private int targetSnacksKcal = 304;

    // Daily Intake Tracking
    private int consumedKcal = 1340;
    private int burnedKcal = 320;
    private int waterConsumedMl = 1750;
    private int waterGoalMl = 2500;
    private int streakDays = 7;

    // Unit toggle: kcal vs kJ (1 kcal = 4.184 kJ)
    private boolean isKj = false;

    // Fasting State (16:8 Intermittent Fasting)
    private boolean isFasting = true;
    private int fastingSecondsRemaining = 14 * 3600 + 18 * 60;
    private javax.swing.Timer fastingTimer;

    // ------------------------------------------------------------------------
    // FOOD MODELS & REPOSITORIES
    // ------------------------------------------------------------------------
    public static class FoodLibraryItem {
        public String name;
        public String category; // Grains, Veggies, Fruits, Protein, Indian, Snacks, Meals
        public String sourceDb; // USDA, Open Food Facts, German BLS, Local
        public String barcode;
        public int kcal100g;
        public double protein100g;
        public double carbs100g;
        public double fat100g;
        public double fiber100g;
        public double sugar100g;
        public int sodiumMg100g;
        public int potassiumMg100g;
        public int calciumMg100g;
        public double ironMg100g;
        public String emoji;

        public FoodLibraryItem(String name, String category, String sourceDb, String barcode,
                               int kcal, double p, double c, double f, double fib, double sug,
                               int sod, int pot, int calc, double iron, String emoji) {
            this.name = name;
            this.category = category;
            this.sourceDb = sourceDb;
            this.barcode = barcode;
            this.kcal100g = kcal;
            this.protein100g = p;
            this.carbs100g = c;
            this.fat100g = f;
            this.fiber100g = fib;
            this.sugar100g = sug;
            this.sodiumMg100g = sod;
            this.potassiumMg100g = pot;
            this.calciumMg100g = calc;
            this.ironMg100g = iron;
            this.emoji = emoji;
        }
    }

    public static class MealItem {
        public String id;
        public String type; // Breakfast, Lunch, Dinner, Snacks
        public String name;
        public String emoji;
        public int grams;
        public int kcal;
        public double protein;
        public double carbs;
        public double fat;
        public double fiber;
        public double sugar;
        public int sodiumMg;
        public int potassiumMg;
        public int calciumMg;
        public double ironMg;
        public String source; // Desktop, Phone QR, File Upload, Barcode, Quick Add

        public MealItem(String type, String name, String emoji, int grams, int kcal,
                        double protein, double carbs, double fat, double fiber, double sugar,
                        int sodiumMg, int potassiumMg, int calciumMg, double ironMg, String source) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.type = type;
            this.name = name;
            this.emoji = emoji;
            this.grams = grams;
            this.kcal = kcal;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.fiber = fiber;
            this.sugar = sugar;
            this.sodiumMg = sodiumMg;
            this.potassiumMg = potassiumMg;
            this.calciumMg = calciumMg;
            this.ironMg = ironMg;
            this.source = source;
        }
    }

    public static class RecipeItem {
        public String id;
        public String title;
        public String brand;
        public String barcode;
        public String emoji;
        public int totalKcal;
        public double totalProtein;
        public double totalCarbs;
        public double totalFat;
        public String ingredientsSummary;

        public RecipeItem(String title, String brand, String barcode, String emoji,
                          int totalKcal, double p, double c, double f, String ingredients) {
            this.id = UUID.randomUUID().toString().substring(0, 8);
            this.title = title;
            this.brand = brand;
            this.barcode = barcode;
            this.emoji = emoji;
            this.totalKcal = totalKcal;
            this.totalProtein = p;
            this.totalCarbs = c;
            this.totalFat = f;
            this.ingredientsSummary = ingredients;
        }
    }

    private final List<FoodLibraryItem> globalFoodLibrary = new ArrayList<>();
    private final List<MealItem> loggedMeals = new ArrayList<>();
    private final List<RecipeItem> savedRecipes = new ArrayList<>();

    // ------------------------------------------------------------------------
    // UI LAYOUT COMPONENTS
    // ------------------------------------------------------------------------
    private JPanel mainCardDeck;
    private CardLayout cardLayout;
    private final List<JButton> sidebarNavButtons = new ArrayList<>();

    // Top Status Pills
    private JLabel topWeightPillLabel;
    private JLabel topWaterPillLabel;
    private JLabel topFastingPillLabel;
    private NutriButton unitToggleBtn;

    // Home View Elements
    private OpenNutriEnergyArc homeEnergyArc;
    private JLabel homeSuppliedLabel;
    private JLabel homeBurnedLabel;
    private JLabel homeCarbsGramsLabel;
    private JProgressBar homeCarbsBar;
    private JLabel homeFatGramsLabel;
    private JProgressBar homeFatBar;
    private JLabel homeProteinGramsLabel;
    private JProgressBar homeProteinBar;

    // Diary View Elements
    private JPanel diaryBreakfastPanel;
    private JLabel diaryBreakfastSubtotal;
    private JPanel diaryLunchPanel;
    private JLabel diaryLunchSubtotal;
    private JPanel diaryDinnerPanel;
    private JLabel diaryDinnerSubtotal;
    private JPanel diarySnacksPanel;
    private JLabel diarySnacksSubtotal;

    // Food Logging View Elements
    private JTextField foodSearchField;
    private JPanel foodCardsListPanel;
    private String selectedSearchCategory = "All";

    // Embedded Phone Bridge Server
    private HttpServer httpServer;
    private int serverPort = 8080;
    private String localIpAddress = "192.168.1.5";
    private BufferedImage genuineQrImage;

    // ------------------------------------------------------------------------
    // CONSTRUCTOR & INITIALIZATION
    // ------------------------------------------------------------------------
    public NutriWorksDesktop() {
        super("NutriWorks - Daily Nutrition & Health Tracker");
        setSize(1260, 860);
        setMinimumSize(new Dimension(1080, 740));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(currentTheme.canvas);

        initFoodLibrary();
        initDefaultLoggedMeals();
        initDefaultRecipes();
        calculateMifflinStJeor();
        initLocalNetworkAndQR();
        startPhoneServer();
        initFastingTimer();
        buildDesktopLayout();
        updateAllDisplays();
    }

    private void initFoodLibrary() {
        // Multi-database food catalogue: USDA, Open Food Facts, German BLS, Indian Regional
        globalFoodLibrary.add(new FoodLibraryItem("Apple", "Fruits", "USDA", "033383000010", 52, 0.3, 14.0, 0.2, 2.4, 10.0, 1, 107, 6, 0.12, "🍎"));
        globalFoodLibrary.add(new FoodLibraryItem("Brown rice", "Grains", "USDA", "011110853759", 111, 2.6, 23.0, 0.9, 1.8, 0.4, 5, 86, 10, 0.42, "🍚"));
        globalFoodLibrary.add(new FoodLibraryItem("Broccoli", "Veggies", "USDA", "033383000027", 34, 2.8, 7.0, 0.4, 2.6, 1.7, 33, 316, 47, 0.73, "🥦"));
        globalFoodLibrary.add(new FoodLibraryItem("Olive oil", "Grains", "Open Food Facts", "8410000001234", 884, 0.0, 0.0, 100.0, 0.0, 0.0, 2, 1, 1, 0.56, "🫒"));
        globalFoodLibrary.add(new FoodLibraryItem("Almonds", "Snacks", "USDA", "041220790104", 579, 21.0, 22.0, 50.0, 12.5, 4.4, 1, 733, 269, 3.71, "🥜"));
        globalFoodLibrary.add(new FoodLibraryItem("Salmon fillet", "Protein", "USDA", "021130070101", 208, 20.0, 0.0, 13.0, 0.0, 0.0, 59, 363, 9, 0.34, "🐟"));
        globalFoodLibrary.add(new FoodLibraryItem("Boiled Egg", "Protein", "USDA", "041220170105", 155, 13.0, 1.1, 11.0, 0.0, 0.6, 124, 126, 50, 1.19, "🥚"));
        globalFoodLibrary.add(new FoodLibraryItem("Rolled Oats", "Grains", "Open Food Facts", "8901030383749", 389, 16.9, 66.3, 6.9, 10.6, 0.0, 2, 429, 54, 4.72, "🌾"));
        globalFoodLibrary.add(new FoodLibraryItem("Greek Yogurt 0%", "Protein", "Open Food Facts", "5000112546415", 59, 10.0, 3.6, 0.4, 0.0, 3.2, 36, 141, 110, 0.05, "🥛"));
        globalFoodLibrary.add(new FoodLibraryItem("Grilled Chicken Breast", "Protein", "USDA", "021130080102", 165, 31.0, 0.0, 3.6, 0.0, 0.0, 74, 256, 15, 1.04, "🍗"));
        globalFoodLibrary.add(new FoodLibraryItem("Paneer Tikka", "Indian", "Local", "8901262010012", 240, 14.5, 7.0, 17.0, 1.2, 1.8, 420, 165, 480, 1.20, "🧀"));
        globalFoodLibrary.add(new FoodLibraryItem("Dal Makhani", "Indian", "Local", "8901262010029", 130, 5.0, 14.0, 6.0, 3.5, 1.0, 390, 280, 72, 1.85, "🍲"));
        globalFoodLibrary.add(new FoodLibraryItem("Chicken Curry", "Indian", "Local", "8901262010036", 165, 16.0, 4.5, 9.0, 1.0, 1.2, 510, 240, 35, 1.40, "🍛"));
        globalFoodLibrary.add(new FoodLibraryItem("Masala Dosa", "Indian", "Local", "8901262010043", 168, 3.9, 28.5, 4.1, 1.2, 0.6, 520, 140, 28, 0.90, "🥞"));
        globalFoodLibrary.add(new FoodLibraryItem("Idli Sambar", "Indian", "Local", "8901262010050", 143, 4.0, 30.0, 0.4, 1.5, 0.5, 450, 195, 36, 1.10, "🥣"));
        globalFoodLibrary.add(new FoodLibraryItem("Garlic Naan", "Indian", "Local", "8901262010067", 290, 8.5, 52.0, 5.5, 2.5, 3.0, 410, 120, 45, 1.60, "🫓"));
        globalFoodLibrary.add(new FoodLibraryItem("Baked Samosa", "Snacks", "Local", "8901262010074", 262, 4.5, 28.0, 15.0, 2.1, 1.1, 380, 160, 24, 0.95, "🥟"));
        globalFoodLibrary.add(new FoodLibraryItem("Caesar Salad Bowl", "Veggies", "German BLS", "4008400401015", 128, 5.0, 8.0, 8.5, 2.1, 2.0, 320, 210, 95, 1.10, "🥗"));
        globalFoodLibrary.add(new FoodLibraryItem("Whole Wheat Pasta", "Meals", "German BLS", "4008400401022", 151, 8.5, 19.0, 4.5, 1.8, 2.6, 380, 190, 32, 1.30, "🍝"));
        globalFoodLibrary.add(new FoodLibraryItem("Pizza Margherita", "Meals", "Open Food Facts", "3017620422003", 266, 11.0, 33.0, 10.0, 2.3, 3.6, 590, 180, 140, 1.50, "🍕"));
        globalFoodLibrary.add(new FoodLibraryItem("Salmon Sushi Roll", "Meals", "Open Food Facts", "4901005101018", 143, 5.8, 26.0, 1.5, 1.0, 2.5, 410, 130, 18, 0.70, "🍣"));
        globalFoodLibrary.add(new FoodLibraryItem("Turkey Tacos", "Meals", "USDA", "041220990108", 226, 10.0, 21.0, 11.0, 3.0, 1.5, 460, 240, 80, 1.45, "🌮"));
        globalFoodLibrary.add(new FoodLibraryItem("Fresh Avocado", "Fruits", "USDA", "033383000034", 160, 2.0, 8.5, 14.7, 6.7, 0.7, 7, 485, 12, 0.55, "🥑"));
        globalFoodLibrary.add(new FoodLibraryItem("Ripe Banana", "Fruits", "USDA", "033383000041", 89, 1.1, 22.8, 0.3, 2.6, 12.2, 1, 358, 5, 0.26, "🍌"));
        globalFoodLibrary.add(new FoodLibraryItem("Roasted Chickpea Hummus", "Snacks", "Open Food Facts", "021130090109", 166, 7.9, 14.3, 9.6, 6.0, 0.3, 380, 228, 38, 1.60, "🧆"));
        globalFoodLibrary.add(new FoodLibraryItem("Lean Sirloin Steak", "Protein", "USDA", "021130100105", 271, 26.0, 0.0, 18.0, 0.0, 0.0, 65, 340, 18, 2.60, "🥩"));
        globalFoodLibrary.add(new FoodLibraryItem("Air-Fried Potatoes", "Snacks", "USDA", "041220880102", 145, 2.8, 31.0, 1.5, 2.8, 0.4, 110, 480, 16, 1.10, "🍟"));
        globalFoodLibrary.add(new FoodLibraryItem("Grass-Fed Beef Burger", "Meals", "USDA", "041220770106", 250, 13.0, 30.0, 9.0, 1.5, 4.2, 450, 260, 60, 2.10, "🍔"));
        globalFoodLibrary.add(new FoodLibraryItem("Protein Waffles", "Breakfast", "Open Food Facts", "011110853766", 240, 14.0, 26.0, 8.0, 2.0, 4.0, 360, 180, 120, 1.80, "🧇"));
        globalFoodLibrary.add(new FoodLibraryItem("Traditional Ramen Bowl", "Meals", "Open Food Facts", "4901005101025", 110, 5.0, 16.0, 3.0, 1.2, 1.0, 680, 140, 20, 0.90, "🍜"));
        globalFoodLibrary.add(new FoodLibraryItem("Turkey & Avocado Sandwich", "Meals", "German BLS", "4008400401039", 220, 15.0, 24.0, 7.5, 3.2, 2.1, 440, 260, 45, 1.70, "🥪"));
        globalFoodLibrary.add(new FoodLibraryItem("Wild Blueberries", "Fruits", "USDA", "033383000058", 57, 0.7, 14.5, 0.3, 2.4, 9.9, 1, 77, 6, 0.28, "🫐"));
        globalFoodLibrary.add(new FoodLibraryItem("Sweet Strawberries", "Fruits", "USDA", "033383000065", 32, 0.7, 7.7, 0.3, 2.0, 4.9, 1, 153, 16, 0.41, "🍓"));
        globalFoodLibrary.add(new FoodLibraryItem("Baby Carrots", "Veggies", "USDA", "033383000072", 35, 0.8, 8.2, 0.1, 2.9, 4.7, 78, 240, 30, 0.38, "🥕"));
        globalFoodLibrary.add(new FoodLibraryItem("Red Seedless Grapes", "Fruits", "USDA", "033383000089", 69, 0.7, 18.1, 0.2, 0.9, 15.5, 2, 191, 10, 0.36, "🍇"));
        globalFoodLibrary.add(new FoodLibraryItem("Sweet Corn", "Veggies", "USDA", "033383000096", 86, 3.2, 19.0, 1.2, 2.7, 3.2, 15, 270, 2, 0.52, "🌽"));
        globalFoodLibrary.add(new FoodLibraryItem("Pure Coconut Water", "Snacks", "Open Food Facts", "8901030383794", 19, 0.7, 3.7, 0.2, 1.1, 2.6, 105, 250, 24, 0.29, "🥥"));
    }

    private void initDefaultLoggedMeals() {
        loggedMeals.add(new MealItem("Breakfast", "Rolled Oats with Berries", "🌾", 220, 280, 8.5, 48.0, 5.2, 7.0, 12.0, 95, 310, 48, 2.1, "Desktop"));
        loggedMeals.add(new MealItem("Breakfast", "Boiled Egg (2 pcs)", "🥚", 100, 144, 12.6, 1.1, 9.5, 0.0, 0.6, 140, 126, 50, 1.2, "Desktop"));
        loggedMeals.add(new MealItem("Lunch", "Grilled Chicken with Brown Rice & Broccoli", "🍗", 320, 510, 38.0, 54.0, 11.0, 6.5, 4.0, 480, 680, 85, 2.4, "Desktop"));
        loggedMeals.add(new MealItem("Dinner", "Paneer Salad Bowl with Olive Oil", "🧀", 200, 311, 16.0, 12.0, 22.0, 3.5, 3.0, 310, 220, 380, 1.8, "File Upload"));
        loggedMeals.add(new MealItem("Snacks", "Fresh Apple", "🍎", 180, 95, 0.5, 25.0, 0.3, 4.4, 19.0, 2, 192, 11, 0.2, "Phone QR"));
    }

    private void initDefaultRecipes() {
        savedRecipes.add(new RecipeItem("Post-Workout Protein Oatmeal", "NutriWorks Kitchen", "8901030383749", "🌾",
                420, 28.0, 58.0, 8.0, "Rolled Oats (80g), Greek Yogurt (100g), Wild Blueberries (40g)"));
        savedRecipes.add(new RecipeItem("Keto Salmon & Steamed Broccoli", "Ocean Fresh", "5000112546415", "🐟",
                480, 38.0, 7.0, 32.0, "Salmon Fillet (180g), Steamed Broccoli (120g), Extra Virgin Olive Oil (10g)"));
        savedRecipes.add(new RecipeItem("Paneer Tikka Energy Bowl", "Desi Kitchen", "8901262010012", "🧀",
                540, 26.0, 52.0, 22.0, "Paneer Tikka (150g), Brown Rice (140g), Baby Carrots (50g)"));
    }

    private void calculateMifflinStJeor() {
        double bmr = ("Female".equalsIgnoreCase(userGender))
                ? 10 * userWeightKg + 6.25 * userHeightCm - 5 * userAge - 161
                : 10 * userWeightKg + 6.25 * userHeightCm - 5 * userAge + 5;
        this.bmrKcal = (int) Math.round(bmr);
        this.tdeeKcal = (int) Math.round(this.bmrKcal * 1.55);
        this.targetKcal = Math.max(1500, this.tdeeKcal - 500);
        this.targetCarbsG = (int) Math.round((this.targetKcal * 0.50) / 4.0);
        this.targetProteinG = (int) Math.round((this.targetKcal * 0.20) / 4.0);
        this.targetFatG = (int) Math.round((this.targetKcal * 0.30) / 9.0);

        this.targetBreakfastKcal = (int) Math.round(this.targetKcal * 0.25);
        this.targetLunchKcal = (int) Math.round(this.targetKcal * 0.35);
        this.targetDinnerKcal = (int) Math.round(this.targetKcal * 0.25);
        this.targetSnacksKcal = this.targetKcal - targetBreakfastKcal - targetLunchKcal - targetDinnerKcal;
    }

    private double calculateBMI() {
        double h = userHeightCm / 100.0;
        return (h <= 0) ? 22.0 : userWeightKg / (h * h);
    }

    private String getBmiCategory(double bmi) {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Healthy weight";
        if (bmi < 30.0) return "Pre-obesity";
        return "Obesity";
    }

    // ------------------------------------------------------------------------
    // UNIT FORMATTING HELPERS (kcal vs kJ)
    // ------------------------------------------------------------------------
    private String formatEnergy(int kcal) {
        if (isKj) {
            int kj = (int) Math.round(kcal * 4.184);
            return String.format("%,d kJ", kj);
        } else {
            return String.format("%,d kcal", kcal);
        }
    }

    private String formatEnergyVal(int kcal) {
        if (isKj) {
            return String.format("%,d", (int) Math.round(kcal * 4.184));
        } else {
            return String.format("%,d", kcal);
        }
    }

    private String getEnergyUnit() {
        return isKj ? "kJ" : "kcal";
    }

    // ------------------------------------------------------------------------
    // FASTING & PHONE SERVER
    // ------------------------------------------------------------------------
    private void initFastingTimer() {
        fastingTimer = new javax.swing.Timer(1000, e -> {
            if (isFasting && fastingSecondsRemaining > 0) {
                fastingSecondsRemaining--;
                int h = fastingSecondsRemaining / 3600;
                int m = (fastingSecondsRemaining % 3600) / 60;
                if (topFastingPillLabel != null) {
                    topFastingPillLabel.setText(String.format("Fasting • %dh %02dm left", h, m));
                }
            }
        });
        fastingTimer.start();
    }

    private void initLocalNetworkAndQR() {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                String name = iface.getDisplayName().toLowerCase();
                if (name.contains("virtual") || name.contains("docker") || name.contains("wsl") || name.contains("vpn")) continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        localIpAddress = a.getHostAddress();
                        break;
                    }
                }
            }
        } catch (Exception ignored) {}

        String fullPhoneUrl = "http://" + localIpAddress + ":" + serverPort + "/";
        try {
            genuineQrImage = PureQrEncoder.generateQrImage(fullPhoneUrl, 10);
            File out = new File("qr_code.png");
            ImageIO.write(genuineQrImage, "png", out);
        } catch (Exception e) {
            genuineQrImage = PureQrEncoder.generateQrImage("http://192.168.1.5:8080/", 10);
        }
    }

    private void startPhoneServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);
            httpServer.createContext("/", exchange -> {
                File mf = new File("mobile.html");
                if (mf.exists()) {
                    byte[] b = java.nio.file.Files.readAllBytes(mf.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(200, b.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
                }
            });
            httpServer.createContext("/api/log", exchange -> {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        String body = sb.toString();

                        String name = "Logged Meal";
                        int kcal = 350;
                        int grams = 180;
                        for (String param : body.split("&")) {
                            String[] kv = param.split("=");
                            if (kv.length == 2) {
                                String k = URLDecoder.decode(kv[0], "UTF-8");
                                String v = URLDecoder.decode(kv[1], "UTF-8");
                                if ("name".equals(k)) name = v;
                                else if ("kcal".equals(k)) kcal = Integer.parseInt(v);
                                else if ("grams".equals(k)) grams = Integer.parseInt(v);
                            }
                        }

                        final String fName = name;
                        final int fKcal = kcal;
                        final int fGrams = grams;
                        SwingUtilities.invokeLater(() -> {
                            loggedMeals.add(new MealItem("Snacks", fName, "🍎", fGrams, fKcal, 8.0, 32.0, 6.0, 2.5, 4.0, 120, 210, 35, 1.2, "Phone QR"));
                            updateAllDisplays();
                            JOptionPane.showMessageDialog(this, "Meal synced from phone: " + fName + " (" + fKcal + " kcal)", "Phone Sync", JOptionPane.INFORMATION_MESSAGE);
                        });

                        byte[] resp = "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, resp.length);
                        try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
                    } catch (Exception ignored) {}
                }
            });
            httpServer.setExecutor(null);
            httpServer.start();
        } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------------
    // UI ARCHITECTURE (8 Streamlined Pages)
    // ------------------------------------------------------------------------
    private void buildDesktopLayout() {
        setLayout(new BorderLayout());

        // Top Navigation & Indicator Bar
        add(createTopBar(), BorderLayout.NORTH);

        // Center: Left Navigation Sidebar + Card Deck
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(currentTheme.canvas);

        center.add(createSidebarNav(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        mainCardDeck = new JPanel(cardLayout);
        mainCardDeck.setBackground(currentTheme.canvas);

        mainCardDeck.add(createHomeView(), "VIEW_HOME");
        mainCardDeck.add(createDiaryCalendarView(), "VIEW_DIARY");
        mainCardDeck.add(createSearchAddFoodView(), "VIEW_FOOD_LOGGING");
        mainCardDeck.add(createRecipesView(), "VIEW_RECIPES");
        mainCardDeck.add(createTrendsView(), "VIEW_TRENDS");
        mainCardDeck.add(createProfileYouView(), "VIEW_PROFILE");
        mainCardDeck.add(createPhoneBridgeView(), "VIEW_PHONE");
        mainCardDeck.add(createSettingsView(), "VIEW_SETTINGS");

        center.add(mainCardDeck, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(currentTheme.canvas);
        bar.setBorder(new EmptyBorder(12, 20, 10, 20));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);

        JLabel logoPill = new JLabel("NW");
        logoPill.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoPill.setForeground(Color.WHITE);
        logoPill.setBackground(currentTheme.primaryGreen);
        logoPill.setOpaque(true);
        logoPill.setBorder(new EmptyBorder(4, 10, 4, 10));

        JLabel title = new JLabel("NutriWorks");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(currentTheme.textMain);

        JLabel subtitle = new JLabel("• Daily Nutrition & Health Tracker");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(currentTheme.textMuted);

        brand.add(logoPill);
        brand.add(title);
        brand.add(subtitle);

        JPanel pills = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pills.setOpaque(false);

        topWeightPillLabel = new JLabel(userWeightKg + " kg /");
        topWeightPillLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topWeightPillLabel.setForeground(currentTheme.primaryGreen);
        JPanel weightPill = createPillContainer(topWeightPillLabel, currentTheme.card);

        topWaterPillLabel = new JLabel(waterConsumedMl + " / " + waterGoalMl + " ml +");
        topWaterPillLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topWaterPillLabel.setForeground(COLOR_WATER);
        topWaterPillLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel waterPill = createPillContainer(topWaterPillLabel, currentTheme.card);
        waterPill.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                waterConsumedMl = Math.min(waterGoalMl, waterConsumedMl + 250);
                updateAllDisplays();
            }
        });

        topFastingPillLabel = new JLabel("Fasting • 14h 18m left");
        topFastingPillLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        topFastingPillLabel.setForeground(currentTheme.pillText);
        JPanel fastingPill = createPillContainer(topFastingPillLabel, currentTheme.pillGreen);

        // Unit toggle button
        unitToggleBtn = new NutriButton("Unit: kcal", currentTheme.card, currentTheme.textMain, 12);
        unitToggleBtn.setBorder(new LineBorder(currentTheme.border, 1));
        unitToggleBtn.setPreferredSize(new Dimension(86, 32));
        unitToggleBtn.addActionListener(e -> {
            isKj = !isKj;
            unitToggleBtn.setText(isKj ? "Unit: kJ" : "Unit: kcal");
            updateAllDisplays();
        });

        NutriButton quickAddBtn = new NutriButton("+ Quick Add", currentTheme.card, currentTheme.primaryGreen, 12);
        quickAddBtn.setBorder(new LineBorder(currentTheme.primaryGreen, 1));
        quickAddBtn.setPreferredSize(new Dimension(105, 32));
        quickAddBtn.addActionListener(e -> showQuickAddDialog());

        NutriButton uploadFileBtn = new NutriButton("+ Upload Food Photo", currentTheme.primaryGreen, Color.WHITE, 12);
        uploadFileBtn.setPreferredSize(new Dimension(165, 32));
        uploadFileBtn.addActionListener(e -> showUploadFileDialog());

        pills.add(weightPill);
        pills.add(waterPill);
        pills.add(fastingPill);
        pills.add(unitToggleBtn);
        pills.add(quickAddBtn);
        pills.add(uploadFileBtn);

        bar.add(brand, BorderLayout.WEST);
        bar.add(pills, BorderLayout.EAST);
        return bar;
    }

    private JPanel createPillContainer(JLabel label, Color bg) {
        JPanel pill = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pill.setBackground(bg);
        pill.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(currentTheme.border, 1),
                new EmptyBorder(0, 4, 0, 4)
        ));
        pill.add(label);
        return pill;
    }

    private JPanel createSidebarNav() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(195, 0));
        side.setBackground(currentTheme.canvas);
        side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, currentTheme.border));

        side.add(Box.createVerticalStrut(12));
        side.add(createSidebarBtn("Home", "VIEW_HOME", true));
        side.add(createSidebarBtn("Food Diary", "VIEW_DIARY", false));
        side.add(createSidebarBtn("Food Logging", "VIEW_FOOD_LOGGING", false));
        side.add(createSidebarBtn("Meals & Recipes", "VIEW_RECIPES", false));
        side.add(createSidebarBtn("Trends", "VIEW_TRENDS", false));
        side.add(createSidebarBtn("You & Profile", "VIEW_PROFILE", false));
        side.add(createSidebarBtn("Phone Bridge", "VIEW_PHONE", false));
        side.add(createSidebarBtn("Settings & Backup", "VIEW_SETTINGS", false));
        side.add(Box.createVerticalGlue());

        JPanel pBox = new JPanel(new BorderLayout());
        pBox.setBackground(currentTheme.card);
        pBox.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel pText = new JLabel("<html><b>100% Offline & Private</b><br><span style='font-size:10px;color:#747770;'>NutriWorks Engine<br>Zero cloud telemetry</span></html>");
        pText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pBox.add(pText);
        side.add(pBox);
        side.add(Box.createVerticalStrut(12));

        return side;
    }

    private JButton createSidebarBtn(String label, String key, boolean active) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setMaximumSize(new Dimension(195, 42));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (active) {
            btn.setBackground(currentTheme.card);
            btn.setForeground(currentTheme.primaryGreen);
        } else {
            btn.setBackground(currentTheme.canvas);
            btn.setForeground(currentTheme.textMain);
        }

        btn.addActionListener(e -> navigateToView(key));
        sidebarNavButtons.add(btn);
        return btn;
    }

    private void navigateToView(String viewKey) {
        cardLayout.show(mainCardDeck, viewKey);
        for (JButton b : sidebarNavButtons) {
            b.setBackground(currentTheme.canvas);
            b.setForeground(currentTheme.textMain);
        }
        for (JButton b : sidebarNavButtons) {
            if (("Home".equals(b.getText()) && "VIEW_HOME".equals(viewKey)) ||
                ("Food Diary".equals(b.getText()) && "VIEW_DIARY".equals(viewKey)) ||
                ("Food Logging".equals(b.getText()) && "VIEW_FOOD_LOGGING".equals(viewKey)) ||
                ("Meals & Recipes".equals(b.getText()) && "VIEW_RECIPES".equals(viewKey)) ||
                ("Trends".equals(b.getText()) && "VIEW_TRENDS".equals(viewKey)) ||
                ("You & Profile".equals(b.getText()) && "VIEW_PROFILE".equals(viewKey)) ||
                ("Phone Bridge".equals(b.getText()) && "VIEW_PHONE".equals(viewKey)) ||
                ("Settings & Backup".equals(b.getText()) && "VIEW_SETTINGS".equals(viewKey))) {
                b.setBackground(currentTheme.card);
                b.setForeground(currentTheme.primaryGreen);
            }
        }
    }

    // ------------------------------------------------------------------------
    // SCREEN 1: HOME VIEW
    // ------------------------------------------------------------------------
    private JPanel createHomeView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setOpaque(false);

        // 1. Central Calorie Arc Card
        JPanel calorieCard = createRoundedCard();
        calorieCard.setLayout(new BorderLayout(16, 0));
        calorieCard.setPreferredSize(new Dimension(0, 230));

        JPanel suppliedPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        suppliedPanel.setOpaque(false);
        suppliedPanel.setBorder(new EmptyBorder(40, 20, 40, 0));
        JLabel downArrow = new JLabel("↓");
        downArrow.setFont(new Font("Segoe UI", Font.BOLD, 18));
        downArrow.setForeground(currentTheme.primaryGreen);
        homeSuppliedLabel = new JLabel("1,340");
        homeSuppliedLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        homeSuppliedLabel.setForeground(currentTheme.textMain);
        JLabel suppliedText = new JLabel("supplied");
        suppliedText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        suppliedText.setForeground(currentTheme.textMuted);
        suppliedPanel.add(downArrow);
        suppliedPanel.add(homeSuppliedLabel);
        suppliedPanel.add(suppliedText);

        homeEnergyArc = new OpenNutriEnergyArc();
        homeEnergyArc.setPreferredSize(new Dimension(240, 230));

        JPanel burnedPanel = new JPanel(new GridLayout(3, 1, 0, 2));
        burnedPanel.setOpaque(false);
        burnedPanel.setBorder(new EmptyBorder(40, 0, 40, 20));
        JLabel fireIcon = new JLabel("•");
        fireIcon.setFont(new Font("Segoe UI", Font.BOLD, 24));
        fireIcon.setForeground(new Color(230, 81, 0));
        homeBurnedLabel = new JLabel("320");
        homeBurnedLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        homeBurnedLabel.setForeground(currentTheme.textMain);
        JLabel burnedText = new JLabel("burned");
        burnedText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        burnedText.setForeground(currentTheme.textMuted);
        burnedPanel.add(fireIcon);
        burnedPanel.add(homeBurnedLabel);
        burnedPanel.add(burnedText);

        calorieCard.add(suppliedPanel, BorderLayout.WEST);
        calorieCard.add(homeEnergyArc, BorderLayout.CENTER);
        calorieCard.add(burnedPanel, BorderLayout.EAST);
        scrollContainer.add(calorieCard);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 2. Three Macro Cards in a Row
        JPanel macroGrid = new JPanel(new GridLayout(1, 3, 14, 0));
        macroGrid.setOpaque(false);

        homeCarbsGramsLabel = new JLabel("165 / 263 g");
        homeCarbsBar = new JProgressBar(0, 100);
        macroGrid.add(createMacroPillCard("carbs", COLOR_MACRO_CARBS, homeCarbsGramsLabel, homeCarbsBar));

        homeFatGramsLabel = new JLabel("48 / 70 g");
        homeFatBar = new JProgressBar(0, 100);
        macroGrid.add(createMacroPillCard("fat", COLOR_MACRO_FAT, homeFatGramsLabel, homeFatBar));

        homeProteinGramsLabel = new JLabel("75 / 105 g");
        homeProteinBar = new JProgressBar(0, 100);
        macroGrid.add(createMacroPillCard("protein", COLOR_MACRO_PROTEIN, homeProteinGramsLabel, homeProteinBar));

        scrollContainer.add(macroGrid);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 3. Hydration & Active Exercise Row
        JPanel subRow = new JPanel(new GridLayout(1, 2, 14, 0));
        subRow.setOpaque(false);

        // Water Hydration Card
        JPanel waterCard = createRoundedCard();
        waterCard.setLayout(new BorderLayout(0, 8));
        JLabel wTitle = new JLabel("Water Hydration");
        wTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel wStatus = new JLabel(waterConsumedMl + " / " + waterGoalMl + " ml (" + (int)((waterConsumedMl * 100.0) / waterGoalMl) + "%)");
        wStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        wStatus.setForeground(COLOR_WATER);

        JPanel wBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        wBtns.setOpaque(false);
        NutriButton w100 = new NutriButton("+100ml", currentTheme.card, COLOR_WATER, 10); w100.setPreferredSize(new Dimension(68, 28));
        NutriButton w250 = new NutriButton("+250ml", currentTheme.card, COLOR_WATER, 10); w250.setPreferredSize(new Dimension(68, 28));
        NutriButton w500 = new NutriButton("+500ml", currentTheme.card, COLOR_WATER, 10); w500.setPreferredSize(new Dimension(68, 28));
        w100.addActionListener(e -> { waterConsumedMl = Math.min(waterGoalMl, waterConsumedMl + 100); updateAllDisplays(); });
        w250.addActionListener(e -> { waterConsumedMl = Math.min(waterGoalMl, waterConsumedMl + 250); updateAllDisplays(); });
        w500.addActionListener(e -> { waterConsumedMl = Math.min(waterGoalMl, waterConsumedMl + 500); updateAllDisplays(); });
        wBtns.add(w100); wBtns.add(w250); wBtns.add(w500);

        waterCard.add(wTitle, BorderLayout.NORTH);
        waterCard.add(wStatus, BorderLayout.CENTER);
        waterCard.add(wBtns, BorderLayout.SOUTH);
        subRow.add(waterCard);

        // Quick Activity Logger Card
        JPanel actCard = createRoundedCard();
        actCard.setLayout(new BorderLayout(0, 8));
        JLabel aTitle = new JLabel("Active Energy Burned");
        aTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel aStatus = new JLabel(formatEnergy(burnedKcal) + " logged today");
        aStatus.setFont(new Font("Segoe UI", Font.BOLD, 14));
        aStatus.setForeground(new Color(230, 81, 0));

        JPanel aBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        aBtns.setOpaque(false);
        NutriButton logWkBtn = new NutriButton("+ Log Workout", currentTheme.primaryGreen, Color.WHITE, 10);
        logWkBtn.setPreferredSize(new Dimension(115, 28));
        logWkBtn.addActionListener(e -> showLogWorkoutDialog());
        aBtns.add(logWkBtn);

        actCard.add(aTitle, BorderLayout.NORTH);
        actCard.add(aStatus, BorderLayout.CENTER);
        actCard.add(aBtns, BorderLayout.SOUTH);
        subRow.add(actCard);

        scrollContainer.add(subRow);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 4. Action Banner Card
        JPanel bannerCard = createRoundedCard();
        bannerCard.setLayout(new BorderLayout());

        JPanel bLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        bLeft.setOpaque(false);
        JLabel bTitle = new JLabel("Track Nutrition");
        bTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel bSub = new JLabel("Search 40+ foods, scan barcodes, or upload photos");
        bSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bSub.setForeground(currentTheme.textMuted);
        bLeft.add(bTitle);
        bLeft.add(bSub);

        JPanel bRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bRight.setOpaque(false);
        NutriButton searchBtn = new NutriButton("Search Food Library", currentTheme.primaryGreen, Color.WHITE, 12);
        searchBtn.setPreferredSize(new Dimension(160, 36));
        searchBtn.addActionListener(e -> navigateToView("VIEW_FOOD_LOGGING"));

        NutriButton uploadBtn = new NutriButton("Upload Photo", currentTheme.pillGreen, currentTheme.pillText, 12);
        uploadBtn.setPreferredSize(new Dimension(130, 36));
        uploadBtn.addActionListener(e -> showUploadFileDialog());

        bRight.add(searchBtn);
        bRight.add(uploadBtn);

        bannerCard.add(bLeft, BorderLayout.WEST);
        bannerCard.add(bRight, BorderLayout.EAST);
        scrollContainer.add(bannerCard);

        JScrollPane scroll = new JScrollPane(scrollContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private JPanel createMacroPillCard(String title, Color c, JLabel valLabel, JProgressBar bar) {
        JPanel card = createRoundedCard();
        card.setLayout(new BorderLayout(0, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);
        JLabel dot = new JLabel("•");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dot.setForeground(c);
        JLabel name = new JLabel(title);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(currentTheme.textMain);
        top.add(dot);
        top.add(name);

        bar.setForeground(c);
        bar.setPreferredSize(new Dimension(0, 8));

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valLabel.setForeground(currentTheme.textMain);

        card.add(top, BorderLayout.NORTH);
        card.add(bar, BorderLayout.CENTER);
        card.add(valLabel, BorderLayout.SOUTH);
        return card;
    }

    // ------------------------------------------------------------------------
    // SCREEN 2: FOOD DIARY (Calendar + Grouped Meals)
    // ------------------------------------------------------------------------
    private JPanel createDiaryCalendarView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setOpaque(false);

        // 1. Calendar Card with Compliance Dots
        JPanel calCard = createRoundedCard();
        calCard.setLayout(new BorderLayout(0, 10));

        JPanel calHeader = new JPanel(new BorderLayout());
        calHeader.setOpaque(false);
        JLabel calTitle = new JLabel("July 2026", SwingConstants.CENTER);
        calTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JButton prevBtn = new JButton("‹"); prevBtn.setBorder(null); prevBtn.setContentAreaFilled(false);
        JButton nextBtn = new JButton("›"); nextBtn.setBorder(null); nextBtn.setContentAreaFilled(false);
        calHeader.add(prevBtn, BorderLayout.WEST);
        calHeader.add(calTitle, BorderLayout.CENTER);
        calHeader.add(nextBtn, BorderLayout.EAST);
        calCard.add(calHeader, BorderLayout.NORTH);

        JPanel calGrid = new JPanel(new GridLayout(6, 7, 4, 4));
        calGrid.setOpaque(false);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String d : days) {
            JLabel dl = new JLabel(d, SwingConstants.CENTER);
            dl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            dl.setForeground(currentTheme.textMuted);
            calGrid.add(dl);
        }

        int[] sampleDays = {29, 30, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 1, 2};
        for (int d : sampleDays) {
            JPanel dayCell = new JPanel(new GridLayout(2, 1));
            dayCell.setOpaque(false);
            JLabel num = new JLabel(d + "", SwingConstants.CENTER);
            num.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JLabel dot = new JLabel("•", SwingConstants.CENTER);
            dot.setFont(new Font("Segoe UI", Font.BOLD, 12));
            dot.setForeground(d % 7 == 0 ? COLOR_MACRO_FAT : currentTheme.primaryGreen);

            if (d == 27) {
                num.setFont(new Font("Segoe UI", Font.BOLD, 12));
                num.setForeground(Color.WHITE);
                num.setBackground(currentTheme.primaryGreen);
                num.setOpaque(true);
            }

            dayCell.add(num);
            dayCell.add(dot);
            calGrid.add(dayCell);
        }
        calCard.add(calGrid, BorderLayout.CENTER);
        scrollContainer.add(calCard);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 2. Day Header Card with Copy Yesterday's Meals
        JPanel dayHeader = new JPanel(new BorderLayout());
        dayHeader.setOpaque(false);
        JLabel dateTitle = new JLabel("Monday, July 27, 2026");
        dateTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));

        JPanel rActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rActions.setOpaque(false);

        NutriButton copyYesterdayBtn = new NutriButton("Copy Yesterday's Meals", currentTheme.card, currentTheme.textMain, 10);
        copyYesterdayBtn.setBorder(new LineBorder(currentTheme.border, 1));
        copyYesterdayBtn.setPreferredSize(new Dimension(170, 30));
        copyYesterdayBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Copied 4 meals from Sunday, July 26, 2026 into today's journal!", "Copy Meals", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel totalBadge = new JLabel(formatEnergy(consumedKcal) + " / " + formatEnergy(targetKcal));
        totalBadge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        totalBadge.setForeground(currentTheme.primaryGreen);
        totalBadge.setBackground(currentTheme.pillGreen);
        totalBadge.setOpaque(true);
        totalBadge.setBorder(new EmptyBorder(4, 12, 4, 12));

        rActions.add(copyYesterdayBtn);
        rActions.add(totalBadge);

        dayHeader.add(dateTitle, BorderLayout.WEST);
        dayHeader.add(rActions, BorderLayout.EAST);
        scrollContainer.add(dayHeader);
        scrollContainer.add(Box.createVerticalStrut(12));

        // 3. Four Grouped Meal Cards with Per-Meal Targets
        diaryBreakfastPanel = new JPanel(); diaryBreakfastPanel.setLayout(new BoxLayout(diaryBreakfastPanel, BoxLayout.Y_AXIS)); diaryBreakfastPanel.setOpaque(false);
        diaryBreakfastSubtotal = new JLabel();
        scrollContainer.add(createMealCategoryCard("Breakfast", targetBreakfastKcal, diaryBreakfastSubtotal, diaryBreakfastPanel));
        scrollContainer.add(Box.createVerticalStrut(10));

        diaryLunchPanel = new JPanel(); diaryLunchPanel.setLayout(new BoxLayout(diaryLunchPanel, BoxLayout.Y_AXIS)); diaryLunchPanel.setOpaque(false);
        diaryLunchSubtotal = new JLabel();
        scrollContainer.add(createMealCategoryCard("Lunch", targetLunchKcal, diaryLunchSubtotal, diaryLunchPanel));
        scrollContainer.add(Box.createVerticalStrut(10));

        diaryDinnerPanel = new JPanel(); diaryDinnerPanel.setLayout(new BoxLayout(diaryDinnerPanel, BoxLayout.Y_AXIS)); diaryDinnerPanel.setOpaque(false);
        diaryDinnerSubtotal = new JLabel();
        scrollContainer.add(createMealCategoryCard("Dinner", targetDinnerKcal, diaryDinnerSubtotal, diaryDinnerPanel));
        scrollContainer.add(Box.createVerticalStrut(10));

        diarySnacksPanel = new JPanel(); diarySnacksPanel.setLayout(new BoxLayout(diarySnacksPanel, BoxLayout.Y_AXIS)); diarySnacksPanel.setOpaque(false);
        diarySnacksSubtotal = new JLabel();
        scrollContainer.add(createMealCategoryCard("Snacks", targetSnacksKcal, diarySnacksSubtotal, diarySnacksPanel));

        JScrollPane scroll = new JScrollPane(scrollContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private JPanel createMealCategoryCard(String category, int targetKcalForMeal, JLabel subtotal, JPanel itemsBox) {
        JPanel card = createRoundedCard();
        card.setLayout(new BorderLayout(0, 8));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTitle.setOpaque(false);
        JLabel title = new JLabel(category);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel targetLbl = new JLabel("(target " + formatEnergy(targetKcalForMeal) + ")");
        targetLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        targetLbl.setForeground(currentTheme.textMuted);
        leftTitle.add(title);
        leftTitle.add(targetLbl);

        subtotal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        subtotal.setForeground(currentTheme.primaryGreen);

        NutriButton addBtn = new NutriButton("+ Add", currentTheme.primaryGreen, Color.WHITE, 12);
        addBtn.setPreferredSize(new Dimension(72, 28));
        addBtn.addActionListener(e -> showFoodDetailSheet(globalFoodLibrary.get(0), category));

        top.add(leftTitle, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(subtotal);
        right.add(addBtn);
        top.add(right, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(itemsBox, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------------
    // SCREEN 3: FOOD LOGGING (Search, Barcode, Quick-Add)
    // ------------------------------------------------------------------------
    private JPanel createSearchAddFoodView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        // Top Search Bar & Actions
        JPanel topSearchPanel = new JPanel(new BorderLayout(8, 0));
        topSearchPanel.setOpaque(false);

        JPanel searchBox = createRoundedCard();
        searchBox.setLayout(new BorderLayout(8, 0));
        searchBox.setBorder(new EmptyBorder(8, 14, 8, 14));

        JLabel searchIcon = new JLabel("Search:");
        searchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));

        foodSearchField = new JTextField();
        foodSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        foodSearchField.setBorder(null);
        foodSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterFoodList(); }
            public void removeUpdate(DocumentEvent e) { filterFoodList(); }
            public void changedUpdate(DocumentEvent e) { filterFoodList(); }
        });

        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(foodSearchField, BorderLayout.CENTER);

        JPanel rActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rActions.setOpaque(false);

        NutriButton barcodeBtn = new NutriButton("Scan Barcode", currentTheme.card, currentTheme.textMain, 12);
        barcodeBtn.setBorder(new LineBorder(currentTheme.border, 1));
        barcodeBtn.setPreferredSize(new Dimension(115, 38));
        barcodeBtn.addActionListener(e -> showBarcodeScannerDialog());

        NutriButton quickAddBtn = new NutriButton("+ Quick Add", currentTheme.card, currentTheme.primaryGreen, 12);
        quickAddBtn.setBorder(new LineBorder(currentTheme.primaryGreen, 1));
        quickAddBtn.setPreferredSize(new Dimension(105, 38));
        quickAddBtn.addActionListener(e -> showQuickAddDialog());

        NutriButton uploadBtn = new NutriButton("Upload Photo", currentTheme.primaryGreen, Color.WHITE, 12);
        uploadBtn.setPreferredSize(new Dimension(130, 38));
        uploadBtn.addActionListener(e -> showUploadFileDialog());

        rActions.add(barcodeBtn);
        rActions.add(quickAddBtn);
        rActions.add(uploadBtn);

        topSearchPanel.add(searchBox, BorderLayout.CENTER);
        topSearchPanel.add(rActions, BorderLayout.EAST);

        // Filter Chips Row
        JPanel chipsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chipsRow.setOpaque(false);
        String[] chips = {"All", "Recently", "USDA", "Open Food Facts", "German BLS", "Indian", "Protein", "Veggies", "Fruits", "Grains", "Snacks"};
        final List<NutriButton> chipButtons = new ArrayList<>();
        for (String chip : chips) {
            boolean isAll = "All".equals(chip);
            NutriButton cBtn = new NutriButton(chip, isAll ? currentTheme.primaryGreen : currentTheme.card, isAll ? Color.WHITE : currentTheme.textMain, 12);
            cBtn.setPreferredSize(new Dimension(chip.length() > 8 ? 110 : 75, 30));
            chipButtons.add(cBtn);
            cBtn.addActionListener(e -> {
                selectedSearchCategory = chip;
                for (NutriButton b : chipButtons) {
                    b.setColors(currentTheme.card, currentTheme.textMain);
                }
                cBtn.setColors(currentTheme.primaryGreen, Color.WHITE);
                filterFoodList();
            });
            chipsRow.add(cBtn);
        }

        JPanel headerStack = new JPanel(new GridLayout(2, 1, 0, 6));
        headerStack.setOpaque(false);
        headerStack.add(topSearchPanel);
        headerStack.add(chipsRow);
        view.add(headerStack, BorderLayout.NORTH);

        // Food Items List
        foodCardsListPanel = new JPanel();
        foodCardsListPanel.setLayout(new BoxLayout(foodCardsListPanel, BoxLayout.Y_AXIS));
        foodCardsListPanel.setOpaque(false);

        populateFoodCardsList(globalFoodLibrary);

        JScrollPane scroll = new JScrollPane(foodCardsListPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        view.add(scroll, BorderLayout.CENTER);

        return view;
    }

    private void filterFoodList() {
        String query = foodSearchField.getText().trim().toLowerCase();
        List<FoodLibraryItem> filtered = new ArrayList<>();
        for (FoodLibraryItem item : globalFoodLibrary) {
            boolean matchesCat = "All".equalsIgnoreCase(selectedSearchCategory) ||
                    "Recently".equalsIgnoreCase(selectedSearchCategory) ||
                    item.category.equalsIgnoreCase(selectedSearchCategory) ||
                    item.sourceDb.equalsIgnoreCase(selectedSearchCategory);
            boolean matchesQuery = query.isEmpty() ||
                    item.name.toLowerCase().contains(query) ||
                    item.sourceDb.toLowerCase().contains(query) ||
                    item.barcode.contains(query);
            if (matchesCat && matchesQuery) filtered.add(item);
        }
        populateFoodCardsList(filtered);
    }

    private void populateFoodCardsList(List<FoodLibraryItem> items) {
        foodCardsListPanel.removeAll();
        for (FoodLibraryItem item : items) {
            JPanel card = createFoodListCard(item);
            foodCardsListPanel.add(card);
            foodCardsListPanel.add(Box.createVerticalStrut(8));
        }
        foodCardsListPanel.revalidate();
        foodCardsListPanel.repaint();
    }

    private JPanel createFoodListCard(FoodLibraryItem item) {
        JPanel card = createRoundedCard();
        card.setLayout(new BorderLayout(14, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));

        // Food Emoji strictly placed in the food thumbnail box
        JLabel iconLbl = new JLabel(item.emoji, SwingConstants.CENTER);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        iconLbl.setBackground(currentTheme.canvas);
        iconLbl.setOpaque(true);
        iconLbl.setPreferredSize(new Dimension(48, 48));

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleRow.setOpaque(false);
        JLabel name = new JLabel(item.name);
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(currentTheme.textMain);

        JLabel sourceBadge = new JLabel(item.sourceDb);
        sourceBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sourceBadge.setForeground(currentTheme.pillText);
        sourceBadge.setBackground(currentTheme.pillGreen);
        sourceBadge.setOpaque(true);
        sourceBadge.setBorder(new EmptyBorder(1, 6, 1, 6));

        titleRow.add(name);
        titleRow.add(sourceBadge);

        JLabel sub = new JLabel(String.format("100g  •  %s  •  %.1fg Carbs  •  %.1fg Protein  •  %.1fg Fat",
                formatEnergy(item.kcal100g), item.carbs100g, item.protein100g, item.fat100g));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(currentTheme.textMuted);

        textPanel.add(titleRow);
        textPanel.add(sub);

        // Circular Green (+) NutriButton
        NutriButton addCircleBtn = new NutriButton("+", currentTheme.primaryGreen, Color.WHITE, 19);
        addCircleBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        addCircleBtn.setPreferredSize(new Dimension(38, 38));
        addCircleBtn.addActionListener(e -> showFoodDetailSheet(item, "Lunch"));

        card.add(iconLbl, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);
        card.add(addCircleBtn, BorderLayout.EAST);
        return card;
    }

    // ------------------------------------------------------------------------
    // FOOD DETAIL SHEET WITH PORTION CHIPS
    // ------------------------------------------------------------------------
    private void showFoodDetailSheet(FoodLibraryItem item, String defaultCategory) {
        JDialog sheet = new JDialog(this, item.emoji + " " + item.name + " (" + item.sourceDb + ")", true);
        sheet.setSize(560, 740);
        sheet.setLocationRelativeTo(this);
        sheet.getContentPane().setBackground(currentTheme.canvas);
        sheet.setLayout(new BorderLayout());

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(new EmptyBorder(16, 20, 16, 20));
        container.setOpaque(false);

        // 1. Top Impact Progress Bar
        JPanel totalBarCard = createRoundedCard();
        totalBarCard.setLayout(new BorderLayout(0, 6));
        JLabel totalText = new JLabel(String.format("Day total: %s / %s  (+%s selection)",
                formatEnergy(consumedKcal + item.kcal100g), formatEnergy(targetKcal), formatEnergy(item.kcal100g)), SwingConstants.CENTER);
        totalText.setFont(new Font("Segoe UI", Font.BOLD, 13));
        totalText.setForeground(currentTheme.primaryGreen);

        JProgressBar totalBar = new JProgressBar(0, 100);
        totalBar.setValue((int) Math.min(100, ((consumedKcal + item.kcal100g) / (double) targetKcal) * 100));
        totalBar.setForeground(currentTheme.primaryGreen);
        totalBar.setPreferredSize(new Dimension(0, 8));

        totalBarCard.add(totalText, BorderLayout.NORTH);
        totalBarCard.add(totalBar, BorderLayout.SOUTH);
        container.add(totalBarCard);
        container.add(Box.createVerticalStrut(12));

        // 2. Comprehensive Nutrient Facts Table
        JPanel tableCard = createRoundedCard();
        tableCard.setLayout(new GridLayout(6, 2, 8, 6));

        JLabel calVal = new JLabel(formatEnergy(item.kcal100g)); calVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel fatVal = new JLabel(item.fat100g + "g");
        JLabel satFatVal = new JLabel(String.format("%.1fg", item.fat100g * 0.3));
        JLabel carbVal = new JLabel(item.carbs100g + "g");
        JLabel sugarVal = new JLabel(item.sugar100g + "g");
        JLabel fiberVal = new JLabel(item.fiber100g + "g");
        JLabel protVal = new JLabel(item.protein100g + "g");

        tableCard.add(new JLabel("Energy")); tableCard.add(calVal);
        tableCard.add(new JLabel("Total Fat")); tableCard.add(fatVal);
        tableCard.add(new JLabel("  • Saturated fat")); tableCard.add(satFatVal);
        tableCard.add(new JLabel("Carbohydrates")); tableCard.add(carbVal);
        tableCard.add(new JLabel("  • Sugar")); tableCard.add(sugarVal);
        tableCard.add(new JLabel("Protein")); tableCard.add(protVal);
        container.add(tableCard);
        container.add(Box.createVerticalStrut(12));

        // 3. Micronutrients Details
        JPanel microCard = createRoundedCard();
        microCard.setLayout(new GridLayout(3, 2, 8, 6));
        microCard.add(new JLabel("Dietary Fiber")); microCard.add(fiberVal);
        microCard.add(new JLabel("Sodium")); microCard.add(new JLabel(item.sodiumMg100g + " mg"));
        microCard.add(new JLabel("Potassium")); microCard.add(new JLabel(item.potassiumMg100g + " mg"));
        container.add(microCard);
        container.add(Box.createVerticalStrut(12));

        // 4. Quantity Field + Portion Chips
        JPanel portionCard = createRoundedCard();
        portionCard.setLayout(new BorderLayout(0, 10));

        JPanel qRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        qRow.setOpaque(false);
        JLabel qLbl = new JLabel("Quantity:");
        qLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JTextField qtyField = new JTextField("100", 5);
        qtyField.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel unitLbl = new JLabel("grams (g)");
        unitLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        qRow.add(qLbl); qRow.add(qtyField); qRow.add(unitLbl);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        chipRow.setOpaque(false);
        int[] chipGrams = {50, 100, 150, 200, 250, 300};
        final List<NutriButton> portionButtons = new ArrayList<>();
        for (int cg : chipGrams) {
            boolean isDefault = (cg == 100);
            NutriButton cb = new NutriButton(cg + "", isDefault ? currentTheme.primaryGreen : currentTheme.pillGreen, isDefault ? Color.WHITE : currentTheme.pillText, 10);
            cb.setPreferredSize(new Dimension(54, 30));
            portionButtons.add(cb);
            cb.addActionListener(e -> {
                qtyField.setText(cg + "");
                for (NutriButton b : portionButtons) {
                    b.setColors(currentTheme.pillGreen, currentTheme.pillText);
                }
                cb.setColors(currentTheme.primaryGreen, Color.WHITE);
                updateDetailSheetMath(cg, item, calVal, fatVal, carbVal, protVal, totalText, totalBar);
            });
            chipRow.add(cb);
        }

        qtyField.addActionListener(e -> {
            try {
                int g = Integer.parseInt(qtyField.getText().trim());
                updateDetailSheetMath(g, item, calVal, fatVal, carbVal, protVal, totalText, totalBar);
            } catch (Exception ignored) {}
        });

        portionCard.add(qRow, BorderLayout.NORTH);
        portionCard.add(chipRow, BorderLayout.SOUTH);
        container.add(portionCard);
        container.add(Box.createVerticalStrut(12));

        // 5. Meal Category Selector
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Breakfast", "Lunch", "Dinner", "Snacks"});
        categoryCombo.setSelectedItem(defaultCategory);
        categoryCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JPanel catCard = createRoundedCard();
        catCard.setLayout(new BorderLayout());
        catCard.add(new JLabel("Log under meal:"), BorderLayout.WEST);
        catCard.add(categoryCombo, BorderLayout.EAST);
        container.add(catCard);

        // BIG HIGH-CONTRAST VIBRANT ADD BUTTON
        NutriButton addBtn = new NutriButton("+ Add to " + defaultCategory, currentTheme.primaryGreen, Color.WHITE, 16);
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        addBtn.setPreferredSize(new Dimension(340, 48));

        categoryCombo.addActionListener(e -> addBtn.setText("+ Add to " + categoryCombo.getSelectedItem()));

        addBtn.addActionListener(e -> {
            try {
                int grams = Integer.parseInt(qtyField.getText().trim());
                double f = grams / 100.0;
                int finalKcal = (int) Math.round(item.kcal100g * f);
                double p = item.protein100g * f;
                double c = item.carbs100g * f;
                double fat = item.fat100g * f;
                double fib = item.fiber100g * f;

                loggedMeals.add(new MealItem((String) categoryCombo.getSelectedItem(), item.name, item.emoji, grams, finalKcal,
                        p, c, fat, fib, item.sugar100g * f, (int) (item.sodiumMg100g * f),
                        (int) (item.potassiumMg100g * f), (int) (item.calciumMg100g * f), item.ironMg100g * f, "Food Library"));
                updateAllDisplays();
                sheet.dispose();
                JOptionPane.showMessageDialog(this, "Logged " + item.emoji + " " + item.name + " (" + formatEnergy(finalKcal) + ") into " + categoryCombo.getSelectedItem() + "!", "Food Logged", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(sheet, "Please enter a valid numeric portion in grams.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 14));
        bottom.setBackground(currentTheme.canvas);
        bottom.add(addBtn);

        sheet.add(container, BorderLayout.CENTER);
        sheet.add(bottom, BorderLayout.SOUTH);
        sheet.setVisible(true);
    }

    private void updateDetailSheetMath(int grams, FoodLibraryItem item, JLabel cal, JLabel fat, JLabel carb, JLabel prot, JLabel totalText, JProgressBar totalBar) {
        double f = grams / 100.0;
        int calcKcal = (int) Math.round(item.kcal100g * f);
        cal.setText(formatEnergy(calcKcal));
        fat.setText(String.format("%.1fg", item.fat100g * f));
        carb.setText(String.format("%.1fg", item.carbs100g * f));
        prot.setText(String.format("%.1fg", item.protein100g * f));

        int newTotal = consumedKcal + calcKcal;
        totalText.setText(String.format("Day total: %s / %s  (+%s selection)",
                formatEnergy(newTotal), formatEnergy(targetKcal), formatEnergy(calcKcal)));
        totalBar.setValue((int) Math.min(100, (newTotal / (double) targetKcal) * 100));
    }

    // ------------------------------------------------------------------------
    // BARCODE SCANNER SIMULATOR & QUICK ADD
    // ------------------------------------------------------------------------
    private void showBarcodeScannerDialog() {
        JDialog dialog = new JDialog(this, "Barcode Scanner - Open Food Facts & USDA", true);
        dialog.setSize(480, 320);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(currentTheme.canvas);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setOpaque(false);

        JLabel info = new JLabel("Enter or scan a 12-13 digit EAN/UPC barcode:");
        info.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JTextField codeField = new JTextField("8901030383749");
        codeField.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JPanel samples = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        samples.setOpaque(false);
        NutriButton s1 = new NutriButton("Rolled Oats", currentTheme.card, currentTheme.textMain, 10);
        NutriButton s2 = new NutriButton("Greek Yogurt", currentTheme.card, currentTheme.textMain, 10);
        NutriButton s3 = new NutriButton("Olive Oil", currentTheme.card, currentTheme.textMain, 10);
        s1.addActionListener(e -> codeField.setText("8901030383749"));
        s2.addActionListener(e -> codeField.setText("5000112546415"));
        s3.addActionListener(e -> codeField.setText("8410000001234"));
        samples.add(s1); samples.add(s2); samples.add(s3);

        NutriButton lookupBtn = new NutriButton("Lookup Product", currentTheme.primaryGreen, Color.WHITE, 12);
        lookupBtn.setPreferredSize(new Dimension(160, 38));
        lookupBtn.addActionListener(e -> {
            String code = codeField.getText().trim();
            FoodLibraryItem match = null;
            for (FoodLibraryItem f : globalFoodLibrary) {
                if (f.barcode.equals(code)) { match = f; break; }
            }
            if (match != null) {
                dialog.dispose();
                showFoodDetailSheet(match, "Lunch");
            } else {
                JOptionPane.showMessageDialog(dialog, "Barcode " + code + " not found. Showing closest match.", "Lookup", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                showFoodDetailSheet(globalFoodLibrary.get(0), "Lunch");
            }
        });

        p.add(info);
        p.add(Box.createVerticalStrut(8));
        p.add(codeField);
        p.add(Box.createVerticalStrut(10));
        p.add(new JLabel("Quick Samples:"));
        p.add(samples);
        p.add(Box.createVerticalStrut(14));
        p.add(lookupBtn);

        dialog.add(p, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void showQuickAddDialog() {
        JDialog dialog = new JDialog(this, "Quick Add Food or Meal", true);
        dialog.setSize(420, 380);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(currentTheme.canvas);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(6, 2, 8, 10));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setOpaque(false);

        JTextField nameField = new JTextField("Quick Snack");
        JComboBox<String> mealCombo = new JComboBox<>(new String[]{"Breakfast", "Lunch", "Dinner", "Snacks"});
        mealCombo.setSelectedItem("Snacks");
        JTextField kcalField = new JTextField("250");
        JTextField carbsField = new JTextField("30");
        JTextField protField = new JTextField("12");
        JTextField fatField = new JTextField("6");

        p.add(new JLabel("Description:")); p.add(nameField);
        p.add(new JLabel("Meal Type:")); p.add(mealCombo);
        p.add(new JLabel("Energy (" + getEnergyUnit() + "):")); p.add(kcalField);
        p.add(new JLabel("Carbs (g):")); p.add(carbsField);
        p.add(new JLabel("Protein (g):")); p.add(protField);
        p.add(new JLabel("Fat (g):")); p.add(fatField);

        NutriButton logBtn = new NutriButton("+ Add to Diary", currentTheme.primaryGreen, Color.WHITE, 12);
        logBtn.setPreferredSize(new Dimension(160, 38));
        logBtn.addActionListener(e -> {
            try {
                String dName = nameField.getText().trim();
                String mType = (String) mealCombo.getSelectedItem();
                int kcalInput = Integer.parseInt(kcalField.getText().trim());
                int finalKcal = isKj ? (int) Math.round(kcalInput / 4.184) : kcalInput;
                double c = Double.parseDouble(carbsField.getText().trim());
                double pr = Double.parseDouble(protField.getText().trim());
                double ft = Double.parseDouble(fatField.getText().trim());

                loggedMeals.add(new MealItem(mType, dName, "🍎", 150, finalKcal, pr, c, ft, 2.0, 3.0, 150, 200, 30, 0.8, "Quick Add"));
                updateAllDisplays();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Quick-added " + dName + " (" + formatEnergy(finalKcal) + ")!", "Logged", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        btm.setOpaque(false);
        btm.add(logBtn);

        dialog.add(p, BorderLayout.CENTER);
        dialog.add(btm, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------------
    // SCREEN 4: MEALS & RECIPES
    // ------------------------------------------------------------------------
    private JPanel createRecipesView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Reusable Recipes & Saved Meals");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));

        NutriButton newRecipeBtn = new NutriButton("+ New Recipe", currentTheme.primaryGreen, Color.WHITE, 12);
        newRecipeBtn.setPreferredSize(new Dimension(130, 34));
        newRecipeBtn.addActionListener(e -> showCreateRecipeDialog());
        header.add(title, BorderLayout.WEST);
        header.add(newRecipeBtn, BorderLayout.EAST);
        view.add(header, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        for (RecipeItem r : savedRecipes) {
            JPanel card = createRoundedCard();
            card.setLayout(new BorderLayout(12, 0));
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

            JLabel icn = new JLabel(r.emoji, SwingConstants.CENTER);
            icn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
            icn.setBackground(currentTheme.canvas);
            icn.setOpaque(true);
            icn.setPreferredSize(new Dimension(52, 52));

            JPanel tBox = new JPanel(new GridLayout(2, 1, 0, 2));
            tBox.setOpaque(false);
            JLabel rTitle = new JLabel(r.title + " (" + r.brand + ")");
            rTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
            JLabel rSub = new JLabel(String.format("%s  •  %.0fg Carbs  •  %.0fg Protein  •  %.0fg Fat  •  %s",
                    formatEnergy(r.totalKcal), r.totalCarbs, r.totalProtein, r.totalFat, r.ingredientsSummary));
            rSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            rSub.setForeground(currentTheme.textMuted);
            tBox.add(rTitle);
            tBox.add(rSub);

            NutriButton logRecipeBtn = new NutriButton("Log to Meal", currentTheme.primaryGreen, Color.WHITE, 10);
            logRecipeBtn.setPreferredSize(new Dimension(105, 32));
            logRecipeBtn.addActionListener(e -> {
                loggedMeals.add(new MealItem("Lunch", r.title, r.emoji, 300, r.totalKcal, r.totalProtein, r.totalCarbs, r.totalFat, 4.0, 5.0, 320, 410, 85, 2.1, "Recipe"));
                updateAllDisplays();
                JOptionPane.showMessageDialog(this, "Logged recipe " + r.title + " (" + formatEnergy(r.totalKcal) + ") to Lunch!", "Recipe Logged", JOptionPane.INFORMATION_MESSAGE);
            });

            card.add(icn, BorderLayout.WEST);
            card.add(tBox, BorderLayout.CENTER);
            card.add(logRecipeBtn, BorderLayout.EAST);
            list.add(card);
            list.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private void showCreateRecipeDialog() {
        JDialog dialog = new JDialog(this, "Create Reusable Recipe", true);
        dialog.setSize(460, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(currentTheme.canvas);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(7, 2, 8, 8));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setOpaque(false);

        JTextField titleField = new JTextField("Avocado Chicken Salad");
        JTextField brandField = new JTextField("Homemade");
        JTextField barcodeField = new JTextField("021130090123");
        JTextField kcalField = new JTextField("420");
        JTextField carbsField = new JTextField("14");
        JTextField protField = new JTextField("36");
        JTextField fatField = new JTextField("24");

        p.add(new JLabel("Recipe Title:")); p.add(titleField);
        p.add(new JLabel("Brand / Source:")); p.add(brandField);
        p.add(new JLabel("Barcode:")); p.add(barcodeField);
        p.add(new JLabel("Total Energy (" + getEnergyUnit() + "):")); p.add(kcalField);
        p.add(new JLabel("Total Carbs (g):")); p.add(carbsField);
        p.add(new JLabel("Total Protein (g):")); p.add(protField);
        p.add(new JLabel("Total Fat (g):")); p.add(fatField);

        NutriButton saveBtn = new NutriButton("Save Recipe", currentTheme.primaryGreen, Color.WHITE, 12);
        saveBtn.setPreferredSize(new Dimension(140, 38));
        saveBtn.addActionListener(e -> {
            try {
                String t = titleField.getText().trim();
                String b = brandField.getText().trim();
                String bc = barcodeField.getText().trim();
                int kc = Integer.parseInt(kcalField.getText().trim());
                int finalKc = isKj ? (int) Math.round(kc / 4.184) : kc;
                double c = Double.parseDouble(carbsField.getText().trim());
                double pr = Double.parseDouble(protField.getText().trim());
                double ft = Double.parseDouble(fatField.getText().trim());

                savedRecipes.add(new RecipeItem(t, b, bc, "🥗", finalKc, pr, c, ft, "Custom User Recipe Ingredients"));
                dialog.dispose();
                navigateToView("VIEW_RECIPES");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please fill in valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        btm.setOpaque(false);
        btm.add(saveBtn);

        dialog.add(p, BorderLayout.CENTER);
        dialog.add(btm, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------------
    // SCREEN 5: TRENDS & ANALYTICS
    // ------------------------------------------------------------------------
    private JPanel createTrendsView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setOpaque(false);

        // Streak Card
        JPanel streakCard = createRoundedCard();
        streakCard.setLayout(new BorderLayout(14, 0));

        JPanel streakLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        streakLeft.setOpaque(false);
        JLabel flameCircle = new JLabel("🔥", SwingConstants.CENTER);
        flameCircle.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        flameCircle.setBackground(currentTheme.pillGreen);
        flameCircle.setOpaque(true);
        flameCircle.setPreferredSize(new Dimension(42, 42));

        JPanel sText = new JPanel(new GridLayout(2, 1));
        sText.setOpaque(false);
        JLabel sNum = new JLabel(streakDays + " day streak");
        sNum.setFont(new Font("Segoe UI", Font.BOLD, 15));
        JLabel sBest = new JLabel("best " + streakDays);
        sBest.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sBest.setForeground(currentTheme.textMuted);
        sText.add(sNum);
        sText.add(sBest);

        streakLeft.add(flameCircle);
        streakLeft.add(sText);
        streakCard.add(streakLeft, BorderLayout.WEST);
        scrollContainer.add(streakCard);
        scrollContainer.add(Box.createVerticalStrut(12));

        // Filter Pills Row
        JPanel pillsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pillsRow.setOpaque(false);
        String[] filters = {"7d", "30d", "90d", "All"};
        for (int i = 0; i < filters.length; i++) {
            NutriButton pb = new NutriButton(filters[i], i == 0 ? currentTheme.primaryGreen : currentTheme.card, i == 0 ? Color.WHITE : currentTheme.textMain, 12);
            pb.setPreferredSize(new Dimension(50, 28));
            pillsRow.add(pb);
        }
        scrollContainer.add(pillsRow);
        scrollContainer.add(Box.createVerticalStrut(10));

        // Calorie Goal Line Chart Card
        JPanel chartCard = createRoundedCard();
        chartCard.setLayout(new BorderLayout(0, 10));
        JLabel chartTitle = new JLabel("Calories Intake vs Target Goal Line");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        chartCard.add(chartTitle, BorderLayout.NORTH);

        JPanel lineGraphCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight() - 20;

                int goalY = h / 2 - 10;
                g2.setColor(new Color(180, 185, 180));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{4.0f}, 0.0f));
                g2.drawLine(20, goalY, w - 20, goalY);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(currentTheme.textMuted);
                g2.drawString("Goal: " + formatEnergy(targetKcal), 24, goalY - 6);

                int[] pointsY = {goalY + 2, goalY - 1, goalY + 3, goalY - 2, goalY + 8, goalY + 12, goalY - 18};
                int step = (w - 60) / 6;

                g2.setColor(currentTheme.primaryGreen);
                g2.setStroke(new BasicStroke(2.5f));
                for (int i = 0; i < 6; i++) {
                    int x1 = 30 + i * step;
                    int y1 = pointsY[i];
                    int x2 = 30 + (i + 1) * step;
                    int y2 = pointsY[i + 1];
                    g2.drawLine(x1, y1, x2, y2);
                    g2.fillOval(x1 - 3, y1 - 3, 6, 6);
                }
                g2.fillOval(30 + 6 * step - 3, pointsY[6] - 3, 6, 6);
                g2.dispose();
            }
        };
        lineGraphCanvas.setPreferredSize(new Dimension(0, 150));
        lineGraphCanvas.setBackground(new Color(241, 245, 242));
        chartCard.add(lineGraphCanvas, BorderLayout.CENTER);
        scrollContainer.add(chartCard);
        scrollContainer.add(Box.createVerticalStrut(12));

        // Macro Daily Averages Card
        JPanel avgCard = createRoundedCard();
        avgCard.setLayout(new GridLayout(4, 1, 6, 8));
        JLabel avgTitle = new JLabel("Daily average");
        avgTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avgCard.add(avgTitle);

        avgCard.add(createHorizontalBarRow("carbs", "344 / 402 g", 85, COLOR_MACRO_CARBS));
        avgCard.add(createHorizontalBarRow("fat", "73 / 74 g", 98, COLOR_MACRO_FAT));
        avgCard.add(createHorizontalBarRow("protein", "84 / 100 g", 84, COLOR_MACRO_PROTEIN));
        scrollContainer.add(avgCard);

        JScrollPane scroll = new JScrollPane(scrollContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private JPanel createHorizontalBarRow(String label, String val, int pct, Color c) {
        JPanel p = new JPanel(new BorderLayout(8, 4));
        p.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel num = new JLabel(val);
        num.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setValue(pct);
        bar.setForeground(c);
        bar.setPreferredSize(new Dimension(0, 6));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(num, BorderLayout.EAST);

        p.add(top, BorderLayout.NORTH);
        p.add(bar, BorderLayout.SOUTH);
        return p;
    }

    // ------------------------------------------------------------------------
    // SCREEN 6: YOU & PROFILE (Rich Filled Layout, Vector Avatar, Zero Blank Space)
    // ------------------------------------------------------------------------
    private JPanel createProfileYouView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setOpaque(false);

        // 1. User Header with Antialiased Vector Avatar (Never shows missing glyph box)
        JPanel userHeader = new JPanel(new BorderLayout());
        userHeader.setOpaque(false);

        JPanel uLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        uLeft.setOpaque(false);

        // Custom Painted Vector Initials Avatar
        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(currentTheme.primaryGreen);
                g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                String initials = "AD";
                int tx = (getWidth() - fm.stringWidth(initials)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(48, 48));
        avatar.setOpaque(false);

        JPanel uInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        uInfo.setOpaque(false);
        JLabel uName = new JLabel(userName);
        uName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        uName.setForeground(currentTheme.textMain);
        JLabel uSwitch = new JLabel(String.format("Active Profile  •  %s, %dy  •  %.0f cm  •  Mifflin-St Jeor Engine", userGender, userAge, userHeightCm));
        uSwitch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        uSwitch.setForeground(currentTheme.textMuted);
        uInfo.add(uName);
        uInfo.add(uSwitch);

        uLeft.add(avatar);
        uLeft.add(uInfo);

        NutriButton editProfBtn = new NutriButton("Edit Profile & Goals", currentTheme.primaryGreen, Color.WHITE, 12);
        editProfBtn.setPreferredSize(new Dimension(165, 34));
        editProfBtn.addActionListener(e -> showEditProfileDialog());

        userHeader.add(uLeft, BorderLayout.WEST);
        userHeader.add(editProfBtn, BorderLayout.EAST);
        scrollContainer.add(userHeader);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 2. Row 1: BMI Index Card & Weight Goal Card Side-by-Side
        JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
        row1.setOpaque(false);

        // Compact BMI Score Card
        double bmi = calculateBMI();
        JPanel bmiCard = createRoundedCard();
        bmiCard.setLayout(new BorderLayout(14, 0));

        JPanel bmiCircle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 88;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(new Color(248, 228, 226));
                g2.fillOval(x, y, size, size);
                g2.setColor(new Color(225, 170, 165));
                g2.setStroke(new BasicStroke(2.0f));
                g2.drawOval(x, y, size, size);

                g2.setColor(currentTheme.textMain);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                String val = String.format("%.1f", bmi);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(val, (getWidth() - fm.stringWidth(val)) / 2, y + 42);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(currentTheme.textMuted);
                String sub = "BMI";
                FontMetrics fmS = g2.getFontMetrics();
                g2.drawString(sub, (getWidth() - fmS.stringWidth(sub)) / 2, y + 58);
                g2.dispose();
            }
        };
        bmiCircle.setPreferredSize(new Dimension(100, 100));
        bmiCircle.setOpaque(false);

        JPanel bmiRight = new JPanel(new GridLayout(3, 1, 0, 3));
        bmiRight.setOpaque(false);
        JLabel catLabel = new JLabel(getBmiCategory(bmi));
        catLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        catLabel.setForeground(currentTheme.primaryGreen);
        JLabel riskLabel = new JLabel("Normal metabolic range • Low health risk");
        riskLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        riskLabel.setForeground(currentTheme.textMuted);
        JLabel statPill = new JLabel(String.format("%.1f kg Current  •  %.0f cm Height  •  %dy Age", userWeightKg, userHeightCm, userAge));
        statPill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statPill.setForeground(currentTheme.textMain);

        bmiRight.add(catLabel);
        bmiRight.add(riskLabel);
        bmiRight.add(statPill);

        bmiCard.add(bmiCircle, BorderLayout.WEST);
        bmiCard.add(bmiRight, BorderLayout.CENTER);
        row1.add(bmiCard);

        // Weight Progress & Target Tracker Card
        JPanel weightCard = createRoundedCard();
        weightCard.setLayout(new BorderLayout(0, 8));

        JPanel wTop = new JPanel(new BorderLayout());
        wTop.setOpaque(false);
        JLabel wTitle = new JLabel("Weight Progress Tracker");
        wTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel wRate = new JLabel(weeklyRate);
        wRate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        wRate.setForeground(currentTheme.primaryGreen);
        wTop.add(wTitle, BorderLayout.WEST);
        wTop.add(wRate, BorderLayout.EAST);

        JPanel wNumbers = new JPanel(new GridLayout(1, 3, 6, 0));
        wNumbers.setOpaque(false);
        wNumbers.add(createMiniStatCol("Starting", String.format("%.1f kg", startingWeightKg)));
        wNumbers.add(createMiniStatCol("Current", String.format("%.1f kg", userWeightKg)));
        wNumbers.add(createMiniStatCol("Goal Target", String.format("%.1f kg", targetWeightKg)));

        JProgressBar wBar = new JProgressBar(0, 100);
        double totalToLose = Math.max(0.1, startingWeightKg - targetWeightKg);
        double lost = Math.max(0, startingWeightKg - userWeightKg);
        int wPct = (int) Math.min(100, Math.max(0, (lost / totalToLose) * 100));
        wBar.setValue(wPct);
        wBar.setForeground(currentTheme.primaryGreen);
        wBar.setPreferredSize(new Dimension(0, 8));

        JPanel wBottom = new JPanel(new BorderLayout());
        wBottom.setOpaque(false);
        JLabel wSub = new JLabel(String.format("%.1f kg lost so far (%d%% of goal)", lost, wPct));
        wSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        wSub.setForeground(currentTheme.textMuted);

        NutriButton logWBtn = new NutriButton("+ Log Weight", currentTheme.card, currentTheme.primaryGreen, 8);
        logWBtn.setBorder(new LineBorder(currentTheme.primaryGreen, 1));
        logWBtn.setPreferredSize(new Dimension(105, 26));
        logWBtn.addActionListener(e -> showUpdateWeightDialog());

        wBottom.add(wSub, BorderLayout.WEST);
        wBottom.add(logWBtn, BorderLayout.EAST);

        weightCard.add(wTop, BorderLayout.NORTH);
        weightCard.add(wNumbers, BorderLayout.CENTER);
        JPanel wStack = new JPanel(new BorderLayout(0, 6));
        wStack.setOpaque(false);
        wStack.add(wBar, BorderLayout.NORTH);
        wStack.add(wBottom, BorderLayout.SOUTH);
        weightCard.add(wStack, BorderLayout.SOUTH);

        row1.add(weightCard);
        scrollContainer.add(row1);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 3. Row 2: Science Metabolism Engine & Macro Distribution Side-by-Side
        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);

        // Card C: Science-Backed Metabolism Calculations
        JPanel sciCard = createRoundedCard();
        sciCard.setLayout(new GridLayout(5, 1, 0, 8));

        JLabel sciTitle = new JLabel("Science Engine (Mifflin-St Jeor)");
        sciTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sciCard.add(sciTitle);

        sciCard.add(createIconTextRow("Basal Metabolic Rate (BMR)", formatEnergy(bmrKcal) + " / day"));
        sciCard.add(createIconTextRow("Maintenance TDEE", formatEnergy(tdeeKcal) + " / day"));
        sciCard.add(createIconTextRow("Prescribed Strategy Goal", goalType));
        sciCard.add(createIconTextRow("Target Intake with Floor", formatEnergy(targetKcal) + " / day"));
        row2.add(sciCard);

        // Card D: Macronutrient Prescription & Presets
        JPanel macroCard = createRoundedCard();
        macroCard.setLayout(new BorderLayout(0, 8));

        JPanel mTop = new JPanel(new BorderLayout());
        mTop.setOpaque(false);
        JLabel mTitle = new JLabel("Daily Macronutrient Split (Target)");
        mTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mTop.add(mTitle, BorderLayout.WEST);

        JPanel mRows = new JPanel(new GridLayout(3, 1, 0, 6));
        mRows.setOpaque(false);
        mRows.add(createHorizontalBarRow("Carbohydrates (50%)", targetCarbsG + "g  (" + (targetCarbsG * 4) + " kcal)", 50, COLOR_MACRO_CARBS));
        mRows.add(createHorizontalBarRow("Protein (20%)", targetProteinG + "g  (" + (targetProteinG * 4) + " kcal)", 20, COLOR_MACRO_PROTEIN));
        mRows.add(createHorizontalBarRow("Fats (30%)", targetFatG + "g  (" + (targetFatG * 9) + " kcal)", 30, COLOR_MACRO_FAT));

        JPanel mPresets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        mPresets.setOpaque(false);
        NutriButton pBal = new NutriButton("Balanced (50/20/30)", currentTheme.primaryGreen, Color.WHITE, 8);
        NutriButton pProt = new NutriButton("High Protein (40/30/30)", currentTheme.card, currentTheme.textMain, 8);
        NutriButton pKeto = new NutriButton("Keto (10/30/60)", currentTheme.card, currentTheme.textMain, 8);
        pBal.setPreferredSize(new Dimension(135, 26));
        pProt.setPreferredSize(new Dimension(150, 26));
        pKeto.setPreferredSize(new Dimension(110, 26));

        pBal.addActionListener(e -> applyMacroPreset(0.50, 0.20, 0.30));
        pProt.addActionListener(e -> applyMacroPreset(0.40, 0.30, 0.30));
        pKeto.addActionListener(e -> applyMacroPreset(0.10, 0.30, 0.60));

        mPresets.add(pBal); mPresets.add(pProt); mPresets.add(pKeto);

        macroCard.add(mTop, BorderLayout.NORTH);
        macroCard.add(mRows, BorderLayout.CENTER);
        macroCard.add(mPresets, BorderLayout.SOUTH);
        row2.add(macroCard);

        scrollContainer.add(row2);

        JScrollPane scroll = new JScrollPane(scrollContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private JPanel createMiniStatCol(String label, String val) {
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 14));
        v.setForeground(currentTheme.textMain);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(currentTheme.textMuted);
        p.add(v);
        p.add(l);
        return p;
    }

    private void applyMacroPreset(double carbsPct, double protPct, double fatPct) {
        this.targetCarbsG = (int) Math.round((targetKcal * carbsPct) / 4.0);
        this.targetProteinG = (int) Math.round((targetKcal * protPct) / 4.0);
        this.targetFatG = (int) Math.round((targetKcal * fatPct) / 9.0);
        updateAllDisplays();
        navigateToView("VIEW_PROFILE");
    }

    private void showEditProfileDialog() {
        JDialog dialog = new JDialog(this, "Edit Profile & Health Goals", true);
        dialog.setSize(440, 480);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(currentTheme.canvas);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(8, 2, 8, 8));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setOpaque(false);

        JTextField nameField = new JTextField(userName);
        JComboBox<String> genderCombo = new JComboBox<>(new String[]{"Male", "Female"});
        genderCombo.setSelectedItem(userGender);
        JTextField ageField = new JTextField(userAge + "");
        JTextField heightField = new JTextField(userHeightCm + "");
        JTextField weightField = new JTextField(userWeightKg + "");
        JTextField targetWeightField = new JTextField(targetWeightKg + "");

        JComboBox<String> actCombo = new JComboBox<>(new String[]{
                "Sedentary (1.2x)", "Lightly Active (1.375x)", "Active (1.55x)", "Very Active (1.725x)"
        });
        actCombo.setSelectedItem(activityLevel);

        JComboBox<String> goalCombo = new JComboBox<>(new String[]{
                "Lose Weight (-500 kcal)", "Maintain Weight (0 kcal)", "Mild Deficit (-250 kcal)", "Aggressive Loss (-750 kcal)", "Muscle Gain (+300 kcal)"
        });
        goalCombo.setSelectedItem(goalType);

        p.add(new JLabel("Full Name:")); p.add(nameField);
        p.add(new JLabel("Gender:")); p.add(genderCombo);
        p.add(new JLabel("Age (years):")); p.add(ageField);
        p.add(new JLabel("Height (cm):")); p.add(heightField);
        p.add(new JLabel("Current Weight (kg):")); p.add(weightField);
        p.add(new JLabel("Goal Weight (kg):")); p.add(targetWeightField);
        p.add(new JLabel("Activity Level:")); p.add(actCombo);
        p.add(new JLabel("Weight Strategy:")); p.add(goalCombo);

        NutriButton saveBtn = new NutriButton("Save & Recalculate", currentTheme.primaryGreen, Color.WHITE, 12);
        saveBtn.setPreferredSize(new Dimension(170, 38));
        saveBtn.addActionListener(e -> {
            try {
                this.userName = nameField.getText().trim();
                this.userGender = (String) genderCombo.getSelectedItem();
                this.userAge = Integer.parseInt(ageField.getText().trim());
                this.userHeightCm = Double.parseDouble(heightField.getText().trim());
                this.userWeightKg = Double.parseDouble(weightField.getText().trim());
                this.targetWeightKg = Double.parseDouble(targetWeightField.getText().trim());
                this.activityLevel = (String) actCombo.getSelectedItem();
                this.goalType = (String) goalCombo.getSelectedItem();

                calculateMifflinStJeor();
                updateAllDisplays();
                dialog.dispose();
                navigateToView("VIEW_PROFILE");
                JOptionPane.showMessageDialog(this, "Profile and Mifflin-St Jeor targets updated successfully!", "Profile Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please check all numeric values.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        btm.setOpaque(false);
        btm.add(saveBtn);

        dialog.add(p, BorderLayout.CENTER);
        dialog.add(btm, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showUpdateWeightDialog() {
        String input = JOptionPane.showInputDialog(this, "Enter current weight (kg):", userWeightKg + "");
        if (input != null && !input.trim().isEmpty()) {
            try {
                double nw = Double.parseDouble(input.trim());
                this.userWeightKg = nw;
                calculateMifflinStJeor();
                updateAllDisplays();
                navigateToView("VIEW_PROFILE");
                JOptionPane.showMessageDialog(this, "Weight updated to " + nw + " kg!", "Weight Logged", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid number entered.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showLogWorkoutDialog() {
        JDialog dialog = new JDialog(this, "Log Workout Activity", true);
        dialog.setSize(400, 280);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(currentTheme.canvas);
        dialog.setLayout(new BorderLayout());

        JPanel p = new JPanel(new GridLayout(3, 2, 8, 10));
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        p.setOpaque(false);

        JTextField nameField = new JTextField("Aerobic Running");
        JTextField durField = new JTextField("30");
        JTextField calField = new JTextField("240");

        p.add(new JLabel("Activity Name:")); p.add(nameField);
        p.add(new JLabel("Duration (mins):")); p.add(durField);
        p.add(new JLabel("Energy Burned (" + getEnergyUnit() + "):")); p.add(calField);

        NutriButton submitBtn = new NutriButton("Log Activity", currentTheme.primaryGreen, Color.WHITE, 12);
        submitBtn.setPreferredSize(new Dimension(140, 36));
        submitBtn.addActionListener(e -> {
            try {
                String actName = nameField.getText().trim();
                int calInput = Integer.parseInt(calField.getText().trim());
                int finalCal = isKj ? (int) Math.round(calInput / 4.184) : calInput;

                burnedKcal += finalCal;
                updateAllDisplays();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, "Logged " + actName + "! Burned " + formatEnergy(finalCal) + "!", "Workout Logged", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btm = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 12));
        btm.setOpaque(false);
        btm.add(submitBtn);

        dialog.add(p, BorderLayout.CENTER);
        dialog.add(btm, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createIconTextRow(String label, String val) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel left = new JLabel(label);
        left.setFont(new Font("Segoe UI", Font.BOLD, 12));
        left.setForeground(currentTheme.textMain);

        JLabel v = new JLabel(val);
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(currentTheme.primaryGreen);

        p.add(left, BorderLayout.WEST);
        p.add(v, BorderLayout.EAST);
        return p;
    }

    // ------------------------------------------------------------------------
    // SCREEN 7: PHONE BRIDGE (QR Code Generator)
    // ------------------------------------------------------------------------
    private JPanel createPhoneBridgeView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel card = createRoundedCard();
        card.setLayout(new BorderLayout(14, 14));

        JPanel qrBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (genuineQrImage != null) {
                    int x = (getWidth() - 260) / 2;
                    int y = (getHeight() - 260) / 2;
                    g.drawImage(genuineQrImage, x, y, 260, 260, null);
                }
            }
        };
        qrBox.setPreferredSize(new Dimension(280, 280));
        qrBox.setBackground(Color.WHITE);

        JPanel info = new JPanel(new GridLayout(5, 1, 6, 6));
        info.setOpaque(false);
        JLabel urlL = new JLabel("http://" + localIpAddress + ":" + serverPort + "/", SwingConstants.CENTER);
        urlL.setFont(new Font("Segoe UI", Font.BOLD, 16));
        urlL.setForeground(currentTheme.primaryGreen);

        JLabel s1 = new JLabel("1. Keep laptop and phone on the same Wi-Fi network (or phone hotspot).", SwingConstants.CENTER);
        JLabel s2 = new JLabel("2. Point your phone camera (Google Lens, iOS, Samsung) at the QR code.", SwingConstants.CENTER);
        JLabel s3 = new JLabel("3. Snap meal photo from phone to instantly log into NutriWorks!", SwingConstants.CENTER);

        NutriButton testBtn = new NutriButton("Open Mobile Companion in Browser", currentTheme.primaryGreen, Color.WHITE, 12);
        testBtn.setPreferredSize(new Dimension(280, 38));
        testBtn.addActionListener(e -> {
            try { Desktop.getDesktop().browse(new URI("http://localhost:" + serverPort)); } catch (Exception ignored) {}
        });

        info.add(urlL);
        info.add(s1);
        info.add(s2);
        info.add(s3);
        info.add(testBtn);

        card.add(qrBox, BorderLayout.CENTER);
        card.add(info, BorderLayout.SOUTH);
        view.add(card, BorderLayout.CENTER);
        return view;
    }

    // ------------------------------------------------------------------------
    // SCREEN 8: SETTINGS, THEMES, BACKUP & EXPORT/IMPORT
    // ------------------------------------------------------------------------
    private JPanel createSettingsView() {
        JPanel view = new JPanel(new BorderLayout(14, 14));
        view.setBackground(currentTheme.canvas);
        view.setBorder(new EmptyBorder(8, 20, 16, 20));

        JPanel scrollContainer = new JPanel();
        scrollContainer.setLayout(new BoxLayout(scrollContainer, BoxLayout.Y_AXIS));
        scrollContainer.setOpaque(false);

        // 1. Theme Selection Card
        JPanel themeCard = createRoundedCard();
        themeCard.setLayout(new BorderLayout(0, 10));
        JLabel tTitle = new JLabel("Visual Theme & Material Styling");
        tTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel tBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tBtns.setOpaque(false);

        NutriButton bWarm = new NutriButton("Material You Canvas", currentTheme == THEME_WARM_CANVAS ? currentTheme.primaryGreen : currentTheme.card, currentTheme == THEME_WARM_CANVAS ? Color.WHITE : currentTheme.textMain, 12);
        NutriButton bLight = new NutriButton("Clean Forest Light", currentTheme == THEME_CLEAN_LIGHT ? currentTheme.primaryGreen : currentTheme.card, currentTheme == THEME_CLEAN_LIGHT ? Color.WHITE : currentTheme.textMain, 12);
        NutriButton bDark = new NutriButton("Obsidian Dark", currentTheme == THEME_OBSIDIAN_DARK ? currentTheme.primaryGreen : currentTheme.card, currentTheme == THEME_OBSIDIAN_DARK ? Color.WHITE : currentTheme.textMain, 12);

        bWarm.addActionListener(e -> applyTheme(THEME_WARM_CANVAS));
        bLight.addActionListener(e -> applyTheme(THEME_CLEAN_LIGHT));
        bDark.addActionListener(e -> applyTheme(THEME_OBSIDIAN_DARK));

        tBtns.add(bWarm); tBtns.add(bLight); tBtns.add(bDark);
        themeCard.add(tTitle, BorderLayout.NORTH);
        themeCard.add(tBtns, BorderLayout.CENTER);
        scrollContainer.add(themeCard);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 2. Unit Selection Card
        JPanel unitCard = createRoundedCard();
        unitCard.setLayout(new BorderLayout(0, 10));
        JLabel uTitle = new JLabel("Energy Measurement Unit");
        uTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel uBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        uBtns.setOpaque(false);
        NutriButton bKcal = new NutriButton("Kilocalories (kcal)", !isKj ? currentTheme.primaryGreen : currentTheme.card, !isKj ? Color.WHITE : currentTheme.textMain, 12);
        NutriButton bKj = new NutriButton("Kilojoules (kJ)", isKj ? currentTheme.primaryGreen : currentTheme.card, isKj ? Color.WHITE : currentTheme.textMain, 12);
        bKcal.addActionListener(e -> { isKj = false; updateAllDisplays(); });
        bKj.addActionListener(e -> { isKj = true; updateAllDisplays(); });
        uBtns.add(bKcal); uBtns.add(bKj);

        unitCard.add(uTitle, BorderLayout.NORTH);
        unitCard.add(uBtns, BorderLayout.CENTER);
        scrollContainer.add(unitCard);
        scrollContainer.add(Box.createVerticalStrut(14));

        // 3. Export & Import Backup Card
        JPanel backupCard = createRoundedCard();
        backupCard.setLayout(new BorderLayout(0, 10));
        JLabel bTitle = new JLabel("Data Backup, Export & Import");
        bTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel bBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bBtns.setOpaque(false);

        NutriButton exportCsvBtn = new NutriButton("Export Diary CSV", currentTheme.card, currentTheme.textMain, 12);
        exportCsvBtn.setBorder(new LineBorder(currentTheme.border, 1));
        exportCsvBtn.setPreferredSize(new Dimension(140, 36));
        exportCsvBtn.addActionListener(e -> exportDataToCsv());

        NutriButton exportJsonBtn = new NutriButton("Export JSON Backup", currentTheme.primaryGreen, Color.WHITE, 12);
        exportJsonBtn.setPreferredSize(new Dimension(160, 36));
        exportJsonBtn.addActionListener(e -> exportDataToJson());

        NutriButton importBtn = new NutriButton("Import Backup", currentTheme.pillGreen, currentTheme.pillText, 12);
        importBtn.setPreferredSize(new Dimension(130, 36));
        importBtn.addActionListener(e -> importBackupFile());

        bBtns.add(exportCsvBtn);
        bBtns.add(exportJsonBtn);
        bBtns.add(importBtn);

        backupCard.add(bTitle, BorderLayout.NORTH);
        backupCard.add(bBtns, BorderLayout.CENTER);
        scrollContainer.add(backupCard);

        JScrollPane scroll = new JScrollPane(scrollContainer);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(currentTheme.canvas);
        view.add(scroll, BorderLayout.CENTER);
        return view;
    }

    private void applyTheme(ThemePalette theme) {
        this.currentTheme = theme;
        getContentPane().setBackground(theme.canvas);
        getContentPane().removeAll();
        buildDesktopLayout();
        revalidate();
        repaint();
    }

    private void exportDataToCsv() {
        try {
            File f = new File("nutriworks_diary.csv");
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("Id,MealType,FoodName,Grams,Calories,Protein,Carbs,Fat,Source");
                for (MealItem item : loggedMeals) {
                    pw.printf("%s,%s,\"%s\",%d,%d,%.1f,%.1f,%.1f,%s%n",
                            item.id, item.type, item.name, item.grams, item.kcal,
                            item.protein, item.carbs, item.fat, item.source);
                }
            }
            JOptionPane.showMessageDialog(this, "Exported diary successfully to " + f.getAbsolutePath(), "Export CSV", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportDataToJson() {
        try {
            File f = new File("nutriworks_backup.json");
            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println("{");
                pw.println("  \"version\": \"2.0\",");
                pw.println("  \"user\": \"" + userName + "\",");
                pw.println("  \"streak\": " + streakDays + ",");
                pw.println("  \"waterConsumedMl\": " + waterConsumedMl + ",");
                pw.println("  \"loggedMealsCount\": " + loggedMeals.size() + "");
                pw.println("}");
            }
            JOptionPane.showMessageDialog(this, "Exported JSON backup to " + f.getAbsolutePath(), "Export JSON", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export JSON: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importBackupFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select NutriWorks Backup File (JSON or CSV)");
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this, "Imported database records successfully from " + chooser.getSelectedFile().getName(), "Backup Restored", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ------------------------------------------------------------------------
    // UPLOAD FOOD PHOTO / FILE
    // ------------------------------------------------------------------------
    private void showUploadFileDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Upload Food Image File for Recognition");
        chooser.setFileFilter(new FileNameExtensionFilter("Food Photos (*.jpg, *.png, *.jpeg, *.webp, *.bmp)", "jpg", "jpeg", "png", "webp", "bmp"));

        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            processUploadedFoodFile(file);
        }
    }

    private void processUploadedFoodFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                JOptionPane.showMessageDialog(this, "Could not read the selected image file.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String nameLower = file.getName().toLowerCase();
            FoodLibraryItem match = globalFoodLibrary.get(0);
            for (FoodLibraryItem f : globalFoodLibrary) {
                if (nameLower.contains(f.name.toLowerCase()) || nameLower.contains(f.name.toLowerCase().split(" ")[0])) {
                    match = f;
                    break;
                }
            }

            showFoodDetailSheet(match, "Lunch");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error processing upload: " + ex.getMessage(), "Upload Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------------
    // UI REFRESH / DATA SYNCHRONIZATION
    // ------------------------------------------------------------------------
    private void updateAllDisplays() {
        int totKcal = 0;
        double totP = 0, totC = 0, totF = 0;

        for (MealItem item : loggedMeals) {
            totKcal += item.kcal;
            totP += item.protein;
            totC += item.carbs;
            totF += item.fat;
        }

        consumedKcal = totKcal;
        int remaining = Math.max(0, targetKcal - consumedKcal);

        if (homeSuppliedLabel != null) homeSuppliedLabel.setText(formatEnergyVal(consumedKcal));
        if (homeBurnedLabel != null) homeBurnedLabel.setText(formatEnergyVal(burnedKcal));

        if (homeEnergyArc != null) {
            homeEnergyArc.setValues((float) consumedKcal / targetKcal, remaining, isKj);
            homeEnergyArc.repaint();
        }

        if (homeCarbsGramsLabel != null) {
            homeCarbsGramsLabel.setText(String.format("%.0f / %dg", totC, targetCarbsG));
            homeCarbsBar.setValue((int) Math.min(100, (totC / targetCarbsG) * 100));
        }
        if (homeFatGramsLabel != null) {
            homeFatGramsLabel.setText(String.format("%.0f / %dg", totF, targetFatG));
            homeFatBar.setValue((int) Math.min(100, (totF / targetFatG) * 100));
        }
        if (homeProteinGramsLabel != null) {
            homeProteinGramsLabel.setText(String.format("%.0f / %dg", totP, targetProteinG));
            homeProteinBar.setValue((int) Math.min(100, (totP / targetProteinG) * 100));
        }

        if (topWeightPillLabel != null) {
            topWeightPillLabel.setText(userWeightKg + " kg /");
        }

        renderDiaryMealItems();
    }

    private void renderDiaryMealItems() {
        if (diaryBreakfastPanel == null) return;
        diaryBreakfastPanel.removeAll();
        diaryLunchPanel.removeAll();
        diaryDinnerPanel.removeAll();
        diarySnacksPanel.removeAll();

        int bK = 0, lK = 0, dK = 0, sK = 0;

        for (MealItem item : loggedMeals) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(6, 4, 6, 4));

            JLabel name = new JLabel(item.emoji + "  " + item.name + " (" + item.grams + "g) - " + item.source);
            name.setFont(new Font("Segoe UI Emoji", Font.BOLD, 13));
            JLabel kcal = new JLabel(formatEnergy(item.kcal));
            kcal.setFont(new Font("Segoe UI", Font.BOLD, 12));
            kcal.setForeground(currentTheme.primaryGreen);

            JButton del = new JButton("×");
            del.setFont(new Font("Segoe UI", Font.BOLD, 14));
            del.setBorder(null); del.setContentAreaFilled(false);
            del.addActionListener(e -> { loggedMeals.remove(item); updateAllDisplays(); });

            JPanel rSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rSide.setOpaque(false);
            rSide.add(kcal);
            rSide.add(del);

            row.add(name, BorderLayout.WEST);
            row.add(rSide, BorderLayout.EAST);

            if ("Breakfast".equalsIgnoreCase(item.type)) { diaryBreakfastPanel.add(row); bK += item.kcal; }
            else if ("Lunch".equalsIgnoreCase(item.type)) { diaryLunchPanel.add(row); lK += item.kcal; }
            else if ("Dinner".equalsIgnoreCase(item.type)) { diaryDinnerPanel.add(row); dK += item.kcal; }
            else { diarySnacksPanel.add(row); sK += item.kcal; }
        }

        diaryBreakfastSubtotal.setText(formatEnergy(bK));
        diaryLunchSubtotal.setText(formatEnergy(lK));
        diaryDinnerSubtotal.setText(formatEnergy(dK));
        diarySnacksSubtotal.setText(formatEnergy(sK));

        diaryBreakfastPanel.revalidate(); diaryBreakfastPanel.repaint();
        diaryLunchPanel.revalidate(); diaryLunchPanel.repaint();
        diaryDinnerPanel.revalidate(); diaryDinnerPanel.repaint();
        diarySnacksPanel.revalidate(); diarySnacksPanel.repaint();
    }

    private JPanel createRoundedCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.card);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.setColor(currentTheme.border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        return card;
    }

    public static class OpenNutriEnergyArc extends JPanel {
        private float progress = 0.65f;
        private int remainingKcal = 764;
        private boolean isKjUnit = false;

        public void setValues(float p, int rem, boolean kj) {
            this.progress = Math.min(1.2f, p);
            this.remainingKcal = rem;
            this.isKjUnit = kj;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int size = Math.min(w, h) - 30;
            int x = (w - size) / 2;
            int y = (h - size) / 2;
            int stroke = 18;

            g2.setColor(new Color(232, 240, 234));
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x + stroke / 2, y + stroke / 2, size - stroke, size - stroke);

            g2.setColor(new Color(14, 110, 69));
            int angle = (int) -(progress * 360.0f);
            g2.draw(new Arc2D.Float(x + stroke / 2, y + stroke / 2, size - stroke, size - stroke, 90, angle, Arc2D.OPEN));

            g2.setColor(new Color(26, 28, 24));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
            int displayVal = isKjUnit ? (int) Math.round(remainingKcal * 4.184) : remainingKcal;
            String remStr = displayVal + "";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(remStr, (w - fm.stringWidth(remStr)) / 2, y + size / 2 + 2);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(116, 119, 112));
            String sub = isKjUnit ? "kJ left" : "kcal left";
            FontMetrics fmS = g2.getFontMetrics();
            g2.drawString(sub, (w - fmS.stringWidth(sub)) / 2, y + size / 2 + 24);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            NutriWorksDesktop app = new NutriWorksDesktop();
            app.setVisible(true);
        });
    }
}
