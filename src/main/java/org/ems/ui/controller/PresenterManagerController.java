package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import org.ems.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.model.enums.PresenterType;
import org.ems.domain.repository.PresenterRepository;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for managing presenters in the admin panel
 * @author <your group number>
 */
public class PresenterManagerController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<PresenterRow> presentersTable;
    @FXML private Label recordCountLabel;

    private PresenterRepository presenterRepo;
    private List<PresenterRow> allPresenters;

    @FXML
    public void initialize() {
        try {
            // Get repository from context
            AppContext context = AppContext.get();
            presenterRepo = context.presenterRepo;

            // Setup type filter combo
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "KEYNOTE_SPEAKER", "PANELIST", "MODERATOR", "GUEST"
            ));
            typeFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all presenters
            loadAllPresenters();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("Initialize error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void setupTableColumns() {
        ObservableList<TableColumn<PresenterRow, ?>> columns = presentersTable.getColumns();

        if (columns.size() >= 7) {
            ((TableColumn<PresenterRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().id));
            ((TableColumn<PresenterRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().username));
            ((TableColumn<PresenterRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().fullName));
            ((TableColumn<PresenterRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().presenterType));
            ((TableColumn<PresenterRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().email));
            ((TableColumn<PresenterRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().phone));
            ((TableColumn<PresenterRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(String.valueOf(cellData.getValue().sessionCount)));
        }
    }

    private void loadAllPresenters() {
        try {
            allPresenters = new ArrayList<>();

            // Load presenters from repository
            if (presenterRepo != null) {
                try {
                    List<Presenter> presenters = presenterRepo.findAll();

                    for (Presenter presenter : presenters) {
                        int sessionCount = presenter.getSessionIds() != null ? presenter.getSessionIds().size() : 0;

                        allPresenters.add(new PresenterRow(
                                presenter.getId().toString(),
                                presenter.getUsername(),
                                presenter.getFullName(),
                                presenter.getPresenterType().name(),
                                presenter.getEmail(),
                                presenter.getPhone(),
                                sessionCount,
                                presenter  // Keep reference to original object
                        ));
                    }
                    System.out.println("✓ Loaded " + presenters.size() + " presenters");
                } catch (Exception e) {
                    System.err.println("Error loading presenters: " + e.getMessage());
                }
            }

            displayPresenters(allPresenters);
            System.out.println("✓ Total presenters loaded: " + allPresenters.size());

        } catch (Exception e) {
            showAlert("Error", "Failed to load presenters: " + e.getMessage());
            System.err.println("Load presenters error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private void displayPresenters(List<PresenterRow> presenters) {
        ObservableList<PresenterRow> observableList = FXCollections.observableArrayList(presenters);
        presentersTable.setItems(observableList);
        recordCountLabel.setText("Total Records: " + presenters.size());
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();

            List<PresenterRow> filtered = new ArrayList<>();

            for (PresenterRow presenter : allPresenters) {
                // Apply type filter
                if (!typeFilter.equals("ALL") && !presenter.presenterType.equals(typeFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    presenter.username.toLowerCase().contains(searchTerm) ||
                    presenter.fullName.toLowerCase().contains(searchTerm) ||
                    presenter.email.toLowerCase().contains(searchTerm)) {
                    filtered.add(presenter);
                }
            }

            displayPresenters(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        displayPresenters(allPresenters);
    }

    @FXML
    public void onCreatePresenter() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Create New Presenter");
            dialog.setHeaderText("Create a new presenter");
            dialog.setResizable(true);

            // Create form fields
            TextField usernameField = new TextField();
            usernameField.setPromptText("Username");
            usernameField.setPrefWidth(300);

            TextField fullNameField = new TextField();
            fullNameField.setPromptText("Full Name");
            fullNameField.setPrefWidth(300);

            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            emailField.setPrefWidth(300);

            TextField phoneField = new TextField();
            phoneField.setPromptText("Phone");
            phoneField.setPrefWidth(300);

            DatePicker dobPicker = new DatePicker();
            dobPicker.setPrefWidth(300);

            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.setItems(FXCollections.observableArrayList(
                    "KEYNOTE_SPEAKER", "PANELIST", "MODERATOR", "GUEST"
            ));
            typeCombo.setValue("KEYNOTE_SPEAKER");
            typeCombo.setPrefWidth(300);

            TextArea bioArea = new TextArea();
            bioArea.setPromptText("Bio / Description");
            bioArea.setPrefWidth(300);
            bioArea.setPrefHeight(100);
            bioArea.setWrapText(true);

            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Password");
            passwordField.setPrefWidth(300);

            // Create form layout
            VBox formBox = new VBox(10);
            formBox.setPadding(new Insets(10));
            formBox.getChildren().addAll(
                    new Label("Username:"),
                    usernameField,
                    new Label("Full Name:"),
                    fullNameField,
                    new Label("Email:"),
                    emailField,
                    new Label("Phone:"),
                    phoneField,
                    new Label("Date of Birth:"),
                    dobPicker,
                    new Label("Presenter Type:"),
                    typeCombo,
                    new Label("Bio:"),
                    bioArea,
                    new Label("Password:"),
                    passwordField
            );

            ScrollPane scrollPane = new ScrollPane(formBox);
            scrollPane.setFitToWidth(true);
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                // Validate inputs
                String username = usernameField.getText();
                String fullName = fullNameField.getText();
                String email = emailField.getText();
                String phone = phoneField.getText();
                LocalDate dob = dobPicker.getValue();
                String type = typeCombo.getValue();
                String bio = bioArea.getText();
                String password = passwordField.getText();

                if (username.isBlank() || fullName.isBlank() || email.isBlank() || password.isBlank()) {
                    showAlert("Validation Error", "Please fill in all required fields");
                    return;
                }

                if (!email.contains("@")) {
                    showAlert("Validation Error", "Invalid email format");
                    return;
                }

                // Create presenter object
                Presenter presenter = new Presenter(
                        fullName,
                        dob != null ? dob : LocalDate.now(),
                        email,
                        phone,
                        username,
                        password  // In production, this should be hashed
                );

                try {
                    presenter.setPresenterType(PresenterType.valueOf(type));
                } catch (IllegalArgumentException e) {
                    presenter.setPresenterType(PresenterType.KEYNOTE_SPEAKER);
                }

                presenter.setBio(bio);

                // Save to database
                if (presenterRepo != null) {
                    presenterRepo.save(presenter);
                    showAlert("Success", "Presenter created successfully!");
                    loadAllPresenters();
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to create presenter: " + e.getMessage());
            System.err.println("Create presenter error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onEditPresenter() {
        try {
            PresenterRow selectedRow = presentersTable.getSelectionModel().getSelectedItem();

            if (selectedRow == null) {
                showAlert("Warning", "Please select a presenter to edit");
                return;
            }

            Presenter presenter = selectedRow.presenter;

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Edit Presenter");
            dialog.setHeaderText("Edit presenter information");
            dialog.setResizable(true);

            // Create form fields with current values
            TextField fullNameField = new TextField(presenter.getFullName());
            fullNameField.setPrefWidth(300);

            TextField emailField = new TextField(presenter.getEmail());
            emailField.setPrefWidth(300);

            TextField phoneField = new TextField(presenter.getPhone() != null ? presenter.getPhone() : "");
            phoneField.setPrefWidth(300);

            DatePicker dobPicker = new DatePicker(presenter.getDateOfBirth());
            dobPicker.setPrefWidth(300);

            ComboBox<String> typeCombo = new ComboBox<>();
            typeCombo.setItems(FXCollections.observableArrayList(
                    "KEYNOTE_SPEAKER", "PANELIST", "MODERATOR", "GUEST"
            ));
            typeCombo.setValue(presenter.getPresenterType().name());
            typeCombo.setPrefWidth(300);

            TextArea bioArea = new TextArea(presenter.getBio() != null ? presenter.getBio() : "");
            bioArea.setPrefWidth(300);
            bioArea.setPrefHeight(100);
            bioArea.setWrapText(true);

            // Create form layout
            VBox formBox = new VBox(10);
            formBox.setPadding(new Insets(10));
            formBox.getChildren().addAll(
                    new Label("Full Name:"),
                    fullNameField,
                    new Label("Email:"),
                    emailField,
                    new Label("Phone:"),
                    phoneField,
                    new Label("Date of Birth:"),
                    dobPicker,
                    new Label("Presenter Type:"),
                    typeCombo,
                    new Label("Bio:"),
                    bioArea
            );

            ScrollPane scrollPane = new ScrollPane(formBox);
            scrollPane.setFitToWidth(true);
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                // Validate inputs
                if (fullNameField.getText().isBlank() || emailField.getText().isBlank()) {
                    showAlert("Validation Error", "Please fill in all required fields");
                    return;
                }

                // Update presenter object
                presenter.setFullName(fullNameField.getText());
                presenter.setEmail(emailField.getText());
                presenter.setPhone(phoneField.getText());
                presenter.setDateOfBirth(dobPicker.getValue());

                try {
                    presenter.setPresenterType(PresenterType.valueOf(typeCombo.getValue()));
                } catch (IllegalArgumentException e) {
                    // Keep existing type
                }

                presenter.setBio(bioArea.getText());

                // Update in database
                if (presenterRepo != null) {
                    presenterRepo.save(presenter);
                    showAlert("Success", "Presenter updated successfully!");
                    loadAllPresenters();
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to edit presenter: " + e.getMessage());
            System.err.println("Edit presenter error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onDeletePresenter() {
        try {
            PresenterRow selectedRow = presentersTable.getSelectionModel().getSelectedItem();

            if (selectedRow == null) {
                showAlert("Warning", "Please select a presenter to delete");
                return;
            }

            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Confirm Delete");
            confirmDialog.setHeaderText("Delete Presenter");
            confirmDialog.setContentText("Are you sure you want to delete this presenter?\n" +
                    "This action cannot be undone.");

            if (confirmDialog.showAndWait().isPresent() && confirmDialog.showAndWait().get() == ButtonType.OK) {
                Presenter presenter = selectedRow.presenter;

                if (presenterRepo != null) {
                    presenterRepo.delete(presenter.getId());
                    showAlert("Success", "Presenter deleted successfully!");
                    loadAllPresenters();
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to delete presenter: " + e.getMessage());
            System.err.println("Delete presenter error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @FXML
    public void onViewDetails() {
        try {
            PresenterRow selectedRow = presentersTable.getSelectionModel().getSelectedItem();

            if (selectedRow == null) {
                showAlert("Warning", "Please select a presenter to view details");
                return;
            }

            Presenter presenter = selectedRow.presenter;

            Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
            detailsDialog.setTitle("Presenter Details");
            detailsDialog.setHeaderText(presenter.getFullName());

            String details = String.format(
                    "ID: %s\n" +
                    "Username: %s\n" +
                    "Full Name: %s\n" +
                    "Email: %s\n" +
                    "Phone: %s\n" +
                    "Date of Birth: %s\n" +
                    "Presenter Type: %s\n" +
                    "Bio: %s\n" +
                    "Sessions Assigned: %d\n" +
                    "Events: %d",
                    presenter.getId(),
                    presenter.getUsername(),
                    presenter.getFullName(),
                    presenter.getEmail(),
                    presenter.getPhone() != null ? presenter.getPhone() : "N/A",
                    presenter.getDateOfBirth(),
                    presenter.getPresenterType().name(),
                    presenter.getBio() != null ? presenter.getBio() : "N/A",
                    presenter.getSessionIds() != null ? presenter.getSessionIds().size() : 0,
                    presenter.getEventIds() != null ? presenter.getEventIds().size() : 0
            );

            detailsDialog.setContentText(details);
            detailsDialog.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to view details: " + e.getMessage());
        }
    }

    @FXML
    public void onAssignSessions() {
        try {
            PresenterRow selectedRow = presentersTable.getSelectionModel().getSelectedItem();

            if (selectedRow == null) {
                showAlert("Warning", "Please select a presenter");
                return;
            }

            Presenter presenter = selectedRow.presenter;
            AppContext ctx = AppContext.get();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Assign Sessions to Presenter");
            dialog.setHeaderText("Select sessions to assign to " + presenter.getFullName());
            dialog.setResizable(true);

            VBox mainBox = new VBox(10);
            mainBox.setPadding(new Insets(10));

            // Get all sessions
            List<Session> allSessions = new ArrayList<>();
            if (ctx.sessionRepo != null) {
                try {
                    allSessions = ctx.sessionRepo.findAll();
                } catch (Exception e) {
                    System.err.println("Error loading sessions: " + e.getMessage());
                }
            }

            // Create checkboxes for each session
            List<CheckBox> sessionCheckboxes = new ArrayList<>();
            Set<UUID> presentersCurrentSessions = new HashSet<>(
                    presenter.getSessionIds() != null ? presenter.getSessionIds() : new ArrayList<>()
            );

            Label sessionsLabel = new Label("Available Sessions:");
            mainBox.getChildren().add(sessionsLabel);

            VBox sessionsBox = new VBox(5);
            for (Session session : allSessions) {
                CheckBox checkbox = new CheckBox(session.getTitle());
                checkbox.setSelected(presentersCurrentSessions.contains(session.getId()));
                sessionCheckboxes.add(checkbox);
                sessionsBox.getChildren().add(checkbox);
            }

            ScrollPane scrollPane = new ScrollPane(sessionsBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(300);
            mainBox.getChildren().add(scrollPane);

            dialog.getDialogPane().setContent(mainBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            if (dialog.showAndWait().isPresent() && dialog.showAndWait().get() == ButtonType.OK) {
                // Update presenter's session assignments
                List<UUID> selectedSessions = new ArrayList<>();
                for (int i = 0; i < sessionCheckboxes.size(); i++) {
                    if (sessionCheckboxes.get(i).isSelected()) {
                        selectedSessions.add(allSessions.get(i).getId());
                    }
                }

                presenter.getSessionIds().clear();
                presenter.getSessionIds().addAll(selectedSessions);

                if (presenterRepo != null) {
                    presenterRepo.save(presenter);
                    showAlert("Success", "Sessions assigned successfully!");
                    loadAllPresenters();
                }
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to assign sessions: " + e.getMessage());
            System.err.println("Assign sessions error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
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

    // Inner class to hold presenter data for table display
    private static class PresenterRow {
        String id;
        String username;
        String fullName;
        String presenterType;
        String email;
        String phone;
        int sessionCount;
        Presenter presenter;  // Reference to the actual object

        PresenterRow(String id, String username, String fullName, String presenterType,
                    String email, String phone, int sessionCount, Presenter presenter) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.presenterType = presenterType;
            this.email = email;
            this.phone = phone;
            this.sessionCount = sessionCount;
            this.presenter = presenter;
        }
    }
}

