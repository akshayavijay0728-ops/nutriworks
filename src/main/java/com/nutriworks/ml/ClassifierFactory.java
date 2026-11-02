package com.nutriworks.ml;

import com.nutriworks.db.DatabaseManager;
import com.nutriworks.db.SettingsDao;
import com.nutriworks.model.AppSettings;

import java.io.File;

/**
 * Factory for creating and caching the active food classifier instance.
 */
public class ClassifierFactory {

    private static FoodClassifier cachedClassifier;

    public static synchronized FoodClassifier getClassifier() {
        if (cachedClassifier == null) {
            AppSettings settings = new SettingsDao(DatabaseManager.getInstance()).getSettings();
            if (settings.isUseStubClassifier()) {
                cachedClassifier = new StubFoodClassifier();
            } else {
                File modelFile = new File(System.getProperty("user.home"), ".nutriworks/model.onnx");
                if (!modelFile.exists()) {
                    modelFile = new File("model.onnx");
                }
                cachedClassifier = new OnnxFoodClassifier(modelFile);
            }
        }
        return cachedClassifier;
    }

    public static synchronized void resetClassifier() {
        if (cachedClassifier != null) {
            cachedClassifier.close();
            cachedClassifier = null;
        }
    }
}
