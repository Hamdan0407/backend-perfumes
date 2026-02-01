package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Tracks the state of a conversation with the chatbot
 * Maintains user preferences and conversation progress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationContext {
    
    /**
     * Conversation stage enum - defines the flow
     */
    public enum ConversationStage {
        INITIAL("initial", "Just started"),
        OCCASION_GATHERING("occasion", "Asking about occasion"),
        SCENT_TYPE_GATHERING("scent_type", "Asking about scent preference"),
        BUDGET_GATHERING("budget", "Asking about budget"),
        RECOMMENDATION_PHASE("recommendation", "Providing recommendations"),
        FOLLOW_UP("follow_up", "Follow-up conversation"),
        COMPLETE("complete", "Conversation complete");
        
        public final String code;
        public final String description;
        
        ConversationStage(String code, String description) {
            this.code = code;
            this.description = description;
        }
    }
    
    // Conversation metadata
    private String conversationId;
    private Long startTime;
    private Long lastInteraction;
    private Integer messageCount;
    
    // Current conversation stage
    private ConversationStage currentStage;
    
    // Collected user preferences
    @Builder.Default
    private Map<String, String> preferences = new HashMap<>();
    
    // Conversation history (message exchanges)
    @Builder.Default
    private List<ConversationMessage> messages = new ArrayList<>();
    
    // Tracking what has been asked
    @Builder.Default
    private Map<String, Boolean> questionsAsked = new HashMap<>();
    
    // Store generated recommendations for follow-up questions
    @Builder.Default
    private List<String> storedRecommendations = new ArrayList<>();
    
    // Store recommended product IDs for detailed lookups
    @Builder.Default
    private Map<String, Long> recommendedProductIds = new HashMap<>();
    
    /**
     * Store a recommendation with its product ID
     */
    public void addRecommendation(String perfumeName, Long productId) {
        if (storedRecommendations == null) {
            storedRecommendations = new ArrayList<>();
        }
        if (recommendedProductIds == null) {
            recommendedProductIds = new HashMap<>();
        }
        storedRecommendations.add(perfumeName);
        recommendedProductIds.put(perfumeName.toLowerCase(), productId);
    }
    
    /**
     * Store a recommendation (perfume name) - legacy method
     */
    public void addRecommendation(String perfumeName) {
        if (storedRecommendations == null) {
            storedRecommendations = new ArrayList<>();
        }
        storedRecommendations.add(perfumeName);
    }
    
    /**
     * Get product ID by name
     */
    public Long getProductIdByName(String perfumeName) {
        if (recommendedProductIds == null) {
            return null;
        }
        return recommendedProductIds.get(perfumeName.toLowerCase());
    }
    
    /**
     * Get all stored recommendations
     */
    public List<String> getStoredRecommendations() {
        return storedRecommendations != null ? storedRecommendations : new ArrayList<>();
    }
    
    /**
     * Check if recommendations have been stored
     */
    public boolean hasRecommendations() {
        return storedRecommendations != null && !storedRecommendations.isEmpty();
    }
    
    /**
     * Check if a specific preference has been collected
     */
    public boolean hasPreference(String key) {
        return preferences != null && preferences.containsKey(key) && 
               preferences.get(key) != null && !preferences.get(key).isEmpty();
    }
    
    /**
     * Get a preference value
     */
    public String getPreference(String key) {
        return preferences != null ? preferences.get(key) : null;
    }
    
    /**
     * Set a preference
     */
    public void setPreference(String key, String value) {
        if (preferences == null) {
            preferences = new HashMap<>();
        }
        preferences.put(key, value);
    }
    
    /**
     * Check if a question was already asked
     */
    public boolean isQuestionAsked(String questionId) {
        return questionsAsked != null && questionsAsked.getOrDefault(questionId, false);
    }
    
    /**
     * Mark a question as asked
     */
    public void markQuestionAsked(String questionId) {
        if (questionsAsked == null) {
            questionsAsked = new HashMap<>();
        }
        questionsAsked.put(questionId, true);
    }
    
    /**
     * Add a message to conversation history
     */
    public void addMessage(ConversationMessage message) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.add(message);
    }
    
    /**
     * Get conversation progress as percentage
     */
    public int getProgressPercentage() {
        int progress = 0;
        if (hasPreference("occasion")) progress += 25;
        if (hasPreference("scent_type")) progress += 25;
        if (hasPreference("budget")) progress += 25;
        if (currentStage == ConversationStage.RECOMMENDATION_PHASE || 
            currentStage == ConversationStage.COMPLETE) progress += 25;
        return progress;
    }
    
    /**
     * Inner class for conversation messages
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private String role; // "user" or "assistant"
        private String content;
        private Long timestamp;
        private String context; // Optional context about the message
    }
}
