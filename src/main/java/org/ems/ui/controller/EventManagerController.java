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

import java.time.LocalDate;
import java.util.UUID;

public class EventManagerController {

    @FXML private TableView<Event> eventTable;
    @FXML private TableColumn<Event, String> colId;
    @FXML private TableColumn<Event, String> colName;
    @FXML private TableColumn<Event, String> colType;
    @FXML private TableColumn<Event, String> colLocation;

    private final EventService eventService = AppContext.get().eventService;

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

        loadEvents();
    }

    private void loadEvents() {
        eventTable.setItems(FXCollections.observableList(eventService.getEvents()));
    }

    @FXML
    public void onRefresh() {
        loadEvents();
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
            eventService.createEvent(ev);
            loadEvents();
        });
    }
}
