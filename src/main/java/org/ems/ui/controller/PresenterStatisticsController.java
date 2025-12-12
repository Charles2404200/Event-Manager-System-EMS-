package org.ems.ui.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.ems.application.service.PresenterStatisticsService;
import org.ems.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.domain.model.Presenter;
import org.ems.ui.stage.FxUtils;
import org.ems.ui.stage.SceneManager;
import org.ems.ui.util.AsyncTaskService;
import org.ems.domain.dto.PresenterStatistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author <your group number>
 *
 * Controller for presenter statistics dashboard
 */
public class PresenterStatisticsController {

    @FXML private Label presenterNameLabel;
    @FXML private Label totalSessionsLabel;
    @FXML private Label totalAttendeesLabel;
    @FXML private Label avgAudienceSizeLabel;
    @FXML private Label upcomingSessionsLabel;
    @FXML private Label completedSessionsLabel;

    @FXML private PieChart eventTypeChart;
    @FXML private BarChart<String, Number> engagementChart;
    @FXML private VBox loadingBox;
    @FXML private VBox contentBox;

    private PresenterStatisticsService statsService;
    private UUID presenterId;
    private String presenterName;

    @FXML
    public void initialize() {
        try {
            AppContext context = AppContext.get();
            statsService = context.presenterStatsService;

            // Get current user (presenter)
            Person currentUser = context.currentUser;

            if (currentUser instanceof Presenter) {
                presenterId = currentUser.getId();
                presenterName = currentUser.getFullName();
                presenterNameLabel.setText("Statistics for: " + presenterName);

                // Load statistics asynchronously
                loadStatisticsAsync();
            } else {
                showAlert("Error", "This page is only accessible to presenters");
                onBack();
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load statistics on background thread to avoid blocking UI
     */
    private void loadStatisticsAsync() {
        showLoading();

        AsyncTaskService.runAsync(
                // Background task
                () -> {
                    if (statsService != null) {
                        return statsService.generateStatistics(presenterId);
                    }
                    return new PresenterStatistics();
                },

                // Success callback
                stats -> {
                    updateUI(stats);
                    hideLoading();
                },

                // Error callback
                error -> {
                    hideLoading();
                    showAlert("Error", "Failed to load statistics: " + error.getMessage());
                }
        );
    }

    /**
     * Update UI with statistics data
     */
    private void updateUI(PresenterStatistics stats) {
        // Update summary cards
        totalSessionsLabel.setText(String.valueOf(stats.totalSessions));
        totalAttendeesLabel.setText(String.valueOf(stats.totalAttendees));
        avgAudienceSizeLabel.setText(String.valueOf(stats.averageAudienceSize));
        upcomingSessionsLabel.setText(String.valueOf(stats.upcomingSessions));
        completedSessionsLabel.setText(String.valueOf(stats.completedSessions));

        // Update event type distribution chart
        updateEventTypeChart(stats.eventTypeDistribution);

        // Update engagement trends chart
        updateEngagementChart(stats.sessionEngagementTrends);
    }

    /**
     * Update pie chart with event type distribution
     */
    private void updateEventTypeChart(Map<String, Integer> distribution) {
        List<PieChart.Data> pieData = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        eventTypeChart.setData(FXCollections.observableArrayList(pieData));
        eventTypeChart.setTitle("Sessions by Event Type");
    }

    /**
     * Update bar chart with session engagement trends
     */
    private void updateEngagementChart(Map<String, Integer> trends) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendees");

        for (Map.Entry<String, Integer> entry : trends.entrySet()) {
            String title = entry.getKey();
            // Truncate long titles
            if (title.length() > 20) {
                title = title.substring(0, 17) + "...";
            }
            series.getData().add(new XYChart.Data<>(title, entry.getValue()));
        }

        engagementChart.getData().clear();
        engagementChart.getData().add(series);
        engagementChart.setTitle("Top Sessions by Attendance");
    }

    /**
     * Export statistics to CSV
     */
    @FXML
    public void onExportStatistics() {
        if (statsService == null) {
            showAlert("Error", "Statistics service not available");
            return;
        }

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Presenter Statistics");
        fileChooser.setInitialFileName(FxUtils.generateFileName("presenter_stats_" + presenterName, "csv"));
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        javafx.stage.Stage stage = (javafx.stage.Stage) contentBox.getScene().getWindow();
        java.io.File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            AsyncTaskService.runAsync(
                    () -> {
                        PresenterStatistics stats = statsService.generateStatistics(presenterId);

                        List<String[]> data = new ArrayList<>();

                        // Summary data
                        data.add(new String[]{"Metric", "Value"});
                        data.add(new String[]{"Total Sessions", String.valueOf(stats.totalSessions)});
                        data.add(new String[]{"Total Attendees", String.valueOf(stats.totalAttendees)});
                        data.add(new String[]{"Average Audience Size", String.valueOf(stats.averageAudienceSize)});
                        data.add(new String[]{"Upcoming Sessions", String.valueOf(stats.upcomingSessions)});
                        data.add(new String[]{"Completed Sessions", String.valueOf(stats.completedSessions)});
                        data.add(new String[]{"", ""});

                        // Event type distribution
                        data.add(new String[]{"Event Type", "Session Count"});
                        for (Map.Entry<String, Integer> entry : stats.eventTypeDistribution.entrySet()) {
                            data.add(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
                        }
                        data.add(new String[]{"", ""});

                        // Engagement trends
                        data.add(new String[]{"Session Title", "Attendee Count"});
                        for (Map.Entry<String, Integer> entry : stats.sessionEngagementTrends.entrySet()) {
                            data.add(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
                        }

                        return FxUtils.exportToCSV(file.getAbsolutePath(), data, new String[]{"Field", "Value"});
                    },
                    success -> {
                        if (success) {
                            showAlert("Success", "Statistics exported to:\n" + file.getAbsolutePath());
                        } else {
                            showAlert("Error", "Failed to export statistics");
                        }
                    },
                    error -> showAlert("Error", "Export failed: " + error.getMessage())
            );
        }
    }

    /**
     * Refresh statistics
     */
    @FXML
    public void onRefresh() {
        loadStatisticsAsync();
    }

    /**
     * Navigate back to dashboard
     */
    @FXML
    public void onBack() {
        SceneManager.switchTo("dashboard.fxml", "Event Manager System - Dashboard");
    }

    private void showLoading() {
        Platform.runLater(() -> {
            if (loadingBox != null) loadingBox.setVisible(true);
            if (contentBox != null) contentBox.setVisible(false);
        });
    }

    private void hideLoading() {
        Platform.runLater(() -> {
            if (loadingBox != null) loadingBox.setVisible(false);
            if (contentBox != null) contentBox.setVisible(true);
        });
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}