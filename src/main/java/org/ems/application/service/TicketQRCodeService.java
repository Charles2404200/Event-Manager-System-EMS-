package org.ems.application.service;

import org.ems.application.dto.TicketDisplayDTO;
import org.ems.infrastructure.util.QRCodeUtil;

import java.io.ByteArrayInputStream;

/**
 * Service for handling QR code presentation
 * Implements Single Responsibility Principle - only handles QR code UI logic
 * @author <your group number>
 */
public class TicketQRCodeService {

    /**
     * Generate QR code image bytes from QR code data
     * @param qrCodeData The QR code data string
     * @return Image bytes, or null if generation fails
     */
    public byte[] generateQRCodeImage(String qrCodeData) {
        long start = System.currentTimeMillis();
        System.out.println("🔲 [TicketQRCodeService] Generating QR code image...");

        if (qrCodeData == null || qrCodeData.equals("N/A")) {
            System.out.println("  ℹ No QR code data available");
            return null;
        }

        try {
            byte[] imageBytes = QRCodeUtil.generateQRCodeImage(qrCodeData);
            System.out.println("  ✓ QR code image generated in " + (System.currentTimeMillis() - start) + "ms");
            return imageBytes;
        } catch (Exception e) {
            System.err.println("  ⚠️ Failed to generate QR code image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if ticket has valid QR code
     * @param ticket Ticket to check
     * @return True if ticket has valid QR code
     */
    public boolean hasValidQRCode(TicketDisplayDTO ticket) {
        return ticket != null &&
               ticket.getQrCode() != null &&
               !ticket.getQrCode().equals("N/A") &&
               !ticket.getQrCode().isEmpty();
    }

    /**
     * Get QR code display type
     * @param qrCodeData The QR code data
     * @return "IMAGE" if can generate image, "TEXT" for fallback
     */
    public String getQRCodeDisplayType(String qrCodeData) {
        byte[] imageBytes = generateQRCodeImage(qrCodeData);
        return imageBytes != null ? "IMAGE" : "TEXT";
    }
}

