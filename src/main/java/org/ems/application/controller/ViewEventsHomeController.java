package org.ems.application.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.repository.EventRepository;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.ui.util.LoadingDialog;

import java.io.File;
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
    private LoadingDialog loadingDialog;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("🎬 [ViewEventsHome] initialize() starting...");

        try {
            long appContextStart = System.currentTimeMillis();
            AppContext context = AppContext.get();
            eventRepo = context.eventRepo;
            System.out.println("  ✓ AppContext loaded in " + (System.currentTimeMillis() - appContextStart) + "ms");

            // Setup combo boxes
            long comboStart = System.currentTimeMillis();
            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "CONFERENCE", "WORKSHOP", "CONCERT", "EXHIBITION", "SEMINAR"
            ));
            typeFilterCombo.setValue("ALL");

            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");
            System.out.println("  ✓ ComboBoxes setup in " + (System.currentTimeMillis() - comboStart) + "ms");

            // Add filter listeners
            typeFilterCombo.setOnAction(e -> applyFilters());
            statusFilterCombo.setOnAction(e -> applyFilters());

            // Load events on background thread
            loadAllEventsAsync();

            System.out.println("✓ initialize() completed in " + (System.currentTimeMillis() - initStart) + "ms");

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            System.err.println("✗ Initialize error: " + e.getMessage());
        }
    }

    /**
     * Load events asynchronously without blocking UI
     */
    private void loadAllEventsAsync() {
        long loadStart = System.currentTimeMillis();
        System.out.println("📋 [loadAllEventsAsync] Starting async event load...");

        // Get stage safely
        javafx.stage.Stage primaryStage = null;
        try {
            if (eventsFlowPane != null && eventsFlowPane.getScene() != null) {
                primaryStage = (javafx.stage.Stage) eventsFlowPane.getScene().getWindow();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not get stage for loading dialog");
        }

        if (primaryStage != null) {
            loadingDialog = new LoadingDialog(primaryStage, "Loading events...");
            loadingDialog.show();
        }

        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    long dbStart = System.currentTimeMillis();
                    List<Event> events = new ArrayList<>();
                    if (eventRepo != null) {
                        events = eventRepo.findAll();
                        System.out.println("  ✓ Database query in " + (System.currentTimeMillis() - dbStart) + "ms - Loaded " + events.size() + " events");
                    }
                    return events;
                },

                // Success callback
                events -> {
                    long callbackStart = System.currentTimeMillis();
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    allEvents = new ArrayList<>(events);
                    System.out.println("  ✓ Callback setup in " + (System.currentTimeMillis() - callbackStart) + "ms");

                    long filterStart = System.currentTimeMillis();
                    applyFilters();
                    System.out.println("✓ loadAllEventsAsync completed in " + (System.currentTimeMillis() - loadStart) + "ms");
                },

                // Error callback
                error -> {
                    if (loadingDialog != null) {
                        loadingDialog.close();
                    }
                    showAlert("Error", "Failed to load events: " + error.getMessage());
                    System.err.println("✗ Error loading events: " + error.getMessage());
                }
        );
    }

    /**
     * Apply filters and display events
     */
    private void applyFilters() {
        long filterStart = System.currentTimeMillis();
        System.out.println("🔍 [applyFilters] Applying filters...");

        if (allEvents == null) {
            System.out.println("⚠️ allEvents is null");
            return;
        }

        long streamStart = System.currentTimeMillis();
        List<Event> filtered = allEvents.stream()
                .filter(e -> {
                    String typeFilter = typeFilterCombo.getValue();
                    if (!"ALL".equals(typeFilter) && !e.getType().name().equals(typeFilter)) {
                        return false;
                    }

                    String statusFilter = statusFilterCombo.getValue();
                    if (!"ALL".equals(statusFilter) && !e.getStatus().name().equals(statusFilter)) {
                        return false;
                    }

                    String search = searchField.getText().toLowerCase();
                    if (!search.isEmpty() && !e.getName().toLowerCase().contains(search)) {
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());
        System.out.println("  ✓ Filter stream in " + (System.currentTimeMillis() - streamStart) + "ms - Filtered to " + filtered.size() + " events");

        long displayStart = System.currentTimeMillis();
        displayEvents(filtered);
        System.out.println("  ✓ Display in " + (System.currentTimeMillis() - displayStart) + "ms");
        System.out.println("✓ applyFilters completed in " + (System.currentTimeMillis() - filterStart) + "ms");
    }

    private void displayEvents(List<Event> events) {
        long displayStart = System.currentTimeMillis();
        System.out.println("📺 [displayEvents] Displaying " + events.size() + " events...");

        long clearStart = System.currentTimeMillis();
        eventsFlowPane.getChildren().clear();
        System.out.println("  ✓ Clear in " + (System.currentTimeMillis() - clearStart) + "ms");

        long renderStart = System.currentTimeMillis();
        for (Event event : events) {
            VBox eventCard = createEventCard(event);
            eventsFlowPane.getChildren().add(eventCard);
        }
        System.out.println("  ✓ Render " + events.size() + " cards in " + (System.currentTimeMillis() - renderStart) + "ms");

        resultCountLabel.setText("Showing " + events.size() + " events");
        System.out.println("✓ displayEvents completed in " + (System.currentTimeMillis() - displayStart) + "ms");
    }

    private VBox createEventCard(Event event) {
        long cardStart = System.currentTimeMillis();

        VBox card = new VBox();
        card.setPrefWidth(280);
        card.setPrefHeight(340);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e0e0e0;" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 10;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 6, 0, 0, 2);"
        );

        // Add hover effect with smooth transition
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #3498db;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-padding: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.3), 15, 0, 0, 8);"
            );
            card.setScaleX(1.03);
            card.setScaleY(1.03);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #e0e0e0;" +
                "-fx-border-width: 1;" +
                "-fx-border-radius: 10;" +
                "-fx-padding: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.1), 6, 0, 0, 2);"
            );
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });

        // Event Image Container with real image or gradient fallback
        VBox imageContainer = new VBox();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle("-fx-padding: 0; -fx-border-radius: 12 12 0 0; -fx-alignment: center;");
        imageContainer.setAlignment(Pos.CENTER);

        // Try to load real event image, fallback to gradient
        if (event.getImagePath() != null && !event.getImagePath().isEmpty()) {
            try {
                File imageFile = new File(event.getImagePath());
                if (imageFile.exists()) {
                    ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
                    imageView.setFitHeight(160);
                    imageView.setFitWidth(280);
                    imageView.setPreserveRatio(false);
                    imageContainer.getChildren().add(imageView);
                } else {
                    // File doesn't exist, use fallback
                    applyGradientFallback(imageContainer, event);
                }
            } catch (Exception e) {
                System.err.println("Error loading event image: " + e.getMessage());
                applyGradientFallback(imageContainer, event);
            }
        } else {
            // No image path, use fallback
            applyGradientFallback(imageContainer, event);
        }

        // Content container
        VBox contentBox = new VBox();
        contentBox.setStyle("-fx-padding: 16; -fx-spacing: 8;");
        contentBox.setPrefHeight(180);

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

        Button viewBtn = new Button("👁 View Details");
        viewBtn.setPrefWidth(130);
        viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.2), 3, 0, 0, 1);"
        );
        viewBtn.setOnMouseEntered(e -> viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #2980b9;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(41, 128, 185, 0.4), 5, 0, 0, 2);"
        ));
        viewBtn.setOnMouseExited(e -> viewBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #3498db;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(52, 152, 219, 0.2), 3, 0, 0, 1);"
        ));
        viewBtn.setOnAction(e -> onViewEvent(event));

        Button moreBtn = new Button("ℹ️ Learn More");
        moreBtn.setPrefWidth(130);
        moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(39, 174, 96, 0.2), 3, 0, 0, 1);"
        );
        moreBtn.setOnMouseEntered(e -> moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #229954;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(34, 153, 84, 0.4), 5, 0, 0, 2);"
        ));
        moreBtn.setOnMouseExited(e -> moreBtn.setStyle(
            "-fx-padding: 10 12;" +
            "-fx-font-size: 11;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-border-radius: 5;" +
            "-fx-effect: dropshadow(gaussian, rgba(39, 174, 96, 0.2), 3, 0, 0, 1);"
        ));
        moreBtn.setOnAction(e -> onLearnMore(event));

        buttonBox.getChildren().addAll(viewBtn, moreBtn);

        VBox.setVgrow(contentBox, Priority.ALWAYS);
        card.getChildren().addAll(imageContainer, contentBox, buttonBox);

        long cardTime = System.currentTimeMillis() - cardStart;
        if (cardTime > 50) {
            System.out.println("  ⚠️ Card creation took " + cardTime + "ms for: " + event.getName());
        }

        return card;
    }

    @FXML
    public void onSearch() {
        long searchStart = System.currentTimeMillis();
        System.out.println("🔎 [onSearch] Starting search...");

        try {
            String searchTerm = searchField.getText().toLowerCase();
            String typeFilter = typeFilterCombo.getValue();
            String statusFilter = statusFilterCombo.getValue();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            System.out.println("  ✓ Search params: term='" + searchTerm + "', type=" + typeFilter + ", status=" + statusFilter);

            long filterStart = System.currentTimeMillis();
            List<Event> filtered = allEvents.stream()
                    .filter(e -> typeFilter.equals("ALL") || e.getType().name().equals(typeFilter))
                    .filter(e -> statusFilter.equals("ALL") || e.getStatus().name().equals(statusFilter))
                    .filter(e -> startDate == null || !e.getStartDate().isBefore(startDate))
                    .filter(e -> endDate == null || !e.getEndDate().isAfter(endDate))
                    .filter(e -> searchTerm.isEmpty() ||
                           e.getName().toLowerCase().contains(searchTerm) ||
                           e.getLocation().toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
            System.out.println("  ✓ Filter in " + (System.currentTimeMillis() - filterStart) + "ms - found " + filtered.size() + " results");

            displayEvents(filtered);
            System.out.println("✓ onSearch completed in " + (System.currentTimeMillis() - searchStart) + "ms");

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
            System.err.println("✗ onSearch error: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        long resetStart = System.currentTimeMillis();
        System.out.println("🔄 [onReset] Resetting filters...");

        searchField.clear();
        typeFilterCombo.setValue("ALL");
        statusFilterCombo.setValue("ALL");
        startDatePicker.setValue(null);
        endDatePicker.setValue(null);

        displayEvents(allEvents);
        System.out.println("✓ onReset completed in " + (System.currentTimeMillis() - resetStart) + "ms");
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
        long backStart = System.currentTimeMillis();
        System.out.println("🔙 [onBack] Going back to home...");

        SceneManager.switchTo("home.fxml", "Event Manager System - Home");
        System.out.println("✓ onBack completed in " + (System.currentTimeMillis() - backStart) + "ms");
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

    /**
     * Apply gradient fallback when image is not available
     */
    private void applyGradientFallback(VBox imageContainer, Event event) {
        String gradient = getGradientByType(event.getType());
        imageContainer.setStyle("-fx-background: " + gradient + "; -fx-padding: 0; -fx-border-radius: 12 12 0 0;");

        Label imageLabel = new Label(getIconByType(event.getType()));
        imageLabel.setStyle("-fx-font-size: 56; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.2), 3, 0, 0, 1);");
        imageLabel.setAlignment(Pos.CENTER);
        imageContainer.getChildren().add(imageLabel);
    }


    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

