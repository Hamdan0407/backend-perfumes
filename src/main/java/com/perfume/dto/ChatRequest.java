package com.perfume.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat Request DTO
 * Represents a user message sent to the chatbot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    
    /**
     * The user's message
     */
    private String message;
    
    /**
     * Previous conversation history for context
     */
    private String conversationHistory;
    
    /**
     * Optional user ID for personalized responses
     */
    private String userId;
    
    /**
     * Session ID for tracking conversation
     */
    private String sessionId;
}
