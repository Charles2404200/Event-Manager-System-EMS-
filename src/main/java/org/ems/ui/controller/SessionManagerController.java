package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.ems.application.service.EventService;
import org.ems.application.service.IdentityService;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * SessionManagerController - Manages session CRUD operations
 * @author EMS Team
 */
public class SessionManagerController {

    @FXML private TableView<Session> sessionTable;
    @FXML private TableColumn<Session, String> colId;
    @FXML private TableColumn<Session, String> colTitle;
    @FXML private TableColumn<Session, String> colEvent;
    @FXML private TableColumn<Session, String> colStart;
    @FXML private TableColumn<Session, String> colVenue;
    @FXML private TableColumn<Session, Integer> colCapacity;

    @FXML private Label detailIdLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailDescLabel;
    @FXML private Label detailStartLabel;
    @FXML private Label detailEndLabel;
    @FXML private Label detailVenueLabel;
    @FXML private Label detailCapacityLabel;
    @FXML private ListView<String> presenterListView;

    private final EventService eventService = AppContext.get().eventService;
    private final IdentityService identityService = AppContext.get().identityService;
    private Session selectedSession;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupTableColumns();
        loadSessions();
        sessionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedSession = newVal;
                displaySessionDetails(newVal);
            }
        });
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getId().toString().substring(0, 8) + "..."
                ));
        colTitle.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTitle()
                ));
        colEvent.setCellValueFactory(data -> {
            UUID eventId = data.getValue().getEventId();
            Event event = eventService.getEvent(eventId);
            return new javafx.beans.property.SimpleStringProperty(
                    event != null ? event.getName() : "N/A"
            );
        });
        colStart.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStart().format(formatter)
                ));
        colVenue.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getVenue()
                ));
        colCapacity.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getCapacity()
                ).asObject());
    }

    private void loadSessions() {
        try {
            List<Session> sessions = eventService.getSessions();
            sessionTable.setItems(FXCollections.observableList(sessions));
        } catch (Exception e) {
            showError("Error loading sessions", e.getMessage());
        }
    }

    private void displaySessionDetails(Session session) {
        try {
            detailIdLabel.setText(session.getId().toString());
            detailTitleLabel.setText(session.getTitle());
            detailDescLabel.setText(session.getDescription() != null ? session.getDescription() : "No description");
            detailStartLabel.setText(session.getStart().format(formatter));
            detailEndLabel.setText(session.getEnd().format(formatter));
            detailVenueLabel.setText(session.getVenue());
            detailCapacityLabel.setText(String.valueOf(session.getCapacity()));

            // Load presenters
            List<String> presenterNames = session.getPresenterIds().stream()
                    .map(pid -> {
                        Presenter p = (Presenter) identityService.getUserById(pid);
                        return p != null ? p.getFullName() : "Unknown";
                    })
                    .toList();
            presenterListView.setItems(FXCollections.observableList(presenterNames));
        } catch (Exception e) {
            showError("Error loading details", e.getMessage());
        }
    }

    @FXML
    public void onRefresh() {
        loadSessions();
        selectedSession = null;
        clearDetails();
    }

    private void clearDetails() {
        detailIdLabel.setText("-");
        detailTitleLabel.setText("-");
        detailDescLabel.setText("-");
        detailStartLabel.setText("-");
        detailEndLabel.setText("-");
        detailVenueLabel.setText("-");
        detailCapacityLabel.setText("-");
        presenterListView.setItems(FXCollections.observableList(List.of()));
    }

    @FXML
    public void onAddSession() {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Add New Session");
        dialog.setHeaderText("Create a new session");

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField();
        titleField.setPrefWidth(300);

        Label descLabel = new Label("Description:");
        TextArea descArea = new TextArea();
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label venueLabel = new Label("Venue:");
        TextField venueField = new TextField();
        venueField.setPrefWidth(300);

        Label capacityLabel = new Label("Capacity:");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, 50);

        Label startLabel = new Label("Start (dd/MM/yyyy HH:mm):");
        TextField startField = new TextField();
        startField.setPromptText("DD/MM/YYYY HH:MM");

        Label endLabel = new Label("End (dd/MM/yyyy HH:mm):");
        TextField endField = new TextField();
        endField.setPromptText("DD/MM/YYYY HH:MM");

        Label eventLabel = new Label("Event:");
        ComboBox<Event> eventBox = new ComboBox<>();
        eventBox.getItems().addAll(eventService.getEvents());
        eventBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });
        eventBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(400);

        grid.addRow(0, titleLabel, titleField);
        grid.addRow(1, descLabel, descArea);
        grid.addRow(2, venueLabel, venueField);
        grid.addRow(3, capacityLabel, capacitySpinner);
        grid.addRow(4, startLabel, startField);
        grid.addRow(5, endLabel, endField);
        grid.addRow(6, eventLabel, eventBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (titleField.getText().isEmpty() || venueField.getText().isEmpty()) {
                        showWarning("Validation Error", "Title and Venue cannot be empty!");
                        return null;
                    }

                    Session s = new Session();
                    s.setId(UUID.randomUUID());
                    s.setTitle(titleField.getText());
                    s.setDescription(descArea.getText());
                    s.setVenue(venueField.getText());
                    s.setCapacity(capacitySpinner.getValue());

                    LocalDateTime start = LocalDateTime.parse(startField.getText(), formatter);
                    LocalDateTime end = LocalDateTime.parse(endField.getText(), formatter);

                    if (end.isBefore(start)) {
                        showWarning("Validation Error", "End time must be after start time!");
                        return null;
                    }

                    s.setStart(start);
                    s.setEnd(end);

                    Event ev = eventBox.getValue();
                    if (ev != null) {
                        s.setEventId(ev.getId());
                    } else {
                        showWarning("Validation Error", "Please select an event!");
                        return null;
                    }

                    return s;
                } catch (Exception ex) {
                    showError("Parse Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            try {
                eventService.createSession(s);
                showInfo("Success", "Session created successfully!");
                loadSessions();
            } catch (Exception e) {
                showError("Creation Error", e.getMessage());
            }
        });
    }

    @FXML
    public void onUpdateSession() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session to update!");
            return;
        }

        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Update Session");
        dialog.setHeaderText("Edit session: " + selectedSession.getTitle());

        Label titleLabel = new Label("Title:");
        TextField titleField = new TextField(selectedSession.getTitle());
        titleField.setPrefWidth(300);

        Label descLabel = new Label("Description:");
        TextArea descArea = new TextArea(selectedSession.getDescription() != null ? selectedSession.getDescription() : "");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);

        Label venueLabel = new Label("Venue:");
        TextField venueField = new TextField(selectedSession.getVenue());
        venueField.setPrefWidth(300);

        Label capacityLabel = new Label("Capacity:");
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, selectedSession.getCapacity());

        Label startLabel = new Label("Start (dd/MM/yyyy HH:mm):");
        TextField startField = new TextField(selectedSession.getStart().format(formatter));

        Label endLabel = new Label("End (dd/MM/yyyy HH:mm):");
        TextField endField = new TextField(selectedSession.getEnd().format(formatter));

        Label eventLabel = new Label("Event:");
        ComboBox<Event> eventBox = new ComboBox<>();
        eventBox.getItems().addAll(eventService.getEvents());
        eventBox.setValue(eventService.getEvent(selectedSession.getEventId()));
        eventBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });
        eventBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(400);

        grid.addRow(0, titleLabel, titleField);
        grid.addRow(1, descLabel, descArea);
        grid.addRow(2, venueLabel, venueField);
        grid.addRow(3, capacityLabel, capacitySpinner);
        grid.addRow(4, startLabel, startField);
        grid.addRow(5, endLabel, endField);
        grid.addRow(6, eventLabel, eventBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (titleField.getText().isEmpty() || venueField.getText().isEmpty()) {
                        showWarning("Validation Error", "Title and Venue cannot be empty!");
                        return null;
                    }

                    selectedSession.setTitle(titleField.getText());
                    selectedSession.setDescription(descArea.getText());
                    selectedSession.setVenue(venueField.getText());
                    selectedSession.setCapacity(capacitySpinner.getValue());

                    LocalDateTime start = LocalDateTime.parse(startField.getText(), formatter);
                    LocalDateTime end = LocalDateTime.parse(endField.getText(), formatter);

                    if (end.isBefore(start)) {
                        showWarning("Validation Error", "End time must be after start time!");
                        return null;
                    }

                    selectedSession.setStart(start);
                    selectedSession.setEnd(end);

                    Event ev = eventBox.getValue();
                    if (ev != null) {
                        selectedSession.setEventId(ev.getId());
                    }

                    return selectedSession;
                } catch (Exception ex) {
                    showError("Parse Error", ex.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(s -> {
            try {
                eventService.updateSession(s);
                showInfo("Success", "Session updated successfully!");
                loadSessions();
            } catch (Exception e) {
                showError("Update Error", e.getMessage());
            }
        });
    }

    @FXML
    public void onDeleteSession() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session to delete!");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Session");
        alert.setContentText("Are you sure you want to delete session: " + selectedSession.getTitle() + "?");

        if (alert.showAndWait().isPresent() && alert.showAndWait().get() == ButtonType.OK) {
            try {
                eventService.deleteSession(selectedSession.getId());
                showInfo("Success", "Session deleted successfully!");
                loadSessions();
                clearDetails();
                selectedSession = null;
            } catch (Exception e) {
                showError("Delete Error", e.getMessage());
            }
        }
    }

    @FXML
    public void onAddPresenter() {
        if (selectedSession == null) {
            showWarning("No Selection", "Please select a session first!");
            return;
        }

        Dialog<Presenter> dialog = new Dialog<>();
        dialog.setTitle("Add Presenter to Session");
        dialog.setHeaderText("Assign presenter to: " + selectedSession.getTitle());

        Label presenterLabel = new Label("Presenter:");
        ComboBox<Presenter> presenterBox = new ComboBox<>();
        List<Presenter> presenters = identityService.getAllPresenters();
        presenterBox.getItems().addAll(presenters);
        presenterBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });
        presenterBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, presenterLabel, presenterBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? presenterBox.getValue() : null);

        dialog.showAndWait().ifPresent(presenter -> {
            try {
                if (eventService.addPresenterToSession(presenter.getId(), selectedSession.getId())) {
                    showInfo("Success", "Presenter added successfully!");
                    displaySessionDetails(selectedSession);
                } else {
                    showWarning("Conflict", "Presenter has a conflicting schedule!");
                }
            } catch (Exception e) {
                showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    public void backToDashboard() {
        SceneManager.switchTo("admin_dashboard.fxml", "EMS - Admin Dashboard");
    }

    // Helper methods for dialogs
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
