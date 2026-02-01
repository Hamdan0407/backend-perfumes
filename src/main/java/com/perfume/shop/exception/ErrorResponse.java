package com.perfume.shop.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive error response DTO with detailed error information.
 * 
 * Includes:
 * - HTTP status code
 * - Error message
 * - Error type classification
 * - Field-level validation errors
 * - Timestamp for debugging
 * - Request path for traceability
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * HTTP status code (e.g., 400, 401, 404, 500)
     */
    private int status;
    
    /**
     * Error type classification for client-side handling
     * Examples: VALIDATION_ERROR, AUTHENTICATION_ERROR, AUTHORIZATION_ERROR, NOT_FOUND, CONFLICT, INTERNAL_ERROR
     */
    private String errorType;
    
    /**
     * User-friendly error message
     */
    private String message;
    
    /**
     * Field-level validation errors (for VALIDATION_ERROR only)
     * Format: {"fieldName": "error message", ...}
     */
    private Map<String, String> fieldErrors;
    
    /**
     * List of validation errors with additional context
     */
    private List<FieldError> errors;
    
    /**
     * Request path that caused the error (for debugging)
     */
    private String path;
    
    /**
     * Timestamp when error occurred (ISO 8601 format)
     */
    private LocalDateTime timestamp;
    
    /**
     * Unique request ID for tracing (optional)
     */
    private String requestId;
    
    /**
     * Build error response with all required fields
     */
    public static ErrorResponse of(int status, String errorType, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .errorType(errorType)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Build error response with field errors for validation failures
     */
    public static ErrorResponse ofValidation(String message, Map<String, String> fieldErrors, String path) {
        return ErrorResponse.builder()
                .status(400)
                .errorType(ErrorType.VALIDATION_ERROR.name())
                .message(message)
                .fieldErrors(fieldErrors)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Represents a single validation error with additional context
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private String rejectedValue;
    }
}
