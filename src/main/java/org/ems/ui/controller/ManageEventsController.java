package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.ems.application.dto.EventCRUDResultDTO;
import org.ems.application.dto.EventFormDataDTO;
import org.ems.application.dto.EventRowDTO;
import org.ems.application.service.*;
import org.ems.config.AppContext;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.ui.stage.SceneManager;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * ManageEventsController - UI Controller for Event Management (Admin)
 *
 * Responsibilities:
 * - Handle FXML bindings
 * - Route user events to services
 * - Update UI with results
 *
 * Does NOT handle:
 * - CRUD operations (→ EventCRUDService)
 * - Filtering/searching (→ EventFilterService)
 * - Image upload (→ EventImageUploadService)
 * - Validation (→ EventValidationService)
 * - Data conversion (→ EventDataConverterService)
 *
 * @author EMS Team
 */
public class ManageEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<EventRowDTO> eventsTable;
    @FXML private Label recordCountLabel;

    // ===== DEPENDENCY INJECTION - Services =====
    private EventCRUDService crudService;
    private EventFilterService filterService;
    private EventImageUploadService imageService;
    private EventValidationService validationService;
    private EventDataConverterService converterService;

    // ===== STATE =====
    private List<EventRowDTO> currentDisplayedEvents;

    @FXML
    public void initialize() {
        long start = System.currentTimeMillis();
        System.out.println("⚙️ [ManageEventsController] initialize() starting...");

        try {
            // Setup services
            AppContext context = AppContext.get();
            EventRepository eventRepo = context.eventRepo;
            SessionRepository sessionRepo = context.sessionRepo;

            crudService = new EventCRUDService(eventRepo, context.imageService);
            filterService = new EventFilterService(eventRepo);
            imageService = new EventImageUploadService(context.imageService);
            validationService = new EventValidationService();
            converterService = new EventDataConverterService(sessionRepo, context.connection);

            System.out.println("✓ Services initialized");

            // Setup UI
            setupTypeFilterCombo();
            setupTableColumns();
            setupEventListeners();

            System.out.println("✓ UI setup completed");

            // Load events async
            Platform.runLater(this::loadEventsAsync);

            System.out.println("✓ initialize() completed in " + (System.currentTimeMillis() - start) + " ms");

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to initialize: " + e.getMessage());
        }
    }

    /**
     * Setup type filter combobox
     */
    private void setupTypeFilterCombo() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
        ));
        typeFilterCombo.setValue("ALL");
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        var columns = eventsTable.getColumns();
        if (columns.size() < 8) return;

        ((TableColumn<EventRowDTO, String>) columns.get(0)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().eventId.toString()));
        ((TableColumn<EventRowDTO, String>) columns.get(1)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().name));
        ((TableColumn<EventRowDTO, String>) columns.get(2)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().type));
        ((TableColumn<EventRowDTO, String>) columns.get(3)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().location));
        ((TableColumn<EventRowDTO, String>) columns.get(4)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().startDate));
        ((TableColumn<EventRowDTO, String>) columns.get(5)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().endDate));
        ((TableColumn<EventRowDTO, String>) columns.get(6)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().status));
        ((TableColumn<EventRowDTO, String>) columns.get(7)).setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().sessionCount)));
    }

    /**
     * Setup event listeners
     */
    private void setupEventListeners() {
        typeFilterCombo.setOnAction(e -> onSearch());
        searchField.setOnAction(e -> onSearch());
    }

    /**
     * Load all events asynchronously
     */
    private void loadEventsAsync() {
        System.out.println("📥 [ManageEventsController] loadEventsAsync() starting...");

        Task<List<EventRowDTO>> task = new Task<>() {
            @Override
            protected List<EventRowDTO> call() {
                long start = System.currentTimeMillis();
                List<org.ems.domain.model.Event> allEvents = filterService.getAllEvents();
                List<EventRowDTO> rows = converterService.convertEventsToRows(allEvents);
                System.out.println("✓ loadEventsAsync task completed in " + (System.currentTimeMillis() - start) + " ms");
                return rows;
            }
        };

        task.setOnSucceeded(evt -> {
            currentDisplayedEvents = task.getValue();
            displayEvents(currentDisplayedEvents);
        });

        task.setOnFailed(evt -> {
            System.err.println("✗ Failed to load events: " + task.getException().getMessage());
            showAlert("Error", "Failed to load events: " + task.getException().getMessage());
        });

        new Thread(task, "manage-events-loader").start();
    }

    /**
     * Display events in table
     */
    private void displayEvents(List<EventRowDTO> events) {
        Platform.runLater(() -> {
            eventsTable.setItems(FXCollections.observableArrayList(events));
            recordCountLabel.setText("Total Records: " + events.size());
        });
    }

    /**
     * Search and filter events
     */
    @FXML
    public void onSearch() {
        String searchTerm = searchField.getText();
        String typeFilter = typeFilterCombo.getValue();

        System.out.println("[ManageEventsController] onSearch: term='" + searchTerm + "', type=" + typeFilter);

        Task<List<EventRowDTO>> task = new Task<>() {
            @Override
            protected List<EventRowDTO> call() {
                List<org.ems.domain.model.Event> filtered = filterService.filterEvents(searchTerm, typeFilter);
                return converterService.convertEventsToRows(filtered);
            }
        };

        task.setOnSucceeded(evt -> displayEvents(task.getValue()));
        task.setOnFailed(evt -> showAlert("Error", "Search failed: " + task.getException().getMessage()));

        new Thread(task, "search-filter-task").start();
    }

    /**
     * Reset filters
     */
    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        if (currentDisplayedEvents != null) {
            displayEvents(currentDisplayedEvents);
        }
    }

    /**
     * Create new event
     */
    @FXML
    public void onCreateEvent() {
        System.out.println("[ManageEventsController] onCreateEvent() starting...");

        EventFormData formData = showEventFormDialog(null);
        if (formData == null) {
            return;  // User cancelled
        }

        // Validate
        var validation = validationService.validate(formData.dto.name, formData.dto.location,
                formData.dto.startDate, formData.dto.endDate);
        if (!validation.valid) {
            showAlert("Validation Error", validation.message);
            return;
        }

        // Create in async task
        Task<EventCRUDResultDTO> task = new Task<>() {
            @Override
            protected EventCRUDResultDTO call() {
                try {
                    // Step 1: Upload image FIRST if selected (before creating event)
                    String imageUrl = null;
                    if (formData.imagePath != null && !formData.imagePath.isEmpty()) {
                        // Generate a temporary UUID for image upload folder organization
                        java.util.UUID tempId = java.util.UUID.randomUUID();
                        imageUrl = imageService.uploadImage(formData.imagePath, tempId);
                        if (imageUrl == null) {
                            System.err.println("[ManageEventsController] Image upload failed - aborting event creation");
                            return EventCRUDResultDTO.error(
                                    "Image upload failed. Event not created. Please check your image file and try again.",
                                    "CREATE"
                            );
                        }
                        System.out.println("[ManageEventsController] Image uploaded to R2: " + imageUrl);
                    }

                    // Step 2: Create event with the image URL (if we have one)
                    var event = crudService.createEvent(
                            formData.dto.name, formData.dto.type, formData.dto.location,
                            formData.dto.startDate, formData.dto.endDate, formData.dto.status,
                            imageUrl  // Include image URL from the start
                    );

                    if (imageUrl != null) {
                        // Optionally: Move/rename the image in R2 to use actual event ID
                        // For now, we keep it with the temporary ID - it still works
                        return EventCRUDResultDTO.success(
                                "Event '" + event.getName() + "' created successfully with image!",
                                "CREATE"
                        );
                    } else {
                        return EventCRUDResultDTO.success("Event '" + event.getName() + "' created successfully!", "CREATE");
                    }

                } catch (Exception e) {
                    System.err.println("[ManageEventsController] Error creating event: " + e.getMessage());
                    e.printStackTrace();
                    return EventCRUDResultDTO.error("Failed to create event: " + e.getMessage(), "CREATE");
                }
            }
        };

        task.setOnSucceeded(evt -> {
            EventCRUDResultDTO result = task.getValue();
            showAlert(result.success ? "Success" : "Error", result.message);
            if (result.success) {
                filterService.invalidateCache();
                loadEventsAsync();
            }
        });

        new Thread(task, "create-event-task").start();
    }

    /**
     * Edit selected event
     */
    @FXML
    public void onEditEvent() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to edit");
            return;
        }

        System.out.println("[ManageEventsController] onEditEvent: " + selected.name);

        // Load full event
        var event = crudService.getEvent(selected.eventId);
        if (event == null) {
            showAlert("Error", "Event not found");
            return;
        }

        EventFormData formData = showEventFormDialog(event);
        if (formData == null) {
            return;  // User cancelled
        }

        // Validate
        var validation = validationService.validate(formData.dto.name, formData.dto.location,
                formData.dto.startDate, formData.dto.endDate);
        if (!validation.valid) {
            showAlert("Validation Error", validation.message);
            return;
        }

        // Update in async task
        Task<EventCRUDResultDTO> task = new Task<>() {
            @Override
            protected EventCRUDResultDTO call() {
                try {
                    // Step 1: Handle image upload if a new image is selected
                    String imageUrlToUse = event.getImagePath();  // Keep existing image by default

                    if (formData.imagePath != null && !formData.imagePath.isEmpty()) {
                        // User selected a new image - upload it
                        String newImageUrl = imageService.uploadImage(formData.imagePath, event.getId());
                        if (newImageUrl != null) {
                            System.out.println("[ManageEventsController] New image uploaded to R2: " + newImageUrl);
                            imageUrlToUse = newImageUrl;  // Use new image
                        } else {
                            System.out.println("[ManageEventsController] Image upload failed - keeping existing image");
                            // Keep the existing image if upload fails
                        }
                    }

                    // Step 2: Update event with the image URL to use
                    var updated = crudService.updateEvent(
                            event.getId(),
                            formData.dto.name, formData.dto.type, formData.dto.location,
                            formData.dto.startDate, formData.dto.endDate, formData.dto.status,
                            imageUrlToUse
                    );

                    if (formData.imagePath != null && !formData.imagePath.isEmpty()) {
                        return EventCRUDResultDTO.success("Event updated successfully with new image!", "UPDATE");
                    } else {
                        return EventCRUDResultDTO.success("Event '" + updated.getName() + "' updated successfully!", "UPDATE");
                    }

                } catch (Exception e) {
                    System.err.println("[ManageEventsController] Error updating event: " + e.getMessage());
                    e.printStackTrace();
                    return EventCRUDResultDTO.error("Failed to update event: " + e.getMessage(), "UPDATE");
                }
            }
        };

        task.setOnSucceeded(evt -> {
            EventCRUDResultDTO result = task.getValue();
            showAlert(result.success ? "Success" : "Error", result.message);
            if (result.success) {
                filterService.invalidateCache();
                loadEventsAsync();
            }
        });

        new Thread(task, "update-event-task").start();
    }

    /**
     * Delete selected event
     */
    @FXML
    public void onDeleteEvent() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to delete");
            return;
        }

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
            System.out.println("[ManageEventsController] onDeleteEvent: " + selected.name);

            Task<EventCRUDResultDTO> task = new Task<>() {
                @Override
                protected EventCRUDResultDTO call() {
                    try {
                        crudService.deleteEvent(selected.eventId);
                        return EventCRUDResultDTO.success("Event '" + selected.name + "' deleted successfully!", "DELETE");
                    } catch (Exception e) {
                        return EventCRUDResultDTO.error("Failed to delete event: " + e.getMessage(), "DELETE");
                    }
                }
            };

            task.setOnSucceeded(evt -> {
                EventCRUDResultDTO result = task.getValue();
                showAlert(result.success ? "Success" : "Error", result.message);
                if (result.success) {
                    filterService.invalidateCache();
                    loadEventsAsync();
                }
            });

            new Thread(task, "delete-event-task").start();
        }
    }

    /**
     * View sessions for event
     */
    @FXML
    public void onViewSessions() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }
        showAlert("Info", "View Sessions feature coming soon!");
    }

    /**
     * View event details
     */
    @FXML
    public void onViewDetails() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }
        showAlert("Info", "View Details feature coming soon!");
    }

    /**
     * Navigate back to dashboard
     */
    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    /**
     * Show event form dialog (create or edit)
     */
    private EventFormData showEventFormDialog(org.ems.domain.model.Event editEvent) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editEvent == null ? "Create New Event" : "Edit Event");
        dialog.setHeaderText(editEvent == null ? "Create a new event" : "Edit event: " + editEvent.getName());

        // Form fields
        TextField nameField = new TextField(editEvent != null ? editEvent.getName() : "");
        nameField.setPromptText("Event Name");
        nameField.setPrefWidth(300);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(
                "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
        ));
        typeCombo.setValue(editEvent != null ? editEvent.getType().name() : "CONFERENCE");
        typeCombo.setPrefWidth(300);

        TextField locationField = new TextField(editEvent != null ? editEvent.getLocation() : "");
        locationField.setPromptText("Location");
        locationField.setPrefWidth(300);

        DatePicker startDatePicker = new DatePicker(editEvent != null ? editEvent.getStartDate() : LocalDate.now());
        startDatePicker.setPrefWidth(300);

        DatePicker endDatePicker = new DatePicker(editEvent != null ? editEvent.getEndDate() : LocalDate.now().plusDays(1));
        endDatePicker.setPrefWidth(300);

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(
                "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
        ));
        statusCombo.setValue(editEvent != null ? editEvent.getStatus().name() : "SCHEDULED");
        statusCombo.setPrefWidth(300);

        // Image upload
        Label imageLabel = new Label(
                editEvent != null && editEvent.getImagePath() != null && !editEvent.getImagePath().isEmpty()
                        ? "Current: " + new File(editEvent.getImagePath()).getName()
                        : "No image"
        );
        imageLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 12;");

        Button uploadImageBtn = new Button(editEvent != null ? "Change Image" : "Select Image");
        uploadImageBtn.setPrefWidth(300);

        final String[] selectedImagePath = {null};

        uploadImageBtn.setOnAction(e -> {
            File selectedFile = imageService.selectImageFile(new Stage());
            if (selectedFile != null) {
                selectedImagePath[0] = selectedFile.getAbsolutePath();
                imageLabel.setText("Selected: " + selectedFile.getName());
                imageLabel.setStyle("-fx-text-fill: #008000; -fx-font-size: 12;");
            }
        });

        // Form layout
        VBox formBox = new VBox(10);
        formBox.setPadding(new Insets(10));
        formBox.getChildren().addAll(
                new Label("Event Name:"),
                nameField,
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
            return new EventFormData(
                    new EventFormDataDTO(
                            nameField.getText(),
                            typeCombo.getValue(),
                            locationField.getText(),
                            startDatePicker.getValue(),
                            endDatePicker.getValue(),
                            statusCombo.getValue(),
                            null
                    ),
                    selectedImagePath[0]
            );
        }

        return null;
    }

    /**
     * Helper: Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Helper class: Form data with image path
     */
    private static class EventFormData {
        EventFormDataDTO dto;
        String imagePath;

        EventFormData(EventFormDataDTO dto, String imagePath) {
            this.dto = dto;
            this.imagePath = imagePath;
        }
    }
}

