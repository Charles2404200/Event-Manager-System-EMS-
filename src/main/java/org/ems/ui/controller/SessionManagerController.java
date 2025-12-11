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

        // Load data asynchronously
        asyncLoader.preloadPresentersAsync(() -> {
            System.out.println("  ✓ Presenters pre-loaded");
        });

        loadSessionsAsync();

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
     * Async load sessions - delegated to asyncLoader
     */
    private void loadSessionsAsync() {
        long mainStart = System.currentTimeMillis();
        System.out.println("📋 [SessionManager] Loading sessions asynchronously...");

        asyncLoader.loadSessionsAsync(sessions -> {
            // On success: initialize pagination and display sessions
            paginationManager.initializePagination(sessions);
            displaySessionsPage();

            if (sessionCountLabel != null) {
                sessionCountLabel.setText("Total Sessions: " + sessions.size());
            }

            // Load events in background after displaying sessions
            asyncLoader.loadEventsAsync(events -> {
                tableManager.setupEventNameMapping();
                System.out.println("✓ Events loaded and table updated");
            }, ex -> {
                System.err.println("✗ Error loading events: " + ex.getMessage());
            });

            System.out.println("✓ Sessions loaded in " + (System.currentTimeMillis() - mainStart) + " ms");
        }, ex -> {
            System.err.println("✗ Error loading sessions: " + ex.getMessage());
            showError("Error loading sessions", ex.getMessage());
        });
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
        paginationManager.reset();
        cacheManager.clearAll();
        presenterManager.clearCache();
        eventManager.clearCache();
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

        List<Event> events = eventManager.getAllEvents();
        Optional<Session> result = SessionDialogFactory.showUpdateSessionDialog(selectedSession, events);

        System.out.println("  ✓ Update session dialog prepared in " + (System.currentTimeMillis() - dialogStart) + " ms");

        result.ifPresent(s -> {
            try {
                sessionService.updateSession(s);
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
                sessionService.deleteSession(selectedSession.getId());
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
