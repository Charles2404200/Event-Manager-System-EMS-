package org.ems.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.ems.application.service.ImageService;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.PresenterRepository;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.ProgressLoadingDialog;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author <your group number>
 *
 * OPTIMIZED: Pagination + Async Loading + Lazy Loading
 * Fixed: No duplicate methods, clean structure
 */
public class ViewEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<EventRow> eventsTable;
    @FXML private Label recordCountLabel;

    // Loading placeholder components
    @FXML private VBox loadingPlaceholder;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingPercentLabel;

    private EventRepository eventRepo;
    private SessionRepository sessionRepo;
    private PresenterRepository presenterRepo;
    private ImageService imageService;
    private AppContext appContext;

    // Pagination settings
    private static final int ITEMS_PER_PAGE = 10;
    private int currentPage = 0;
    private int totalPages = 0;
    private long totalEvents = 0;

    // Cache for presenters/sessions, images and current page
    private final Map<UUID, Presenter> presenterCache = new HashMap<>();
    private final Map<UUID, List<Session>> sessionCache = new HashMap<>();
    private final Map<UUID, Image> imageCache = new HashMap<>();
    private final Map<UUID, Event> eventCache = new HashMap<>();  // NEW: Cache events
    private final Set<UUID> userRegisteredEvents = new HashSet<>();
    private List<EventRow> currentPageCache = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            appContext = AppContext.get();
            eventRepo = appContext.eventRepo;
            sessionRepo = appContext.sessionRepo;
            presenterRepo = appContext.presenterRepo;
            imageService = appContext.imageService;

            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            setupTableColumns();

            typeFilterCombo.setOnAction(e -> applyFiltersAndReset());
            statusFilterCombo.setOnAction(e -> applyFiltersAndReset());
            searchField.setOnAction(e -> applyFiltersAndReset());

            // Load first page asynchronously with progress
            loadEventsPageAsync(0, null); // no extra filter yet

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        // Columns are defined in FXML in order:
        // 0: Image, 1: Event Name, 2: Type, 3: Location, 4: Start Date, 5: End Date, 6: Status, 7: Sessions, 8: Registered
        if (eventsTable == null) return;
        var columns = eventsTable.getColumns();
        if (columns.size() < 9) return;

        // Image column with thumbnail preview - USE EventRow.imagePath directly!
        TableColumn<EventRow, String> imageCol = (TableColumn<EventRow, String>) columns.get(0);
        imageCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().imagePath));
        imageCol.setCellFactory(col -> new TableCell<EventRow, String>() {
            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setText("📷");
                    setGraphic(null);
                    setStyle("-fx-alignment: center; -fx-text-fill: #999;");
                    return;
                }

                try {
                    EventRow row = getTableView().getItems().get(getIndex());

                    // Check cache first
                    Image cachedImage = imageCache.get(row.eventId);
                    if (cachedImage == null) {
                        // Load asynchronously in background
                        cachedImage = loadImage(imagePath);
                        if (cachedImage != null) {
                            imageCache.put(row.eventId, cachedImage);
                        }
                    }

                    if (cachedImage != null) {
                        ImageView imageView = new ImageView(cachedImage);
                        imageView.setFitWidth(80);
                        imageView.setFitHeight(60);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 3;");
                        setGraphic(imageView);
                        setText(null);
                    } else {
                        setText("❌");
                        setGraphic(null);
                        setStyle("-fx-alignment: center;");
                    }
                } catch (Exception e) {
                    setText("❌");
                    setGraphic(null);
                    setStyle("-fx-alignment: center;");
                }
            }
        });

        ((TableColumn<EventRow, String>) columns.get(1)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().name));
        ((TableColumn<EventRow, String>) columns.get(2)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().type));
        ((TableColumn<EventRow, String>) columns.get(3)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().location));
        ((TableColumn<EventRow, String>) columns.get(4)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().startDate));
        ((TableColumn<EventRow, String>) columns.get(5)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().endDate));
        ((TableColumn<EventRow, String>) columns.get(6)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().status));
        ((TableColumn<EventRow, String>) columns.get(7)).setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().sessionCount)));
        ((TableColumn<EventRow, String>) columns.get(8)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isRegistered ? "✓ Yes" : "No"));
    }

    /**
     * Async load events for a given page from DB using LIMIT/OFFSET.
     * If filteredIds != null, we page over that in-memory ID list instead.
     */
    private void loadEventsPageAsync(int page, List<UUID> filteredIds) {
        showLoadingPlaceholder();
        updateInTableProgress(0);

        AsyncTaskService.runAsync(
                () -> {
                    try {
                        if (eventRepo == null) return Collections.<EventRow>emptyList();

                        if (filteredIds == null) {
                            totalEvents = eventRepo.count();
                        } else {
                            totalEvents = filteredIds.size();
                        }
                        totalPages = totalEvents == 0 ? 0 : (int) Math.ceil((double) totalEvents / ITEMS_PER_PAGE);
                        if (totalPages == 0) return Collections.<EventRow>emptyList();

                        int safePage = Math.max(0, Math.min(page, totalPages - 1));
                        int offset = safePage * ITEMS_PER_PAGE;

                        List<Event> events;
                        if (filteredIds == null) {
                            events = eventRepo.findPage(offset, ITEMS_PER_PAGE);
                        } else {
                            int end = Math.min(offset + ITEMS_PER_PAGE, filteredIds.size());
                            List<UUID> pageIds = filteredIds.subList(offset, end);
                            events = new ArrayList<>();
                            for (UUID id : pageIds) {
                                Event e = eventRepo.findById(id);
                                if (e != null) events.add(e);
                            }
                        }

                        updateInTableProgress(40);
                        preloadUserRegistrations();
                        updateInTableProgress(60);

                        List<EventRow> rows = new ArrayList<>();
                        for (Event event : events) {
                            rows.add(new EventRow(
                                    event.getId(),
                                    event.getName(),
                                    event.getType().name(),
                                    event.getLocation(),
                                    event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                                    event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                                    event.getStatus().name(),
                                    0,
                                    userRegisteredEvents.contains(event.getId()),
                                    event.getImagePath()  // NEW: Add image path from event
                            ));
                        }

                        updateInTableProgress(80);
                        if (sessionRepo != null && !rows.isEmpty()) {
                            List<UUID> ids = rows.stream().map(er -> er.eventId).collect(Collectors.toList());
                            Map<UUID, Integer> counts = sessionRepo.countByEventIds(ids);
                            for (EventRow er : rows) {
                                er.sessionCount = counts.getOrDefault(er.eventId, 0);
                            }
                        }

                        updateInTableProgress(95);
                        currentPage = safePage;
                        return rows;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return Collections.<EventRow>emptyList();
                    }
                },
                rows -> {
                    updateInTableProgress(100);
                    currentPageCache = rows;
                    eventsTable.setItems(FXCollections.observableArrayList(rows));
                    updatePaginationInfo();
                    showTable();
                },
                error -> {
                    showTable();
                    showAlert("Error", "Failed to load events: " + error.getMessage());
                }
        );
    }

    /**
     * Pre-load user registrations in single query
     */
    private void preloadUserRegistrations() {
        try {
            if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null) {
                Attendee attendee = (Attendee) appContext.currentUser;
                List<Ticket> userTickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                for (Ticket ticket : userTickets) {
                    if (ticket.getEventId() != null) {
                        userRegisteredEvents.add(ticket.getEventId());
                    }
                }
                System.out.println("✓ Pre-loaded " + userRegisteredEvents.size() + " registered events");
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not pre-load registrations");
        }
    }

    /**
     * Update progress in table placeholder
     * @param percent Progress percentage (0-100)
     */
    private void updateInTableProgress(int percent) {
        javafx.application.Platform.runLater(() -> {
            if (loadingProgressBar != null) {
                double progress = Math.min(100, Math.max(0, percent)) / 100.0;
                loadingProgressBar.setProgress(progress);
            }
            if (loadingPercentLabel != null) {
                loadingPercentLabel.setText(percent + "%");
            }
        });
    }

    /**
     * Show loading placeholder, hide table
     */
    private void showLoadingPlaceholder() {
        javafx.application.Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(true);
                loadingPlaceholder.setManaged(true);
            }
            if (eventsTable != null) {
                eventsTable.setVisible(false);
                eventsTable.setManaged(false);
            }
        });
    }

    /**
     * Hide loading placeholder, show table
     */
    private void showTable() {
        javafx.application.Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(false);
                loadingPlaceholder.setManaged(false);
            }
            if (eventsTable != null) {
                eventsTable.setVisible(true);
                eventsTable.setManaged(true);
            }
        });
    }

    /**
     * Update pagination info
     */
    private void updatePaginationInfo() {
        if (totalEvents == 0) {
            recordCountLabel.setText("No events found");
            return;
        }
        int start = currentPage * ITEMS_PER_PAGE + 1;
        int end = (int) Math.min((currentPage + 1L) * ITEMS_PER_PAGE, totalEvents);
        recordCountLabel.setText(String.format("Events %d-%d of %d (Page %d/%d)",
                start, end, totalEvents, totalPages == 0 ? 0 : currentPage + 1, totalPages));
    }

    /**
     * Apply filters and reset to page 1
     */
    private void applyFiltersAndReset() {
        currentPage = 0;
        applyFiltersWithPagination();
    }

    /**
     * Apply filters with pagination
     */
    private void applyFiltersWithPagination() {
        try {
            if (eventRepo == null) return;

            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();

            // Simple approach: filter from full list (findAllOptimized), then paginate IDs via memory
            List<Event> base = eventRepo.findAll();
            List<Event> filtered = base.stream()
                    .filter(e -> "ALL".equals(typeFilter) || e.getType().name().equals(typeFilter))
                    .filter(e -> "ALL".equals(statusFilter) || e.getStatus().name().equals(statusFilter))
                    .filter(e -> searchTerm.isEmpty() ||
                            e.getName().toLowerCase().contains(searchTerm) ||
                            e.getLocation().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());

            List<UUID> filteredIds = filtered.stream().map(Event::getId).collect(Collectors.toList());

            // Load first page of this filtered result
            loadEventsPageAsync(0, filteredIds);

        } catch (Exception e) {
            showAlert("Error", "Filter failed: " + e.getMessage());
        }
    }

    @FXML
    public void onSearch() {
        applyFiltersAndReset();
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        currentPage = 0;
        loadEventsPageAsync(0, null);
    }

    @FXML
    public void onViewDetails() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }
        showEventDetailsWithImage(selected);
    }

    @FXML
    public void onBuyTicket() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        try {
            List<Ticket> availableTickets = new ArrayList<>();
            if (appContext.ticketRepo != null) { // sửa cú pháp if
                List<Ticket> allTickets = appContext.ticketRepo.findByEvent(selected.eventId);
                for (Ticket ticket : allTickets) {
                    if (ticket.getAttendeeId() == null) {
                        availableTickets.add(ticket);
                    }
                }
            }

            if (availableTickets.isEmpty()) {
                showAlert("Info", "No available tickets");
                return;
            }

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Buy Ticket");
            dialog.setHeaderText("Select ticket for: " + selected.name);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            Label infoLabel = new Label("Available Tickets:");
            ComboBox<String> ticketCombo = new ComboBox<>();
            List<String> ticketDisplay = new ArrayList<>();
            Map<String, Ticket> ticketMap = new HashMap<>();

            for (Ticket ticket : availableTickets) {
                String display = ticket.getType().name() + " - $" + ticket.getPrice();
                ticketDisplay.add(display);
                ticketMap.put(display, ticket);
            }

            ticketCombo.setItems(FXCollections.observableArrayList(ticketDisplay));
            if (!ticketDisplay.isEmpty()) {
                ticketCombo.setValue(ticketDisplay.get(0));
            }
            ticketCombo.setPrefWidth(300);

            content.getChildren().addAll(infoLabel, ticketCombo);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String selectedTicket = ticketCombo.getValue();
                Ticket selectedTemplate = ticketMap.get(selectedTicket);

                if (selectedTemplate != null && appContext.currentUser instanceof Attendee attendee) {
                    Ticket newTicket = new Ticket();
                    newTicket.setId(UUID.randomUUID());
                    newTicket.setAttendeeId(attendee.getId());
                    newTicket.setEventId(selectedTemplate.getEventId());
                    // Note: sessionId removed - tickets are now event-level only
                    newTicket.setType(selectedTemplate.getType());
                    newTicket.setPrice(selectedTemplate.getPrice());
                    newTicket.setTicketStatus(TicketStatus.ACTIVE);
                    newTicket.setPaymentStatus(PaymentStatus.PAID);
                    newTicket.setQrCodeData("QR-" + newTicket.getId().toString().substring(0, 12).toUpperCase());

                    if (appContext.ticketRepo != null) {
                        appContext.ticketRepo.save(newTicket);
                        showQRCodeDialog(newTicket, selected.name);
                        loadEventsPageAsync(currentPage, null);
                    }
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Error buying ticket: " + e.getMessage());
        }
    }

    private void showQRCodeDialog(Ticket ticket, String eventName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket Confirmation");
        dialog.setHeaderText("Successfully purchased!");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        Label titleLabel = new Label("Event: " + eventName);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label typeLabel = new Label("Ticket Type: " + ticket.getType().name());
        Label priceLabel = new Label("Price: $" + ticket.getPrice());
        Label qrCodeLabel = new Label(ticket.getQrCodeData());
        qrCodeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-padding: 15;");

        content.getChildren().addAll(titleLabel, new Separator(), typeLabel, priceLabel, qrCodeLabel);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void onViewSessions() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        try {
            List<Session> sessions = sessionCache.getOrDefault(selected.eventId, new ArrayList<>());

            if (sessions.isEmpty() && sessionRepo != null) {
                sessions = sessionRepo.findByEvent(selected.eventId);
                sessionCache.put(selected.eventId, sessions);
            }

            if (sessions.isEmpty()) {
                showAlert("Info", "No sessions found");
                return;
            }

            StringBuilder sessionInfo = new StringBuilder("Sessions for " + selected.name + ":\n\n");
            for (Session session : sessions) {
                sessionInfo.append("• ").append(session.getTitle()).append("\n");
                sessionInfo.append("  Time: ").append(session.getStart()).append(" - ").append(session.getEnd()).append("\n");
                sessionInfo.append("  Venue: ").append(session.getVenue()).append("\n");
                sessionInfo.append("  Capacity: ").append(session.getCapacity()).append("\n");

                if (presenterRepo != null && session.getPresenterIds() != null && !session.getPresenterIds().isEmpty()) {
                    sessionInfo.append("  Presenters: ");
                    List<String> names = new ArrayList<>();
                    for (UUID pid : session.getPresenterIds()) {
                        Presenter p = presenterCache.get(pid);
                        if (p == null) {
                            try {
                                p = presenterRepo.findById(pid);
                                if (p != null) presenterCache.put(pid, p);
                            } catch (Exception ex) {
                                System.err.println("Error loading presenter: " + ex.getMessage());
                            }
                        }
                        if (p != null) names.add(p.getFullName());
                    }
                    sessionInfo.append(names.isEmpty() ? "No presenters" : String.join(", ", names)).append("\n");
                } else {
                    sessionInfo.append("  Presenters: None\n");
                }
                sessionInfo.append("\n");
            }

            showAlert("Sessions", sessionInfo.toString());
        } catch (Exception e) {
            showAlert("Error", "Error loading sessions: " + e.getMessage());
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage < totalPages - 1) {
            loadEventsPageAsync(currentPage + 1, null);
        }
    }

    @FXML
    public void onPreviousPage() {
        if (currentPage > 0) {
            loadEventsPageAsync(currentPage - 1, null);
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }

    /**
     * Load and cache image from file path or R2 URL
     * @param imagePath Path to image file or R2 URL
     * @return Image object or null if failed
     */
    private Image loadImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        try {
            // Check if it's R2 URL (Cloudflare - both endpoints)
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                System.out.println("Loading image from R2: " + imagePath);
                // Use async loading for remote URLs
                return new Image(imagePath, true);  // true = async loading
            }
            // Otherwise treat as local file
            else {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    return new Image(new FileInputStream(imageFile));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load image: " + imagePath + " -> " + e.getMessage());
        }
        return null;
    }

    /**
     * Show event details dialog with image (async loading for speed)
     * @param selected Selected EventRow
     */
    private void showEventDetailsWithImage(EventRow selected) {
        try {
            // Show loading dialog immediately
            Dialog<Void> loadingDialog = new Dialog<>();
            loadingDialog.setTitle("Event Details");
            loadingDialog.setHeaderText("Loading...");
            VBox loadingContent = new VBox(10);
            loadingContent.setStyle("-fx-alignment: center; -fx-padding: 50;");
            loadingContent.getChildren().add(new Label("Loading event details..."));
            loadingDialog.getDialogPane().setContent(loadingContent);
            loadingDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

            // Load event details asynchronously
            AsyncTaskService.runAsync(
                    () -> {
                        // Check cache first
                        Event event = eventCache.get(selected.eventId);
                        if (event == null) {
                            event = eventRepo.findById(selected.eventId);
                            if (event != null) {
                                eventCache.put(selected.eventId, event);
                            }
                        }
                        return event;
                    },
                    event -> {
                        loadingDialog.close();
                        if (event != null) {
                            showEventDetailsDialog(event, selected);
                        } else {
                            showAlert("Error", "Event not found");
                        }
                    },
                    error -> {
                        loadingDialog.close();
                        showAlert("Error", "Failed to load event: " + error.getMessage());
                    }
            );

            loadingDialog.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to load event details: " + e.getMessage());
        }
    }

    /**
     * Show event details dialog (called after async load)
     * @param event The event object (already loaded)
     * @param selected The EventRow for display info
     */
    private void showEventDetailsDialog(Event event, EventRow selected) {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Event Details");
            dialog.setHeaderText(selected.name);

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-font-size: 12;");

            // Event image section (load async in background)
            VBox imageSection = new VBox(10);
            imageSection.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10; -fx-border-radius: 5;");

            if (event.getImagePath() != null && !event.getImagePath().isEmpty()) {
                // Check cache first
                Image cachedImage = imageCache.get(event.getId());
                if (cachedImage != null) {
                    addImageToSection(imageSection, cachedImage);
                } else {
                    // Load image asynchronously in background
                    Label loadingLabel = new Label("📷 Loading image...");
                    loadingLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                    imageSection.getChildren().add(loadingLabel);

                    AsyncTaskService.runAsync(
                            () -> loadImage(event.getImagePath()),
                            loadedImage -> {
                                if (loadedImage != null) {
                                    imageCache.put(event.getId(), loadedImage);
                                    imageSection.getChildren().clear();
                                    addImageToSection(imageSection, loadedImage);
                                } else {
                                    imageSection.getChildren().clear();
                                    Label noImageLabel = new Label("❌ Image not available");
                                    noImageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                                    imageSection.getChildren().add(noImageLabel);
                                }
                            },
                            error -> {
                                imageSection.getChildren().clear();
                                Label errorLabel = new Label("❌ Failed to load image");
                                errorLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");
                                imageSection.getChildren().add(errorLabel);
                            }
                    );
                }
            } else {
                Label noImageLabel = new Label("📷 No image for this event");
                noImageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                imageSection.getChildren().add(noImageLabel);
            }

            // Event details section
            VBox detailsSection = new VBox(10);
            detailsSection.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

            Label typeLabel = new Label("🏷️  Type: " + selected.type);
            Label locationLabel = new Label("📍 Location: " + selected.location);
            Label startDateLabel = new Label("📅 Start Date: " + selected.startDate);
            Label endDateLabel = new Label("📅 End Date: " + selected.endDate);
            Label statusLabel = new Label("⚡ Status: " + selected.status);
            Label sessionsLabel = new Label("🎤 Sessions: " + selected.sessionCount);

            // Style labels
            for (Label label : Arrays.asList(typeLabel, locationLabel, startDateLabel, endDateLabel, statusLabel, sessionsLabel)) {
                label.setStyle("-fx-font-size: 12; -fx-padding: 5;");
                label.setWrapText(true);
            }

            detailsSection.getChildren().addAll(typeLabel, locationLabel, startDateLabel, endDateLabel, statusLabel, sessionsLabel);

            // Description section (if available)
            if (event.getName() != null && !event.getName().isEmpty()) {
                Label descLabel = new Label("ℹ️  About: " + event.getName());
                descLabel.setStyle("-fx-font-size: 12; -fx-padding: 5; -fx-wrap-text: true;");
                descLabel.setWrapText(true);
                detailsSection.getChildren().add(new Separator());
                detailsSection.getChildren().add(descLabel);
            }

            // Registration status
            if (selected.isRegistered) {
                Label registeredLabel = new Label("✅ You are registered for this event");
                registeredLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #27ae60; -fx-padding: 10; -fx-background-color: #e8f8f5; -fx-border-radius: 3;");
                detailsSection.getChildren().add(new Separator());
                detailsSection.getChildren().add(registeredLabel);
            }

            // Add all sections to main content
            content.getChildren().addAll(
                    new Label("Event Image:") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 13;"); }},
                    imageSection,
                    new Label("Event Information:") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 13;"); }},
                    detailsSection
            );

            // Create scrollable content
            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefWidth(600);
            scrollPane.setPrefHeight(600);

            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to show event details: " + e.getMessage());
        }
    }

    /**
     * Helper method to add image to section
     */
    private void addImageToSection(VBox section, Image image) {
        section.getChildren().clear();
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(400);
        imageView.setFitHeight(250);
        imageView.setPreserveRatio(true);
        imageView.setStyle("-fx-border-color: #999999; -fx-border-width: 1;");
        section.getChildren().add(imageView);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Helper class for event display
     */
    public static class EventRow {
        public UUID eventId;
        public String name;
        public String type;
        public String location;
        public String startDate;
        public String endDate;
        public String status;
        public int sessionCount;
        public boolean isRegistered;
        public String imagePath;  // NEW: Image URL from R2

        public EventRow(UUID eventId, String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount, boolean isRegistered) {
            this(eventId, name, type, location, startDate, endDate, status, sessionCount, isRegistered, null);
        }

        public EventRow(UUID eventId, String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount, boolean isRegistered, String imagePath) {
            this.eventId = eventId;
            this.name = name;
            this.type = type;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.sessionCount = sessionCount;
            this.isRegistered = isRegistered;
            this.imagePath = imagePath;
        }
    }
}

