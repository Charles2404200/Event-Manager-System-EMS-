package org.ems.infrastructure.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class for handling image uploads and storage.
 * Supports storing images as files or binary data.
 */
public class ImageUploadUtil {

    private static final String IMAGE_UPLOAD_DIR = "event_images";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    static {
        try {
            Files.createDirectories(Paths.get(IMAGE_UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create image upload directory: " + e.getMessage());
        }
    }

    /**
     * Upload an image file and return its stored path.
     *
     * @param filePath Path to the image file
     * @param entityId Entity ID (event, session, or ticket)
     * @param entityType Type of entity (EVENT, SESSION, TICKET)
     * @return Stored image path or null if upload fails
     */
    public static String uploadImage(String filePath, String entityId, String entityType) {
        try {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                throw new FileNotFoundException("Source file not found: " + filePath);
            }

            // Validate file
            validateImageFile(sourceFile);

            // Create entity-specific directory
            String entityDir = IMAGE_UPLOAD_DIR + File.separator + entityType.toLowerCase() + File.separator + entityId;
            Files.createDirectories(Paths.get(entityDir));

            // Generate unique filename
            String filename = generateFilename(sourceFile.getName());
            String destinationPath = entityDir + File.separator + filename;

            // Copy file
            Files.copy(
                    sourceFile.toPath(),
                    Paths.get(destinationPath),
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("Image uploaded successfully: " + destinationPath);
            return destinationPath;

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Image upload failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Read image file and return binary data.
     * Useful for storing in database.
     *
     * @param imagePath Path to the image file
     * @return Binary data of the image, or null if read fails
     */
    public static byte[] readImageAsBinary(String imagePath) {
        try {
            return Files.readAllBytes(Paths.get(imagePath));
        } catch (IOException e) {
            System.err.println("Failed to read image file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save binary image data to file and return the path.
     *
     * @param imageData Binary data of the image
     * @param entityId Entity ID
     * @param entityType Type of entity
     * @param filename Desired filename
     * @return Stored image path or null if save fails
     */
    public static String saveBinaryImageToFile(byte[] imageData, String entityId, String entityType, String filename) {
        try {
            String entityDir = IMAGE_UPLOAD_DIR + File.separator + entityType.toLowerCase() + File.separator + entityId;
            Files.createDirectories(Paths.get(entityDir));

            String destinationPath = entityDir + File.separator + filename;
            Files.write(Paths.get(destinationPath), imageData);

            System.out.println("Image saved successfully: " + destinationPath);
            return destinationPath;

        } catch (IOException e) {
            System.err.println("Failed to save image file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete an image file.
     *
     * @param imagePath Path to the image file
     * @return true if deletion successful, false otherwise
     */
    public static boolean deleteImage(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                Files.deleteIfExists(Paths.get(imagePath));
                System.out.println("Image deleted successfully: " + imagePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("Failed to delete image: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate if the file is a valid image.
     *
     * @param file File to validate
     * @throws IllegalArgumentException if file is not valid
     * @throws IOException if file size cannot be read
     */
    private static void validateImageFile(File file) throws IllegalArgumentException, IOException {
        // Check file size
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // Check file extension
        String filename = file.getName().toLowerCase();
        boolean isValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(ext -> filename.endsWith("." + ext));

        if (!isValidExtension) {
            throw new IllegalArgumentException("File format not supported. Allowed formats: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * Generate a unique filename to avoid conflicts.
     *
     * @param originalFilename Original filename
     * @return Generated unique filename
     */
    private static String generateFilename(String originalFilename) {
        String timestamp = System.currentTimeMillis() + "";
        String uuid = UUID.randomUUID().toString();
        int lastDotIndex = originalFilename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFilename.substring(lastDotIndex) : "";
        return uuid + "_" + timestamp + extension;
    }

    /**
     * Get image file from storage path.
     *
     * @param imagePath Storage path of the image
     * @return File object if exists, null otherwise
     */
    public static File getImageFile(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    return file;
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to get image file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if image file exists.
     *
     * @param imagePath Storage path of the image
     * @return true if image exists, false otherwise
     */
    public static boolean imageExists(String imagePath) {
        try {
            return imagePath != null && !imagePath.isEmpty() && Files.exists(Paths.get(imagePath));
        } catch (Exception e) {
            return false;
        }
    }
}

