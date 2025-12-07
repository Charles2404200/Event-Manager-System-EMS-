package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.ems.application.service.EventService;
import org.ems.application.service.ImageService;
import org.ems.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.model.enums.EventType;
import org.ems.domain.model.enums.EventStatus;
import org.ems.domain.repository.EventRepository;
import org.ems.infrastructure.repository.jdbc.JdbcEventRepository;
import org.ems.ui.stage.SceneManager;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.*;

public class ManageEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<EventRow> eventsTable;
    @FXML private Label recordCountLabel;

    private EventRepository eventRepo;
    private EventService eventService;
    private ImageService imageService;
    private List<EventRow> allEvents;

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            eventRepo = context.eventRepo;
            eventService = context.eventService;
            imageService = context.imageService;

            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            setupTableColumns();

            // Load events bất đồng bộ để không block UI
            loadAllEventsAsync();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
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

    /**
     * Bản tối ưu: load events trên background thread, dùng JOIN/COUNT để đếm session
     * tránh N+1 query cực chậm khi số event lớn.
     */
    private void loadAllEventsAsync() {
        Task<List<EventRow>> task = new Task<>() {
            @Override
            protected List<EventRow> call() {
                return loadAllEventsOptimized();
            }
        };

        task.setOnSucceeded(evt -> {
            allEvents = task.getValue();
            displayEvents(allEvents);
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            showAlert("Error", "Failed to load events: " + (ex != null ? ex.getMessage() : "unknown error"));
            ex.printStackTrace(System.err);
        });

        Thread t = new Thread(task, "manage-events-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Thực tế logic load, chạy trong background thread.
     * - Nếu có JdbcEventRepository: dùng findAllOptimized() đã JOIN sessions
     * - Nếu không: dùng findAll() + một query COUNT(*) nhóm theo event_id để đếm session
     */
    private List<EventRow> loadAllEventsOptimized() {
        List<EventRow> rows = new ArrayList<>();
        try {
            if (eventRepo == null) {
                return rows;
            }

            AppContext context = AppContext.get();
            Map<UUID, Integer> sessionCounts = new HashMap<>();

            // Đếm session theo event chỉ với 1 query COUNT(*) GROUP BY event_id
            if (context.connection != null) {
                String sql = "SELECT event_id, COUNT(*) AS cnt FROM sessions GROUP BY event_id";
                try (PreparedStatement ps = context.connection.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID eventId = (UUID) rs.getObject("event_id");
                        int cnt = rs.getInt("cnt");
                        sessionCounts.put(eventId, cnt);
                    }
                }
            }

            List<Event> events;
            if (eventRepo instanceof JdbcEventRepository jdbcRepo) {
                // Dùng bản join tối ưu sẵn nếu có
                events = jdbcRepo.findAllOptimized();
            } else {
                events = eventRepo.findAll();
            }

            for (Event event : events) {
                int sessionCount = sessionCounts.getOrDefault(event.getId(), 0);

                rows.add(new EventRow(
                        event.getId().toString(),
                        event.getName(),
                        event.getType().name(),
                        event.getLocation(),
                        event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                        event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                        event.getStatus().name(),
                        sessionCount
                ));
            }

            System.out.println("[ManageEvents] Loaded " + events.size() + " events (optimized)");
            return rows;

        } catch (Exception e) {
            System.err.println("Load events error (optimized): " + e.getMessage());
            e.printStackTrace(System.err);
            return rows;
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
                if (!typeFilter.equals("ALL") && !event.type.equals(typeFilter)) {
                    continue;
                }
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
        if (allEvents != null) {
            displayEvents(allEvents);
        }
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

            // Image upload fields
            Label imageLabel = new Label("No image selected");
            imageLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
            Button uploadImageBtn = new Button("Select Image");
            uploadImageBtn.setPrefWidth(300);

            final String[] selectedImagePath = {null};

            uploadImageBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Event Image");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                File selectedFile = fileChooser.showOpenDialog(new Stage());
                if (selectedFile != null) {
                    selectedImagePath[0] = selectedFile.getAbsolutePath();
                    imageLabel.setText("Selected: " + selectedFile.getName());
                    imageLabel.setStyle("-fx-text-fill: #008000; -fx-font-size: 12;");
                }
            });

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
                    statusCombo,
                    new Label("Event Image:"),
                    uploadImageBtn,
                    imageLabel
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

                // Upload image if selected
                if (selectedImagePath[0] != null && !selectedImagePath[0].isEmpty()) {
                    boolean imageUploadSuccess = eventService.uploadEventImage(selectedImagePath[0], event.getId());
                    if (imageUploadSuccess) {
                        showAlert("Success", "Event '" + eventName + "' created successfully with image!");
                    } else {
                        showAlert("Warning", "Event created but image upload failed. You can update the image later.");
                    }
                } else {
                    showAlert("Success", "Event '" + eventName + "' created successfully!");
                }

                loadAllEventsAsync(); // Refresh table

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

        try {
            UUID eventId = UUID.fromString(selected.id);
            Event event = eventRepo.findById(eventId);

            if (event == null) {
                showAlert("Error", "Event not found");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Event");
            dialog.setHeaderText("Edit event: " + event.getName());

            // Create form fields with current values
            TextField eventNameField = new TextField(event.getName());
            eventNameField.setPrefWidth(300);

            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.setItems(FXCollections.observableArrayList(
                    "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeCombo.setValue(event.getType().name());
            typeCombo.setPrefWidth(300);

            TextField locationField = new TextField(event.getLocation());
            locationField.setPrefWidth(300);

            DatePicker startDatePicker = new DatePicker(event.getStartDate());
            startDatePicker.setPrefWidth(300);

            DatePicker endDatePicker = new DatePicker(event.getEndDate());
            endDatePicker.setPrefWidth(300);

            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.setItems(FXCollections.observableArrayList(
                    "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusCombo.setValue(event.getStatus().name());
            statusCombo.setPrefWidth(300);

            // Image upload fields
            Label imageLabel = new Label(event.getImagePath() != null && !event.getImagePath().isEmpty()
                    ? "Current: " + new File(event.getImagePath()).getName()
                    : "No image");
            imageLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");
            Button uploadImageBtn = new Button("Change Image");
            uploadImageBtn.setPrefWidth(300);

            final String[] selectedImagePath = {null};

            uploadImageBtn.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Event Image");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
                );
                File selectedFile = fileChooser.showOpenDialog(new Stage());
                if (selectedFile != null) {
                    selectedImagePath[0] = selectedFile.getAbsolutePath();
                    imageLabel.setText("New: " + selectedFile.getName());
                    imageLabel.setStyle("-fx-text-fill: #008000; -fx-font-size: 12;");
                }
            });

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
                    statusCombo,
                    new Label("Event Image:"),
                    uploadImageBtn,
                    imageLabel
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

                // Update event
                event.setName(eventName);
                event.setType(EventType.valueOf(type));
                event.setLocation(location);
                event.setStartDate(startDate);
                event.setEndDate(endDate);
                event.setStatus(EventStatus.valueOf(status));

                eventRepo.save(event);

                // Upload new image if selected
                if (selectedImagePath[0] != null && !selectedImagePath[0].isEmpty()) {
                    boolean imageUploadSuccess = eventService.uploadEventImage(selectedImagePath[0], event.getId());
                    if (imageUploadSuccess) {
                        showAlert("Success", "Event updated successfully with new image!");
                    } else {
                        showAlert("Warning", "Event updated but image upload failed.");
                    }
                } else {
                    showAlert("Success", "Event '" + eventName + "' updated successfully!");
                }

                loadAllEventsAsync(); // Refresh table

            }
        } catch (Exception e) {
            showAlert("Error", "Failed to edit event: " + e.getMessage());
            System.err.println("Edit event error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
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
                // Dùng hàm async tối ưu thay cho hàm cũ không tồn tại
                loadAllEventsAsync(); // Refresh table

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
