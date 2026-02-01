package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chatbot interactions
 * Includes conversation context for stateful conversations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String message;
    private String status;
    private Long timestamp;
    
    // Conversation state information
    private String conversationId; // Session identifier
    private ConversationContext context; // Current conversation context
    private Integer progressPercentage; // Conversation progress (0-100)
}
