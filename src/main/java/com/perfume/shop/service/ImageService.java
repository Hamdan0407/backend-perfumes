package com.perfume.shop.service;

import com.perfume.shop.config.CdnConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Image Service
 * 
 * Handles image URL generation with CDN support
 * Automatically prepends CDN base URL when CDN is enabled
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final CdnConfig cdnConfig;

    /**
     * Get full image URL with CDN support
     * 
     * @param imagePath Relative image path (e.g., /products/chanel-no5.jpg)
     * @return Full URL (CDN or local)
     */
    public String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return getPlaceholderUrl();
        }

        // If already a full URL, return as-is
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            return imagePath;
        }

        // Use CDN if configured
        if (cdnConfig.isConfigured()) {
            String cdnUrl = cdnConfig.getUrl(imagePath);
            log.debug("Generated CDN URL: {} -> {}", imagePath, cdnUrl);
            return cdnUrl;
        }

        // Return local path
        return imagePath;
    }

    /**
     * Get product image URL
     * 
     * @param productId Product ID
     * @param imageType Type of image (main, thumb, gallery-1, etc.)
     * @return Full image URL
     */
    public String getProductImageUrl(Long productId, String imageType) {
        String imagePath = String.format("/products/product-%d-%s.jpg", productId, imageType);
        return getImageUrl(imagePath);
    }

    /**
     * Get product main image URL
     */
    public String getProductMainImage(Long productId) {
        return getProductImageUrl(productId, "main");
    }

    /**
     * Get product thumbnail URL
     */
    public String getProductThumbnail(Long productId) {
        return getProductImageUrl(productId, "thumb");
    }

    /**
     * Get placeholder image URL
     */
    public String getPlaceholderUrl() {
        return getImageUrl("/images/placeholder.jpg");
    }

    /**
     * Get WebP version of image if available
     * Falls back to original if WebP not found
     */
    public String getWebPUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return getPlaceholderUrl();
        }

        // Replace extension with .webp
        String webpPath = imagePath.replaceAll("\\.(jpg|jpeg|png)$", ".webp");
        return getImageUrl(webpPath);
    }

    /**
     * Check if CDN is enabled
     */
    public boolean isCdnEnabled() {
        return cdnConfig.isConfigured();
    }

    /**
     * Get CDN base URL
     */
    public String getCdnBaseUrl() {
        return cdnConfig.getBaseUrl();
    }
}
