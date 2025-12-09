package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import org.ems.config.AppContext;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.repository.SessionRepository;
import org.ems.ui.stage.SceneManager;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AssignedSessionsController {

    // === FXML bindings =======================================================

    @FXML
    private ListView<Session> assignedSessionsListView;

    @FXML
    private Label errorLabel;

    @FXML
    private Button prevButton;

    @FXML
    private Button nextButton;

    @FXML
    private Label pageLabel;

    @FXML
    private Label detailIdLabel;

    @FXML
    private Label detailTitleLabel;

    @FXML
    private Label detailDescriptionLabel;

    @FXML
    private Label detailStartLabel;

    @FXML
    private Label detailEndLabel;

    @FXML
    private Label detailVenueLabel;

    @FXML
    private Label detailCapacityLabel;

    // === Pagination config ===================================================

    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;

    // === App context + repository + current presenter ========================

    private final AppContext appContext = AppContext.get();
    private Presenter presenter;
    private SessionRepository sessionRepo;

    // === App-level cache for this screen ====================================

    /**
     * Cache sessions by id for O(1) lookup to avoid repeated DB queries.
     */
    private final Map<UUID, Session> sessionCache = new HashMap<>();

    /**
     * Data currently displayed in the ListView for the current page.
     */
    private final ObservableList<Session> pageSessions =
            FXCollections.observableArrayList();

    // ========================================================================
    // STEP 1 — initialize(): setup UI, pagination and load first page
    // ========================================================================
    @FXML
    public void initialize() {

        long uiStart = System.currentTimeMillis();

        // Bind observable list to ListView
        assignedSessionsListView.setItems(pageSessions);

        // Validate current user role
        if (!(appContext.currentUser instanceof Presenter)) {
            errorLabel.setText("Current user is not a presenter.");
            disablePagination();
            System.out.println("[AssignedSessions] initialize() aborted: current user is not a presenter. UI setup took "
                    + (System.currentTimeMillis() - uiStart) + " ms");
            return;
        }

        presenter = (Presenter) appContext.currentUser;
        sessionRepo = appContext.sessionRepo;

        if (sessionRepo == null) {
            errorLabel.setText("Session repository is not available (offline mode).");
            disablePagination();
            System.out.println("[AssignedSessions] initialize() aborted: sessionRepo is null. UI setup took "
                    + (System.currentTimeMillis() - uiStart) + " ms");
            return;
        }

        setupListView();
        initPagination();

        System.out.println("[AssignedSessions] UI initialized in "
                + (System.currentTimeMillis() - uiStart) + " ms");

        // Load the first page (page 0)
        loadPageAsync();
    }

    // ========================================================================
    // STEP 1.1 — Setup ListView (cell factory, selection listener)
    // ========================================================================
    private void setupListView() {
        // Do not query DB inside cell; only use data already in pageSessions
        assignedSessionsListView.setCellFactory(listView -> new ListCell<>() {
            private final DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            @Override
            protected void updateItem(Session session, boolean empty) {
                super.updateItem(session, empty);

                if (empty || session == null) {
                    setText(null);
                } else {
                    String line = "Session: " + session.getTitle()
                            + " | Date: " + session.getStart().format(formatter);
                    setText(line);
                }
            }
        });

        // When selection changes, show details in the right panel
        assignedSessionsListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> displayDetails(newSel));
    }

    // ========================================================================
    // STEP 1.2 — Pagination UI initialization
    // ========================================================================
    private void initPagination() {
        prevButton.setOnAction(e -> {
            if (currentPage > 0) {
                currentPage--;
                loadPageAsync();
            }
        });

        nextButton.setOnAction(e -> {
            currentPage++;
            loadPageAsync();
        });

        updatePaginationUI(false);
    }

    private void updatePaginationUI(boolean hasNextPage) {
        prevButton.setDisable(currentPage == 0);
        nextButton.setDisable(!hasNextPage);
        pageLabel.setText("Page " + (currentPage + 1));
    }

    private void disablePagination() {
        prevButton.setDisable(true);
        nextButton.setDisable(true);
        pageLabel.setText("Page 1");
    }

    // ========================================================================
    // STEP 2 — loadPageAsync(): paginate query, rebuild cache, update ListView
    // ========================================================================
    private void loadPageAsync() {
        errorLabel.setText("");

        if (presenter == null || sessionRepo == null) {
            errorLabel.setText("Cannot load sessions (no presenter or repository).");
            disablePagination();
            System.out.println("[AssignedSessions] loadPageAsync() aborted: presenter or sessionRepo is null");
            return;
        }

        final int offset = currentPage * PAGE_SIZE;
        final String runId = "AssignedSessionsPage-" + currentPage + "-" + System.currentTimeMillis();
        final long asyncStart = System.currentTimeMillis();

        // Database query must run on a background thread (Task)
        Task<List<Session>> task = new Task<>() {
            @Override
            protected List<Session> call() {
                long startDb = System.currentTimeMillis();
                List<Session> result = sessionRepo.findByPresenter(
                        presenter.getId(),
                        offset,
                        PAGE_SIZE
                );
                long tookDb = System.currentTimeMillis() - startDb;
                int size = (result != null ? result.size() : 0);
                System.out.println("[" + runId + "] sessionRepo.findByPresenter(presenterId="
                        + presenter.getId() + ", offset=" + offset + ", limit=" + PAGE_SIZE
                        + ") took " + tookDb + " ms, result size=" + size);
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            List<Session> sessions = task.getValue();
            long totalTook = System.currentTimeMillis() - asyncStart;
            int size = (sessions != null ? sessions.size() : 0);

            // Rebuild cache from the current page
            sessionCache.clear();
            if (sessions != null) {
                for (Session s : sessions) {
                    sessionCache.put(s.getId(), s);
                }
                // Update ListView for this page
                pageSessions.setAll(sessions);
            } else {
                pageSessions.clear();
            }

            // If we got exactly PAGE_SIZE items, there might be a next page
            boolean hasNextPage = size == PAGE_SIZE;
            updatePaginationUI(hasNextPage);

            // Case 1: No sessions at all for this presenter (first page empty)
            if ((sessions == null || sessions.isEmpty()) && currentPage == 0) {
                pageSessions.clear();
                clearDetails();
                errorLabel.setText("No session assigned to you.");
                disablePagination();
                System.out.println("[" + runId + "] No sessions assigned for this presenter. Total loadPageAsync took "
                        + totalTook + " ms");
                return;
            }

            // Case 2: Page index is too large (beyond the last page)
            if ((sessions == null || sessions.isEmpty()) && currentPage > 0) {
                currentPage--;
                updatePaginationUI(false);
            }

            System.out.println("[" + runId + "] loadPageAsync for page " + currentPage
                    + " completed in " + totalTook + " ms (display size=" + size + ")");
        });

        task.setOnFailed(e -> {
            long totalTook = System.currentTimeMillis() - asyncStart;
            Throwable ex = task.getException();
            errorLabel.setText("Failed to load sessions: "
                    + (ex != null ? ex.getMessage() : "Unknown error"));
            System.err.println("[" + runId + "] loadPageAsync FAILED in "
                    + totalTook + " ms: " + (ex != null ? ex.getMessage() : "Unknown error"));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread t = new Thread(task, "assigned-sessions-loader-page-" + currentPage);
        t.setDaemon(true);
        t.start();
    }

    // ========================================================================
    // STEP 3 — displayDetails(): show details of selected session
    // ========================================================================
    private void displayDetails(Session session) {
        if (session == null) {
            clearDetails();
            return;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // ID
        detailIdLabel.setText(
                session.getId() != null ? session.getId().toString() : ""
        );

        // Title
        detailTitleLabel.setText(
                session.getTitle() != null ? session.getTitle() : ""
        );

        // Description
        detailDescriptionLabel.setText(
                session.getDescription() != null ? session.getDescription() : ""
        );

        // Start / End
        detailStartLabel.setText(
                session.getStart() != null ? session.getStart().format(formatter) : ""
        );

        detailEndLabel.setText(
                session.getEnd() != null ? session.getEnd().format(formatter) : ""
        );

        // Venue
        detailVenueLabel.setText(
                session.getVenue() != null ? session.getVenue() : ""
        );

        // Capacity
        detailCapacityLabel.setText(String.valueOf(session.getCapacity()));
    }

    private void clearDetails() {
        detailIdLabel.setText("");
        detailTitleLabel.setText("");
        detailDescriptionLabel.setText("");
        detailStartLabel.setText("");
        detailEndLabel.setText("");
        detailVenueLabel.setText("");
        detailCapacityLabel.setText("");
    }

    // ========================================================================
    // STEP 4 — Back navigation
    // ========================================================================
    @FXML
    private void onBack() {
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }
}
