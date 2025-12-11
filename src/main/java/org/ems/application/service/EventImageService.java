package org.ems.application.service;

import javafx.scene.image.Image;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * EventImageService - Handles image loading and caching
 * Single Responsibility: Image loading + caching only
 *
 * @author EMS Team
 */
public class EventImageService {

    private final ImageService imageService;
    private final Map<UUID, Image> imageCache = new HashMap<>();

    public EventImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Load image from path (R2 or local file)
     * Returns cached image if available
     */
    public Image loadImage(String imagePath, UUID cacheKeyId) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        // Check cache
        if (cacheKeyId != null && imageCache.containsKey(cacheKeyId)) {
            System.out.println("[EventImageService] Image cache hit for " + cacheKeyId);
            return imageCache.get(cacheKeyId);
        }

        try {
            Image image = null;

            // R2 URL (Cloudflare)
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                System.out.println("[EventImageService] Loading image from R2/URL: " + imagePath);
                image = new Image(imagePath, true);  // true = async loading
            }
            // Local file
            else {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    System.out.println("[EventImageService] Loading image from file: " + imagePath);
                    image = new Image(new FileInputStream(imageFile));
                }
            }

            // Cache if successful
            if (image != null && cacheKeyId != null) {
                imageCache.put(cacheKeyId, image);
                System.out.println("[EventImageService] Image cached for " + cacheKeyId);
            }

            return image;

        } catch (Exception e) {
            System.err.println("[EventImageService] Error loading image: " + imagePath + " -> " + e.getMessage());
            return null;
        }
    }

    /**
     * Pre-cache an image
     */
    public void cacheImage(UUID id, Image image) {
        if (id != null && image != null) {
            imageCache.put(id, image);
        }
    }

    /**
     * Check if image is cached
     */
    public boolean isImageCached(UUID id) {
        return id != null && imageCache.containsKey(id);
    }

    /**
     * Get cached image without loading
     */
    public Image getImageFromCache(UUID id) {
        return id != null ? imageCache.get(id) : null;
    }

    /**
     * Clear cache
     */
    public void clearCache() {
        imageCache.clear();
        System.out.println("[EventImageService] Image cache cleared");
    }
}

