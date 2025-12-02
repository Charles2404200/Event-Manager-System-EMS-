package org.ems.ui.controller;

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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <your group number>
 */
public class ManageUsersController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private TableView<UserRow> usersTable;
    @FXML private Label recordCountLabel;

    private AttendeeRepository attendeeRepo;
    private PresenterRepository presenterRepo;
    private List<UserRow> allUsers;

    @FXML
    public void initialize() {
        try {
            // Get repositories from context
            AppContext context = AppContext.get();
            attendeeRepo = context.attendeeRepo;
            presenterRepo = context.presenterRepo;

            // Setup role filter combo
            roleFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "ATTENDEE", "PRESENTER", "SYSTEM_ADMIN", "EVENT_ADMIN"
            ));
            roleFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all users
            loadAllUsers();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void setupTableColumns() {
        // Get columns and bind to UserRow properties
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

    private void loadAllUsers() {
        try {
            allUsers = new ArrayList<>();

            // Load attendees (Attendee extends Person)
            if (attendeeRepo != null) {
                try {
                    List<Attendee> attendees = attendeeRepo.findAll();
                    for (Attendee attendee : attendees) {
                        allUsers.add(new UserRow(
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
                    System.out.println(" Loaded " + attendees.size() + " attendees");
                } catch (Exception e) {
                    System.err.println("⚠ Error loading attendees: " + e.getMessage());
                }
            }

            // Load presenters (Presenter extends Person)
            if (presenterRepo != null) {
                try {
                    List<Presenter> presenters = presenterRepo.findAll();
                    for (Presenter presenter : presenters) {
                        allUsers.add(new UserRow(
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
                    System.out.println(" Loaded " + presenters.size() + " presenters");
                } catch (Exception e) {
                    System.err.println("️ Error loading presenters: " + e.getMessage());
                }
            }

            // Load SYSTEM_ADMIN and EVENT_ADMIN users from persons table
            // (These are Person objects that don't have attendee/presenter subtables)
            try {
                java.sql.Connection conn = org.ems.config.DatabaseConfig.getConnection();
                String adminQuery = "SELECT * FROM persons WHERE role IN ('SYSTEM_ADMIN', 'EVENT_ADMIN')";
                try (java.sql.Statement stmt = conn.createStatement()) {
                    java.sql.ResultSet rs = stmt.executeQuery(adminQuery);
                    int adminCount = 0;
                    while (rs.next()) {
                        allUsers.add(new UserRow(
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
                    System.out.println(" Loaded " + adminCount + " admin users");
                }
            } catch (Exception e) {
                System.err.println("⚠ Error loading admin users: " + e.getMessage());
            }

            displayUsers(allUsers);
            System.out.println(" Total users loaded: " + allUsers.size());

        } catch (Exception e) {
            showAlert("Error", "Failed to load users: " + e.getMessage());
            System.err.println("Load users error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void displayUsers(List<UserRow> users) {
        ObservableList<UserRow> observableList = FXCollections.observableArrayList(users);
        usersTable.setItems(observableList);
        recordCountLabel.setText("Total Records: " + users.size());
    }

    @FXML
    public void onSearch() {
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

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        roleFilterCombo.setValue("ALL");
        displayUsers(allUsers);
    }

    @FXML
    public void onAddUser() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Add New User");
            dialog.setHeaderText("Create a new user account");

            // Create form fields
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

            // Create form layout
            VBox formBox = new VBox(10);
            formBox.setPadding(new Insets(10));
            formBox.getChildren().addAll(
                    new Label("Full Name:"),
                    fullNameField,
                    new Label("Email:"),
                    emailField,
                    new Label("Username:"),
                    usernameField,
                    new Label("Password:"),
                    passwordField,
                    new Label("Phone:"),
                    phoneField,
                    new Label("Role:"),
                    roleCombo
            );

            dialog.getDialogPane().setContent(formBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                // Validate inputs
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

                // Create user based on role
                if (role.equals("ATTENDEE")) {
                    Attendee attendee = new Attendee(fullName, LocalDate.now(), email, phone, username, password);
                    attendeeRepo.save(attendee);
                } else if (role.equals("PRESENTER")) {
                    Presenter presenter = new Presenter(fullName, LocalDate.now(), email, phone, username, password);
                    presenterRepo.save(presenter);
                }

                showAlert("Success", "User '" + username + "' created successfully!");
                loadAllUsers(); // Refresh table

            }
        } catch (Exception e) {
            showAlert("Error", "Failed to add user: " + e.getMessage());
            System.err.println("Add user error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onEditUser() {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to edit");
            return;
        }
        System.out.println("Edit User clicked: " + selected.username);
        // TODO: Implement edit user dialog
        showAlert("Info", "Edit User feature coming soon!");
    }

    @FXML
    public void onDeleteUser() {
        UserRow selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a user to delete");
            return;
        }

        // Show confirmation dialog
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
            try {
                // Delete user based on role
                java.util.UUID userId = java.util.UUID.fromString(selected.id);

                if (selected.role.equals("ATTENDEE")) {
                    if (attendeeRepo != null) {
                        attendeeRepo.delete(userId);
                        System.out.println(" Deleted attendee: " + selected.username);
                    }
                } else if (selected.role.equals("PRESENTER")) {
                    if (presenterRepo != null) {
                        presenterRepo.delete(userId);
                        System.out.println(" Deleted presenter: " + selected.username);
                    }
                } else {
                    // Admin users - would need a separate repository or direct SQL
                    showAlert("Info", "Cannot delete admin users from this interface.\nPlease use database management tools.");
                    return;
                }

                showAlert("Success", "User '" + selected.username + "' has been deleted successfully!");
                loadAllUsers(); // Refresh table to remove deleted user

            } catch (Exception e) {
                showAlert("Error", "Failed to delete user: " + e.getMessage());
                System.err.println("Delete user error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    @FXML
    public void onExport() {
        System.out.println("Export clicked");
        // TODO: Implement export to CSV/Excel
        showAlert("Info", "Export feature coming soon!");
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for displaying user data in table
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

