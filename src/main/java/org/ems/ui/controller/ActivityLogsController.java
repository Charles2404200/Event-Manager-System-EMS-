package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import org.ems.config.AppContext;
import org.ems.domain.model.ActivityLog;
import org.ems.domain.repository.ActivityLogRepository;
import org.ems.ui.stage.FxUtils;
import org.ems.ui.stage.SceneManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogsController {

    @FXML private TableView<ActivityLogRow> logsTable;
    @FXML private Label totalLogsLabel;
    @FXML private Label lastActivityLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> actionFilterCombo;
    @FXML private Label pageLabel;

    private ActivityLogRepository activityLogRepo;
    private static final int PAGE_SIZE = 20;
    private int currentPage = 0;
    private int totalPages = 1;
    private List<ActivityLogRow> allLogs = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            activityLogRepo = context.activityLogRepo;

            setupTableColumns();
            setupFilters();
            loadActivityLogsAsync();
        } catch (Exception e) {
            System.err.println("Error initializing activity logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        if (logsTable.getColumns().size() >= 5) {
            ((TableColumn<ActivityLogRow, String>) logsTable.getColumns().get(0))
                    .setCellValueFactory(new PropertyValueFactory<>("timestamp"));
            ((TableColumn<ActivityLogRow, String>) logsTable.getColumns().get(1))
                    .setCellValueFactory(new PropertyValueFactory<>("userId"));
            ((TableColumn<ActivityLogRow, String>) logsTable.getColumns().get(2))
                    .setCellValueFactory(new PropertyValueFactory<>("action"));
            ((TableColumn<ActivityLogRow, String>) logsTable.getColumns().get(3))
                    .setCellValueFactory(new PropertyValueFactory<>("resource"));
            ((TableColumn<ActivityLogRow, String>) logsTable.getColumns().get(4))
                    .setCellValueFactory(new PropertyValueFactory<>("description"));
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

    private void loadActivityLogsAsync() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                long start = System.currentTimeMillis();
                try {
                    System.out.println("[ActivityLogsController] activityLogRepo = " + (activityLogRepo != null ? "OK" : "NULL"));

                    if (activityLogRepo != null) {
                        List<ActivityLog> logs = activityLogRepo.findAll();
                        System.out.println("[ActivityLogsController] Fetched " + logs.size() + " logs from DB");

                        allLogs.clear();

                        for (ActivityLog log : logs) {
                            ActivityLogRow row = new ActivityLogRow(
                                    log.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                                    log.getUserId(),
                                    log.getAction(),
                                    log.getResource(),
                                    log.getDescription()
                            );
                            allLogs.add(row);
                            System.out.println("[ActivityLogsController] Added log: " + log.getAction() + " - " + log.getDescription());
                        }

                        System.out.println("[ActivityLogsController] Loaded " + allLogs.size() + " logs in " +
                                (System.currentTimeMillis() - start) + " ms");
                    } else {
                        System.err.println("[ActivityLogsController] ERROR: activityLogRepo is NULL!");
                    }
                } catch (Exception e) {
                    System.err.println("Error loading activity logs: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };

        task.setOnSucceeded(evt -> {
            System.out.println("[ActivityLogsController] Task succeeded, allLogs size = " + allLogs.size());
            displayLogs(allLogs);
            updateStats();
        });

        task.setOnFailed(evt -> {
            System.err.println("Failed to load activity logs");
            showAlert("Error", "Failed to load activity logs");
        });

        Thread t = new Thread(task, "activity-logs-loader");
        t.setDaemon(true);
        t.start();
    }


    private void displayLogs(List<ActivityLogRow> logs) {
        totalPages = Math.max(1, (int) Math.ceil(logs.size() / (double) PAGE_SIZE));
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, logs.size());

        List<ActivityLogRow> pageData = new ArrayList<>();
        if (start < logs.size()) {
            pageData.addAll(logs.subList(start, end));
        }

        logsTable.setItems(FXCollections.observableArrayList(pageData));
        pageLabel.setText("Page " + (currentPage + 1) + " / " + totalPages);
    }

    private void updateStats() {
        totalLogsLabel.setText(String.valueOf(allLogs.size()));

        if (!allLogs.isEmpty()) {
            lastActivityLabel.setText(allLogs.get(0).getTimestamp());
        } else {
            lastActivityLabel.setText("No activity");
        }
    }

    @FXML
    public void onSearch() {
        String query = searchField.getText().toLowerCase();
        List<ActivityLogRow> filtered = new ArrayList<>();

        for (ActivityLogRow log : allLogs) {
            if (log.getUserId().toLowerCase().contains(query) ||
                log.getAction().toLowerCase().contains(query) ||
                log.getResource().toLowerCase().contains(query) ||
                log.getDescription().toLowerCase().contains(query)) {
                filtered.add(log);
            }
        }

        currentPage = 0;
        displayLogs(filtered);
    }

    @FXML
    public void onFilterByAction() {
        String selectedAction = actionFilterCombo.getValue();
        List<ActivityLogRow> filtered = new ArrayList<>();

        if ("All Actions".equals(selectedAction)) {
            filtered.addAll(allLogs);
        } else {
            for (ActivityLogRow log : allLogs) {
                if (log.getAction().equals(selectedAction)) {
                    filtered.add(log);
                }
            }
        }

        currentPage = 0;
        displayLogs(filtered);
    }

    @FXML
    public void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            displayLogs(allLogs);
        }
    }

    @FXML
    public void onNextPage() {
        if (currentPage + 1 < totalPages) {
            currentPage++;
            displayLogs(allLogs);
        }
    }

    @FXML
    public void onExportLogs() {
        if (allLogs.isEmpty()) {
            showAlert("Info", "No logs to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Activity Logs");
        fileChooser.setInitialFileName(FxUtils.generateFileName("activity_logs", "csv"));
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
                        List<String[]> data = new ArrayList<>();
                        for (ActivityLogRow log : allLogs) {
                            data.add(new String[]{
                                    log.getTimestamp(),
                                    log.getUserId(),
                                    log.getAction(),
                                    log.getResource(),
                                    log.getDescription()
                            });
                        }

                        String[] headers = {"Timestamp", "User ID", "Action", "Resource", "Description"};
                        return FxUtils.exportToCSV(selectedFile.getAbsolutePath(), data, headers);
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
            });

            Thread t = new Thread(exportTask, "activity-logs-exporter");
            t.setDaemon(true);
            t.start();
        }
    }

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
                        if (activityLogRepo != null) {
                            activityLogRepo.deleteAll();
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class ActivityLogRow {
        private String timestamp;
        private String userId;
        private String action;
        private String resource;
        private String description;

        public ActivityLogRow(String timestamp, String userId, String action, String resource, String description) {
            this.timestamp = timestamp;
            this.userId = userId;
            this.action = action;
            this.resource = resource;
            this.description = description;
        }

        public String getTimestamp() { return timestamp; }
        public String getUserId() { return userId; }
        public String getAction() { return action; }
        public String getResource() { return resource; }
        public String getDescription() { return description; }
    }
}

