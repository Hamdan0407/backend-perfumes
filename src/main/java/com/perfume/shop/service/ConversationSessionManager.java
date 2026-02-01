package com.perfume.shop.service;

import com.perfume.shop.dto.ConversationContext;
import com.perfume.shop.dto.ConversationContext.ConversationMessage;
import com.perfume.shop.dto.ConversationContext.ConversationStage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages conversation sessions for the chatbot
 * Maintains conversation state across multiple messages
 */
@Slf4j
@Service
public class ConversationSessionManager {
    
    // In-memory store of conversation contexts (could be extended to use database)
    private final Map<String, ConversationContext> sessions = new ConcurrentHashMap<>();
    
    // Session timeout in milliseconds (30 minutes)
    private static final long SESSION_TIMEOUT = 30 * 60 * 1000;
    
    /**
     * Get or create a conversation context for a session
     */
    public ConversationContext getOrCreateSession(String conversationId) {
        if (conversationId == null) {
            conversationId = UUID.randomUUID().toString();
        }
        
        ConversationContext context = sessions.get(conversationId);
        
        if (context == null) {
            context = ConversationContext.builder()
                .conversationId(conversationId)
                .currentStage(ConversationStage.INITIAL)
                .startTime(System.currentTimeMillis())
                .messageCount(0)
                .preferences(new HashMap<>())
                .messages(new ArrayList<>())
                .questionsAsked(new HashMap<>())
                .build();
            sessions.put(conversationId, context);
            log.info("Created new conversation session: {}", conversationId);
        }
        
        return context;
    }
    
    /**
     * Update conversation context after processing user message
     */
    public void updateSession(String conversationId, ConversationContext context) {
        context.setLastInteraction(System.currentTimeMillis());
        context.setMessageCount((context.getMessageCount() != null ? context.getMessageCount() : 0) + 1);
        sessions.put(conversationId, context);
    }
    
    /**
     * Add a message to the conversation history
     */
    public void addMessage(String conversationId, String role, String content, String context) {
        ConversationContext session = getOrCreateSession(conversationId);
        
        ConversationMessage message = ConversationMessage.builder()
            .role(role)
            .content(content)
            .timestamp(System.currentTimeMillis())
            .context(context)
            .build();
        
        session.addMessage(message);
        updateSession(conversationId, session);
    }
    
    /**
     * Get full conversation history as formatted string
     */
    public String getConversationHistoryAsString(String conversationId) {
        ConversationContext context = sessions.get(conversationId);
        if (context == null || context.getMessages() == null) {
            return "";
        }
        
        StringBuilder history = new StringBuilder();
        for (ConversationMessage msg : context.getMessages()) {
            history.append(msg.getRole().equals("user") ? "User: " : "Assistant: ")
                   .append(msg.getContent())
                   .append("\n");
        }
        return history.toString();
    }
    
    /**
     * Determine next conversation stage based on collected preferences
     */
    public ConversationStage determineNextStage(ConversationContext context) {
        if (!context.hasPreference("occasion")) {
            return ConversationStage.OCCASION_GATHERING;
        }
        if (!context.hasPreference("scent_type")) {
            return ConversationStage.SCENT_TYPE_GATHERING;
        }
        if (!context.hasPreference("budget")) {
            return ConversationStage.BUDGET_GATHERING;
        }
        
        // All preferences collected, move to recommendations
        return ConversationStage.RECOMMENDATION_PHASE;
    }
    
    /**
     * Clean up expired sessions
     */
    public void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getLastInteraction() > SESSION_TIMEOUT
        );
    }
    
    /**
     * Get session by ID
     */
    public Optional<ConversationContext> getSession(String conversationId) {
        return Optional.ofNullable(sessions.get(conversationId));
    }
    
    /**
     * Extract preference value from user message using keyword detection
     */
    public String extractPreferenceValue(String message, String preferenceType) {
        String lower = message.toLowerCase();
        
        switch (preferenceType) {
            case "occasion":
                if (lower.contains("work") || lower.contains("office") || lower.contains("professional")) 
                    return "professional";
                if (lower.contains("date") || lower.contains("romantic") || lower.contains("evening")) 
                    return "romantic";
                if (lower.contains("casual") || lower.contains("everyday") || lower.contains("day")) 
                    return "casual";
                if (lower.contains("party") || lower.contains("night out") || lower.contains("club")) 
                    return "party";
                if (lower.contains("summer") || lower.contains("warm")) 
                    return "summer";
                if (lower.contains("winter") || lower.contains("cold")) 
                    return "winter";
                break;
                
            case "scent_type":
                if (lower.contains("floral") || lower.contains("flower") || lower.contains("rose") || lower.contains("jasmine"))
                    return "floral";
                if (lower.contains("woody") || lower.contains("sandalwood") || lower.contains("cedar") || lower.contains("oud"))
                    return "woody";
                if (lower.contains("fresh") || lower.contains("citrus") || lower.contains("lemon") || lower.contains("bergamot"))
                    return "fresh";
                if (lower.contains("oriental") || lower.contains("amber") || lower.contains("vanilla") || lower.contains("musk"))
                    return "oriental";
                if (lower.contains("fruity") || lower.contains("berry") || lower.contains("apple") || lower.contains("peach"))
                    return "fruity";
                break;
                
            case "budget":
                if (lower.contains("budget") || lower.contains("cheap") || lower.contains("affordable") || 
                    lower.contains("₹1") || lower.contains("1500") || lower.contains("2500") || lower.contains("under") || lower.contains("low"))
                    return "budget-friendly";
                if (lower.contains("mid") || lower.contains("medium") || lower.contains("moderate") || 
                    lower.contains("3000") || lower.contains("5000") || lower.contains("4000") || lower.contains("medium-range"))
                    return "mid-range";
                if (lower.contains("luxury") || lower.contains("premium") || lower.contains("expensive") || 
                    lower.contains("high") || lower.contains("8000") || lower.contains("10000") || lower.contains("above") ||
                    lower.contains("premium luxury") || lower.contains("high-end"))
                    return "luxury";
                break;
        }
        
        return null;
    }
}
