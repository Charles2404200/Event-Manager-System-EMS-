package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.ems.application.dto.UserCreateRequestDTO;
import org.ems.application.dto.UserDisplayRowDTO;
import org.ems.application.service.UserManagementService;
import org.ems.application.service.UserFilteringService;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;

import java.util.List;

/**
 * Manage Users Admin Page
 * SOLID REFACTORED:
 * - Single Responsibility: Controller handles only UI coordination
 * - Dependency Injection: Services are injected via constructor
 * - Delegation: Business logic delegated to appropriate services
 * - Clean Architecture: Separated concerns between UI, Services, and Data layers
 * @author <your group number>
 */
public class ManageUsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private TableView<UserDisplayRowDTO> usersTable;
    @FXML private Label recordCountLabel;
    @FXML private VBox loadingPlaceholder;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingPercentLabel;

    // Injected Services
    private UserManagementService userManagementService;
    private UserFilteringService userFilteringService;

    // UI State
    private List<UserDisplayRowDTO> allUsers;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("👥 [ManageUsersController] initialize() starting...");
        try {
            // Inject services from AppContext
            long servicesStart = System.currentTimeMillis();
            AppContext context = AppContext.get();
            userManagementService = new UserManagementService(
                context.attendeeRepo,
                context.presenterRepo
            );
            userFilteringService = new UserFilteringService();
            System.out.println("  ✓ Services initialized in " + (System.currentTimeMillis() - servicesStart) + " ms");

            // Setup role filter combo
            long comboStart = System.currentTimeMillis();
            roleFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "ATTENDEE", "PRESENTER", "SYSTEM_ADMIN", "EVENT_ADMIN"
            ));
            roleFilterCombo.setValue("ALL");
            System.out.println("  ✓ Filter combo setup in " + (System.currentTimeMillis() - comboStart) + " ms");

            // Setup table columns
            long colStart = System.currentTimeMillis();
            setupTableColumns();
            System.out.println("  ✓ Table columns setup in " + (System.currentTimeMillis() - colStart) + " ms");

            // Hide loading placeholder initially
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(false);
                loadingPlaceholder.setManaged(false);
            }

            System.out.println("  ✓ UI initialized in " + (System.currentTimeMillis() - initStart) + " ms");
            System.out.println("  🔄 Starting async load...");

            // Load all users asynchronously
            loadAllUsersAsync();

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup table columns with proper binding to UserDisplayRowDTO
     */
    private void setupTableColumns() {
        ObservableList<TableColumn<UserDisplayRowDTO, ?>> columns = usersTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUsername()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPhone()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRole()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDateOfBirth()));
            ((TableColumn<UserDisplayRowDTO, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCreatedDate()));
        }
    }

    /**
     * Load all users asynchronously via UserManagementService
     * Delegates complex data loading logic to service layer
     * Controller only handles UI state and callback coordination
     */
    private void loadAllUsersAsync() {
        long asyncStart = System.currentTimeMillis();

        showLoadingPlaceholder();

        // Create progress callback to update UI
        org.ems.application.service.UserDataLoaderService.ProgressCallback progressCallback =
            percent -> updateProgress(percent);

        AsyncTaskService.runAsync(
                () -> {
                    try {
                        // Delegate to service - clean separation of concerns
                        return userManagementService.loadAllUsersWithProgress(progressCallback);
                    } catch (UserManagementService.UserManagementException e) {
                        System.err.println("    ✗ Error in background task: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                result -> {
                    long uiStart = System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    List<UserDisplayRowDTO> users = (List<UserDisplayRowDTO>) result;

                    updateProgress(100);
                    allUsers = users;
                    displayUsers(allUsers);
                    System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
                    System.out.println("✓ ManageUsersController loaded successfully in " + (System.currentTimeMillis() - asyncStart) + " ms");
                    hideLoadingPlaceholder();
                },
                error -> {
                    System.err.println("✗ Error loading users: " + error.getMessage());
                    showAlert("Error", "Failed to load users: " + error.getMessage());
                    hideLoadingPlaceholder();
                }
        );
    }

    /**
     * Update progress bar and label on UI thread
     */
    private void updateProgress(int percent) {
        Platform.runLater(() -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(percent / 100.0);
            }
            if (loadingPercentLabel != null) {
                loadingPercentLabel.setText(percent + "%");
            }
        });
    }

    /**
     * Show loading placeholder and hide table
     */
    private void showLoadingPlaceholder() {
        Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(true);
                loadingPlaceholder.setManaged(true);
            }
            if (usersTable != null) {
                usersTable.setVisible(false);
                usersTable.setManaged(false);
            }
        });
    }

    /**
     * Hide loading placeholder and show table
     */
    private void hideLoadingPlaceholder() {
        Platform.runLater(() -> {
            if (loadingPlaceholder != null) {
                loadingPlaceholder.setVisible(false);
                loadingPlaceholder.setManaged(false);
            }
            if (usersTable != null) {
                usersTable.setVisible(true);
                usersTable.setManaged(true);
            }
        });
    }

    /**
     * Display users in table
     */
    private void displayUsers(List<UserDisplayRowDTO> users) {
        ObservableList<UserDisplayRowDTO> observableList = FXCollections.observableArrayList(users);
        usersTable.setItems(observableList);
        recordCountLabel.setText("Total Records: " + users.size());
    }

    /**
     * Handle search filter
     * Delegates filtering logic to UserFilteringService
     */
    @FXML
    public void onSearch() {
        long searchStart = System.currentTimeMillis();
        System.out.println("🔎 [ManageUsersController] onSearch() starting...");
        try {
            String searchTerm = searchField.getText();
            String roleFilter = roleFilterCombo.getValue();

            // Delegate to service - clean separation of concerns
            List<UserDisplayRowDTO> filtered = userFilteringService.filterUsers(allUsers, searchTerm, roleFilter);

            displayUsers(filtered);
            System.out.println("  ✓ onSearch() completed in " + (System.currentTimeMillis() - searchStart) +
                             " ms, filtered to " + filtered.size());

        } catch (Exception e) {
            System.err.println("✗ onSearch() failed: " + e.getMessage());
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    /**
     * Reset search and filter criteria
     */
    @FXML
    public void onReset() {
        long resetStart = System.currentTimeMillis();
        System.out.println("🔄 [ManageUsersController] onReset() called");
        searchField.clear();
        roleFilterCombo.setValue("ALL");
        displayUsers(allUsers);
        System.out.println("  ✓ onReset() completed in " + (System.currentTimeMillis() - resetStart) + " ms");
    }

    /**
     * Handle add user button click
     * Shows dialog for user input, delegates creation to service
     */
    @FXML
    public void onAddUser() {
        long addStart = System.currentTimeMillis();
        System.out.println("➕ [ManageUsersController] onAddUser() starting...");
        try {
            // Show form dialog
            UserCreateFormDialog dialog = new UserCreateFormDialog();
            UserCreateRequestDTO request = dialog.showAndWait();

            if (request != null) {
                // Delegate creation to service
                createUserAsync(request, addStart);
            }
        } catch (Exception e) {
            System.err.println("✗ onAddUser() exception: " + e.getMessage());
            showAlert("Error", "Failed to add user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Asynchronously create user via service
     */
    private void createUserAsync(UserCreateRequestDTO request, long addStart) {
        long saveStart = System.currentTimeMillis();

        AsyncTaskService.runAsync(
                () -> {
                    try {
                        // Delegate to service - clean separation of concerns
                        return userManagementService.createUser(request);
                    } catch (UserManagementService.UserManagementException e) {
                        System.err.println("  ✗ Error creating user: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                success -> {
                    showAlert("Success", "User '" + request.getUsername() + "' created successfully!");
                    loadAllUsersAsync();
                    System.out.println("✓ onAddUser() completed in " + (System.currentTimeMillis() - addStart) + " ms");
                },
                error -> {
                    showAlert("Error", "Failed to add user: " + error.getMessage());
                    System.err.println("✗ onAddUser() failed in " + (System.currentTimeMillis() - addStart) + " ms");
                }
        );
    }

    /**
     * Handle edit user button click
     */
    @FXML
    public void onEditUser() {
        System.out.println("✏️ [ManageUsersController] onEditUser() called");
        UserDisplayRowDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to edit");
            return;
        }
        showAlert("Info", "Edit User feature coming soon!");
    }

    /**
     * Handle delete user button click
     * Shows confirmation dialog, delegates deletion to service
     */
    @FXML
    public void onDeleteUser() {
        long deleteStart = System.currentTimeMillis();
        System.out.println("🗑️ [ManageUsersController] onDeleteUser() starting...");

        UserDisplayRowDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to delete");
            return;
        }

        // Show confirmation dialog
        if (showDeleteConfirmation(selected)) {
            deleteUserAsync(selected, deleteStart);
        }
    }

    /**
     * Show delete confirmation dialog
     */
    private boolean showDeleteConfirmation(UserDisplayRowDTO user) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete User - " + user.getUsername());
        confirmDialog.setContentText(
                "Are you sure you want to delete user: " + user.getUsername() + "?\n\n" +
                "Name: " + user.getFullName() + "\n" +
                "Email: " + user.getEmail() + "\n" +
                "Role: " + user.getRole() + "\n\n" +
                "This action CANNOT be undone!"
        );

        return confirmDialog.showAndWait().isPresent() &&
               confirmDialog.showAndWait().get() == ButtonType.OK;
    }

    /**
     * Asynchronously delete user via service
     */
    private void deleteUserAsync(UserDisplayRowDTO user, long deleteStart) {
        AsyncTaskService.runAsync(
                () -> {
                    try {
                        // Delegate to service - clean separation of concerns
                        java.util.UUID userId = java.util.UUID.fromString(user.getId());
                        userManagementService.deleteUser(userId, user.getRole());
                        return true;
                    } catch (UserManagementService.UserManagementException e) {
                        System.err.println("  ✗ Error deleting user: " + e.getMessage());
                        throw new RuntimeException(e);
                    }
                },
                success -> {
                    showAlert("Success", "User '" + user.getUsername() + "' has been deleted successfully!");
                    loadAllUsersAsync();
                    System.out.println("✓ onDeleteUser() completed in " + (System.currentTimeMillis() - deleteStart) + " ms");
                },
                error -> {
                    showAlert("Error", "Failed to delete user: " + error.getMessage());
                    System.err.println("✗ onDeleteUser() failed in " + (System.currentTimeMillis() - deleteStart) + " ms");
                }
        );
    }

    /**
     * Handle export button click
     */
    @FXML
    public void onExport() {
        System.out.println("📤 [ManageUsersController] onExport() called");
        showAlert("Info", "Export feature coming soon!");
    }

    /**
     * Handle back button click - navigate to admin dashboard
     */
    @FXML
    public void onBack() {
        System.out.println("🔙 [ManageUsersController] Back to admin dashboard");
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for user creation form dialog
     * Separated from controller to improve readability and testability
     */
    private static class UserCreateFormDialog {

        public UserCreateRequestDTO showAndWait() {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add New User");
            dialog.setHeaderText("Create a new user account");

            TextField fullNameField = new TextField();
            fullNameField.setPromptText("Full Name");
            fullNameField.setPrefWidth(300);

            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            emailField.setPrefWidth(300);

            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            usernameField.setPrefWidth(300);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            passwordField.setPrefWidth(300);

            TextField phoneField = new TextField();
            phoneField.setPromptText("Phone");
            phoneField.setPrefWidth(300);

            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.setItems(FXCollections.observableArrayList("ATTENDEE", "PRESENTER"));
            roleCombo.setValue("ATTENDEE");
            roleCombo.setPrefWidth(300);

            VBox formBox = new VBox(10);
            formBox.setPadding(new Insets(10));
            formBox.getChildren().addAll(
                    new Label("Full Name:"), fullNameField,
                    new Label("Email:"), emailField,
                    new Label("Username:"), usernameField,
                    new Label("Password:"), passwordField,
                    new Label("Phone:"), phoneField,
                    new Label("Role:"), roleCombo
            );

            dialog.getDialogPane().setContent(formBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                return new UserCreateRequestDTO(
                        fullNameField.getText(),
                        emailField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        phoneField.getText(),
                        roleCombo.getValue()
                );
            }
            return null;
        }
    }
}

