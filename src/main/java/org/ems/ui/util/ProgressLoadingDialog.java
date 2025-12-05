package org.ems.ui.util;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Enhanced Loading dialog with progress bar showing percentage
 *
 * Usage:
 *   ProgressLoadingDialog dialog = new ProgressLoadingDialog(stage, "Loading events...");
 *   dialog.show();
 *   dialog.updateProgress(25);  // 25%
 *   dialog.close();
 */
public class ProgressLoadingDialog {

    private final Stage stage;
    private final Label messageLabel;
    private final Label percentLabel;
    private final ProgressBar progressBar;

    /**
     * Create a loading dialog with progress bar
     *
     * @param owner Parent window
     * @param message Initial message to display
     */
    public ProgressLoadingDialog(Window owner, String message) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Loading");
        stage.setResizable(false);

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setStyle("-fx-pref-width: 250; -fx-pref-height: 20;");

        // Message label
        messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        // Percent label
        percentLabel = new Label("0%");
        percentLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Layout
        VBox vBox = new VBox(10);
        vBox.setAlignment(Pos.CENTER);
        vBox.setStyle("-fx-padding: 25; -fx-background-color: #f5f5f5;");
        vBox.getChildren().addAll(messageLabel, progressBar, percentLabel);

        Scene scene = new Scene(vBox, 350, 150);
        stage.setScene(scene);
    }

    /**
     * Show the loading dialog
     */
    public void show() {
        Platform.runLater(() -> stage.show());
    }

    /**
     * Close the loading dialog
     */
    public void close() {
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.close();
            }
        });
    }

    /**
     * Update progress percentage (0-100)
     *
     * @param percent Progress percentage (0-100)
     */
    public void updateProgress(int percent) {
        Platform.runLater(() -> {
            double progress = Math.min(100, Math.max(0, percent)) / 100.0;
            progressBar.setProgress(progress);
            percentLabel.setText(percent + "%");
        });
    }

    /**
     * Update the message displayed
     *
     * @param message New message
     */
    public void setMessage(String message) {
        Platform.runLater(() -> messageLabel.setText(message));
    }

    /**
     * Check if dialog is showing
     */
    public boolean isShowing() {
        return stage.isShowing();
    }
}

