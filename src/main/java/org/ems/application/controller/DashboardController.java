package org.ems.application.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import javafx.application.Platform;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.domain.model.enums.Role;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;
import org.ems.application.service.user.UserProfileService;
import org.ems.application.service.dashboard.DashboardAttendeeService;
import org.ems.application.service.dashboard.DashboardPresenterService;
import org.ems.application.service.dashboard.DashboardEventAdminService;
import org.ems.application.service.dashboard.DashboardEventFilteringService;
import org.ems.application.service.attendee.AttendeeScheduleExportService;
import org.ems.application.dto.dashboard.DashboardAttendeeContentDTO;
import org.ems.application.dto.dashboard.DashboardPresenterContentDTO;
import org.ems.application.dto.dashboard.DashboardEventAdminContentDTO;

import java.util.List;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;

/**
 * Dashboard Controller - SOLID REFACTORED
 * - Single Responsibility: UI coordination only
 * - Dependency Injection: Services injected via AppContext
 * - Delegation: Business logic delegated to appropriate services
 * - Clean Architecture: Separated concerns between UI, Services, and Data layers
 * @author <your group number>
 */
public class DashboardController {

    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label nOAssignedSessions;

    // Role-specific sections
    @FXML private VBox attendeeSection;
    @FXML private VBox presenterSection;
    @FXML private VBox eventAdminSection;
    @FXML private VBox systemAdminSection;

    private final AppContext appContext = AppContext.get();
    private Person currentUser;
    private Role userRole;
    private LoadingDialog loadingDialog;

    // Injected Services
    private UserProfileService userProfileService;
    private DashboardAttendeeService dashboardAttendeeService;
    private DashboardPresenterService dashboardPresenterService;
    private DashboardEventAdminService dashboardEventAdminService;
    private DashboardEventFilteringService dashboardEventFilteringService;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println(" [DashboardController] initialize() starting...");

        try {
            // Inject services from AppContext
            long serviceStart = System.currentTimeMillis();
            userProfileService = new UserProfileService();
            dashboardAttendeeService = new DashboardAttendeeService(appContext.ticketRepo, appContext.eventRepo);
            dashboardPresenterService = new DashboardPresenterService(appContext.sessionRepo);
            dashboardEventAdminService = new DashboardEventAdminService(
                    appContext.eventRepo, appContext.sessionRepo, appContext.ticketRepo);
            dashboardEventFilteringService = new DashboardEventFilteringService();
            System.out.println("  ✓ Services initialized in " + (System.currentTimeMillis() - serviceStart) + "ms");

            // Load user info
            loadUserInfo();

            // Setup role-based UI
            setupRoleBasedView();

            // Load dynamic content asynchronously
            loadDynamicContentAsync();

            System.out.println("✓ Dashboard initialized in " + (System.currentTimeMillis() - initStart) + "ms");

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load current user information from AppContext
     */
    private void loadUserInfo() {
        long start = System.currentTimeMillis();
        System.out.println("👤 [DashboardController] Loading user info...");

        currentUser = appContext.currentUser;

        if (currentUser == null) {
            // No user logged in, redirect to login
            SceneManager.switchTo("login.fxml", "EMS - Login");
            return;
        }

        userRole = currentUser.getRole();

        // Display user information
        userNameLabel.setText("Welcome, " + currentUser.getFullName());
        userRoleLabel.setText("Role: " + userProfileService.getRoleDisplayName(userRole));
        userEmailLabel.setText(currentUser.getEmail());

        System.out.println("  ✓ User info loaded in " + (System.currentTimeMillis() - start) + "ms");
    }

    /**
     * Load dynamic content based on user role - asynchronously
     * Delegates all business logic to services
     */
    private void loadDynamicContentAsync() {
        long dashStart = System.currentTimeMillis();
        System.out.println("📊 [DashboardController] loadDynamicContentAsync() starting...");

        // Get the primary stage safely
        javafx.stage.Stage primaryStage = null;
        try {
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
                // Background task - delegate to services
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("  🔄 [Background] Loading content for role: " + userRole);
                    try {
                        switch (userRole) {
                            case ATTENDEE:
                                loadAttendeeContentViaService();
                                System.out.println("  ✓ Attendee content loaded in " + (System.currentTimeMillis() - taskStart) + " ms");
                                break;
                            case PRESENTER:
                                loadPresenterContentViaService();
                                System.out.println("  ✓ Presenter content loaded in " + (System.currentTimeMillis() - taskStart) + " ms");
                                break;
                            case EVENT_ADMIN:
                                loadEventAdminContentViaService();
                                System.out.println("  ✓ Event admin content loaded in " + (System.currentTimeMillis() - taskStart) + " ms");
                                break;
                            case SYSTEM_ADMIN:
                                System.out.println("  ✓ System admin - no dynamic content");
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
                    System.out.println("✓ Dashboard loaded successfully in " + (System.currentTimeMillis() - dashStart) + " ms");
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
     * Load attendee content via service
     * Delegates all business logic to DashboardAttendeeService
     */
    private void loadAttendeeContentViaService() {
        try {
            if (!(currentUser instanceof Attendee)) {
                System.out.println("  ⚠️ User is not attendee");
                return;
            }

            Attendee attendee = (Attendee) currentUser;
            VBox upcomingEventsBox = findVBoxByTitle(attendeeSection, "Your Upcoming Events");
            if (upcomingEventsBox == null) {
                System.out.println("  ⚠️ upcomingEventsBox not found");
                return;
            }

            // Delegate to service
            DashboardAttendeeContentDTO content = dashboardAttendeeService.loadAttendeeContent(attendee);

            // Update UI on FX thread
            Platform.runLater(() -> {
                long uiStart = System.currentTimeMillis();
                updateUpcomingEventsDisplay(upcomingEventsBox, content.getUpcomingEvents());
                System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
            });

        } catch (DashboardAttendeeService.DashboardException e) {
            System.err.println("Error loading attendee content: " + e.getMessage());
            Platform.runLater(() -> {
                VBox upcomingEventsBox = findVBoxByTitle(attendeeSection, "Your Upcoming Events");
                if (upcomingEventsBox != null) {
                    Label errorLabel = new Label("Error loading events: " + e.getMessage());
                    errorLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #e74c3c;");
                    upcomingEventsBox.getChildren().clear();
                    upcomingEventsBox.getChildren().add(errorLabel);
                }
            });
        }
    }


    /**
     * Load presenter content via service
     * Delegates all business logic to DashboardPresenterService
     */
    private void loadPresenterContentViaService() {
        try {
            if (!(currentUser instanceof Presenter)) {
                System.out.println("  ⚠️ User is not presenter");
                return;
            }

            Presenter presenter = (Presenter) currentUser;

            // Delegate to service
            DashboardPresenterContentDTO content = dashboardPresenterService.loadPresenterContent(presenter);

            // Update UI on FX thread
            Platform.runLater(() -> {
                if (nOAssignedSessions != null) {
                    nOAssignedSessions.setText(String.valueOf(content.getAssignedSessionsCount()));
                }
                System.out.println("  ✓ Presenter UI updated with " + content.getAssignedSessionsCount() + " sessions");
            });

        } catch (DashboardPresenterService.DashboardPresenterException e) {
            System.err.println("Error loading presenter content: " + e.getMessage());
        }
    }

    /**
     * Load event admin content via service
     * Delegates all business logic to DashboardEventAdminService
     */
    private void loadEventAdminContentViaService() {
        try {
            // Delegate to service
            DashboardEventAdminContentDTO content = dashboardEventAdminService.loadEventAdminContent();

            System.out.println("  ℹ Event Admin - Events: " + content.getTotalEvents() +
                             ", Sessions: " + content.getTotalSessions() +
                             ", Tickets: " + content.getTotalTickets());

        } catch (DashboardEventAdminService.DashboardEventAdminException e) {
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
     * Create a display box for an upcoming event - OPTIMIZED: No DB queries, just display
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
        long exportStart = System.currentTimeMillis();
        System.out.println("📅 [DashboardController] Export My Schedule clicked");

        // Only attendees can export schedule
        if (!(currentUser instanceof Attendee)) {
            showErrorDialog("Access Denied", "Only attendees can export schedule");
            return;
        }

        Attendee attendee = (Attendee) currentUser;

        // Step 1: Prompt for export format
        String exportFormat = promptForExportFormat();
        if (exportFormat == null) {
            System.out.println("  ⚠️ Export format selection cancelled");
            return;
        }
        System.out.println("  ✓ Selected format: " + exportFormat);

        // Step 2: Get Downloads folder as default output path
        String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
        System.out.println("  ✓ Output path: " + downloadsPath);

        // Step 3: Show loading dialog
        javafx.stage.Stage primaryStage = null;
        try {
            if (attendeeSection != null && attendeeSection.getScene() != null) {
                primaryStage = (javafx.stage.Stage) attendeeSection.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("Could not get primary stage: " + e.getMessage());
        }

        LoadingDialog exportDialog = null;
        if (primaryStage != null) {
            exportDialog = new LoadingDialog(primaryStage, "Exporting schedule as " + exportFormat + "...");
            exportDialog.show();
        }

        // Step 4: Export schedule asynchronously
        final LoadingDialog finalExportDialog = exportDialog;
        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    long taskStart = System.currentTimeMillis();
                    try {
                        System.out.println("  🔄 [Background] Exporting schedule for " + attendee.getFullName());

                        // Create service and export
                        AttendeeScheduleExportService exportService = new AttendeeScheduleExportService(
                                appContext.ticketRepo,
                                appContext.eventRepo,
                                appContext.sessionRepo
                        );

                        String filePath;
                        switch (exportFormat.toLowerCase()) {
                            case "csv":
                                filePath = exportService.exportScheduleToCSV(attendee, downloadsPath);
                                break;
                            case "excel":
                                filePath = exportService.exportScheduleToExcel(attendee, downloadsPath);
                                break;
                            case "pdf":
                                filePath = exportService.exportScheduleToPDF(attendee, downloadsPath);
                                break;
                            default:
                                throw new IllegalArgumentException("Unsupported format: " + exportFormat);
                        }

                        System.out.println("  ✓ Schedule exported in " + (System.currentTimeMillis() - taskStart) + "ms");
                        System.out.println("  ✓ File: " + filePath);
                        return filePath;

                    } catch (AttendeeScheduleExportService.ScheduleExportException e) {
                        System.err.println("  ✗ Export error: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },

                // Success callback
                result -> {
                    if (finalExportDialog != null) {
                        finalExportDialog.close();
                    }
                    System.out.println("✓ Export completed in " + (System.currentTimeMillis() - exportStart) + "ms");
                    showSuccessDialog("Export Successful",
                        "Your schedule has been exported successfully!\n\nFile: " + result);
                },

                // Error callback
                error -> {
                    if (finalExportDialog != null) {
                        finalExportDialog.close();
                    }
                    System.err.println("✗ Export failed: " + error.getMessage());
                    showErrorDialog("Export Failed",
                        "Failed to export schedule:\n\n" + error.getMessage());
                }
        );
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

        SceneManager.switchTo("presenter_assigned_sessions.fxml", "My assigned sessions");
    }

    @FXML
    public void onUploadMaterials() {
        long uploadStart = System.currentTimeMillis();
        System.out.println("📤 [DashboardController] Upload materials clicked");

        // Step 1: Prompt for session selection FIRST
        String sessionId = promptForSessionId();
        if (sessionId == null) {
            System.out.println("  ⚠️ Session selection cancelled");
            return;
        }
        System.out.println("  ✓ Selected session: " + sessionId);

        // Step 2: Prompt for file selection
        String filePath = promptForMaterialFile();
        if (filePath == null) {
            System.out.println("  ⚠️ Material file selection cancelled");
            return;
        }
        System.out.println("  ✓ Selected file: " + filePath);

        // Step 3: Show loading dialog
        javafx.stage.Stage primaryStage = null;
        try {
            if (presenterSection != null && presenterSection.getScene() != null) {
                primaryStage = (javafx.stage.Stage) presenterSection.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("Could not get primary stage: " + e.getMessage());
        }

        LoadingDialog uploadDialog = null;
        if (primaryStage != null) {
            uploadDialog = new LoadingDialog(primaryStage, "Uploading material...");
            uploadDialog.show();
        }

        // Step 4: Upload material asynchronously
        final LoadingDialog finalUploadDialog = uploadDialog;
        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    long taskStart = System.currentTimeMillis();
                    try {
                        System.out.println("  🔄 [Background] Uploading material to session: " + sessionId);
                        String materialPath = dashboardPresenterService.uploadSessionMaterial(sessionId, filePath);
                        System.out.println("  ✓ Material uploaded in " + (System.currentTimeMillis() - taskStart) + "ms");
                        System.out.println("  ✓ Material path: " + materialPath);
                        return materialPath;
                    } catch (DashboardPresenterService.DashboardPresenterException e) {
                        System.err.println("  ✗ Upload error: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },

                // Success callback
                result -> {
                    if (finalUploadDialog != null) {
                        finalUploadDialog.close();
                    }
                    System.out.println("✓ Upload completed in " + (System.currentTimeMillis() - uploadStart) + "ms");
                    showSuccessDialog("Material uploaded successfully!",
                        "Your material has been saved to session.\n\nPath: " + result);
                },

                // Error callback
                error -> {
                    if (finalUploadDialog != null) {
                        finalUploadDialog.close();
                    }
                    System.err.println("✗ Upload failed: " + error.getMessage());
                    showErrorDialog("Upload Failed",
                        "Failed to upload material:\n\n" + error.getMessage());
                }
        );
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
        SceneManager.switchTo(
                "presenter_activity_export.fxml",
                "Export Presenter Activity Summary"
        );
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

        if (appContext.currentUser instanceof Presenter) {
            SceneManager.switchTo("presenter_profile.fxml", "Update Profile");
        }


    }

    @FXML
    public void onLogout() {
        appContext.currentUser = null;
        appContext.currentUserRole = "VISITOR";
        SceneManager.switchTo("home.fxml", "EMS - Home");
    }

    /**
     * Prompt user to select export format (CSV, Excel, PDF)
     */
    private String promptForExportFormat() {
        try {
            List<String> formats = java.util.Arrays.asList("CSV", "Excel", "PDF");
            javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(formats.get(0), formats);
            dialog.setTitle("Export Schedule");
            dialog.setHeaderText("Select export format");
            dialog.setContentText("Format:");
            java.util.Optional<String> result = dialog.showAndWait();
            return result.orElse(null);
        } catch (Exception e) {
            System.err.println("Error in promptForExportFormat: " + e.getMessage());
            return null;
        }
    }

    // Helper stubs for file/session selection
    private String promptForMaterialFile() {
        try {
            Stage stage = (Stage) presenterSection.getScene().getWindow();
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Chọn file tài liệu để upload");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tài liệu", "*.pdf", "*.ppt", "*.pptx", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.txt")
            );
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                return selectedFile.getAbsolutePath();
            }
        } catch (Exception e) {
            System.err.println("Không thể chọn file: " + e.getMessage());
        }
        return null;
    }

    private String promptForSessionId() {
        if (currentUser instanceof Presenter) {
            List<Session> sessions = appContext.sessionRepo.findByPresenter(currentUser.getId(), 0, 50);
            if (sessions == null || sessions.isEmpty()) return null;
            if (sessions.size() == 1) return sessions.get(0).getId().toString();
            List<String> sessionTitles = sessions.stream().map(Session::getTitle).toList();
            javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(sessionTitles.get(0), sessionTitles);
            dialog.setTitle("Chọn session để upload tài liệu");
            dialog.setHeaderText("Vui lòng chọn một session");
            dialog.setContentText("Session:");
            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String selectedTitle = result.get();
                for (Session s : sessions) {
                    if (s.getTitle().equals(selectedTitle)) {
                        return s.getId().toString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Show success dialog
     */
    private void showSuccessDialog(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show error dialog
     */
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
