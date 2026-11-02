package com.nutriworks.app.ml;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback stub classifier allowing end-to-end testing of the UI,
 * ViewModel, and LiveData pipelines without requiring the real trained
 * model binary to be present.
 */
public class StubFoodClassifier {

    private static final String TAG = "StubFoodClassifier";

    public interface Callback {
        void onSuccess(List<Prediction> predictions, long inferenceTimeMs);
        void onError(String message);
    }

    /**
     * Simulates inference by inspecting bitmap characteristics and returning
     * plausible top-3 food predictions with realistic confidence scores.
     */
    public void classify(@NonNull Bitmap bitmap, @NonNull Callback callback) {
        long startTime = SystemClock.uptimeMillis();

        // Simple heuristic based on average pixel warmth to pick realistic meal pairs
        int width = Math.min(bitmap.getWidth(), 30);
        int height = Math.min(bitmap.getHeight(), 30);
        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        int count = 0;

        for (int y = 0; y < height; y += 3) {
            for (int x = 0; x < width; x += 3) {
                int pixel = bitmap.getPixel(x, y);
                redSum += (pixel >> 16) & 0xFF;
                greenSum += (pixel >> 8) & 0xFF;
                blueSum += pixel & 0xFF;
                count++;
            }
        }

        long avgRed = (count > 0) ? redSum / count : 128;
        long avgGreen = (count > 0) ? greenSum / count : 128;

        List<Prediction> predictions = new ArrayList<>();
        if (avgRed > avgGreen + 20) {
            // Warm/Orange/Reddish food (e.g. Biryani, Butter Chicken, Pizza)
            predictions.add(new Prediction("biryani", 0.88f));
            predictions.add(new Prediction("chicken_curry", 0.08f));
            predictions.add(new Prediction("fried_rice", 0.04f));
        } else if (avgGreen > avgRed) {
            // Greenish food (e.g. Caesar Salad, Palak/Dal)
            predictions.add(new Prediction("caesar_salad", 0.79f));
            predictions.add(new Prediction("greek_salad", 0.14f));
            predictions.add(new Prediction("edamame", 0.07f));
        } else {
            // Balanced/Golden food (e.g. Dosa, Samosa, Grilled Salmon)
            predictions.add(new Prediction("dosa", 0.84f));
            predictions.add(new Prediction("samosa", 0.11f));
            predictions.add(new Prediction("garlic_bread", 0.05f));
        }

        // Simulate realistic 30ms inference duration
        SystemClock.sleep(35);
        long inferenceTimeMs = SystemClock.uptimeMillis() - startTime;
        Log.i(TAG, "Stub inference completed in " + inferenceTimeMs + " ms. Top result: " + predictions.get(0));

        callback.onSuccess(predictions, inferenceTimeMs);
    }
}
