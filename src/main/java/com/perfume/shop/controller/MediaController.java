package com.perfume.shop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Media management controller for uploading and serving images.
 * Files are stored on server filesystem (served via Nginx).
 * 
 * Architecture:
 * - Files uploaded to /tmp/media (or configured MEDIA_UPLOAD_DIR)
 * - Nginx serves from https://yourdomain.com/media/*
 * - This controller stores and validates uploads
 */
@RestController
@RequestMapping("/api/media")
@Slf4j
public class MediaController {

    @Value("${app.media.upload-dir:./media}")
    private String uploadDir;

    @Value("${app.media.url:/media}")
    private String mediaUrl;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
            "image/jpeg", "image/png", "image/gif", "image/webp"
    };

    /**
     * Upload an image file.
     * Stores on server filesystem (nginx will serve from there).
     * 
     * @param file Image file (jpeg, png, gif, webp only)
     * @return URL of uploaded file
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file is not empty
            if (file.isEmpty()) {
                log.warn("Upload attempt with empty file");
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
            }

            // Validate file type
            String contentType = file.getContentType();
            if (!isValidImageType(contentType)) {
                log.warn("Invalid file type uploaded: {}", contentType);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Only JPEG, PNG, GIF, and WebP images are allowed"));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("File size exceeds limit: {} bytes", file.getSize());
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("File size exceeds 10MB limit"));
            }

            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created media upload directory: {}", uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                log.warn("Invalid filename: {}", originalFilename);
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid file name"));
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID() + fileExtension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, file.getBytes());

            // Return file URL for frontend
            String fileUrl = mediaUrl + "/" + uniqueFilename;

            log.info("File uploaded successfully: {} ({})", fileUrl, file.getSize());

            return ResponseEntity.ok(new UploadResponse(
                    fileUrl,
                    uniqueFilename,
                    file.getSize()));

        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("File upload failed: " + e.getMessage()));
        }
    }

    /**
     * Download/serve an image file
     * Note: In production, Nginx serves directly (better performance)
     * This is a fallback endpoint
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            // Security: Prevent directory traversal
            if (filename.contains("..")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                log.warn("File not found: {}", filename);
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000") // 30 days
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("File download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete an image file (for cleanup purposes)
     * Can be called by admin or file owner
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            // Security: Prevent directory traversal
            if (filename.contains("..")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid filename"));
            }

            Path filePath = Paths.get(uploadDir).resolve(filename);

            if (!Files.exists(filePath)) {
                log.warn("File not found for deletion: {}", filename);
                return ResponseEntity.notFound().build();
            }

            Files.delete(filePath);
            log.info("File deleted: {}", filename);

            return ResponseEntity.ok(new MessageResponse("File deleted successfully"));

        } catch (IOException e) {
            log.error("File deletion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("File deletion failed: " + e.getMessage()));
        }
    }

    /**
     * Check if content type is a valid image format.
     * 
     * @param contentType MIME type of the file
     * @return true if valid image type, false otherwise
     */
    private boolean isValidImageType(String contentType) {
        if (contentType == null) {
            return false;
        }

        for (String allowedType : ALLOWED_CONTENT_TYPES) {
            if (allowedType.equals(contentType)) {
                return true;
            }
        }
        return false;
    }

    // ==========================================
    // Response DTOs
    // ==========================================

    public static class UploadResponse {
        public String url;
        public String filename;
        public long size;

        public UploadResponse(String url, String filename, long size) {
            this.url = url;
            this.filename = filename;
            this.size = size;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }
}
