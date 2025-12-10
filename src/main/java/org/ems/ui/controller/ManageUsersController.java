package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import org.ems.config.AppContext;
import org.ems.domain.model.Attendee;
import org.ems.domain.model.Presenter;
import org.ems.domain.repository.AttendeeRepository;
import org.ems.domain.repository.PresenterRepository;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage Users Admin Page
 * OPTIMIZED: Async loading, batch queries, detailed logging
 * @author <your group number>
 */
public class ManageUsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private TableView<UserRow> usersTable;
    @FXML private Label recordCountLabel;
    @FXML private VBox loadingPlaceholder;
    @FXML private ProgressBar loadingProgressBar;
    @FXML private Label loadingPercentLabel;

    private AttendeeRepository attendeeRepo;
    private PresenterRepository presenterRepo;
    private List<UserRow> allUsers;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("👥 [ManageUsers] initialize() starting...");
        try {
            // Get repositories from context
            long appStart = System.currentTimeMillis();
            AppContext context = AppContext.get();
            attendeeRepo = context.attendeeRepo;
            presenterRepo = context.presenterRepo;
            System.out.println("  ✓ AppContext loaded in " + (System.currentTimeMillis() - appStart) + " ms");

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

    private void setupTableColumns() {
        ObservableList<TableColumn<UserRow, ?>> columns = usersTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<UserRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().id));
            ((TableColumn<UserRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().username));
            ((TableColumn<UserRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().fullName));
            ((TableColumn<UserRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().email));
            ((TableColumn<UserRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().phone));
            ((TableColumn<UserRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().role));
            ((TableColumn<UserRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().dob));
            ((TableColumn<UserRow, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().createdDate));
        }
    }

    /**
     * Load all users asynchronously - OPTIMIZED: Batch load
     */
    private void loadAllUsersAsync() {
        long asyncStart = System.currentTimeMillis();

        showLoadingPlaceholder();

        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background] Loading users...");

                    List<UserRow> users = new ArrayList<>();

                    try {
                        // Step 1: Load attendees
                        long attendeeStart = System.currentTimeMillis();
                        if (attendeeRepo != null) {
                            try {
                                List<Attendee> attendees = attendeeRepo.findAll();
                                long attendeeTime = System.currentTimeMillis() - attendeeStart;
                                System.out.println("    ✓ findAll(attendees) took " + attendeeTime + " ms: " + attendees.size());

                                for (Attendee attendee : attendees) {
                                    users.add(new UserRow(
                                            attendee.getId().toString(),
                                            attendee.getUsername(),
                                            attendee.getFullName(),
                                            attendee.getEmail(),
                                            attendee.getPhone(),
                                            "ATTENDEE",
                                            attendee.getDateOfBirth() != null ? attendee.getDateOfBirth().toString() : "N/A",
                                            "N/A"
                                    ));
                                }
                            } catch (Exception e) {
                                System.err.println("    ⚠️ Error loading attendees: " + e.getMessage());
                            }
                        }

                        updateProgress(30);

                        // Step 2: Load presenters
                        long presenterStart = System.currentTimeMillis();
                        if (presenterRepo != null) {
                            try {
                                List<Presenter> presenters = presenterRepo.findAll();
                                long presenterTime = System.currentTimeMillis() - presenterStart;
                                System.out.println("    ✓ findAll(presenters) took " + presenterTime + " ms: " + presenters.size());

                                for (Presenter presenter : presenters) {
                                    users.add(new UserRow(
                                            presenter.getId().toString(),
                                            presenter.getUsername(),
                                            presenter.getFullName(),
                                            presenter.getEmail(),
                                            presenter.getPhone(),
                                            "PRESENTER",
                                            presenter.getDateOfBirth() != null ? presenter.getDateOfBirth().toString() : "N/A",
                                            "N/A"
                                    ));
                                }
                            } catch (Exception e) {
                                System.err.println("    ⚠️ Error loading presenters: " + e.getMessage());
                            }
                        }

                        updateProgress(60);

                        // Step 3: Load admin users from persons table
                        long adminStart = System.currentTimeMillis();
                        try {
                            java.sql.Connection conn = org.ems.config.DatabaseConfig.getConnection();
                            String adminQuery = "SELECT * FROM persons WHERE role IN ('SYSTEM_ADMIN', 'EVENT_ADMIN')";
                            try (java.sql.Statement stmt = conn.createStatement()) {
                                java.sql.ResultSet rs = stmt.executeQuery(adminQuery);
                                int adminCount = 0;
                                while (rs.next()) {
                                    users.add(new UserRow(
                                            rs.getString("id"),
                                            rs.getString("username"),
                                            rs.getString("full_name"),
                                            rs.getString("email"),
                                            rs.getString("phone"),
                                            rs.getString("role"),
                                            rs.getString("dob") != null ? rs.getString("dob") : "N/A",
                                            rs.getString("created_at") != null ? rs.getString("created_at") : "N/A"
                                    ));
                                    adminCount++;
                                }
                                long adminTime = System.currentTimeMillis() - adminStart;
                                System.out.println("    ✓ Admin query took " + adminTime + " ms: " + adminCount);
                            }
                        } catch (Exception e) {
                            System.err.println("    ⚠️ Error loading admin users: " + e.getMessage());
                        }

                        updateProgress(90);

                        long totalTime = System.currentTimeMillis() - taskStart;
                        System.out.println("    ✓ Background task completed in " + totalTime + " ms");
                        System.out.println("    ✓ Total users loaded: " + users.size());

                        return users;

                    } catch (Exception e) {
                        System.err.println("    ✗ Error loading users: " + e.getMessage());
                        e.printStackTrace();
                        return new ArrayList<>();
                    }
                },
                result -> {
                    long uiStart = System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    List<UserRow> users = (List<UserRow>) result;

                    updateProgress(100);
                    allUsers = users;
                    displayUsers(allUsers);
                    System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
                    System.out.println("✓ ManageUsers loaded successfully in " + (System.currentTimeMillis() - asyncStart) + " ms");
                    hideLoadingPlaceholder();
                },
                error -> {
                    System.err.println("✗ Error loading users: " + error.getMessage());
                    showAlert("Error", "Failed to load users: " + error.getMessage());
                    hideLoadingPlaceholder();
                }
        );
    }

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

    private void displayUsers(List<UserRow> users) {
        ObservableList<UserRow> observableList = FXCollections.observableArrayList(users);
        usersTable.setItems(observableList);
        recordCountLabel.setText("Total Records: " + users.size());
    }

    @FXML
    public void onSearch() {
        long searchStart = System.currentTimeMillis();
        System.out.println("🔎 [ManageUsers] onSearch() starting...");
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String roleFilter = roleFilterCombo.getValue();

            List<UserRow> filtered = new ArrayList<>();

            for (UserRow user : allUsers) {
                // Apply role filter
                if (!roleFilter.equals("ALL") && !user.role.equals(roleFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    user.username.toLowerCase().contains(searchTerm) ||
                    user.fullName.toLowerCase().contains(searchTerm) ||
                    user.email.toLowerCase().contains(searchTerm)) {
                    filtered.add(user);
                }
            }

            displayUsers(filtered);
            System.out.println("  ✓ onSearch() completed in " + (System.currentTimeMillis() - searchStart) + " ms, filtered to " + filtered.size());

        } catch (Exception e) {
            System.err.println("✗ onSearch() failed: " + e.getMessage());
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        long resetStart = System.currentTimeMillis();
        System.out.println("🔄 [ManageUsers] onReset() called");
        searchField.clear();
        roleFilterCombo.setValue("ALL");
        displayUsers(allUsers);
        System.out.println("  ✓ onReset() completed in " + (System.currentTimeMillis() - resetStart) + " ms");
    }

    @FXML
    public void onAddUser() {
        long addStart = System.currentTimeMillis();
        System.out.println("➕ [ManageUsers] onAddUser() starting...");
        try {
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
                String fullName = fullNameField.getText();
                String email = emailField.getText();
                String username = usernameField.getText();
                String password = passwordField.getText();
                String phone = phoneField.getText();
                String role = roleCombo.getValue();

                if (fullName.isBlank() || email.isBlank() || username.isBlank() || password.isBlank()) {
                    showAlert("Validation Error", "Please fill in all required fields");
                    return;
                }

                if (!email.contains("@")) {
                    showAlert("Validation Error", "Please enter a valid email");
                    return;
                }

                if (password.length() < 6) {
                    showAlert("Validation Error", "Password must be at least 6 characters");
                    return;
                }

                long saveStart = System.currentTimeMillis();
                AsyncTaskService.runAsync(
                        () -> {
                            try {
                                if (role.equals("ATTENDEE")) {
                                    Attendee attendee = new Attendee(fullName, LocalDate.now(), email, phone, username, password);
                                    attendeeRepo.save(attendee);
                                    System.out.println("  ✓ Attendee saved in " + (System.currentTimeMillis() - saveStart) + " ms");
                                } else if (role.equals("PRESENTER")) {
                                    Presenter presenter = new Presenter(fullName, LocalDate.now(), email, phone, username, password);
                                    presenterRepo.save(presenter);
                                    System.out.println("  ✓ Presenter saved in " + (System.currentTimeMillis() - saveStart) + " ms");
                                }
                                return true;
                            } catch (Exception e) {
                                System.err.println("  ✗ Error saving user: " + e.getMessage());
                                e.printStackTrace();
                                return false;
                            }
                        },
                        success -> {
                            showAlert("Success", "User '" + username + "' created successfully!");
                            loadAllUsersAsync();
                            System.out.println("✓ onAddUser() completed in " + (System.currentTimeMillis() - addStart) + " ms");
                        },
                        error -> {
                            showAlert("Error", "Failed to add user: " + error.getMessage());
                            System.err.println("✗ onAddUser() failed in " + (System.currentTimeMillis() - addStart) + " ms");
                        }
                );
            }
        } catch (Exception e) {
            System.err.println("✗ onAddUser() exception: " + e.getMessage());
            showAlert("Error", "Failed to add user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onEditUser() {
        System.out.println("✏️ [ManageUsers] onEditUser() called");
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to edit");
            return;
        }
        showAlert("Info", "Edit User feature coming soon!");
    }

    @FXML
    public void onDeleteUser() {
        long deleteStart = System.currentTimeMillis();
        System.out.println("🗑️ [ManageUsers] onDeleteUser() starting...");
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to delete");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText("Delete User - " + selected.username);
        confirmDialog.setContentText(
                "Are you sure you want to delete user: " + selected.username + "?\n\n" +
                "Name: " + selected.fullName + "\n" +
                "Email: " + selected.email + "\n" +
                "Role: " + selected.role + "\n\n" +
                "This action CANNOT be undone!"
        );

        if (confirmDialog.showAndWait().isPresent() && confirmDialog.showAndWait().get() == ButtonType.OK) {
            AsyncTaskService.runAsync(
                    () -> {
                        long taskStart = System.currentTimeMillis();
                        try {
                            java.util.UUID userId = java.util.UUID.fromString(selected.id);

                            if (selected.role.equals("ATTENDEE")) {
                                if (attendeeRepo != null) {
                                    attendeeRepo.delete(userId);
                                    System.out.println("  ✓ Deleted attendee in " + (System.currentTimeMillis() - taskStart) + " ms");
                                }
                            } else if (selected.role.equals("PRESENTER")) {
                                if (presenterRepo != null) {
                                    presenterRepo.delete(userId);
                                    System.out.println("  ✓ Deleted presenter in " + (System.currentTimeMillis() - taskStart) + " ms");
                                }
                            } else {
                                System.out.println("  ℹ Cannot delete admin users from UI");
                                return false;
                            }
                            return true;
                        } catch (Exception e) {
                            System.err.println("  ✗ Error deleting user: " + e.getMessage());
                            e.printStackTrace();
                            return false;
                        }
                    },
                    success -> {
                        showAlert("Success", "User '" + selected.username + "' has been deleted successfully!");
                        loadAllUsersAsync();
                        System.out.println("✓ onDeleteUser() completed in " + (System.currentTimeMillis() - deleteStart) + " ms");
                    },
                    error -> {
                        showAlert("Error", "Failed to delete user: " + error.getMessage());
                        System.err.println("✗ onDeleteUser() failed in " + (System.currentTimeMillis() - deleteStart) + " ms");
                    }
            );
        }
    }

    @FXML
    public void onExport() {
        System.out.println("📤 [ManageUsers] onExport() called");
        showAlert("Info", "Export feature coming soon!");
    }

    @FXML
    public void onBack() {
        System.out.println("🔙 [ManageUsers] Back to admin dashboard");
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class UserRow {
        public String id;
        public String username;
        public String fullName;
        public String email;
        public String phone;
        public String role;
        public String dob;
        public String createdDate;

        public UserRow(String id, String username, String fullName, String email,
                      String phone, String role, String dob, String createdDate) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.role = role;
            this.dob = dob;
            this.createdDate = createdDate;
        }
    }
}

