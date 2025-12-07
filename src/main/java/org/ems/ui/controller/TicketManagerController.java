package org.ems.ui.controller;

/**
 * @author <your group number>
 */

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
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

    private TicketRepository ticketRepo;
    private List<TemplateRow> allTemplates;
    private List<TicketRow> allAssignedTickets;

    // Keyset Pagination state
    private static final int PAGE_SIZE = 20;
    private Timestamp lastTemplateCreatedAt = null;
    private UUID lastTemplateId = null;
    private Timestamp lastAssignedCreatedAt = null;
    private UUID lastAssignedId = null;

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
        // ✅ STEP 1: UI SETUP ONLY (< 50ms)
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

        System.out.println("✅ UI initialized in < 50ms");

        // ✅ STEP 2: DEFER ALL DB OPERATIONS TO AFTER UI RENDER
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

        // Load template page (keyset pagination)
        loadTemplatesAsync(runId);

        // Build caches in background (event/attendee/session)
        initCachesAsync(runId);
    }


    private void initCachesAsync(String runId) {
        Task<Void> cacheTask = new Task<>() {
            @Override
            protected Void call() {
                long start = System.currentTimeMillis();
                try {
                    AppContext context = AppContext.get();
                    eventCacheById.clear();
                    sessionCacheById.clear();
                    attendeeCacheById.clear();

                    if (context.eventRepo != null) {
                        for (Event e : context.eventRepo.findAll()) {
                            eventCacheById.put(e.getId(), e);
                        }
                    }
                    if (context.sessionRepo != null) {
                        for (Session s : context.sessionRepo.findAll()) {
                            sessionCacheById.put(s.getId(), s);
                        }
                    }
                    if (context.attendeeRepo != null) {
                        for (Attendee a : context.attendeeRepo.findAll()) {
                            attendeeCacheById.put(a.getId(), a);
                        }
                    }

                    System.out.println("[" + runId + "] initCachesAsync built caches in " +
                            (System.currentTimeMillis() - start) + " ms (events=" + eventCacheById.size() +
                            ", sessions=" + sessionCacheById.size() + ", attendees=" + attendeeCacheById.size() + ")");

                } catch (Exception ex) {
                    System.err.println("[" + runId + "] initCachesAsync failed: " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }
        };

        Thread t = new Thread(cacheTask, "ticket-cache-loader");
        t.setDaemon(true);
        t.start();
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
            if (!templateTickets.isEmpty()) {
                for (Ticket ticket : templateTickets) {
                    UUID eventId = ticket.getEventId();

                    String eventName = "Unknown";
                    String sessionName = "N/A";

                    Event evt = eventId != null ? eventCacheById.get(eventId) : null;
                    if (evt == null && context.eventRepo != null && eventId != null) {
                        try {
                            evt = context.eventRepo.findById(eventId);
                            if (evt != null) {
                                eventCacheById.put(eventId, evt);
                            }
                        } catch (Exception ignored) {
                        }
                    }
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

    // Lấy (và cache) thống kê assigned theo template; chỉ query DB 1 lần cho vòng đời controller
    private Map<TemplateKey, Long> getTemplateAssignedCountCache(String runId) {
        if (templateAssignedCountCache != null) {
            return templateAssignedCountCache;
        }
        long statsStart = System.currentTimeMillis();
        List<TicketRepository.TemplateAssignmentStats> statsList = ticketRepo.findAssignedStatsForTemplates();
        Map<TemplateKey, Long> map = new HashMap<>();
        for (TicketRepository.TemplateAssignmentStats s : statsList) {
            // Note: sessionId is now null - tickets are event-level only
            TemplateKey key = new TemplateKey(s.getEventId(), null, s.getType(), s.getPrice());
            map.put(key, s.getAssignedCount());
        }
        templateAssignedCountCache = map;
        System.out.println("[" + (runId != null ? runId : "TicketTemplates") + "] findAssignedStatsForTemplates() took " +
                (System.currentTimeMillis() - statsStart) + " ms, keys=" + map.size());
        return map;
    }

    private void invalidateTemplateAssignedStatsCache() {
        templateAssignedCountCache = null;
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

    private void loadAttendees() {
        try {
            AppContext context = AppContext.get();
            List<String> attendeeList = new ArrayList<>();
            attendeeMap.clear();

            if (context.attendeeRepo != null) {
                List<Attendee> attendees = context.attendeeRepo.findAll();
                for (Attendee att : attendees) {
                    String display = att.getFullName() + " (" + att.getId().toString().substring(0, 8) + ")";
                    attendeeList.add(display);
                    attendeeMap.put(display, att.getId());
                }
            }

            assignAttendeeCombo.setItems(FXCollections.observableArrayList(attendeeList));
            if (!attendeeList.isEmpty()) {
                assignAttendeeCombo.setValue(attendeeList.get(0));
            }

        } catch (Exception e) {
            System.err.println("Error loading attendees: " + e.getMessage());
        }
    }

    private void loadTemplatesForAssign() {
        try {
            List<String> templateList = new ArrayList<>();
            templateMap.clear();

            AppContext context = AppContext.get();

            List<Ticket> templates = ticketRepo != null ? ticketRepo.findTemplates() : Collections.emptyList();

            for (Ticket ticket : templates) {
                UUID eventId = ticket.getEventId();

                String eventName = "Unknown";
                String sessionName = "N/A";  // Event-level template, no specific session

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

            assignTemplateCombo.setItems(FXCollections.observableArrayList(templateList));
            if (!templateList.isEmpty()) {
                assignTemplateCombo.setValue(templateList.get(0));
            }

        } catch (Exception e) {
            System.err.println("Error loading templates for assign: " + e.getMessage());
        }
    }

    private void loadAssignedTickets() {
        loadAssignedTickets(null);
    }

    private void loadAssignedTickets(String runId) {
        assignedCountLabel.setText("Loading...");
        long asyncStart = System.currentTimeMillis();

        Task<AssignedTicketsData> task = new Task<>() {
            @Override
            protected AssignedTicketsData call() {
                long start = System.currentTimeMillis();
                List<TicketRow> tickets = new ArrayList<>();
                List<Ticket> assignedTickets = new ArrayList<>();
                try {
                    if (ticketRepo == null) return new AssignedTicketsData(tickets, assignedTickets);

                    long dbAssignedStart = System.currentTimeMillis();
                    assignedTickets = ticketRepo.findAssignedByCursor(
                        lastAssignedCreatedAt,
                        lastAssignedId,
                        PAGE_SIZE + 1
                    );
                    System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] findAssignedByCursor() took " +
                            (System.currentTimeMillis() - dbAssignedStart) + " ms, size=" + assignedTickets.size());

                    boolean hasMore = assignedTickets.size() > PAGE_SIZE;
                    if (hasMore) {
                        assignedTickets = assignedTickets.subList(0, PAGE_SIZE);
                    }

                    AppContext context = AppContext.get();

                    for (Ticket ticket : assignedTickets) {
                        UUID attendeeId = ticket.getAttendeeId();
                        UUID eventId = ticket.getEventId();

                        String attendeeName = "Unknown";
                        String eventName = "Unknown";
                        String sessionName = "N/A";

                        Attendee att = attendeeId != null ? attendeeCacheById.get(attendeeId) : null;
                        if (att == null && context.attendeeRepo != null && attendeeId != null) {
                            try {
                                att = context.attendeeRepo.findById(attendeeId);
                                if (att != null) {
                                    attendeeCacheById.put(attendeeId, att);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        if (att != null && att.getFullName() != null) {
                            attendeeName = att.getFullName();
                        }

                        Event evt = eventId != null ? eventCacheById.get(eventId) : null;
                        if (evt == null && context.eventRepo != null && eventId != null) {
                            try {
                                evt = context.eventRepo.findById(eventId);
                                if (evt != null) {
                                    eventCacheById.put(eventId, evt);
                                }
                            } catch (Exception ignored) {
                            }
                        }
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

        task.setOnSucceeded(event -> {
            AssignedTicketsData data = task.getValue();
            allAssignedTickets = data.tickets;
            displayAssignedTickets(allAssignedTickets);
            loadAttendees();
            loadTemplatesForAssign();
            calculateStatistics();
            System.out.println("[" + (runId != null ? runId : "AssignedTickets") + "] loadAssignedTickets total (task) " +
                    (System.currentTimeMillis() - asyncStart) + " ms");
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
            int total = allAssignedTickets.size();
            int active = (int) allAssignedTickets.stream()
                    .filter(t -> t.status.equals("ACTIVE"))
                    .count();

            BigDecimal revenue = BigDecimal.ZERO;
            for (TicketRow t : allAssignedTickets) {
                try {
                    String priceStr = t.price.replace("$", "");
                    revenue = revenue.add(new BigDecimal(priceStr));
                } catch (Exception e) {
                    // skip
                }
            }

            totalAssignedLabel.setText(String.valueOf(total));
            activeAssignedLabel.setText(String.valueOf(active));
            totalRevenueLabel.setText("$" + revenue);

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
}
