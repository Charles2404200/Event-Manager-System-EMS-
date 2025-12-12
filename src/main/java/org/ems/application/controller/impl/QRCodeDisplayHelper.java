package org.ems.application.controller.impl;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.ems.application.service.ticket.TicketQRCodeImageService;

import java.io.ByteArrayInputStream;

/**
 * QR Code Display Helper for MyTicketsController
 * Converts QR code text to image and displays in UI
 *
 * Usage in MyTicketsController.onViewQRCode():
 * ```
 * QRCodeDisplayHelper helper = new QRCodeDisplayHelper();
 * helper.displayQRCode(content, selected.getQrCode());
 * ```
 *
 * @author <your group number>
 */
public class QRCodeDisplayHelper {

    private final TicketQRCodeImageService imageService;

    public QRCodeDisplayHelper() {
        this.imageService = new TicketQRCodeImageService();
    }

    /**
     * Display QR code as image in UI
     *
     * @param parentContainer VBox to add QR code to
     * @param qrCodeData QR code data (text or base64)
     * @return true if successfully displayed image, false if fallback to text
     */
    public boolean displayQRCode(VBox parentContainer, String qrCodeData) {
        System.out.println("📱 [QRCodeDisplayHelper] Displaying QR code...");

        if (qrCodeData == null || qrCodeData.isEmpty() || qrCodeData.equals("N/A")) {
            System.out.println("  ⚠️ No QR code data");
            displayTextFallback(parentContainer, qrCodeData);
            return false;
        }

        try {
            // Check format
            String formatInfo = imageService.getQRCodeFormatInfo(qrCodeData);
            System.out.println("  📋 Format: " + formatInfo);

            // Convert to image
            byte[] imageBytes = imageService.convertQRCodeToImage(qrCodeData);

            if (imageBytes != null && imageBytes.length > 0) {
                System.out.println("  ✓ QR code image generated successfully");
                return displayImageQRCode(parentContainer, imageBytes);
            } else {
                System.out.println("  ⚠️ Failed to generate image, using text fallback");
                displayTextFallback(parentContainer, qrCodeData);
                return false;
            }

        } catch (Exception e) {
            System.err.println("  ✗ Error displaying QR code: " + e.getMessage());
            e.printStackTrace();
            displayTextFallback(parentContainer, qrCodeData);
            return false;
        }
    }

    /**
     * Display QR code as ImageView
     *
     * @param container VBox to add to
     * @param imageBytes PNG image bytes
     * @return true if successful
     */
    private boolean displayImageQRCode(VBox container, byte[] imageBytes) {
        try {
            // Create ImageView
            ImageView qrImageView = new ImageView(
                    new Image(new ByteArrayInputStream(imageBytes))
            );
            qrImageView.setFitWidth(300);
            qrImageView.setFitHeight(300);
            qrImageView.setPreserveRatio(true);
            qrImageView.setStyle("-fx-border-color: #2c3e50; -fx-border-width: 2; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 0);");

            // Create QR box
            VBox qrBox = new VBox(10);
            qrBox.setStyle("-fx-alignment: center; -fx-border-color: #ecf0f1; " +
                    "-fx-border-width: 1; -fx-padding: 20; -fx-border-radius: 5;");

            Label qrTitle = new Label("QR Code");
            qrTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                    "-fx-text-fill: #2c3e50;");

            // Add instructions
            Label instructions = new Label("📱 Show this to staff at event entrance");
            instructions.setStyle("-fx-font-size: 11; -fx-text-fill: #666666;");

            qrBox.getChildren().addAll(qrTitle, qrImageView, instructions);
            container.getChildren().add(qrBox);

            System.out.println("  ✓ QR code image displayed successfully");
            return true;

        } catch (Exception e) {
            System.err.println("  ✗ Failed to display QR code image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Display QR code as text fallback
     *
     * @param container VBox to add to
     * @param qrCodeData QR code data as text
     */
    private void displayTextFallback(VBox container, String qrCodeData) {
        System.out.println("  📝 Using text fallback");

        VBox qrBox = new VBox(10);
        qrBox.setStyle("-fx-alignment: top-center; -fx-border-color: #e74c3c; " +
                "-fx-border-width: 2; -fx-padding: 15; -fx-border-radius: 5;");

        Label warningLabel = new Label("⚠️ QR Code Display Issue");
        warningLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; " +
                "-fx-text-fill: #e74c3c;");

        Label textLabel = new Label(qrCodeData);
        textLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #2c3e50; " +
                "-fx-padding: 10; -fx-background-color: #ecf0f1; " +
                "-fx-border-radius: 3;");
        textLabel.setWrapText(true);

        Label descLabel = new Label("QR Code shown as text. Please scan the image version if available, " +
                "or provide this code to staff.");
        descLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #7f8c8d;");
        descLabel.setWrapText(true);

        qrBox.getChildren().addAll(warningLabel, textLabel, descLabel);
        container.getChildren().add(qrBox);
    }

    /**
     * Check if QR code can be displayed as image
     *
     * @param qrCodeData QR code data
     * @return true if can display as image
     */
    public boolean canDisplayAsImage(String qrCodeData) {
        return imageService.canConvertToImage(qrCodeData);
    }

    /**
     * Get QR code format info
     *
     * @param qrCodeData QR code data
     * @return Format description
     */
    public String getFormatInfo(String qrCodeData) {
        return imageService.getQRCodeFormatInfo(qrCodeData);
    }
}

