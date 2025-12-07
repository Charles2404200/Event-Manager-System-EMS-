package org.ems.application.service;

import java.util.UUID;

/**
 * Service interface for managing image uploads and storage.
 */
public interface ImageService {

    /**
     * Upload an image for an event.
     *
     * @param filePath Path to the image file
     * @param eventId Event UUID
     * @return Image path if successful, null otherwise
     */
    String uploadEventImage(String filePath, UUID eventId);

    /**
     * Upload an image for a session (session materials).
     *
     * @param filePath Path to the image/material file
     * @param sessionId Session UUID
     * @return Material path if successful, null otherwise
     */
    String uploadSessionMaterial(String filePath, UUID sessionId);

    /**
     * Upload a QR code image for a ticket.
     *
     * @param filePath Path to the QR code image file
     * @param ticketId Ticket UUID
     * @return QR code path if successful, null otherwise
     */
    String uploadTicketQRCode(String filePath, UUID ticketId);

    /**
     * Upload image from binary data (e.g., generated QR code).
     *
     * @param imageData Binary image data
     * @param entityId Entity UUID
     * @param entityType Type of entity (EVENT, SESSION, TICKET)
     * @param filename Desired filename
     * @return Image path if successful, null otherwise
     */
    String uploadBinaryImage(byte[] imageData, UUID entityId, String entityType, String filename);

    /**
     * Retrieve image as binary data.
     *
     * @param imagePath Storage path of the image
     * @return Binary image data, or null if not found
     */
    byte[] getImageData(String imagePath);

    /**
     * Delete an image.
     *
     * @param imagePath Storage path of the image
     * @return true if deletion successful, false otherwise
     */
    boolean deleteImage(String imagePath);

    /**
     * Check if image exists.
     *
     * @param imagePath Storage path of the image
     * @return true if image exists, false otherwise
     */
    boolean imageExists(String imagePath);
}

