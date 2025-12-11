package org.ems.infrastructure.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Utility class for handling session material uploads and storage.
 * Mirrors logic from ImageUploadUtil but for materials (PDF, PPT, DOCX, etc).
 */
public class SessionMaterialUploadUtil {

    private static final String MATERIAL_UPLOAD_DIR = "session_materials";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "ppt", "pptx", "doc", "docx", "xls", "xlsx", "txt");
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB

    static {
        try {
            Files.createDirectories(Paths.get(MATERIAL_UPLOAD_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create material upload directory: " + e.getMessage());
        }
    }

    /**
     * Upload a material file and return its stored path.
     *
     * @param filePath Path to the material file
     * @param sessionId Session ID
     * @return Stored material path or null if upload fails
     */
    public static String uploadMaterial(String filePath, String sessionId) {
        try {
            File sourceFile = new File(filePath);
            if (!sourceFile.exists()) {
                throw new FileNotFoundException("Source file not found: " + filePath);
            }

            // Validate file
            validateMaterialFile(sourceFile);

            // Create session-specific directory
            String sessionDir = MATERIAL_UPLOAD_DIR + File.separator + sessionId;
            Files.createDirectories(Paths.get(sessionDir));

            // Generate unique filename
            String filename = generateFilename(sourceFile.getName());
            String destinationPath = sessionDir + File.separator + filename;

            // Copy file
            Files.copy(
                    sourceFile.toPath(),
                    Paths.get(destinationPath),
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("Material uploaded successfully: " + destinationPath);
            return destinationPath;

        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Material upload failed: " + e.getMessage());
            return null;
        }
    }

    private static void validateMaterialFile(File file) throws IllegalArgumentException, IOException {
        if (file.length() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        String filename = file.getName().toLowerCase();
        boolean isValidExtension = ALLOWED_EXTENSIONS.stream()
                .anyMatch(ext -> filename.endsWith("." + ext));
        if (!isValidExtension) {
            throw new IllegalArgumentException("File format not supported. Allowed formats: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private static String generateFilename(String originalFilename) {
        String timestamp = System.currentTimeMillis() + "";
        String uuid = UUID.randomUUID().toString();
        int lastDotIndex = originalFilename.lastIndexOf('.');
        String extension = lastDotIndex > 0 ? originalFilename.substring(lastDotIndex) : "";
        return uuid + "_" + timestamp + extension;
    }
}

