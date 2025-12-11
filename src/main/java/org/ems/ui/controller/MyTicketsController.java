package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.application.Platform;
import org.ems.config.AppContext;
import org.ems.domain.model.Attendee;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.application.dto.TicketDisplayDTO;
import org.ems.application.service.TicketLoaderService;
import org.ems.application.service.TicketFilterService;
import org.ems.application.service.TicketCalculationService;
import org.ems.application.service.TicketQRCodeService;
import org.ems.application.service.TicketExportService;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * My Tickets Controller - SOLID REFACTORED
 * - Single Responsibility: UI coordination only
 * - Dependency Injection: Services injected via constructor
 * - Delegation: Business logic delegated to services
 * - Clean Architecture: Separated concerns between UI, Services, and Data layers
 * @author <your group number>
 */
public class MyTicketsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<TicketDisplayDTO> ticketsTable;
    @FXML private Label recordCountLabel;
    @FXML private Label totalValueLabel;

    private List<TicketDisplayDTO> allTickets;
    private AppContext appContext;

    // Injected Services
    private TicketLoaderService ticketLoaderService;
    private TicketFilterService ticketFilterService;
    private TicketCalculationService ticketCalculationService;
    private TicketQRCodeService ticketQRCodeService;
    private TicketExportService ticketExportService;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("📋 [MyTicketsController] initialize() starting...");
        try {
            appContext = AppContext.get();

            // Inject services
            long serviceStart = System.currentTimeMillis();
            ticketLoaderService = new TicketLoaderService(appContext.ticketRepo, appContext.eventRepo);
            ticketFilterService = new TicketFilterService();
            ticketCalculationService = new TicketCalculationService();
            ticketQRCodeService = new TicketQRCodeService();
            ticketExportService = new TicketExportService();
            System.out.println("  ✓ Services initialized in " + (System.currentTimeMillis() - serviceStart) + "ms");

            // Setup filter combos
            long comboStart = System.currentTimeMillis();
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "ACTIVE", "USED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "GENERAL", "VIP", "EARLY_BIRD", "STUDENT", "GROUP"
            ));
            typeFilterCombo.setValue("ALL");
            System.out.println("  ✓ Filters setup in " + (System.currentTimeMillis() - comboStart) + "ms");

            // Setup table columns
            long colStart = System.currentTimeMillis();
            setupTableColumns();
            System.out.println("  ✓ Table columns setup in " + (System.currentTimeMillis() - colStart) + "ms");

            System.out.println("  ✓ UI initialized in " + (System.currentTimeMillis() - initStart) + "ms");
            System.out.println("  🔄 Starting async load...");

            // Load all tickets asynchronously
            loadMyTicketsAsync();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup table columns with proper binding to TicketDisplayDTO
     */
    private void setupTableColumns() {
        ObservableList<TableColumn<TicketDisplayDTO, ?>> columns = ticketsTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<TicketDisplayDTO, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTicketId()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEventName()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getSessionName()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPrice()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPurchaseDate()));
            ((TableColumn<TicketDisplayDTO, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getQrCode()));
        }
    }

    /**
     * Load tickets asynchronously via TicketLoaderService
     * Preserves batch loading optimization (single query per repository)
     */
    private void loadMyTicketsAsync() {
        long asyncStart = System.currentTimeMillis();

        AsyncTaskService.runAsync(
                () -> {
                    long taskStart = System.currentTimeMillis();
                    System.out.println("    🔄 [Background] Loading tickets...");

                    try {
                        if (appContext.currentUser instanceof Attendee) {
                            Attendee attendee = (Attendee) appContext.currentUser;
                            // Delegate to service - all batch optimization preserved
                            return ticketLoaderService.loadAttendeeTickets(attendee);
                        } else {
                            System.out.println("  ⚠️ User is not attendee");
                            return new ArrayList<>();
                        }

                    } catch (TicketLoaderService.TicketLoaderException e) {
                        System.err.println("    ✗ Error loading tickets: " + e.getMessage());
                        return new ArrayList<>();
                    }
                },
                tickets -> {
                    long uiStart = System.currentTimeMillis();
                    @SuppressWarnings("unchecked")
                    List<TicketDisplayDTO> result = (List<TicketDisplayDTO>) tickets;

                    allTickets = result;
                    displayTickets(allTickets);
                    calculateAndDisplayTotalValue();
                    System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + " ms");
                    System.out.println("✓ MyTickets loaded successfully in " + (System.currentTimeMillis() - asyncStart) + " ms");
                },
                error -> {
                    showAlert("Error", "Failed to load tickets: " + error.getMessage());
                    System.err.println("✗ Error loading tickets: " + error.getMessage());
                }
        );
    }

    /**
     * Display tickets in table
     */
    private void displayTickets(List<TicketDisplayDTO> tickets) {
        ObservableList<TicketDisplayDTO> observableList = FXCollections.observableArrayList(tickets);
        ticketsTable.setItems(observableList);
        recordCountLabel.setText("Total Tickets: " + tickets.size());
    }

    /**
     * Calculate and display total value via service
     */
    private void calculateAndDisplayTotalValue() {
        try {
            double totalValue = ticketCalculationService.calculateTotalValue(allTickets);
            totalValueLabel.setText("Total Value: " + ticketCalculationService.formatPrice(totalValue));
        } catch (Exception e) {
            totalValueLabel.setText("Total Value: $0.00");
            System.err.println("Error calculating total value: " + e.getMessage());
        }
    }

    /**
     * Handle search filter
     * Delegates filtering logic to TicketFilterService
     */
    @FXML
    public void onSearch() {
        long searchStart = System.currentTimeMillis();
        System.out.println("🔎 [MyTicketsController] onSearch() starting...");
        try {
            String searchTerm = searchField.getText();
            String statusFilter = statusFilterCombo.getValue();
            String typeFilter = typeFilterCombo.getValue();

            // Delegate to service
            List<TicketDisplayDTO> filtered = ticketFilterService.filterTickets(
                    allTickets, searchTerm, statusFilter, typeFilter);

            displayTickets(filtered);
            System.out.println("  ✓ onSearch() completed in " + (System.currentTimeMillis() - searchStart) + "ms");

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    /**
     * Reset search and filter criteria
     */
    @FXML
    public void onReset() {
        long resetStart = System.currentTimeMillis();
        System.out.println("🔄 [MyTicketsController] onReset() called");
        searchField.clear();
        statusFilterCombo.setValue("ALL");
        typeFilterCombo.setValue("ALL");
        displayTickets(allTickets);
        calculateAndDisplayTotalValue();
        System.out.println("  ✓ onReset() completed in " + (System.currentTimeMillis() - resetStart) + "ms");
    }

    /**
     * Handle view QR code button
     * Delegates QR code generation to TicketQRCodeService
     */
    @FXML
    public void onViewQRCode() {
        TicketDisplayDTO selected = ticketsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a ticket to view QR code");
            return;
        }

        try {
            // Check if ticket has valid QR code
            if (!ticketQRCodeService.hasValidQRCode(selected)) {
                showAlert("Info", "No QR code available for this ticket");
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("🎟️ Your Ticket - QR Code");
            dialog.setHeaderText("Show this QR code at event entrance");

            VBox content = new VBox(15);
            content.setPadding(new Insets(20));
            content.setStyle("-fx-alignment: center;");

            // Ticket Info Section
            VBox infoBox = new VBox(8);
            infoBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 15; -fx-border-radius: 5;");

            Label eventLabel = new Label("🎪 Event: " + selected.getEventName());
            eventLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

            Label sessionLabel = new Label("🎤 Session: " + selected.getSessionName());
            sessionLabel.setStyle("-fx-font-size: 12;");

            Label typeLabel = new Label("🎫 Type: " + selected.getType());
            typeLabel.setStyle("-fx-font-size: 12;");

            Label priceLabel = new Label("💰 Price: " + selected.getPrice());
            priceLabel.setStyle("-fx-font-size: 12;");

            Label statusLabel = new Label("✅ Status: " + selected.getStatus());
            statusLabel.setStyle("-fx-font-size: 12;");

            infoBox.getChildren().addAll(eventLabel, sessionLabel, typeLabel, priceLabel, statusLabel);
            content.getChildren().add(infoBox);

            // QR Code Image Section
            byte[] qrCodeImage = ticketQRCodeService.generateQRCodeImage(selected.getQrCode());

            if (qrCodeImage != null) {
                try {
                    // Create ImageView for QR code
                    ImageView qrImageView = new ImageView(
                        new Image(new ByteArrayInputStream(qrCodeImage))
                    );
                    qrImageView.setFitWidth(300);
                    qrImageView.setFitHeight(300);
                    qrImageView.setPreserveRatio(true);
                    qrImageView.setStyle("-fx-border-color: #2c3e50; -fx-border-width: 2;");

                    // QR Code container with label
                    VBox qrBox = new VBox(10);
                    qrBox.setStyle("-fx-alignment: center; -fx-border-color: #ecf0f1; -fx-border-width: 1; -fx-padding: 20; -fx-border-radius: 5;");

                    Label qrTitleLabel = new Label("QR Code");
                    qrTitleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                    qrBox.getChildren().addAll(qrTitleLabel, qrImageView);
                    content.getChildren().add(qrBox);

                    System.out.println("✓ QR code image generated and displayed");
                } catch (Exception e) {
                    System.err.println("Failed to generate QR code image: " + e.getMessage());
                    addQRCodeTextFallback(content, selected.getQrCode(), "Text Fallback");
                }
            } else {
                addQRCodeTextFallback(content, selected.getQrCode(), "Text");
            }

            // Instructions
            Label instructionLabel = new Label("📱 Show this QR code at the event entrance to check in");
            instructionLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666666; -fx-padding: 10;");
            instructionLabel.setWrapText(true);
            content.getChildren().add(new Separator());
            content.getChildren().add(instructionLabel);

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();

        } catch (Exception e) {
            System.err.println("Error displaying QR code: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to display QR code: " + e.getMessage());
        }
    }

    /**
     * Add QR code text fallback to dialog
     */
    private void addQRCodeTextFallback(VBox content, String qrCode, String label) {
        Label qrLabel = new Label(qrCode);
        qrLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #2c3e50; " +
                "-fx-border-color: #2c3e50; -fx-border-width: 1; -fx-padding: 10; " +
                "-fx-background-color: #ecf0f1; -fx-border-radius: 3;");
        qrLabel.setWrapText(true);

        VBox qrBox = new VBox(10);
        qrBox.setStyle("-fx-alignment: center;");
        qrBox.getChildren().addAll(
            new Label("QR Code (" + label + "):"),
            qrLabel
        );
        content.getChildren().add(qrBox);
    }

    /**
     * Handle export tickets button
     * Delegates CSV generation to TicketExportService
     */
    @FXML
    public void onExportTickets() {
        long exportStart = System.currentTimeMillis();
        System.out.println("📤 [MyTicketsController] onExportTickets() starting...");

        try {
            // Delegate to service to generate CSV
            String csv = ticketExportService.generateCSV(allTickets);

            if (csv.isEmpty()) {
                showAlert("Warning", "No tickets to export");
                return;
            }

            // TODO: Save CSV to file (implement file dialog and write)
            // String filename = ticketExportService.generateFilename();
            // FileUtils.writeToFile(filename, csv);

            showAlert("Success", "Export feature coming soon!\n\n" +
                    "This would export " + allTickets.size() + " tickets to CSV file.");

            System.out.println("  ✓ onExportTickets() completed in " + (System.currentTimeMillis() - exportStart) + "ms");

        } catch (Exception e) {
            showAlert("Error", "Export failed: " + e.getMessage());
            System.err.println("✗ Export error: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }

    /**
     * Show alert dialog to user
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

