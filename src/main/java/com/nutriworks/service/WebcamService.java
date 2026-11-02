package com.nutriworks.service;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

/**
 * Service managing laptop webcam device discovery and image capture.
 */
public class WebcamService {

    private static WebcamService instance;
    private Webcam activeWebcam;

    public static synchronized WebcamService getInstance() {
        if (instance == null) {
            instance = new WebcamService();
        }
        return instance;
    }

    public List<Webcam> getAvailableWebcams() {
        try {
            List<Webcam> list = Webcam.getWebcams();
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("No webcam hardware detected or driver error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public synchronized boolean startWebcam() {
        try {
            if (activeWebcam == null) {
                activeWebcam = Webcam.getDefault();
            }
            if (activeWebcam != null && !activeWebcam.isOpen()) {
                activeWebcam.setViewSize(WebcamResolution.VGA.getSize());
                activeWebcam.open();
                return true;
            }
            return activeWebcam != null && activeWebcam.isOpen();
        } catch (Exception e) {
            System.err.println("Failed opening webcam: " + e.getMessage());
            return false;
        }
    }

    public synchronized BufferedImage captureFrame() {
        if (activeWebcam != null && activeWebcam.isOpen()) {
            return activeWebcam.getImage();
        }
        return null;
    }

    public synchronized void stopWebcam() {
        if (activeWebcam != null) {
            try {
                if (activeWebcam.isOpen()) {
                    activeWebcam.close();
                }
            } catch (Exception ignored) {
            }
            activeWebcam = null;
        }
    }

    public synchronized boolean isStreaming() {
        return activeWebcam != null && activeWebcam.isOpen();
    }
}
