package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.ems.ui.stage.SceneManager;

/**
 * @author <your group number>
 */
public class AdminDashboardController {

    @FXML private Label adminInfoLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label totalEventsLabel;
    @FXML private Label totalSessionsLabel;
    @FXML private Label totalTicketsLabel;


    @FXML
    public void initialize() {
        // Get current admin from session/context (you may need to implement session storage)
        String adminUsername = "admin"; // TODO: Get from session
        adminInfoLabel.setText("Logged in as: " + adminUsername + " (SYSTEM_ADMIN)");

        // Load statistics
        loadStatistics();
    }

    private void loadStatistics() {
        try {
            // TODO: Get real statistics from repositories
            totalUsersLabel.setText("3");
            totalEventsLabel.setText("0");
            totalSessionsLabel.setText("0");
            totalTicketsLabel.setText("0");
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
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
        // TODO: Load ticket management page
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

