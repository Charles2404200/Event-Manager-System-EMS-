package org.ems.ui.controller;
/**
 * @author <your group number>
 */

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.ems.application.service.EventService;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.Event;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EventManagerController {

    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, String> colId;
    @FXML private TableColumn<Event, String> colName;
    @FXML private TableColumn<Event, String> colType;
    @FXML private TableColumn<Event, String> colLocation;

    private final EventService eventService = AppContext.get().eventService;
    private LoadingDialog loadingDialog;

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
        // Get stage safely
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
                () -> eventService.getEvents(),

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
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
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

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.addRow(0, nameLabel, nameField);
        grid.addRow(1, typeLabel, typeBox);
        grid.addRow(2, locLabel, locField);
        grid.addRow(3, startLabel, startField);
        grid.addRow(4, endLabel, endField);

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
            AsyncTaskService.runAsync(
                    () -> {
                        eventService.createEvent(ev);
                        return null;
                    },
                    result -> {
                        if (finalDialog != null) {
                            finalDialog.close();
                        }
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
