package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;

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

    private List<Session> availableSessions = new ArrayList<>();
    private Map<String, UUID> eventMap = new HashMap<>();
    private Map<CheckBox, UUID> sessionCheckMap = new HashMap<>();
    private AppContext appContext;

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
        registeredSessionsContainer.getChildren().clear();
        registeredSessionsContainer.setVisible(true);
        registeredSessionsContainer.setManaged(true);

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
                displayRegisteredSessions((List<Session>) result);
            },
            error -> {
                registeredSessionsContainer.getChildren().add(new Label("Error loading registered sessions"));
            }
        );
    }

    /**
     * Display sessions UI
     */
    private void displaySessions(List<Session> sessions) {
        Platform.runLater(() -> {
            sessionsContainer.getChildren().clear();
            sessionCheckMap.clear();
            availableSessions.clear();

            if (sessions == null || sessions.isEmpty()) {
                Label noSessionsLabel = new Label("No sessions available for this event");
                noSessionsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                sessionsContainer.getChildren().add(noSessionsLabel);
                recordCountLabel.setText("Total Sessions: 0");
                showTable();
                return;
            }

            availableSessions = sessions;

            // Add title
            Label titleLabel = new Label("Select Sessions to Register:");
            titleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            sessionsContainer.getChildren().add(titleLabel);

            // Add separator
            Separator sep1 = new Separator();
            sessionsContainer.getChildren().add(sep1);

            // Add checkboxes for each session
            for (Session session : sessions) {
                HBox sessionRow = new HBox(10);
                sessionRow.setPadding(new Insets(8));
                sessionRow.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 0 1 0; -fx-padding: 8;");

                CheckBox checkbox = new CheckBox();
                sessionCheckMap.put(checkbox, session.getId());

                String sessionInfo = (session.getTitle() != null ? session.getTitle() : "Unknown") +
                                   " | " + (session.getStart() != null ? session.getStart().toLocalTime() : "N/A") +
                                   " | " + (session.getVenue() != null ? session.getVenue() : "N/A");

                Label sessionLabel = new Label(sessionInfo);
                sessionLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");

                String capacity = session.getCapacity() + " seats";
                Label capacityLabel = new Label(capacity);
                capacityLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");

                sessionRow.getChildren().addAll(checkbox, sessionLabel, capacityLabel);
                HBox.setHgrow(sessionLabel, Priority.ALWAYS);
                sessionsContainer.getChildren().add(sessionRow);
            }

            // Add separator
            Separator sep2 = new Separator();
            sessionsContainer.getChildren().add(sep2);

            // Add action buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setPadding(new Insets(10));

            Button saveButton = new Button("💾 Save Registration");
            saveButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand; -fx-background-color: #27ae60; -fx-text-fill: white;");
            saveButton.setOnAction(e -> saveRegistrations());

            Button selectAllButton = new Button("✓ Select All");
            selectAllButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand;");
            selectAllButton.setOnAction(e -> selectAllSessions());

            Button clearButton = new Button("✗ Clear All");
            clearButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand;");
            clearButton.setOnAction(e -> clearAllSelections());

            buttonBox.getChildren().addAll(saveButton, selectAllButton, clearButton);
            sessionsContainer.getChildren().add(buttonBox);

            recordCountLabel.setText("Total Sessions: " + sessions.size());

            showTable();
        });
    }

    /**
     * Display registered sessions with cancel option
     */
    private void displayRegisteredSessions(List<Session> sessions) {
        Platform.runLater(() -> {
            registeredSessionsContainer.getChildren().clear();
            if (sessions == null || sessions.isEmpty()) {
                Label noRegLabel = new Label("No registered sessions for this event");
                noRegLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                registeredSessionsContainer.getChildren().add(noRegLabel);
                return;
            }
            Label title = new Label("Registered Sessions:");
            title.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            registeredSessionsContainer.getChildren().add(title);
            for (Session session : sessions) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(8));
                row.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 0 1 0; -fx-padding: 8;");
                Label info = new Label((session.getTitle() != null ? session.getTitle() : "Unknown") +
                    " | " + (session.getStart() != null ? session.getStart().toLocalTime() : "N/A") +
                    " | " + (session.getVenue() != null ? session.getVenue() : "N/A"));
                info.setStyle("-fx-font-size: 11; -fx-text-fill: #333;");
                Button cancelBtn = new Button("Cancel");
                cancelBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 11; -fx-background-color: #e74c3c; -fx-text-fill: white;");
                cancelBtn.setOnAction(e -> cancelRegistration(session.getId()));
                row.getChildren().addAll(info, cancelBtn);
                registeredSessionsContainer.getChildren().add(row);
            }
        });
    }

    /**
     * Save selected sessions registration - ASYNC
     */
    @FXML
    private void saveRegistrations() {
        long saveStart = System.currentTimeMillis();
        System.out.println("💾 [MyRegistrations] Saving registrations...");

        List<UUID> selectedSessions = new ArrayList<>();

        for (CheckBox checkbox : sessionCheckMap.keySet()) {
            if (checkbox.isSelected()) {
                selectedSessions.add(sessionCheckMap.get(checkbox));
            }
        }

        if (selectedSessions.isEmpty()) {
            showAlert("Warning", "Please select at least one session to register");
            return;
        }

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background] Registering for " + selectedSessions.size() + " sessions...");

                    try {
                        if (appContext.currentUser instanceof Attendee && appContext.sessionRepo != null) {
                            Attendee attendee = (Attendee) appContext.currentUser;
                            int successCount = 0;

                            for (UUID sessionId : selectedSessions) {
                                try {
                                    appContext.sessionRepo.registerAttendeeForSession(attendee.getId(), sessionId);
                                    successCount++;
                                } catch (Exception e) {
                                    System.err.println("    ⚠️ Failed to register for session " + sessionId + ": " + e.getMessage());
                                }
                            }

                            System.out.println("    ✓ Successfully registered for " + successCount + " sessions in " + (System.currentTimeMillis() - taskStart) + " ms");
                            return successCount;
                        }
                    } catch (Exception e) {
                        System.err.println("    ✗ Error saving registrations: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return 0;
                },
                successCount -> {
                    showTable();
                    showAlert("Success", "Successfully registered for " + successCount + " session(s)!");
                    System.out.println("✓ Registrations saved in " + (System.currentTimeMillis() - saveStart) + " ms");
                },
                error -> {
                    showTable();
                    System.err.println("✗ Error saving registrations: " + error.getMessage());
                    showAlert("Error", "Failed to save registrations: " + error.getMessage());
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
                    // Reload registered sessions
                    String selected = eventCombo.getValue();
                    if (selected != null && eventMap.containsKey(selected)) {
                        loadRegisteredSessionsForEvent(eventMap.get(selected));
                    }
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
     * Clear all selections
     */
    @FXML
    private void clearAllSelections() {
        for (CheckBox checkbox : sessionCheckMap.keySet()) {
            checkbox.setSelected(false);
        }
    }

    /**
     * Select all sessions
     */
    @FXML
    private void selectAllSessions() {
        for (CheckBox checkbox : sessionCheckMap.keySet()) {
            checkbox.setSelected(true);
        }
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

