package org.ems.ui.controller;

/**
 * @author <your group number>
 */

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.ems.application.service.EventService;
import org.ems.application.service.ImageUploadService;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.Event;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;

import java.io.File;
import java.time.LocalDate;
import java.util.UUID;

public class EventManagerController {

    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, String> colId;
    @FXML private TableColumn<Event, String> colName;
    @FXML private TableColumn<Event, String> colType;
    @FXML private TableColumn<Event, String> colLocation;

    private final EventService eventService = AppContext.get().eventService;
    private ImageUploadService imageUploadService = null;
    private LoadingDialog loadingDialog;
    private File selectedImageFile = null;

    /**
     * Get or create ImageUploadService (lazy initialization)
     */
    private ImageUploadService getImageUploadService() {
        if (imageUploadService == null) {
            try {
                imageUploadService = new ImageUploadService();
            } catch (Exception e) {
                System.err.println("Warning: Could not initialize ImageUploadService: " + e.getMessage());
                return null;
            }
        }
        return imageUploadService;
    }

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getId().toString()
                ));
        colName.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getName()
                ));
        colType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getType().name()
                ));
        colLocation.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getLocation()
                ));

        // Load events on background thread
        loadEventsAsync();
    }

    /**
     * Load events asynchronously without blocking UI
     */
    private void loadEventsAsync() {
        javafx.stage.Stage primaryStage = null;
        try {
            if (eventTable != null && eventTable.getScene() != null) {
                primaryStage = (javafx.stage.Stage) eventTable.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not get stage for loading dialog");
        }

        if (primaryStage != null) {
            loadingDialog = new LoadingDialog(primaryStage, "Loading events...");
            loadingDialog.show();
        }

        // Run on background thread
        AsyncTaskService.runAsync(
                // Background task: Load events from database
                eventService::getEvents,

                // Success callback: Update UI on JavaFX thread
                events -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    eventTable.setItems(FXCollections.observableList(events));
                    System.out.println("✓ Loaded " + events.size() + " events");
                },

                // Error callback
                error -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    System.err.println("✗ Failed to load events: " + error.getMessage());
                }
        );
    }

    @FXML
    public void onRefresh() {
        loadEventsAsync();
    }

    @FXML
    public void backToDashboard() {
        cleanup();
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        if (imageUploadService != null) {
            imageUploadService.close();
        }
    }

    @FXML
    public void onAddEvent() {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Add Event");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label typeLabel = new Label("Type:");
        ComboBox<EventType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(EventType.values());

        Label locLabel = new Label("Location:");
        TextField locField = new TextField();

        Label startLabel = new Label("Start (YYYY-MM-DD):");
        TextField startField = new TextField();

        Label endLabel = new Label("End (YYYY-MM-DD):");
        TextField endField = new TextField();

        // Image upload section
        Label imageLabel = new Label("Event Image:");
        Button imageButton = new Button("Choose Image");
        Label imageStatusLabel = new Label("No image selected");
        imageStatusLabel.setStyle("-fx-text-fill: #999;");

        imageButton.setOnAction(evt -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Event Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            try {
                javafx.stage.Stage stage = (javafx.stage.Stage) dialog.getDialogPane().getScene().getWindow();
                File file = fileChooser.showOpenDialog(stage);

                if (file != null) {
                    ImageUploadService uploadService = getImageUploadService();
                    if (uploadService == null) {
                        imageStatusLabel.setText("Image service not available");
                        imageStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                    } else if (!uploadService.isValidImageFile(file)) {
                        imageStatusLabel.setText("Invalid image format");
                        imageStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                        selectedImageFile = null;
                    } else if (!uploadService.isValidFileSize(file)) {
                        imageStatusLabel.setText("File too large (max 10MB)");
                        imageStatusLabel.setStyle("-fx-text-fill: #e74c3c;");
                        selectedImageFile = null;
                    } else {
                        selectedImageFile = file;
                        imageStatusLabel.setText("✓ " + file.getName());
                        imageStatusLabel.setStyle("-fx-text-fill: #27ae60;");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error selecting file: " + e.getMessage());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.addRow(0, nameLabel, nameField);
        grid.addRow(1, typeLabel, typeBox);
        grid.addRow(2, locLabel, locField);
        grid.addRow(3, startLabel, startField);
        grid.addRow(4, endLabel, endField);
        grid.addRow(5, imageLabel, imageButton);
        grid.add(imageStatusLabel, 1, 6);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(
                ButtonType.OK, ButtonType.CANCEL
        );

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    Event e = new Event();
                    e.setId(UUID.randomUUID());
                    e.setName(nameField.getText());
                    e.setType(typeBox.getValue());
                    e.setLocation(locField.getText());
                    e.setStartDate(LocalDate.parse(startField.getText()));
                    e.setEndDate(LocalDate.parse(endField.getText()));

                    // DO NOT set local path - it will be set after upload to R2
                    // Image path will be set to R2 URL after successful upload

                    return e;
                } catch (Exception ex) {
                    System.err.println("Error adding event: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ev -> {
            // Add event asynchronously - safely get stage
            javafx.stage.Stage primaryStage = null;
            try {
                if (eventTable != null && eventTable.getScene() != null) {
                    primaryStage = (javafx.stage.Stage) eventTable.getScene().getWindow();
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not get stage for loading dialog");
            }

            LoadingDialog addDialog = null;
            if (primaryStage != null) {
                addDialog = new LoadingDialog(primaryStage, "Adding event...");
                addDialog.show();
            }

            final LoadingDialog finalDialog = addDialog;
            final File imageToUpload = selectedImageFile;

            AsyncTaskService.runAsync(
                    () -> {
                        // Upload image if selected
                        if (imageToUpload != null && imageToUpload.exists()) {
                            try {
                                ImageUploadService uploadService = getImageUploadService();
                                if (uploadService != null) {
                                    System.out.println("Uploading image to Cloudflare R2...");
                                    String imageUrl = uploadService.uploadEventImage(
                                            imageToUpload,
                                            ev.getId().toString()
                                    );
                                    ev.setImagePath(imageUrl);
                                    System.out.println("✓ Image uploaded: " + imageUrl);
                                }
                            } catch (Exception ex) {
                                System.err.println("⚠ Warning: Failed to upload image: " + ex.getMessage());
                                // Continue anyway without image
                            }
                        }

                        // Save event
                        eventService.createEvent(ev);
                        return null;
                    },
                    result -> {
                        if (finalDialog != null) {
                            finalDialog.close();
                        }
                        selectedImageFile = null;  // Reset
                        loadEventsAsync();  // Refresh list
                        System.out.println("✓ Event added successfully");
                    },
                    error -> {
                        if (finalDialog != null) {
                            finalDialog.close();
                        }
                        System.err.println("✗ Failed to add event: " + error.getMessage());
                    }
            );
        });
    }
}
