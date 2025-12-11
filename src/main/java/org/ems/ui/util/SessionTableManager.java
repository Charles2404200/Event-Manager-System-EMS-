package org.ems.ui.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.ems.domain.model.Session;
import org.ems.application.service.SessionEventManager;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * SessionTableManager - Manages table configuration and column setup
 * Single Responsibility: Setup and manage UI table structure
 *
 * @author EMS Team
 */
public class SessionTableManager {

    private final TableView<Session> sessionTable;
    private final TableColumn<Session, String> colId;
    private final TableColumn<Session, String> colTitle;
    private final TableColumn<Session, String> colEvent;
    private final TableColumn<Session, String> colStart;
    private final TableColumn<Session, String> colVenue;
    private final TableColumn<Session, Integer> colCapacity;

    private final SessionEventManager eventManager;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Constructor with table columns and event manager
     */
    public SessionTableManager(TableView<Session> sessionTable,
                              TableColumn<Session, String> colId,
                              TableColumn<Session, String> colTitle,
                              TableColumn<Session, String> colEvent,
                              TableColumn<Session, String> colStart,
                              TableColumn<Session, String> colVenue,
                              TableColumn<Session, Integer> colCapacity,
                              SessionEventManager eventManager) {
        this.sessionTable = sessionTable;
        this.colId = colId;
        this.colTitle = colTitle;
        this.colEvent = colEvent;
        this.colStart = colStart;
        this.colVenue = colVenue;
        this.colCapacity = colCapacity;
        this.eventManager = eventManager;
    }

    /**
     * Setup all table columns
     */
    public void setupTableColumns() {
        long start = System.currentTimeMillis();
        System.out.println("🎨 [SessionTableManager] Setting up table columns...");

        // ID column - show first 8 characters
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getId().toString().substring(0, 8) + "..."
                ));

        // Title column
        colTitle.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTitle()
                ));

        // Event column - initially empty, will be filled after events loaded
        colEvent.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(""));

        // Start time column
        colStart.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStart().format(formatter)
                ));

        // Venue column
        colVenue.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getVenue()
                ));

        // Capacity column
        colCapacity.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getCapacity()
                ).asObject());

        System.out.println("  ✓ Table columns setup completed in " +
                (System.currentTimeMillis() - start) + " ms");
    }

    /**
     * Setup event name mapping for the table
     * This should be called after events are loaded
     */
    public void setupEventNameMapping() {
        long start = System.currentTimeMillis();
        System.out.println("🗺️ [SessionTableManager] Setting up event name mapping...");

        Map<UUID, String> eventNameMap = eventManager.buildEventNameMap();

        colEvent.setCellValueFactory(data -> {
            UUID eventId = data.getValue().getEventId();
            String name = eventNameMap.getOrDefault(eventId, "N/A");
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        System.out.println("  ✓ Event name mapping setup completed in " +
                (System.currentTimeMillis() - start) + " ms");
    }
}

