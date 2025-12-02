package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.ems.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.repository.EventRepository;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author <your group number>
 */
public class ManageEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
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
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().id));
            ((TableColumn<EventRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
            ((TableColumn<EventRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().type));
            ((TableColumn<EventRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().location));
            ((TableColumn<EventRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().startDate));
            ((TableColumn<EventRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().endDate));
            ((TableColumn<EventRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
            ((TableColumn<EventRow, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().sessionCount)));
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
                                event.getId().toString(),
                                event.getName(),
                                event.getType().name(),
                                event.getLocation(),
                                event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                                event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                                event.getStatus().name(),
                                0  // TODO: Load session count from sessions table
                        ));
                    }
                    System.out.println(" Loaded " + events.size() + " events");
                } catch (Exception e) {
                    System.err.println("Error loading events: " + e.getMessage());
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
        recordCountLabel.setText("Total Records: " + events.size());
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();

            List<EventRow> filtered = new ArrayList<>();

            for (EventRow event : allEvents) {
                // Apply type filter
                if (!typeFilter.equals("ALL") && !event.type.equals(typeFilter)) {
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
        displayEvents(allEvents);
    }

    @FXML
    public void onCreateEvent() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Create New Event");
            dialog.setHeaderText("Create a new event");

            // Create form fields
            TextField eventNameField = new TextField();
            eventNameField.setPromptText("Event Name");
            eventNameField.setPrefWidth(300);

            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.setItems(FXCollections.observableArrayList(
                    "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeCombo.setValue("CONFERENCE");
            typeCombo.setPrefWidth(300);

            TextField locationField = new TextField();
            locationField.setPromptText("Location");
            locationField.setPrefWidth(300);

            DatePicker startDatePicker = new DatePicker();
            startDatePicker.setPrefWidth(300);

            DatePicker endDatePicker = new DatePicker();
            endDatePicker.setPrefWidth(300);

            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.setItems(FXCollections.observableArrayList(
                    "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusCombo.setValue("SCHEDULED");
            statusCombo.setPrefWidth(300);

            // Create form layout
            VBox formBox = new VBox(10);
            formBox.setPadding(new Insets(10));
            formBox.getChildren().addAll(
                    new Label("Event Name:"),
                    eventNameField,
                    new Label("Type:"),
                    typeCombo,
                    new Label("Location:"),
                    locationField,
                    new Label("Start Date:"),
                    startDatePicker,
                    new Label("End Date:"),
                    endDatePicker,
                    new Label("Status:"),
                    statusCombo
            );

            dialog.getDialogPane().setContent(formBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                // Validate inputs
                String eventName = eventNameField.getText();
                String type = typeCombo.getValue();
                String location = locationField.getText();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                String status = statusCombo.getValue();

                if (eventName.isBlank() || location.isBlank() || startDate == null || endDate == null) {
                    showAlert("Validation Error", "Please fill in all required fields");
                    return;
                }

                if (endDate.isBefore(startDate)) {
                    showAlert("Validation Error", "End date must be after start date");
                    return;
                }

                // Create event
                Event event = new Event();
                event.setId(UUID.randomUUID());
                event.setName(eventName);
                event.setType(EventType.valueOf(type));
                event.setLocation(location);
                event.setStartDate(startDate);
                event.setEndDate(endDate);
                event.setStatus(EventStatus.valueOf(status));

                eventRepo.save(event);

                showAlert("Success", "Event '" + eventName + "' created successfully!");
                loadAllEvents(); // Refresh table

            }
        } catch (Exception e) {
            showAlert("Error", "Failed to create event: " + e.getMessage());
            System.err.println("Create event error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onEditEvent() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to edit");
            return;
        }
        System.out.println("Edit Event clicked: " + selected.name);
        // TODO: Implement edit event dialog
        showAlert("Info", "Edit Event feature coming soon!");
    }

    @FXML
    public void onDeleteEvent() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to delete");
            return;
        }

        // Show confirmation dialog
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete Event - " + selected.name);
        confirmDialog.setContentText(
                "Are you sure you want to delete event: " + selected.name + "?\n\n" +
                "Type: " + selected.type + "\n" +
                "Location: " + selected.location + "\n" +
                "Start Date: " + selected.startDate + "\n\n" +
                "This action CANNOT be undone!"
        );

        if (confirmDialog.showAndWait().isPresent() && confirmDialog.showAndWait().get() == ButtonType.OK) {
            try {
                UUID eventId = UUID.fromString(selected.id);
                eventRepo.delete(eventId);

                showAlert("Success", "Event '" + selected.name + "' has been deleted successfully!");
                loadAllEvents(); // Refresh table

            } catch (Exception e) {
                showAlert("Error", "Failed to delete event: " + e.getMessage());
                System.err.println("Delete event error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
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
        showAlert("Info", "View Sessions feature coming soon!");
    }

    @FXML
    public void onViewDetails() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to view details");
            return;
        }
        System.out.println("View Details for event: " + selected.name);
        // TODO: Implement view details page
        showAlert("Info", "View Details feature coming soon!");
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
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
        public String id;
        public String name;
        public String type;
        public String location;
        public String startDate;
        public String endDate;
        public String status;
        public int sessionCount;

        public EventRow(String id, String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.sessionCount = sessionCount;
        }
    }
}

