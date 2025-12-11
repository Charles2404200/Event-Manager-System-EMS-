package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.ems.application.service.*;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.*;

import java.util.List;
import java.util.Optional;

/**
 * SessionManagerController - Manages session UI and event routing
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

    // Services
    private SessionManagementService sessionService;
    private SessionEventManager eventManager;
    private SessionPresenterManager presenterManager;
    private SessionCacheManager cacheManager;
    private SessionAsyncLoader asyncLoader;
    private SessionTableManager tableManager;
    private SessionPaginationManager paginationManager;

    private Session selectedSession;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("⚙️ [SessionManager] initialize() starting...");

        // Initialize services
        initializeServices();

        // Setup UI components
        tableManager.setupTableColumns();
        paginationManager.setPageInfoLabel(pageInfoLabel);
        asyncLoader.setProgressUI(loadingContainer, loadingProgressBar, loadingStatusLabel);

        // Setup table selection listener
        sessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedSession = newVal;
                displaySessionDetails(newVal);
            }
        });

        // ⚡ OPTIMIZATION: Try to use cached data first for instant display
        List<Session> cachedSessions = cacheManager.getSessions();
        if (cachedSessions != null && cacheManager.isSessionsCacheValid()) {
            System.out.println("⚡ [Init] Using cached sessions for instant display");
            paginationManager.initializePagination(cachedSessions);
            displaySessionsPage();
            if (sessionCountLabel != null) {
                sessionCountLabel.setText("Total Sessions: " + cachedSessions.size());
            }
        }

        // Load data asynchronously (parallel: sessions + events + presenters)
        loadSessionsAndEventsParallel();

        System.out.println("  ✓ initialize() completed in " + (System.currentTimeMillis() - initStart) + " ms");
    }

    /**
     * Initialize all service classes
     */
    private void initializeServices() {
        this.cacheManager = new SessionCacheManager();
        this.sessionService = new SessionManagementService();
        this.eventManager = new SessionEventManager(cacheManager);
        this.presenterManager = new SessionPresenterManager(cacheManager);
        this.asyncLoader = new SessionAsyncLoader(sessionService, eventManager, presenterManager);
        this.tableManager = new SessionTableManager(sessionTable, colId, colTitle, colEvent, colStart, colVenue, colCapacity, eventManager);
        this.paginationManager = new SessionPaginationManager();
    }

    /**
     * ⚡ Load sessions and events in PARALLEL (faster than sequential)
     */
    private void loadSessionsAndEventsParallel() {
        long mainStart = System.currentTimeMillis();
        System.out.println("⚡ [SessionManager] Loading sessions & events in PARALLEL...");

        // Start both loads at the same time
        asyncLoader.loadSessionsAsync(sessions -> {
            // Update cache
            cacheManager.setSessions(sessions);

            // On success: initialize pagination and display sessions
            paginationManager.initializePagination(sessions);
            displaySessionsPage();

            if (sessionCountLabel != null) {
                sessionCountLabel.setText("Total Sessions: " + sessions.size());
            }

            System.out.println("✓ Sessions loaded in " + (System.currentTimeMillis() - mainStart) + " ms");
        }, ex -> {
            System.err.println("✗ Error loading sessions: " + ex.getMessage());
            showError("Error loading sessions", ex.getMessage());
        });

        // Load events at same time (parallel)
        asyncLoader.loadEventsAsync(events -> {
            cacheManager.setEvents(events);
            tableManager.setupEventNameMapping();
            System.out.println("✓ Events loaded in " + (System.currentTimeMillis() - mainStart) + " ms");
        }, ex -> {
            System.err.println("✗ Error loading events: " + ex.getMessage());
        });

        // Lazy load presenters in background (only when needed)
        lazyLoadPresentersInBackground();
    }

    /**
     * ⚡ Lazy load presenters only when needed (on first detail view)
     */
    private void lazyLoadPresentersInBackground() {
        Thread lazyLoaderThread = new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay to let UI settle
                long start = System.currentTimeMillis();
                presenterManager.getAllPresentersAsMap();
                System.out.println("✓ Presenters pre-loaded in background: " +
                    (System.currentTimeMillis() - start) + " ms");
            } catch (Exception e) {
                System.err.println("⚠️ Background presenter load failed: " + e.getMessage());
            }
        }, "lazy-presenter-loader");
        lazyLoaderThread.setDaemon(true);
        lazyLoaderThread.start();
    }



    /**
     * Display current page from pagination manager
     */
    private void displaySessionsPage() {
        long start = System.currentTimeMillis();
        System.out.println("📄 [SessionManager] Displaying current page...");

        List<Session> pageData = paginationManager.getCurrentPageData();
        ObservableList<Session> sessions = FXCollections.observableArrayList(pageData);
        sessionTable.setItems(sessions);

        System.out.println("  ✓ Displayed " + pageData.size() + " sessions in " +
                (System.currentTimeMillis() - start) + " ms");
    }

    @FXML
    public void onPrevPage() {
        if (paginationManager.previousPage()) {
            displaySessionsPage();
        }
    }

    @FXML
    public void onNextPage() {
        if (paginationManager.nextPage()) {
            displaySessionsPage();
        }
    }

    @FXML
    public void onRefresh() {
        long refreshStart = System.currentTimeMillis();
        System.out.println("🔄 [onRefresh] Refreshing session data...");

        paginationManager.reset();

        // ⚡ OPTIMIZATION: Use cache check - only reload if cache expired
        boolean sessionsCacheValid = cacheManager.isSessionsCacheValid();
        boolean eventsCacheValid = cacheManager.isEventsCacheValid();

        if (sessionsCacheValid && eventsCacheValid) {
            System.out.println("⚡ [Refresh] Cache still valid - skipping database query");
            List<Session> cachedSessions = cacheManager.getSessions();
            paginationManager.initializePagination(cachedSessions);
            displaySessionsPage();
            System.out.println("✓ Refresh completed in " + (System.currentTimeMillis() - refreshStart) + " ms");
            return;
        }

        // Cache expired - reload from database
        System.out.println("📊 [Refresh] Cache expired - reloading from database");
        cacheManager.clearAll();
        presenterManager.clearCache();
        eventManager.clearCache();
        loadSessionsAndEventsParallel();

        selectedSession = null;
        clearDetails();
        System.out.println("✓ Full refresh completed in " + (System.currentTimeMillis() - refreshStart) + " ms");
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

    private void displaySessionDetails(Session session) {
        long detailStart = System.currentTimeMillis();
        System.out.println("📄 [SessionManager] Displaying session details: " + session.getTitle());

        try {
            long basicStart = System.currentTimeMillis();
            detailIdLabel.setText(session.getId().toString());
            detailTitleLabel.setText(session.getTitle());
            detailDescLabel.setText(session.getDescription() != null ? session.getDescription() : "No description");
            detailStartLabel.setText(session.getStart().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            detailEndLabel.setText(session.getEnd().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            detailVenueLabel.setText(session.getVenue());
            detailCapacityLabel.setText(String.valueOf(session.getCapacity()));
            System.out.println("  ✓ Basic labels updated in " + (System.currentTimeMillis() - basicStart) + " ms");

            // Get presenter names from presenter manager
            long presenterStart = System.currentTimeMillis();
            List<String> presenterNames = presenterManager.getPresentersForSession(session);
            presenterListView.setItems(FXCollections.observableList(presenterNames));
            System.out.println("  ✓ Presenter names retrieved in " + (System.currentTimeMillis() - presenterStart) +
                    " ms (" + presenterNames.size() + " presenters)");

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

        List<Event> events = eventManager.getAllEvents();
        Optional<Session> result = SessionDialogFactory.showAddSessionDialog(events);

        System.out.println("  ✓ Add session dialog prepared in " + (System.currentTimeMillis() - dialogStart) + " ms");

        result.ifPresent(s -> {
            try {
                sessionService.createSession(s);
                showInfo("Success", "Session created successfully!");
                loadSessionsAndEventsParallel();
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

        List<Event> events = eventManager.getAllEvents();
        Optional<Session> result = SessionDialogFactory.showUpdateSessionDialog(selectedSession, events);

        System.out.println("  ✓ Update session dialog prepared in " + (System.currentTimeMillis() - dialogStart) + " ms");

        result.ifPresent(s -> {
            try {
                sessionService.updateSession(s);
                showInfo("Success", "Session updated successfully!");
                loadSessionsAndEventsParallel();
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
                sessionService.deleteSession(selectedSession.getId());
                showInfo("Success", "Session deleted successfully!");
                loadSessionsAndEventsParallel();
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

        long presStart = System.currentTimeMillis();
        System.out.println("🎭 [onAddPresenter] Loading presenters for dialog...");

        // Load presenters (will use cache if valid)
        List<Presenter> presenters = presenterManager.getAllPresenters();

        Optional<Presenter> result = SessionDialogFactory.showAddPresenterDialog(
                selectedSession.getTitle(), presenters);

        System.out.println("  ✓ Presenter selection completed in " + (System.currentTimeMillis() - presStart) + " ms");

        result.ifPresent(presenter -> {
            try {
                if (presenterManager.addPresenterToSession(presenter.getId(), selectedSession.getId())) {
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
    public void onUploadMaterial() {
        long uploadStart = System.currentTimeMillis();
        System.out.println("📤 [onUploadMaterial] Upload material clicked");

        // Check if session is selected
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session to upload material!");
            return;
        }

        System.out.println("  ✓ Selected session: " + selectedSession.getTitle());

        // Step 1: Prompt for file selection
        String filePath = promptForMaterialFile();
        if (filePath == null) {
            System.out.println("  ⚠️ Material file selection cancelled");
            return;
        }
        System.out.println("  ✓ Selected file: " + filePath);

        // Step 2: Show loading dialog
        javafx.stage.Stage primaryStage = null;
        try {
            if (sessionTable != null && sessionTable.getScene() != null) {
                primaryStage = (javafx.stage.Stage) sessionTable.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("Could not get primary stage: " + e.getMessage());
        }

        LoadingDialog uploadDialog = null;
        if (primaryStage != null) {
            uploadDialog = new LoadingDialog(primaryStage, "Uploading material to Cloudflare R2...");
            uploadDialog.show();
        }

        // Step 3: Upload material asynchronously
        final LoadingDialog finalUploadDialog = uploadDialog;
        final String sessionId = selectedSession.getId().toString();
        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    long taskStart = System.currentTimeMillis();
                    try {
                        System.out.println("  🔄 [Background] Uploading material to session: " + selectedSession.getTitle());
                        String materialPath = uploadSessionMaterial(sessionId, filePath);
                        System.out.println("  ✓ Material uploaded in " + (System.currentTimeMillis() - taskStart) + "ms");
                        System.out.println("  ✓ Material path: " + materialPath);
                        return materialPath;
                    } catch (Exception e) {
                        System.err.println("  ✗ Upload error: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },

                // Success callback
                result -> {
                    if (finalUploadDialog != null) {
                        finalUploadDialog.close();
                    }
                    System.out.println("✓ Upload completed in " + (System.currentTimeMillis() - uploadStart) + "ms");
                    showInfo("Material uploaded successfully!",
                            "Your material has been saved to session: " + selectedSession.getTitle() +
                            "\n\nPath: " + result);
                    displaySessionDetails(selectedSession);
                },

                // Error callback
                error -> {
                    if (finalUploadDialog != null) {
                        finalUploadDialog.close();
                    }
                    System.err.println("✗ Upload failed: " + error.getMessage());
                    showError("Upload Failed",
                            "Failed to upload material:\n\n" + error.getMessage());
                }
        );
    }

    /**
     * Upload session material and save to database
     * @param sessionId Session ID
     * @param filePath Path to material file
     * @return Material path
     */
    private String uploadSessionMaterial(String sessionId, String filePath) throws Exception {
        // Use ImageService to upload to Cloudflare R2
        org.ems.application.service.ImageService imageService =
            new org.ems.application.impl.ImageServiceImpl();
        java.util.UUID sessionUUID = java.util.UUID.fromString(sessionId);
        String materialPath = imageService.uploadSessionMaterial(filePath, sessionUUID);

        if (materialPath == null) {
            throw new Exception("Material upload failed");
        }

        // Get session and update material_path
        Session session = AppContext.get().sessionRepo.findById(sessionUUID);
        if (session == null) {
            throw new Exception("Session not found");
        }

        session.setMaterialPath(materialPath);
        AppContext.get().sessionRepo.save(session);

        System.out.println("  ✓ Material path saved to database: " + materialPath);
        return materialPath;
    }

    /**
     * Open file chooser to select material file
     * @return File path or null if cancelled
     */
    private String promptForMaterialFile() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) sessionTable.getScene().getWindow();
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select material to upload");

            // Add file type filters
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"),
                    new javafx.stage.FileChooser.ExtensionFilter("PowerPoint", "*.ppt", "*.pptx"),
                    new javafx.stage.FileChooser.ExtensionFilter("Word", "*.doc", "*.docx"),
                    new javafx.stage.FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx"),
                    new javafx.stage.FileChooser.ExtensionFilter("Text", "*.txt"),
                    new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );

            java.io.File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                return selectedFile.getAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("Cannot select file: " + e.getMessage());
        }
        return null;
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
