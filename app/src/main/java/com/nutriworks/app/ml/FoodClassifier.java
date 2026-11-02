package com.nutriworks.app.ml;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.core.BaseOptions;
import org.tensorflow.lite.task.vision.classifier.Classifications;
import org.tensorflow.lite.task.vision.classifier.ImageClassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * On-device Food Classifier utilizing TensorFlow Lite Task Vision Library.
 * Features lazy background initialization, inference execution on a worker thread,
 * inference latency profiling, and automatic fallback to StubFoodClassifier.
 */
public class FoodClassifier {

    private static final String TAG = "FoodClassifier";
    private static final String MODEL_FILE_NAME = "model.tflite";
    private static final int MAX_RESULTS = 3;

    public interface ClassificationCallback {
        void onSuccess(@NonNull List<Prediction> predictions, long inferenceTimeMs);
        void onError(@NonNull String errorMessage);
    }

    private final Context context;
    private final ExecutorService executorService;
    private ImageClassifier imageClassifier;
    private final StubFoodClassifier stubClassifier;
    private boolean isInitialized = false;
    private boolean useStubFallback = false;

    public FoodClassifier(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.stubClassifier = new StubFoodClassifier();
    }

    /**
     * Classifies a food image asynchronously on a background thread.
     *
     * @param bitmap Input bitmap (preferably 224x224).
     * @param callback Callback delivering predictions or errors.
     */
    public void classify(@NonNull Bitmap bitmap, @NonNull ClassificationCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isInitialized) {
                    initializeClassifier();
                }

                if (useStubFallback || imageClassifier == null) {
                    stubClassifier.classify(bitmap, new StubFoodClassifier.Callback() {
                        @Override
                        public void onSuccess(List<Prediction> predictions, long inferenceTimeMs) {
                            callback.onSuccess(predictions, inferenceTimeMs);
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
                    return;
                }

                long startTime = SystemClock.uptimeMillis();

                // Convert bitmap to TFLite TensorImage
                TensorImage tensorImage = TensorImage.fromBitmap(bitmap);

                // Run on-device inference
                List<Classifications> results = imageClassifier.classify(tensorImage);
                long inferenceTimeMs = SystemClock.uptimeMillis() - startTime;

                Log.i(TAG, "TFLite Task Vision inference completed in " + inferenceTimeMs + " ms");

                List<Prediction> predictions = new ArrayList<>();
                if (results != null && !results.isEmpty()) {
                    List<Category> categories = results.get(0).getCategories();
                    for (int i = 0; i < Math.min(MAX_RESULTS, categories.size()); i++) {
                        Category category = categories.get(i);
                        predictions.add(new Prediction(category.getLabel(), category.getScore()));
                    }
                }

                if (predictions.isEmpty()) {
                    // Fall back to stub if no predictions were generated
                    stubClassifier.classify(bitmap, new StubFoodClassifier.Callback() {
                        @Override
                        public void onSuccess(List<Prediction> fallbackPredictions, long timeMs) {
                            callback.onSuccess(fallbackPredictions, timeMs);
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
                } else {
                    callback.onSuccess(predictions, inferenceTimeMs);
                }

            } catch (Exception e) {
                Log.e(TAG, "Classification error: " + e.getMessage(), e);
                // Use stub fallback to keep UI functional
                stubClassifier.classify(bitmap, new StubFoodClassifier.Callback() {
                    @Override
                    public void onSuccess(List<Prediction> fallbackPredictions, long timeMs) {
                        callback.onSuccess(fallbackPredictions, timeMs);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
            }
        });
    }

    /**
     * Initializes the ImageClassifier from the assets directory.
     * If the model cannot be loaded, switches to stub fallback.
     */
    private synchronized void initializeClassifier() {
        if (isInitialized) {
            return;
        }

        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setNumThreads(2)
                    .build();

            ImageClassifier.ImageClassifierOptions options =
                    ImageClassifier.ImageClassifierOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMaxResults(MAX_RESULTS)
                            .build();

            imageClassifier = ImageClassifier.createFromFileAndOptions(
                    context,
                    MODEL_FILE_NAME,
                    options
            );

            isInitialized = true;
            useStubFallback = false;
            Log.i(TAG, "TensorFlow Lite ImageClassifier initialized successfully.");

        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Could not load " + MODEL_FILE_NAME + " from assets (" + e.getMessage() +
                    "). Falling back to StubFoodClassifier for UI preview.");
            useStubFallback = true;
            isInitialized = true;
        }
    }

    /**
     * Closes the classifier and terminates the background thread.
     */
    public void close() {
        executorService.execute(() -> {
            if (imageClassifier != null) {
                imageClassifier.close();
                imageClassifier = null;
            }
        });
        executorService.shutdown();
    }
}
