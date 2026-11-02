package com.nutriworks.ml;

import com.nutriworks.model.Prediction;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Interface for on-device food image classification engines.
 */
public interface FoodClassifier {

    /**
     * Runs inference on the given input image.
     *
     * @param image Input food photo.
     * @param topK Number of top predictions to return (e.g. 3).
     * @return Ranked list of predictions with label and confidence score.
     */
    List<Prediction> classify(BufferedImage image, int topK);

    /**
     * Releases any native model memory resources.
     */
    void close();
}
