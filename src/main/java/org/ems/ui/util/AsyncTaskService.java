package org.ems.ui.util;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility class for running tasks on background threads without blocking JavaFX UI thread
 *
 * Usage:
 *   AsyncTaskService.runAsync(() -> {
 *       return expensiveOperation();  // Runs on background thread
 *   }, result -> {
 *       updateUI(result);  // Runs on UI thread
 *   });
 */
public class AsyncTaskService {

    /**
     * Run a task on background thread with UI thread callback
     *
     * @param backgroundTask Task to run on background thread
     * @param uiCallback Callback to execute on UI thread with result
     * @param <T> Result type
     */
    public static <T> void runAsync(Supplier<T> backgroundTask, Consumer<T> uiCallback) {
        runAsync(backgroundTask, uiCallback, null);
    }

    /**
     * Run a task on background thread with UI callbacks
     *
     * @param backgroundTask Task to run on background thread
     * @param successCallback Called on success with result
     * @param errorCallback Called on error with exception
     * @param <T> Result type
     */
    public static <T> void runAsync(
            Supplier<T> backgroundTask,
            Consumer<T> successCallback,
            Consumer<Throwable> errorCallback) {

        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                updateMessage("Processing...");
                updateProgress(-1, -1);  // Indeterminate progress
                return backgroundTask.get();
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Platform.runLater(() -> {
                    try {
                        if (successCallback != null) {
                            successCallback.accept(getValue());
                        }
                    } catch (Exception e) {
                        System.err.println("Error in success callback: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }

            @Override
            protected void failed() {
                super.failed();
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    System.err.println("Background task failed: " + exception.getMessage());
                    exception.printStackTrace();

                    if (errorCallback != null) {
                        errorCallback.accept(exception);
                    } else {
                        // Show default error dialog
                        showErrorAlert("Operation Failed", exception.getMessage());
                    }
                });
            }
        };

        // Run on background thread
        new Thread(task).start();
    }

    /**
     * Run a task on background thread without callback (fire and forget)
     *
     * @param backgroundTask Task to run on background thread
     */
    public static void runAsync(Runnable backgroundTask) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                backgroundTask.run();
                return null;
            }

            @Override
            protected void failed() {
                super.failed();
                Throwable exception = getException();
                System.err.println("Background task failed: " + exception.getMessage());
                exception.printStackTrace();
            }
        };

        new Thread(task).start();
    }

    /**
     * Run a task with progress tracking
     *
     * @param backgroundTask Task that returns progress info
     * @param progressCallback Callback for progress updates (0.0 - 1.0)
     * @param completeCallback Callback when complete
     * @param <T> Result type
     */
    public static <T> void runAsyncWithProgress(
            TaskWithProgress<T> backgroundTask,
            Consumer<Double> progressCallback,
            Consumer<T> completeCallback) {

        Task<T> task = new Task<T>() {
            @Override
            protected T call() throws Exception {
                return backgroundTask.execute(this::updateProgress);
            }

            @Override
            protected void succeeded() {
                super.succeeded();
                Platform.runLater(() -> {
                    if (completeCallback != null) {
                        completeCallback.accept(getValue());
                    }
                });
            }

            @Override
            protected void failed() {
                super.failed();
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    System.err.println("Background task failed: " + exception.getMessage());
                    showErrorAlert("Operation Failed", exception.getMessage());
                });
            }
        };

        // Listen to progress updates
        task.progressProperty().addListener((obs, oldVal, newVal) -> {
            if (progressCallback != null) {
                progressCallback.accept(newVal.doubleValue());
            }
        });

        new Thread(task).start();
    }

    /**
     * Show error alert dialog
     */
    private static void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Functional interface for tasks with progress reporting
     */
    @FunctionalInterface
    public interface TaskWithProgress<T> {
        T execute(ProgressReporter reporter) throws Exception;
    }

    /**
     * Interface for reporting progress
     */
    @FunctionalInterface
    public interface ProgressReporter {
        void updateProgress(long workDone, long max);
    }
}

