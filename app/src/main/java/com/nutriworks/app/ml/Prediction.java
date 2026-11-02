package com.nutriworks.app.ml;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Encapsulates a food classification result with a human-readable label
 * and a confidence score between 0.0 and 1.0.
 */
public class Prediction {

    private final String label;
    private final float confidence;

    public Prediction(@NonNull String label, float confidence) {
        this.label = label;
        this.confidence = confidence;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    /**
     * Converts a raw underscore label (e.g. "paneer_tikka")
     * into a title-cased display label (e.g. "Paneer Tikka").
     */
    @NonNull
    public String getFormattedDisplayName() {
        if (label.isEmpty()) {
            return "";
        }

        String[] parts = label.replace('_', ' ').split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase(Locale.US));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Returns formatted percentage string (e.g. "94%").
     */
    @NonNull
    public String getConfidencePercentage() {
        int percentage = Math.round(confidence * 100);
        return percentage + "%";
    }

    @NonNull
    @Override
    public String toString() {
        return getFormattedDisplayName() + " (" + getConfidencePercentage() + ")";
    }
}
