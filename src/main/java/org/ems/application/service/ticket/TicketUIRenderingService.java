package org.ems.application.service.ticket;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import org.ems.application.dto.template.TemplateRow;
import org.ems.application.dto.ticket.TicketRow;

/**
 * TicketUIRenderingService - Handles UI rendering of tables and data display
 * Single Responsibility: Table column setup and row formatting
 *
 * @author EMS Team
 */
public class TicketUIRenderingService {

    /**
     * Setup column value factories for template table
     */
    public void setupTemplateTableColumns(TableView<TemplateRow> table) {
        if (table == null || table.getColumns() == null) {
            return;
        }

        var cols = table.getColumns();
        if (cols.size() >= 5) {
            ((TableColumn<TemplateRow, String>) cols.get(0))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEvent()));
            ((TableColumn<TemplateRow, String>) cols.get(1))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSession()));
            ((TableColumn<TemplateRow, String>) cols.get(2))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType()));
            ((TableColumn<TemplateRow, String>) cols.get(3))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPrice()));
            ((TableColumn<TemplateRow, String>) cols.get(4))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAvailable()));
        }
    }

    /**
     * Setup column value factories for assigned tickets table
     */
    public void setupAssignedTicketTableColumns(TableView<TicketRow> table) {
        if (table == null || table.getColumns() == null) {
            return;
        }

        var cols = table.getColumns();
        if (cols.size() >= 7) {
            ((TableColumn<TicketRow, String>) cols.get(0))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getId()));
            ((TableColumn<TicketRow, String>) cols.get(1))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getAttendee()));
            ((TableColumn<TicketRow, String>) cols.get(2))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEvent()));
            ((TableColumn<TicketRow, String>) cols.get(3))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSession()));
            ((TableColumn<TicketRow, String>) cols.get(4))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getType()));
            ((TableColumn<TicketRow, String>) cols.get(5))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getPrice()));
            ((TableColumn<TicketRow, String>) cols.get(6))
                    .setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        }
    }
}

