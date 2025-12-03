package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.ems.config.AppContext;
import org.ems.domain.model.*;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.ui.stage.SceneManager;

import java.util.*;

/**
 * @author <your group number>
 */
public class MyTicketsController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private ComboBox<String> typeFilterCombo;
    @FXML private TableView<TicketRow> ticketsTable;
    @FXML private Label recordCountLabel;
    @FXML private Label totalValueLabel;

    private List<TicketRow> allTickets;
    private AppContext appContext;

    @FXML
    public void initialize() {
        try {
            appContext = AppContext.get();

            // Setup filter combos
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "ACTIVE", "USED", "CANCELLED"
            ));
            statusFilterCombo.setValue("ALL");

            typeFilterCombo.setItems(FXCollections.observableArrayList(
                    "ALL", "GENERAL", "VIP", "EARLY_BIRD", "STUDENT", "GROUP"
            ));
            typeFilterCombo.setValue("ALL");

            // Setup table columns
            setupTableColumns();

            // Load all tickets for current attendee
            loadMyTickets();

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        ObservableList<TableColumn<TicketRow, ?>> columns = ticketsTable.getColumns();

        if (columns.size() >= 8) {
            ((TableColumn<TicketRow, String>) columns.get(0)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().ticketId));
            ((TableColumn<TicketRow, String>) columns.get(1)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().eventName));
            ((TableColumn<TicketRow, String>) columns.get(2)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().sessionName));
            ((TableColumn<TicketRow, String>) columns.get(3)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().type));
            ((TableColumn<TicketRow, String>) columns.get(4)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().price));
            ((TableColumn<TicketRow, String>) columns.get(5)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().status));
            ((TableColumn<TicketRow, String>) columns.get(6)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().purchaseDate));
            ((TableColumn<TicketRow, String>) columns.get(7)).setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().qrCode));
        }
    }

    private void loadMyTickets() {
        try {
            allTickets = new ArrayList<>();

            if (appContext.currentUser instanceof Attendee && appContext.ticketRepo != null && appContext.eventRepo != null) {
                Attendee attendee = (Attendee) appContext.currentUser;

                // Get all tickets for this attendee
                List<Ticket> tickets = appContext.ticketRepo.findByAttendee(attendee.getId());

                // Convert to display rows
                for (Ticket ticket : tickets) {
                    String eventName = "Unknown";
                    String sessionName = "Unknown";

                    try {
                        if (appContext.eventRepo != null && ticket.getEventId() != null) {
                            Event event = appContext.eventRepo.findById(ticket.getEventId());
                            if (event != null) eventName = event.getName();
                        }
                        if (appContext.sessionRepo != null && ticket.getSessionId() != null) {
                            Session session = appContext.sessionRepo.findById(ticket.getSessionId());
                            if (session != null) sessionName = session.getTitle();
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading ticket details: " + e.getMessage());
                    }

                    allTickets.add(new TicketRow(
                            ticket.getId().toString().substring(0, 8),
                            eventName,
                            sessionName,
                            ticket.getType() != null ? ticket.getType().name() : "N/A",
                            ticket.getPrice() != null ? "$" + ticket.getPrice() : "$0",
                            ticket.getTicketStatus() != null ? ticket.getTicketStatus().name() : "N/A",
                            "2025-12-03", // TODO: Add created_at timestamp to Ticket model
                            ticket.getQrCodeData() != null ? ticket.getQrCodeData() : "N/A"
                    ));
                }
            }

            displayTickets(allTickets);
            calculateTotalValue();

        } catch (Exception e) {
            showAlert("Error", "Failed to load tickets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayTickets(List<TicketRow> tickets) {
        ObservableList<TicketRow> observableList = FXCollections.observableArrayList(tickets);
        ticketsTable.setItems(observableList);
        recordCountLabel.setText("Total Tickets: " + tickets.size());
    }

    private void calculateTotalValue() {
        try {
            double total = 0;
            for (TicketRow ticket : allTickets) {
                String priceStr = ticket.price.replace("$", "");
                total += Double.parseDouble(priceStr);
            }
            totalValueLabel.setText("Total Value: $" + String.format("%.2f", total));
        } catch (Exception e) {
            totalValueLabel.setText("Total Value: $0.00");
        }
    }

    @FXML
    public void onSearch() {
        try {
            String searchTerm = searchField.getText().toLowerCase();
            String statusFilter = statusFilterCombo.getValue();
            String typeFilter = typeFilterCombo.getValue();

            List<TicketRow> filtered = new ArrayList<>();

            for (TicketRow ticket : allTickets) {
                // Apply status filter
                if (!statusFilter.equals("ALL") && !ticket.status.equals(statusFilter)) {
                    continue;
                }

                // Apply type filter
                if (!typeFilter.equals("ALL") && !ticket.type.equals(typeFilter)) {
                    continue;
                }

                // Apply search filter
                if (searchTerm.isEmpty() ||
                    ticket.ticketId.toLowerCase().contains(searchTerm) ||
                    ticket.eventName.toLowerCase().contains(searchTerm) ||
                    ticket.sessionName.toLowerCase().contains(searchTerm)) {
                    filtered.add(ticket);
                }
            }

            displayTickets(filtered);

        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    @FXML
    public void onReset() {
        searchField.clear();
        statusFilterCombo.setValue("ALL");
        typeFilterCombo.setValue("ALL");
        displayTickets(allTickets);
    }

    @FXML
    public void onViewQRCode() {
        TicketRow selected = ticketsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Warning", "Please select a ticket to view QR code");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ticket QR Code");
        dialog.setHeaderText("QR Code for: " + selected.eventName);

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-alignment: center;");

        Label eventLabel = new Label("Event: " + selected.eventName);
        eventLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Label sessionLabel = new Label("Session: " + selected.sessionName);
        sessionLabel.setStyle("-fx-font-size: 12;");

        Label typeLabel = new Label("Type: " + selected.type);
        typeLabel.setStyle("-fx-font-size: 12;");

        Label priceLabel = new Label("Price: " + selected.price);
        priceLabel.setStyle("-fx-font-size: 12;");

        Label qrLabel = new Label(selected.qrCode);
        qrLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2c3e50; " +
                "-fx-border-color: #2c3e50; -fx-border-width: 2; -fx-padding: 15; " +
                "-fx-background-color: #ecf0f1; -fx-border-radius: 5;");

        content.getChildren().addAll(
                eventLabel,
                new Separator(),
                sessionLabel,
                typeLabel,
                priceLabel,
                new Label("QR Code:"),
                qrLabel,
                new Separator(),
                new Label("Show this QR code at event entrance")
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML
    public void onExportTickets() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Ticket ID,Event,Session,Type,Price,Status,Purchase Date,QR Code\n");

            for (TicketRow ticket : allTickets) {
                csv.append(ticket.ticketId).append(",")
                   .append(ticket.eventName).append(",")
                   .append(ticket.sessionName).append(",")
                   .append(ticket.type).append(",")
                   .append(ticket.price).append(",")
                   .append(ticket.status).append(",")
                   .append(ticket.purchaseDate).append(",")
                   .append(ticket.qrCode).append("\n");
            }

            // TODO: Save CSV to file
            showAlert("Success", "Export feature coming soon!");

        } catch (Exception e) {
            showAlert("Error", "Export failed: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class
    public static class TicketRow {
        public String ticketId;
        public String eventName;
        public String sessionName;
        public String type;
        public String price;
        public String status;
        public String purchaseDate;
        public String qrCode;

        public TicketRow(String ticketId, String eventName, String sessionName, String type,
                        String price, String status, String purchaseDate, String qrCode) {
            this.ticketId = ticketId;
            this.eventName = eventName;
            this.sessionName = sessionName;
            this.type = type;
            this.price = price;
            this.status = status;
            this.purchaseDate = purchaseDate;
            this.qrCode = qrCode;
        }

        // Getters for TableView binding
        public String getTicketId() { return ticketId; }
        public String getEventName() { return eventName; }
        public String getSessionName() { return sessionName; }
        public String getType() { return type; }
        public String getPrice() { return price; }
        public String getStatus() { return status; }
        public String getPurchaseDate() { return purchaseDate; }
        public String getQrCode() { return qrCode; }
    }
}

