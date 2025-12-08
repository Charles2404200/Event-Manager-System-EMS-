package org.ems.application.impl;

import org.ems.application.service.ImageService;
import org.ems.application.service.ImageUploadService;

import java.io.File;
import java.util.UUID;

/**
 * Implementation of ImageService for managing image uploads to Cloudflare R2.
 */
public class ImageServiceImpl implements ImageService {

    private final ImageUploadService uploadService;

    public ImageServiceImpl() {
        this.uploadService = new ImageUploadService();
    }

    @Override
    public String uploadEventImage(String filePath, UUID eventId) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("Image file path is empty");
                return null;
            }

            File imageFile = new File(filePath);
            if (!imageFile.exists()) {
                System.err.println("Image file does not exist: " + filePath);
                return null;
            }

            System.out.println("Uploading event image to Cloudflare R2: " + filePath);
            String imageUrl = uploadService.uploadEventImage(imageFile, eventId.toString());
            System.out.println("✓ Event image uploaded to R2: " + imageUrl);
            return imageUrl;
        } catch (Exception e) {
            System.err.println("✗ Failed to upload event image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String uploadSessionMaterial(String filePath, UUID sessionId) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("Session material path is empty");
                return null;
            }

            File materialFile = new File(filePath);
            if (!materialFile.exists()) {
                System.err.println("Session material file does not exist: " + filePath);
                return null;
            }

            System.out.println("Uploading session material to Cloudflare R2: " + filePath);
            String materialUrl = uploadService.uploadEventImage(materialFile, sessionId.toString());
            System.out.println("✓ Session material uploaded to R2: " + materialUrl);
            return materialUrl;
        } catch (Exception e) {
            System.err.println("✗ Failed to upload session material: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String uploadTicketQRCode(String filePath, UUID ticketId) {
        try {
            if (filePath == null || filePath.isEmpty()) {
                System.err.println("QR code file path is empty");
                return null;
            }

            File qrCodeFile = new File(filePath);
            if (!qrCodeFile.exists()) {
                System.err.println("QR code file does not exist: " + filePath);
                return null;
            }

            System.out.println("Uploading ticket QR code to Cloudflare R2: " + filePath);
            String qrCodeUrl = uploadService.uploadEventImage(qrCodeFile, ticketId.toString());
            System.out.println("✓ Ticket QR code uploaded to R2: " + qrCodeUrl);
            return qrCodeUrl;
        } catch (Exception e) {
            System.err.println("✗ Failed to upload ticket QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String uploadBinaryImage(byte[] imageData, UUID entityId, String entityType, String filename) {
        try {
            if (imageData == null || imageData.length == 0) {
                System.err.println("Image data is empty");
                return null;
            }

            // Save binary data to temporary file
            File tempFile = File.createTempFile("temp_" + entityId, "_" + filename);
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(imageData);
            }

            System.out.println("Uploading binary image (" + entityType + ") to Cloudflare R2: " + filename);
            String imageUrl = uploadService.uploadEventImage(tempFile, entityId.toString());

            // Clean up temp file
            tempFile.delete();

            System.out.println("✓ Binary image uploaded to R2: " + imageUrl);
            return imageUrl;
        } catch (Exception e) {
            System.err.println("✗ Failed to upload binary image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public byte[] getImageData(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                System.err.println("Image path is empty");
                return null;
            }

            // For R2 URLs (both r2.dev and r2.cloudflarestorage.com), download from public URL
            if (imagePath.contains("r2.dev") || imagePath.contains("r2.cloudflarestorage.com")) {
                System.out.println("Downloading image from R2: " + imagePath);
                return downloadFromUrl(imagePath);
            } else {
                // Fallback for local files
                File imageFile = new File(imagePath);
                if (!imageFile.exists()) {
                    System.err.println("Image file not found: " + imagePath);
                    return null;
                }
                return java.nio.file.Files.readAllBytes(imageFile.toPath());
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to read image data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean deleteImage(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                System.err.println("Image path is empty");
                return false;
            }

            // For R2 URLs (both r2.dev and r2.cloudflarestorage.com), call delete on upload service
            if (imagePath.contains("r2.dev") || imagePath.contains("r2.cloudflarestorage.com")) {
                System.out.println("Deleting image from R2: " + imagePath);
                uploadService.deleteEventImage(imagePath);
                System.out.println("✓ Image deleted from R2");
                return true;
            } else {
                // Fallback for local files
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    if (deleted) {
                        System.out.println("✓ Image deleted: " + imagePath);
                    }
                    return deleted;
                }
                return false;
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to delete image: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean imageExists(String imagePath) {
        try {
            if (imagePath == null || imagePath.isEmpty()) {
                return false;
            }

            // For R2 URLs (both r2.dev and r2.cloudflarestorage.com), check if URL returns 200
            if (imagePath.contains("r2.dev") || imagePath.contains("r2.cloudflarestorage.com")) {
                return checkUrlExists(imagePath);
            } else {
                // Fallback for local files
                File imageFile = new File(imagePath);
                return imageFile.exists();
            }
        } catch (Exception e) {
            System.err.println("✗ Failed to check image existence: " + e.getMessage());
            return false;
        }
    }

    /**
     * Download image data from R2 URL
     */
    private byte[] downloadFromUrl(String urlString) throws Exception {
        java.net.URL url = new java.net.URL(urlString);
        try (java.io.InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }

    /**
     * Check if R2 URL exists (returns 200)
     */
    private boolean checkUrlExists(String urlString) throws Exception {
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(urlString).openConnection();
        try {
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } finally {
            connection.disconnect();
        }
    }
}

