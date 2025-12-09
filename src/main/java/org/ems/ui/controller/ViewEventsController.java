package org.ems.ui.controller;

import javafx.application.Platform;
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
    private final Map<UUID, Event> eventCache = new HashMap<>();  // Cache all events
    private List<Event> cachedAllEvents = null;  // OPTIMIZED: Cache findAll() result
    private List<UUID> cachedFilteredIds = null;  // OPTIMIZED: Cache filtered IDs
    private final Set<UUID> userRegisteredEvents = new HashSet<>();
    private List<EventRow> currentPageCache = new ArrayList<>();

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("⚙️ [ViewEvents] initialize() starting...");
        try {
            long appStart = System.currentTimeMillis();
            appContext = AppContext.get();
            eventRepo = appContext.eventRepo;
            sessionRepo = appContext.sessionRepo;
            presenterRepo = appContext.presenterRepo;
            imageService = appContext.imageService;
            System.out.println("  ✓ AppContext initialized in " + (System.currentTimeMillis() - appStart) + " ms");

            long comboStart = System.currentTimeMillis();
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");
            System.out.println("  ✓ Combos initialized in " + (System.currentTimeMillis() - comboStart) + " ms");

            long colStart = System.currentTimeMillis();
            setupTableColumns();
            System.out.println("  ✓ Table columns setup in " + (System.currentTimeMillis() - colStart) + " ms");

            long eventStart = System.currentTimeMillis();
            typeFilterCombo.setOnAction(e -> applyFiltersAndReset());
            statusFilterCombo.setOnAction(e -> applyFiltersAndReset());
            searchField.setOnAction(e -> applyFiltersAndReset());
            System.out.println("  ✓ Event listeners setup in " + (System.currentTimeMillis() - eventStart) + " ms");

            System.out.println("  ✓ About to load initial data...");
            long initialLoadStart = System.currentTimeMillis();
            loadEventsPageAsync(0, null);
            System.out.println("  ✓ Initial load async started in " + (System.currentTimeMillis() - initialLoadStart) + " ms");

            System.out.println("✓ Dashboard loaded successfully");
            System.out.println("✓ initialize() completed in " + (System.currentTimeMillis() - initStart) + " ms");
        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to initialize: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        long colStart = System.currentTimeMillis();
        System.out.println("📋 [ViewEvents] setupTableColumns() starting...");
        try {
            // Columns are defined in FXML in order:
            // 0: Image, 1: Event Name, 2: Type, 3: Location, 4: Start Date, 5: End Date, 6: Status, 7: Sessions, 8: Registered
            if (eventsTable == null) return;
            var columns = eventsTable.getColumns();
            if (columns.size() < 9) return;

            // OPTIMIZED: Image column - Load async and display thumbnail
            long imgColStart = System.currentTimeMillis();
            TableColumn<EventRow, String> imageCol = (TableColumn<EventRow, String>) columns.get(0);
            imageCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().imagePath));
            imageCol.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String imagePath, boolean empty) {
                    super.updateItem(imagePath, empty);
                    if (empty || imagePath == null || imagePath.isEmpty()) {
                        setText(null);
                        setGraphic(null);
                        return;
                    }

                    try {
                        EventRow row = getTableView().getItems().get(getIndex());

                        // Check cache first
                        Image cachedImage = imageCache.get(row.eventId);
                        if (cachedImage != null) {
                            displayImage(cachedImage);
                            return;
                        }

                        // Show loading placeholder
                        Label loadingLabel = new Label("⏳");
                        loadingLabel.setStyle("-fx-font-size: 20; -fx-padding: 5;");
                        setGraphic(loadingLabel);
                        setText(null);

                        // Load image async in background
                        AsyncTaskService.runAsync(
                                () -> {
                                    try {
                                        return loadImage(imagePath);
                                    } catch (Exception e) {
                                        System.err.println("Error loading image: " + e.getMessage());
                                        return null;
                                    }
                                },
                                loadedImage -> {
                                    if (loadedImage != null) {
                                        imageCache.put(row.eventId, loadedImage);
                                        // Only update if still visible
                                        if (getIndex() >= 0 && getIndex() < getTableView().getItems().size()) {
                                            if (getTableView().getItems().get(getIndex()).eventId.equals(row.eventId)) {
                                                displayImage(loadedImage);
                                            }
                                        }
                                    } else {
                                        setText("❌");
                                        setGraphic(null);
                                    }
                                },
                                error -> {
                                    setText("❌");
                                    setGraphic(null);
                                }
                        );
                    } catch (Exception e) {
                        setText("❌");
                        setGraphic(null);
                    }
                }

                private void displayImage(Image image) {
                    if (image == null) {
                        setText("No Image");
                        setGraphic(null);
                        return;
                    }

                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(80);
                    imageView.setFitHeight(60);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle("-fx-border-color: #ddd; -fx-border-width: 1;");
                    setGraphic(imageView);
                    setText(null);
                }
            });
            System.out.println("  ✓ Image column setup in " + (System.currentTimeMillis() - imgColStart) + " ms");

            // ...existing code...
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

            System.out.println("  ✓ All table columns setup in " + (System.currentTimeMillis() - colStart) + " ms");
        } catch (Exception e) {
            System.err.println("✗ setupTableColumns() failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Async load events for a given page from DB using LIMIT/OFFSET.
     * If filteredIds != null, we page over that in-memory ID list instead.
     * OPTIMIZED: All DB operations on background thread
     */
    private void loadEventsPageAsync(int page, List<UUID> filteredIds) {
        long pageStart = System.currentTimeMillis();
        System.out.println("📄 [ViewEvents] loadEventsPageAsync(page=" + page + ", filteredIds=" +
                (filteredIds != null ? filteredIds.size() : "null") + ") starting...");

        showLoadingPlaceholder();
        updateInTableProgress(0);
        System.out.println("  ✓ Loading placeholder shown");

        System.out.println("  ✓ Starting background task...");
        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background Thread] Task executing...");
                    try {
                        if (eventRepo == null) {
                            System.err.println("    ✗ eventRepo is null!");
                            return Collections.<EventRow>emptyList();
                        }

                        long countStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Counting events...");
                        if (filteredIds == null) {
                            totalEvents = eventRepo.count();
                        } else {
                            totalEvents = filteredIds.size();
                        }
                        long countTime = System.currentTimeMillis() - countStart;
                        System.out.println("    ✓ Count took " + countTime + " ms: " + totalEvents + " events");

                        totalPages = totalEvents == 0 ? 0 : (int) Math.ceil((double) totalEvents / ITEMS_PER_PAGE);
                        if (totalPages == 0) {
                            System.out.println("    ⚠️ No pages");
                            return Collections.<EventRow>emptyList();
                        }

                        int safePage = Math.max(0, Math.min(page, totalPages - 1));
                        int offset = safePage * ITEMS_PER_PAGE;

                        long loadStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Loading events...");
                        List<Event> events;
                        if (filteredIds == null) {
                            events = eventRepo.findPage(offset, ITEMS_PER_PAGE);
                            System.out.println("    ✓ findPage() took " + (System.currentTimeMillis() - loadStart) + " ms: " + events.size() + " events");
                        } else {
                            int end = Math.min(offset + ITEMS_PER_PAGE, filteredIds.size());
                            List<UUID> pageIds = filteredIds.subList(offset, end);
                            events = new ArrayList<>();
                            for (UUID id : pageIds) {
                                Event e = eventRepo.findById(id);
                                if (e != null) events.add(e);
                            }
                            System.out.println("    ✓ Loaded " + events.size() + " events from filteredIds in " + (System.currentTimeMillis() - loadStart) + " ms");
                        }

                        updateInTableProgress(40);

                        long regStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Preloading registrations...");
                        preloadUserRegistrations();
                        System.out.println("    ✓ preloadUserRegistrations() in " + (System.currentTimeMillis() - regStart) + " ms");

                        updateInTableProgress(60);

                        long rowStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Building rows...");
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
                                    event.getImagePath()
                            ));
                        }
                        System.out.println("    ✓ Built " + rows.size() + " rows in " + (System.currentTimeMillis() - rowStart) + " ms");

                        updateInTableProgress(80);

                        long sessionStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Loading session counts...");
                        if (sessionRepo != null && !rows.isEmpty()) {
                            List<UUID> ids = rows.stream().map(er -> er.eventId).collect(Collectors.toList());
                            Map<UUID, Integer> counts = sessionRepo.countByEventIds(ids);
                            for (EventRow er : rows) {
                                er.sessionCount = counts.getOrDefault(er.eventId, 0);
                            }
                        }
                        System.out.println("    ✓ Session counts loaded in " + (System.currentTimeMillis() - sessionStart) + " ms");

                        updateInTableProgress(95);
                        currentPage = safePage;

                        long taskTime = System.currentTimeMillis() - taskStart;
                        System.out.println("    ✓ Background task completed in " + taskTime + " ms");
                        return rows;

                    } catch (Exception ex) {
                        long failTime = System.currentTimeMillis() - taskStart;
                        System.err.println("    ✗ Background task failed in " + failTime + " ms");
                        ex.printStackTrace();
                        return Collections.<EventRow>emptyList();
                    }
                },
                rows -> {
                    long uiStart = System.currentTimeMillis();
                    System.out.println("  ✓ Background task returned, updating UI...");
                    updateInTableProgress(100);
                    currentPageCache = rows;
                    System.out.println("    🔄 Setting table items (" + rows.size() + " rows)...");
                    eventsTable.setItems(FXCollections.observableArrayList(rows));
                    System.out.println("    ✓ Table items set in " + (System.currentTimeMillis() - uiStart) + " ms");

                    long paginationTime = System.currentTimeMillis();
                    updatePaginationInfo();
                    System.out.println("    ✓ Pagination updated in " + (System.currentTimeMillis() - paginationTime) + " ms");

                    long showTime = System.currentTimeMillis();
                    showTable();
                    System.out.println("    ✓ Table shown in " + (System.currentTimeMillis() - showTime) + " ms");

                    long totalUiTime = System.currentTimeMillis() - uiStart;
                    System.out.println("  ✓ UI update completed in " + totalUiTime + " ms");
                    System.out.println("✓ loadEventsPageAsync() completed in " + (System.currentTimeMillis() - pageStart) + " ms");
                },
                error -> {
                    long errorTime = System.currentTimeMillis() - pageStart;
                    System.err.println("✗ loadEventsPageAsync() failed in " + errorTime + " ms");
                    System.err.println("  Error: " + error.getMessage());
                    error.printStackTrace();
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
        System.out.println("🔄 [ViewEvents] applyFiltersAndReset() called");
        currentPage = 0;
        // Move to background immediately
        Platform.runLater(this::applyFiltersWithPagination);
    }

    /**
     * Apply filters with pagination - OPTIMIZED: Cache all events, filter in-memory on background thread
     */
    private void applyFiltersWithPagination() {
        long filterStart = System.currentTimeMillis();
        System.out.println("🔍 [ViewEvents] applyFiltersWithPagination() starting...");

        try {
            if (eventRepo == null) {
                System.err.println("  ✗ eventRepo is null!");
                return;
            }

            showLoadingPlaceholder();
            updateInTableProgress(0);

            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();
            System.out.println("  Search: '" + searchTerm + "', Type: " + typeFilter + ", Status: " + statusFilter);

            // OPTIMIZED: Run filter on background thread to prevent UI freeze
            System.out.println("  🔄 Starting background filter task...");
            AsyncTaskService.runAsync(
                    () -> {
                        long bgStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Filter task executing...");

                        // Load all events ONCE and cache
                        if (cachedAllEvents == null) {
                            long allStart = System.currentTimeMillis();
                            System.out.println("    🔄 [Background Thread] Loading all events (first time)...");
                            cachedAllEvents = eventRepo.findAll();
                            long allTime = System.currentTimeMillis() - allStart;
                            System.out.println("    ✓ eventRepo.findAll() took " + allTime + " ms, loaded " + cachedAllEvents.size() + " events");
                        } else {
                            System.out.println("    ℹ Using cached all events (" + cachedAllEvents.size() + " events)");
                        }

                        // Filter in-memory (fast, no DB query)
                        long streamStart = System.currentTimeMillis();
                        System.out.println("    🔄 [Background Thread] Filtering events...");
                        List<Event> filtered = cachedAllEvents.stream()
                                .filter(e -> "ALL".equals(typeFilter) || e.getType().name().equals(typeFilter))
                                .filter(e -> "ALL".equals(statusFilter) || e.getStatus().name().equals(statusFilter))
                                .filter(e -> searchTerm.isEmpty() ||
                                        e.getName().toLowerCase().contains(searchTerm) ||
                                        e.getLocation().toLowerCase().contains(searchTerm))
                                .collect(Collectors.toList());

                        long streamTime = System.currentTimeMillis() - streamStart;
                        List<UUID> filteredIds = filtered.stream().map(Event::getId).collect(Collectors.toList());
                        System.out.println("    ✓ Stream filtering took " + streamTime + " ms, filtered to " + filteredIds.size() + " events");

                        long totalTime = System.currentTimeMillis() - bgStart;
                        System.out.println("    ✓ applyFiltersWithPagination background task took " + totalTime + " ms");

                        return filteredIds;
                    },
                    filteredIds -> {
                        System.out.println("  ✓ Filter task returned " + filteredIds.size() + " results");
                        System.out.println("  🔄 Loading first page...");
                        loadEventsPageAsync(0, filteredIds);
                    },
                    error -> {
                        System.err.println("  ✗ Filter task failed: " + error.getMessage());
                        showTable();
                        showAlert("Error", "Filter failed: " + error.getMessage());
                    }
            );
            System.out.println("  ✓ Background filter task started");

        } catch (Exception e) {
            System.err.println("✗ applyFiltersWithPagination exception: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Filter failed: " + e.getMessage());
        }
    }

    @FXML
    public void onSearch() {
        System.out.println("🔎 [ViewEvents] onSearch() called - starting filter");
        applyFiltersAndReset();
    }

    @FXML
    public void onReset() {
        System.out.println("🔄 [ViewEvents] onReset() called");
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        currentPage = 0;

        // Ensure async execution
        Platform.runLater(() -> {
            System.out.println("  🔄 Loading first page from reset...");
            loadEventsPageAsync(0, null);
        });
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
        long buyStart = System.currentTimeMillis();
        System.out.println("🛒 [ViewEvents] onBuyTicket() starting...");

        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        // Load tickets async to prevent UI freeze
        AsyncTaskService.runAsync(
                () -> {
                    long loadStart = System.currentTimeMillis();
                    try {
                        List<Ticket> availableTickets = new ArrayList<>();
                        if (appContext.ticketRepo != null) {
                            List<Ticket> allTickets = appContext.ticketRepo.findByEvent(selected.eventId);
                            System.out.println("  ✓ findByEvent() took " + (System.currentTimeMillis() - loadStart) + " ms");
                            for (Ticket ticket : allTickets) {
                                if (ticket.getAttendeeId() == null) {
                                    availableTickets.add(ticket);
                                }
                            }
                        }
                        return availableTickets;
                    } catch (Exception e) {
                        System.err.println("  ✗ Error loading tickets: " + e.getMessage());
                        return new ArrayList<>();
                    }
                },
                availableTickets -> {
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

                    @SuppressWarnings("unchecked")
                    List<Ticket> ticketList = (List<Ticket>) availableTickets;
                    for (Ticket ticket : ticketList) {
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
                            // Save ticket async
                            AsyncTaskService.runAsync(
                                    () -> {
                                        Ticket newTicket = new Ticket();
                                        newTicket.setId(UUID.randomUUID());
                                        newTicket.setAttendeeId(attendee.getId());
                                        newTicket.setEventId(selectedTemplate.getEventId());
                                        newTicket.setType(selectedTemplate.getType());
                                        newTicket.setPrice(selectedTemplate.getPrice());
                                        newTicket.setTicketStatus(TicketStatus.ACTIVE);
                                        newTicket.setPaymentStatus(PaymentStatus.PAID);
                                        newTicket.setQrCodeData("QR-" + newTicket.getId().toString().substring(0, 12).toUpperCase());

                                        if (appContext.ticketRepo != null) {
                                            appContext.ticketRepo.save(newTicket);
                                        }
                                        return newTicket;
                                    },
                                    newTicket -> {
                                        showQRCodeDialog(newTicket, selected.name);
                                        loadEventsPageAsync(currentPage, null);
                                    },
                                    error -> showAlert("Error", "Failed to save ticket: " + error.getMessage())
                            );
                        }
                    }

                    System.out.println("  ✓ onBuyTicket() completed in " + (System.currentTimeMillis() - buyStart) + " ms");
                },
                error -> showAlert("Error", "Error loading tickets: " + error.getMessage())
        );
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
        long sessStart = System.currentTimeMillis();
        System.out.println("🎤 [ViewEvents] onViewSessions() starting...");

        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event");
            return;
        }

        // Load sessions async to prevent UI freeze
        AsyncTaskService.runAsync(
                () -> {
                    long loadStart = System.currentTimeMillis();
                    try {
                        List<Session> sessions = sessionCache.getOrDefault(selected.eventId, new ArrayList<>());

                        if (sessions.isEmpty() && sessionRepo != null) {
                            long queryStart = System.currentTimeMillis();
                            sessions = sessionRepo.findByEvent(selected.eventId);
                            System.out.println("  ✓ sessionRepo.findByEvent() took " + (System.currentTimeMillis() - queryStart) + " ms");
                            sessionCache.put(selected.eventId, sessions);
                        }

                        if (sessions.isEmpty()) {
                            return "No sessions found";
                        }

                        // OPTIMIZED: Batch load all missing presenters at once
                        long presStart = System.currentTimeMillis();
                        Set<UUID> presenterIds = new HashSet<>();
                        for (Session session : sessions) {
                            if (session.getPresenterIds() != null) {
                                for (UUID pid : session.getPresenterIds()) {
                                    if (!presenterCache.containsKey(pid)) {
                                        presenterIds.add(pid);
                                    }
                                }
                            }
                        }

                        // Load missing presenters
                        if (!presenterIds.isEmpty() && presenterRepo != null) {
                            for (UUID pid : presenterIds) {
                                try {
                                    Presenter p = presenterRepo.findById(pid);
                                    if (p != null) {
                                        presenterCache.put(pid, p);
                                    }
                                } catch (Exception ignored) {}
                            }
                            System.out.println("  ✓ Batch loaded " + presenterIds.size() + " presenters in " + (System.currentTimeMillis() - presStart) + " ms");
                        }

                        // Build display string with cached presenters
                        StringBuilder sessionInfo = new StringBuilder("Sessions for " + selected.name + ":\n\n");
                        for (Session session : sessions) {
                            sessionInfo.append("• ").append(session.getTitle()).append("\n");
                            sessionInfo.append("  Time: ").append(session.getStart()).append(" - ").append(session.getEnd()).append("\n");
                            sessionInfo.append("  Venue: ").append(session.getVenue()).append("\n");
                            sessionInfo.append("  Capacity: ").append(session.getCapacity()).append("\n");

                            if (session.getPresenterIds() != null && !session.getPresenterIds().isEmpty()) {
                                sessionInfo.append("  Presenters: ");
                                List<String> names = new ArrayList<>();
                                for (UUID pid : session.getPresenterIds()) {
                                    Presenter p = presenterCache.get(pid);
                                    if (p != null) {
                                        names.add(p.getFullName());
                                    }
                                }
                                sessionInfo.append(names.isEmpty() ? "No presenters" : String.join(", ", names)).append("\n");
                            } else {
                                sessionInfo.append("  Presenters: None\n");
                            }
                            sessionInfo.append("\n");
                        }

                        System.out.println("  ✓ onViewSessions() prepared in " + (System.currentTimeMillis() - loadStart) + " ms");
                        return sessionInfo.toString();
                    } catch (Exception e) {
                        System.err.println("  ✗ Error loading sessions: " + e.getMessage());
                        return "Error: " + e.getMessage();
                    }
                },
                sessionInfo -> {
                    if (sessionInfo.startsWith("Error")) {
                        showAlert("Error", sessionInfo);
                    } else {
                        showAlert("Sessions", sessionInfo);
                    }
                    System.out.println("  ✓ onViewSessions() completed in " + (System.currentTimeMillis() - sessStart) + " ms");
                },
                error -> {
                    showAlert("Error", "Error loading sessions: " + error.getMessage());
                    System.err.println("  ✗ onViewSessions() failed: " + error.getMessage());
                }
        );
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

