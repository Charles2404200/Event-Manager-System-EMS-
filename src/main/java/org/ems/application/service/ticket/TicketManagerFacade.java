package org.ems.application.service.ticket;

import org.ems.infrastructure.config.AppContext;
import org.ems.domain.repository.*;

/**
 * TicketManagerFacade - Composite service providing single entry point for ticket management
 * Manages all service dependencies and lifecycle
 *
 * @author EMS Team
 */
public class TicketManagerFacade {

    // Core Services
    private final TicketCacheManager cacheManager;
    private final TicketPaginationService paginationService;
    private final TicketTemplateService templateService;
    private final TicketAssignmentService assignmentService;
    private final TicketStatisticsService statisticsService;
    private final TicketDataLoaderService dataLoaderService;
    private final TicketTableUIService tableUIService;

    // New UI Services
    private final TicketValidationService validationService;
    private final TicketDataConverterService dataConverterService;
    private final TicketUIStatisticsService uiStatisticsService;
    private final TicketUIRenderingService uiRenderingService;
    private final TicketEntityLoaderService entityLoaderService;

    // Repositories
    private final TicketRepository ticketRepo;
    private final EventRepository eventRepo;
    private final SessionRepository sessionRepo;
    private final AttendeeRepository attendeeRepo;

    /**
     * Initialize all services with repositories from AppContext
     */
    public TicketManagerFacade() {
        AppContext ctx = AppContext.get();

        this.ticketRepo = ctx.ticketRepo;
        this.eventRepo = ctx.eventRepo;
        this.sessionRepo = ctx.sessionRepo;
        this.attendeeRepo = ctx.attendeeRepo;

        // Initialize core services
        this.cacheManager = new TicketCacheManager();
        this.paginationService = new TicketPaginationService();
        this.templateService = new TicketTemplateService(ticketRepo, eventRepo, sessionRepo, cacheManager);
        this.assignmentService = new TicketAssignmentService(ticketRepo, attendeeRepo, cacheManager, paginationService);
        this.statisticsService = new TicketStatisticsService(ticketRepo, cacheManager);
        this.dataLoaderService = new TicketDataLoaderService(eventRepo, sessionRepo, attendeeRepo, cacheManager);
        this.tableUIService = new TicketTableUIService();

        // Initialize new UI services
        this.validationService = new TicketValidationService();
        this.dataConverterService = new TicketDataConverterService(cacheManager);
        this.uiStatisticsService = new TicketUIStatisticsService();
        this.uiRenderingService = new TicketUIRenderingService();
        this.entityLoaderService = new TicketEntityLoaderService(cacheManager, eventRepo, attendeeRepo);

        System.out.println("✅ [TicketManagerFacade] Initialized successfully with all services");
    }

    // Getters for core services
    public TicketCacheManager getCacheManager() { return cacheManager; }
    public TicketPaginationService getPaginationService() { return paginationService; }
    public TicketTemplateService getTemplateService() { return templateService; }
    public TicketAssignmentService getAssignmentService() { return assignmentService; }
    public TicketStatisticsService getStatisticsService() { return statisticsService; }
    public TicketDataLoaderService getDataLoaderService() { return dataLoaderService; }
    public TicketTableUIService getTableUIService() { return tableUIService; }

    // Getters for new UI services
    public TicketValidationService getValidationService() { return validationService; }
    public TicketDataConverterService getDataConverterService() { return dataConverterService; }
    public TicketUIStatisticsService getUiStatisticsService() { return uiStatisticsService; }
    public TicketUIRenderingService getUiRenderingService() { return uiRenderingService; }
    public TicketEntityLoaderService getEntityLoaderService() { return entityLoaderService; }

    // Getters for repositories
    public TicketRepository getTicketRepo() { return ticketRepo; }
    public EventRepository getEventRepo() { return eventRepo; }
    public SessionRepository getSessionRepo() { return sessionRepo; }
    public AttendeeRepository getAttendeeRepo() { return attendeeRepo; }

    /**
     * Clear all caches and reset pagination
     */
    public void reset() {
        System.out.println("🔄 [TicketManagerFacade] Resetting...");
        cacheManager.clearAll();
        paginationService.resetTemplatePagination();
        paginationService.resetAssignedPagination();
    }
}

