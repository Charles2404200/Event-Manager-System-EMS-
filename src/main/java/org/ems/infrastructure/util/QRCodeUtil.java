package org.ems.infrastructure.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import java.util.Random;

/**
 * Utility class for generating QR codes for tickets
 */
public class QRCodeUtil {

    private static final int QR_CODE_SIZE = 300; // 300x300 pixels
    private static final Random random = new Random();

    /**
     * Generate random QR code data (base64 encoded)
     * Each ticket gets a unique QR code
     *
     * @param ticketId UUID of the ticket
     * @param attendeeId UUID of the attendee
     * @param sessionId UUID of the session
     * @return Base64 encoded QR code string
     */
    public static String generateQRCodeData(UUID ticketId, UUID attendeeId, UUID sessionId) {
        try {
            // Create unique QR payload
            String payload = generateUniquePayload(ticketId, attendeeId, sessionId);

            System.out.println("Generated QR payload: " + payload);
            return payload;

        } catch (Exception e) {
            System.err.println("Failed to generate QR code data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate unique payload for QR code
     * Format: ticketId|attendeeId|sessionId|timestamp|randomUUID|checksum
     *
     * @param ticketId UUID of the ticket
     * @param attendeeId UUID of the attendee
     * @param sessionId UUID of the session
     * @return Base64 encoded payload
     */
    private static String generateUniquePayload(UUID ticketId, UUID attendeeId, UUID sessionId) {
        // Components
        String ticketIdStr = ticketId.toString();
        String attendeeIdStr = attendeeId.toString();
        String sessionIdStr = sessionId.toString();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomUUID = UUID.randomUUID().toString();

        // Random component for extra uniqueness
        String randomData = generateRandomString(16);

        // Create payload
        String payload = String.format(
            "%s|%s|%s|%s|%s|%s",
            ticketIdStr,
            attendeeIdStr,
            sessionIdStr,
            timestamp,
            randomUUID,
            randomData
        );

        // Encode to base64
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    /**
     * Generate random string for extra uniqueness
     *
     * @param length Length of random string
     * @return Random alphanumeric string
     */
    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(chars.charAt(random.nextInt(chars.length())));
        }

        return result.toString();
    }

    /**
     * Generate QR code image as byte array (PNG format)
     * Can be used to display or save QR code image
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return Byte array of QR code image (PNG)
     */
    public static byte[] generateQRCodeImage(String qrCodeData) {
        try {
            // Decode base64 to get original data
            String originalData = new String(Base64.getDecoder().decode(qrCodeData));

            // Generate QR code
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(originalData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            // Convert to image bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            System.out.println("Generated QR code image: " + baos.size() + " bytes");
            return baos.toByteArray();

        } catch (WriterException | IOException e) {
            System.err.println("Failed to generate QR code image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verify if QR code data is valid
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return True if valid, false otherwise
     */
    public static boolean isValidQRCodeData(String qrCodeData) {
        try {
            // Try to decode base64
            Base64.getDecoder().decode(qrCodeData);

            // Check if contains all required components
            String decoded = new String(Base64.getDecoder().decode(qrCodeData));
            String[] parts = decoded.split("\\|");

            // Should have 6 parts: ticketId|attendeeId|sessionId|timestamp|randomUUID|randomData
            return parts.length == 6;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decode QR code data to get original payload
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return Original payload string
     */
    public static String decodeQRCodeData(String qrCodeData) {
        try {
            return new String(Base64.getDecoder().decode(qrCodeData));
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to decode QR code data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extract ticket ID from QR code data
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return Ticket ID or null if invalid
     */
    public static UUID extractTicketId(String qrCodeData) {
        try {
            String decoded = decodeQRCodeData(qrCodeData);
            if (decoded == null) return null;

            String[] parts = decoded.split("\\|");
            if (parts.length > 0) {
                return UUID.fromString(parts[0]);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract ticket ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract attendee ID from QR code data
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return Attendee ID or null if invalid
     */
    public static UUID extractAttendeeId(String qrCodeData) {
        try {
            String decoded = decodeQRCodeData(qrCodeData);
            if (decoded == null) return null;

            String[] parts = decoded.split("\\|");
            if (parts.length > 1) {
                return UUID.fromString(parts[1]);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract attendee ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extract session ID from QR code data
     *
     * @param qrCodeData Base64 encoded QR code data
     * @return Session ID or null if invalid
     */
    public static UUID extractSessionId(String qrCodeData) {
        try {
            String decoded = decodeQRCodeData(qrCodeData);
            if (decoded == null) return null;

            String[] parts = decoded.split("\\|");
            if (parts.length > 2) {
                return UUID.fromString(parts[2]);
            }
        } catch (Exception e) {
            System.err.println("Failed to extract session ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Test QR code generation
     */
    public static void main(String[] args) {
        UUID ticketId = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        System.out.println("=== QR Code Generation Test ===");
        System.out.println("Ticket ID: " + ticketId);
        System.out.println("Attendee ID: " + attendeeId);
        System.out.println("Session ID: " + sessionId);
        System.out.println();

        // Generate QR code data
        String qrCodeData = generateQRCodeData(ticketId, attendeeId, sessionId);
        System.out.println("QR Code Data (Base64): " + qrCodeData);
        System.out.println();

        // Verify
        boolean isValid = isValidQRCodeData(qrCodeData);
        System.out.println("Is Valid: " + isValid);
        System.out.println();

        // Decode
        String decoded = decodeQRCodeData(qrCodeData);
        System.out.println("Decoded: " + decoded);
        System.out.println();

        // Extract IDs
        System.out.println("Extracted Ticket ID: " + extractTicketId(qrCodeData));
        System.out.println("Extracted Attendee ID: " + extractAttendeeId(qrCodeData));
        System.out.println("Extracted Session ID: " + extractSessionId(qrCodeData));
        System.out.println();

        // Generate image
        byte[] qrCodeImage = generateQRCodeImage(qrCodeData);
        if (qrCodeImage != null) {
            System.out.println("QR Code Image Generated: " + qrCodeImage.length + " bytes");
        }
    }
}

