package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.ui.stage.SceneManager;

import java.util.*;

/**
 * Register Sessions Page
 * @author <your group number>
 */
public class MyRegistrationsController {

    @FXML private ComboBox<String> eventCombo;
    @FXML private VBox sessionsContainer;
    @FXML private Label recordCountLabel;

    private List<Session> availableSessions = new ArrayList<>();
    private Map<String, UUID> eventMap = new HashMap<>();
    private Map<CheckBox, UUID> sessionCheckMap = new HashMap<>();
    private AppContext appContext;

    @FXML
    public void initialize() {
        try {
            appContext = AppContext.get();
            loadEventsWithTickets();
        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all events where user has tickets
     */
    private void loadEventsWithTickets() {
        try {
            if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null && appContext.eventRepo != null) {
                Attendee attendee = (Attendee) appContext.currentUser;

                // Get all tickets for current attendee
                List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                if (tickets == null || tickets.isEmpty()) {
                    Label noTicketsLabel = new Label("No tickets yet. Buy a ticket first to register for sessions!");
                    noTicketsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                    sessionsContainer.getChildren().add(noTicketsLabel);
                    return;
                }

                // Get unique events from tickets
                Set<UUID> eventIds = new HashSet<>();
                for (Ticket ticket : tickets) {
                    if (ticket.getEventId() != null) {
                        eventIds.add(ticket.getEventId());
                    }
                }

                // Build event combo
                List<String> eventNames = new ArrayList<>();
                List<Event> allEvents = appContext.eventRepo.findAll();

                for (UUID eventId : eventIds) {
                    Event event = allEvents.stream()
                        .filter(e -> e.getId().equals(eventId))
                        .findFirst()
                        .orElse(null);

                    if (event != null) {
                        String display = event.getName();
                        eventNames.add(display);
                        eventMap.put(display, eventId);
                    }
                }

                eventCombo.setItems(FXCollections.observableArrayList(eventNames));
                if (!eventNames.isEmpty()) {
                    eventCombo.setValue(eventNames.get(0));
                    loadSessionsForEvent(eventMap.get(eventNames.get(0)));
                }

                // Load sessions when event selected
                eventCombo.setOnAction(e -> {
                    String selected = eventCombo.getValue();
                    if (selected != null && eventMap.containsKey(selected)) {
                        loadSessionsForEvent(eventMap.get(selected));
                    }
                });

            }
        } catch (Exception e) {
            showAlert("Error", "Failed to load events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load all sessions for selected event
     */
    private void loadSessionsForEvent(UUID eventId) {
        try {
            // Clear previous sessions
            sessionsContainer.getChildren().clear();
            sessionCheckMap.clear();
            availableSessions.clear();

            // Get sessions for this event
            List<Session> sessions = appContext.sessionRepo.findByEvent(eventId);

            if (sessions == null || sessions.isEmpty()) {
                Label noSessionsLabel = new Label("No sessions available for this event");
                noSessionsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
                sessionsContainer.getChildren().add(noSessionsLabel);
                recordCountLabel.setText("Total Sessions: 0");
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
                sessionsContainer.getChildren().add(sessionRow);
            }

            // Add separator
            Separator sep2 = new Separator();
            sessionsContainer.getChildren().add(sep2);

            // Add action buttons
            HBox buttonBox = new HBox(10);
            buttonBox.setPadding(new Insets(10));

            Button saveButton = new Button("Save Registration");
            saveButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand;");
            saveButton.setOnAction(e -> saveRegistrations());

            Button clearButton = new Button("Clear All");
            clearButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand;");
            clearButton.setOnAction(e -> clearAllSelections());

            Button selectAllButton = new Button("Select All");
            selectAllButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand;");
            selectAllButton.setOnAction(e -> selectAllSessions());

            buttonBox.getChildren().addAll(saveButton, clearButton, selectAllButton);
            sessionsContainer.getChildren().add(buttonBox);

            recordCountLabel.setText("Total Sessions: " + sessions.size());

        } catch (Exception e) {
            showAlert("Error", "Failed to load sessions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Save selected sessions registration
     */
    @FXML
    private void saveRegistrations() {
        try {
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

            // Register attendee for each selected session
            if (appContext.currentUser instanceof Attendee && appContext.sessionRepo != null) {
                Attendee attendee = (Attendee) appContext.currentUser;
                int successCount = 0;

                for (UUID sessionId : selectedSessions) {
                    try {
                        appContext.sessionRepo.registerAttendeeForSession(attendee.getId(), sessionId);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("Failed to register for session " + sessionId + ": " + e.getMessage());
                    }
                }

                showAlert("Success", "Successfully registered for " + successCount + " session(s)!");
                System.out.println("✓ Attendee " + attendee.getId() + " registered for " + successCount + " sessions");
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to save registrations: " + e.getMessage());
            e.printStackTrace();
        }
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
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

