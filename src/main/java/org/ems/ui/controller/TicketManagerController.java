package org.ems.ui.controller;
/**
 * @author <your group number>
 */

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.ems.application.service.TicketService;
import org.ems.domain.model.Ticket;
import org.ems.config.AppContext;
import org.ems.ui.stage.SceneManager;

public class TicketManagerController {

    @FXML private TableView<Ticket> ticketTable;
    @FXML private TableColumn<Ticket, String> colId;
    @FXML private TableColumn<Ticket, String> colAttendee;
    @FXML private TableColumn<Ticket, String> colEvent;
    @FXML private TableColumn<Ticket, String> colType;
    @FXML private TableColumn<Ticket, String> colStatus;
    @FXML private TableColumn<Ticket, String> colPrice;

    private final TicketService ticketService = AppContext.get().ticketService;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getId().toString()
                ));
        colAttendee.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getAttendeeId() != null
                                ? data.getValue().getAttendeeId().toString()
                                : ""
                ));
        colEvent.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEventId() != null
                                ? data.getValue().getEventId().toString()
                                : ""
                ));
        colType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getType().name()
                ));
        colStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTicketStatus().name()
                ));
        colPrice.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getPrice().toPlainString()
                ));

        loadTickets();
    }

    private void loadTickets() {
        ticketTable.setItems(FXCollections.observableList(ticketService.getTickets()));
    }

    @FXML
    public void onRefresh() {
        loadTickets();
    }

    @FXML
    public void backToDashboard() {
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }
}
