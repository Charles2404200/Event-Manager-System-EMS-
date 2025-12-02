package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import org.ems.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;
import org.ems.ui.stage.SceneManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <your group number>
 */
public class ViewEventsHomeController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private FlowPane eventsFlowPane;
    @FXML private Label resultCountLabel;

    private EventRepository eventRepo;
    private List<Event> allEvents;

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            eventRepo = context.eventRepo;

            // Setup combo boxes
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            // Load events
            loadAllEvents();
            displayEvents(allEvents);

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("Initialize error: " + e.getMessage());
        }
    }

    private void loadAllEvents() {
        try {
            allEvents = new ArrayList<>();

            if (eventRepo != null) {
                List<Event> events = eventRepo.findAll();
                allEvents.addAll(events);
                System.out.println(" Loaded " + events.size() + " events");
            }
        } catch (Exception e) {
            System.err.println("⚠ Error loading events: " + e.getMessage());
        }
    }

    private void displayEvents(List<Event> events) {
        eventsFlowPane.getChildren().clear();

        for (Event event : events) {
            VBox eventCard = createEventCard(event);
            eventsFlowPane.getChildren().add(eventCard);
        }

        resultCountLabel.setText("Showing " + events.size() + " events");
    }

    private VBox createEventCard(Event event) {
        VBox card = new VBox();
        card.setPrefWidth(280);
        card.setPrefHeight(380);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #f0f0f0;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 8, 0, 0, 3);"
        );

        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e8e8e8;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-padding: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 12, 0, 0, 5);"
            );
            card.setScaleX(1.02);
            card.setScaleY(1.02);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #f0f0f0;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 12;" +
                "-fx-padding: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.08), 8, 0, 0, 3);"
            );
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Event Image Container with gradient
        VBox imageContainer = new VBox();
        imageContainer.setPrefHeight(160);
        String gradient = getGradientByType(event.getType());
        imageContainer.setStyle("-fx-background: " + gradient + "; -fx-padding: 0; -fx-border-radius: 12 12 0 0;");
        imageContainer.setAlignment(Pos.CENTER);

        // Image label with emoji icon
        Label imageLabel = new Label(getIconByType(event.getType()));
        imageLabel.setStyle("-fx-font-size: 56; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 3, 0, 0, 1);");
        imageLabel.setAlignment(Pos.CENTER);
        imageContainer.getChildren().add(imageLabel);

        // Content container
        VBox contentBox = new VBox();
        contentBox.setStyle("-fx-padding: 16; -fx-spacing: 10;");
        contentBox.setPrefHeight(220);

        // Event Name
        Label nameLabel = new Label(event.getName());
        nameLabel.setWrapText(true);
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1a1a1a; -fx-line-spacing: 2;");

        // Event Type Badge with better styling
        Label typeLabel = new Label(event.getType().name());
        typeLabel.setStyle(
            "-fx-padding: 5 10;" +
            "-fx-background-color: " + getColorByType(event.getType()) + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        );
        typeLabel.setPrefWidth(120);

        // Location with icon
        Label locationLabel = new Label("📍 " + event.getLocation());
        locationLabel.setWrapText(true);
        locationLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555555; -fx-line-spacing: 1;");

        // Dates with better formatting
        String dateRange = event.getStartDate() + " → " + event.getEndDate();
        Label dateLabel = new Label("📅 " + dateRange);
        dateLabel.setWrapText(true);
        dateLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #888888;");

        // Status Badge with dynamic color
        Label statusLabel = new Label(event.getStatus().name());
        String statusColor = getStatusColor(event.getStatus().name());
        statusLabel.setStyle(
            "-fx-padding: 4 8;" +
            "-fx-background-color: " + statusColor + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 9;" +
            "-fx-font-weight: bold;" +
            "-fx-border-radius: 3;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        );
        statusLabel.setPrefWidth(100);

        // Add content to card
        contentBox.getChildren().addAll(
                nameLabel,
                typeLabel,
                locationLabel,
                dateLabel,
                statusLabel
        );

        // Button container at bottom with better spacing
        HBox buttonBox = new HBox();
        buttonBox.setStyle("-fx-spacing: 8; -fx-padding: 12 16 16 16;");
        buttonBox.setPrefHeight(50);

        Button viewBtn = new Button("👁 View");
        viewBtn.setPrefWidth(125);
        viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        );
        viewBtn.setOnMouseEntered(e -> viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #2980b9;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 3, 0, 0, 2);"
        ));
        viewBtn.setOnMouseExited(e -> viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        ));
        viewBtn.setOnAction(e -> onViewEvent(event));

        Button moreBtn = new Button("➕ More");
        moreBtn.setPrefWidth(125);
        moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #2ecc71;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        );
        moreBtn.setOnMouseEntered(e -> moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.15), 3, 0, 0, 2);"
        ));
        moreBtn.setOnMouseExited(e -> moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #2ecc71;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 4;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 2, 0, 0, 1);"
        ));
        moreBtn.setOnAction(e -> onLearnMore(event));

        buttonBox.getChildren().addAll(viewBtn, moreBtn);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        card.getChildren().addAll(imageContainer, contentBox, buttonBox);

        return card;
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();

            List<Event> filtered = allEvents.stream()
                    .filter(e -> typeFilter.equals("ALL") || e.getType().name().equals(typeFilter))
                    .filter(e -> statusFilter.equals("ALL") || e.getStatus().name().equals(statusFilter))
                    .filter(e -> startDate == null || !e.getStartDate().isBefore(startDate))
                    .filter(e -> endDate == null || !e.getEndDate().isAfter(endDate))
                    .filter(e -> searchTerm.isEmpty() ||
                           e.getName().toLowerCase().contains(searchTerm) ||
                           e.getLocation().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());

            displayEvents(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);
        displayEvents(allEvents);
    }

    private void onViewEvent(Event event) {
        showAlert("Event Details",
                "Event: " + event.getName() + "\n\n" +
                "Type: " + event.getType() + "\n" +
                "Location: " + event.getLocation() + "\n" +
                "Start: " + event.getStartDate() + "\n" +
                "End: " + event.getEndDate() + "\n" +
                "Status: " + event.getStatus() + "\n\n" +
                "To register, please login first.");
    }

    private void onLearnMore(Event event) {
        showAlert("Learn More",
                "Explore all sessions and details for " + event.getName() + "\n\n" +
                "Please login to register and view full details.");
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
    }

    private String getIconByType(org.ems.domain.model.enums.EventType type) {
        return switch (type.name()) {
            case "CONFERENCE" -> "🎤";
            case "WORKSHOP" -> "🛠️";
            case "CONCERT" -> "🎵";
            case "EXHIBITION" -> "🖼️";
            case "SEMINAR" -> "📚";
            default -> "📅";
        };
    }

    private String getColorByType(org.ems.domain.model.enums.EventType type) {
        return switch (type.name()) {
            case "CONFERENCE" -> "#3498db";
            case "WORKSHOP" -> "#e74c3c";
            case "CONCERT" -> "#9b59b6";
            case "EXHIBITION" -> "#f39c12";
            case "SEMINAR" -> "#16a085";
            default -> "#95a5a6";
        };
    }

    private String getGradientByType(org.ems.domain.model.enums.EventType type) {
        return switch (type.name()) {
            case "CONFERENCE" -> "linear-gradient(135deg, #3498db 0%, #2980b9 100%)";
            case "WORKSHOP" -> "linear-gradient(135deg, #e74c3c 0%, #c0392b 100%)";
            case "CONCERT" -> "linear-gradient(135deg, #9b59b6 0%, #8e44ad 100%)";
            case "EXHIBITION" -> "linear-gradient(135deg, #f39c12 0%, #d68910 100%)";
            case "SEMINAR" -> "linear-gradient(135deg, #16a085 0%, #138d75 100%)";
            default -> "linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%)";
        };
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "SCHEDULED" -> "#3498db";
            case "ONGOING" -> "#2ecc71";
            case "COMPLETED" -> "#95a5a6";
            case "CANCELLED" -> "#e74c3c";
            default -> "#95a5a6";
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

