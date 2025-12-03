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
import org.ems.ui.stage.SceneManager;

import java.math.BigDecimal;
import java.util.*;

public class TicketManagerController {

    // Tab 1: Ticket Templates
    @FXML private ComboBox<String> templateEventCombo;
    @FXML private ComboBox<String> templateSessionCombo;
    @FXML private ComboBox<String> templateTypeCombo;
    @FXML private TextField templatePriceField;
    @FXML private TableView<TemplateRow> templatesTable;
    @FXML private Label templatesCountLabel;

    // Tab 2: Assign Tickets
    @FXML private ComboBox<String> assignAttendeeCombo;
    @FXML private ComboBox<String> assignTemplateCombo;
    @FXML private TableView<TicketRow> assignedTicketsTable;
    @FXML private Label assignedCountLabel;
    @FXML private Label totalAssignedLabel;
    @FXML private Label activeAssignedLabel;
    @FXML private Label totalRevenueLabel;

    private TicketRepository ticketRepo;
    private List<TemplateRow> allTemplates;
    private List<TicketRow> allAssignedTickets;

    private Map<String, UUID> eventMap = new HashMap<>();
    private Map<String, UUID> sessionMap = new HashMap<>();
    private Map<String, UUID> attendeeMap = new HashMap<>();
    private Map<String, UUID> templateMap = new HashMap<>(); // Cache for template IDs

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            ticketRepo = context.ticketRepo;

            // Setup Template Type combo
            templateTypeCombo.setItems(FXCollections.observableArrayList(
                    "GENERAL", "VIP", "EARLY_BIRD", "STUDENT", "GROUP"
            ));
            templateTypeCombo.setValue("GENERAL");

            // Setup table columns
            setupTemplateTableColumns();
            setupAssignedTicketTableColumns();

            // Load data
            loadEvents();
            loadTemplates();
            loadAssignedTickets();


        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
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


    private void loadEvents() {
        try {
            AppContext context = AppContext.get();
            List<String> eventList = new ArrayList<>();
            eventMap.clear();

            if (context.eventRepo != null) {
                List<Event> events = context.eventRepo.findAll();
                for (Event evt : events) {
                    String display = evt.getName() + " (" + evt.getId().toString().substring(0, 8) + ")";
                    eventList.add(display);
                    eventMap.put(display, evt.getId());
                }
            }

            templateEventCombo.setItems(FXCollections.observableArrayList(eventList));
            if (!eventList.isEmpty()) {
                templateEventCombo.setValue(eventList.get(0));
                loadSessions(eventMap.get(eventList.get(0)));
            }

            templateEventCombo.setOnAction(e -> {
                String selected = templateEventCombo.getValue();
                if (selected != null) {
                    loadSessions(eventMap.get(selected));
                }
            });

        } catch (Exception e) {
            System.err.println("Error loading events: " + e.getMessage());
        }
    }

    private void loadSessions(UUID eventId) {
        try {
            AppContext context = AppContext.get();
            List<String> sessionList = new ArrayList<>();
            sessionMap.clear();

            if (context.sessionRepo != null) {
                List<Session> sessions = context.sessionRepo.findByEvent(eventId);
                for (Session sess : sessions) {
                    String display = sess.getTitle() + " (" + sess.getId().toString().substring(0, 8) + ")";
                    sessionList.add(display);
                    sessionMap.put(display, sess.getId());
                }
            }

            templateSessionCombo.setItems(FXCollections.observableArrayList(sessionList));
            if (!sessionList.isEmpty()) {
                templateSessionCombo.setValue(sessionList.get(0));
            }

        } catch (Exception e) {
            System.err.println("Error loading sessions: " + e.getMessage());
        }
    }

    private void loadTemplates() {
        try {
            allTemplates = new ArrayList<>();

            if (ticketRepo != null) {
                // Only get templates (tickets without attendeeId) - use filtered query if available
                List<Ticket> templates = new ArrayList<>();
                List<Ticket> allTickets = ticketRepo.findAll();

                // Filter only templates on client side
                for (Ticket ticket : allTickets) {
                    if (ticket.getAttendeeId() == null) {
                        templates.add(ticket);
                    }
                }

                AppContext context = AppContext.get();

                // Add templates to display
                for (Ticket ticket : templates) {
                    String eventName = "Unknown";
                    String sessionName = "Unknown";

                    try {
                        if (context.eventRepo != null && ticket.getEventId() != null) {
                            Event evt = context.eventRepo.findById(ticket.getEventId());
                            if (evt != null) eventName = evt.getName();
                        }
                        if (context.sessionRepo != null && ticket.getSessionId() != null) {
                            Session sess = context.sessionRepo.findById(ticket.getSessionId());
                            if (sess != null) sessionName = sess.getTitle();
                        }
                    } catch (Exception e) {
                        System.err.println("Error mapping template: " + e.getMessage());
                    }

                    // Count how many times this template was used (assigned)
                    int assigned = 0;
                    for (Ticket t : allTickets) {
                        if (t.getAttendeeId() != null &&
                            t.getEventId().equals(ticket.getEventId()) &&
                            t.getSessionId().equals(ticket.getSessionId()) &&
                            t.getType().equals(ticket.getType()) &&
                            t.getPrice().equals(ticket.getPrice())) {
                            assigned++;
                        }
                    }

                    int available = 100 - assigned;

                    allTemplates.add(new TemplateRow(
                        eventName,
                        sessionName,
                        ticket.getType() != null ? ticket.getType().name() : "N/A",
                        ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                        String.valueOf(available)
                    ));
                }
            }

            displayTemplates(allTemplates);

        } catch (Exception e) {
            System.err.println("Error loading templates: " + e.getMessage());
        }
    }

    private void displayTemplates(List<TemplateRow> templates) {
        ObservableList<TemplateRow> obs = FXCollections.observableArrayList(templates);
        templatesTable.setItems(obs);
        templatesCountLabel.setText("Total Templates: " + templates.size());
    }

    @FXML
    public void onCreateTemplate() {
        try {
            if (templateEventCombo.getValue() == null || templateSessionCombo.getValue() == null) {
                showAlert("Error", "Please select Event and Session");
                return;
            }

            String price = templatePriceField.getText();
            if (price.isEmpty()) {
                showAlert("Error", "Please enter price");
                return;
            }

            UUID eventId = eventMap.get(templateEventCombo.getValue());
            UUID sessionId = sessionMap.get(templateSessionCombo.getValue());

            if (eventId == null || sessionId == null) {
                showAlert("Error", "Invalid event or session selected");
                return;
            }

            Ticket template = new Ticket();
            template.setId(UUID.randomUUID());
            template.setEventId(eventId);
            template.setSessionId(sessionId);
            template.setType(TicketType.valueOf(templateTypeCombo.getValue()));
            template.setPrice(new BigDecimal(price));
            template.setTicketStatus(TicketStatus.ACTIVE);
            template.setPaymentStatus(PaymentStatus.UNPAID);

            if (ticketRepo != null) {
                try {
                    ticketRepo.save(template);
                    showAlert("Success", "Ticket template created successfully!");
                    templatePriceField.clear();
                    loadTemplates();
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

    private void loadTemplatesForAssign(List<Ticket> allTickets) {
        try {
            List<String> templateList = new ArrayList<>();
            templateMap.clear();

            if (ticketRepo != null && !allTickets.isEmpty()) {
                AppContext context = AppContext.get();

                for (Ticket ticket : allTickets) {
                    // Only show templates (tickets without attendeeId)
                    if (ticket.getAttendeeId() == null) {
                        String eventName = "Unknown";
                        String sessionName = "Unknown";

                        try {
                            if (context.eventRepo != null && ticket.getEventId() != null) {
                                Event evt = context.eventRepo.findById(ticket.getEventId());
                                if (evt != null) eventName = evt.getName();
                            }
                            if (context.sessionRepo != null && ticket.getSessionId() != null) {
                                Session sess = context.sessionRepo.findById(ticket.getSessionId());
                                if (sess != null) sessionName = sess.getTitle();
                            }
                        } catch (Exception e) {
                            System.err.println("Error mapping template: " + e.getMessage());
                        }

                        String display = eventName + " - " + sessionName + " (" + ticket.getType().name() + ": $" + ticket.getPrice() + ")";
                        templateList.add(display);
                        templateMap.put(display, ticket.getId()); // Cache template ID for later use
                    }
                }
            }

            assignTemplateCombo.setItems(FXCollections.observableArrayList(templateList));
            if (!templateList.isEmpty()) {
                assignTemplateCombo.setValue(templateList.get(0));
            }

        } catch (Exception e) {
            System.err.println("Error loading templates: " + e.getMessage());
        }
    }

    private void loadAssignedTickets() {
        assignedCountLabel.setText("Loading...");

        Task<AssignedTicketsData> task = new Task<>() {
            @Override
            protected AssignedTicketsData call() throws Exception {
                List<TicketRow> tickets = new ArrayList<>();
                List<Ticket> allTickets = new ArrayList<>();

                try {
                    if (ticketRepo == null) {
                        return new AssignedTicketsData(tickets, allTickets);
                    }

                    // OPTIMIZATION 1: Batch load all data at once
                    updateMessage("Loading data...");
                    allTickets = ticketRepo.findAll();

                    AppContext context = AppContext.get();

                    // OPTIMIZATION 2: Cache all related entities (single query each)
                    Map<UUID, Attendee> attendeeCache = new HashMap<>();
                    Map<UUID, Event> eventCache = new HashMap<>();
                    Map<UUID, Session> sessionCache = new HashMap<>();

                    // Pre-load all attendees, events, sessions into cache
                    updateMessage("Building cache...");
                    if (context.attendeeRepo != null) {
                        List<Attendee> allAttendees = context.attendeeRepo.findAll();
                        for (Attendee att : allAttendees) {
                            attendeeCache.put(att.getId(), att);
                        }
                    }

                    if (context.eventRepo != null) {
                        List<Event> allEvents = context.eventRepo.findAll();
                        for (Event evt : allEvents) {
                            eventCache.put(evt.getId(), evt);
                        }
                    }

                    if (context.sessionRepo != null) {
                        List<Session> allSessions = context.sessionRepo.findAll();
                        for (Session sess : allSessions) {
                            sessionCache.put(sess.getId(), sess);
                        }
                    }

                    updateMessage("Processing " + allTickets.size() + " tickets...");

                    // OPTIMIZATION 3: Filter only assigned tickets + use cache for O(1) lookup
                    int processedCount = 0;
                    for (Ticket ticket : allTickets) {
                        // Only process assigned tickets (has attendeeId)
                        if (ticket.getAttendeeId() != null) {
                            String attendeeName = "Unknown";
                            String eventName = "Unknown";
                            String sessionName = "Unknown";

                            // Use cached data instead of findById()
                            Attendee att = attendeeCache.get(ticket.getAttendeeId());
                            if (att != null) attendeeName = att.getFullName();

                            Event evt = eventCache.get(ticket.getEventId());
                            if (evt != null) eventName = evt.getName();

                            Session sess = sessionCache.get(ticket.getSessionId());
                            if (sess != null) sessionName = sess.getTitle();

                            tickets.add(new TicketRow(
                                    ticket.getId().toString().substring(0, 8),
                                    attendeeName,
                                    eventName,
                                    sessionName,
                                    ticket.getType() != null ? ticket.getType().name() : "N/A",
                                    ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                                    ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "N/A"
                            ));

                            processedCount++;
                            if (processedCount % 50 == 0) {
                                updateMessage("Processed " + processedCount + " tickets...");
                            }
                        }
                    }

                    updateMessage("Done! " + tickets.size() + " assigned tickets loaded");

                } catch (Exception e) {
                    System.err.println("❌ Error loading assigned tickets: " + e.getMessage());
                    e.printStackTrace();
                }

                return new AssignedTicketsData(tickets, allTickets);
            }
        };

        task.setOnSucceeded(event -> {
            AssignedTicketsData data = task.getValue();
            allAssignedTickets = data.tickets;

            // Update UI on FX thread
            loadAttendees();
            displayAssignedTickets(allAssignedTickets);
            loadTemplatesForAssign(data.allTickets);
            calculateStatistics();

            System.out.println("✓ Assigned tickets loaded in " + assignedCountLabel.getText());
        });

        task.setOnFailed(event -> {
            System.err.println("❌ Failed to load assigned tickets: " + task.getException().getMessage());
            task.getException().printStackTrace();
            assignedCountLabel.setText("Error loading data");
        });

        // Run in background thread
        Thread bgThread = new Thread(task);
        bgThread.setDaemon(true);
        bgThread.start();
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
            newTicket.setSessionId(templateTicket.getSessionId());
            newTicket.setType(templateTicket.getType());
            newTicket.setPrice(templateTicket.getPrice());
            newTicket.setTicketStatus(TicketStatus.ACTIVE);
            newTicket.setPaymentStatus(PaymentStatus.PAID); // Admin assigns = already paid

            if (ticketRepo != null) {
                try {
                    ticketRepo.save(newTicket);
                    showAlert("Success", "Ticket assigned to attendee successfully!");
                    loadAssignedTickets();
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
}

