package org.ems.application.service.ticket;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.ems.domain.dto.TemplateRow;
import org.ems.domain.dto.TicketRow;

/**
 * TicketTableUIService - Manages table column configuration
 * Single Responsibility: Setup table columns and cell factories
 *
 * @author EMS Team
 */
public class TicketTableUIService {

    /**
     * Setup template table columns
     */
    public void setupTemplateTableColumns(TableView<TemplateRow> table) {
        long start = System.currentTimeMillis();
        System.out.println("🎨 [TicketTableUIService] Setting up template table columns...");

        try {
            if (table.getColumns().isEmpty()) {
                System.out.println("  ⚠️ Table has no columns, skipping setup");
                return;
            }

            // Get columns from FXML
            var cols = table.getColumns();

            // Setup column bindings (assuming: event, session, type, price, available)
            if (cols.size() >= 5) {
                ((TableColumn<TemplateRow, String>) cols.get(0)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getEvent()));
                ((TableColumn<TemplateRow, String>) cols.get(1)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getSession()));
                ((TableColumn<TemplateRow, String>) cols.get(2)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
                ((TableColumn<TemplateRow, String>) cols.get(3)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrice()));
                ((TableColumn<TemplateRow, String>) cols.get(4)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getAvailable()));
            }

            System.out.println("  ✓ Template table setup completed in " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error setting up template table: " + e.getMessage());
        }
    }

    /**
     * Setup assigned ticket table columns
     */
    public void setupAssignedTicketTableColumns(TableView<TicketRow> table) {
        long start = System.currentTimeMillis();
        System.out.println("🎨 [TicketTableUIService] Setting up assigned ticket table columns...");

        try {
            if (table.getColumns().isEmpty()) {
                System.out.println("  ⚠️ Table has no columns, skipping setup");
                return;
            }

            var cols = table.getColumns();

            // Setup column bindings (assuming: id, attendee, event, session, type, price, status)
            if (cols.size() >= 7) {
                ((TableColumn<TicketRow, String>) cols.get(0)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getId()));
                ((TableColumn<TicketRow, String>) cols.get(1)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getAttendee()));
                ((TableColumn<TicketRow, String>) cols.get(2)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getEvent()));
                ((TableColumn<TicketRow, String>) cols.get(3)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getSession()));
                ((TableColumn<TicketRow, String>) cols.get(4)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getType()));
                ((TableColumn<TicketRow, String>) cols.get(5)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getPrice()));
                ((TableColumn<TicketRow, String>) cols.get(6)).setCellValueFactory(cd ->
                    new javafx.beans.property.SimpleStringProperty(cd.getValue().getStatus()));
            }

            System.out.println("  ✓ Assigned ticket table setup completed in " + (System.currentTimeMillis() - start) + " ms");
        } catch (Exception e) {
            System.err.println("✗ Error setting up assigned ticket table: " + e.getMessage());
        }
    }
}

