package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ems.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;
import org.ems.ui.stage.SceneManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <your group number>
 */
public class ViewEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<EventRow> eventsTable;
    @FXML private Label recordCountLabel;

    private EventRepository eventRepo;
    private List<EventRow> allEvents;

    @FXML
    public void initialize() {
        try {
            // Get repository from context
            AppContext context = AppContext.get();
            eventRepo = context.eventRepo;

            // Setup type filter combo
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            // Setup status filter combo
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all events
            loadAllEvents();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void setupTableColumns() {
        ObservableList<TableColumn<EventRow, ?>> columns = eventsTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<EventRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
            ((TableColumn<EventRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().type));
            ((TableColumn<EventRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().location));
            ((TableColumn<EventRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().startDate));
            ((TableColumn<EventRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().endDate));
            ((TableColumn<EventRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
            ((TableColumn<EventRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().sessionCount)));
            ((TableColumn<EventRow, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isRegistered ? "✓ Yes" : "No"));
        }
    }

    private void loadAllEvents() {
        try {
            allEvents = new ArrayList<>();

            // Load events from repository
            if (eventRepo != null) {
                try {
                    List<Event> events = eventRepo.findAll();
                    for (Event event : events) {
                        allEvents.add(new EventRow(
                                event.getName(),
                                event.getType().name(),
                                event.getLocation(),
                                event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                                event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                                event.getStatus().name(),
                                0,  // TODO: Load session count from sessions table
                                false  // TODO: Check if current user is registered
                        ));
                    }
                    System.out.println(" Loaded " + events.size() + " events");
                } catch (Exception e) {
                    System.err.println("⚠ Error loading events: " + e.getMessage());
                }
            }

            displayEvents(allEvents);
            System.out.println(" Total events loaded: " + allEvents.size());

        } catch (Exception e) {
            showAlert("Error", "Failed to load events: " + e.getMessage());
            System.err.println("Load events error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void displayEvents(List<EventRow> events) {
        ObservableList<EventRow> observableList = FXCollections.observableArrayList(events);
        eventsTable.setItems(observableList);
        recordCountLabel.setText("Total Events: " + events.size());
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();

            List<EventRow> filtered = new ArrayList<>();

            for (EventRow event : allEvents) {
                // Apply type filter
                if (!typeFilter.equals("ALL") && !event.type.equals(typeFilter)) {
                    continue;
                }

                // Apply status filter
                if (!statusFilter.equals("ALL") && !event.status.equals(statusFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    event.name.toLowerCase().contains(searchTerm) ||
                    event.location.toLowerCase().contains(searchTerm)) {
                    filtered.add(event);
                }
            }

            displayEvents(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        displayEvents(allEvents);
    }

    @FXML
    public void onViewDetails() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to view details");
            return;
        }
        System.out.println("View Details for event: " + selected.name);
        showAlert("Info", "Event: " + selected.name + "\n\n" +
                "Type: " + selected.type + "\n" +
                "Location: " + selected.location + "\n" +
                "Start: " + selected.startDate + "\n" +
                "End: " + selected.endDate + "\n" +
                "Status: " + selected.status + "\n" +
                "Sessions: " + selected.sessionCount);
    }

    @FXML
    public void onRegister() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to register");
            return;
        }

        if (selected.isRegistered) {
            showAlert("Info", "You are already registered for this event!");
            return;
        }

        System.out.println("Register for event: " + selected.name);
        // TODO: Implement actual event registration
        showAlert("Success", " Successfully registered for event: " + selected.name);
    }

    @FXML
    public void onViewSessions() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to view sessions");
            return;
        }
        System.out.println("View Sessions for event: " + selected.name);
        // TODO: Implement view sessions page
        showAlert("Info", "Sessions feature coming soon for: " + selected.name);
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for displaying event data in table
    public static class EventRow {
        public String name;
        public String type;
        public String location;
        public String startDate;
        public String endDate;
        public String status;
        public int sessionCount;
        public boolean isRegistered;

        public EventRow(String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount, boolean isRegistered) {
            this.name = name;
            this.type = type;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.sessionCount = sessionCount;
            this.isRegistered = isRegistered;
        }
    }
}

