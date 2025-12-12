package org.ems.application.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.ems.application.dto.template.TemplateRow;
import org.ems.application.dto.ticket.TicketRow;
import org.ems.application.service.ticket.TicketManagerFacade;
import org.ems.domain.model.Ticket;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.TicketType;
import org.ems.infrastructure.util.ActivityLogger;
import org.ems.ui.stage.SceneManager;

import java.math.BigDecimal;
import java.util.*;

public class TicketManagerController {

    // ===== FXML BINDINGS - Tab 1: Ticket Templates =====
    @FXML private ComboBox<String> templateEventCombo;
    @FXML private ComboBox<String> templateTypeCombo;
    @FXML private TextField templatePriceField;
    @FXML private TableView<TemplateRow> templatesTable;
    @FXML private Label templatesCountLabel;
    @FXML private Label templatesPageLabel;

    // ===== FXML BINDINGS - Tab 2: Assign Tickets =====
    @FXML private ComboBox<String> assignAttendeeCombo;
    @FXML private ComboBox<String> assignTemplateCombo;
    @FXML private TableView<TicketRow> assignedTicketsTable;
    @FXML private Label assignedCountLabel;
    @FXML private Label totalAssignedLabel;
    @FXML private Label activeAssignedLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label assignedPageLabel;
    @FXML private Button assignedPrevButton;
    @FXML private Button assignedNextButton;
    @FXML private HBox assignedPaginationBox;
    @FXML private Label assignedPageInfoLabel;
    @FXML private ProgressBar assignedLoadingProgressBar;
    @FXML private Label assignedLoadingStatusLabel;
    @FXML private javafx.scene.layout.VBox assignedLoadingContainer;

    // ===== DEPENDENCY INJECTION - Services Facade =====
    private TicketManagerFacade facade;

    // ===== LOCAL STATE =====
    private List<TemplateRow> currentTemplateRows = new ArrayList<>();
    private List<TicketRow> currentTicketRows = new ArrayList<>();
    private boolean assignedTabInitialized = false;

    private static final int PAGE_SIZE = 20;

    /**
     * Initialize UI - setup all FXML bindings
     * Defers all data loading to Platform.runLater() to avoid blocking UI thread
     */
    @FXML
    public void initialize() {
        // Initialize services facade - single dependency
        this.facade = new TicketManagerFacade();

        // Setup UI components
        setupTemplateTypeCombo();
        setupUILabels();
        setupTableColumns();

        System.out.println("✅ [TicketManagerController] UI initialized in < 50ms");

        // Defer all database operations to after UI render
        Platform.runLater(this::loadDataAsync);
    }

    /**
     * Setup template type combobox options
     */
    private void setupTemplateTypeCombo() {
        templateTypeCombo.setItems(FXCollections.observableArrayList(
                "GENERAL", "VIP", "EARLY_BIRD", "STUDENT", "GROUP"
        ));
        templateTypeCombo.setValue("GENERAL");
    }

    /**
     * Initialize status labels
     */
    private void setupUILabels() {
        templatesPageLabel.setText("(Keyset Pagination)");
        assignedPageLabel.setText("(Keyset Pagination)");
        templatesCountLabel.setText("Loading...");
        assignedCountLabel.setText("Ready");
    }

    /**
     * Setup table column cell value factories
     */
    private void setupTableColumns() {
        var renderingService = facade.getUiRenderingService();
        renderingService.setupTemplateTableColumns(templatesTable);
        renderingService.setupAssignedTicketTableColumns(assignedTicketsTable);
    }

    /**
     * Run all database operations AFTER UI is rendered
     * Called from Platform.runLater() after initialize() returns
     */
    private void loadDataAsync() {
        System.out.println("🚀 [TicketManagerController] UI rendered. Starting async data load...");
        loadEventsAsync();
        loadTemplatesAsync();
    }

    /**
     * Load events asynchronously to populate templateEventCombo
     */
    private void loadEventsAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> eventList = new ArrayList<>();
                Map<String, UUID> eventMap = new HashMap<>();

                try {
                    var eventRepo = facade.getEventRepo();
                    if (eventRepo != null) {
                        var events = eventRepo.findAll();
                        for (var evt : events) {
                            String display = evt.getName() + " (" + evt.getId().toString().substring(0, 8) + ")";
                            eventList.add(display);
                            eventMap.put(display, evt.getId());
                            facade.getCacheManager().addEvent(evt);
                        }
                    }
                    System.out.println("[TicketManagerController] loadEventsAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + eventList.size() + " events");
                } catch (Exception e) {
                    System.err.println("[TicketManagerController] Error loading events: " + e.getMessage());
                }

                // Cache the event mappings
                for (var entry : eventMap.entrySet()) {
                    facade.getCacheManager().putEventMapping(entry.getKey(), entry.getValue());
                }

                return eventList;
            }
        };

        task.setOnSucceeded(evt -> {
            List<String> eventList = task.getValue();
            Platform.runLater(() -> {
                templateEventCombo.setItems(FXCollections.observableArrayList(eventList));
                if (!eventList.isEmpty()) {
                    templateEventCombo.setValue(eventList.get(0));
                }
            });
        });

        task.setOnFailed(evt -> {
            System.err.println("[TicketManagerController] Failed to load events: " + task.getException().getMessage());
        });

        new Thread(task, "events-loader").start();
    }

    /**
     * Load templates from repository and populate table
     */
    private void loadTemplatesAsync() {
        templatesCountLabel.setText("Loading templates...");

        Task<List<TemplateRow>> task = new Task<>() {
            @Override
            protected List<TemplateRow> call() {
                long start = System.currentTimeMillis();
                List<TemplateRow> rows = new ArrayList<>();

                try {
                    var ticketRepo = facade.getTicketRepo();

                    if (ticketRepo != null) {
                        var templates = ticketRepo.findTemplates();

                        // Preload all events for templates
                        if (!templates.isEmpty()) {
                            facade.getEntityLoaderService().preloadEventsForTickets(templates);
                        }

                        var converterService = facade.getDataConverterService();

                        for (var template : templates) {
                            // Convert to UI DTO (events are now cached)
                            var row = converterService.convertToTemplateRow(template, 0);
                            if (row != null) {
                                rows.add(row);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[TicketManagerController] Error loading templates: " + e.getMessage());
                }

                System.out.println("[TicketManagerController] loadTemplatesAsync took " +
                        (System.currentTimeMillis() - start) + " ms, rows=" + rows.size());
                return rows;
            }
        };

        task.setOnSucceeded(evt -> {
            currentTemplateRows = task.getValue();
            displayTemplates(currentTemplateRows);
        });

        task.setOnFailed(evt -> {
            System.err.println("[TicketManagerController] Failed to load templates: " + task.getException().getMessage());
            templatesCountLabel.setText("Error loading templates");
        });

        new Thread(task, "ticket-templates-loader").start();
    }

    /**
     * Event handler: Create new ticket template
     * Delegates validation and business logic to services
     */
    @FXML
    public void onCreateTemplate() {
        try {
            // Validate inputs via validation service
            var validationService = facade.getValidationService();
            var validation = validationService.validateTemplateCreation(
                    templateEventCombo.getValue(),
                    templatePriceField.getText(),
                    templateTypeCombo.getValue()
            );

            if (!validation.isValid()) {
                showAlert("Validation Error", validation.getMessage());
                return;
            }

            // Get selected values
            String eventDisplay = templateEventCombo.getValue();
            String priceStr = templatePriceField.getText();
            String typeStr = templateTypeCombo.getValue();

            // Map display name to UUID via cache
            UUID eventId = facade.getCacheManager().getEventMap().get(eventDisplay);
            if (eventId == null) {
                showAlert("Error", "Invalid event selected");
                return;
            }

            // Create ticket from validated inputs
            Ticket template = new Ticket();
            template.setId(UUID.randomUUID());
            template.setEventId(eventId);
            template.setType(TicketType.valueOf(typeStr));
            template.setPrice(new BigDecimal(priceStr.trim()));
            template.setTicketStatus(TicketStatus.ACTIVE);
            template.setPaymentStatus(PaymentStatus.UNPAID);

            // Save via repository
            var ticketRepo = facade.getTicketRepo();
            if (ticketRepo != null) {
                ticketRepo.save(template);
                ActivityLogger.getInstance().logCreate("Ticket Template",
                        "Created template: " + typeStr + " - $" + priceStr);
                showAlert("Success", "Ticket template created successfully!");
                templatePriceField.clear();

                // Refresh template list
                loadTemplatesAsync();
            }

        } catch (NumberFormatException ex) {
            showAlert("Error", "Invalid price format");
        } catch (Exception e) {
            System.err.println("[TicketManagerController] Template creation error: " + e.getMessage());
            showAlert("Error", "Failed to create template: " + e.getMessage());
        }
    }

    /**
     * Load attendees asynchronously - populate assignAttendeeCombo
     */
    private void loadAttendeesAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> attendeeList = new ArrayList<>();
                Map<String, UUID> attendeeMap = new HashMap<>();

                try {
                    var attendeeRepo = facade.getAttendeeRepo();
                    if (attendeeRepo != null) {
                        List<org.ems.domain.model.Attendee> attendees = attendeeRepo.findAll();
                        for (var att : attendees) {
                            String display = att.getFullName() + " (" + att.getId().toString().substring(0, 8) + ")";
                            attendeeList.add(display);
                            attendeeMap.put(display, att.getId());
                        }
                    }
                    System.out.println("[TicketManagerController] loadAttendeesAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + attendeeList.size() + " attendees");
                } catch (Exception e) {
                    System.err.println("[TicketManagerController] Error loading attendees: " + e.getMessage());
                }

                // Cache attendee mappings
                for (var entry : attendeeMap.entrySet()) {
                    facade.getCacheManager().putAttendeeMapping(entry.getKey(), entry.getValue());
                }

                return attendeeList;
            }
        };

        task.setOnSucceeded(event -> {
            List<String> attendeeList = task.getValue();
            Platform.runLater(() -> {
                assignAttendeeCombo.setItems(FXCollections.observableArrayList(attendeeList));
                if (!attendeeList.isEmpty()) {
                    assignAttendeeCombo.setValue(attendeeList.get(0));
                }
            });
        });

        new Thread(task, "async-attendees-loader").start();
    }

    /**
     * Load templates for assignment - populate assignTemplateCombo
     */
    private void loadTemplatesForAssignAsync() {
        Task<List<String>> task = new Task<>() {
            @Override
            protected List<String> call() {
                long start = System.currentTimeMillis();
                List<String> templateList = new ArrayList<>();
                Map<String, UUID> templateMap = new HashMap<>();

                try {
                    var ticketRepo = facade.getTicketRepo();
                    List<Ticket> templates = ticketRepo != null ? ticketRepo.findTemplates() : Collections.emptyList();

                    for (Ticket ticket : templates) {
                        UUID eventId = ticket.getEventId();

                        String eventName = "Unknown";
                        var evt = facade.getCacheManager().getEventFromCache(eventId);
                        if (evt != null) {
                            eventName = evt.getName();
                        }

                        String typeName = ticket.getType() != null ? ticket.getType().name() : "N/A";
                        String priceStr = ticket.getPrice() != null ? ticket.getPrice().toString() : "0";
                        String display = eventName + " (" + typeName + ": $" + priceStr + ")";

                        templateList.add(display);
                        templateMap.put(display, ticket.getId());
                    }

                    System.out.println("[TicketManagerController] loadTemplatesForAssignAsync completed in " +
                            (System.currentTimeMillis() - start) + " ms, loaded " + templateList.size() + " templates");
                } catch (Exception e) {
                    System.err.println("[TicketManagerController] Error loading templates for assign: " + e.getMessage());
                }

                // Cache template mappings
                for (var entry : templateMap.entrySet()) {
                    facade.getCacheManager().putTemplateMapping(entry.getKey(), entry.getValue());
                }

                return templateList;
            }
        };

        task.setOnSucceeded(event -> {
            List<String> templateList = task.getValue();
            Platform.runLater(() -> {
                assignTemplateCombo.setItems(FXCollections.observableArrayList(templateList));
                if (!templateList.isEmpty()) {
                    assignTemplateCombo.setValue(templateList.get(0));
                }
            });
        });

        new Thread(task, "async-templates-loader").start();
    }

    /**
     * Load assigned tickets with pagination
     */
    private void loadAssignedTickets() {
        assignedCountLabel.setText("Loading...");

        Task<List<TicketRow>> task = new Task<>() {
            @Override
            protected List<TicketRow> call() {
                long start = System.currentTimeMillis();
                List<TicketRow> rows = new ArrayList<>();

                try {
                    var ticketRepo = facade.getTicketRepo();

                    if (ticketRepo != null) {
                        var tickets = ticketRepo.findAssigned();

                        // Preload all events and attendees BEFORE converting
                        if (!tickets.isEmpty()) {
                            facade.getEntityLoaderService().preloadAllEntitiesForTickets(tickets);
                        }

                        var converterService = facade.getDataConverterService();

                        for (var ticket : tickets) {
                            // Convert to UI DTO (events & attendees now cached)
                            var row = converterService.convertToTicketRow(ticket);
                            if (row != null) {
                                rows.add(row);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[TicketManagerController] Error loading assigned tickets: " + e.getMessage());
                }

                System.out.println("[TicketManagerController] loadAssignedTickets took " +
                        (System.currentTimeMillis() - start) + " ms, rows=" + rows.size());
                return rows;
            }
        };

        task.setOnSucceeded(evt -> {
            currentTicketRows = task.getValue();
            displayAssignedTickets(currentTicketRows);
            calculateStatistics();

            // Load attendees and templates in background
            loadAttendeesAsync();
            loadTemplatesForAssignAsync();
        });

        task.setOnFailed(evt -> {
            System.err.println("[TicketManagerController] Failed to load assigned tickets: " + task.getException().getMessage());
            assignedCountLabel.setText("Error loading data");
        });

        new Thread(task, "assigned-tickets-loader").start();
    }

    /**
     * Lazy-load tab Assigned khi user mở lần đầu
     */
    public void onAssignedTabSelected() {
        if (!assignedTabInitialized) {
            assignedTabInitialized = true;
            loadAssignedTickets();
        }
    }

    /**
     * Display templates in table
     */
    private void displayTemplates(List<TemplateRow> templates) {
        ObservableList<TemplateRow> obs = FXCollections.observableArrayList(templates);
        templatesTable.setItems(obs);
        templatesCountLabel.setText("Total Templates: " + templates.size());
    }

    /**
     * Display assigned tickets in table
     */
    private void displayAssignedTickets(List<TicketRow> tickets) {
        ObservableList<TicketRow> obs = FXCollections.observableArrayList(tickets);
        assignedTicketsTable.setItems(obs);
        assignedCountLabel.setText("Total Assigned: " + tickets.size());
    }

    /**
     * Calculate and display statistics
     */
    private void calculateStatistics() {
        try {
            var statsService = facade.getUiStatisticsService();
            var stats = statsService.calculateStatistics(currentTicketRows);

            totalAssignedLabel.setText(String.valueOf(stats.total));
            activeAssignedLabel.setText(String.valueOf(stats.active));
            totalRevenueLabel.setText("$" + stats.revenue);

        } catch (Exception e) {
            System.err.println("Error calculating statistics: " + e.getMessage());
        }
    }

    /**
     * Event handler: Assign ticket to attendee
     */
    @FXML
    public void onAssignTicket() {
        try {
            // Validate inputs
            var validationService = facade.getValidationService();
            var validation = validationService.validateTicketAssignment(
                    assignAttendeeCombo.getValue(),
                    assignTemplateCombo.getValue()
            );

            if (!validation.isValid()) {
                showAlert("Error", validation.getMessage());
                return;
            }

            UUID attendeeId = facade.getCacheManager().getAttendeeMap().get(assignAttendeeCombo.getValue());
            if (attendeeId == null) {
                showAlert("Error", "Invalid attendee selected");
                return;
            }

            // Get template ID from cached map
            String selectedTemplate = assignTemplateCombo.getValue();
            UUID templateId = facade.getCacheManager().getTemplateMap().get(selectedTemplate);

            if (templateId == null) {
                showAlert("Error", "Template ticket not found");
                return;
            }

            // Get the template ticket
            Ticket templateTicket = facade.getTicketRepo().findById(templateId);
            if (templateTicket == null) {
                showAlert("Error", "Template ticket not found");
                return;
            }

            // Create new ticket from template
            Ticket newTicket = new Ticket();
            newTicket.setId(UUID.randomUUID());
            newTicket.setAttendeeId(attendeeId);
            newTicket.setEventId(templateTicket.getEventId());
            newTicket.setType(templateTicket.getType());
            newTicket.setPrice(templateTicket.getPrice());
            newTicket.setTicketStatus(TicketStatus.ACTIVE);
            newTicket.setPaymentStatus(PaymentStatus.PAID);

            if (facade.getTicketRepo() != null) {
                facade.getTicketRepo().save(newTicket);
                showAlert("Success", "Ticket assigned to attendee successfully!");

                // Refresh both tabs
                loadAssignedTickets();
                loadTemplatesAsync();
            }

        } catch (Exception e) {
            System.err.println("[TicketManagerController] Assign ticket error: " + e.getMessage());
            showAlert("Error", "Failed to assign ticket: " + e.getMessage());
        }
    }

    @FXML
    public void onBack() {
        SceneManager.switchTo("admin_dashboard.fxml", "Event Manager System - Admin Dashboard");
    }

    /**
     * Refresh templates
     */
    @FXML
    public void onTemplatesRefresh() {
        loadTemplatesAsync();
    }

    /**
     * Refresh assigned tickets
     */
    @FXML
    public void onAssignedRefresh() {
        loadAssignedTickets();
    }

    /**
     * Show alert dialog
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Load next page of assigned tickets
     */
    @FXML
    public void onAssignedNextPage() {
        // Placeholder for pagination - can be enhanced later
    }

    /**
     * Load previous page of assigned tickets
     */
    @FXML
    public void onAssignedPrevPage() {
        // Placeholder for pagination - can be enhanced later
    }
}
