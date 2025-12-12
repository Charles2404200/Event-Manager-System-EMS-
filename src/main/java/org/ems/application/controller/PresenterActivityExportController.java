package org.ems.application.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.ems.infrastructure.config.AppContext;
import org.ems.domain.model.Event;
import org.ems.domain.model.Presenter;
import org.ems.domain.model.Session;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.ui.stage.SceneManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PresenterActivityExportController {

    @FXML
    private Label presenterInfoLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private VBox exportLoadingContainer;

    @FXML
    private ProgressBar exportProgressBar;

    @FXML
    private Label progressLabel;

    private final AppContext appContext = AppContext.get();
    private Presenter presenter;
    private SessionRepository sessionRepo;
    private EventRepository eventRepo;

    private static final int PAGE_SIZE = 100;

    @FXML
    public void initialize() {
        // take current user
        if (!(appContext.currentUser instanceof Presenter)) {
            presenterInfoLabel.setText("Error: current user is not a presenter.");
            statusLabel.setText("Cannot export activity summary.");
            hideLoadingBox();
            return;
        }

        presenter = (Presenter) appContext.currentUser;
        sessionRepo = appContext.sessionRepo;
        eventRepo = appContext.eventRepo;

        if (sessionRepo == null || eventRepo == null) {
            presenterInfoLabel.setText("Repositories are not available.");
            statusLabel.setText("Please contact system administrator.");
            hideLoadingBox();
            return;
        }

        presenterInfoLabel.setText(
                "Presenter: " + presenter.getFullName() +
                        " (" + presenter.getEmail() + ")"
        );
        statusLabel.setText("Ready to export your activity summary.");

        // Ẩn progress khi mới mở màn
        hideLoadingBox();
    }

    // ================== UI helpers cho loading box ==================

    private void hideLoadingBox() {
        if (exportLoadingContainer != null) {
            exportLoadingContainer.setVisible(false);
        }
        if (exportProgressBar != null) {
            exportProgressBar.setVisible(false);
            exportProgressBar.setProgress(0.0);
        }
        if (progressLabel != null) {
            progressLabel.setVisible(false);
            progressLabel.setText("");
        }
    }

    private void showLoadingBox() {
        if (exportLoadingContainer != null) {
            exportLoadingContainer.setVisible(true);
        }
        if (exportProgressBar != null) {
            exportProgressBar.setVisible(true);
        }
        if (progressLabel != null) {
            progressLabel.setVisible(true);
        }
    }

    // ================== Export ==================

    @FXML
    private void onExportSummary() {

        if (presenter == null || sessionRepo == null || eventRepo == null) {
            showError("Export Failed", "Presenter or repositories not available.");
            return;
        }

        // select csv
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Presenter Activity Summary");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File chosen = chooser.showSaveDialog(presenterInfoLabel.getScene().getWindow());
        if (chosen == null) {
            return; // user cancel
        }
        final File file = chosen;

        statusLabel.setText("Exporting... please wait.");

        showLoadingBox();
        exportProgressBar.setProgress(0.1);
        progressLabel.setText("Preparing export... 10%");

        final UUID presenterId = presenter.getId();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                // 1) load all sessions
                updateProgress(0.15, 1.0);
                updateMessage("Loading sessions... 15%");

                List<Session> allSessions = new ArrayList<>();
                int pageIndex = 0;

                while (true) {
                    int offset = pageIndex * PAGE_SIZE;
                    List<Session> batch =
                            sessionRepo.findByPresenter(presenterId, offset, PAGE_SIZE);

                    if (batch == null || batch.isEmpty()) break;
                    allSessions.addAll(batch);

                    if (batch.size() < PAGE_SIZE) break;
                    pageIndex++;
                }

                if (allSessions.isEmpty()) {
                    updateProgress(1.0, 1.0);
                    updateMessage("No sessions found. 100%");
                    return null;
                }

                // 2) Cache events
                updateProgress(0.30, 1.0);
                updateMessage("Loading events... 30%");

                Map<UUID, Event> eventMap = new HashMap<>();
                for (Event ev : eventRepo.findAll()) {
                    eventMap.put(ev.getId(), ev);
                }

                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                // 3) write to csv
                updateProgress(0.45, 1.0);
                updateMessage("Writing CSV... 45%");

                int total = allSessions.size();
                int done = 0;

                try (PrintWriter out = new PrintWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(file),
                                StandardCharsets.UTF_8
                        )
                )) {
                    out.println("Event,Session,Start Time,End Time,Venue,Capacity");

                    for (Session s : allSessions) {
                        Event ev = s.getEventId() != null
                                ? eventMap.get(s.getEventId())
                                : null;

                        String eventName = ev != null ? ev.getName() : "";
                        String venue = s.getVenue() != null
                                ? s.getVenue()
                                : (ev != null ? ev.getLocation() : "");
                        String start = s.getStart() != null
                                ? s.getStart().format(formatter)
                                : "";
                        String end = s.getEnd() != null
                                ? s.getEnd().format(formatter)
                                : "";
                        String capacity = String.valueOf(s.getCapacity());

                        out.printf("%s,%s,%s,%s,%s,%s%n",
                                escapeCsv(eventName),
                                escapeCsv(s.getTitle()),
                                escapeCsv(start),
                                escapeCsv(end),
                                escapeCsv(venue),
                                escapeCsv(capacity)
                        );

                        done++;
                        // track progress
                        double progress = 0.45 + 0.54 * ((double) done / total);
                        if (progress > 0.99) progress = 0.99;
                        updateProgress(progress, 1.0);
                        updateMessage("Writing CSV... " + (int)(progress * 100) + "%");
                    }
                }


                updateProgress(1.0, 1.0);
                updateMessage("Export completed. 100%");

                return null;
            }
        };

        // Giống TicketManager: dùng listener để cập nhật UI
        task.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (progressLabel != null) {
                Platform.runLater(() -> progressLabel.setText(newVal));
            }
        });

        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (exportProgressBar != null) {
                Platform.runLater(() -> exportProgressBar.setProgress(newVal.doubleValue()));
            }
        });

        task.setOnSucceeded(e -> {
            statusLabel.setText("Export successful: " + file.getName());
            showInfo("Export Successful",
                    "Your presenter activity summary has been exported.");


            hideLoadingBox();
        });

        task.setOnFailed(e -> {
            statusLabel.setText("Export failed.");
            Throwable ex = task.getException();
            showError("Export Failed",
                    ex != null ? ex.getMessage() : "Unknown error");

            hideLoadingBox();
        });

        Thread t = new Thread(task, "presenter-activity-export");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onBack() {
        SceneManager.switchTo("dashboard.fxml", "EMS - Dashboard");
    }

    // ================== Helpers ==================

    private String escapeCsv(String value) {
        if (value == null) return "";
        String result = value.replace("\"", "\"\"");
        boolean needQuotes = result.contains(",")
                || result.contains("\"")
                || result.contains("\n");
        return needQuotes ? "\"" + result + "\"" : result;
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
