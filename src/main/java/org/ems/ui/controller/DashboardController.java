package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.ems.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.Role;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;

import java.time.LocalDateTime;
import java.util.List;


public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userEmailLabel;

    // Role-specific sections
    @FXML private VBox attendeeSection;
    @FXML private VBox presenterSection;
    @FXML private VBox eventAdminSection;
    @FXML private VBox systemAdminSection;

    private final AppContext appContext = AppContext.get();
    private Person currentUser;
    private Role userRole;
    private LoadingDialog loadingDialog;

    @FXML
    public void initialize() {
        loadUserInfo();
        setupRoleBasedView();
        loadDynamicContentAsync();
    }

    /**
     * Load current user information from AppContext
     */
    private void loadUserInfo() {
        currentUser = appContext.currentUser;

        if (currentUser == null) {
            // No user logged in, redirect to login
            SceneManager.switchTo("login.fxml", "EMS - Login");
            return;
        }

        userRole = currentUser.getRole();

        // Display user information
        userNameLabel.setText("Welcome, " + currentUser.getFullName());
        userRoleLabel.setText("Role: " + getRoleDisplayName(userRole));
        userEmailLabel.setText(currentUser.getEmail());
    }

    /**
     * Load dynamic content based on user role - asynchronously
     */
    private void loadDynamicContentAsync() {
        // Get the primary stage safely
        javafx.stage.Stage primaryStage = null;
        try {
            // Try to get stage from root element if available
            if (attendeeSection != null && attendeeSection.getScene() != null) {
                primaryStage = (javafx.stage.Stage) attendeeSection.getScene().getWindow();
            }
        } catch (Exception e) {
            // Fallback: primary stage will be null, no loading dialog
        }

        // Only show loading dialog if we can get the stage
        if (primaryStage != null) {
            loadingDialog = new LoadingDialog(primaryStage, "Loading dashboard...");
            loadingDialog.show();
        }

        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    try {
                        switch (userRole) {
                            case ATTENDEE:
                                loadAttendeeContent();
                                break;
                            case PRESENTER:
                                loadPresenterContent();
                                break;
                            case EVENT_ADMIN:
                                loadEventAdminContent();
                                break;
                            case SYSTEM_ADMIN:
                                // No dynamic content needed for now
                                break;
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading dynamic content: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                },

                // Success callback
                result -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    System.out.println("✓ Dashboard loaded successfully");
                },

                // Error callback
                error -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    System.err.println("✗ Error loading dashboard: " + error.getMessage());
                }
        );
    }

    /**
     * Load attendee's upcoming events and tickets
     */
    private void loadAttendeeContent() {
        try {
            if (currentUser instanceof Attendee && appContext.ticketRepo != null && appContext.eventRepo != null) {
                Attendee attendee = (Attendee) currentUser;

                // Get all tickets for this attendee
                List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                // Find the "Your Upcoming Events" section in attendeeSection
                if (attendeeSection != null) {
                    VBox upcomingEventsBox = findVBoxByTitle(attendeeSection, "Your Upcoming Events");
                    if (upcomingEventsBox != null) {
                        if (tickets.isEmpty()) {
                            // No tickets, show message
                            Platform.runLater(() -> updateUpcomingEventsDisplay(upcomingEventsBox, null));
                        } else {
                            // Get event and session details
                            List<Event> allEvents = appContext.eventRepo.findAll();

                            // Filter only upcoming events (start date in future)
                            List<Event> upcomingEvents = new java.util.ArrayList<>();
                            java.time.LocalDate today = java.time.LocalDate.now();

                            for (Event event : allEvents) {
                                // Check if attendee has ticket for this event
                                boolean hasTicket = tickets.stream()
                                    .anyMatch(t -> t.getEventId().equals(event.getId()));

                                // Check if event is in future
                                if (hasTicket && event.getStartDate() != null) {
                                    java.time.LocalDate eventStart = event.getStartDate();
                                    if (eventStart.isAfter(today)) {
                                        upcomingEvents.add(event);
                                    }
                                }
                            }

                            // Update UI on FX Application Thread
                            Platform.runLater(() -> updateUpcomingEventsDisplay(upcomingEventsBox, upcomingEvents));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading attendee content: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Load presenter's assigned sessions
     */
    private void loadPresenterContent() {
        try {
            if (currentUser instanceof Presenter && appContext.sessionRepo != null) {
                Presenter presenter = (Presenter) currentUser;
                List<Session> sessions = appContext.sessionRepo.findAll();

                // Filter sessions assigned to this presenter
                // Note: This requires checking presenter_session mapping table
                // For now, we'll display info
                System.out.println("Presenter sessions count: " + sessions.size());
            }
        } catch (Exception e) {
            System.err.println("Error loading presenter content: " + e.getMessage());
        }
    }

    /**
     * Load event admin's event statistics
     */
    private void loadEventAdminContent() {
        try {
            if (appContext.eventRepo != null && appContext.sessionRepo != null && appContext.ticketRepo != null) {
                int totalEvents = appContext.eventRepo.findAll().size();
                int totalSessions = appContext.sessionRepo.findAll().size();
                int totalTickets = appContext.ticketRepo.findAll().size();

                System.out.println("Event Admin - Events: " + totalEvents + ", Sessions: " + totalSessions + ", Tickets: " + totalTickets);
            }
        } catch (Exception e) {
            System.err.println("Error loading event admin content: " + e.getMessage());
        }
    }

    /**
     * Find VBox in a parent VBox by its title label
     */
    private VBox findVBoxByTitle(VBox parent, String titleText) {
        for (var child : parent.getChildren()) {
            if (child instanceof VBox) {
                VBox vbox = (VBox) child;
                for (var item : vbox.getChildren()) {
                    if (item instanceof Label) {
                        Label label = (Label) item;
                        if (label.getText().contains(titleText)) {
                            return vbox;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Update upcoming events display with actual event data
     */
    private void updateUpcomingEventsDisplay(VBox container, List<Event> upcomingEvents) {
        // Clear existing content except title
        container.getChildren().retainAll(
            container.getChildren().stream()
                .filter(n -> n instanceof Label && ((Label) n).getStyle().contains("font-weight"))
                .toList()
        );

        // Add upcoming events information
        if (upcomingEvents == null || upcomingEvents.isEmpty()) {
            Label noEventsLabel = new Label("No upcoming events - Browse and register for events to see them here");
            noEventsLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
            container.getChildren().add(noEventsLabel);
        } else {
            // Sort events by start date
            upcomingEvents.sort((e1, e2) -> {
                if (e1.getStartDate() == null || e2.getStartDate() == null) return 0;
                return e1.getStartDate().compareTo(e2.getStartDate());
            });

            // Display upcoming events
            for (Event event : upcomingEvents) {
                HBox eventBox = createUpcomingEventDisplayBox(event);
                container.getChildren().add(eventBox);
            }
        }
    }

    /**
     * Create a display box for an upcoming event with its sessions
     */
    private HBox createUpcomingEventDisplayBox(Event event) {
        HBox box = new HBox(15);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #3498db; -fx-border-width: 1; -fx-padding: 10; -fx-border-radius: 5; -fx-background-color: #ecf0f1;");

        VBox infoBox = new VBox(5);

        // Event header
        Label eventLabel = new Label("📅 " + (event.getName() != null ? event.getName() : "Unknown Event"));
        eventLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label typeLabel = new Label("Type: " + (event.getType() != null ? event.getType().name() : "N/A"));
        typeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555;");

        Label dateLabel = new Label("Event Start: " + (event.getStartDate() != null ? event.getStartDate() : "N/A"));
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #27ae60; -fx-font-weight: bold;");

        Label locationLabel = new Label("Location: " + (event.getLocation() != null ? event.getLocation() : "N/A"));
        locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        Label statusLabel = new Label("Status: " + (event.getStatus() != null ? event.getStatus().name() : "N/A"));
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #f39c12;");

        infoBox.getChildren().addAll(eventLabel, typeLabel, dateLabel, locationLabel, statusLabel);

        // Load and display sessions for this event that attendee has tickets for
        try {
            if (appContext.sessionRepo != null && appContext.ticketRepo != null && appContext.eventRepo != null && currentUser instanceof Attendee) {
                Attendee attendee = (Attendee) currentUser;

                // Get all events
                List<Event> allEvents = appContext.eventRepo.findAll();

                // Get all sessions for this event
                List<Session> allSessions = appContext.sessionRepo.findByEvent(event.getId());

                // Get all tickets for attendee
                List<Ticket> attendeeTickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                // Note: Tickets are now event-level only, not session-specific
                // Session registrations are done separately after buying ticket
                // For now, show the events user has tickets for
                List<Event> ticketedEvents = new java.util.ArrayList<>();
                for (Ticket ticket : attendeeTickets) {
                    if (ticket.getEventId() != null) {
                        Event evt = allEvents.stream()
                            .filter(e -> e.getId().equals(ticket.getEventId()))
                            .findFirst()
                            .orElse(null);
                        if (evt != null && !ticketedEvents.contains(evt)) {
                            ticketedEvents.add(evt);
                        }
                    }
                }

                if (!ticketedEvents.isEmpty()) {
                    Label eventsLabel = new Label("Events You Have Tickets For:");
                    eventsLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 10 0 5 0;");
                    infoBox.getChildren().add(eventsLabel);

                    // Sort events by start date
                    ticketedEvents.sort((e1, e2) -> {
                        if (e1.getStartDate() == null || e2.getStartDate() == null) return 0;
                        return e1.getStartDate().compareTo(e2.getStartDate());
                    });

                    // Display first 3 events (to avoid too much content)
                    int count = 0;
                    for (Event ticketedEvent : ticketedEvents) {
                        if (count >= 3) {
                            Label moreLabel = new Label("  ... and " + (ticketedEvents.size() - 3) + " more event(s)");
                            moreLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #999;");
                            infoBox.getChildren().add(moreLabel);
                            break;
                        }

                        // Event info
                        String eventInfo = "  • " + (ticketedEvent.getName() != null ? ticketedEvent.getName() : "Unknown") +
                                " (" + (ticketedEvent.getStartDate() != null ? ticketedEvent.getStartDate() : "N/A") + ")";
                        Label ticketedEventLabel = new Label(eventInfo);
                        ticketedEventLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #555;");
                        infoBox.getChildren().add(ticketedEventLabel);

                        // Location info
                        if (ticketedEvent.getLocation() != null) {
                            Label ticketedLocationLabel = new Label("    Location: " + ticketedEvent.getLocation());
                            ticketedLocationLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #888;");
                            infoBox.getChildren().add(ticketedLocationLabel);
                        }

                        count++;
                    }
                } else {
                    Label noSessionsLabel = new Label("(No registered sessions for this event)");
                    noSessionsLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #999;");
                    infoBox.getChildren().add(noSessionsLabel);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading sessions for event: " + e.getMessage());
        }

        // Add Register Sessions button
        VBox actionBox = new VBox(8);
        actionBox.setPadding(new Insets(0, 10, 0, 0));
        actionBox.setStyle("-fx-alignment: center;");

        Button registerButton = new Button("📍 Register Sessions");
        registerButton.setStyle("-fx-padding: 8 15; -fx-font-size: 11; -fx-cursor: hand; -fx-background-color: #27ae60; -fx-text-fill: white;");
        registerButton.setOnAction(e -> navigateToRegisterSessions(event.getId()));

        actionBox.getChildren().add(registerButton);

        box.getChildren().addAll(infoBox, actionBox);
        javafx.scene.layout.HBox.setHgrow(actionBox, javafx.scene.layout.Priority.ALWAYS);

        return box;
    }

    /**
     * Navigate to Register Sessions page
     */
    private void navigateToRegisterSessions(java.util.UUID eventId) {
        try {
            AppContext.get().selectedEventId = eventId;
            SceneManager.switchTo("register_sessions.fxml", "Register for Sessions");
        } catch (Exception e) {
            System.err.println("Error navigating to register sessions: " + e.getMessage());
        }
    }

    /**
     * Setup UI based on user role
     */
    private void setupRoleBasedView() {
        // Hide all sections first
        if (attendeeSection != null) attendeeSection.setVisible(false);
        if (presenterSection != null) presenterSection.setVisible(false);
        if (eventAdminSection != null) eventAdminSection.setVisible(false);
        if (systemAdminSection != null) systemAdminSection.setVisible(false);

        // Show relevant section based on role
        switch (userRole) {
            case ATTENDEE:
                if (attendeeSection != null) attendeeSection.setVisible(true);
                break;
            case PRESENTER:
                if (presenterSection != null) presenterSection.setVisible(true);
                break;
            case EVENT_ADMIN:
                if (eventAdminSection != null) eventAdminSection.setVisible(true);
                break;
            case SYSTEM_ADMIN:
                if (systemAdminSection != null) systemAdminSection.setVisible(true);
                break;
            default:
                break;
        }
    }

    /**
     * Get human-readable role name
     */
    private String getRoleDisplayName(Role role) {
        switch (role) {
            case ATTENDEE:
                return "Event Attendee";
            case PRESENTER:
                return "Presenter";
            case EVENT_ADMIN:
                return "Event Administrator";
            case SYSTEM_ADMIN:
                return "System Administrator";
            default:
                return "Unknown";
        }
    }

    // ==================== ATTENDEE ACTIONS ====================
    @FXML
    public void onBrowseEvents() {
        SceneManager.switchTo("view_events.fxml", "EMS - Browse Events");
    }

    @FXML
    public void onViewMyTickets() {
        SceneManager.switchTo("my_tickets.fxml", "EMS - My Tickets");
    }

    @FXML
    public void onViewMyRegistrations() {
        SceneManager.switchTo("my_registrations.fxml", "EMS - My Registrations");
    }

    @FXML
    public void onExportMySchedule() {
        // TODO: Export attendee's schedule
        System.out.println("Export My Schedule clicked");
    }

    @FXML
    public void onUpdateAttendeeProfile() {
        // TODO: Update attendee profile
        System.out.println("Update Profile clicked");
    }

    // ==================== PRESENTER ACTIONS ====================
    @FXML
    public void onViewAssignedSessions() {
        // TODO: View presenter's assigned sessions
        System.out.println("View Assigned Sessions clicked");
    }

    @FXML
    public void onUploadMaterials() {
        // TODO: Upload session materials
        System.out.println("Upload Materials clicked");
    }

    @FXML
    public void onViewPresenterStats() {
        // TODO: View presenter statistics
        System.out.println("View Stats clicked");
    }

    @FXML
    public void onExportPresenterSummary() {
        // TODO: Export presenter activity summary
        System.out.println("Export Summary clicked");
    }

    @FXML
    public void onUpdatePresenterProfile() {
        // TODO: Update presenter profile
        System.out.println("Update Profile clicked");
    }

    // ==================== EVENT ADMIN ACTIONS ====================
    @FXML
    public void onManageEvents() {
        SceneManager.switchTo("manage_events.fxml", "EMS - Manage Events");
    }

    @FXML
    public void onManageSessions() {
        SceneManager.switchTo("session_manager.fxml", "EMS - Manage Sessions");
    }

    @FXML
    public void onManageTickets() {
        SceneManager.switchTo("ticket_manager.fxml", "EMS - Manage Tickets");
    }

    @FXML
    public void onGenerateEventReports() {
        // TODO: Generate event-level reports
        System.out.println("Generate Reports clicked");
    }

    @FXML
    public void onAssignPresenters() {
        // TODO: Assign presenters to sessions
        System.out.println("Assign Presenters clicked");
    }

    // ==================== SYSTEM ADMIN ACTIONS ====================
    @FXML
    public void onAdminDashboard() {
        SceneManager.switchTo("admin_dashboard.fxml", "EMS - Admin Dashboard");
    }

    @FXML
    public void onManageUsers() {
        SceneManager.switchTo("manage_users.fxml", "EMS - Manage Users");
    }

    @FXML
    public void onManageAllEvents() {
        SceneManager.switchTo("manage_events.fxml", "EMS - Manage All Events");
    }

    @FXML
    public void onManageAllSessions() {
        SceneManager.switchTo("session_manager.fxml", "EMS - Manage All Sessions");
    }

    @FXML
    public void onViewSystemReports() {
        // TODO: View all system reports
        System.out.println("View System Reports clicked");
    }

    @FXML
    public void onViewActivityLogs() {
        // TODO: View system activity logs
        System.out.println("View Activity Logs clicked");
    }

    @FXML
    public void onSystemSettings() {
        // TODO: System settings
        System.out.println("System Settings clicked");
    }

    // ==================== COMMON ACTIONS ====================
    @FXML
    public void onUpdateProfile() {
        // TODO: Update user profile (generic for all roles)
        System.out.println("Update Profile clicked");
    }

    @FXML
    public void onLogout() {
        appContext.currentUser = null;
        appContext.currentUserRole = "VISITOR";
        SceneManager.switchTo("home.fxml", "EMS - Home");
    }
}
