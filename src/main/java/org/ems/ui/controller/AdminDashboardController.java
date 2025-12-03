package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.concurrent.Task;
import org.ems.config.AppContext;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Event;
import org.ems.domain.model.enums.EventStatus;
import org.ems.ui.stage.SceneManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author <your group number>
 */
public class AdminDashboardController {

    @FXML private Label adminInfoLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalAttendeesLabel;
    @FXML private Label totalPresentersLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalSessionsLabel;
    @FXML private Label totalTicketsLabel;
    @FXML private Label activeEventsLabel;
    @FXML private Label dbStatusLabel;
    @FXML private Label lastUpdateLabel;


    @FXML
    public void initialize() {
        // Get current user from AppContext
        AppContext ctx = AppContext.get();
        String adminUsername = ctx.currentUser != null ? ctx.currentUser.getUsername() : "admin";
        adminInfoLabel.setText("Logged in as: " + adminUsername + " (SYSTEM_ADMIN)");

        // Load statistics in background to avoid freezing UI
        loadStatisticsAsync();
    }

    private void loadStatisticsAsync() {
        Task<StatisticsData> task = new Task<>() {
            @Override
            protected StatisticsData call() throws Exception {
                try {
                    AppContext ctx = AppContext.get();
                    
                    // Check database connection
                    boolean dbConnected = ctx.connection != null && !ctx.connection.isClosed();

                    // Get counts from repositories
                    int totalUsers = 0;
                    int totalAttendees = 0;
                    int totalPresenters = 0;
                    int totalEvents = 0;
                    int totalSessions = 0;
                    int totalTickets = 0;
                    int activeEvents = 0;

                    if (dbConnected) {
                        // Total Users
                        if (ctx.userRepo != null) {
                            totalUsers = ctx.userRepo.findAll().size();
                        }

                        // Total Attendees
                        if (ctx.attendeeRepo != null) {
                            totalAttendees = ctx.attendeeRepo.findAll().size();
                        }

                        // Total Presenters
                        if (ctx.presenterRepo != null) {
                            totalPresenters = ctx.presenterRepo.findAll().size();
                        }

                        // Total Events
                        if (ctx.eventRepo != null) {
                            java.util.List<Event> events = ctx.eventRepo.findAll();
                            totalEvents = events.size();

                            // Count active events
                            for (Event event : events) {
                                if (event.getStatus() == EventStatus.ONGOING ||
                                    event.getStatus() == EventStatus.SCHEDULED) {
                                    activeEvents++;
                                }
                            }
                        }

                        // Total Sessions
                        if (ctx.sessionRepo != null) {
                            totalSessions = ctx.sessionRepo.findAll().size();
                        }

                        // Total Tickets
                        if (ctx.ticketRepo != null) {
                            totalTickets = ctx.ticketRepo.findAll().size();
                        }
                    }

                    return new StatisticsData(
                        totalUsers,
                        totalAttendees,
                        totalPresenters,
                        totalEvents,
                        totalSessions,
                        totalTickets,
                        activeEvents,
                        dbConnected
                    );
                } catch (Exception e) {
                    System.err.println("Error loading statistics: " + e.getMessage());
                    e.printStackTrace();
                    return new StatisticsData(0, 0, 0, 0, 0, 0, 0, false);
                }
            }
        };
        
        // Handle success
        task.setOnSucceeded(event -> {
            StatisticsData stats = task.getValue();
            totalUsersLabel.setText(String.valueOf(stats.totalUsers));
            totalAttendeesLabel.setText(String.valueOf(stats.totalAttendees));
            totalPresentersLabel.setText(String.valueOf(stats.totalPresenters));
            totalEventsLabel.setText(String.valueOf(stats.totalEvents));
            totalSessionsLabel.setText(String.valueOf(stats.totalSessions));
            totalTicketsLabel.setText(String.valueOf(stats.totalTickets));
            activeEventsLabel.setText(String.valueOf(stats.activeEvents));

            // Update database status
            if (stats.dbConnected) {
                dbStatusLabel.setText("🟢 Connected");
                dbStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                dbStatusLabel.setText("🔴 Disconnected");
                dbStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }

            // Update last update time
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            lastUpdateLabel.setText(now.format(formatter));
        });
        
        // Handle failure
        task.setOnFailed(event -> {
            System.err.println("Failed to load statistics: " + task.getException().getMessage());
            totalUsersLabel.setText("N/A");
            totalAttendeesLabel.setText("N/A");
            totalPresentersLabel.setText("N/A");
            totalEventsLabel.setText("N/A");
            totalSessionsLabel.setText("N/A");
            totalTicketsLabel.setText("N/A");
            activeEventsLabel.setText("N/A");
            dbStatusLabel.setText("🔴 Error");
            dbStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        });
        
        // Run in background thread
        new Thread(task).start();
    }
    
    // Inner class to hold statistics data
    private static class StatisticsData {
        int totalUsers;
        int totalAttendees;
        int totalPresenters;
        int totalEvents;
        int totalSessions;
        int totalTickets;
        int activeEvents;
        boolean dbConnected;

        StatisticsData(int totalUsers, int totalAttendees, int totalPresenters,
                      int totalEvents, int totalSessions, int totalTickets,
                      int activeEvents, boolean dbConnected) {
            this.totalUsers = totalUsers;
            this.totalAttendees = totalAttendees;
            this.totalPresenters = totalPresenters;
            this.totalEvents = totalEvents;
            this.totalSessions = totalSessions;
            this.totalTickets = totalTickets;
            this.activeEvents = activeEvents;
            this.dbConnected = dbConnected;
        }
    }

    @FXML
    public void onDashboard() {
        System.out.println("Dashboard clicked");
    }

    @FXML
    public void onManageUsers() {
        System.out.println("Manage Users clicked");
        SceneManager.switchTo("manage_users.fxml", "Event Manager System - Manage Users");
    }

    @FXML
    public void onManageEvents() {
        System.out.println("Manage Events clicked");
        SceneManager.switchTo("manage_events.fxml", "Event Manager System - Manage Events");
    }

    @FXML
    public void onManageSessions() {
        System.out.println("Manage Sessions clicked");
        SceneManager.switchTo("session_manager.fxml", "Event Manager System - Manage Sessions");
    }

    @FXML
    public void onManageTickets() {
        System.out.println("Manage Tickets clicked");
        SceneManager.switchTo("ticket_manager.fxml", "Event Manager System - Manage Tickets");
    }

    @FXML
    public void onManagePresenters() {
        System.out.println("Manage Presenters clicked");
        SceneManager.switchTo("presenter_manager.fxml", "Event Manager System - Manage Presenters");
    }

    @FXML
    public void onViewReports() {
        System.out.println("View Reports clicked");
        // TODO: Load reports page
    }

    @FXML
    public void onActivityLogs() {
        System.out.println("Activity Logs clicked");
        // TODO: Load activity logs page
    }

    @FXML
    public void onSettings() {
        System.out.println("Settings clicked");
        // TODO: Load settings page
    }

    @FXML
    public void onLogout() {
        System.out.println("Logout clicked");
        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
    }
}

