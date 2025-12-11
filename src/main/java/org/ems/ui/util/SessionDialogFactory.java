package org.ems.ui.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * SessionDialogFactory - Factory for creating dialogs
 * Single Responsibility: Create and configure dialogs for session management
 *
 * @author EMS Team
 */
public class SessionDialogFactory {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Create Add Session Dialog
     */
    public static Optional<Session> showAddSessionDialog(List<Event> events) {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Add New Session");
        dialog.setHeaderText("Create a new session");

        // Setup form fields
        TextField titleField = createTextField("Session Title", 300);
        TextArea descArea = createTextArea("Description", 3);
        TextField venueField = createTextField("Venue", 300);
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, 50);
        TextField startField = createTextField("Start (dd/MM/yyyy HH:mm)", 300);
        TextField endField = createTextField("End (dd/MM/yyyy HH:mm)", 300);
        ComboBox<Event> eventBox = createEventCombo(events, null);

        // Build grid
        GridPane grid = buildSessionGrid(
                titleField, descArea, venueField, capacitySpinner, startField, endField, eventBox
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return parseSessionInput(
                        titleField, descArea, venueField, capacitySpinner,
                        startField, endField, eventBox, null
                );
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Create Update Session Dialog
     */
    public static Optional<Session> showUpdateSessionDialog(Session session, List<Event> events) {
        Dialog<Session> dialog = new Dialog<>();
        dialog.setTitle("Update Session");
        dialog.setHeaderText("Edit session: " + session.getTitle());

        // Setup form fields with current values
        TextField titleField = createTextField("Session Title", 300);
        titleField.setText(session.getTitle());

        TextArea descArea = createTextArea("Description", 3);
        descArea.setText(session.getDescription() != null ? session.getDescription() : "");

        TextField venueField = createTextField("Venue", 300);
        venueField.setText(session.getVenue());

        Spinner<Integer> capacitySpinner = new Spinner<>(1, 1000, session.getCapacity());

        TextField startField = createTextField("Start (dd/MM/yyyy HH:mm)", 300);
        startField.setText(session.getStart().format(formatter));

        TextField endField = createTextField("End (dd/MM/yyyy HH:mm)", 300);
        endField.setText(session.getEnd().format(formatter));

        ComboBox<Event> eventBox = createEventCombo(events, session.getEventId());

        // Build grid
        GridPane grid = buildSessionGrid(
                titleField, descArea, venueField, capacitySpinner, startField, endField, eventBox
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                Session updated = parseSessionInput(
                        titleField, descArea, venueField, capacitySpinner,
                        startField, endField, eventBox, session.getId()
                );
                if (updated != null) {
                    updated.setId(session.getId());
                    // Preserve existing presenters
                    for (UUID presenterId : session.getPresenterIds()) {
                        updated.addPresenter(presenterId);
                    }
                }
                return updated;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    /**
     * Create Add Presenter to Session Dialog
     */
    public static Optional<Presenter> showAddPresenterDialog(String sessionTitle, List<Presenter> presenters) {
        Dialog<Presenter> dialog = new Dialog<>();
        dialog.setTitle("Add Presenter to Session");
        dialog.setHeaderText("Assign presenter to: " + sessionTitle);

        ComboBox<Presenter> presenterBox = createPresenterCombo(presenters);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.addRow(0, new Label("Presenter:"), presenterBox);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> btn == ButtonType.OK ? presenterBox.getValue() : null);

        return dialog.showAndWait();
    }

    /**
     * Helper: Create text field
     */
    private static TextField createTextField(String promptText, double width) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.setPrefWidth(width);
        return field;
    }

    /**
     * Helper: Create text area
     */
    private static TextArea createTextArea(String promptText, int rowCount) {
        TextArea area = new TextArea();
        area.setPromptText(promptText);
        area.setPrefRowCount(rowCount);
        area.setWrapText(true);
        return area;
    }

    /**
     * Helper: Create event combo box
     */
    private static ComboBox<Event> createEventCombo(List<Event> events, UUID currentEventId) {
        ComboBox<Event> box = new ComboBox<>();
        box.getItems().addAll(events);

        // Custom cell factory for display
        box.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Event e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? "" : e.getName());
            }
        });

        // Set current value if provided
        if (currentEventId != null) {
            for (Event e : events) {
                if (e.getId().equals(currentEventId)) {
                    box.setValue(e);
                    break;
                }
            }
        }

        return box;
    }

    /**
     * Helper: Create presenter combo box
     */
    private static ComboBox<Presenter> createPresenterCombo(List<Presenter> presenters) {
        ComboBox<Presenter> box = new ComboBox<>();
        box.getItems().addAll(presenters);

        box.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });

        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Presenter p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "" : p.getFullName());
            }
        });

        return box;
    }

    /**
     * Helper: Build session form grid
     */
    private static GridPane buildSessionGrid(TextField titleField, TextArea descArea, TextField venueField,
                                           Spinner<Integer> capacitySpinner, TextField startField,
                                           TextField endField, ComboBox<Event> eventBox) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setPrefWidth(400);

        grid.addRow(0, new Label("Title:"), titleField);
        grid.addRow(1, new Label("Description:"), descArea);
        grid.addRow(2, new Label("Venue:"), venueField);
        grid.addRow(3, new Label("Capacity:"), capacitySpinner);
        grid.addRow(4, new Label("Start (dd/MM/yyyy HH:mm):"), startField);
        grid.addRow(5, new Label("End (dd/MM/yyyy HH:mm):"), endField);
        grid.addRow(6, new Label("Event:"), eventBox);

        return grid;
    }

    /**
     * Helper: Parse and validate session input
     */
    private static Session parseSessionInput(TextField titleField, TextArea descArea, TextField venueField,
                                           Spinner<Integer> capacitySpinner, TextField startField,
                                           TextField endField, ComboBox<Event> eventBox, UUID sessionId) {
        // Validate required fields
        if (titleField.getText().isEmpty() || venueField.getText().isEmpty()) {
            showWarning("Validation Error", "Title and Venue cannot be empty!");
            return null;
        }

        try {
            // Parse date times
            LocalDateTime start = LocalDateTime.parse(startField.getText(), formatter);
            LocalDateTime end = LocalDateTime.parse(endField.getText(), formatter);

            // Validate date logic
            if (end.isBefore(start)) {
                showWarning("Validation Error", "End time must be after start time!");
                return null;
            }

            // Get selected event
            Event event = eventBox.getValue();
            if (event == null) {
                showWarning("Validation Error", "Please select an event!");
                return null;
            }

            // Create session
            Session session = new Session();
            session.setId(sessionId != null ? sessionId : UUID.randomUUID());
            session.setTitle(titleField.getText());
            session.setDescription(descArea.getText());
            session.setVenue(venueField.getText());
            session.setCapacity(capacitySpinner.getValue());
            session.setStart(start);
            session.setEnd(end);
            session.setEventId(event.getId());

            return session;
        } catch (Exception ex) {
            showError("Parse Error", ex.getMessage());
            return null;
        }
    }

    // Dialog helper methods
    private static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

