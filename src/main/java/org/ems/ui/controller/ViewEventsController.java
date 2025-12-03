package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.ui.stage.SceneManager;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author <your group number>
 */
public class ViewEventsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<EventRow> eventsTable;
    @FXML private Label recordCountLabel;

    private EventRepository eventRepo;
    private List<EventRow> allEvents;
    private AppContext appContext;

    @FXML
    public void initialize() {
        try {
            // Get repository from context
            appContext = AppContext.get();
            eventRepo = appContext.eventRepo;

            // Setup type filter combo
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            // Setup status filter combo
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all events
            loadAllEvents();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void setupTableColumns() {
        ObservableList<TableColumn<EventRow, ?>> columns = eventsTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<EventRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().name));
            ((TableColumn<EventRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().type));
            ((TableColumn<EventRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().location));
            ((TableColumn<EventRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().startDate));
            ((TableColumn<EventRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().endDate));
            ((TableColumn<EventRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
            ((TableColumn<EventRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().sessionCount)));
            ((TableColumn<EventRow, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().isRegistered ? "✓ Yes" : "No"));
        }
    }

    private void loadAllEvents() {
        try {
            allEvents = new ArrayList<>();

            // Load events from repository
            if (eventRepo != null) {
                try {
                    List<Event> events = eventRepo.findAll();
                    for (Event event : events) {
                        // Load session count
                        int sessionCount = 0;
                        if (appContext.sessionRepo != null) {
                            try {
                                sessionCount = appContext.sessionRepo.findByEvent(event.getId()).size();
                            } catch (Exception e) {
                                System.err.println("Error loading sessions: " + e.getMessage());
                            }
                        }

                        // Check if current user is registered (has ticket for this event)
                        boolean isRegistered = false;
                        if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null) {
                            try {
                                Attendee attendee = (Attendee) appContext.currentUser;
                                List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());
                                for (Ticket ticket : tickets) {
                                    if (ticket.getEventId().equals(event.getId())) {
                                        isRegistered = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                System.err.println("Error checking registration: " + e.getMessage());
                            }
                        }

                        allEvents.add(new EventRow(
                                event.getId(),
                                event.getName(),
                                event.getType().name(),
                                event.getLocation(),
                                event.getStartDate() != null ? event.getStartDate().toString() : "N/A",
                                event.getEndDate() != null ? event.getEndDate().toString() : "N/A",
                                event.getStatus().name(),
                                sessionCount,
                                isRegistered
                        ));
                    }
                    System.out.println(" Loaded " + events.size() + " events");
                } catch (Exception e) {
                    System.err.println("⚠ Error loading events: " + e.getMessage());
                }
            }

            displayEvents(allEvents);
            System.out.println(" Total events loaded: " + allEvents.size());

        } catch (Exception e) {
            showAlert("Error", "Failed to load events: " + e.getMessage());
            System.err.println("Load events error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void displayEvents(List<EventRow> events) {
        ObservableList<EventRow> observableList = FXCollections.observableArrayList(events);
        eventsTable.setItems(observableList);
        recordCountLabel.setText("Total Events: " + events.size());
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();

            List<EventRow> filtered = new ArrayList<>();

            for (EventRow event : allEvents) {
                // Apply type filter
                if (!typeFilter.equals("ALL") && !event.type.equals(typeFilter)) {
                    continue;
                }

                // Apply status filter
                if (!statusFilter.equals("ALL") && !event.status.equals(statusFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    event.name.toLowerCase().contains(searchTerm) ||
                    event.location.toLowerCase().contains(searchTerm)) {
                    filtered.add(event);
                }
            }

            displayEvents(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        displayEvents(allEvents);
    }

    @FXML
    public void onViewDetails() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to view details");
            return;
        }
        System.out.println("View Details for event: " + selected.name);
        showAlert("Info", "Event: " + selected.name + "\n\n" +
                "Type: " + selected.type + "\n" +
                "Location: " + selected.location + "\n" +
                "Start: " + selected.startDate + "\n" +
                "End: " + selected.endDate + "\n" +
                "Status: " + selected.status + "\n" +
                "Sessions: " + selected.sessionCount);
    }

    @FXML
    public void onBuyTicket() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to buy tickets");
            return;
        }

        try {
            // Get available ticket templates for this event
            List<Ticket> availableTickets = new ArrayList<>();
            if (appContext.ticketRepo != null) {
                try {
                    List<Ticket> allTickets = appContext.ticketRepo.findAll();
                    for (Ticket ticket : allTickets) {
                        // Get templates (no attendeeId) for this event
                        if (ticket.getAttendeeId() == null && ticket.getEventId().equals(selected.eventId)) {
                            availableTickets.add(ticket);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error loading available tickets: " + e.getMessage());
                }
            }

            if (availableTickets.isEmpty()) {
                showAlert("Info", "No available tickets for this event yet");
                return;
            }

            // Show ticket selection dialog
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Buy Ticket");
            dialog.setHeaderText("Select ticket type for: " + selected.name);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            Label infoLabel = new Label("Available Tickets:");
            infoLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

            ComboBox<String> ticketCombo = new ComboBox<>();
            List<String> ticketDisplay = new ArrayList<>();
            Map<String, Ticket> ticketMap = new HashMap<>();

            for (Ticket ticket : availableTickets) {
                String display = ticket.getType().name() + " - $" + ticket.getPrice();
                ticketDisplay.add(display);
                ticketMap.put(display, ticket);
            }

            ticketCombo.setItems(FXCollections.observableArrayList(ticketDisplay));
            ticketCombo.setValue(ticketDisplay.get(0));
            ticketCombo.setPrefWidth(300);

            content.getChildren().addAll(infoLabel, ticketCombo);
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                String selected_ticket = ticketCombo.getValue();
                Ticket selectedTemplate = ticketMap.get(selected_ticket);

                if (selectedTemplate != null) {
                    // Create ticket for current attendee
                    if (appContext.currentUser instanceof Attendee) {
                        Attendee attendee = (Attendee) appContext.currentUser;

                        Ticket newTicket = new Ticket();
                        newTicket.setId(UUID.randomUUID());
                        newTicket.setAttendeeId(attendee.getId());
                        newTicket.setEventId(selectedTemplate.getEventId());
                        newTicket.setSessionId(selectedTemplate.getSessionId());
                        newTicket.setType(selectedTemplate.getType());
                        newTicket.setPrice(selectedTemplate.getPrice());
                        newTicket.setTicketStatus(TicketStatus.ACTIVE);
                        newTicket.setPaymentStatus(PaymentStatus.PAID);

                        // Generate QR code (simple simulation)
                        String qrCode = "QR-" + newTicket.getId().toString().substring(0, 12).toUpperCase();
                        newTicket.setQrCodeData(qrCode);

                        if (appContext.ticketRepo != null) {
                            try {
                                appContext.ticketRepo.save(newTicket);

                                // Show QR code dialog
                                showQRCodeDialog(newTicket, selected.name);

                                // Reload events to update registration status
                                loadAllEvents();
                            } catch (Exception e) {
                                showAlert("Error", "Failed to purchase ticket: " + e.getMessage());
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Error buying ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showQRCodeDialog(Ticket ticket, String eventName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket Purchase Confirmation");
        dialog.setHeaderText("Successfully purchased ticket!");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-alignment: center;");

        Label titleLabel = new Label("Event: " + eventName);
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label typeLabel = new Label("Ticket Type: " + ticket.getType().name());
        typeLabel.setStyle("-fx-font-size: 12;");

        Label priceLabel = new Label("Price: $" + ticket.getPrice());
        priceLabel.setStyle("-fx-font-size: 12;");

        Label qrTitleLabel = new Label("QR Code:");
        qrTitleLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

        Label qrCodeLabel = new Label(ticket.getQrCodeData());
        qrCodeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                "-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-padding: 15; " +
                "-fx-background-color: #ecf0f1; -fx-border-radius: 5;");

        Label ticketIdLabel = new Label("Ticket ID: " + ticket.getId().toString().substring(0, 8));
        ticketIdLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");

        content.getChildren().addAll(
                titleLabel,
                new Separator(),
                typeLabel,
                priceLabel,
                qrTitleLabel,
                qrCodeLabel,
                ticketIdLabel,
                new Separator(),
                new Label("Show this QR code at the event entrance")
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void onViewSessions() {
        EventRow selected = eventsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select an event to view sessions");
            return;
        }
        System.out.println("View Sessions for event: " + selected.name);

        try {
            if (appContext.sessionRepo != null) {
                List<Session> sessions = appContext.sessionRepo.findByEvent(selected.eventId);

                if (sessions.isEmpty()) {
                    showAlert("Info", "No sessions found for this event");
                    return;
                }

                StringBuilder sessionInfo = new StringBuilder("Sessions for " + selected.name + ":\n\n");
                for (Session session : sessions) {
                    sessionInfo.append("• ").append(session.getTitle()).append("\n");
                    sessionInfo.append("  Time: ").append(session.getStart()).append(" - ").append(session.getEnd()).append("\n");
                    sessionInfo.append("  Venue: ").append(session.getVenue()).append("\n");
                    sessionInfo.append("  Capacity: ").append(session.getCapacity()).append("\n");

                    // Load and display presenter names
                    if (appContext.presenterRepo != null && session.getPresenterIds() != null && !session.getPresenterIds().isEmpty()) {
                        sessionInfo.append("  Presenters: ");
                        List<String> presenterNames = new ArrayList<>();

                        for (UUID presenterId : session.getPresenterIds()) {
                            try {
                                Presenter presenter = appContext.presenterRepo.findById(presenterId);
                                if (presenter != null) {
                                    presenterNames.add(presenter.getFullName() + " (" + presenter.getPresenterType().name() + ")");
                                }
                            } catch (Exception e) {
                                System.err.println("Error loading presenter: " + e.getMessage());
                            }
                        }

                        if (!presenterNames.isEmpty()) {
                            sessionInfo.append(String.join(", ", presenterNames)).append("\n");
                        } else {
                            sessionInfo.append("No presenters assigned\n");
                        }
                    } else {
                        sessionInfo.append("  Presenters: No presenters assigned\n");
                    }

                    sessionInfo.append("\n");
                }

                showAlert("Sessions", sessionInfo.toString());
            }
        } catch (Exception e) {
            showAlert("Error", "Error loading sessions: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        // Go back to Attendee Dashboard instead of Home
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for displaying event data in table
    public static class EventRow {
        public UUID eventId;
        public String name;
        public String type;
        public String location;
        public String startDate;
        public String endDate;
        public String status;
        public int sessionCount;
        public boolean isRegistered;

        public EventRow(UUID eventId, String name, String type, String location,
                       String startDate, String endDate, String status, int sessionCount, boolean isRegistered) {
            this.eventId = eventId;
            this.name = name;
            this.type = type;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.status = status;
            this.sessionCount = sessionCount;
            this.isRegistered = isRegistered;
        }
    }
}

