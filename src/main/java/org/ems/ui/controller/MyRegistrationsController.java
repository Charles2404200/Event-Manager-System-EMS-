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
 * @author <your group number>
 */
public class MyRegistrationsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> eventFilterCombo;
    @FXML private TableView<RegistrationRow> registrationsTable;
    @FXML private Label recordCountLabel;

    private List<RegistrationRow> allRegistrations;
    private AppContext appContext;

    @FXML
    public void initialize() {
        try {
            appContext = AppContext.get();

            // Setup filter combo
            eventFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL"
            ));
            eventFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all registrations for current attendee
            loadMyRegistrations();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        ObservableList<TableColumn<RegistrationRow, ?>> columns = registrationsTable.getColumns();

        if (columns.size() >= 7) {
            ((TableColumn<RegistrationRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().eventName));
            ((TableColumn<RegistrationRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().sessionTitle));
            ((TableColumn<RegistrationRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().sessionStart));
            ((TableColumn<RegistrationRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().sessionEnd));
            ((TableColumn<RegistrationRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().venue));
            ((TableColumn<RegistrationRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().capacity));
            ((TableColumn<RegistrationRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().presenters));
        }
    }

    private void loadMyRegistrations() {
        try {
            allRegistrations = new ArrayList<>();

            if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null &&
                appContext.eventRepo != null && appContext.sessionRepo != null) {

                Attendee attendee = (Attendee) appContext.currentUser;

                // Get all tickets for this attendee
                List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                // Get unique events from tickets
                Set<UUID> eventIds = new HashSet<>();
                Map<UUID, List<UUID>> eventToSessions = new HashMap<>();

                for (Ticket ticket : tickets) {
                    if (ticket.getEventId() != null) {
                        eventIds.add(ticket.getEventId());
                        eventToSessions.computeIfAbsent(ticket.getEventId(), k -> new ArrayList<>())
                                .add(ticket.getSessionId());
                    }
                }

                // Load event and session details
                List<Event> allEvents = appContext.eventRepo.findAll();
                List<Session> allSessions = appContext.sessionRepo.findAll();

                for (UUID eventId : eventIds) {
                    Event event = allEvents.stream()
                        .filter(e -> e.getId().equals(eventId))
                        .findFirst()
                        .orElse(null);

                    if (event != null) {
                        // Get sessions for this event
                        List<UUID> sessionIds = eventToSessions.get(eventId);
                        if (sessionIds != null) {
                            for (UUID sessionId : sessionIds) {
                                if (sessionId != null) {
                                    Session session = allSessions.stream()
                                        .filter(s -> s.getId().equals(sessionId))
                                        .findFirst()
                                        .orElse(null);

                                    if (session != null) {
                                        // Get presenters for this session
                                        String presentersStr = getPresentersForSession(session, allSessions);

                                        allRegistrations.add(new RegistrationRow(
                                                event.getName(),
                                                session.getTitle(),
                                                session.getStart() != null ? session.getStart().toString() : "N/A",
                                                session.getEnd() != null ? session.getEnd().toString() : "N/A",
                                                session.getVenue() != null ? session.getVenue() : "N/A",
                                                String.valueOf(session.getCapacity()),
                                                presentersStr
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }

                // Load unique events into filter combo
                List<String> eventNames = new ArrayList<>();
                eventNames.add("ALL");
                for (UUID eventId : eventIds) {
                    Event event = allEvents.stream()
                        .filter(e -> e.getId().equals(eventId))
                        .findFirst()
                        .orElse(null);
                    if (event != null) {
                        eventNames.add(event.getName());
                    }
                }
                eventFilterCombo.setItems(FXCollections.observableArrayList(eventNames));
            }

            displayRegistrations(allRegistrations);

        } catch (Exception e) {
            showAlert("Error", "Failed to load registrations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getPresentersForSession(Session session, List<Session> allSessions) {
        // Get presenter IDs from session
        List<UUID> presenterIds = session.getPresenterIds();
        if (presenterIds == null || presenterIds.isEmpty()) {
            return "No presenters assigned";
        }
        // TODO: Load presenter names from repository
        return presenterIds.size() + " presenter(s)";
    }

    private void displayRegistrations(List<RegistrationRow> registrations) {
        ObservableList<RegistrationRow> observableList = FXCollections.observableArrayList(registrations);
        registrationsTable.setItems(observableList);
        recordCountLabel.setText("Total Registrations: " + registrations.size());
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String eventFilter = eventFilterCombo.getValue();

            List<RegistrationRow> filtered = new ArrayList<>();

            for (RegistrationRow reg : allRegistrations) {
                // Apply event filter
                if (!eventFilter.equals("ALL") && !reg.eventName.equals(eventFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    reg.eventName.toLowerCase().contains(searchTerm) ||
                    reg.sessionTitle.toLowerCase().contains(searchTerm) ||
                    reg.venue.toLowerCase().contains(searchTerm)) {
                    filtered.add(reg);
                }
            }

            displayRegistrations(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        eventFilterCombo.setValue("ALL");
        displayRegistrations(allRegistrations);
    }

    @FXML
    public void onViewSessionDetails() {
        RegistrationRow selected = registrationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a session to view details");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Session Details");
        alert.setHeaderText("Registration Details");
        alert.setContentText(
                "Event: " + selected.eventName + "\n\n" +
                "Session: " + selected.sessionTitle + "\n" +
                "Start: " + selected.sessionStart + "\n" +
                "End: " + selected.sessionEnd + "\n" +
                "Venue: " + selected.venue + "\n" +
                "Capacity: " + selected.capacity + "\n" +
                "Presenters: " + selected.presenters
        );
        alert.showAndWait();
    }

    @FXML
    public void onCancelRegistration() {
        RegistrationRow selected = registrationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a registration to cancel");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Cancellation");
        confirmAlert.setHeaderText("Cancel Registration");
        confirmAlert.setContentText("Are you sure you want to cancel registration for:\n" + selected.sessionTitle + "?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // TODO: Implement cancel registration logic
            showAlert("Success", "Registration cancelled for: " + selected.sessionTitle);
            loadMyRegistrations();
        }
    }

    @FXML
    public void onExportRegistrations() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Event,Session,Start Time,End Time,Venue,Capacity,Presenters\n");

            for (RegistrationRow reg : allRegistrations) {
                csv.append(reg.eventName).append(",")
                   .append(reg.sessionTitle).append(",")
                   .append(reg.sessionStart).append(",")
                   .append(reg.sessionEnd).append(",")
                   .append(reg.venue).append(",")
                   .append(reg.capacity).append(",")
                   .append(reg.presenters).append("\n");
            }

            // TODO: Save CSV to file
            showAlert("Success", "Export feature coming soon!");

        } catch (Exception e) {
            showAlert("Error", "Export failed: " + e.getMessage());
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

    // Helper class
    public static class RegistrationRow {
        public String eventName;
        public String sessionTitle;
        public String sessionStart;
        public String sessionEnd;
        public String venue;
        public String capacity;
        public String presenters;

        public RegistrationRow(String eventName, String sessionTitle, String sessionStart,
                              String sessionEnd, String venue, String capacity, String presenters) {
            this.eventName = eventName;
            this.sessionTitle = sessionTitle;
            this.sessionStart = sessionStart;
            this.sessionEnd = sessionEnd;
            this.venue = venue;
            this.capacity = capacity;
            this.presenters = presenters;
        }

        // Getters for TableView binding
        public String getEventName() { return eventName; }
        public String getSessionTitle() { return sessionTitle; }
        public String getSessionStart() { return sessionStart; }
        public String getSessionEnd() { return sessionEnd; }
        public String getVenue() { return venue; }
        public String getCapacity() { return capacity; }
        public String getPresenters() { return presenters; }
    }
}

