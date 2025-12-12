package org.ems.application.service.ticket;

import javafx.concurrent.Task;
import org.ems.domain.repository.EventRepository;
import org.ems.domain.repository.SessionRepository;
import org.ems.domain.repository.AttendeeRepository;

import java.util.List;
import java.util.function.Consumer;

/**
 * TicketDataLoaderService - Handles asynchronous data loading
 * Single Responsibility: Load events, sessions, attendees with progress tracking
 *
 * @author EMS Team
 */
public class TicketDataLoaderService {

    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;
    private final TicketCacheManager cacheManager;

    public TicketDataLoaderService(EventRepository eventRepo, SessionRepository sessionRepo,
                                   AttendeeRepository attendeeRepo, TicketCacheManager cacheManager) {
        this.eventRepo = eventRepo;
        this.sessionRepo = sessionRepo;
        this.attendeeRepo = attendeeRepo;
        this.cacheManager = cacheManager;
    }

    /**
     * Load events asynchronously
     */
    public void loadEventsAsync(Consumer<List<String>> onSuccess, Consumer<Exception> onError) {
        long start = System.currentTimeMillis();
        System.out.println("📦 [TicketDataLoaderService] Loading events async...");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                try {
                    // TODO: Implement event loading from original controller
                    return List.of(); // Placeholder
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        task.setOnSucceeded(evt -> {
            System.out.println("  ✓ Events loaded in " + (System.currentTimeMillis() - start) + " ms");
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(evt -> {
            System.err.println("  ✗ Error loading events");
            Throwable exception = task.getException();
            Exception ex = (exception instanceof Exception) ? (Exception) exception : new Exception(exception);
            onError.accept(ex);
        });

        Thread t = new Thread(task, "ticket-events-loader");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Load attendees asynchronously
     */
    public void loadAttendeesAsync(Consumer<List<String>> onSuccess, Consumer<Exception> onError) {
        long start = System.currentTimeMillis();
        System.out.println("👥 [TicketDataLoaderService] Loading attendees async...");

        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                try {
                    // TODO: Implement attendee loading from original controller
                    return List.of(); // Placeholder
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        task.setOnSucceeded(evt -> {
            System.out.println("  ✓ Attendees loaded in " + (System.currentTimeMillis() - start) + " ms");
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(evt -> {
            System.err.println("  ✗ Error loading attendees");
            Throwable exception = task.getException();
            Exception ex = (exception instanceof Exception) ? (Exception) exception : new Exception(exception);
            onError.accept(ex);
        });

        Thread t = new Thread(task, "ticket-attendees-loader");
        t.setDaemon(true);
        t.start();
    }
}

