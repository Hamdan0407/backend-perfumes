package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for chatbot interactions
 * Supports both simple string history and structured conversation context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String conversationHistory; // Legacy support for simple string history
    private String conversationId; // Unique conversation session identifier
    private ConversationContext context; // Full structured conversation context
}
