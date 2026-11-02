package com.nutriworks.ui.pages;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the About page detailing architecture, offline privacy,
 * and clinical disclaimers.
 */
public class AboutController implements Initializable {

    @FXML private Label lblJavaVersion;
    @FXML private Label lblJavaFxVersion;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblJavaVersion.setText("Java Runtime: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        lblJavaFxVersion.setText("JavaFX Version: 21.0.2");
    }
}
