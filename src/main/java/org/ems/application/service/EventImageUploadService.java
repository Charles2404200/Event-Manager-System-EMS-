package org.ems.application.service;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * EventImageUploadService - Handles image file selection and upload
 * Single Responsibility: Image file handling and upload only
 *
 * @author EMS Team
 */
public class EventImageUploadService {

    private final ImageService imageService;

    public EventImageUploadService(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Open file chooser and select image file
     */
    public File selectImageFile(Stage parentStage) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Event Image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File selectedFile = fileChooser.showOpenDialog(parentStage);
            if (selectedFile != null && selectedFile.exists()) {
                System.out.println("[EventImageUploadService] Selected image: " + selectedFile.getAbsolutePath());
                return selectedFile;
            }
            return null;

        } catch (Exception e) {
            System.err.println("Error selecting image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Upload image for event and return the public R2 URL
     * @return Public R2 URL if successful, null if failed
     */
    public String uploadImage(String imagePath, java.util.UUID eventId) {
        if (imagePath == null || imagePath.isEmpty() || imageService == null) {
            System.out.println("[EventImageUploadService] No image to upload or ImageService is null");
            return null;
        }

        try {
            long start = System.currentTimeMillis();
            String publicUrl = imageService.uploadEventImage(imagePath, eventId);
            System.out.println("[EventImageUploadService] uploadImage completed in " +
                    (System.currentTimeMillis() - start) + " ms, url=" + publicUrl);
            return publicUrl;

        } catch (Exception e) {
            System.err.println("Error uploading image: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get file name from path
     */
    public String getFileName(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }
        return new File(imagePath).getName();
    }
}

