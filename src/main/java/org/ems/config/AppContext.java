package org.ems.config;

import org.ems.application.impl.EventServiceImpl;
import org.ems.application.impl.IdentityServiceImpl;
import org.ems.application.impl.ImageServiceImpl;
import org.ems.application.impl.ReportingServiceImpl;
import org.ems.application.impl.ScheduleServiceImpl;
import org.ems.application.impl.TicketServiceImpl;

import org.ems.application.service.EventService;
import org.ems.application.service.IdentityService;
import org.ems.application.service.ImageService;
import org.ems.application.service.ReportingService;
import org.ems.application.service.ScheduleService;
import org.ems.application.service.TicketService;

import org.ems.infrastructure.repository.jdbc.*;

import org.ems.domain.repository.*;
import org.ems.domain.model.Person;

import java.sql.Connection;

/**
 * @author <your group number>
 */
public class AppContext {

    private static AppContext instance;

    public final Connection connection;

    // Repositories
    public final EventRepository eventRepo;
    public final SessionRepository sessionRepo;
    public final AttendeeRepository attendeeRepo;
    public final PresenterRepository presenterRepo;
    public final TicketRepository ticketRepo;
    public final UserRepository userRepo;
    public final ActivityLogRepository activityLogRepo;

    // Services
    public final IdentityService identityService;
    public final EventService eventService;
    public final TicketService ticketService;
    public final ScheduleService scheduleService;
    public final ReportingService reportingService;
    public final ImageService imageService;
    public Person currentUser = null;
    public String currentUserRole = "VISITOR"; // default

    // Navigation context - for passing data between pages
    public java.util.UUID selectedEventId = null;
    public java.util.UUID selectedTicketId = null;


    // ============================================================
    //  Singleton Access
    // ============================================================
    public static AppContext get() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    // ============================================================
    //  Constructor: wire all dependencies here
    // ============================================================
    private AppContext() {

        Connection tempConnection;
        try {
            // 1. Connect to database
            tempConnection = DatabaseConfig.getConnection();
            System.out.println(" Database connection established");
        } catch (Exception e) {
            System.err.println(" Warning: Failed to connect to database: " + e.getMessage());
            System.err.println("️ Application will run in offline mode");
            tempConnection = null;
        }

        this.connection = tempConnection;
        this.eventRepo     = connection != null ? new JdbcEventRepository(connection) : null;
        this.sessionRepo   = connection != null ? new JdbcSessionRepository(connection) : null;
        this.attendeeRepo  = connection != null ? new JdbcAttendeeRepository(connection) : null;
        this.presenterRepo = connection != null ? new JdbcPresenterRepository(connection) : null;
        this.ticketRepo    = connection != null ? new JdbcTicketRepository(connection) : null;
        this.userRepo      = null;
        this.activityLogRepo = connection != null ? new JdbcActivityLogRepository(connection) : null;
        System.out.println(" ActivityLogRepository initialized: " + (activityLogRepo != null ? "OK" : "NULL"));
        this.scheduleService = sessionRepo != null ? new ScheduleServiceImpl(sessionRepo) : null;

        this.eventService = eventRepo != null ? new EventServiceImpl(
                eventRepo,
                sessionRepo,
                attendeeRepo,
                presenterRepo,
                scheduleService,
                new ImageServiceImpl()
        ) : null;

        this.identityService = attendeeRepo != null ? new IdentityServiceImpl(
                attendeeRepo,
                presenterRepo
        ) : null;

        this.ticketService = ticketRepo != null ? new TicketServiceImpl(
                ticketRepo,
                sessionRepo,
                attendeeRepo,
                eventRepo,
                new ImageServiceImpl()
        ) : null;

        this.imageService = new ImageServiceImpl();

        this.reportingService = eventRepo != null ? new ReportingServiceImpl(
                eventRepo,
                sessionRepo,
                attendeeRepo,
                ticketRepo
        ) : null;

        System.out.println(" AppContext initialized successfully.");

        // Register shutdown hook for graceful connection pool closure
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(" Shutting down AppContext...");
            shutdown();
        }));
    }

    // ============================================================
    //  Shutdown
    // ============================================================
    /**
     * Gracefully shutdown the application and close connection pool
     */
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println(" Database connection closed");
            }
        } catch (Exception e) {
            System.err.println(" Error closing connection: " + e.getMessage());
        }

        // Close HikariCP connection pool
        DatabaseConfig.closePool();
        System.out.println(" AppContext shutdown completed");
    }

    // ============================================================
    //  Connection Pool Monitoring
    // ============================================================
    /**
     * Get current HikariCP connection pool statistics
     * @return Pool stats as formatted string
     */
    public String getPoolStats() {
        return DatabaseConfig.getPoolStats();
    }
}
