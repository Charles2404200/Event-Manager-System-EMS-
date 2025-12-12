package org.ems.application.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.ems.application.service.activity.*;
import org.ems.infrastructure.config.AppContext;
import org.ems.ui.stage.FxUtils;
import org.ems.ui.stage.SceneManager;
import org.ems.application.dto.activity.ActivityLogDisplayDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity Logs Controller - SOLID REFACTORED
 * - Single Responsibility: UI coordination only
 * - Dependency Injection: Services injected via constructor
 * - Delegation: Business logic delegated to services
 * - Clean Architecture: Separated concerns between UI, Services, and Data layers
 * @author <your group number>
 */
public class ActivityLogsController {

    @FXML private TableView<ActivityLogDisplayDTO> logsTable;
    @FXML private Label totalLogsLabel;
    @FXML private Label lastActivityLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private Label pageLabel;

    private List<ActivityLogDisplayDTO> allLogs = new ArrayList<>();
    private List<ActivityLogDisplayDTO> currentFilteredLogs = new ArrayList<>();
    private int currentPage = 0;
    private int totalPages = 1;

    // Injected Services
    private ActivityLogDataLoaderService dataLoaderService;
    private ActivityLogFilterService filterService;
    private ActivityLogStatisticsService statisticsService;
    private ActivityLogExportService exportService;
    private ActivityLogPaginationService paginationService;

    @FXML
    public void initialize() {
        long initStart = System.currentTimeMillis();
        System.out.println("📊 [ActivityLogsController] initialize() starting...");
        try {
            AppContext context = AppContext.get();

            // Inject services
            long serviceStart = System.currentTimeMillis();
            dataLoaderService = new ActivityLogDataLoaderServiceImpl(context.activityLogRepo);
            filterService = new ActivityLogFilterServiceImpl();
            statisticsService = new ActivityLogStatisticsServiceImpl();
            exportService = new ActivityLogExportServiceImpl();
            paginationService = new ActivityLogPaginationServiceImpl(20);
            System.out.println("  ✓ Services initialized in " + (System.currentTimeMillis() - serviceStart) + "ms");

            // Setup table columns
            long colStart = System.currentTimeMillis();
            setupTableColumns();
            System.out.println("  ✓ Table columns setup in " + (System.currentTimeMillis() - colStart) + "ms");

            // Setup filters
            long filterStart = System.currentTimeMillis();
            setupFilters();
            System.out.println("  ✓ Filters setup in " + (System.currentTimeMillis() - filterStart) + "ms");

            System.out.println("  ✓ UI initialized in " + (System.currentTimeMillis() - initStart) + "ms");
            System.out.println("  🔄 Starting async load...");

            // Load logs asynchronously
            loadActivityLogsAsync();

        } catch (Exception e) {
            System.err.println("✗ initialize() failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Setup table columns with proper binding to ActivityLogDisplayDTO
     */
    private void setupTableColumns() {
        if (logsTable.getColumns().size() >= 5) {
            ((TableColumn<ActivityLogDisplayDTO, String>) logsTable.getColumns().get(0))
                    .setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimestamp()));
            ((TableColumn<ActivityLogDisplayDTO, String>) logsTable.getColumns().get(1))
                    .setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getUserId()));
            ((TableColumn<ActivityLogDisplayDTO, String>) logsTable.getColumns().get(2))
                    .setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getAction()));
            ((TableColumn<ActivityLogDisplayDTO, String>) logsTable.getColumns().get(3))
                    .setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getResource()));
            ((TableColumn<ActivityLogDisplayDTO, String>) logsTable.getColumns().get(4))
                    .setCellValueFactory(cellData ->
                        new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDescription()));
        }
    }

    private void setupFilters() {
        actionFilterCombo.setItems(FXCollections.observableArrayList(
                "All Actions",
                "CREATE",
                "UPDATE",
                "DELETE",
                "VIEW",
                "EXPORT",
                "LOGIN",
                "LOGOUT"
        ));
        actionFilterCombo.setValue("All Actions");
    }

    /**
     * Load activity logs asynchronously via ActivityLogDataLoaderService
     */
    private void loadActivityLogsAsync() {
        Task<List<ActivityLogDisplayDTO>> task = new Task<>() {
            @Override
            protected List<ActivityLogDisplayDTO> call() {
                long start = System.currentTimeMillis();
                System.out.println("    🔄 [Background] Loading activity logs...");
                try {
                    // Delegate to service - all loading and conversion logic
                    return dataLoaderService.loadAllLogs();
                } catch (ActivityLogDataLoaderService.ActivityLogException e) {
                    System.err.println("    ✗ Error loading logs: " + e.getMessage());
                    return new ArrayList<>();
                }
            }
        };

        task.setOnSucceeded(evt -> {
            long uiStart = System.currentTimeMillis();
            allLogs = task.getValue();
            displayLogs(allLogs);
            updateStats();
            System.out.println("  ✓ UI updated in " + (System.currentTimeMillis() - uiStart) + "ms");
            System.out.println("✓ Activity logs loaded successfully");
        });

        task.setOnFailed(evt -> {
            System.err.println("✗ Failed to load activity logs");
            showAlert("Error", "Failed to load activity logs");
        });

        Thread t = new Thread(task, "activity-logs-loader");
        t.setDaemon(true);
        t.start();
    }


    /**
     * Display logs on current page
     */
    private void displayLogs(List<ActivityLogDisplayDTO> logs) {
        currentFilteredLogs = logs;
        totalPages = paginationService.getTotalPages(logs.size());
        currentPage = 0;

        List<ActivityLogDisplayDTO> pageData = paginationService.getPage(logs, currentPage);
        logsTable.setItems(FXCollections.observableArrayList(pageData));
        pageLabel.setText(paginationService.getPageLabel(currentPage, totalPages));
    }

    /**
     * Update statistics via ActivityLogStatisticsService
     */
    private void updateStats() {
        int totalLogs = statisticsService.getTotalCount(allLogs);
        String lastActivity = statisticsService.getLastActivityTimestamp(allLogs);

        totalLogsLabel.setText(String.valueOf(totalLogs));
        lastActivityLabel.setText(lastActivity);
    }

    /**
     * Handle search filter
     * Delegates filtering logic to ActivityLogFilterService
     */
    @FXML
    public void onSearch() {
        long searchStart = System.currentTimeMillis();
        System.out.println("🔎 [ActivityLogsController] onSearch() starting...");
        try {
            String searchTerm = searchField.getText();
            List<ActivityLogDisplayDTO> filtered = filterService.filterBySearchTerm(allLogs, searchTerm);

            displayLogs(filtered);
            System.out.println("  ✓ onSearch() completed in " + (System.currentTimeMillis() - searchStart) + "ms");
        } catch (Exception e) {
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    /**
     * Handle filter by action
     * Delegates filtering logic to ActivityLogFilterService
     */
    @FXML
    public void onFilterByAction() {
        long filterStart = System.currentTimeMillis();
        System.out.println("🔎 [ActivityLogsController] onFilterByAction() starting...");
        try {
            String selectedAction = actionFilterCombo.getValue();
            List<ActivityLogDisplayDTO> filtered = filterService.filterByAction(allLogs, selectedAction);

            displayLogs(filtered);
            System.out.println("  ✓ onFilterByAction() completed in " + (System.currentTimeMillis() - filterStart) + "ms");
        } catch (Exception e) {
            showAlert("Error", "Filter failed: " + e.getMessage());
        }
    }

    /**
     * Handle previous page button
     */
    @FXML
    public void onPrevPage() {
        if (paginationService.canGoPreviousPage(currentPage)) {
            currentPage--;
            List<ActivityLogDisplayDTO> pageData = paginationService.getPage(currentFilteredLogs, currentPage);
            logsTable.setItems(FXCollections.observableArrayList(pageData));
            pageLabel.setText(paginationService.getPageLabel(currentPage, totalPages));
        }
    }

    /**
     * Handle next page button
     */
    @FXML
    public void onNextPage() {
        if (paginationService.canGoNextPage(currentPage, totalPages)) {
            currentPage++;
            List<ActivityLogDisplayDTO> pageData = paginationService.getPage(currentFilteredLogs, currentPage);
            logsTable.setItems(FXCollections.observableArrayList(pageData));
            pageLabel.setText(paginationService.getPageLabel(currentPage, totalPages));
        }
    }

    /**
     * Handle export logs button
     * Delegates CSV generation to ActivityLogExportService
     */
    @FXML
    public void onExportLogs() {
        long exportStart = System.currentTimeMillis();
        System.out.println("📤 [ActivityLogsController] onExportLogs() starting...");

        if (allLogs.isEmpty()) {
            showAlert("Info", "No logs to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Logs");
        fileChooser.setInitialFileName(exportService.generateFilename());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showSaveDialog(logsTable.getScene().getWindow());
        if (selectedFile != null) {
            Task<Boolean> exportTask = new Task<>() {
                @Override
                protected Boolean call() {
                    try {
                        // Delegate to service for CSV conversion
                        String[][] data = exportService.convertToCSVData(allLogs);
                        String[] headers = exportService.getCSVHeaders();

                        return FxUtils.exportToCSV(selectedFile.getAbsolutePath(),
                                java.util.Arrays.asList(data).subList(1, data.length), headers);
                    } catch (Exception e) {
                        System.err.println("Export error: " + e.getMessage());
                        return false;
                    }
                }
            };

            exportTask.setOnSucceeded(evt -> {
                if (exportTask.getValue()) {
                    showAlert("Success", "Activity logs exported successfully to:\n" + selectedFile.getAbsolutePath());
                } else {
                    showAlert("Error", "Failed to export logs");
                }
                System.out.println("  ✓ onExportLogs() completed in " + (System.currentTimeMillis() - exportStart) + "ms");
            });

            Thread t = new Thread(exportTask, "activity-logs-exporter");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * Handle clear logs button
     */
    @FXML
    public void onClearLogs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Logs");
        confirm.setContentText("Are you sure you want to clear all activity logs? This cannot be undone.");

        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isPresent()) {
            Task<Void> clearTask = new Task<>() {
                @Override
                protected Void call() {
                    try {
                        // Clear logs via repository (if available)
                        AppContext context = AppContext.get();
                        if (context.activityLogRepo != null) {
                            context.activityLogRepo.deleteAll();
                        }
                    } catch (Exception e) {
                        System.err.println("Error clearing logs: " + e.getMessage());
                    }
                    return null;
                }
            };

            clearTask.setOnSucceeded(evt -> {
                allLogs.clear();
                displayLogs(allLogs);
                updateStats();
                showAlert("Success", "All activity logs cleared");
            });

            Thread t = new Thread(clearTask, "activity-logs-clearer");
            t.setDaemon(true);
            t.start();
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    /**
     * Show alert dialog to user
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

