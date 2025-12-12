package org.ems.application.controller;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import org.ems.application.service.auth.RoleAuthorizationService;
import org.ems.domain.model.enums.Role;
import org.ems.infrastructure.config.AppContext;
import org.ems.infrastructure.repository.jdbc.JdbcEventRepository;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    // Menu buttons
    @FXML private Button manageUsersBtn;
    @FXML private Button managePresentersBtn;
    @FXML private Button settingsBtn;

    private AppContext appContext;

    @FXML
    public void initialize() {
        appContext = AppContext.get();
        String adminUsername = appContext.currentUser != null ? appContext.currentUser.getUsername() : "admin";
        String roleDisplay = RoleAuthorizationService.getRoleDisplayName(appContext.currentUser);

        adminInfoLabel.setText("Logged in as: " + adminUsername + " (" + roleDisplay + ")");

        // Restrict features based on role
        restrictFeaturesByRole();

        loadStatisticsAsync();
    }

    /**
     * Restrict UI features based on user role
     */
    private void restrictFeaturesByRole() {
        if (appContext.currentUser == null) {
            return;
        }

        boolean isSystemAdmin = appContext.currentUser.getRole() == Role.SYSTEM_ADMIN;
        boolean isEventAdmin = appContext.currentUser.getRole() == Role.EVENT_ADMIN;

        // EVENT_ADMIN cannot manage users
        if (manageUsersBtn != null) {
            manageUsersBtn.setDisable(!isSystemAdmin);
            manageUsersBtn.setStyle(isSystemAdmin ?
                "-fx-padding: 10; -fx-font-size: 12; -fx-cursor: hand;" :
                "-fx-padding: 10; -fx-font-size: 12; -fx-opacity: 0.5; -fx-cursor: not-allowed;");
        }

        // EVENT_ADMIN cannot manage presenters
        if (managePresentersBtn != null) {
            managePresentersBtn.setDisable(!isSystemAdmin);
            managePresentersBtn.setStyle(isSystemAdmin ?
                "-fx-padding: 10; -fx-font-size: 12; -fx-cursor: hand;" :
                "-fx-padding: 10; -fx-font-size: 12; -fx-opacity: 0.5; -fx-cursor: not-allowed;");
        }

        // EVENT_ADMIN cannot access settings
        if (settingsBtn != null) {
            settingsBtn.setDisable(!isSystemAdmin);
            settingsBtn.setStyle(isSystemAdmin ?
                "-fx-padding: 10; -fx-font-size: 12; -fx-cursor: hand;" :
                "-fx-padding: 10; -fx-font-size: 12; -fx-opacity: 0.5; -fx-cursor: not-allowed;");
        }

        System.out.println("✓ Features restricted for: " + RoleAuthorizationService.getRoleDisplayName(appContext.currentUser));
    }

    private void loadStatisticsAsync() {
        Task<StatisticsData> task = new Task<>() {
            @Override
            protected StatisticsData call() {
                try {
                    AppContext ctx = AppContext.get();
                    boolean dbConnected = ctx.connection != null && !ctx.connection.isClosed();

                    int totalUsers = 0; // userRepo hiện null, để 0 cho nhanh
                    int totalAttendees = 0;
                    int totalPresenters = 0;
                    int totalEvents = 0;
                    int totalSessions = 0;
                    int totalTickets = 0;
                    int activeEvents = 0;

                    if (dbConnected) {
                        if (ctx.attendeeRepo != null) {
                            totalAttendees = (int) ctx.attendeeRepo.count();
                        }
                        if (ctx.presenterRepo != null) {
                            totalPresenters = (int) ctx.presenterRepo.count();
                        }
                        if (ctx.sessionRepo != null) {
                            totalSessions = (int) ctx.sessionRepo.count();
                        }
                        if (ctx.ticketRepo != null) {
                            totalTickets = (int) ctx.ticketRepo.count();
                        }
                        if (ctx.eventRepo != null) {
                            // Dùng COUNT(*) thay vì findAll().size()
                            if (ctx.eventRepo instanceof JdbcEventRepository jdbcEventRepo) {
                                totalEvents = (int) jdbcEventRepo.count();
                                activeEvents = (int) jdbcEventRepo.countActiveEvents();
                            } else {
                                totalEvents = ctx.eventRepo.findAll().size();
                                activeEvents = 0; // fallback đơn giản
                            }
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

        task.setOnSucceeded(evt -> {
            StatisticsData s = task.getValue();
            totalUsersLabel.setText(String.valueOf(s.totalUsers));
            totalAttendeesLabel.setText(String.valueOf(s.totalAttendees));
            totalPresentersLabel.setText(String.valueOf(s.totalPresenters));
            totalEventsLabel.setText(String.valueOf(s.totalEvents));
            totalSessionsLabel.setText(String.valueOf(s.totalSessions));
            totalTicketsLabel.setText(String.valueOf(s.totalTickets));
            activeEventsLabel.setText(String.valueOf(s.activeEvents));

            if (s.dbConnected) {
                dbStatusLabel.setText("🟢 Connected");
                dbStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            } else {
                dbStatusLabel.setText("🔴 Disconnected");
                dbStatusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }

            LocalDateTime now = LocalDateTime.now();
            lastUpdateLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });

        task.setOnFailed(evt -> {
            System.err.println("Failed to load statistics: " + task.getException());
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

        Thread t = new Thread(task, "admin-stats-loader");
        t.setDaemon(true);
        t.start();
    }

    private record StatisticsData(
            int totalUsers,
            int totalAttendees,
            int totalPresenters,
            int totalEvents,
            int totalSessions,
            int totalTickets,
            int activeEvents,
            boolean dbConnected
    ) {}

    @FXML
    public void onDashboard() {
        System.out.println("Dashboard clicked");
    }

    @FXML
    public void onManageUsers() {
        if (!RoleAuthorizationService.canManageUsers(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to manage users");
            return;
        }
        SceneManager.switchTo("manage_users.fxml", "Event Manager System - Manage Users");
    }

    @FXML
    public void onManageEvents() {
        if (!RoleAuthorizationService.canManageEvents(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to manage events");
            return;
        }
        SceneManager.switchTo("manage_events.fxml", "Event Manager System - Manage Events");
    }

    @FXML
    public void onManageSessions() {
        if (!RoleAuthorizationService.canManageSessions(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to manage sessions");
            return;
        }
        SceneManager.switchTo("session_manager.fxml", "Event Manager System - Manage Sessions");
    }

    @FXML
    public void onManageTickets() {
        if (!RoleAuthorizationService.canManageTickets(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to manage tickets");
            return;
        }
        SceneManager.switchTo("ticket_manager.fxml", "Event Manager System - Manage Tickets");
    }

    @FXML
    public void onManagePresenters() {
        if (!RoleAuthorizationService.canManagePresenters(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to manage presenters");
            return;
        }
        SceneManager.switchTo("presenter_manager.fxml", "Event Manager System - Manage Presenters");
    }

    @FXML
    public void onViewReports() {
        if (!RoleAuthorizationService.canViewReports(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to view reports");
            return;
        }
        SceneManager.switchTo("report_view.fxml", "Event Manager System - Reports & Analytics");
    }

    @FXML
    public void onActivityLogs() {
        if (!RoleAuthorizationService.canViewActivityLogs(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to view activity logs");
            return;
        }
        SceneManager.switchTo("activity_logs.fxml", "Event Manager System - Activity Logs");
    }

    @FXML
    public void onSettings() {
        if (!RoleAuthorizationService.canAccessSettings(appContext.currentUser)) {
            showUnauthorizedAlert("You don't have permission to access settings");
            return;
        }
        System.out.println("Settings clicked");
    }

    @FXML
    public void onLogout() {
        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
    }

    /**
     * Show unauthorized access alert
     */
    private void showUnauthorizedAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Access Denied");
        alert.setHeaderText("Insufficient Permissions");
        alert.setContentText(message + "\n\nYour role: " + RoleAuthorizationService.getRoleDisplayName(appContext.currentUser));
        alert.showAndWait();
        System.out.println("⚠️ Unauthorized access attempt: " + message);
    }
}
