package org.ems.application.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.*;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;

import java.awt.Desktop;
import java.io.File;
import java.util.*;

/**
 * Register Sessions Page
 * OPTIMIZED: Async loading, batch queries, consistent with other pages
 * @author <your group number>
 */
public class MyRegistrationsController {

    @FXML private ComboBox<String> eventCombo;
    @FXML private VBox sessionsContainer;
    @FXML private Label recordCountLabel;
    @FXML private VBox loadingPlaceholder;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingPercentLabel;
    @FXML private VBox registeredSessionsContainer;
    @FXML private VBox materialsContainer;

    private final ObservableList<SessionRow> sessionRows = FXCollections.observableArrayList();
    private final Map<String, UUID> eventMap = new HashMap<>();
    private AppContext appContext;
    private Set<UUID> registeredSessionIds = new HashSet<>();

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("📋 [MyRegistrations] initialize() starting...");
        try {
            appContext = AppContext.get();

            // Hide loading placeholder initially
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(false);
                loadingPlaceholder.setManaged(false);
            }

            System.out.println("  ✓ UI setup in " + (System.currentTimeMillis() - initStart) + " ms");
            System.out.println("  🔄 Starting async load...");

            // Load events with tickets asynchronously
            loadEventsWithTicketsAsync();

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all events where user has tickets - ASYNC
     */
    private void loadEventsWithTicketsAsync() {
        long asyncStart = System.currentTimeMillis();

        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background] Loading events with tickets...");

                    List<String> eventNames = new ArrayList<>();
                    Map<String, UUID> tempEventMap = new HashMap<>();

                    try {
                        if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null && appContext.eventRepo != null) {
                            Attendee attendee = (Attendee) appContext.currentUser;

                            // Step 1: Get all tickets for attendee
                            long ticketStart = System.currentTimeMillis();
                            List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());
                            long ticketTime = System.currentTimeMillis() - ticketStart;
                            System.out.println("    ✓ Loaded " + tickets.size() + " tickets in " + ticketTime + " ms");

                            if (tickets == null || tickets.isEmpty()) {
                                System.out.println("    ℹ No tickets found");
                                return new Object[]{eventNames, tempEventMap};
                            }

                            // Step 2: Get unique event IDs from tickets
                            Set<UUID> eventIds = new HashSet<>();
                            for (Ticket ticket : tickets) {
                                if (ticket.getEventId() != null) {
                                    eventIds.add(ticket.getEventId());
                                }
                            }
                            System.out.println("    ✓ Found " + eventIds.size() + " unique events");

                            // Step 3: Load all events ONCE (batch)
                            long eventStart = System.currentTimeMillis();
                            List<Event> allEvents = appContext.eventRepo.findAll();
                            long eventTime = System.currentTimeMillis() - eventStart;
                            System.out.println("    ✓ Loaded all " + allEvents.size() + " events in " + eventTime + " ms");

                            // Step 4: Match tickets to events (in-memory)
                            for (UUID eventId : eventIds) {
                                Event event = allEvents.stream()
                                    .filter(e -> e.getId().equals(eventId))
                                    .findFirst()
                                    .orElse(null);

                                if (event != null && event.getName() != null) {
                                    String display = event.getName();
                                    eventNames.add(display);
                                    tempEventMap.put(display, eventId);
                                }
                            }
                            System.out.println("    ✓ Matched " + eventNames.size() + " events");
                        }
                    } catch (Exception e) {
                        System.err.println("    ✗ Error loading events: " + e.getMessage());
                        e.printStackTrace();
                    }

                    System.out.println("    ✓ Background task completed in " + (System.currentTimeMillis() - taskStart) + " ms");
                    return new Object[]{eventNames, tempEventMap};
                },
                result -> {
                    long uiStart = System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    Object[] data = (Object[]) result;
                    @SuppressWarnings("unchecked")
                    List<String> eventNames = (List<String>) data[0];
                    @SuppressWarnings("unchecked")
                    Map<String, UUID> tempEventMap = (Map<String, UUID>) data[1];

                    eventMap.putAll(tempEventMap);
                    eventCombo.setItems(FXCollections.observableArrayList(eventNames));

                    if (!eventNames.isEmpty()) {
                        eventCombo.setValue(eventNames.get(0));
                        loadSessionsForEvent(eventMap.get(eventNames.get(0)));
                        loadRegisteredSessionsForEvent(eventMap.get(eventNames.get(0)));
                    } else {
                        Label noTicketsLabel = new Label("No tickets yet. Buy a ticket first to register for sessions!");
                        noTicketsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                        sessionsContainer.getChildren().clear();
                        sessionsContainer.getChildren().add(noTicketsLabel);
                    }

                    // Setup event combo listener
                    eventCombo.setOnAction(e -> {
                        String selected = eventCombo.getValue();
                        if (selected != null && eventMap.containsKey(selected)) {
                            loadSessionsForEvent(eventMap.get(selected));
                            loadRegisteredSessionsForEvent(eventMap.get(selected));
                        }
                    });

                    System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
                    System.out.println("✓ MyRegistrations loaded successfully");
                },
                error -> {
                    System.err.println("✗ Error loading events: " + error.getMessage());
                    showAlert("Error", "Failed to load events: " + error.getMessage());
                }
        );
    }

    /**
     * Load all sessions for selected event - ASYNC
     */
    private void loadSessionsForEvent(UUID eventId) {
        long loadStart = System.currentTimeMillis();
        System.out.println("🎤 [MyRegistrations] Loading sessions for event: " + eventId);

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background] Loading sessions...");

                    List<Session> sessions = new ArrayList<>();
                    try {
                        if (appContext.sessionRepo != null) {
                            sessions = appContext.sessionRepo.findByEvent(eventId);
                            long queryTime = System.currentTimeMillis() - taskStart;
                            System.out.println("    ✓ Loaded " + sessions.size() + " sessions in " + queryTime + " ms");
                        }
                    } catch (Exception e) {
                        System.err.println("    ✗ Error loading sessions: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return sessions;
                },
                sessions -> {
                    long uiStart = System.currentTimeMillis();
                    displaySessions(sessions);
                    System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
                    System.out.println("✓ Sessions loaded in " + (System.currentTimeMillis() - loadStart) + " ms");
                },
                error -> {
                    showTable();
                    System.err.println("✗ Error loading sessions: " + error.getMessage());
                    showAlert("Error", "Failed to load sessions: " + error.getMessage());
                }
        );
    }

    /**
     * Load registered sessions for attendee for selected event - ASYNC
     */
    private void loadRegisteredSessionsForEvent(UUID eventId) {
        if (!(appContext.currentUser instanceof Attendee)) return;
        Attendee attendee = (Attendee) appContext.currentUser;

        AsyncTaskService.runAsync(
            () -> {
                List<Session> registered = new ArrayList<>();
                try {
                    if (appContext.sessionRepo != null) {
                        registered = appContext.sessionRepo.findSessionsByAttendeeAndEvent(attendee.getId(), eventId);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading registered sessions: " + e.getMessage());
                }
                return registered;
            },
                result -> {
                registeredSessionIds.clear();
                for (Session s : (List<Session>) result) {
                    registeredSessionIds.add(s.getId());
                }
                // Refresh table to update button states
                Platform.runLater(() -> {
                    for (var node : sessionsContainer.getChildren()) {
                        if (node instanceof TableView<?>) {
                            ((TableView<?>) node).refresh();
                        }
                    }
                });
            },
            error -> System.err.println("Error loading registered sessions: " + error.getMessage())
        );
    }

    /**
     * Display sessions UI - Using TableView with material links
     */
    private void displaySessions(List<Session> sessions) {
        Platform.runLater(() -> {
            sessionsContainer.getChildren().clear();
            sessionRows.clear();

            if (sessions == null || sessions.isEmpty()) {
                Label noSessionsLabel = new Label("No sessions available for this event");
                noSessionsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                sessionsContainer.getChildren().add(noSessionsLabel);
                recordCountLabel.setText("Total Sessions: 0");
                showTable();
                return;
            }

            // Action buttons - Register All & Cancel All
            HBox actionButtonsBox = new HBox(10);
            actionButtonsBox.setPadding(new Insets(10));
            actionButtonsBox.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

            Button registerAllBtn = new Button("✓ Register All Sessions");
            registerAllBtn.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand; -fx-background-color: #27ae60; -fx-text-fill: white; -fx-border-radius: 3;");
            registerAllBtn.setOnAction(e -> registerAllSessions(sessions));

            Button cancelAllBtn = new Button("❌ Cancel All Sessions");
            cancelAllBtn.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand; -fx-background-color: #e74c3c; -fx-text-fill: white; -fx-border-radius: 3;");
            cancelAllBtn.setOnAction(e -> cancelAllSessions(sessions));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            actionButtonsBox.getChildren().addAll(registerAllBtn, cancelAllBtn, spacer);
            sessionsContainer.getChildren().add(actionButtonsBox);

            // Create TableView
            TableView<SessionRow> table = new TableView<>();
            table.setItems(sessionRows);
            table.setPrefHeight(400);
            table.setMinHeight(300);
            table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            table.setStyle("-fx-font-size: 11;");

            // Tên Session
            TableColumn<SessionRow, String> titleCol = new TableColumn<>("Session Name");
            titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
            titleCol.setMinWidth(150);
            titleCol.setPrefWidth(200);

            // Thời gian
            TableColumn<SessionRow, String> timeCol = new TableColumn<>("Time");
            timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
            timeCol.setMinWidth(80);
            timeCol.setPrefWidth(100);

            // Địa điểm
            TableColumn<SessionRow, String> venueCol = new TableColumn<>("Venue");
            venueCol.setCellValueFactory(new PropertyValueFactory<>("venue"));
            venueCol.setMinWidth(100);
            venueCol.setPrefWidth(120);

            // Số ghế
            TableColumn<SessionRow, String> capacityCol = new TableColumn<>("Capacity");
            capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
            capacityCol.setMinWidth(80);
            capacityCol.setPrefWidth(80);

            // Material - Link View/Download cho từng session
            TableColumn<SessionRow, String> materialCol = new TableColumn<>("Material");
            materialCol.setMinWidth(140);
            materialCol.setPrefWidth(160);
            materialCol.setCellValueFactory(new PropertyValueFactory<>("materialPath"));
            materialCol.setCellFactory(col -> new TableCell<SessionRow, String>() {
                @Override
                protected void updateItem(String path, boolean empty) {
                    super.updateItem(path, empty);
                    if (empty || path == null || path.isEmpty()) {
                        setGraphic(null);
                        setText("N/A");
                    } else {
                        HBox btnBox = new HBox(5);
                        btnBox.setStyle("-fx-alignment: center;");
                        Button viewBtn = new Button("👁 View");
                        viewBtn.setPrefWidth(45);
                        viewBtn.setStyle("-fx-padding: 3 5; -fx-font-size: 9;");
                        viewBtn.setOnAction(e -> viewMaterial(path, "Material"));

                        Button downloadBtn = new Button("⬇ Down");
                        downloadBtn.setPrefWidth(50);
                        downloadBtn.setStyle("-fx-padding: 3 5; -fx-font-size: 9;");
                        downloadBtn.setOnAction(e -> downloadMaterial(path, extractFileName(path)));

                        btnBox.getChildren().addAll(viewBtn, downloadBtn);
                        setGraphic(btnBox);
                        setText(null);
                    }
                }
            });

            // Action - Register/Cancel per session
            TableColumn<SessionRow, UUID> actionCol = new TableColumn<>("Action");
            actionCol.setMinWidth(100);
            actionCol.setPrefWidth(120);
            actionCol.setCellValueFactory(cellData -> javafx.beans.binding.Bindings.createObjectBinding(() -> cellData.getValue().getSessionId()));
            actionCol.setCellFactory(col -> new TableCell<SessionRow, UUID>() {
                @Override
                protected void updateItem(UUID sessionId, boolean empty) {
                    super.updateItem(sessionId, empty);
                    if (empty || sessionId == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        int rowIndex = getIndex();
                        if (rowIndex >= 0 && rowIndex < sessionRows.size()) {
                            SessionRow row = sessionRows.get(rowIndex);
                            Button actionBtn = new Button();
                            actionBtn.setPrefWidth(100);
                            actionBtn.setStyle("-fx-padding: 5 8; -fx-font-size: 10; -fx-cursor: hand;");

                            if (registeredSessionIds.contains(sessionId)) {
                                actionBtn.setText("❌ Cancel");
                                actionBtn.setStyle("-fx-padding: 5 8; -fx-font-size: 10; -fx-cursor: hand; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                                actionBtn.setOnAction(e -> {
                                    cancelRegistration(sessionId);
                                    registeredSessionIds.remove(sessionId);
                                    row.setIsRegistered(false);
                                    table.refresh();
                                });
                            } else {
                                actionBtn.setText("✓ Register");
                                actionBtn.setStyle("-fx-padding: 5 8; -fx-font-size: 10; -fx-cursor: hand; -fx-background-color: #27ae60; -fx-text-fill: white;");
                                actionBtn.setOnAction(e -> {
                                    registerForSession(sessionId);
                                    registeredSessionIds.add(sessionId);
                                    row.setIsRegistered(true);
                                    table.refresh();
                                });
                            }
                            setGraphic(actionBtn);
                            setText(null);
                        }
                    }
                }
            });

            table.getColumns().addAll(titleCol, timeCol, venueCol, capacityCol, materialCol, actionCol);

            // Add rows
            for (Session s : sessions) {
                boolean isRegistered = registeredSessionIds.contains(s.getId());
                sessionRows.add(new SessionRow(s, isRegistered));
            }

            // Add table to container
            VBox.setVgrow(table, Priority.ALWAYS);
            HBox tableWrapper = new HBox();
            HBox.setHgrow(table, Priority.ALWAYS);
            tableWrapper.getChildren().add(table);
            sessionsContainer.getChildren().add(tableWrapper);

            recordCountLabel.setText("Total Sessions: " + sessions.size());
            showTable();
        });
    }

    private String extractFileName(String path) {
        if (path == null || path.isEmpty()) return "file";
        if (path.contains("/")) {
            return path.substring(path.lastIndexOf("/") + 1);
        }
        if (path.contains("\\")) {
            return path.substring(path.lastIndexOf("\\") + 1);
        }
        return path;
    }

    /**
     * Session Row class for TableView
     */
    public static class SessionRow {
        private final UUID sessionId;
        private final String title;
        private final String time;
        private final String venue;
        private final String capacity;
        private final String materialPath;
        private boolean isRegistered;

        public SessionRow(Session session, boolean registered) {
            this.sessionId = session.getId();
            this.title = session.getTitle() != null ? session.getTitle() : "Unknown";
            this.time = session.getStart() != null ? session.getStart().toLocalTime().toString() : "N/A";
            this.venue = session.getVenue() != null ? session.getVenue() : "N/A";
            this.capacity = session.getCapacity() + " seats";
            this.materialPath = session.getMaterialPath() != null ? session.getMaterialPath() : "";
            this.isRegistered = registered;
        }

        public UUID getSessionId() { return sessionId; }
        public String getTitle() { return title; }
        public String getTime() { return time; }
        public String getVenue() { return venue; }
        public String getCapacity() { return capacity; }
        public String getMaterialPath() { return materialPath; }
        public boolean getIsRegistered() { return isRegistered; }
        public void setIsRegistered(boolean reg) { this.isRegistered = reg; }
    }

    /**
     * Register for a session - ASYNC
     */
    private void registerForSession(UUID sessionId) {
        if (!(appContext.currentUser instanceof Attendee)) return;
        Attendee attendee = (Attendee) appContext.currentUser;

        AsyncTaskService.runAsync(
            () -> {
                try {
                    if (appContext.sessionRepo != null) {
                        appContext.sessionRepo.registerAttendeeForSession(attendee.getId(), sessionId);
                        return true;
                    }
                } catch (Exception e) {
                    System.err.println("Error registering for session: " + e.getMessage());
                }
                return false;
            },
            success -> {
                if ((Boolean) success) {
                    showAlert("Success", "Registered for session successfully!");
                } else {
                    showAlert("Error", "Failed to register for session");
                }
            },
            error -> {
                showAlert("Error", "Failed to register: " + error.getMessage());
            }
        );
    }

    /**
     * Cancel registration for a session - ASYNC
     */
    private void cancelRegistration(UUID sessionId) {
        if (!(appContext.currentUser instanceof Attendee)) return;
        Attendee attendee = (Attendee) appContext.currentUser;

        AsyncTaskService.runAsync(
            () -> {
                try {
                    if (appContext.sessionRepo != null) {
                        appContext.sessionRepo.cancelAttendeeSession(attendee.getId(), sessionId);
                        return true;
                    }
                } catch (Exception e) {
                    System.err.println("Error canceling registration: " + e.getMessage());
                }
                return false;
            },
            success -> {
                if ((Boolean) success) {
                    showAlert("Success", "Session registration cancelled!");
                } else {
                    showAlert("Error", "Failed to cancel registration");
                }
            },
            error -> {
                showAlert("Error", "Failed to cancel registration: " + error.getMessage());
            }
        );
    }


    /**
     * Register all sessions at once - ASYNC
     */
    private void registerAllSessions(List<Session> sessions) {
        if (!(appContext.currentUser instanceof Attendee)) return;
        Attendee attendee = (Attendee) appContext.currentUser;

        if (sessions == null || sessions.isEmpty()) {
            showAlert("Warning", "No sessions to register");
            return;
        }

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
            () -> {
                int successCount = 0;
                int totalSessions = sessions.size();
                System.out.println("📝 [MyRegistrations] Registering for " + totalSessions + " sessions...");

                try {
                    if (appContext.sessionRepo != null) {
                        for (Session session : sessions) {
                            try {
                                if (!registeredSessionIds.contains(session.getId())) {
                                    appContext.sessionRepo.registerAttendeeForSession(attendee.getId(), session.getId());
                                    registeredSessionIds.add(session.getId());
                                    successCount++;
                                    System.out.println("  ✓ Registered for: " + session.getTitle());
                                }
                            } catch (Exception e) {
                                System.err.println("  ⚠️ Failed to register for session " + session.getTitle() + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("✗ Error registering all sessions: " + e.getMessage());
                }

                System.out.println("✓ Successfully registered for " + successCount + " out of " + totalSessions + " sessions");
                return successCount;
            },
            successCount -> {
                showTable();
                showAlert("Success", "Successfully registered for " + successCount + " session(s)!");
                // Refresh table
                for (var node : sessionsContainer.getChildren()) {
                    if (node instanceof TableView<?>) {
                        ((TableView<?>) node).refresh();
                    }
                }
            },
            error -> {
                showTable();
                System.err.println("✗ Error in registerAllSessions: " + error.getMessage());
                showAlert("Error", "Failed to register sessions: " + error.getMessage());
            }
        );
    }

    /**
     * Cancel all sessions registrations at once - ASYNC
     */
    private void cancelAllSessions(List<Session> sessions) {
        if (!(appContext.currentUser instanceof Attendee)) return;
        Attendee attendee = (Attendee) appContext.currentUser;

        if (sessions == null || sessions.isEmpty()) {
            showAlert("Warning", "No sessions to cancel");
            return;
        }

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
            () -> {
                int successCount = 0;
                int totalSessions = sessions.size();
                System.out.println("🗑️ [MyRegistrations] Cancelling " + totalSessions + " sessions...");

                try {
                    if (appContext.sessionRepo != null) {
                        for (Session session : sessions) {
                            try {
                                if (registeredSessionIds.contains(session.getId())) {
                                    appContext.sessionRepo.cancelAttendeeSession(attendee.getId(), session.getId());
                                    registeredSessionIds.remove(session.getId());
                                    successCount++;
                                    System.out.println("  ✓ Cancelled: " + session.getTitle());
                                }
                            } catch (Exception e) {
                                System.err.println("  ⚠️ Failed to cancel session " + session.getTitle() + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("✗ Error cancelling all sessions: " + e.getMessage());
                }

                System.out.println("✓ Successfully cancelled " + successCount + " out of " + totalSessions + " sessions");
                return successCount;
            },
            successCount -> {
                showTable();
                showAlert("Success", "Successfully cancelled " + successCount + " session(s)!");
                // Refresh table
                for (var node : sessionsContainer.getChildren()) {
                    if (node instanceof TableView<?>) {
                        ((TableView<?>) node).refresh();
                    }
                }
            },
            error -> {
                showTable();
                System.err.println("✗ Error in cancelAllSessions: " + error.getMessage());
                showAlert("Error", "Failed to cancel sessions: " + error.getMessage());
            }
        );
    }



    /**
     * View material - supports both URL and local file paths - ASYNC
     */
    private void viewMaterial(String materialPath, String materialName) {
        long viewStart = System.currentTimeMillis();
        System.out.println("👁 [MyRegistrations] Viewing material: " + materialName);
        System.out.println("    Path: " + materialPath);

        AsyncTaskService.runAsync(
            () -> {
                try {
                    // Check if it's a URL
                    if (materialPath != null && (materialPath.startsWith("http://") || materialPath.startsWith("https://"))) {
                        // It's a remote URL - open in browser
                        System.out.println("  🌐 Detected URL, opening in browser...");
                        if (Desktop.isDesktopSupported()) {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                                desktop.browse(new java.net.URI(materialPath));
                                System.out.println("  ✓ Material URL opened in " + (System.currentTimeMillis() - viewStart) + "ms");
                                return "SUCCESS_URL";
                            }
                        }
                        return "BROWSER_NOT_SUPPORTED";
                    } else {
                        // It's a local file path
                        System.out.println("  📁 Detected local file, opening...");
                        java.io.File file = new java.io.File(materialPath);

                        // Validate file exists
                        if (!file.exists()) {
                            return "FILE_NOT_FOUND";
                        }

                        // Check if it's a file (not directory)
                        if (!file.isFile()) {
                            return "NOT_A_FILE";
                        }

                        // Try to open with desktop
                        if (Desktop.isDesktopSupported()) {
                            Desktop desktop = Desktop.getDesktop();
                            if (desktop.isSupported(Desktop.Action.OPEN)) {
                                desktop.open(file);
                                System.out.println("  ✓ Material opened in " + (System.currentTimeMillis() - viewStart) + "ms");
                                return "SUCCESS";
                            }
                        }
                        return "DESKTOP_NOT_SUPPORTED";
                    }

                } catch (Exception e) {
                    System.err.println("  ✗ Error opening material: " + e.getMessage());
                    e.printStackTrace();
                    return "ERROR: " + e.getMessage();
                }
            },
            result -> {
                String status = (String) result;
                if ("SUCCESS".equals(status) || "SUCCESS_URL".equals(status)) {
                    showAlert("Success", "Opening material: " + materialName);
                } else if ("FILE_NOT_FOUND".equals(status)) {
                    showAlert("Error", "Material file not found at:\n" + materialPath);
                    System.out.println("  ⚠️ File not found: " + materialPath);
                } else if ("NOT_A_FILE".equals(status)) {
                    showAlert("Error", "Invalid material path (not a file)");
                } else if ("DESKTOP_NOT_SUPPORTED".equals(status)) {
                    showAlert("Error", "Cannot open files on this system");
                } else if ("BROWSER_NOT_SUPPORTED".equals(status)) {
                    showAlert("Error", "Browser not supported on this system");
                } else {
                    showAlert("Error", "Cannot open material: " + status);
                }
            },
            error -> {
                System.err.println("  ✗ Error in viewMaterial: " + error.getMessage());
                showAlert("Error", "Failed to open material: " + error.getMessage());
            }
        );
    }

    /**
     * Download material to user's downloads folder - supports both URL and local files - ASYNC
     */
    private void downloadMaterial(String materialPath, String fileName) {
        long downloadStart = System.currentTimeMillis();
        System.out.println("⬇️ [MyRegistrations] Downloading material: " + fileName);

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
            () -> {
                try {
                    // Get downloads folder
                    String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
                    java.io.File downloadDir = new java.io.File(downloadsPath);
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }

                    java.io.File destFile = new java.io.File(downloadDir, fileName);

                    // Check if it's a URL
                    if (materialPath != null && (materialPath.startsWith("http://") || materialPath.startsWith("https://"))) {
                        // Download from URL
                        System.out.println("  🌐 Downloading from URL: " + materialPath);
                        try (java.io.InputStream in = new java.net.URL(materialPath).openStream()) {
                            java.nio.file.Files.copy(
                                in,
                                destFile.toPath(),
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                        System.out.println("  ✓ Downloaded from URL to: " + destFile.getAbsolutePath());
                        return "SUCCESS: " + destFile.getAbsolutePath();
                    } else {
                        // Copy from local file
                        System.out.println("  📁 Copying from local file: " + materialPath);
                        java.io.File sourceFile = new java.io.File(materialPath);
                        if (!sourceFile.exists()) {
                            return "ERROR: Source file not found";
                        }

                        java.nio.file.Files.copy(
                            sourceFile.toPath(),
                            destFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                        );
                        System.out.println("  ✓ Copied to: " + destFile.getAbsolutePath());
                        return "SUCCESS: " + destFile.getAbsolutePath();
                    }

                } catch (Exception e) {
                    System.err.println("  ✗ Download error: " + e.getMessage());
                    e.printStackTrace();
                    return "ERROR: " + e.getMessage();
                }
            },
            result -> {
                showTable();
                String status = (String) result;
                if (status.startsWith("SUCCESS:")) {
                    String path = status.substring(8).trim();
                    System.out.println("  ✓ Download completed in " + (System.currentTimeMillis() - downloadStart) + "ms");
                    showAlert("Success", "Material downloaded to:\n" + path);
                } else if (status.startsWith("ERROR:")) {
                    showAlert("Error", "Download failed: " + status.substring(6).trim());
                }
            },
            error -> {
                showTable();
                System.err.println("  ✗ Error in downloadMaterial: " + error.getMessage());
                showAlert("Error", "Failed to download material: " + error.getMessage());
            }
        );
    }

    @FXML
    public void onBack() {
        System.out.println("🔙 [MyRegistrations] Back to dashboard");
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }

    private void showLoadingPlaceholder() {
        Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(true);
                loadingPlaceholder.setManaged(true);
            }
            sessionsContainer.setVisible(false);
            sessionsContainer.setManaged(false);
        });
    }

    private void showTable() {
        Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(false);
                loadingPlaceholder.setManaged(false);
            }
            sessionsContainer.setVisible(true);
            sessionsContainer.setManaged(true);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

