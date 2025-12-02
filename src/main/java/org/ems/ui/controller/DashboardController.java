package org.ems.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.ems.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.domain.model.enums.Role;
import org.ems.ui.stage.SceneManager;


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

    @FXML
    public void initialize() {
        loadUserInfo();
        setupRoleBasedView();
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
        // TODO: View attendee's tickets
        System.out.println("View My Tickets clicked");
    }

    @FXML
    public void onViewMyRegistrations() {
        // TODO: View attendee's registered sessions
        System.out.println("View My Registrations clicked");
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
