package org.ems.application.service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Service for uploading images to Cloudflare R2 storage
 * Uses AWS SDK for proper AWS Signature V4 signing
 */
public class ImageUploadService {

    // Cloudflare R2 Configuration
    private static final String R2_ENDPOINT = "https://a4652a07eb284b5295b7e710ff59a071.r2.cloudflarestorage.com";
    private static final String R2_PUBLIC_ENDPOINT = "https://pub-9aa0fc184b60436e9347d729de11e4a5.r2.dev";  // Public domain for image loading
    private static final String R2_BUCKET_NAME = "event-manager-system";
    private static final String R2_REGION = "us-east-1";

    // Cloudflare R2 Credentials
    private static final String R2_ACCESS_KEY = "2d3d57f1707cd1cfab0b081439bcd6bc";
    private static final String R2_SECRET_KEY = "421cd5492717d2df7a14b67c6da34194dd56ba64547f181f8022562b6e822c03";

    private S3Client s3Client;

    /**
     * Constructor - Initialize S3 client for Cloudflare R2
     */
    public ImageUploadService() {
        try {
            this.s3Client = S3Client.builder()
                    .region(Region.US_EAST_1)
                    .endpointOverride(java.net.URI.create(R2_ENDPOINT))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(R2_ACCESS_KEY, R2_SECRET_KEY)
                    ))
                    .build();

            System.out.println("✓ AWS SDK S3 Client initialized for Cloudflare R2");
            System.out.println("✓ R2 Endpoint: " + R2_ENDPOINT);
            System.out.println("✓ R2 Bucket: " + R2_BUCKET_NAME);
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize S3 client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Upload event image to Cloudflare R2
     *
     * @param imageFile The image file to upload
     * @param eventId The event ID for organizing storage
     * @return The public URL of the uploaded image
     * @throws Exception if upload fails
     */
    public String uploadEventImage(File imageFile, String eventId) throws Exception {
        if (imageFile == null || !imageFile.exists()) {
            throw new IllegalArgumentException("Image file does not exist");
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID is required");
        }

        if (s3Client == null) {
            throw new RuntimeException("S3 client not initialized");
        }

        try {
            // Generate unique filename
            String fileExtension = getFileExtension(imageFile.getName());
            String uniqueFileName = generateUniqueFileName(fileExtension);
            // Key KHÔNG bao gồm bucket name - chỉ là object path
            String s3Key = "event-manager-system/events/" + eventId + "/" + uniqueFileName;

            System.out.println("Uploading to R2: " + s3Key);
            System.out.println("File size: " + imageFile.length() + " bytes");

            // Upload using AWS SDK (proper Signature V4)
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(R2_BUCKET_NAME)
                    .key(s3Key)
                    .acl("public-read")
                    .build();

            s3Client.putObject(putRequest, Paths.get(imageFile.getAbsolutePath()));

            // Return public R2.dev URL in CORRECT format: https://pub-9aa0fc184b60436e9347d729de11e4a5.r2.dev/ + key
            String publicUrl = "https://pub-9aa0fc184b60436e9347d729de11e4a5.r2.dev/" + s3Key;
            System.out.println("✓ Image uploaded successfully: " + publicUrl);
            return publicUrl;

        } catch (Exception e) {
            System.err.println("✗ Failed to upload image: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Image upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete image from Cloudflare R2
     *
     * @param imageUrl The public URL of the image to delete
     */
    public void deleteEventImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            if (s3Client == null) {
                System.err.println("✗ S3 client not initialized");
                return;
            }

            // Extract S3 key from URL
            String s3Key = imageUrl.replace(R2_ENDPOINT + "/" + R2_BUCKET_NAME + "/", "");

            s3Client.deleteObject(builder -> builder
                    .bucket(R2_BUCKET_NAME)
                    .key(s3Key)
            );

            System.out.println("✓ Image deleted successfully: " + imageUrl);

        } catch (Exception e) {
            System.err.println("✗ Failed to delete image: " + e.getMessage());
        }
    }

    /**
     * Generate unique filename
     */
    private String generateUniqueFileName(String extension) {
        return UUID.randomUUID() + "_" + System.currentTimeMillis() + extension;
    }

    /**
     * Extract file extension
     */
    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return ".jpg";
    }

    /**
     * Validate image file
     */
    public boolean isValidImageFile(File file) {
        if (file == null || !file.exists()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") ||
                fileName.endsWith(".gif") ||
                fileName.endsWith(".webp");
    }

    /**
     * Get maximum file size (10MB)
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Check if file size is valid
     */
    public boolean isValidFileSize(File file) {
        return file != null && file.length() <= MAX_FILE_SIZE;
    }

    /**
     * Cleanup resources
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            System.out.println("✓ S3 client closed");
        }
    }
}

import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Service for uploading images to Cloudflare R2 (S3-compatible storage)
 */
public class ImageUploadService implements AutoCloseable {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrl;

    // Cloudflare R2 Configuration
    private static final String R2_ACCOUNT_ID = "your_account_id";
    private static final String R2_ACCESS_KEY = "your_access_key";
    private static final String R2_SECRET_KEY = "your_secret_key";
    private static final String R2_BUCKET_NAME = "event-manager-images";
    private static final String R2_PUBLIC_URL = "https://pub-your_id.r2.dev";

    public ImageUploadService() {
        try {
            // Initialize S3 client for Cloudflare R2
            String endpoint = String.format("https://%s.r2.cloudflarestorage.com", R2_ACCOUNT_ID);

            AwsBasicCredentials credentials = AwsBasicCredentials.create(R2_ACCESS_KEY, R2_SECRET_KEY);

            this.s3Client = S3Client.builder()
                    .region(Region.US_EAST_1) // R2 uses auto region
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .endpointOverride(URI.create(endpoint))
                    .build();

            this.bucketName = R2_BUCKET_NAME;
            this.publicUrl = R2_PUBLIC_URL;

            System.out.println("✓ ImageUploadService initialized with Cloudflare R2");
        } catch (Exception e) {
            System.err.println("✗ Failed to initialize ImageUploadService: " + e.getMessage());
            throw new RuntimeException("Failed to initialize R2 client", e);
        }
    }

    /**
     * Upload event image to R2
     */
    public String uploadEventImage(File imageFile, String eventId) {
        try {
            String key = "events/" + eventId + "/" + generateUniqueFilename(imageFile.getName());
            return uploadFile(imageFile, key);
        } catch (Exception e) {
            System.err.println("Failed to upload event image: " + e.getMessage());
            throw new RuntimeException("Event image upload failed", e);
        }
    }

    /**
     * Upload session material to R2
     */
    public String uploadSessionMaterial(File file, String sessionId) {
        try {
            String key = "sessions/" + sessionId + "/" + generateUniqueFilename(file.getName());
            return uploadFile(file, key);
        } catch (Exception e) {
            System.err.println("Failed to upload session material: " + e.getMessage());
            throw new RuntimeException("Session material upload failed", e);
        }
    }

    /**
     * Upload ticket QR code to R2
     */
    public String uploadTicketQRCode(File qrFile, String ticketId) {
        try {
            String key = "tickets/" + ticketId + "/" + generateUniqueFilename(qrFile.getName());
            return uploadFile(qrFile, key);
        } catch (Exception e) {
            System.err.println("Failed to upload QR code: " + e.getMessage());
            throw new RuntimeException("QR code upload failed", e);
        }
    }

    /**
     * Generic file upload to R2
     */
    private String uploadFile(File file, String key) throws IOException {
        byte[] fileData = Files.readAllBytes(file.toPath());
        String contentType = determineContentType(file.getName());

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(fileData));

        // Return public URL
        return publicUrl + "/" + key;
    }

    /**
     * Delete image from R2
     */
    public void deleteEventImage(String imageUrl) {
        try {
            String key = extractKeyFromUrl(imageUrl);
            if (key != null) {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteRequest);
                System.out.println("✓ Deleted image: " + key);
            }
        } catch (Exception e) {
            System.err.println("Failed to delete image: " + e.getMessage());
        }
    }

    /**
     * Validate image file
     */
    public boolean isValidImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".gif") ||
                name.endsWith(".webp");
    }

    /**
     * Validate file size (max 10MB)
     */
    public boolean isValidFileSize(File file) {
        return file.length() <= 10 * 1024 * 1024;
    }

    /**
     * Generate unique filename
     */
    private String generateUniqueFilename(String originalName) {
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalName.substring(lastDot);
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Determine content type from filename
     */
    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    /**
     * Extract key from R2 URL
     */
    private String extractKeyFromUrl(String url) {
        if (url == null || !url.contains(publicUrl)) return null;
        return url.substring(url.indexOf(publicUrl) + publicUrl.length() + 1);
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
            System.out.println("✓ ImageUploadService closed");
        }
    }
}
