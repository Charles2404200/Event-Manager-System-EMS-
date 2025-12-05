package org.ems.ui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.concurrent.Task;
import org.ems.config.AppContext;
import org.ems.domain.model.Person;
import org.ems.domain.model.Presenter;
import org.ems.domain.dto.PresenterStatisticsDTO;
import org.ems.ui.stage.SceneManager;

import java.util.Map;

/**
 * @author <your group number>
 */
public class PresenterStatisticsController {

    @FXML private Label presenterNameLabel;
    @FXML private Label totalSessionsLabel;
    @FXML private Label totalAudienceLabel;
    @FXML private Label averageAudienceLabel;
    @FXML private Label upcomingSessionsLabel;
    @FXML private Label completedSessionsLabel;

    @FXML private VBox eventTypeBox;
    @FXML private VBox sessionEngagementBox;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private VBox contentBox;

    private AppContext appContext;
    private Presenter currentPresenter;

    @FXML
    public void initialize() {
        appContext = AppContext.get();

        // Get current presenter
        Person user = appContext.currentUser;
        if (user instanceof Presenter) {
            currentPresenter = (Presenter) user;
            presenterNameLabel.setText("Statistics for: " + currentPresenter.getFullName());
            loadStatistics();
        } else {
            showAlert("Error", "Current user is not a presenter");
        }
    }

    private void loadStatistics() {
        // Show loading indicator
        loadingIndicator.setVisible(true);
        contentBox.setVisible(false);

        Task<PresenterStatisticsDTO> task = new Task<>() {
            @Override
            protected PresenterStatisticsDTO call() throws Exception {
                return appContext.identityService.getPresenterStatistics(currentPresenter.getId());
            }
        };

        task.setOnSucceeded(event -> {
            PresenterStatisticsDTO stats = task.getValue();
            displayStatistics(stats);
            loadingIndicator.setVisible(false);
            contentBox.setVisible(true);
        });

        task.setOnFailed(event -> {
            System.err.println("Failed to load statistics: " + task.getException().getMessage());
            showAlert("Error", "Failed to load statistics");
            loadingIndicator.setVisible(false);
        });

        new Thread(task).start();
    }

    private void displayStatistics(PresenterStatisticsDTO stats) {
        // Display basic stats
        totalSessionsLabel.setText(String.valueOf(stats.getTotalSessions()));
        totalAudienceLabel.setText(String.valueOf(stats.getTotalAudience()));
        averageAudienceLabel.setText(String.format("%.1f", stats.getAverageAudiencePerSession()));
        upcomingSessionsLabel.setText(String.valueOf(stats.getUpcomingSessions()));
        completedSessionsLabel.setText(String.valueOf(stats.getCompletedSessions()));

        // Display event type distribution
        eventTypeBox.getChildren().clear();
        if (stats.getEventTypeDistribution() != null && !stats.getEventTypeDistribution().isEmpty()) {
            for (Map.Entry<String, Integer> entry : stats.getEventTypeDistribution().entrySet()) {
                HBox typeRow = createStatRow(entry.getKey(), String.valueOf(entry.getValue()));
                eventTypeBox.getChildren().add(typeRow);
            }
        } else {
            Label noDataLabel = new Label("No event type data available");
            noDataLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
            eventTypeBox.getChildren().add(noDataLabel);
        }

        // Display session engagement
        sessionEngagementBox.getChildren().clear();
        if (stats.getSessionEngagement() != null && !stats.getSessionEngagement().isEmpty()) {
            // Sort by engagement (descending)
            stats.getSessionEngagement().entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(10) // Show top 10
                    .forEach(entry -> {
                        HBox engagementRow = createStatRow(entry.getKey(), entry.getValue() + " attendees");
                        sessionEngagementBox.getChildren().add(engagementRow);
                    });
        } else {
            Label noDataLabel = new Label("No session engagement data available");
            noDataLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #999;");
            sessionEngagementBox.getChildren().add(noDataLabel);
        }
    }

    private HBox createStatRow(String label, String value) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: #f5f5f5; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #333;");
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #3498db;");

        row.getChildren().addAll(nameLabel, valueLabel);
        return row;
    }

    @FXML
    public void onRefresh() {
        loadStatistics();
    }

    @FXML
    public void onExport() {
        // TODO: Implement export functionality
        showAlert("Export", "Export functionality will be implemented soon");
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
}