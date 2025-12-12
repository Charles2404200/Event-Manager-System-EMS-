package org.ems.ui.util;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import org.ems.domain.model.Event;
import org.ems.domain.model.Session;
import org.ems.application.service.session.SessionManagementService;
import org.ems.application.service.session.SessionEventManager;
import org.ems.application.service.session.SessionPresenterManager;

import java.util.List;
import java.util.function.Consumer;

/**
 * SessionAsyncLoader - Handles asynchronous loading of sessions, events, and presenters
 * Single Responsibility: Manage async operations with progress tracking for UI
 *
 * @author EMS Team
 */
public class SessionAsyncLoader {

    private final SessionManagementService sessionService;
    private final SessionEventManager eventManager;
    private final SessionPresenterManager presenterManager;

    // UI components for progress tracking
    private ProgressBar loadingProgressBar;
    private Label loadingStatusLabel;
    private VBox loadingContainer;

    /**
     * Constructor with dependency injection
     */
    public SessionAsyncLoader(SessionManagementService sessionService,
                             SessionEventManager eventManager,
                             SessionPresenterManager presenterManager) {
        this.sessionService = sessionService;
        this.eventManager = eventManager;
        this.presenterManager = presenterManager;
    }

    /**
     * Set UI components for progress tracking
     */
    public void setProgressUI(VBox loadingContainer, ProgressBar progressBar, Label statusLabel) {
        this.loadingContainer = loadingContainer;
        this.loadingProgressBar = progressBar;
        this.loadingStatusLabel = statusLabel;
    }

    /**
     * Load sessions asynchronously
     */
    public void loadSessionsAsync(Consumer<List<Session>> onSuccess, Consumer<Exception> onError) {
        long mainStart = System.currentTimeMillis();
        System.out.println("📋 [SessionAsyncLoader] Starting async session load...");

        showLoadingProgress();

        Task<List<Session>> task = new Task<>() {
            @Override
            protected List<Session> call() {
                long dbStart = System.currentTimeMillis();
                try {
                    List<Session> sessions = sessionService.getAllSessions();

                    long elapsed = System.currentTimeMillis() - dbStart;
                    System.out.println("  ✓ Sessions loaded in " + elapsed + " ms");

                    updateProgress(0.7, 1.0);
                    return sessions;
                } catch (Exception e) {
                    System.err.println("✗ Error loading sessions: " + e.getMessage());
                    updateProgress(0.0, 1.0);
                    throw new RuntimeException(e);
                }
            }
        };

        setupProgressTracking(task);

        task.setOnSucceeded(evt -> {
            List<Session> sessions = task.getValue();
            System.out.println("✓ Sessions loaded in " + (System.currentTimeMillis() - mainStart) + " ms");
            hideLoadingProgress();
            onSuccess.accept(sessions);
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            System.err.println("✗ Failed to load sessions");
            hideLoadingProgress();
            onError.accept(new RuntimeException(ex));
        });

        Thread t = new Thread(task, "session-async-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Load events asynchronously
     */
    public void loadEventsAsync(Consumer<List<Event>> onSuccess, Consumer<Exception> onError) {
        long mainStart = System.currentTimeMillis();
        System.out.println("📦 [SessionAsyncLoader] Starting async event load...");

        showLoadingProgress();

        Task<List<Event>> task = new Task<>() {
            @Override
            protected List<Event> call() {
                long dbStart = System.currentTimeMillis();
                try {
                    List<Event> events = eventManager.getAllEvents();

                    long elapsed = System.currentTimeMillis() - dbStart;
                    System.out.println("  ✓ Events loaded in " + elapsed + " ms");

                    updateProgress(0.95, 1.0);
                    return events;
                } catch (Exception e) {
                    System.err.println("✗ Error loading events: " + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        };

        setupProgressTracking(task);

        task.setOnSucceeded(evt -> {
            List<Event> events = task.getValue();
            System.out.println("✓ Events loaded in " + (System.currentTimeMillis() - mainStart) + " ms");
            hideLoadingProgress();
            onSuccess.accept(events);
        });

        task.setOnFailed(evt -> {
            System.err.println("✗ Failed to load events");
            hideLoadingProgress();
            onError.accept(new RuntimeException(task.getException()));
        });

        Thread t = new Thread(task, "event-async-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Pre-load presenters asynchronously (background task during init)
     */
    public void preloadPresentersAsync(Runnable onComplete) {
        long preloadStart = System.currentTimeMillis();
        System.out.println("👥 [SessionAsyncLoader] Pre-loading presenters in background...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    presenterManager.getAllPresentersAsMap();
                    System.out.println("  ✓ Presenters pre-loaded in " +
                            (System.currentTimeMillis() - preloadStart) + " ms");
                } catch (Exception e) {
                    System.err.println("✗ Error pre-loading presenters: " + e.getMessage());
                }
                return null;
            }
        };

        task.setOnSucceeded(evt -> onComplete.run());
        task.setOnFailed(evt -> {
            System.err.println("✗ Failed to pre-load presenters");
            onComplete.run();
        });

        Thread t = new Thread(task, "presenter-preloader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Setup progress tracking for task
     */
    private void setupProgressTracking(Task<?> task) {
        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (loadingProgressBar != null) {
                loadingProgressBar.setProgress(newVal.doubleValue());
            }
            if (loadingStatusLabel != null) {
                int percent = (int) (newVal.doubleValue() * 100);
                loadingStatusLabel.setText("Loading... " + percent + "%");
            }
        });
    }

    /**
     * Show loading progress UI
     */
    private void showLoadingProgress() {
        if (loadingContainer != null) {
            loadingContainer.setVisible(true);
        }
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisible(true);
            loadingProgressBar.setProgress(0.1);
        }
        if (loadingStatusLabel != null) {
            loadingStatusLabel.setVisible(true);
            loadingStatusLabel.setText("Loading... 0%");
        }
    }

    /**
     * Hide loading progress UI
     */
    public void hideLoadingProgress() {
        if (loadingContainer != null) {
            loadingContainer.setVisible(false);
        }
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisible(false);
            loadingProgressBar.setProgress(0.0);
        }
        if (loadingStatusLabel != null) {
            loadingStatusLabel.setVisible(false);
        }
    }
}

