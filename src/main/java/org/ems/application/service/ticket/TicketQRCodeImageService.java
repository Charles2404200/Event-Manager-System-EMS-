package org.ems.application.service.ticket;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Service for converting QR code text to image
 * Handles both DB format (qr-12345) and display format (base64) QR codes
 * Generates PNG images for display in UI
 *
 * @author <your group number>
 */
public class TicketQRCodeImageService {

    private static final int QR_CODE_SIZE = 300; // 300x300 pixels

    /**
     * Convert QR code text to PNG image bytes
     * Supports both DB format (qr-12345) and base64 format
     *
     * @param qrCodeData QR code data (text or base64)
     * @return PNG image bytes, or null if conversion fails
     */
    public byte[] convertQRCodeToImage(String qrCodeData) {
        System.out.println("🔲 [QRCodeImageService] Converting QR code to image...");

        if (qrCodeData == null || qrCodeData.isEmpty() || qrCodeData.equals("N/A")) {
            System.out.println("  ℹ No QR code data available");
            return null;
        }

        try {
            String dataToEncode = qrCodeData;

            // Check if it's base64 encoded
            if (isBase64(qrCodeData)) {
                System.out.println("  📋 Format: Base64 encoded");
                try {
                    // Decode base64 to get original data
                    dataToEncode = new String(Base64.getDecoder().decode(qrCodeData));
                    System.out.println("  ✓ Decoded base64 successfully");
                } catch (IllegalArgumentException e) {
                    System.out.println("  ⚠️ Base64 decode failed, using as-is");
                    dataToEncode = qrCodeData;
                }
            } else {
                System.out.println("  📋 Format: Plain text (DB format)");
            }

            // Generate QR code
            byte[] imageBytes = generateQRCodeImageFromData(dataToEncode);

            if (imageBytes != null) {
                System.out.println("  ✓ QR code image generated: " + imageBytes.length + " bytes");
            }

            return imageBytes;

        } catch (Exception e) {
            System.err.println("  ✗ Error converting QR code to image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generate QR code image from plain text data
     *
     * @param data Plain text data to encode
     * @return PNG image bytes
     */
    private byte[] generateQRCodeImageFromData(String data) {
        try {
            System.out.println("  🔧 Generating QR code from data: " +
                    (data.length() > 50 ? data.substring(0, 50) + "..." : data));

            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            byte[] imageBytes = baos.toByteArray();
            System.out.println("  ✓ QR code PNG generated: " + imageBytes.length + " bytes");

            return imageBytes;

        } catch (WriterException | IOException e) {
            System.err.println("  ✗ Failed to generate QR code PNG: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if string is valid base64
     *
     * @param str String to check
     * @return true if valid base64
     */
    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Base64 strings are typically longer and contain only A-Z, a-z, 0-9, +, /, =
        if (!str.matches("^[A-Za-z0-9+/]*={0,2}$")) {
            return false;
        }

        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get detailed QR code format information
     *
     * @param qrCodeData QR code data
     * @return Format description
     */
    public String getQRCodeFormatInfo(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isEmpty()) {
            return "EMPTY";
        }

        if (qrCodeData.equals("N/A")) {
            return "NO_DATA";
        }

        if (qrCodeData.toLowerCase().startsWith("qr-")) {
            return "DB_FORMAT (qr-number)";
        }

        if (isBase64(qrCodeData)) {
            return "BASE64_ENCODED";
        }

        return "PLAIN_TEXT";
    }

    /**
     * Validate QR code data can be converted to image
     *
     * @param qrCodeData QR code data to validate
     * @return true if can be converted
     */
    public boolean canConvertToImage(String qrCodeData) {
        if (qrCodeData == null || qrCodeData.isEmpty() || qrCodeData.equals("N/A")) {
            return false;
        }

        // Try to convert and see if it works
        byte[] result = convertQRCodeToImage(qrCodeData);
        return result != null && result.length > 0;
    }

    /**
     * Get recommended QR code size in pixels
     *
     * @return QR code size
     */
    public int getQRCodeSize() {
        return QR_CODE_SIZE;
    }
}

