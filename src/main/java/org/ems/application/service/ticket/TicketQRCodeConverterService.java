package org.ems.application.service.ticket;

import org.ems.domain.model.Ticket;
import org.ems.application.dto.ticket.TicketDisplayDTO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for converting QR code data for display
 * Converts DB QR code format (qr-12345) to unique QR codes for display
 * Implements Single Responsibility Principle - only handles QR code conversion
 * Implements Caching to avoid regenerating same QR codes
 *
 * @author <your group number>
 */
public class TicketQRCodeConverterService {

    private final Map<String, String> qrCodeCache = new HashMap<>();
    private final int MAX_CACHE_SIZE = 10000;

    /**
     * Convert ticket QR code data from DB format to display format
     * DB stores: "qr-12345" or similar numeric format
     * Display shows: Full unique QR code
     *
     * @param ticket The ticket with DB QR code data
     * @return Converted QR code data for display
     */
    public String convertQRCodeForDisplay(Ticket ticket) {
        if (ticket == null) {
            System.out.println("⚠️ [QRCodeConverter] Ticket is null");
            return "N/A";
        }

        String dbQRCode = ticket.getQrCodeData();
        return convertQRCodeForDisplay(dbQRCode, ticket.getId().toString(), ticket.getAttendeeId().toString());
    }

    /**
     * Convert QR code data from DB format to display format
     *
     * @param dbQRCode The QR code stored in DB (format: "qr-12345")
     * @param ticketId Ticket ID for uniqueness
     * @param attendeeId Attendee ID for uniqueness
     * @return Converted unique QR code for display
     */
    public String convertQRCodeForDisplay(String dbQRCode, String ticketId, String attendeeId) {
        System.out.println("🔄 [QRCodeConverter] Converting QR code for display");
        System.out.println("   Input (DB): " + dbQRCode);

        // If no QR code in DB, return N/A
        if (dbQRCode == null || dbQRCode.isEmpty()) {
            System.out.println("   ✓ No QR code data in DB, returning N/A");
            return "N/A";
        }

        // Check cache first
        String cacheKey = dbQRCode + "|" + ticketId;
        if (qrCodeCache.containsKey(cacheKey)) {
            System.out.println("   ✓ Found in cache, returning cached QR code");
            return qrCodeCache.get(cacheKey);
        }

        // Convert DB format to unique QR code
        String uniqueQRCode = generateUniqueQRFromDBData(dbQRCode, ticketId, attendeeId);

        // Store in cache
        if (qrCodeCache.size() >= MAX_CACHE_SIZE) {
            System.out.println("   ℹ Cache full, clearing old entries");
            qrCodeCache.clear();
        }
        qrCodeCache.put(cacheKey, uniqueQRCode);

        System.out.println("   ✓ Converted QR code: " + uniqueQRCode.substring(0, Math.min(50, uniqueQRCode.length())) + "...");
        return uniqueQRCode;
    }

    /**
     * Generate unique QR code from DB data
     * Takes simple DB format (qr-12345) and creates full unique QR code
     *
     * @param dbQRCode DB format QR code (e.g., "qr-12345")
     * @param ticketId Ticket ID for uniqueness
     * @param attendeeId Attendee ID for uniqueness
     * @return Unique QR code string
     */
    private String generateUniqueQRFromDBData(String dbQRCode, String ticketId, String attendeeId) {
        // Extract number from DB format (e.g., "12345" from "qr-12345")
        String qrNumber = extractNumberFromDBFormat(dbQRCode);

        // Create unique payload with DB data + ticket + attendee + timestamp
        String uniquePayload = String.format(
                "QR|%s|TICKET:%s|ATTENDEE:%s|TIME:%d|UNIQUE:%s",
                qrNumber,
                ticketId.substring(0, 8), // First 8 chars of ticket ID
                attendeeId.substring(0, 8), // First 8 chars of attendee ID
                System.currentTimeMillis(),
                UUID.randomUUID().toString().substring(0, 8)
        );

        // Encode to base64 for display
        return Base64.getEncoder().encodeToString(uniquePayload.getBytes());
    }

    /**
     * Extract number from DB QR format
     * E.g., "qr-12345" -> "12345"
     *
     * @param dbQRCode DB format (qr-something)
     * @return Extracted number or original if not in expected format
     */
    private String extractNumberFromDBFormat(String dbQRCode) {
        if (dbQRCode == null) {
            return "UNKNOWN";
        }

        // If format is "qr-" followed by numbers
        if (dbQRCode.toLowerCase().startsWith("qr-")) {
            return dbQRCode.substring(3); // Remove "qr-" prefix
        }

        // If format is "qr" followed by numbers
        if (dbQRCode.toLowerCase().startsWith("qr")) {
            return dbQRCode.substring(2); // Remove "qr" prefix
        }

        // Return as-is if different format
        return dbQRCode;
    }

    /**
     * Convert display QR code back to DB format for verification
     * Display format: Full unique QR code (base64)
     * DB format: "qr-12345"
     *
     * @param displayQRCode The QR code shown to user
     * @return DB format QR code ("qr-12345")
     */
    public String convertQRCodeBackToDBFormat(String displayQRCode) {
        System.out.println("🔄 [QRCodeConverter] Converting QR code back to DB format");

        if (displayQRCode == null || displayQRCode.equals("N/A")) {
            return "N/A";
        }

        try {
            // Decode base64
            String decoded = new String(Base64.getDecoder().decode(displayQRCode));
            System.out.println("   Decoded: " + decoded);

            // Extract QR number from decoded string
            // Format: "QR|12345|TICKET:xxx|ATTENDEE:xxx|..."
            String[] parts = decoded.split("\\|");
            if (parts.length > 1) {
                String qrNumber = parts[1]; // Get the number part
                String dbFormat = "qr-" + qrNumber;
                System.out.println("   ✓ Converted back to DB format: " + dbFormat);
                return dbFormat;
            }
        } catch (IllegalArgumentException e) {
            System.out.println("   ⚠️ Failed to decode QR code: " + e.getMessage());
        }

        return "N/A";
    }

    /**
     * Convert batch of tickets' QR codes
     *
     * @param tickets List of tickets to convert
     * @return Map of ticketId -> converted QR code
     */
    public Map<String, String> convertBatchQRCodes(List<Ticket> tickets) {
        System.out.println("🔄 [QRCodeConverter] Converting batch of " + tickets.size() + " tickets");

        return tickets.stream().collect(Collectors.toMap(
                ticket -> ticket.getId().toString(),
                this::convertQRCodeForDisplay
        ));
    }

    /**
     * Convert TicketDisplayDTO QR codes
     *
     * @param displayDTOs List of display DTOs
     * @return List with converted QR codes
     */
    public List<TicketDisplayDTO> convertDisplayDTOsQRCodes(List<TicketDisplayDTO> displayDTOs) {
        System.out.println("🔄 [QRCodeConverter] Converting QR codes for " + displayDTOs.size() + " display DTOs");

        return displayDTOs.stream().map(dto -> {
            // The DTO's QR code is already from DB, convert it for display
            String convertedQR = convertQRCodeForDisplay(dto.getQrCode(), dto.getTicketId(), "unknown");
            return new TicketDisplayDTO(
                    dto.getTicketId(),
                    dto.getEventName(),
                    dto.getSessionName(),
                    dto.getType(),
                    dto.getPrice(),
                    dto.getStatus(),
                    dto.getPurchaseDate(),
                    convertedQR  // Use converted QR code
            );
        }).collect(Collectors.toList());
    }

    /**
     * Validate if QR code is in valid format
     *
     * @param qrCode QR code to validate
     * @return true if valid
     */
    public boolean isValidQRCodeFormat(String qrCode) {
        if (qrCode == null || qrCode.isEmpty() || qrCode.equals("N/A")) {
            return false;
        }

        // Check if it's base64 encoded (converted format)
        try {
            Base64.getDecoder().decode(qrCode);
            return true;
        } catch (IllegalArgumentException e) {
            // Not base64, check if it's DB format
            return qrCode.toLowerCase().startsWith("qr-") || qrCode.toLowerCase().startsWith("qr");
        }
    }

    /**
     * Get QR code format type
     *
     * @param qrCode QR code to check
     * @return "DB_FORMAT", "DISPLAY_FORMAT", or "INVALID"
     */
    public String getQRCodeFormatType(String qrCode) {
        if (qrCode == null || qrCode.isEmpty()) {
            return "INVALID";
        }

        // Check if DB format (qr-12345)
        if (qrCode.toLowerCase().startsWith("qr-") || qrCode.toLowerCase().startsWith("qr")) {
            return "DB_FORMAT";
        }

        // Check if display format (base64)
        try {
            Base64.getDecoder().decode(qrCode);
            return "DISPLAY_FORMAT";
        } catch (IllegalArgumentException e) {
            return "INVALID";
        }
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        System.out.println("🗑️ [QRCodeConverter] Clearing QR code cache (" + qrCodeCache.size() + " entries)");
        qrCodeCache.clear();
    }

    /**
     * Get cache statistics
     *
     * @return Cache size and info
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("size", qrCodeCache.size());
        stats.put("maxSize", MAX_CACHE_SIZE);
        stats.put("utilization", String.format("%.2f%%", (qrCodeCache.size() * 100.0) / MAX_CACHE_SIZE));
        return stats;
    }
}

