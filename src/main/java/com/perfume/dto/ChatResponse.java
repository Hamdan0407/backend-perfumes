package com.perfume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Response DTO
 * Represents the chatbot's response to user
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    
    /**
     * The bot's message response
     */
    private String message;
    
    /**
     * Response status (success, error, etc.)
     */
    private String status;
    
    /**
     * Timestamp of the response
     */
    private Long timestamp;
    
    /**
     * Optional recommended products (JSON array)
     */
    private String recommendedProducts;
    
    /**
     * Confidence score of the response (0-1)
     */
    private Double confidence;
}
