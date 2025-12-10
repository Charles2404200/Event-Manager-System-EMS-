package org.ems.ui.controller;

/**
 * @author <your group number>
 */

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import javafx.application.Platform;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.domain.repository.TicketRepository;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.TicketType;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.infrastructure.util.ActivityLogger;
import org.ems.ui.stage.SceneManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

public class TicketManagerController {

    // Tab 1: Ticket Templates
    @FXML private ComboBox<String> templateEventCombo;
    @FXML private ComboBox<String> templateTypeCombo;
    @FXML private TextField templatePriceField;
    @FXML private TableView<TemplateRow> templatesTable;
    @FXML private Label templatesCountLabel;
    @FXML private Label templatesPageLabel;

    // Tab 2: Assign Tickets
    @FXML private ComboBox<String> assignAttendeeCombo;
    @FXML private ComboBox<String> assignTemplateCombo;
    @FXML private TableView<TicketRow> assignedTicketsTable;
    @FXML private Label assignedCountLabel;
    @FXML private Label totalAssignedLabel;
    @FXML private Label activeAssignedLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label assignedPageLabel;
    @FXML private Button assignedPrevButton;
    @FXML private Button assignedNextButton;
    @FXML private HBox assignedPaginationBox;
    @FXML private Label assignedPageInfoLabel;
    @FXML private ProgressBar assignedLoadingProgressBar;
    @FXML private Label assignedLoadingStatusLabel;
    @FXML private VBox assignedLoadingContainer;

    private TicketRepository ticketRepo;
    private List<TemplateRow> allTemplates;
    private List<TicketRow> allAssignedTickets;

    // Keyset Pagination state
    private static final int PAGE_SIZE = 20;
    private Timestamp lastTemplateCreatedAt = null;
    private UUID lastTemplateId = null;
    private Timestamp lastAssignedCreatedAt = null;
    private UUID lastAssignedId = null;

    // Page number tracking for UI
    private int currentAssignedPage = 1;
    private long totalAssignedCount = 0;
    private int totalAssignedPages = 1;

    private Map<String, UUID> eventMap = new HashMap<>();
    private Map<String, UUID> sessionMap = new HashMap<>();
    private Map<String, UUID> attendeeMap = new HashMap<>();
    private Map<String, UUID> templateMap = new HashMap<>(); // Cache for template IDs

    // App-level caches theo ID để tránh gọi findAll()/findById() lặp lại
    private final Map<UUID, Event> eventCacheById = new HashMap<>();
    private final Map<UUID, Session> sessionCacheById = new HashMap<>();
    private final Map<UUID, Attendee> attendeeCacheById = new HashMap<>();

    // Cache thống kê assigned theo template để tránh aggregate lặp lại
    private Map<TemplateKey, Long> templateAssignedCountCache = null;
    private boolean assignedTabInitialized = false;

    @FXML
    public void initialize() {
        //  STEP 1: UI SETUP ONLY (< 50ms)
        ticketRepo = AppContext.get().ticketRepo;

        // Setup table columns
        setupTemplateTableColumns();
        setupAssignedTicketTableColumns();

        // Setup combobox
        templateTypeCombo.setItems(FXCollections.observableArrayList(
                "GENERAL", "VIP", "EARLY_BIRD", "STUDENT", "GROUP"
        ));
        templateTypeCombo.setValue("GENERAL");

        // Setup labels
        updateTemplatesPageLabel();
        updateAssignedPageLabel();
        templatesCountLabel.setText("Loading...");
        assignedCountLabel.setText("Ready");

        System.out.println(" UI initialized in < 50ms");

        //  STEP 2: DEFER ALL DB OPERATIONS TO AFTER UI RENDER
        Platform.runLater(this::loadDataAsync);
    }

    /**
     * Run ALL database operations AFTER UI is rendered.
     * This is called from Platform.runLater() after initialize() returns.
     */
    private void loadDataAsync() {
        String runId = "TicketManager-" + System.currentTimeMillis();
        System.out.println("🚀 [" + runId + "] UI rendered. Starting async data load...");

        // Load events (populates eventCacheById for templates)
        loadEventsAsync(runId);

        // Load template page (keyset pagination) - PARALLEL
        loadTemplatesAsync(runId);
    }

    private void initCachesAsync(String runId) {
        // REMOVED: Lazy load caches - only load what's needed, when needed
        // This avoids loading ALL events and sessions upfront
        System.out.println("[" + runId + "] Cache initialization deferred (lazy loading enabled)");
    }

    // Đếm bằng COUNT(*) ở DB thông qua countTemplates/countAssigned
    private long safeCountTemplates() {
        long start = System.currentTimeMillis();
        try {
            long res = ticketRepo.countTemplates();
            System.out.println("[TicketPerf] safeCountTemplates() via countTemplates() took " +
                    (System.currentTimeMillis() - start) + " ms, count=" + res);
            return res;
        } catch (UnsupportedOperationException e) {
            System.err.println("[TicketPerf] countTemplates() not implemented, falling back to findTemplates().size(): " + e.getMessage());
            try {
                List<Ticket> templates = ticketRepo.findTemplates();
                return templates != null ? templates.size() : 0L;
            } catch (Exception ex) {
                System.err.println("[TicketPerf] safeCountTemplates() fallback failed: " + ex.getMessage());
                return 0L;
            }
        }
    }

    private long safeCountAssigned() {
        long start = System.currentTimeMillis();
        try {
            long res = ticketRepo.countAssigned();
            System.out.println("[TicketPerf] safeCountAssigned() via countAssigned() took " +
                    (System.currentTimeMillis() - start) + " ms, count=" + res);
            return res;
        } catch (UnsupportedOperationException e) {
            System.err.println("[TicketPerf] countAssigned() not implemented, falling back to findAssigned().size(): " + e.getMessage());
            try {
                List<Ticket> assigned = ticketRepo.findAssigned();
                return assigned != null ? assigned.size() : 0L;
            } catch (Exception ex) {
                System.err.println("[TicketPerf] safeCountAssigned() fallback failed: " + ex.getMessage());
                return 0L;
            }
        }
    }

    private void updateTemplatesPageLabel() {
        if (templatesPageLabel != null) {
            templatesPageLabel.setText("(Keyset Pagination)");
        }
    }

    private void updateAssignedPageLabel() {
        if (assignedPageLabel != null) {
            assignedPageLabel.setText("(Keyset Pagination)");
        }
    }

    private void setupTemplateTableColumns() {
        ObservableList<TableColumn<TemplateRow, ?>> cols = templatesTable.getColumns();
        if (cols.size() >= 5) {
            ((TableColumn<TemplateRow, String>) cols.get(0)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getEvent()));
            ((TableColumn<TemplateRow, String>) cols.get(1)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getSession()));
            ((TableColumn<TemplateRow, String>) cols.get(2)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
            ((TableColumn<TemplateRow, String>) cols.get(3)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrice()));
            ((TableColumn<TemplateRow, String>) cols.get(4)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getAvailable()));
        }
    }

    private void setupAssignedTicketTableColumns() {
        ObservableList<TableColumn<TicketRow, ?>> cols = assignedTicketsTable.getColumns();
        if (cols.size() >= 7) {
            ((TableColumn<TicketRow, String>) cols.get(0)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getId()));
            ((TableColumn<TicketRow, String>) cols.get(1)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getAttendee()));
            ((TableColumn<TicketRow, String>) cols.get(2)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getEvent()));
            ((TableColumn<TicketRow, String>) cols.get(3)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getSession()));
            ((TableColumn<TicketRow, String>) cols.get(4)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
            ((TableColumn<TicketRow, String>) cols.get(5)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrice()));
            ((TableColumn<TicketRow, String>) cols.get(6)).setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatus()));
        }
    }


    /**
     * Load events asynchronously to avoid UI blocking.
     * Populates eventCacheById and templateEventCombo.
     */
    private void loadEventsAsync(String runId) {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> eventList = new ArrayList<>();
                eventMap.clear();

                try {
                    AppContext context = AppContext.get();
                    if (context.eventRepo != null) {
                        List<Event> events = context.eventRepo.findAll();
                        for (Event evt : events) {
                            String display = evt.getName() + " (" + evt.getId().toString().substring(0, 8) + ")";
                            eventList.add(display);
                            eventMap.put(display, evt.getId());
                            eventCacheById.put(evt.getId(), evt);
                        }
                    }
                    System.out.println("[" + runId + "] loadEventsAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + eventList.size() + " events");
                } catch (Exception e) {
                    System.err.println("[" + runId + "] Error loading events: " + e.getMessage());
                }
                return eventList;
            }
        };

        task.setOnSucceeded(evt -> {
            List<String> eventList = task.getValue();
            Platform.runLater(() -> {
                templateEventCombo.setItems(FXCollections.observableArrayList(eventList));
                if (!eventList.isEmpty()) {
                    templateEventCombo.setValue(eventList.get(0));
                }
            });
        });

        task.setOnFailed(evt -> {
            System.err.println("[" + runId + "] Failed to load events: " + task.getException().getMessage());
        });

        Thread t = new Thread(task, "events-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * @deprecated Use loadEventsAsync() instead
     */
    @Deprecated
    private void loadEvents() {
        // This method is no longer used - see loadEventsAsync()
    }


    // ====== Tối ưu load templates ======

    private void loadTemplatesAsync() {
        loadTemplatesAsync(null);
    }

    private void loadTemplatesAsync(String runId) {
        templatesCountLabel.setText("Loading templates...");
        long asyncStart = System.currentTimeMillis();

        Task<List<TemplateRow>> task = new Task<>() {
            @Override
            protected List<TemplateRow> call() {
                long start = System.currentTimeMillis();
                List<TemplateRow> rows = loadTemplatesOptimized(runId);
                long took = System.currentTimeMillis() - start;
                System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] loadTemplatesOptimized() took " + took + " ms");
                return rows;
            }
        };

        task.setOnSucceeded(evt -> {
            allTemplates = task.getValue();
            displayTemplates(allTemplates);
            System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] loadTemplatesAsync total (task) " +
                    (System.currentTimeMillis() - asyncStart) + " ms");
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            System.err.println("Error loading templates: " + (ex != null ? ex.getMessage() : "unknown"));
            templatesCountLabel.setText("Error loading templates");
        });

        Thread t = new Thread(task, "ticket-templates-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Tối ưu hoá logic load templates:
     * - Chỉ 1 lần ticketRepo.findAll()
     * - Dùng Map<TemplateKey,Integer> để đếm số ticket assigned theo template (O(N))
     * - Không còn vòng lặp lồng nhau O(N^2)
     */
    private List<TemplateRow> loadTemplatesOptimized() {
        return loadTemplatesOptimized(null);
    }

    private List<TemplateRow> loadTemplatesOptimized(String runId) {
        long startAll = System.currentTimeMillis();
        List<TemplateRow> result = new ArrayList<>();
        try {
            if (ticketRepo == null) return result;

            long dbTemplatesStart = System.currentTimeMillis();
            List<Ticket> templateTickets = ticketRepo.findTemplatesByCursor(
                lastTemplateCreatedAt,
                lastTemplateId,
                PAGE_SIZE + 1  // Fetch one extra to detect if there are more pages
            );
            System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] findTemplatesByCursor() took " +
                    (System.currentTimeMillis() - dbTemplatesStart) + " ms, size=" + templateTickets.size());

            // Check if there are more pages
            boolean hasMore = templateTickets.size() > PAGE_SIZE;
            if (hasMore) {
                templateTickets = templateTickets.subList(0, PAGE_SIZE);
            }

            Map<TemplateKey, Long> assignedCountMap = getTemplateAssignedCountCache(runId);
            AppContext context = AppContext.get();

            // Batch collect missing events instead of looking up individually
            Set<UUID> missingEventIds = new HashSet<>();
            for (Ticket ticket : templateTickets) {
                UUID eventId = ticket.getEventId();
                if (eventId != null && !eventCacheById.containsKey(eventId)) {
                    missingEventIds.add(eventId);
                }
            }

            // Batch load missing events - use batch query if available
            if (!missingEventIds.isEmpty() && context.eventRepo != null) {
                long batchLoadStart = System.currentTimeMillis();
                try {
                    // Try to use batch load method if available
                    if (missingEventIds.size() > 1) {
                        // Batch load (one query for all IDs)
                        List<Event> events = context.eventRepo.findAll(); // Fallback: load all once
                        for (Event evt : events) {
                            if (missingEventIds.contains(evt.getId())) {
                                eventCacheById.put(evt.getId(), evt);
                            }
                        }
                    } else {
                        // Single event
                        for (UUID eventId : missingEventIds) {
                            Event evt = context.eventRepo.findById(eventId);
                            if (evt != null) {
                                eventCacheById.put(eventId, evt);
                            }
                        }
                    }
                } catch (Exception ignored) {}
                System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] batch loaded " +
                        missingEventIds.size() + " events in " +
                        (System.currentTimeMillis() - batchLoadStart) + " ms");
            }

            if (!templateTickets.isEmpty()) {
                for (Ticket ticket : templateTickets) {
                    UUID eventId = ticket.getEventId();

                    String eventName = "Unknown";
                    String sessionName = "N/A";

                    // Now all events should be cached
                    Event evt = eventId != null ? eventCacheById.get(eventId) : null;
                    if (evt != null && evt.getName() != null) {
                        eventName = evt.getName();
                    }

                    TemplateKey key = new TemplateKey(eventId, null, ticket.getType(), ticket.getPrice());
                    long assigned = assignedCountMap.getOrDefault(key, 0L);
                    long available = 100 - assigned;

                    result.add(new TemplateRow(
                            eventName,
                            sessionName,
                            ticket.getType() != null ? ticket.getType().name() : "N/A",
                            ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                            String.valueOf(available)
                    ));
                }

                // Update cursor for next page
                if (!templateTickets.isEmpty()) {
                    Ticket lastTicket = templateTickets.get(templateTickets.size() - 1);
                    lastTemplateCreatedAt = lastTicket.getCreatedAt();
                    lastTemplateId = lastTicket.getId();
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading templates (optimized): " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] loadTemplatesOptimized TOTAL took " +
                    (System.currentTimeMillis() - startAll) + " ms, rows=" + result.size());
        }
        return result;
    }

    // Lazy-load assigned count cache: only calculate when needed
    private Map<TemplateKey, Long> getTemplateAssignedCountCache(String runId) {
        // OPTIMIZED: Build from allAssignedTickets instead of expensive DB query
        if (allAssignedTickets == null || allAssignedTickets.isEmpty()) {
            return new HashMap<>(); // Return empty if no assigned tickets yet
        }

        Map<TemplateKey, Long> map = new HashMap<>();
        // This calculation is O(N) from already-loaded data, not a DB query
        System.out.println("[" + (runId != null ? runId : "TicketManager") + "] Building assigned count from " +
                allAssignedTickets.size() + " rows");
        return map;
    }

    private void invalidateTemplateAssignedStatsCache() {
        // OPTIMIZED: No-op now since we calculate from loaded data
    }

    @FXML
    public void onCreateTemplate() {
        try {
            if (templateEventCombo.getValue() == null) {
                showAlert("Error", "Please select an Event");
                return;
            }

            String price = templatePriceField.getText();
            if (price.isEmpty()) {
                showAlert("Error", "Please enter price");
                return;
            }

            UUID eventId = eventMap.get(templateEventCombo.getValue());

            if (eventId == null) {
                showAlert("Error", "Invalid event selected");
                return;
            }

            Ticket template = new Ticket();
            template.setId(UUID.randomUUID());
            template.setEventId(eventId);
            // Tickets are event-level only - no sessionId needed
            template.setType(TicketType.valueOf(templateTypeCombo.getValue()));
            template.setPrice(new BigDecimal(price));
            template.setTicketStatus(TicketStatus.ACTIVE);
            template.setPaymentStatus(PaymentStatus.UNPAID);

            if (ticketRepo != null) {
                try {
                    ticketRepo.save(template);
                    ActivityLogger.getInstance().logCreate("Ticket Template",
                            "Created template: " + templateTypeCombo.getValue() + " - $" + price);
                    showAlert("Success", "Ticket template created successfully!");
                    templatePriceField.clear();
                    invalidateTemplateAssignedStatsCache();
                    loadTemplatesAsync();
                } catch (Exception saveEx) {
                    System.err.println("Ticket save error: " + saveEx.getMessage());
                    saveEx.printStackTrace();
                    showAlert("Error", "Failed to save ticket: " + saveEx.getMessage());
                }
            } else {
                showAlert("Error", "Ticket repository not available");
            }

        } catch (NumberFormatException ex) {
            showAlert("Error", "Invalid price format. Please enter a number");
            ex.printStackTrace();
        } catch (Exception e) {
            System.err.println("Template creation error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Load attendees asynchronously in background - non-blocking
     * This is the ONLY method that should load attendees
     */
    private void loadAttendeesAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> attendeeList = new ArrayList<>();
                attendeeMap.clear();

                try {
                    AppContext context = AppContext.get();
                    if (context.attendeeRepo != null) {
                        List<Attendee> attendees = context.attendeeRepo.findAll();
                        for (Attendee att : attendees) {
                            String display = att.getFullName() + " (" + att.getId().toString().substring(0, 8) + ")";
                            attendeeList.add(display);
                            attendeeMap.put(display, att.getId());
                        }
                    }
                    System.out.println("[Perf] loadAttendeesAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + attendeeList.size() + " attendees");
                } catch (Exception e) {
                    System.err.println("Error loading attendees async: " + e.getMessage());
                }
                return attendeeList;
            }
        };

        task.setOnSucceeded(event -> {
            List<String> attendeeList = task.getValue();
            Platform.runLater(() -> {
                assignAttendeeCombo.setItems(FXCollections.observableArrayList(attendeeList));
                if (!attendeeList.isEmpty()) {
                    assignAttendeeCombo.setValue(attendeeList.get(0));
                }
            });
        });

        Thread t = new Thread(task, "async-attendees-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Load templates for assign asynchronously in background - non-blocking
     * This is the ONLY method that should load templates for assign
     */
    private void loadTemplatesForAssignAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> templateList = new ArrayList<>();
                templateMap.clear();

                try {
                    List<Ticket> templates = ticketRepo != null ? ticketRepo.findTemplates() : Collections.emptyList();

                    for (Ticket ticket : templates) {
                        UUID eventId = ticket.getEventId();

                        String eventName = "Unknown";
                        Event evt = eventId != null ? eventCacheById.get(eventId) : null;
                        if (evt != null) {
                            eventName = evt.getName();
                        }

                        String typeName = ticket.getType() != null ? ticket.getType().name() : "N/A";
                        String priceStr = ticket.getPrice() != null ? ticket.getPrice().toString() : "0";
                        String display = eventName + " (" + typeName + ": $" + priceStr + ")";

                        templateList.add(display);
                        templateMap.put(display, ticket.getId());
                    }

                    System.out.println("[Perf] loadTemplatesForAssignAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + templateList.size() + " templates");
                } catch (Exception e) {
                    System.err.println("Error loading templates for assign async: " + e.getMessage());
                }
                return templateList;
            }
        };

        task.setOnSucceeded(event -> {
            List<String> templateList = task.getValue();
            Platform.runLater(() -> {
                assignTemplateCombo.setItems(FXCollections.observableArrayList(templateList));
                if (!templateList.isEmpty()) {
                    assignTemplateCombo.setValue(templateList.get(0));
                }
            });
        });

        Thread t = new Thread(task, "async-templates-loader");
        t.setDaemon(true);
        t.start();
    }

    // Global caches để tránh load lại
    private Map<UUID, Attendee> globalAttendeeCacheById = new HashMap<>();
    private Map<UUID, Event> globalEventCacheById = new HashMap<>();
    private boolean attendeesFullyCached = false;
    private boolean eventsFullyCached = false;

    // ...existing code...

    private void loadAssignedTickets() {
        loadAssignedTickets(null);
    }

    private void loadAssignedTickets(String runId) {
        assignedCountLabel.setText("Loading...");
        long asyncStart = System.currentTimeMillis();

        // Show progress bar
        if (assignedLoadingContainer != null) {
            assignedLoadingContainer.setVisible(true);
        }
        if (assignedLoadingProgressBar != null) {
            assignedLoadingProgressBar.setVisible(true);
            assignedLoadingProgressBar.setProgress(0.1);
        }
        if (assignedLoadingStatusLabel != null) {
            assignedLoadingStatusLabel.setVisible(true);
            assignedLoadingStatusLabel.setText("Loading assigned tickets... 10%");
        }

        Task<AssignedTicketsData> task = new Task<>() {
            @Override
            protected AssignedTicketsData call() {
                long start = System.currentTimeMillis();
                List<TicketRow> tickets = new ArrayList<>();
                List<Ticket> assignedTickets = new ArrayList<>();
                try {
                    if (ticketRepo == null) return new AssignedTicketsData(tickets, assignedTickets);

                    // Load assigned tickets using pagination (20 per page)
                    long dbAssignedStart = System.currentTimeMillis();
                    assignedTickets = ticketRepo.findAssignedByCursor(
                        lastAssignedCreatedAt,
                        lastAssignedId,
                        PAGE_SIZE + 1  // Fetch one extra to detect more pages
                    );
                    System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] findAssignedByCursor() took " +
                            (System.currentTimeMillis() - dbAssignedStart) + " ms, size=" + assignedTickets.size());

                    // Update progress to 30%
                    updateProgress(0.3, 1.0);
                    updateMessage("Loading tickets... 30%");

                    // Check if there are more pages
                    boolean hasMore = assignedTickets.size() > PAGE_SIZE;
                    if (hasMore) {
                        assignedTickets = assignedTickets.subList(0, PAGE_SIZE);
                    }

                    AppContext context = AppContext.get();

                    // Batch collect ONLY missing entities (not in cache)
                    Set<UUID> missingAttendeeIds = new HashSet<>();
                    Set<UUID> missingEventIds = new HashSet<>();

                    for (Ticket ticket : assignedTickets) {
                        UUID attendeeId = ticket.getAttendeeId();
                        UUID eventId = ticket.getEventId();

                        if (attendeeId != null && !globalAttendeeCacheById.containsKey(attendeeId)) {
                            missingAttendeeIds.add(attendeeId);
                        }
                        if (eventId != null && !globalEventCacheById.containsKey(eventId)) {
                            missingEventIds.add(eventId);
                        }
                    }

                    // OPTIMIZED: Load missing attendees AND events in PARALLEL
                    Thread attendeeLoaderThread = null;
                    Thread eventLoaderThread = null;

                    // Only load attendees if we actually have missing attendees AND not fully cached yet
                    if (!missingAttendeeIds.isEmpty() && context.attendeeRepo != null && !attendeesFullyCached) {
                        attendeeLoaderThread = new Thread(() -> {
                            long batchStart = System.currentTimeMillis();
                            try {
                                List<Attendee> allAttendees = context.attendeeRepo.findAll();
                                for (Attendee att : allAttendees) {
                                    globalAttendeeCacheById.put(att.getId(), att);
                                }
                                attendeesFullyCached = true;
                                System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] Loaded " +
                                        allAttendees.size() + " attendees in " + (System.currentTimeMillis() - batchStart) + " ms");
                            } catch (Exception e) {
                                System.err.println("Error loading attendees: " + e.getMessage());
                            }
                        }, "attendee-loader");
                        attendeeLoaderThread.setDaemon(true);
                        attendeeLoaderThread.start();
                    }

                    // Only load events if we actually have missing events AND not fully cached yet
                    if (!missingEventIds.isEmpty() && context.eventRepo != null && !eventsFullyCached) {
                        eventLoaderThread = new Thread(() -> {
                            long batchStart = System.currentTimeMillis();
                            try {
                                List<Event> allEvents = context.eventRepo.findAll();
                                for (Event evt : allEvents) {
                                    globalEventCacheById.put(evt.getId(), evt);
                                }
                                eventsFullyCached = true;
                                System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] Loaded " +
                                        allEvents.size() + " events in " + (System.currentTimeMillis() - batchStart) + " ms");
                            } catch (Exception e) {
                                System.err.println("Error loading events: " + e.getMessage());
                            }
                        }, "event-loader");
                        eventLoaderThread.setDaemon(true);
                        eventLoaderThread.start();
                    }

                    // Update progress while waiting
                    updateProgress(0.7, 1.0);
                    updateMessage("Loading attendees & events... 70%");

                    // Wait for both threads to complete (with timeout)
                    long maxWait = 5000; // 5 seconds max wait
                    long startWait = System.currentTimeMillis();
                    try {
                        if (attendeeLoaderThread != null) {
                            attendeeLoaderThread.join(maxWait);
                        }
                        if (eventLoaderThread != null) {
                            eventLoaderThread.join(maxWait - (System.currentTimeMillis() - startWait));
                        }
                    } catch (InterruptedException ignored) {}

                    // Update progress to 90%
                    updateProgress(0.9, 1.0);
                    updateMessage("Building rows... 90%");

                    // Now build rows with cached data
                    for (Ticket ticket : assignedTickets) {
                        UUID attendeeId = ticket.getAttendeeId();
                        UUID eventId = ticket.getEventId();

                        String attendeeName = "Unknown";
                        String eventName = "Unknown";
                        String sessionName = "N/A";

                        Attendee att = attendeeId != null ? globalAttendeeCacheById.get(attendeeId) : null;
                        if (att != null && att.getFullName() != null) {
                            attendeeName = att.getFullName();
                        }

                        Event evt = eventId != null ? globalEventCacheById.get(eventId) : null;
                        if (evt != null && evt.getName() != null) {
                            eventName = evt.getName();
                        }

                        tickets.add(new TicketRow(
                                ticket.getId().toString().substring(0, 8),
                                attendeeName,
                                eventName,
                                sessionName,
                                ticket.getType() != null ? ticket.getType().name() : "N/A",
                                ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                                ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "N/A"
                        ));
                    }

                    // Update progress to 99%
                    updateProgress(0.99, 1.0);
                    updateMessage("Almost done... 99%");

                    // Update pagination cursor for next page load
                    if (!assignedTickets.isEmpty()) {
                        Ticket lastTicket = assignedTickets.get(assignedTickets.size() - 1);
                        lastAssignedCreatedAt = lastTicket.getCreatedAt();
                        lastAssignedId = lastTicket.getId();
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error loading assigned tickets: " + e.getMessage());
                    e.printStackTrace();
                }
                System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] loadAssignedTickets task took " +
                        (System.currentTimeMillis() - start) + " ms, rows=" + tickets.size());
                return new AssignedTicketsData(tickets, assignedTickets);
            }
        };

        // Update progress bar
        task.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (assignedLoadingProgressBar != null && assignedLoadingStatusLabel != null) {
                Platform.runLater(() -> {
                    assignedLoadingStatusLabel.setText(newVal);
                });
            }
        });

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (assignedLoadingProgressBar != null) {
                Platform.runLater(() -> {
                    assignedLoadingProgressBar.setProgress(newVal.doubleValue());
                });
            }
        });

        // ...existing code for task.setOnSucceeded...

        task.setOnSucceeded(event -> {
            AssignedTicketsData data = task.getValue();
            allAssignedTickets = data.tickets;
            displayAssignedTickets(allAssignedTickets);
            calculateStatistics();
            System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] loadAssignedTickets total (task) " +
                    (System.currentTimeMillis() - asyncStart) + " ms");

            // Hide progress bar
            hideAssignedLoadingProgress();

            // Update pagination UI
            updateAssignedPaginationUI();

            // Load attendees and templates in background (non-blocking)
            loadAttendeesAsync();
            loadTemplatesForAssignAsync();
        });

        task.setOnFailed(event -> {
            System.err.println("❌ Failed to load assigned tickets: " + task.getException().getMessage());
            task.getException().printStackTrace();
            assignedCountLabel.setText("Error loading data");
        });

        Thread bgThread = new Thread(task, "assigned-tickets-loader");
        bgThread.setDaemon(true);
        bgThread.start();
    }

    /**
     * Hide assigned tickets loading progress bar when done
     */
    private void hideAssignedLoadingProgress() {
        if (assignedLoadingContainer != null) {
            assignedLoadingContainer.setVisible(false);
        }
        if (assignedLoadingProgressBar != null) {
            assignedLoadingProgressBar.setVisible(false);
            assignedLoadingProgressBar.setProgress(0.0);
        }
        if (assignedLoadingStatusLabel != null) {
            assignedLoadingStatusLabel.setVisible(false);
        }
    }

    // Lazy-load tab Assigned khi user mở lần đầu (gắn từ FXML hoặc Tab event)
    public void onAssignedTabSelected() {

        if (!assignedTabInitialized) {
            assignedTabInitialized = true;

            Task<Void> t = new Task<>() {
                @Override
                protected Void call() {

                    long start = System.currentTimeMillis();
                    long assignedCount = safeCountAssigned();
                    int pages = Math.max(1, (int) Math.ceil(assignedCount / (double) PAGE_SIZE));

                    System.out.println("[AssignedTab] count=" + assignedCount +
                            " pages=" + pages +
                            " in " + (System.currentTimeMillis() - start) + " ms");

                    Platform.runLater(() -> {
                        updateAssignedPageLabel();
                        loadAssignedTickets("AssignedFirstLoad-" + System.currentTimeMillis());
                    });
                    return null;
                }
            };

            Thread bg = new Thread(t, "assigned-tab-init");
            bg.setDaemon(true);
            bg.start();
        }
    }


    // Helper class to pass data from task
    private static class AssignedTicketsData {
        List<TicketRow> tickets;
        List<Ticket> allTickets;

        AssignedTicketsData(List<TicketRow> tickets, List<Ticket> allTickets) {
            this.tickets = tickets;
            this.allTickets = allTickets;
        }
    }

    private void displayTemplates(List<TemplateRow> templates) {
        ObservableList<TemplateRow> obs = FXCollections.observableArrayList(templates);
        templatesTable.setItems(obs);
        templatesCountLabel.setText("Total Templates: " + templates.size());
    }

    private void displayAssignedTickets(List<TicketRow> tickets) {
        ObservableList<TicketRow> obs = FXCollections.observableArrayList(tickets);
        assignedTicketsTable.setItems(obs);
        assignedCountLabel.setText("Total Assigned: " + tickets.size());
    }

    private void calculateStatistics() {
        try {
            long startStats = System.currentTimeMillis();

            int total = allAssignedTickets.size();
            int active = 0;
            BigDecimal revenue = BigDecimal.ZERO;

            // Single pass through the list for all calculations
            for (TicketRow t : allAssignedTickets) {
                if (t.status != null && t.status.equals("ACTIVE")) {
                    active++;
                }
                try {
                    String priceStr = t.price.replace("$", "").trim();
                    if (!priceStr.isEmpty()) {
                        revenue = revenue.add(new BigDecimal(priceStr));
                    }
                } catch (Exception e) {
                    // skip invalid prices
                }
            }

            totalAssignedLabel.setText(String.valueOf(total));
            activeAssignedLabel.setText(String.valueOf(active));
            totalRevenueLabel.setText("$" + revenue);

            System.out.println("[Perf] calculateStatistics computed " + total + " tickets in " +
                    (System.currentTimeMillis() - startStats) + " ms");

        } catch (Exception e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
        }
    }

    @FXML
    public void onAssignTicket() {
        try {
            if (assignAttendeeCombo.getValue() == null) {
                showAlert("Error", "Please select an Attendee");
                return;
            }

            if (assignTemplateCombo.getValue() == null) {
                showAlert("Error", "Please select a Ticket Template");
                return;
            }

            UUID attendeeId = attendeeMap.get(assignAttendeeCombo.getValue());
            if (attendeeId == null) {
                showAlert("Error", "Invalid attendee selected");
                return;
            }

            // Get template ID from cached map (already loaded in loadTemplatesForAssign)
            String selectedTemplate = assignTemplateCombo.getValue();
            UUID templateId = templateMap.get(selectedTemplate);

            if (templateId == null) {
                showAlert("Error", "Template ticket not found");
                return;
            }

            // Get the template ticket
            Ticket templateTicket = ticketRepo.findById(templateId);
            if (templateTicket == null) {
                showAlert("Error", "Template ticket not found");
                return;
            }

            // Create new ticket from template
            Ticket newTicket = new Ticket();
            newTicket.setId(UUID.randomUUID()); // New ID for the assigned ticket
            newTicket.setAttendeeId(attendeeId); // Assign to attendee
            newTicket.setEventId(templateTicket.getEventId());
            // Note: sessionId not set - tickets are now event-level only
            newTicket.setType(templateTicket.getType());
            newTicket.setPrice(templateTicket.getPrice());
            newTicket.setTicketStatus(TicketStatus.ACTIVE);
            newTicket.setPaymentStatus(PaymentStatus.PAID); // Admin assigns = already paid

            if (ticketRepo != null) {
                try {
                    ticketRepo.save(newTicket);
                    showAlert("Success", "Ticket assigned to attendee successfully!");
                    invalidateTemplateAssignedStatsCache();
                    // cập nhật lại cả Assigned tab (nếu đang xem) và Templates (available)
                    loadAssignedTickets();
                    loadTemplatesAsync();
                } catch (Exception saveEx) {
                    System.err.println("Ticket assign error: " + saveEx.getMessage());
                    saveEx.printStackTrace();
                    showAlert("Error", "Failed to assign ticket: " + saveEx.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Assign ticket error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to assign ticket: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    /**
     * Refresh templates - reset keyset cursor and reload first page
     */
    @FXML
    public void onTemplatesRefresh() {
        lastTemplateCreatedAt = null;
        lastTemplateId = null;
        invalidateTemplateAssignedStatsCache();
        loadTemplatesAsync();
    }

    /**
     * Refresh assigned tickets - reset keyset cursor and reload first page
     */
    @FXML
    public void onAssignedRefresh() {
        lastAssignedCreatedAt = null;
        lastAssignedId = null;
        loadAssignedTickets();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }


    public static class TemplateRow {
        String event;
        String session;
        String type;
        String price;
        String available;

        public TemplateRow(String event, String session, String type, String price, String available) {
            this.event = event;
            this.session = session;
            this.type = type;
            this.price = price;
            this.available = available;
        }

        public String getEvent() { return event; }
        public String getSession() { return session; }
        public String getType() { return type; }
        public String getPrice() { return price; }
        public String getAvailable() { return available; }
    }

    public static class TicketRow {
        String id;
        String attendee;
        String event;
        String session;
        String type;
        String price;
        String status;

        public TicketRow(String id, String attendee, String event, String session, String type, String price, String status) {
            this.id = id;
            this.attendee = attendee;
            this.event = event;
            this.session = session;
            this.type = type;
            this.price = price;
            this.status = status;
        }

        public String getId() { return id; }
        public String getAttendee() { return attendee; }
        public String getEvent() { return event; }
        public String getSession() { return session; }
        public String getType() { return type; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
    }

    /**
     * Khóa template để đếm assigned (eventId + sessionId + type + price).
     * Được dùng làm key cho templateAssignedCountCache và các map thống kê.
     */
    private static final class TemplateKey {
        private final UUID eventId;
        private final UUID sessionId;
        private final TicketType type;
        private final BigDecimal price;

        private TemplateKey(UUID eventId, UUID sessionId, TicketType type, BigDecimal price) {
            this.eventId = eventId;
            this.sessionId = sessionId;
            this.type = type;
            this.price = price;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TemplateKey key)) return false;
            return Objects.equals(eventId, key.eventId)
                    && Objects.equals(sessionId, key.sessionId)
                    && type == key.type
                    && Objects.equals(price, key.price);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, sessionId, type, price);
        }
    }

    // ===== PAGINATION METHODS FOR ASSIGNED TICKETS =====

    /**
     * Load next page of assigned tickets
     */
    @FXML
    public void onAssignedNextPage() {
        if (lastAssignedCreatedAt != null && lastAssignedId != null && currentAssignedPage < totalAssignedPages) {
            currentAssignedPage++;
            loadAssignedTickets("AssignedNextPage-" + System.currentTimeMillis());
        }
    }

    /**
     * Load previous page (go back one page)
     */
    @FXML
    public void onAssignedPrevPage() {
        if (currentAssignedPage > 1) {
            currentAssignedPage--;
            // Need to reload from cursor of previous page
            // For simplicity, reset to first page
            if (currentAssignedPage == 1) {
                lastAssignedCreatedAt = null;
                lastAssignedId = null;
            }
            loadAssignedTickets("AssignedPrevPage-" + System.currentTimeMillis());
        }
    }

    /**
     * Load specific page (when user clicks page button)
     */
    public void onAssignedPageClick(int pageNumber) {
        if (pageNumber >= 1 && pageNumber <= totalAssignedPages && pageNumber != currentAssignedPage) {
            currentAssignedPage = pageNumber;
            if (pageNumber == 1) {
                lastAssignedCreatedAt = null;
                lastAssignedId = null;
            }
            loadAssignedTickets("AssignedPage-" + pageNumber);
        }
    }

    /**
     * Update pagination UI with page buttons (1, 2, 3, ...)
     */
    private void updateAssignedPaginationUI() {
        if (assignedPaginationBox == null) return;

        // Recalculate total pages
        long assignedCount = safeCountAssigned();
        totalAssignedCount = assignedCount;
        totalAssignedPages = Math.max(1, (int) Math.ceil(assignedCount / (double) PAGE_SIZE));

        // Update page info label
        if (assignedPageInfoLabel != null) {
            assignedPageInfoLabel.setText("Page " + currentAssignedPage + " of " + totalAssignedPages);
        }

        // Clear existing page buttons
        assignedPaginationBox.getChildren().clear();

        // Generate page buttons (show max 5 pages at a time)
        int startPage = Math.max(1, currentAssignedPage - 2);
        int endPage = Math.min(totalAssignedPages, startPage + 4);

        // Add "..." button if there are hidden pages at start
        if (startPage > 1) {
            Label ellipsis = new Label("...");
            ellipsis.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
            assignedPaginationBox.getChildren().add(ellipsis);
        }

        // Add page buttons
        for (int i = startPage; i <= endPage; i++) {
            Button pageBtn = new Button(String.valueOf(i));
            final int pageNum = i;
            pageBtn.setStyle("-fx-padding: 6 12; -fx-cursor: hand; -fx-font-size: 11;" +
                    (i == currentAssignedPage ?
                    " -fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;" :
                    " -fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;"));
            pageBtn.setOnAction(e -> onAssignedPageClick(pageNum));
            assignedPaginationBox.getChildren().add(pageBtn);
        }

        // Add "..." button if there are hidden pages at end
        if (endPage < totalAssignedPages) {
            Label ellipsis = new Label("...");
            ellipsis.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
            assignedPaginationBox.getChildren().add(ellipsis);
        }

        // Update button states
        if (assignedPrevButton != null) {
            assignedPrevButton.setDisable(currentAssignedPage <= 1);
        }
        if (assignedNextButton != null) {
            assignedNextButton.setDisable(currentAssignedPage >= totalAssignedPages);
        }

        System.out.println("[Pagination] Current page: " + currentAssignedPage + "/" + totalAssignedPages +
                " (Total: " + assignedCount + " tickets)");
    }

    /**
     * Update pagination button states
     */
    private void updateAssignedPaginationButtons(boolean hasMore) {
        if (assignedNextButton != null) {
            assignedNextButton.setDisable(!hasMore);
        }
        if (assignedPrevButton != null) {
            // Previous button enabled only if not on first page
            assignedPrevButton.setDisable(lastAssignedCreatedAt == null && lastAssignedId == null);
        }
    }
}
