package org.ems.application.service.ticket.impl;

import org.ems.application.service.ticket.TicketQRCodeConverterService;
import org.ems.domain.model.Ticket;
import org.ems.application.dto.ticket.TicketDisplayDTO;
import org.ems.domain.model.enums.TicketStatus;
import org.ems.domain.model.enums.PaymentStatus;
import org.ems.domain.model.enums.TicketType;

import java.math.BigDecimal;
import java.util.*;

/**
 * Implementation and Example class for TicketQRCodeConverterService
 * Demonstrates QR code conversion from DB format to display format
 *
 * DB Format: qr-12345 (stored in database)
 * Display Format: Unique base64 encoded QR code (shown to attendee)
 *
 * @author <your group number>
 */
public class TicketQRCodeConverterServiceImpl {

    private final TicketQRCodeConverterService converterService;

    public TicketQRCodeConverterServiceImpl() {
        this.converterService = new TicketQRCodeConverterService();
    }

    /**
     * Example 1: Convert single ticket QR code
     */
    public void exampleConvertSingleTicket() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 1: Convert Single Ticket QR Code");
        System.out.println("=".repeat(70));

        // Create sample ticket with DB format QR code
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setAttendeeId(UUID.randomUUID());
        ticket.setEventId(UUID.randomUUID());
        ticket.setQrCodeData("qr-12345"); // DB format
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("50.00"));
        ticket.setPaymentStatus(PaymentStatus.PAID);
        ticket.setTicketStatus(TicketStatus.ACTIVE);

        System.out.println("\n📊 Original Ticket QR Code (from DB):");
        System.out.println("   Format: DB (qr-12345)");
        System.out.println("   Value: " + ticket.getQrCodeData());

        // Convert for display
        String displayQR = converterService.convertQRCodeForDisplay(ticket);

        System.out.println("\n📊 Converted Ticket QR Code (for display):");
        System.out.println("   Format: Display (base64 encoded unique)");
        System.out.println("   Value: " + displayQR);
        System.out.println("   First 60 chars: " + displayQR.substring(0, Math.min(60, displayQR.length())) + "...");

        System.out.println("\n✅ Conversion completed");
    }

    /**
     * Example 2: Convert multiple tickets
     */
    public void exampleConvertMultipleTickets() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 2: Convert Multiple Tickets (Batch)");
        System.out.println("=".repeat(70));

        List<Ticket> tickets = new ArrayList<>();

        // Create 5 sample tickets
        for (int i = 1; i <= 5; i++) {
            Ticket ticket = new Ticket();
            ticket.setId(UUID.randomUUID());
            ticket.setAttendeeId(UUID.randomUUID());
            ticket.setEventId(UUID.randomUUID());
            ticket.setQrCodeData("qr-" + (10000 + i)); // DB format
            ticket.setType(TicketType.GENERAL);
            ticket.setPrice(new BigDecimal("50.00"));
            ticket.setPaymentStatus(PaymentStatus.PAID);
            ticket.setTicketStatus(TicketStatus.ACTIVE);
            tickets.add(ticket);
        }

        System.out.println("\n📊 Original Tickets (from DB):");
        for (Ticket t : tickets) {
            System.out.println("   ID: " + t.getId().toString().substring(0, 8) + "... | QR: " + t.getQrCodeData());
        }

        // Convert batch
        Map<String, String> conversions = converterService.convertBatchQRCodes(tickets);

        System.out.println("\n📊 Converted Tickets (for display):");
        int index = 1;
        for (Map.Entry<String, String> entry : conversions.entrySet()) {
            String qrDisplay = entry.getValue();
            String preview = qrDisplay.length() > 40 ? qrDisplay.substring(0, 40) + "..." : qrDisplay;
            System.out.println("   " + index + ". ID: " + entry.getKey().substring(0, 8) + "...");
            System.out.println("      QR: " + preview);
            index++;
        }

        System.out.println("\n✅ Batch conversion completed: " + conversions.size() + " tickets");
    }

    /**
     * Example 3: Convert and verify format types
     */
    public void exampleVerifyFormatTypes() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 3: Verify QR Code Format Types");
        System.out.println("=".repeat(70));

        List<String> qrCodes = Arrays.asList(
                "qr-12345",                    // DB format
                "qr-99999",                    // DB format
                "QUJ8TXh",                     // Display format (base64)
                "QUJ8TXh8VElDS0VUOnh4eHx8",  // Display format (base64)
                "invalid-qr",                  // Invalid format
                "",                             // Empty
                null                            // Null
        );

        System.out.println("\n📊 Analyzing QR Code Formats:\n");
        for (String qr : qrCodes) {
            System.out.println("QR Code: " + (qr != null ? qr : "null"));

            if (qr != null) {
                String formatType = converterService.getQRCodeFormatType(qr);
                boolean isValid = converterService.isValidQRCodeFormat(qr);
                System.out.println("  Format Type: " + formatType);
                System.out.println("  Is Valid: " + isValid);

                // If it's a display format, convert back to DB
                if (formatType.equals("DISPLAY_FORMAT")) {
                    try {
                        String dbFormat = converterService.convertQRCodeBackToDBFormat(qr);
                        System.out.println("  Converted back to DB: " + dbFormat);
                    } catch (Exception e) {
                        System.out.println("  Conversion error: " + e.getMessage());
                    }
                }
            }
            System.out.println();
        }
    }

    /**
     * Example 4: Convert Display DTOs
     */
    public void exampleConvertDisplayDTOs() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 4: Convert Display DTOs");
        System.out.println("=".repeat(70));

        List<TicketDisplayDTO> displayDTOs = new ArrayList<>();

        // Create sample display DTOs with DB format QR codes
        for (int i = 1; i <= 3; i++) {
            UUID ticketId = UUID.randomUUID();
            TicketDisplayDTO dto = new TicketDisplayDTO(
                    ticketId.toString(),
                    "Event " + i,
                    "Session " + i,
                    "GENERAL",
                    "$50.00",
                    "ACTIVE",
                    "2024-12-01",
                    "qr-" + (10000 + i)  // DB format QR
            );
            displayDTOs.add(dto);
        }

        System.out.println("\n📊 Original Display DTOs (DB QR format):");
        for (int i = 0; i < displayDTOs.size(); i++) {
            TicketDisplayDTO dto = displayDTOs.get(i);
            System.out.println("   " + (i + 1) + ". Event: " + dto.getEventName() + " | QR: " + dto.getQrCode());
        }

        // Convert
        List<TicketDisplayDTO> convertedDTOs = converterService.convertDisplayDTOsQRCodes(displayDTOs);

        System.out.println("\n📊 Converted Display DTOs (unique QR format):");
        for (int i = 0; i < convertedDTOs.size(); i++) {
            TicketDisplayDTO dto = convertedDTOs.get(i);
            String qrPreview = dto.getQrCode().substring(0, Math.min(40, dto.getQrCode().length())) + "...";
            System.out.println("   " + (i + 1) + ". Event: " + dto.getEventName());
            System.out.println("      QR: " + qrPreview);
        }

        System.out.println("\n✅ Display DTO conversion completed");
    }

    /**
     * Example 5: Cache operations
     */
    public void exampleCacheOperations() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 5: Cache Operations");
        System.out.println("=".repeat(70));

        System.out.println("\n📊 Initial Cache Stats:");
        displayCacheStats();

        // Convert some tickets (will be cached)
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Ticket ticket = new Ticket();
            ticket.setId(UUID.randomUUID());
            ticket.setAttendeeId(UUID.randomUUID());
            ticket.setQrCodeData("qr-" + (10000 + i));
            tickets.add(ticket);
        }

        System.out.println("\n🔄 Converting 3 tickets (will cache results)...");
        converterService.convertBatchQRCodes(tickets);

        System.out.println("\n📊 Cache Stats After Conversion:");
        displayCacheStats();

        System.out.println("\n🔄 Converting same tickets again (will use cache)...");
        converterService.convertBatchQRCodes(tickets);

        System.out.println("\n📊 Cache Stats After Second Conversion:");
        displayCacheStats();

        System.out.println("\n🗑️ Clearing cache...");
        converterService.clearCache();

        System.out.println("\n📊 Cache Stats After Clear:");
        displayCacheStats();
    }

    /**
     * Example 6: Real-world scenario - Display ticket to attendee
     */
    public void exampleRealWorldScenario() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("EXAMPLE 6: Real-World Scenario - Display Ticket to Attendee");
        System.out.println("=".repeat(70));

        // Attendee purchases ticket - gets saved to DB with format: qr-12345
        Ticket purchasedTicket = new Ticket();
        purchasedTicket.setId(UUID.randomUUID());
        purchasedTicket.setAttendeeId(UUID.randomUUID());
        purchasedTicket.setEventId(UUID.randomUUID());
        purchasedTicket.setQrCodeData("qr-55555");
        purchasedTicket.setType(TicketType.VIP);
        purchasedTicket.setPrice(new BigDecimal("99.99"));
        purchasedTicket.setPaymentStatus(PaymentStatus.PAID);
        purchasedTicket.setTicketStatus(TicketStatus.ACTIVE);

        System.out.println("\n1️⃣ Ticket purchased and saved to DB:");
        System.out.println("   Ticket ID: " + purchasedTicket.getId().toString().substring(0, 8) + "...");
        System.out.println("   Type: " + purchasedTicket.getType());
        System.out.println("   Price: " + purchasedTicket.getPrice());
        System.out.println("   QR Code (stored): " + purchasedTicket.getQrCodeData());

        // Attendee views their tickets - QR code is converted to unique format
        System.out.println("\n2️⃣ Attendee views tickets in app:");
        String displayQR = converterService.convertQRCodeForDisplay(purchasedTicket);
        System.out.println("   QR Code (displayed): " + displayQR.substring(0, 50) + "...");
        System.out.println("   (This unique code is never stored, generated on-the-fly)");

        // Later, for verification at event
        System.out.println("\n3️⃣ At event, staff scans the QR code:");
        System.out.println("   Scanned: " + displayQR.substring(0, 50) + "...");
        String verifyFormat = converterService.getQRCodeFormatType(displayQR);
        System.out.println("   Format verified: " + verifyFormat);

        // Can convert back to DB for lookup
        String dbFormatVerify = converterService.convertQRCodeBackToDBFormat(displayQR);
        System.out.println("   DB lookup value: " + dbFormatVerify);

        System.out.println("\n✅ Complete workflow demonstrated");
    }

    /**
     * Display cache statistics
     */
    private void displayCacheStats() {
        Map<String, Object> stats = converterService.getCacheStats();
        System.out.println("   Size: " + stats.get("size") + " / " + stats.get("maxSize"));
        System.out.println("   Utilization: " + stats.get("utilization"));
    }

    /**
     * Run all examples
     */
    public void runAllExamples() {
        System.out.println("\n\n");
        System.out.println("╔" + "=".repeat(68) + "╗");
        System.out.println("║  TICKET QR CODE CONVERTER SERVICE - IMPLEMENTATION EXAMPLES       ║");
        System.out.println("╚" + "=".repeat(68) + "╝");

        try {
            exampleConvertSingleTicket();
            exampleConvertMultipleTickets();
            exampleVerifyFormatTypes();
            exampleConvertDisplayDTOs();
            exampleCacheOperations();
            exampleRealWorldScenario();

            System.out.println("\n" + "=".repeat(70));
            System.out.println("✅ ALL EXAMPLES COMPLETED SUCCESSFULLY");
            System.out.println("=".repeat(70) + "\n");

        } catch (Exception e) {
            System.err.println("\n❌ ERROR running examples: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main method for testing
     */
    public static void main(String[] args) {
        TicketQRCodeConverterServiceImpl impl = new TicketQRCodeConverterServiceImpl();
        impl.runAllExamples();
    }
}

