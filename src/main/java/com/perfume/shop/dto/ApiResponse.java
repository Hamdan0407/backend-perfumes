package com.perfume.shop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper for all successful responses.
 * 
 * Provides consistent response format across all endpoints:
 * - Boolean success flag
 * - User-friendly message
 * - Response data (payload)
 * - Timestamp for tracing
 * 
 * Example success response:
 * {
 *   "success": true,
 *   "message": "Login successful",
 *   "data": {
 *     "token": "...",
 *     "email": "user@example.com"
 *   },
 *   "timestamp": "2024-01-19T10:30:00"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    
    /**
     * Indicates if operation was successful
     */
    private Boolean success;
    
    /**
     * User-friendly message describing the result
     */
    private String message;
    
    /**
     * Response payload (data, object, list, etc.)
     */
    private Object data;
    
    /**
     * Timestamp when response was generated
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Create success response with message only
     */
    public static ApiResponse success(String message) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create success response with message and data
     */
    public static ApiResponse success(String message, Object data) {
        return ApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with message only
     */
    public static ApiResponse error(String message) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with message and error details
     */
    public static ApiResponse error(String message, Object errorDetails) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .data(errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
