package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;

import java.util.ArrayList;
import java.util.List;

public class ReportViewController {

    @FXML private Label totalEventsLabel;
    @FXML private Label totalAttendeesLabel;
    @FXML private Label totalTicketsSoldLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private TableView<TicketRow> ticketReportTable;

    @FXML
    public void initialize() {
        try {
            setupTableColumns();
            loadReportData();
        } catch (Exception e) {
            System.err.println("Error initializing report view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        if (ticketReportTable.getColumns().size() >= 5) {
            ((TableColumn<TicketRow, String>) ticketReportTable.getColumns().get(0))
                    .setCellValueFactory(new PropertyValueFactory<>("ticketId"));
            ((TableColumn<TicketRow, String>) ticketReportTable.getColumns().get(1))
                    .setCellValueFactory(new PropertyValueFactory<>("attendeeName"));
            ((TableColumn<TicketRow, String>) ticketReportTable.getColumns().get(2))
                    .setCellValueFactory(new PropertyValueFactory<>("eventName"));
            ((TableColumn<TicketRow, String>) ticketReportTable.getColumns().get(3))
                    .setCellValueFactory(new PropertyValueFactory<>("ticketType"));
            ((TableColumn<TicketRow, String>) ticketReportTable.getColumns().get(4))
                    .setCellValueFactory(new PropertyValueFactory<>("price"));
        }
    }

    private void loadReportData() {
        try {
            AppContext context = AppContext.get();
            int eventCount = 0;
            int attendeeCount = 0;
            int ticketCount = 0;
            String totalRevenue = "$0";

            List<TicketRow> rows = new ArrayList<>();

            if (context.eventRepo != null) {
                eventCount = (int) context.eventRepo.count();
            }

            if (context.attendeeRepo != null) {
                attendeeCount = (int) context.attendeeRepo.count();
            }

            if (context.ticketRepo != null) {
                ticketCount = (int) context.ticketRepo.count();
                var tickets = context.ticketRepo.findAll();
                for (var ticket : tickets) {
                    if (ticket.getAttendeeId() != null) {
                        rows.add(new TicketRow(
                                ticket.getId().toString().substring(0, 8),
                                "Attendee",
                                "Event",
                                ticket.getType() != null ? ticket.getType().name() : "N/A",
                                ticket.getPrice() != null ? ticket.getPrice().toString() : "0"
                        ));
                    }
                }
            }

            totalEventsLabel.setText(String.valueOf(eventCount));
            totalAttendeesLabel.setText(String.valueOf(attendeeCount));
            totalTicketsSoldLabel.setText(String.valueOf(ticketCount));
            totalRevenueLabel.setText(totalRevenue);

            ticketReportTable.setItems(FXCollections.observableArrayList(rows));

        } catch (Exception e) {
            System.err.println("Error loading report data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    public static class TicketRow {
        private String ticketId;
        private String attendeeName;
        private String eventName;
        private String ticketType;
        private String price;

        public TicketRow(String ticketId, String attendeeName, String eventName, String ticketType, String price) {
            this.ticketId = ticketId;
            this.attendeeName = attendeeName;
            this.eventName = eventName;
            this.ticketType = ticketType;
            this.price = price;
        }

        public String getTicketId() { return ticketId; }
        public String getAttendeeName() { return attendeeName; }
        public String getEventName() { return eventName; }
        public String getTicketType() { return ticketType; }
        public String getPrice() { return price; }
    }
}

