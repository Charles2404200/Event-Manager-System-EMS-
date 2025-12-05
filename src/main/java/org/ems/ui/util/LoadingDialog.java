package org.ems.ui.util;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Loading dialog utility for displaying loading progress while tasks run on background thread
 *
 * Usage:
 *   LoadingDialog dialog = new LoadingDialog(primaryStage, "Loading data...");
 *   dialog.show();
 *   AsyncTaskService.runAsync(
 *       () -> expensiveOperation(),
 *       result -> {
 *           dialog.close();
 *           updateUI(result);
 *       }
 *   );
 */
public class LoadingDialog {

    private final Stage stage;
    private final Label messageLabel;
    private final ProgressIndicator progressIndicator;

    /**
     * Create a loading dialog
     *
     * @param owner Parent window
     * @param message Initial message to display
     */
    public LoadingDialog(Window owner, String message) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Loading");
        stage.setResizable(false);

        // Progress indicator
        progressIndicator = new ProgressIndicator();
        progressIndicator.setStyle("-fx-pref-width: 60; -fx-pref-height: 60;");

        // Message label
        messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 14px;");

        // Layout
        VBox vBox = new VBox(15);
        vBox.setAlignment(Pos.CENTER);
        vBox.setStyle("-fx-padding: 30; -fx-background-color: #f5f5f5;");
        vBox.getChildren().addAll(progressIndicator, messageLabel);

        Scene scene = new Scene(vBox, 300, 150);
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

