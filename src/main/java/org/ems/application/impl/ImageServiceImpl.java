package org.ems.application.impl;

import org.ems.application.service.ImageService;
import org.ems.infrastructure.util.ImageUploadUtil;

import java.util.UUID;

/**
 * Implementation of ImageService for managing image uploads and storage.
 */
public class ImageServiceImpl implements ImageService {

    @Override
    public String uploadEventImage(String filePath, UUID eventId) {
        return ImageUploadUtil.uploadImage(filePath, eventId.toString(), "EVENT");
    }

    @Override
    public String uploadSessionMaterial(String filePath, UUID sessionId) {
        return ImageUploadUtil.uploadImage(filePath, sessionId.toString(), "SESSION");
    }

    @Override
    public String uploadTicketQRCode(String filePath, UUID ticketId) {
        return ImageUploadUtil.uploadImage(filePath, ticketId.toString(), "TICKET");
    }

    @Override
    public String uploadBinaryImage(byte[] imageData, UUID entityId, String entityType, String filename) {
        return ImageUploadUtil.saveBinaryImageToFile(imageData, entityId.toString(), entityType, filename);
    }

    @Override
    public byte[] getImageData(String imagePath) {
        return ImageUploadUtil.readImageAsBinary(imagePath);
    }

    @Override
    public boolean deleteImage(String imagePath) {
        return ImageUploadUtil.deleteImage(imagePath);
    }

    @Override
    public boolean imageExists(String imagePath) {
        return ImageUploadUtil.imageExists(imagePath);
    }
}

