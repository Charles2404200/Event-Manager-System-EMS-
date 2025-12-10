package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.ems.application.service.EventService;
import org.ems.application.service.IdentityService;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.config.AppContext;
import org.ems.domain.repository.SessionRepository;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SessionManagerController - Manages session CRUD operations
 * @author EMS Team
 */
public class SessionManagerController {

    @FXML private TableView<Session> sessionTable;
    @FXML private TableColumn<Session, String> colId;
    @FXML private TableColumn<Session, String> colTitle;
    @FXML private TableColumn<Session, String> colEvent;
    @FXML private TableColumn<Session, String> colStart;
    @FXML private TableColumn<Session, String> colVenue;
    @FXML private TableColumn<Session, Integer> colCapacity;

    @FXML private Label detailIdLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailDescLabel;
    @FXML private Label detailStartLabel;
    @FXML private Label detailEndLabel;
    @FXML private Label detailVenueLabel;
    @FXML private Label detailCapacityLabel;
    @FXML private ListView<String> presenterListView;
    @FXML private Label sessionCountLabel;
    @FXML private Label pageInfoLabel;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingStatusLabel;
    @FXML private VBox loadingContainer;

    private final EventService eventService = AppContext.get().eventService;
    private final IdentityService identityService = AppContext.get().identityService;

    private Session selectedSession;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Pagination state
    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private int totalPages = 1;

    // Cache để tránh load lại sessions khi pagination
    private List<Session> allSessionsCache = null;

    private Map<UUID, Presenter> presenterCache;
    private List<Event> eventCache;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("⚙️ [SessionManager] initialize() starting...");

        setupTableColumns();
        initPagination();

        // OPTIMIZED: Pre-load presenters on background thread during UI init
        // This way, when user clicks a session detail, presenters are already cached
        preloadPresentersBackground();

        // Load sessions immediately
        loadSessionsAsync();

        sessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedSession = newVal;
                displaySessionDetails(newVal);
            }
        });

        System.out.println("  ✓ initialize() completed in " + (System.currentTimeMillis() - initStart) + " ms");
    }

    /**
     * Pre-load presenters on background thread during init
     * This ensures presenters are cached before user clicks session details
     */
    private void preloadPresentersBackground() {
        long preloadStart = System.currentTimeMillis();
        System.out.println("👥 [SessionManager] Pre-loading presenters on background...");

        Task<Map<UUID, Presenter>> task = new Task<>() {
            @Override
            protected Map<UUID, Presenter> call() {
                long dbStart = System.currentTimeMillis();
                try {
                    List<Presenter> allPresenters = identityService.getAllPresenters();
                    System.out.println("  ✓ identityService.getAllPresenters() took " +
                            (System.currentTimeMillis() - dbStart) + " ms, loaded " + allPresenters.size() + " presenters");

                    return allPresenters.stream()
                            .collect(Collectors.toMap(Presenter::getId, p -> p));
                } catch (Exception e) {
                    System.err.println("✗ Error pre-loading presenters: " + e.getMessage());
                    return new HashMap<>();
                }
            }
        };

        task.setOnSucceeded(evt -> {
            presenterCache = task.getValue();
            System.out.println("  ✓ Presenter cache built (" + presenterCache.size() + " presenters) in " +
                    (System.currentTimeMillis() - preloadStart) + " ms");
        });

        task.setOnFailed(evt -> {
            presenterCache = new HashMap<>();
            System.err.println("✗ Failed to pre-load presenters");
        });

        Thread t = new Thread(task, "presenter-preloader");
        t.setDaemon(true);
        t.start();
    }

    private void initPagination() {
        // OPTIMIZED: Don't query DB for count - pagination is calculated after load
        currentPage = 0;
        totalPages = 1;
        updatePageInfo();
    }

    private void updatePageInfo() {
        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getId().toString().substring(0, 8) + "..."
                ));
        colTitle.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTitle()
                ));

        // colEvent: set rỗng; sẽ được set lại sau khi đã có cache tên event trong loadSessionsAsync
        colEvent.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(""));

        colStart.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStart().format(formatter)
                ));
        colVenue.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getVenue()
                ));
        colCapacity.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getCapacity()
                ).asObject());
    }

    // Async load sessions + handle pagination on client-side (OPTIMIZED: parallel loading)
    private void loadSessionsAsync() {
        long mainStart = System.currentTimeMillis();
        System.out.println("📋 [SessionManager] Starting loadSessionsAsync()...");

        // Show loading progress bar
        if (loadingContainer != null) {
            loadingContainer.setVisible(true);
        }
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisible(true);
            loadingProgressBar.setProgress(0.1); // Start at 10%
        }
        if (loadingStatusLabel != null) {
            loadingStatusLabel.setVisible(true);
            loadingStatusLabel.setText("Loading sessions... 0%");
        }

        Task<List<Session>> task = new Task<>() {
            @Override
            protected List<Session> call() {
                long dbStart = System.currentTimeMillis();
                try {
                    // OPTIMIZED: Load ALL sessions to get total count, but we'll paginate display
                    List<Session> allSessions = eventService.getSessions();

                    long elapsed = System.currentTimeMillis() - dbStart;
                    System.out.println("  ✓ eventService.getSessions() took " +
                            elapsed + " ms, loaded " + allSessions.size() + " sessions");

                    // Update progress to 70% after loading sessions
                    updateProgress(0.7, 1.0);

                    return allSessions;
                } catch (Exception e) {
                    System.err.println("✗ Error loading sessions: " + e.getMessage());
                    e.printStackTrace();
                    updateProgress(0.0, 1.0);
                    return new ArrayList<>();
                }
            }
        };

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(newVal.doubleValue());
            }
            if (loadingStatusLabel != null) {
                int percent = (int) (newVal.doubleValue() * 100);
                loadingStatusLabel.setText("Loading sessions... " + percent + "%");
            }
        });

        task.setOnSucceeded(evt -> {
            long pageStart = System.currentTimeMillis();
            List<Session> allSessions = task.getValue();
            allSessionsCache = allSessions; // Cache for pagination

            if (allSessions.isEmpty()) {
                System.out.println("  ⚠️ No sessions found!");
                sessionTable.setItems(FXCollections.observableArrayList());
                hideLoadingProgress();
                return;
            }

            // Calculate total pages based on loaded data
            totalPages = Math.max(1, (int) Math.ceil(allSessions.size() / (double) PAGE_SIZE));
            updatePageInfo();
            System.out.println("  ✓ Pagination calculated in " + (System.currentTimeMillis() - pageStart) +
                    " ms (total pages: " + totalPages + ", total sessions: " + allSessions.size() + ")");

            // Extract ONLY current page data (first 20 if page 0)
            int offset = currentPage * PAGE_SIZE;
            int endIndex = Math.min(offset + PAGE_SIZE, allSessions.size());
            List<Session> pageData = allSessions.subList(offset, endIndex);

            System.out.println("  ✓ Extracted page " + (currentPage + 1) + " with " + pageData.size() + " items");

            // Display sessions IMMEDIATELY without waiting for events
            long displayStart = System.currentTimeMillis();
            ObservableList<Session> sessions = FXCollections.observableArrayList(pageData);
            sessionTable.setItems(sessions);

            if (sessionCountLabel != null) {
                sessionCountLabel.setText("Total Sessions: " + allSessions.size());
            }
            System.out.println("  ✓ UI updated (displayed " + pageData.size() + " sessions) in " +
                    (System.currentTimeMillis() - displayStart) + " ms");

            // Hide loading progress immediately - sessions are displayed
            hideLoadingProgress();
            System.out.println("✓ Sessions displayed to user in " + (System.currentTimeMillis() - mainStart) + " ms");

            // Load events in background AFTER displaying sessions (parallel, no UI block)
            loadEventsLazyForDisplay();
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            System.err.println("✗ Failed to load sessions: " + (ex != null ? ex.getMessage() : "unknown"));
            showError("Error loading sessions", ex != null ? ex.getMessage() : "Unknown error");
            hideLoadingProgress();
        });

        System.out.println("  ⏱ Total initialization took " + (System.currentTimeMillis() - mainStart) + " ms");
        Thread t = new Thread(task, "session-manager-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Hide loading progress bar when done
     */
    private void hideLoadingProgress() {
        if (loadingContainer != null) {
            loadingContainer.setVisible(false);
        }
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisible(false);
            loadingProgressBar.setProgress(0.0);
        }
        if (loadingStatusLabel != null) {
            loadingStatusLabel.setVisible(false);
        }
    }

    /**
     * Load events lazily for display - OPTIMIZED for parallel execution
     */
    private void loadEventsLazyForDisplay() {
        if (eventCache != null && !eventCache.isEmpty()) {
            setupEventNameMapping();
            hideLoadingProgress(); // Hide progress bar
            return;
        }

        long lazyStart = System.currentTimeMillis();
        System.out.println("📦 [SessionManager] Starting lazy load of events...");

        Task<List<Event>> task = new Task<>() {
            @Override
            protected List<Event> call() {
                long eventDbStart = System.currentTimeMillis();
                try {
                    List<Event> result = eventService.getEvents();
                    System.out.println("  ✓ eventService.getEvents() took " +
                            (System.currentTimeMillis() - eventDbStart) + " ms, loaded " + result.size() + " events");

                    // Update progress to 95%
                    updateProgress(0.95, 1.0);

                    return result;
                } catch (Exception e) {
                    System.err.println("✗ Error loading events: " + e.getMessage());
                    return new ArrayList<>();
                }
            }
        };

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(newVal.doubleValue());
            }
            if (loadingStatusLabel != null) {
                int percent = (int) (newVal.doubleValue() * 100);
                loadingStatusLabel.setText("Loading events... " + percent + "%");
            }
        });

        task.setOnSucceeded(evt -> {
            eventCache = task.getValue();
            long mappingStart = System.currentTimeMillis();
            setupEventNameMapping();
            System.out.println("  ✓ Event name mapping setup in " + (System.currentTimeMillis() - mappingStart) + " ms");
            System.out.println("✓ Event lazy loading completed in " + (System.currentTimeMillis() - lazyStart) + " ms");

            // Hide progress bar when done
            hideLoadingProgress();
        });

        task.setOnFailed(evt -> {
            eventCache = new ArrayList<>();
            System.err.println("✗ Failed to load events lazily");
            hideLoadingProgress();
        });

        Thread t = new Thread(task, "session-events-lazy-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Setup event name mapping - called once after events loaded
     */
    private void setupEventNameMapping() {
        long mappingStart = System.currentTimeMillis();
        Map<UUID, String> eventNameMap = new HashMap<>();
        if (eventCache != null) {
            for (Event e : eventCache) {
                eventNameMap.put(e.getId(), e.getName());
            }
        }

        colEvent.setCellValueFactory(data -> {
            UUID eventId = data.getValue().getEventId();
            String name = eventNameMap.getOrDefault(eventId, "N/A");
            return new javafx.beans.property.SimpleStringProperty(name);
        });
        System.out.println("  ✓ Event name mapping applied in " + (System.currentTimeMillis() - mappingStart) + " ms");
    }

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            updatePageInfo();
            displayPageFromCache();
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage + 1 < totalPages) {
            currentPage++;
            updatePageInfo();
            displayPageFromCache();
        }
    }

    /**
     * Display current page from cached sessions - no DB query needed
     */
    private void displayPageFromCache() {
        if (allSessionsCache == null || allSessionsCache.isEmpty()) {
            System.out.println("⚠️ No cached sessions available");
            return;
        }

        long displayStart = System.currentTimeMillis();
        System.out.println("📄 [Pagination] Displaying page " + (currentPage + 1) + "...");

        // Extract current page data from cache
        int offset = currentPage * PAGE_SIZE;
        int endIndex = Math.min(offset + PAGE_SIZE, allSessionsCache.size());
        List<Session> pageData = allSessionsCache.subList(offset, endIndex);

        ObservableList<Session> sessions = FXCollections.observableArrayList(pageData);
        sessionTable.setItems(sessions);

        System.out.println("  ✓ Displayed " + pageData.size() + " sessions from cache in " +
                (System.currentTimeMillis() - displayStart) + " ms");
    }

    @FXML
    public void onRefresh() {
        currentPage = 0;
        eventCache = null; // Force reload events on next load
        presenterCache = null; // Force reload presenters on next load
        loadSessionsAsync();
        selectedSession = null;
        clearDetails();
    }

    private void clearDetails() {
        detailIdLabel.setText("-");
        detailTitleLabel.setText("-");
        detailDescLabel.setText("-");
        detailStartLabel.setText("-");
        detailEndLabel.setText("-");
        detailVenueLabel.setText("-");
        detailCapacityLabel.setText("-");
        presenterListView.setItems(FXCollections.observableList(List.of()));
    }

    /**
     * Lazy load presenters only when needed (first time showing details or add presenter dialog)
     * Note: Presenters are pre-loaded during initialize(), so this usually just returns cached data
     */
    private void loadPresentersCached() {
        if (presenterCache != null && !presenterCache.isEmpty()) {
            System.out.println("  ℹ Presenters already cached (" + presenterCache.size() + " presenters, loaded during init)");
            return; // Already loaded during init
        }

        // Fallback: if pre-load failed, load now (shouldn't happen)
        long presStart = System.currentTimeMillis();
        System.out.println("  ⚠️ Presenter cache is empty, loading now (this should not happen - presenters should be pre-loaded)...");
        try {
            List<Presenter> allPresenters = identityService.getAllPresenters();
            presenterCache = allPresenters.stream()
                    .collect(Collectors.toMap(Presenter::getId, p -> p));
            System.out.println("  ✓ Loaded " + presenterCache.size() + " presenters in " +
                    (System.currentTimeMillis() - presStart) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error loading presenters: " + e.getMessage());
            presenterCache = new HashMap<>();
        }
    }

    private void displaySessionDetails(Session session) {
        long detailStart = System.currentTimeMillis();
        System.out.println("📄 [SessionManager] Displaying session details: " + session.getTitle());

        try {
            long basicStart = System.currentTimeMillis();
            detailIdLabel.setText(session.getId().toString());
            detailTitleLabel.setText(session.getTitle());
            detailDescLabel.setText(session.getDescription() != null ? session.getDescription() : "No description");
            detailStartLabel.setText(session.getStart().format(formatter));
            detailEndLabel.setText(session.getEnd().format(formatter));
            detailVenueLabel.setText(session.getVenue());
            detailCapacityLabel.setText(String.valueOf(session.getCapacity()));
            System.out.println("  ✓ Basic labels updated in " + (System.currentTimeMillis() - basicStart) + " ms");

            // Lazy load presenters only when displaying details
            long presenterStart = System.currentTimeMillis();
            loadPresentersCached();
            System.out.println("  ✓ Presenter cache checked in " + (System.currentTimeMillis() - presenterStart) + " ms");

            List<UUID> presenterIds = session.getPresenterIds();

            if (presenterIds == null || presenterIds.isEmpty()) {
                presenterListView.setItems(FXCollections.observableList(List.of()));
                System.out.println("  ℹ No presenters assigned to this session");
            } else {
                List<String> presenterNames = new ArrayList<>();

                long mappingStart = System.currentTimeMillis();
                for (UUID pid : presenterIds) {
                    Presenter p = presenterCache.get(pid);
                    if (p == null) {
                        // Fallback: if not in cache, try to load single presenter
                        p = (Presenter) identityService.getUserById(pid);
                        if (p != null) {
                            presenterCache.put(pid, p);
                        }
                    }
                    presenterNames.add(p != null ? p.getFullName() : "Unknown");
                }
                presenterListView.setItems(FXCollections.observableList(presenterNames));
                System.out.println("  ✓ Presenter names mapped in " + (System.currentTimeMillis() - mappingStart) +
                        " ms (" + presenterNames.size() + " presenters)");
            }

            System.out.println("✓ Session details loaded in " + (System.currentTimeMillis() - detailStart) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error loading details: " + e.getMessage());
            showError("Error loading details", e.getMessage());
        }
    }

    @FXML
    public void onAddSession() {
        long dialogStart = System.currentTimeMillis();
        System.out.println("➕ [onAddSession] Opening add session dialog...");

        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Add New Session");
        dialog.setHeaderText("Create a new session");

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField();
        titleField.setPrefWidth(300);

        Label descLabel = new Label("Description:");
        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label venueLabel = new Label("Venue:");
        TextField venueField = new TextField();
        venueField.setPrefWidth(300);

        Label capacityLabel = new Label("Capacity:");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, 50);

        Label startLabel = new Label("Start (dd/MM/yyyy HH:mm):");
        TextField startField = new TextField();
        startField.setPromptText("DD/MM/YYYY HH:MM");

        Label endLabel = new Label("End (dd/MM/yyyy HH:mm):");
        TextField endField = new TextField();
        endField.setPromptText("DD/MM/YYYY HH:MM");

        Label eventLabel = new Label("Event:");
        ComboBox<Event> eventBox = new ComboBox<>();

        // Lazy load events: use cache if available, otherwise load on demand
        long eventStart = System.currentTimeMillis();
        if (eventCache == null || eventCache.isEmpty()) {
            System.out.println("  📦 Loading events for add dialog...");
            Task<List<Event>> task = new Task<>() {
                @Override
                protected List<Event> call() {
                    long dbStart = System.currentTimeMillis();
                    try {
                        List<Event> events = eventService.getEvents();
                        System.out.println("  ✓ eventService.getEvents() took " +
                                (System.currentTimeMillis() - dbStart) + " ms, loaded " + events.size() + " events");
                        return events;
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }
                }
            };

            task.setOnSucceeded(evt -> {
                eventCache = task.getValue();
                eventBox.getItems().addAll(eventCache);
                System.out.println("  ✓ Event combo populated in " + (System.currentTimeMillis() - eventStart) + " ms");
            });

            Thread t = new Thread(task, "add-session-events-loader");
            t.setDaemon(true);
            t.start();
        } else {
            System.out.println("  ℹ Using cached events (" + eventCache.size() + " events)");
            eventBox.getItems().addAll(eventCache);
            System.out.println("  ✓ Event combo populated in " + (System.currentTimeMillis() - eventStart) + " ms");
        }

        eventBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });
        eventBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(400);

        grid.addRow(0, titleLabel, titleField);
        grid.addRow(1, descLabel, descArea);
        grid.addRow(2, venueLabel, venueField);
        grid.addRow(3, capacityLabel, capacitySpinner);
        grid.addRow(4, startLabel, startField);
        grid.addRow(5, endLabel, endField);
        grid.addRow(6, eventLabel, eventBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (titleField.getText().isEmpty() || venueField.getText().isEmpty()) {
                        showWarning("Validation Error", "Title and Venue cannot be empty!");
                        return null;
                    }

                    Session s = new Session();
                    s.setId(UUID.randomUUID());
                    s.setTitle(titleField.getText());
                    s.setDescription(descArea.getText());
                    s.setVenue(venueField.getText());
                    s.setCapacity(capacitySpinner.getValue());

                    LocalDateTime start = LocalDateTime.parse(startField.getText(), formatter);
                    LocalDateTime end = LocalDateTime.parse(endField.getText(), formatter);

                    if (end.isBefore(start)) {
                        showWarning("Validation Error", "End time must be after start time!");
                        return null;
                    }

                    s.setStart(start);
                    s.setEnd(end);

                    Event ev = eventBox.getValue();
                    if (ev != null) {
                        s.setEventId(ev.getId());
                    } else {
                        showWarning("Validation Error", "Please select an event!");
                        return null;
                    }

                    return s;
                } catch (Exception ex) {
                    showError("Parse Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        System.out.println("  ✓ Add session dialog prepared in " + (System.currentTimeMillis() - dialogStart) + " ms");
        dialog.showAndWait().ifPresent(s -> {
            try {
                eventService.createSession(s);
                showInfo("Success", "Session created successfully!");
                loadSessionsAsync();
            } catch (Exception e) {
                showError("Creation Error", e.getMessage());
            }
        });
    }

    @FXML
    public void onUpdateSession() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session to update!");
            return;
        }

        long dialogStart = System.currentTimeMillis();
        System.out.println("✏️ [onUpdateSession] Opening update session dialog for: " + selectedSession.getTitle());

        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Update Session");
        dialog.setHeaderText("Edit session: " + selectedSession.getTitle());

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField(selectedSession.getTitle());
        titleField.setPrefWidth(300);

        Label descLabel = new Label("Description:");
        TextArea descArea = new TextArea(selectedSession.getDescription() != null ? selectedSession.getDescription() : "");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label venueLabel = new Label("Venue:");
        TextField venueField = new TextField(selectedSession.getVenue());
        venueField.setPrefWidth(300);

        Label capacityLabel = new Label("Capacity:");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, selectedSession.getCapacity());

        Label startLabel = new Label("Start (dd/MM/yyyy HH:mm):");
        TextField startField = new TextField(selectedSession.getStart().format(formatter));

        Label endLabel = new Label("End (dd/MM/yyyy HH:mm):");
        TextField endField = new TextField(selectedSession.getEnd().format(formatter));

        Label eventLabel = new Label("Event:");
        ComboBox<Event> eventBox = new ComboBox<>();

        // Lazy load events if not already cached
        long eventStart = System.currentTimeMillis();
        if (eventCache == null || eventCache.isEmpty()) {
            System.out.println("  📦 Loading events for update dialog...");
            Task<List<Event>> task = new Task<>() {
                @Override
                protected List<Event> call() {
                    long dbStart = System.currentTimeMillis();
                    try {
                        List<Event> events = eventService.getEvents();
                        System.out.println("  ✓ eventService.getEvents() took " +
                                (System.currentTimeMillis() - dbStart) + " ms, loaded " + events.size() + " events");
                        return events;
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }
                }
            };

            task.setOnSucceeded(evt -> {
                eventCache = task.getValue();
                eventBox.getItems().addAll(eventCache);
                // Set current value
                for (Event ev : eventCache) {
                    if (ev.getId().equals(selectedSession.getEventId())) {
                        eventBox.setValue(ev);
                        break;
                    }
                }
                System.out.println("  ✓ Event combo populated in " + (System.currentTimeMillis() - eventStart) + " ms");
            });

            Thread t = new Thread(task, "update-session-events-loader");
            t.setDaemon(true);
            t.start();
        } else {
            System.out.println("  ℹ Using cached events (" + eventCache.size() + " events)");
            eventBox.getItems().addAll(eventCache);
            // Set current value
            for (Event ev : eventCache) {
                if (ev.getId().equals(selectedSession.getEventId())) {
                    eventBox.setValue(ev);
                    break;
                }
            }
            System.out.println("  ✓ Event combo populated in " + (System.currentTimeMillis() - eventStart) + " ms");
        }

        eventBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });
        eventBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(400);

        grid.addRow(0, titleLabel, titleField);
        grid.addRow(1, descLabel, descArea);
        grid.addRow(2, venueLabel, venueField);
        grid.addRow(3, capacityLabel, capacitySpinner);
        grid.addRow(4, startLabel, startField);
        grid.addRow(5, endLabel, endField);
        grid.addRow(6, eventLabel, eventBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (titleField.getText().isEmpty() || venueField.getText().isEmpty()) {
                        showWarning("Validation Error", "Title and Venue cannot be empty!");
                        return null;
                    }

                    selectedSession.setTitle(titleField.getText());
                    selectedSession.setDescription(descArea.getText());
                    selectedSession.setVenue(venueField.getText());
                    selectedSession.setCapacity(capacitySpinner.getValue());

                    LocalDateTime start = LocalDateTime.parse(startField.getText(), formatter);
                    LocalDateTime end = LocalDateTime.parse(endField.getText(), formatter);

                    if (end.isBefore(start)) {
                        showWarning("Validation Error", "End time must be after start time!");
                        return null;
                    }

                    selectedSession.setStart(start);
                    selectedSession.setEnd(end);

                    Event ev = eventBox.getValue();
                    if (ev != null) {
                        selectedSession.setEventId(ev.getId());
                    }

                    return selectedSession;
                } catch (Exception ex) {
                    showError("Parse Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        System.out.println("  ✓ Update session dialog prepared in " + (System.currentTimeMillis() - dialogStart) + " ms");
        dialog.showAndWait().ifPresent(s -> {
            try {
                eventService.updateSession(s);
                showInfo("Success", "Session updated successfully!");
                loadSessionsAsync();
            } catch (Exception e) {
                showError("Update Error", e.getMessage());
            }
        });
    }

    @FXML
    public void onDeleteSession() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session to delete!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Session");
        alert.setContentText("Are you sure you want to delete session: " + selectedSession.getTitle() + "?");

        if (alert.showAndWait().isPresent() && alert.showAndWait().get() == ButtonType.OK) {
            try {
                eventService.deleteSession(selectedSession.getId());
                showInfo("Success", "Session deleted successfully!");
                loadSessionsAsync();
                clearDetails();
                selectedSession = null;
            } catch (Exception e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    @FXML
    public void onAddPresenter() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session first!");
            return;
        }

        Dialog<Presenter> dialog = new Dialog<>();
        dialog.setTitle("Add Presenter to Session");
        dialog.setHeaderText("Assign presenter to: " + selectedSession.getTitle());

        Label presenterLabel = new Label("Presenter:");
        ComboBox<Presenter> presenterBox = new ComboBox<>();

        long presStart = System.currentTimeMillis();
        System.out.println("🎭 [onAddPresenter] Loading presenters for combo box...");

        // Lazy load presenters only when needed - use cache if available
        List<Presenter> presenters;
        if (presenterCache != null && !presenterCache.isEmpty()) {
            System.out.println("  ℹ Using cached presenters (" + presenterCache.size() + " presenters)");
            presenters = new ArrayList<>(presenterCache.values());
            presenterBox.getItems().addAll(presenters);
            System.out.println("  ✓ Presenter combo populated in " + (System.currentTimeMillis() - presStart) + " ms");
        } else {
            // Load on demand in background
            System.out.println("  📦 Fetching presenters from database...");
            Task<List<Presenter>> task = new Task<>() {
                @Override
                protected List<Presenter> call() {
                    long dbStart = System.currentTimeMillis();
                    try {
                        List<Presenter> pres = identityService.getAllPresenters();
                        System.out.println("  ✓ identityService.getAllPresenters() took " +
                                (System.currentTimeMillis() - dbStart) + " ms, loaded " + pres.size() + " presenters");
                        return pres;
                    } catch (Exception e) {
                        System.err.println("✗ Error loading presenters: " + e.getMessage());
                        return new ArrayList<>();
                    }
                }
            };

            task.setOnSucceeded(evt -> {
                long cacheStart = System.currentTimeMillis();
                List<Presenter> pres = task.getValue();
                presenterCache = pres.stream()
                        .collect(Collectors.toMap(Presenter::getId, p -> p));
                presenterBox.getItems().addAll(pres);
                System.out.println("  ✓ Presenter cache built in " + (System.currentTimeMillis() - cacheStart) + " ms");
                System.out.println("  ✓ Presenter combo populated in " + (System.currentTimeMillis() - presStart) + " ms");
            });

            Thread t = new Thread(task, "presenter-loader");
            t.setDaemon(true);
            t.start();
        }

        presenterBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });
        presenterBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, presenterLabel, presenterBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? presenterBox.getValue() : null);

        dialog.showAndWait().ifPresent(presenter -> {
            try {
                if (eventService.addPresenterToSession(presenter.getId(), selectedSession.getId())) {
                    showInfo("Success", "Presenter added successfully!");
                    displaySessionDetails(selectedSession);
                } else {
                    showWarning("Conflict", "Presenter has a conflicting schedule!");
                }
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    public void backToDashboard() {
        SceneManager.switchTo("admin_dashboard.fxml", "EMS - Admin Dashboard");
    }

    // Helper methods for dialogs
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
