package com.nutriworks.model;

/**
 * Value object representing an ML classification prediction result.
 */
public class Prediction implements Comparable<Prediction> {

    private final String label;
    private final float confidence;

    public Prediction(String label, float confidence) {
        this.label = label != null ? label : "Unknown";
        this.confidence = Math.max(0.0f, Math.min(1.0f, confidence));
    }

    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    public int getConfidencePercent() {
        return Math.round(confidence * 100.0f);
    }

    public String getFormattedConfidence() {
        return String.format("%.1f%%", confidence * 100.0f);
    }

    @Override
    public int compareTo(Prediction other) {
        return Float.compare(other.confidence, this.confidence);
    }

    @Override
    public String toString() {
        return label + " (" + getFormattedConfidence() + ")";
    }
}
