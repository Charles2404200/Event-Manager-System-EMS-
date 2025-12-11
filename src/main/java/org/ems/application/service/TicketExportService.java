package org.ems.application.service;

import org.ems.application.dto.TicketDisplayDTO;
import java.util.List;

/**
 * Service for exporting tickets
 * Implements Single Responsibility Principle - only handles export logic
 * @author <your group number>
 */
public class TicketExportService {

    /**
     * Generate CSV content from tickets
     * @param tickets Tickets to export
     * @return CSV content as string
     */
    public String generateCSV(List<TicketDisplayDTO> tickets) {
        long start = System.currentTimeMillis();
        System.out.println("📤 [TicketExportService] Generating CSV...");

        if (tickets == null || tickets.isEmpty()) {
            System.out.println("  ℹ No tickets to export");
            return "";
        }

        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Ticket ID,Event,Session,Type,Price,Status,Purchase Date,QR Code\n");

            for (TicketDisplayDTO ticket : tickets) {
                csv.append(escapeCSV(ticket.getTicketId())).append(",")
                   .append(escapeCSV(ticket.getEventName())).append(",")
                   .append(escapeCSV(ticket.getSessionName())).append(",")
                   .append(escapeCSV(ticket.getType())).append(",")
                   .append(escapeCSV(ticket.getPrice())).append(",")
                   .append(escapeCSV(ticket.getStatus())).append(",")
                   .append(escapeCSV(ticket.getPurchaseDate())).append(",")
                   .append(escapeCSV(ticket.getQrCode())).append("\n");
            }

            System.out.println("  ✓ CSV generated in " + (System.currentTimeMillis() - start) + "ms: " + tickets.size() + " rows");
            return csv.toString();

        } catch (Exception e) {
            System.err.println("  ✗ Error generating CSV: " + e.getMessage());
            return "";
        }
    }

    /**
     * Escape CSV field (handle quotes and commas)
     * @param field Field to escape
     * @return Escaped field
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }

        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    /**
     * Generate filename for export
     * @return Filename with timestamp
     */
    public String generateFilename() {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return "MyTickets_" + timestamp + ".csv";
    }
}

