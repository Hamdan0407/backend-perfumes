package com.perfume.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * CDN Configuration Properties
 * 
 * Configures CDN settings for serving static assets (images, videos, etc.)
 * Supports local storage, AWS S3, and Cloudflare R2
 */
@Configuration
@ConfigurationProperties(prefix = "app.cdn")
@Data
public class CdnConfig {

    /**
     * Enable/disable CDN usage
     * If false, images will be served from local server
     */
    private boolean enabled = false;

    /**
     * CDN base URL (e.g., https://cdn.yourdomain.com or https://pub-xxxxx.r2.dev)
     */
    private String baseUrl = "";

    /**
     * CDN provider: local, s3, r2, cloudflare
     */
    private String provider = "local";

    /**
     * Get full CDN URL for a given path
     * 
     * @param path Relative path (e.g., /products/image.jpg)
     * @return Full CDN URL or local path
     */
    public String getUrl(String path) {
        if (!enabled || baseUrl == null || baseUrl.isEmpty()) {
            return path;
        }

        // Remove leading slash if present
        String cleanPath = path.startsWith("/") ? path.substring(1) : path;

        // Ensure base URL doesn't end with slash
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        return cleanBaseUrl + "/" + cleanPath;
    }

    /**
     * Check if CDN is enabled and configured
     */
    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isEmpty();
    }
}
