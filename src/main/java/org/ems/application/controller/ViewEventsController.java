package org.ems.application.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import org.ems.application.dto.event.EventDetailsDTO;
import org.ems.application.dto.event.EventFilterCriteriaDTO;
import org.ems.application.dto.event.EventRowDTO;
import org.ems.application.dto.page.PagedResult;
import org.ems.application.dto.session.SessionViewDTO;
import org.ems.application.dto.ticket.TicketPurchaseRequestDTO;
import org.ems.application.service.event.EventDetailService;
import org.ems.application.service.event.EventImageService;
import org.ems.application.service.event.EventListingService;
import org.ems.application.service.session.SessionViewService;
import org.ems.application.service.ticket.TicketPurchaseService;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Ticket;
import org.ems.domain.repository.*;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;

import java.util.*;

/**
 * ViewEventsController - UI Controller for Event Browsing & Ticket Purchase
 *
 * Responsibilities:
 * - Handle FXML bindings
 * - Route user events to services
 * - Update UI with results
 *
 * Does NOT handle:
 * - Event filtering/pagination (→ EventListingService)
 * - Event detail loading (→ EventDetailService)
 * - Image loading (→ EventImageService)
 * - Session loading (→ SessionViewService)
 * - Ticket purchase (→ TicketPurchaseService)
 *
 * @author EMS Team
 */
public class ViewEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<EventRowDTO> eventsTable;
    @FXML private Label recordCountLabel;
    @FXML private VBox loadingPlaceholder;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingPercentLabel;

    // ===== DEPENDENCY INJECTION - Services =====
    private EventListingService listingService;
    private EventDetailService detailService;
    private EventImageService imageService;
    private SessionViewService sessionService;
    private TicketPurchaseService ticketService;

    // ===== REPOSITORIES & STATE =====
    private EventRepository eventRepo;
    private SessionRepository sessionRepo;
    private PresenterRepository presenterRepo;
    private AppContext appContext;
    private Set<UUID> userRegisteredEvents = new HashSet<>();
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 10;

    @FXML
    public void initialize() {
        long start = System.currentTimeMillis();
        System.out.println("⚙️ [ViewEventsController] initialize() starting...");

        try {
            // Setup dependencies
            appContext = AppContext.get();
            eventRepo = appContext.eventRepo;
            sessionRepo = appContext.sessionRepo;
            presenterRepo = appContext.presenterRepo;

            if (appContext == null) {
                throw new IllegalStateException("AppContext is null");
            }

            // Preload user registrations
            preloadUserRegistrations();

            // Initialize services
            listingService = new EventListingService(eventRepo, sessionRepo, userRegisteredEvents);
            detailService = new EventDetailService(eventRepo, sessionRepo, userRegisteredEvents);
            imageService = new EventImageService(appContext.imageService);
            sessionService = new SessionViewService(sessionRepo, presenterRepo);
            ticketService = new TicketPurchaseService(appContext.ticketRepo);

            System.out.println("✓ Services initialized");

            // Setup UI
            setupComboBoxes();
            setupTableColumns();
            setupEventListeners();

            System.out.println("✓ UI setup completed");

            // Load initial data
            Platform.runLater(this::loadEventsPage);

            System.out.println("✓ initialize() completed in " + (System.currentTimeMillis() - start) + " ms");

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to initialize: " + e.getMessage());
        }
    }

    /**
     * Setup filter combo boxes
     */
    private void setupComboBoxes() {
        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
        ));
        typeFilterCombo.setValue("ALL");

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
        ));
        statusFilterCombo.setValue("ALL");
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        var columns = eventsTable.getColumns();
        if (columns.size() < 9) return;

        // Image column with async loading and caching
        ((TableColumn<EventRowDTO, String>) columns.get(0)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().imagePath));

        ((TableColumn<EventRowDTO, String>) columns.get(0)).setCellFactory(col -> new TableCell<EventRowDTO, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(80);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);

                if (empty) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                EventRowDTO row = getTableView().getItems().get(getIndex());
                if (row == null) {
                    setGraphic(null);
                    setText("❌");
                    return;
                }

                // Check if image is cached
                Image cachedImage = imageService.getImageFromCache(row.eventId);
                if (cachedImage != null) {
                    imageView.setImage(cachedImage);
                    setGraphic(imageView);
                    setText(null);
                    return;
                }

                // If no image path, show placeholder
                if (imagePath == null || imagePath.isEmpty()) {
                    setText("No Image");
                    setGraphic(null);
                    return;
                }

                // Load image asynchronously
                setText(null);
                setGraphic(new Label("⏳"));

                AsyncTaskService.runAsync(
                        () -> imageService.loadImage(imagePath, row.eventId),
                        loadedImage -> {
                            if (loadedImage != null && isVisible()) {
                                // Update only if cell is still visible
                                Platform.runLater(() -> {
                                    imageView.setImage(loadedImage);
                                    setGraphic(imageView);
                                    setText(null);
                                });
                            } else if (!isVisible()) {
                                Platform.runLater(() -> {
                                    setText("❌");
                                    setGraphic(null);
                                });
                            }
                        },
                        error -> Platform.runLater(() -> {
                            setText("❌");
                            setGraphic(null);
                        })
                );
            }
        });

        // Other columns
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
        ((TableColumn<EventRowDTO, String>) columns.get(8)).setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().isRegistered ? "✓ Yes" : "No"));
    }

    /**
     * Setup event listeners
     */
    private void setupEventListeners() {
        typeFilterCombo.setOnAction(e -> loadEventsPage());
        statusFilterCombo.setOnAction(e -> loadEventsPage());
        searchField.setOnAction(e -> loadEventsPage());
    }

    /**
     * Preload user's registered events
     */
    private void preloadUserRegistrations() {
        try {
            if (appContext == null) {
                System.err.println("Warning: AppContext is null");
                return;
            }

            if (appContext.currentUser == null) {
                System.out.println("[ViewEventsController] No current user");
                return;
            }

            if (!(appContext.currentUser instanceof Attendee)) {
                System.out.println("[ViewEventsController] Current user is not an Attendee");
                return;
            }

            if (appContext.ticketRepo == null) {
                System.err.println("Warning: TicketRepo is null");
                return;
            }

            Attendee attendee = (Attendee) appContext.currentUser;
            List<Ticket> userTickets = appContext.ticketRepo.findByAttendee(attendee.getId());
            for (Ticket ticket : userTickets) {
                if (ticket.getEventId() != null) {
                    userRegisteredEvents.add(ticket.getEventId());
                }
            }
            System.out.println("[ViewEventsController] Preloaded " + userRegisteredEvents.size() + " registered events");
        } catch (Exception e) {
            System.err.println("Warning: Could not preload registrations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load events page with current filters
     */
    private void loadEventsPage() {
        currentPage = 0;
        Platform.runLater(this::loadEventsPageAsync);
    }

    /**
     * Load events asynchronously
     */
    private void loadEventsPageAsync() {
        long start = System.currentTimeMillis();
        System.out.println("📄 [ViewEventsController] loadEventsPageAsync() starting...");

        showLoadingPlaceholder();
        updateProgress(0);

        AsyncTaskService.runAsync(
                () -> {
                    EventFilterCriteriaDTO criteria = new EventFilterCriteriaDTO(
                            searchField.getText(),
                            typeFilterCombo.getValue(),
                            statusFilterCombo.getValue(),
                            currentPage,
                            ITEMS_PER_PAGE
                    );
                    return listingService.loadEventPage(criteria);
                },
                pagedResult -> {
                    updateProgress(100);
                    eventsTable.setItems(FXCollections.observableArrayList(pagedResult.getItems()));
                    updatePaginationInfo(pagedResult);
                    showTable();
                    System.out.println("✓ loadEventsPageAsync completed in " + (System.currentTimeMillis() - start) + " ms");
                },
                error -> {
                    System.err.println("✗ Error loading events: " + error.getMessage());
                    showTable();
                    showAlert("Error", "Failed to load events: " + error.getMessage());
                }
        );
    }

    /**
     * Update pagination info
     */
    private void updatePaginationInfo(PagedResult<?> pagedResult) {
        Platform.runLater(() -> {
            if (recordCountLabel == null) {
                System.err.println("Warning: recordCountLabel is null");
                return;
            }

            if (pagedResult == null) {
                recordCountLabel.setText("Error: No data");
                return;
            }

            if (pagedResult.getTotalItems() == 0) {
                recordCountLabel.setText("No events found");
                return;
            }

            try {
                int pageNum = pagedResult.getPage();
                long totalItems = pagedResult.getTotalItems();
                int totalPages = pagedResult.getTotalPages();

                int start = (pageNum - 1) * ITEMS_PER_PAGE + 1;
                int end = Math.min(pageNum * ITEMS_PER_PAGE, (int) totalItems);

                String text = String.format("Events %d-%d of %d (Page %d/%d)",
                        start, end, totalItems, pageNum, totalPages);
                recordCountLabel.setText(text);
                System.out.println("[ViewEventsController] " + text);

            } catch (Exception e) {
                System.err.println("Error updating pagination info: " + e.getMessage());
                recordCountLabel.setText("Events: " + pagedResult.getTotalItems());
            }
        });
    }

    /**
     * Update progress bar
     */
    private void updateProgress(int percent) {
        Platform.runLater(() -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(Math.min(100, Math.max(0, percent)) / 100.0);
            }
            if (loadingPercentLabel != null) {
                loadingPercentLabel.setText(percent + "%");
            }
        });
    }

    /**
     * Show/hide loading placeholder
     */
    private void showLoadingPlaceholder() {
        Platform.runLater(() -> {
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

    private void showTable() {
        Platform.runLater(() -> {
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

    @FXML
    public void onSearch() {
        loadEventsPage();
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        loadEventsPage();
    }

    @FXML
    public void onViewDetails() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        AsyncTaskService.runAsync(
                () -> detailService.loadEventDetails(selected.eventId),
                details -> {
                    if (details != null) {
                        showEventDetailsDialog(details, selected);
                    } else {
                        showAlert("Error", "Event not found");
                    }
                },
                error -> showAlert("Error", "Failed to load event: " + error.getMessage())
        );
    }

    /**
     * Show event details dialog
     */
    private void showEventDetailsDialog(EventDetailsDTO details, EventRowDTO selected) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Event Details");
        dialog.setHeaderText(selected.name);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Image section
        VBox imageSection = createImageSection(details);

        // Details section
        VBox detailsSection = new VBox(10);
        detailsSection.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");
        detailsSection.getChildren().addAll(
                createLabel("🏷️  Type: " + selected.type),
                createLabel("📍 Location: " + selected.location),
                createLabel("📅 Start Date: " + selected.startDate),
                createLabel("📅 End Date: " + selected.endDate),
                createLabel("⚡ Status: " + selected.status),
                createLabel("🎤 Sessions: " + selected.sessionCount)
        );

        if (selected.isRegistered) {
            detailsSection.getChildren().addAll(
                    new Separator(),
                    createLabel("✅ You are registered for this event")
            );
        }

        content.getChildren().addAll(
                createBoldLabel("Event Image:"),
                imageSection,
                createBoldLabel("Event Information:"),
                detailsSection
        );

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefWidth(600);
        scrollPane.setPrefHeight(600);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Create image section
     */
    private VBox createImageSection(EventDetailsDTO details) {
        VBox section = new VBox(10);
        section.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10; -fx-border-radius: 5;");

        if (details.imagePath != null && !details.imagePath.isEmpty()) {
            Image cachedImage = imageService.getImageFromCache(details.eventId);
            if (cachedImage != null) {
                addImageView(section, cachedImage);
            } else {
                Label loadingLabel = new Label("📷 Loading image...");
                loadingLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                section.getChildren().add(loadingLabel);

                AsyncTaskService.runAsync(
                        () -> imageService.loadImage(details.imagePath, details.eventId),
                        image -> {
                            section.getChildren().clear();
                            if (image != null) {
                                addImageView(section, image);
                            } else {
                                section.getChildren().add(new Label("❌ Image not available"));
                            }
                        },
                        error -> {
                            section.getChildren().clear();
                            section.getChildren().add(new Label("❌ Failed to load image"));
                        }
                );
            }
        } else {
            Label noImageLabel = new Label("📷 No image for this event");
            noImageLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
            section.getChildren().add(noImageLabel);
        }

        return section;
    }

    private void addImageView(VBox section, Image image) {
        ImageView iv = new ImageView(image);
        iv.setFitWidth(400);
        iv.setFitHeight(250);
        iv.setPreserveRatio(true);
        section.getChildren().add(iv);
    }

    @FXML
    public void onBuyTicket() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        AsyncTaskService.runAsync(
                () -> ticketService.loadAvailableTickets(selected.eventId),
                availableTickets -> showBuyTicketDialog(selected, availableTickets),
                error -> showAlert("Error", "Error loading tickets: " + error.getMessage())
        );
    }

    /**
     * Show ticket purchase dialog
     */
    private void showBuyTicketDialog(EventRowDTO event, List<Ticket> availableTickets) {
        if (availableTickets.isEmpty()) {
            showAlert("Info", "No available tickets");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Buy Ticket");
        dialog.setHeaderText("Select ticket for: " + event.name);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        ComboBox<String> ticketCombo = new ComboBox<>();
        Map<String, Ticket> ticketMap = new HashMap<>();

        for (Ticket ticket : availableTickets) {
            String display = ticket.getType().name() + " - $" + ticket.getPrice();
            ticketMap.put(display, ticket);
        }

        ticketCombo.setItems(FXCollections.observableArrayList(ticketMap.keySet()));
        if (!ticketMap.isEmpty()) {
            ticketCombo.setValue(ticketMap.keySet().iterator().next());
        }

        content.getChildren().addAll(new Label("Available Tickets:"), ticketCombo);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String selectedDisplay = ticketCombo.getValue();
            Ticket selectedTemplate = ticketMap.get(selectedDisplay);

            if (selectedTemplate != null && appContext.currentUser instanceof Attendee attendee) {
                AsyncTaskService.runAsync(
                        () -> {
                            TicketPurchaseRequestDTO request = new TicketPurchaseRequestDTO(
                                    selectedTemplate.getEventId(),
                                    attendee.getId(),
                                    selectedTemplate.getId(),
                                    selectedTemplate.getType().name(),
                                    selectedTemplate.getPrice()
                            );
                            return ticketService.purchaseTicket(request, attendee);
                        },
                        newTicket -> {
                            showQRCodeDialog(newTicket, event.name);
                            loadEventsPageAsync();
                        },
                        error -> showAlert("Error", "Failed to save ticket: " + error.getMessage())
                );
            }
        }
    }

    /**
     * Show QR code confirmation
     */
    private void showQRCodeDialog(Ticket ticket, String eventName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket Confirmation");
        dialog.setHeaderText("Successfully purchased!");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
                createBoldLabel("Event: " + eventName),
                new Separator(),
                createLabel("Ticket Type: " + ticket.getType().name()),
                createLabel("Price: $" + ticket.getPrice()),
                createBoldLabel("QR Code: " + ticket.getQrCodeData())
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void onViewSessions() {
        EventRowDTO selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        AsyncTaskService.runAsync(
                () -> sessionService.loadSessions(selected.eventId),
                sessions -> {
                    if (sessions.isEmpty()) {
                        showAlert("Info", "No sessions for this event");
                    } else {
                        showSessionsDialog(selected, sessions);
                    }
                },
                error -> showAlert("Error", "Error loading sessions: " + error.getMessage())
        );
    }

    /**
     * Show sessions dialog
     */
    private void showSessionsDialog(EventRowDTO event, List<SessionViewDTO> sessions) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sessions");
        dialog.setHeaderText("Sessions for " + event.name);

        StringBuilder info = new StringBuilder();
        for (SessionViewDTO session : sessions) {
            info.append("• ").append(session.title).append("\n");
            info.append("  Time: ").append(session.startTime).append(" - ").append(session.endTime).append("\n");
            info.append("  Venue: ").append(session.venue).append(" (Capacity: ").append(session.capacity).append(")\n");
            info.append("  Presenters: ").append(
                    session.presenterNames.isEmpty() ? "None" : String.join(", ", session.presenterNames)
            ).append("\n\n");
        }

        TextArea textArea = new TextArea(info.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefHeight(400);

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void onNextPage() {
        currentPage++;
        loadEventsPageAsync();
    }

    @FXML
    public void onPreviousPage() {
        if (currentPage > 0) {
            currentPage--;
            loadEventsPageAsync();
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }

    /**
     * Helper: Create label
     */
    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12; -fx-padding: 5; -fx-wrap-text: true;");
        label.setWrapText(true);
        return label;
    }

    /**
     * Helper: Create bold label
     */
    private Label createBoldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
        return label;
    }

    /**
     * Helper: Show alert
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

