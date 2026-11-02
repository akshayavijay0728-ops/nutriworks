package com.nutriworks.ml;

import com.nutriworks.model.Prediction;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Deterministic offline fallback food classifier.
 * Evaluates dominant color histograms of the food image to generate top-3 predictions
 * with realistic confidence scores, allowing the phone bridge and UI to be tested seamlessly.
 */
public class StubFoodClassifier implements FoodClassifier {

    @Override
    public List<Prediction> classify(BufferedImage image, int topK) {
        if (image == null) {
            return Collections.emptyList();
        }

        // Sample dominant color in center region
        long rTotal = 0, gTotal = 0, bTotal = 0;
        int count = 0;

        int startX = image.getWidth() / 4;
        int startY = image.getHeight() / 4;
        int w = Math.max(1, image.getWidth() / 2);
        int h = Math.max(1, image.getHeight() / 2);

        for (int y = startY; y < startY + h; y += 4) {
            for (int x = startX; x < startX + w; x += 4) {
                if (x < image.getWidth() && y < image.getHeight()) {
                    int rgb = image.getRGB(x, y);
                    rTotal += (rgb >> 16) & 0xFF;
                    gTotal += (rgb >> 8) & 0xFF;
                    bTotal += rgb & 0xFF;
                    count++;
                }
            }
        }

        if (count == 0) count = 1;
        int avgR = (int) (rTotal / count);
        int avgG = (int) (gTotal / count);
        int avgB = (int) (bTotal / count);

        List<Prediction> predictions = new ArrayList<>();

        // Color heuristic matching for demonstration
        if (avgG > avgR && avgG > avgB && avgG > 90) {
            // Green dominance -> Salad
            predictions.add(new Prediction("caesar salad", 0.94f));
            predictions.add(new Prediction("greek salad", 0.04f));
            predictions.add(new Prediction("edamame", 0.02f));
        } else if (avgR > 140 && avgG > 100 && avgB < 80) {
            // Yellow / Golden orange -> Biryani or Dosa
            predictions.add(new Prediction("biryani", 0.92f));
            predictions.add(new Prediction("dosa", 0.05f));
            predictions.add(new Prediction("fried rice", 0.03f));
        } else if (avgR > 130 && avgG < 90 && avgB < 90) {
            // Reddish / baked -> Pizza or Samosa
            predictions.add(new Prediction("pizza", 0.91f));
            predictions.add(new Prediction("samosa", 0.06f));
            predictions.add(new Prediction("hamburger", 0.03f));
        } else if (avgR > 180 && avgG > 180 && avgB > 180) {
            // Light / whitish -> Idli or Naan
            predictions.add(new Prediction("idli", 0.93f));
            predictions.add(new Prediction("naan", 0.04f));
            predictions.add(new Prediction("hummus", 0.03f));
        } else {
            // Default balanced distribution
            predictions.add(new Prediction("biryani", 0.88f));
            predictions.add(new Prediction("dosa", 0.08f));
            predictions.add(new Prediction("pizza", 0.04f));
        }

        return predictions.subList(0, Math.min(topK, predictions.size()));
    }

    @Override
    public void close() {
        // No-op for stub
    }
}
