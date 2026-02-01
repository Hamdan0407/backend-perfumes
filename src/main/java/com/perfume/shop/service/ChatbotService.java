package com.perfume.shop.service;

import com.perfume.shop.dto.ConversationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Chatbot Service - Intelligent fragrance consultation with state management
 * Provides comprehensive scent recommendations with conversation state tracking
 * Uses finite state machine to maintain conversation flow and prevent question
 * repetition
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final ProductService productService;
    private final ConversationSessionManager conversationSessionManager;
    private final GeminiService geminiService;

    /**
     * Main chat endpoint with state management - handles customer queries while
     * maintaining conversation state
     * 
     * @param userMessage         Customer message or question
     * @param conversationHistory Previous conversation for context
     * @return Intelligent response based on message content and conversation state
     */
    public String chat(String userMessage, String conversationHistory) {
        // Default to new session if no conversationId provided
        return chat(userMessage, null, null);
    }

    /**
     * Main chat endpoint with full state management
     * 
     * @param userMessage    Customer message
     * @param conversationId Unique conversation session ID
     * @param context        Conversation context from previous messages
     * @return Intelligent response with state awareness
     */
    public String chat(String userMessage, String conversationId, ConversationContext context) {
        try {
            if (userMessage == null || userMessage.trim().isEmpty()) {
                return "Hello! 👋 How can I help you find your perfect fragrance today?";
            }

            // Get or create conversation session
            ConversationContext session = conversationSessionManager.getOrCreateSession(conversationId);

            // LOG conversation state for debugging
            log.info("=== CHATBOT STATE DEBUG ===");
            log.info("ConversationId: {}", conversationId);
            log.info("User Message: {}", userMessage);
            log.info("Current Stage: {}", session.getCurrentStage());
            log.info("Stored Preferences: {}", session.getPreferences());
            log.info("Stored Recommendations: {}", session.getStoredRecommendations());
            log.info("Questions Asked: {}", session.getQuestionsAsked());
            log.info("Message History Size: {}", session.getMessages().size());
            log.info("AI Enabled: {}", geminiService.isEnabled());
            log.info("===========================");

            // Extract and store preferences from user message
            extractAndStorePreferences(userMessage, session);

            // Check for direct product lookup intent (price, product info, availability)
            // This takes priority over AI to provide instant, accurate product data
            String directResponse = tryDirectProductLookup(userMessage, session);
            if (directResponse != null) {
                log.info("Direct product lookup matched, using rule-based response");
                return recordMessageAndReturn(conversationId, userMessage, directResponse, session);
            }

            // Try AI-powered response first (if enabled)
            String aiResponse = null;
            if (geminiService.isEnabled()) {
                try {
                    log.info("Attempting AI-powered response via Gemini");
                    aiResponse = geminiService.generateResponse(userMessage, session);

                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        log.info("AI response generated successfully");

                        // Extract preferences from AI conversation
                        extractAndStorePreferences(aiResponse, session);

                        // Record and return AI response
                        return recordMessageAndReturn(conversationId, userMessage, aiResponse, session);
                    } else {
                        log.warn("AI returned empty response, falling back to rule-based");
                    }
                } catch (Exception e) {
                    log.error("AI response failed, falling back to rule-based: ", e);
                }
            }

            // Fallback to rule-based conversation flow
            log.info("Using rule-based conversation flow");
            String response = handleConversationStage(userMessage, session.getCurrentStage(), session);

            // Record in conversation history
            session.addMessage(ConversationContext.ConversationMessage.builder()
                    .role("user")
                    .content(userMessage)
                    .timestamp(System.currentTimeMillis())
                    .build());
            session.addMessage(ConversationContext.ConversationMessage.builder()
                    .role("assistant")
                    .content(response)
                    .timestamp(System.currentTimeMillis())
                    .build());

            // Update session metadata
            conversationSessionManager.updateSession(conversationId, session);

            return response;

        } catch (Exception e) {
            log.error("Error in chatbot service: ", e);
            return "I apologize, but I'm having trouble processing your request. Please try again or contact our support team at support@perfumeshop.com.";
        }
    }

    /**
     * Extract user preferences from message and store in conversation context
     * Intelligently detects occasion, scent type, and budget from user input
     */
    private void extractAndStorePreferences(String userMessage, ConversationContext context) {
        if (userMessage == null || userMessage.isEmpty())
            return;

        String message = userMessage.toLowerCase();

        // Extract occasion if not already collected
        if (!context.hasPreference("occasion")) {
            String occasion = conversationSessionManager.extractPreferenceValue(message, "occasion");
            if (occasion != null) {
                context.setPreference("occasion", occasion);
                context.markQuestionAsked("occasion");
            }
        }

        // Extract scent type if not already collected
        if (!context.hasPreference("scent_type")) {
            String scentType = conversationSessionManager.extractPreferenceValue(message, "scent_type");
            if (scentType != null) {
                context.setPreference("scent_type", scentType);
                context.markQuestionAsked("scent_type");
            }
        }

        // Extract budget if not already collected
        if (!context.hasPreference("budget")) {
            String budget = conversationSessionManager.extractPreferenceValue(message, "budget");
            if (budget != null) {
                context.setPreference("budget", budget);
                context.markQuestionAsked("budget");
            }
        }
    }

    /**
     * Helper method to record messages and return response
     */
    private String recordMessageAndReturn(String conversationId, String userMessage, String response,
            ConversationContext session) {
        session.addMessage(ConversationContext.ConversationMessage.builder()
                .role("user")
                .content(userMessage)
                .timestamp(System.currentTimeMillis())
                .build());
        session.addMessage(ConversationContext.ConversationMessage.builder()
                .role("assistant")
                .content(response)
                .timestamp(System.currentTimeMillis())
                .build());

        // Update session metadata
        conversationSessionManager.updateSession(conversationId, session);

        return response;
    }

    /**
     * Try direct product lookup without conversation flow
     * Allows users to ask about any product at any time
     * Handles: price queries, product name queries, availability checks
     */
    private String tryDirectProductLookup(String userMessage, ConversationContext session) {
        String messageLower = userMessage.toLowerCase();

        // Intent 1: Price query - "price of X", "how much is X", "X cost"
        if (messageLower.contains("price") || messageLower.contains("cost") || messageLower.contains("how much")) {
            String result = handleDirectPriceQuery(userMessage, messageLower);
            if (result != null)
                return result;
            // Product not found for price query - return helpful message
            return "I couldn't find that product in our catalog. " +
                    "Could you tell me what type of fragrance you're looking for? " +
                    "I can recommend some great options based on your preferences!";
        }

        // Intent 2: Stock/Availability query - "is X available", "stock of X", "how
        // many X"
        if (messageLower.contains("stock") || messageLower.contains("available") || messageLower.contains("in stock")) {
            String result = handleDirectStockQuery(userMessage, messageLower);
            if (result != null)
                return result;
            // Product not found for stock query
            return "I'm not sure which product you're asking about. " +
                    "Could you be more specific or tell me the brand name?";
        }

        // Intent 3: Product info query - "tell me about X", "details of X", "info about
        // X"
        if (messageLower.contains("tell me about") || messageLower.contains("details")
                || messageLower.contains("info about")) {
            String result = handleDirectProductInfoQuery(userMessage, messageLower);
            if (result != null)
                return result;
            // Product not found for info query
            return "I don't have that product in my database. " +
                    "Would you like me to suggest some popular alternatives?";
        }

        // Intent 4: Simple product name query - if message is just a product name
        String productResponse = handleSimpleProductNameQuery(userMessage, messageLower);
        if (productResponse != null) {
            return productResponse;
        }

        return null; // No direct product lookup matched
    }

    /**
     * Handle direct price queries like "Gucci Bloom price" or "how much is Dior
     * Sauvage"
     */
    private String handleDirectPriceQuery(String userMessage, String messageLower) {
        log.info("=== DIRECT PRICE QUERY ===");
        log.info("User Query: {}", userMessage);

        // Extract product name from the query (everything except "price" and "cost"
        // keywords)
        String query = userMessage
                .replaceAll("(?i)\\b(price|cost|how much is|how much are)\\b", "")
                .trim();

        if (query.isEmpty()) {
            return null;
        }

        // Try exact match first
        var product = productService.findProductByName(query);
        if (product == null) {
            // Try fuzzy match
            product = productService.findProductByNameContains(query);
        }

        if (product != null) {
            log.info("Matched Product by Price Query: {}", product.getName());

            // Direct response - no unnecessary follow-ups
            String response = "💰 **" + product.getName() + "** - **₹" + product.getPrice() + "**\n\n";

            // Stock status - clear YES/NO
            if (product.getStock() > 0) {
                response += "✅ **In Stock** - " + product.getStock() + " units available\n";
            } else {
                response += "❌ **Out of Stock**\n";
            }

            response += "🏷️ **Brand**: " + product.getBrand() + "\n";
            response += "📏 **Volume**: " + (product.getVolume() != null ? product.getVolume() + "ml" : "N/A") + "\n";
            response += "⭐ **Rating**: " + String.format("%.1f", product.getRating()) + "/5";

            if (product.getReviewCount() > 0) {
                response += " (" + product.getReviewCount() + " reviews)";
            }

            return response;
        }

        log.info("No product found for price query");
        return null;
    }

    /**
     * Handle stock/availability queries like "is Gucci Bloom available" or "Dior
     * stock"
     */
    private String handleDirectStockQuery(String userMessage, String messageLower) {
        log.info("=== DIRECT STOCK QUERY ===");
        log.info("User Query: {}", userMessage);

        // Extract product name (remove stock/available keywords)
        String query = userMessage
                .replaceAll("(?i)\\b(is|are|stock|available|in stock|how many)\\b", "")
                .replaceAll("[?]", "")
                .trim();

        if (query.isEmpty()) {
            return null;
        }

        // Try exact match first
        var product = productService.findProductByName(query);
        if (product == null) {
            // Try fuzzy match
            product = productService.findProductByNameContains(query);
        }

        if (product != null) {
            log.info("Matched Product by Stock Query: {}", product.getName());

            // Direct response - clear YES or NO
            if (product.getStock() > 0) {
                String response = "✅ **" + product.getName() + "** is **IN STOCK**\n\n";
                response += "📦 **Available**: " + product.getStock() + " units\n";
                response += "💰 **Price**: ₹" + product.getPrice() + "\n";
                response += "🏷️ **Brand**: " + product.getBrand() + "\n";
                response += "📏 **Volume**: " + (product.getVolume() != null ? product.getVolume() + "ml" : "N/A");
                return response;
            } else {
                String response = "❌ **" + product.getName() + "** is **OUT OF STOCK**\n\n";
                response += "💰 **Price**: ₹" + product.getPrice() + "\n";
                response += "🏷️ **Brand**: " + product.getBrand() + "\n\n";
                response += "This product is currently unavailable. Would you like me to recommend similar alternatives?";
                return response;
            }
        }

        log.info("No product found for stock query");
        return null;
    }

    /**
     * Handle detailed product info queries like "tell me about Gucci Bloom"
     */
    private String handleDirectProductInfoQuery(String userMessage, String messageLower) {
        log.info("=== DIRECT PRODUCT INFO QUERY ===");
        log.info("User Query: {}", userMessage);

        // Extract product name
        String query = userMessage
                .replaceAll("(?i)\\b(tell me about|details|info about|information|describe|what is)\\b", "")
                .trim();

        if (query.isEmpty()) {
            return null;
        }

        // Try exact match first
        var product = productService.findProductByName(query);
        if (product == null) {
            // Try fuzzy match
            product = productService.findProductByNameContains(query);
        }

        if (product != null) {
            log.info("Matched Product by Info Query: {}", product.getName());
            return formatProductDetailsResponse(product);
        }

        log.info("No product found for info query");
        return null;
    }

    /**
     * Handle simple product name queries (when user just mentions a product name)
     * Returns null if not a simple name query to avoid false positives
     */
    private String handleSimpleProductNameQuery(String userMessage, String messageLower) {
        // Only do this for short messages that might be just a product name
        if (userMessage.length() > 30) {
            return null; // Too long to be just a product name
        }

        // Try to find the product by exact name
        var product = productService.findProductByName(userMessage.trim());

        if (product != null) {
            log.info("Matched Product by Simple Name Query: {}", product.getName());

            String response = "Found **" + product.getName() + "**!\n\n";
            response += "💰 **Price**: ₹" + product.getPrice() + "\n";

            // Clear stock indicator
            if (product.getStock() > 0) {
                response += "✅ **In Stock**: " + product.getStock() + " units available\n";
            } else {
                response += "❌ **Out of Stock**\n";
            }

            response += "🏷️ **Brand**: " + product.getBrand() + "\n";
            response += "📏 **Volume**: " + (product.getVolume() != null ? product.getVolume() + " ml" : "N/A") + "\n";
            response += "⭐ **Rating**: " + String.format("%.1f", product.getRating()) + "/5";

            if (product.getReviewCount() > 0) {
                response += " (" + product.getReviewCount() + " reviews)";
            }

            return response;
        }

        return null;
    }

    /**
     * Handle conversation routing based on current stage
     * Implements finite state machine for conversation flow
     */
    private String handleConversationStage(String userMessage, ConversationContext.ConversationStage stage,
            ConversationContext context) {
        switch (stage) {
            case INITIAL:
                return handleInitialStage(userMessage, context);
            case OCCASION_GATHERING:
                return handleOccasionGathering(userMessage, context);
            case SCENT_TYPE_GATHERING:
                return handleScentTypeGathering(userMessage, context);
            case BUDGET_GATHERING:
                return handleBudgetGathering(userMessage, context);
            case RECOMMENDATION_PHASE:
                return handleRecommendationPhase(userMessage, context);
            case FOLLOW_UP:
                return handleFollowUp(userMessage, context);
            case COMPLETE:
                return handleCompletePhase(userMessage, context);
            default:
                return handleInitialStage(userMessage, context);
        }
    }

    /**
     * Handle initial conversation stage - greet and ask for occasion
     */
    private String handleInitialStage(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase().trim();

        // If greeting, move to occasion gathering
        if (isGreeting(message) || context.getMessageCount() == 1) {
            context.setCurrentStage(ConversationContext.ConversationStage.OCCASION_GATHERING);
            context.markQuestionAsked("occasion");
            return "✨ Welcome to Perfumé! I'm Sophia, your personal fragrance consultant. 🌸\n\n" +
                    "I'm here to help you find your perfect scent match!\n\n" +
                    "First, what's the main occasion for your fragrance? " +
                    "(e.g., work/professional, romantic date, casual everyday, party, holiday, special event)";
        }

        return "Hello! ✨ I'm Sophia. What brings you to Perfumé today? 🌹";
    }

    /**
     * Handle occasion gathering stage - ask about occasion and transition to scent
     * type
     */
    private String handleOccasionGathering(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase();

        // Check if user provided occasion
        if (context.hasPreference("occasion")) {
            String occasion = context.getPreference("occasion");
            context.setCurrentStage(ConversationContext.ConversationStage.SCENT_TYPE_GATHERING);
            context.markQuestionAsked("scent_type");

            String occasionResponse = getOccasionResponse(occasion);
            return occasionResponse + "\n\nNow, what type of scent appeals to you? " +
                    "(floral, woody, fresh, oriental, fruity, or a mix?)";
        }

        // If not asking already, ask for occasion
        if (!context.isQuestionAsked("occasion")) {
            context.markQuestionAsked("occasion");
            return "What's the main occasion for your fragrance? " +
                    "(e.g., work/professional, romantic date, casual everyday, party, holiday, special event)";
        }

        // Follow-up if still no occasion provided
        return "I'd love to know more about the occasion! Is it for:\n" +
                "💼 Work/Professional settings\n" +
                "💕 Romantic occasions\n" +
                "👔 Casual everyday wear\n" +
                "🎉 Party or celebration\n" +
                "❄️ Seasonal or holiday\n\n" +
                "Which resonates with you?";
    }

    /**
     * Handle scent type gathering stage
     */
    private String handleScentTypeGathering(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase();

        // Check if user provided scent type
        if (context.hasPreference("scent_type")) {
            String scentType = context.getPreference("scent_type");
            context.setCurrentStage(ConversationContext.ConversationStage.BUDGET_GATHERING);
            context.markQuestionAsked("budget");

            String scentResponse = getScentTypeResponse(scentType);
            return scentResponse + "\n\nOne more thing - what's your budget range? " +
                    "(budget-friendly ₹1,500-2,500 | mid-range ₹3,000-5,000 | luxury ₹8,000+)";
        }

        // If not asking already, ask for scent type
        if (!context.isQuestionAsked("scent_type")) {
            context.markQuestionAsked("scent_type");
            return "What type of scent appeals to you?\n" +
                    "🌸 Floral - Classic and romantic\n" +
                    "🌲 Woody - Sophisticated and grounded\n" +
                    "🌊 Fresh - Light and energizing\n" +
                    "✨ Oriental - Warm and sensual\n" +
                    "🍓 Fruity - Fun and playful";
        }

        return "Tell me more about what scent type you prefer! " +
                "(floral, woody, fresh, oriental, fruity, or something else?)";
    }

    /**
     * Handle budget gathering stage
     */
    private String handleBudgetGathering(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase();

        // Check if user provided budget
        if (context.hasPreference("budget")) {
            context.setCurrentStage(ConversationContext.ConversationStage.RECOMMENDATION_PHASE);
            return handleRecommendationPhase(userMessage, context);
        }

        // If not asking already, ask for budget
        if (!context.isQuestionAsked("budget")) {
            context.markQuestionAsked("budget");
            return "Perfect! Last question - what's your budget range?\n" +
                    "💰 Budget-friendly: ₹1,500 - 2,500\n" +
                    "💳 Mid-range: ₹3,000 - 5,000\n" +
                    "👑 Luxury: ₹8,000+\n\n" +
                    "This helps me recommend exactly what you'll love!";
        }

        return "Could you tell me your budget range? " +
                "(budget-friendly, mid-range, or luxury?)";
    }

    /**
     * Handle recommendation phase - provide personalized recommendations
     */
    private String handleRecommendationPhase(String userMessage, ConversationContext context) {
        // Extract any missing preferences from this message
        extractAndStorePreferences(userMessage, context);

        // If now have all preferences, generate recommendation
        if (context.hasPreference("occasion") && context.hasPreference("scent_type")
                && context.hasPreference("budget")) {
            String occasion = context.getPreference("occasion");
            String scentType = context.getPreference("scent_type");
            String budget = context.getPreference("budget");

            context.setCurrentStage(ConversationContext.ConversationStage.FOLLOW_UP);

            return generatePersonalizedRecommendation(occasion, scentType, budget, context);
        }

        // If missing any preference, ask for it
        if (!context.hasPreference("occasion")) {
            return "Before I recommend, could you tell me the occasion? " +
                    "(work, romantic, casual, party, etc.)";
        }
        if (!context.hasPreference("scent_type")) {
            return "What scent type appeals to you? " +
                    "(floral, woody, fresh, oriental, fruity)";
        }
        if (!context.hasPreference("budget")) {
            return "What's your budget range? " +
                    "(budget-friendly ₹1,500-2,500 | mid-range ₹3,000-5,000 | luxury ₹8,000+)";
        }

        return "Let me get you the perfect recommendation! 🎁";
    }

    /**
     * Handle follow-up phase - answer questions using stored recommendations
     */
    private String handleFollowUp(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase();

        log.info("=== FOLLOW-UP PHASE DEBUG ===");
        log.info("User Message: {}", userMessage);
        log.info("Stored Recommendations: {}", context.getStoredRecommendations());
        log.info("Recommended Product IDs: {}", context.getRecommendedProductIds());
        log.info("==============================");

        // Check if asking about product details (price, stock, availability, details)
        if (message.contains("price") || message.contains("cost") || message.contains("stock") ||
                message.contains("available") || message.contains("detail") || message.contains("info")) {

            String productDetails = getProductDetailsResponse(userMessage, context);
            if (productDetails != null) {
                return productDetails;
            }
        }

        // If asking about perfume names/recommendations
        if (message.contains("name") || message.contains("perfume") || message.contains("recommendation") ||
                message.contains("which") || message.contains("product")) {

            if (context.hasRecommendations()) {
                String recList = String.join(", ", context.getStoredRecommendations());
                return "Based on your preferences, I recommended:\n\n" +
                        "💎 **" + recList + "**\n\n" +
                        "These fragrances perfectly match your:\n" +
                        "• Occasion: " + context.getPreference("occasion") + "\n" +
                        "• Scent Type: " + context.getPreference("scent_type") + "\n" +
                        "• Budget: " + context.getPreference("budget") + "\n\n" +
                        "Would you like to know more about any of these, or explore other options?";
            }
        }

        // Check if asking for more recommendations
        if (message.contains("other") || message.contains("another") || message.contains("more")
                || message.contains("different")) {
            context.setCurrentStage(ConversationContext.ConversationStage.RECOMMENDATION_PHASE);
            return "Great! I can suggest alternatives with different notes or price points. " +
                    "Would you like something:\n" +
                    "• With different scent characteristics\n" +
                    "• At a different price point\n" +
                    "• For a different occasion\n\n" +
                    "What would you prefer?";
        }

        // Default follow-up
        if (message.contains("thank") || message.contains("perfect") || message.contains("great")) {
            context.setCurrentStage(ConversationContext.ConversationStage.COMPLETE);
            return "Wonderful! 🌸 Is there anything else I can help you with today? " +
                    "I can answer questions about:\n" +
                    "• Fragrance application tips\n" +
                    "• Longevity and projection\n" +
                    "• Shipping and returns\n" +
                    "• Payment options\n\n" +
                    "Or feel free to browse our collection!";
        }

        return "Would you like to know anything else about these fragrances? " +
                "I can help with pricing, availability, application tips, or answer any other questions! 💬";
    }

    /**
     * Handle complete phase - end conversation gracefully
     */
    private String handleCompletePhase(String userMessage, ConversationContext context) {
        String message = userMessage.toLowerCase();

        // Help questions
        if (message.contains("help") || message.contains("how") || message.contains("apply")
                || message.contains("use")) {
            return provideHelpResponse(message);
        }

        // More recommendations
        if (message.contains("recommend") || message.contains("more")) {
            context.setCurrentStage(ConversationContext.ConversationStage.RECOMMENDATION_PHASE);
            return "I'd be happy to recommend more fragrances! Tell me:\n" +
                    "• Different scent type\n" +
                    "• Different occasion\n" +
                    "• Different budget range\n\n" +
                    "What would you like to explore?";
        }

        // Default
        return "Thanks for shopping with Perfumé! 🌹 " +
                "Feel free to ask anything else or visit our website at www.perfumeshop.com!";
    }

    /**
     * Generate personalized fragrance recommendation based on collected preferences
     */
    private String generatePersonalizedRecommendation(String occasion, String scentType, String budget,
            ConversationContext context) {
        String recommendation = "🎁 **Based on your preferences**, I recommend:\n\n";
        recommendation += "📍 **Occasion**: " + occasion + "\n";
        recommendation += "👃 **Scent Type**: " + scentType + "\n";
        recommendation += "💰 **Budget**: " + budget + "\n\n";

        // CLEAR previous recommendations before storing new ones
        context.getStoredRecommendations().clear();
        context.getRecommendedProductIds().clear();

        // Generate recommendations based on combination of preferences
        if (occasion.contains("professional") || occasion.contains("work")) {
            if (scentType.contains("fresh") || scentType.contains("citrus")) {
                recommendation += "✨ **Fresh & Professional** Collection:\n" +
                        "• Dior Sauvage - Fresh aquatic & citrus (₹2,500)\n" +
                        "• Bleu de Chanel - Bright lemon & bergamot (₹2,800)\n" +
                        "Perfect for office settings with subtle projection!";
                addRecommendationWithLookup("Dior Sauvage", context);
                addRecommendationWithLookup("Bleu de Chanel", context);
            } else if (scentType.contains("woody")) {
                recommendation += "💼 **Professional Woody** Collection:\n" +
                        "• Prada L'Homme - Creamy sandalwood (₹4,500)\n" +
                        "• Giorgio Armani Code - Rich cedar & vetiver (₹5,200)\n" +
                        "Sophisticated and grounded for business environments!";
                addRecommendationWithLookup("Prada L'Homme", context);
                addRecommendationWithLookup("Giorgio Armani Code", context);
            } else {
                recommendation += "💼 **Office Appropriate** Collection:\n" +
                        "• Dior Sauvage - Professional fragrances\n" +
                        "• Giorgio Armani Code - Refined scents\n" +
                        "Both perfect for work settings!";
                addRecommendationWithLookup("Dior Sauvage", context);
                addRecommendationWithLookup("Giorgio Armani Code", context);
            }
        } else if (occasion.contains("romantic") || occasion.contains("date")) {
            if (scentType.contains("floral")) {
                recommendation += "💕 **Romantic Floral** Collection:\n" +
                        "• Gucci Bloom - Classic rose & jasmine (₹3,800)\n" +
                        "• Dior Jadore - Delicate jasmine & lily (₹4,200)\n" +
                        "Captivating and sensual for special evenings!";
                addRecommendationWithLookup("Gucci Bloom", context);
                addRecommendationWithLookup("Dior Jadore", context);
            } else if (scentType.contains("oriental") || scentType.contains("warm")) {
                recommendation += "💎 **Romantic Oriental** Collection:\n" +
                        "• Tom Ford Black Orchid - Warm amber & vanilla (₹6,500)\n" +
                        "• Lancôme Trésor - Sensual & mysterious (₹7,200)\n" +
                        "Create an unforgettable impression!";
                addRecommendationWithLookup("Tom Ford Black Orchid", context);
                addRecommendationWithLookup("Lancôme Trésor", context);
            } else {
                recommendation += "💕 **Date Night Perfect** Collection:\n" +
                        "• Gucci Bloom - Romantic floral\n" +
                        "• Tom Ford Black Orchid - Warm and sensual\n" +
                        "Both ideal for creating memorable moments!";
                addRecommendationWithLookup("Gucci Bloom", context);
                addRecommendationWithLookup("Tom Ford Black Orchid", context);
            }
        } else if (occasion.contains("casual")) {
            if (scentType.contains("fresh")) {
                recommendation += "🌟 **Casual Fresh** Collection:\n" +
                        "• Marc Jacobs Daisy Love - Aquatic & fresh (₹2,500)\n" +
                        "• Jo Malone Cologne - Energizing citrus (₹2,800)\n" +
                        "Perfect for everyday wear and errands!";
                addRecommendationWithLookup("Marc Jacobs Daisy Love", context);
                addRecommendationWithLookup("Jo Malone Cologne", context);
            } else if (scentType.contains("fruity")) {
                recommendation += "🌟 **Casual Fruity** Collection:\n" +
                        "• Burberry Brit Sheer - Sweet berries & peach (₹2,200)\n" +
                        "• Marc Jacobs Decadence - Exotic fruits (₹2,600)\n" +
                        "Fun and playful for casual occasions!";
                addRecommendationWithLookup("Burberry Brit Sheer", context);
                addRecommendationWithLookup("Marc Jacobs Decadence", context);
            } else {
                recommendation += "🌟 **Everyday Versatile** Collection:\n" +
                        "• Marc Jacobs Daisy Love - Day wear\n" +
                        "• Jo Malone Cologne - Comfortable fragrances for any setting\n" +
                        "Perfect for your lifestyle!";
                addRecommendationWithLookup("Marc Jacobs Daisy Love", context);
                addRecommendationWithLookup("Jo Malone Cologne", context);
            }
        } else {
            recommendation += "✨ **Our Top Pick** Collection:\n" +
                    "• " + capitalize(scentType) + " fragrances perfect for " + occasion + "\n" +
                    "• Available in your " + budget + " range\n" +
                    "Guaranteed to impress!";
            context.addRecommendation(capitalize(scentType) + " fragrances");
        }

        log.info("=== RECOMMENDATIONS STORED ===");
        log.info("ConversationId: {}", context.getConversationId());
        log.info("Stored Perfume Names: {}", context.getStoredRecommendations());
        log.info("Current Stage: {}", context.getCurrentStage());
        log.info("================================");

        recommendation += "\n\n💬 **Would you like to**:\n" +
                "• Know more about these fragrances\n" +
                "• See other recommendations\n" +
                "• Learn about application tips";

        return recommendation;
    }

    /**
     * Get response based on occasion preference
     */
    private String getOccasionResponse(String occasion) {
        if (occasion.contains("professional") || occasion.contains("work")) {
            return "Great! 💼 For professional settings, you'll want something that's elegant but not overpowering.";
        } else if (occasion.contains("romantic") || occasion.contains("date")) {
            return "Perfect! 💕 Romantic occasions call for something captivating and memorable!";
        } else if (occasion.contains("casual")) {
            return "Excellent! 🌟 For everyday wear, you'll want something comfortable and versatile.";
        } else if (occasion.contains("party") || occasion.contains("celebration")) {
            return "Fantastic! 🎉 Parties call for something bold and memorable!";
        } else if (occasion.contains("holiday") || occasion.contains("seasonal")) {
            return "Perfect timing! ❄️ Seasonal fragrances can really enhance the occasion.";
        }
        return "Wonderful! Let me help you find the perfect scent for " + occasion + ".";
    }

    /**
     * Get response based on scent type preference
     */
    private String getScentTypeResponse(String scentType) {
        if (scentType.contains("floral")) {
            return "Beautiful! 🌸 Floral fragrances are classic, romantic, and timeless.";
        } else if (scentType.contains("woody")) {
            return "Excellent choice! 🌲 Woody fragrances are sophisticated and sophisticated.";
        } else if (scentType.contains("fresh")) {
            return "Great! 🌊 Fresh fragrances are energizing and perfect for all occasions.";
        } else if (scentType.contains("oriental")) {
            return "Wonderful! ✨ Oriental fragrances are warm, sensual, and luxurious.";
        } else if (scentType.contains("fruity")) {
            return "Fun choice! 🍓 Fruity fragrances are playful and vibrant.";
        }
        return "Nice! " + capitalize(scentType) + " fragrances are wonderful!";
    }

    /**
     * Provide help response for application tips and product information
     */
    private String provideHelpResponse(String message) {
        if (message.contains("apply") || message.contains("use") || message.contains("spray")) {
            return "Here are the best application tips:\n" +
                    "✨ Apply to pulse points - wrists, behind ears, inside elbows\n" +
                    "💧 Use 2-3 spritzes from 6 inches away\n" +
                    "🚫 Don't rub your wrists - let it dry naturally\n" +
                    "🌡️ Apply to warm areas for better projection\n" +
                    "💫 For longevity, apply to moisturized skin\n\n" +
                    "Any other questions? 🌹";
        } else if (message.contains("longevity") || message.contains("how long") || message.contains("last")) {
            return "Fragrance longevity depends on concentration:\n" +
                    "🌼 Eau de Cologne - 2-3 hours\n" +
                    "🌸 Eau de Toilette - 3-5 hours (most popular)\n" +
                    "💎 Eau de Parfum - 5-8 hours (longer-lasting)\n" +
                    "👑 Parfum - 8+ hours (most luxurious)\n\n" +
                    "For daily wear, Eau de Toilette is ideal!";
        } else if (message.contains("shipping") || message.contains("delivery")) {
            return "For shipping information, contact us at support@perfumeshop.com or visit www.perfumeshop.com! 📦 " +
                    "We offer fast, secure delivery options!";
        } else if (message.contains("return") || message.contains("refund")) {
            return "We want you satisfied! 💚 We have a hassle-free return process within 14 days. " +
                    "Contact support@perfumeshop.com for details!";
        }
        return "I'm here to help! What would you like to know more about? 🌹";
    }

    /**
     * Check if message is a greeting
     */
    private boolean isGreeting(String message) {
        return message.matches(".*(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy|g'day).*");
    }

    /**
     * Get quick fragrance recommendations based on type (legacy support)
     */
    public String quickRecommendation(String fragmentType) {
        if (fragmentType == null || fragmentType.isEmpty()) {
            return "I'd love to help you find the perfect fragrance! Could you tell me what type of scents you prefer?";
        }

        String type = fragmentType.toLowerCase();
        if (type.contains("floral")) {
            return "💐 **Floral Recommendations**:\n" +
                    "• **Rose Garden** - Classic rose with jasmine\n" +
                    "• **Jasmine Essence** - Delicate jasmine & lily\n" +
                    "Perfect for romantic occasions and everyday elegance!";
        } else if (type.contains("woody")) {
            return "🌲 **Woody Recommendations**:\n" +
                    "• **Sandalwood Symphony** - Creamy sandalwood\n" +
                    "• **Cedar Dreams** - Rich cedar & vetiver\n" +
                    "Perfect for sophisticated, grounded scents!";
        } else if (type.contains("fresh")) {
            return "🌊 **Fresh Recommendations**:\n" +
                    "• **Ocean Breeze** - Fresh aquatic & citrus\n" +
                    "• **Citrus Burst** - Bright lemon & grapefruit\n" +
                    "Perfect for everyday wear and professional settings!";
        } else if (type.contains("oriental")) {
            return "✨ **Oriental Recommendations**:\n" +
                    "• **Amber Luxury** - Warm amber & vanilla\n" +
                    "• **Vanilla Gold** - Rich vanilla & musk\n" +
                    "Perfect for evening wear and special occasions!";
        } else if (type.contains("fruity")) {
            return "🍓 **Fruity Recommendations**:\n" +
                    "• **Berry Bliss** - Sweet berries & peach\n" +
                    "• **Tropical Paradise** - Exotic fruits & florals\n" +
                    "Perfect for fun, playful occasions!";
        } else {
            return "I'd love to help! Could you tell me more about what types of scents you enjoy?";
        }
    }

    /**
     * Analyze scent preference and provide recommendations (legacy support)
     */
    public String analyzeScentPreference(String preference) {
        if (preference == null || preference.isEmpty()) {
            return "I'd love to help! Could you tell me what types of scents you prefer? " +
                    "Light, intense, fresh, warm, floral, woody, etc.?";
        }

        String pref = preference.toLowerCase();

        if (pref.contains("light") || pref.contains("fresh")) {
            return "Perfect! 🌸 Based on your preference for light, fresh scents, I recommend:\n" +
                    "• **Fresh & Light Collection** - Perfect for daytime wear\n" +
                    "• **Eau de Toilette** concentration - Light & refreshing\n" +
                    "• Citrus & aquatic notes - Clean & invigorating\n\n" +
                    "Would you like specific product recommendations?";
        } else if (pref.contains("intense") || pref.contains("strong") || pref.contains("bold")) {
            return "Excellent! 💎 Based on your preference for intense scents, I suggest:\n" +
                    "• **Intense & Luxurious Collection** - Make a powerful statement\n" +
                    "• **Eau de Parfum** concentration - Long-lasting & strong\n" +
                    "• Oriental & woody notes - Rich & captivating\n\n" +
                    "Perfect for evening and special occasions!";
        } else if (pref.contains("warm") || pref.contains("cozy") || pref.contains("comfort")) {
            return "Lovely! ☕ Based on your preference for warm scents:\n" +
                    "• **Warm & Cozy Collection** - Comfortable & embracing\n" +
                    "• Amber, vanilla & woody notes - Soft & inviting\n" +
                    "• Great for cooler months & intimate settings\n\n" +
                    "Would you like recommendations?";
        } else {
            return "Great choice! 🎁 Based on your preference for **" + preference + "** scents:\n" +
                    "• Our **Balanced Collection** - Versatile for any occasion\n" +
                    "• Works for both day and evening wear\n" +
                    "• Long-lasting & elegant\n\n" +
                    "Would you like specific product recommendations?";
        }
    }

    /**
     * Add recommendation with database product lookup
     */
    private void addRecommendationWithLookup(String perfumeName, ConversationContext context) {
        context.addRecommendation(perfumeName);

        // Try to find exact match first
        var product = productService.findProductByName(perfumeName);

        if (product != null) {
            context.addRecommendation(perfumeName, product.getId());
            log.info("Linked recommendation: {} -> Product ID: {}", perfumeName, product.getId());
        } else {
            // Try partial match
            product = productService.findProductByNameContains(perfumeName);
            if (product != null) {
                context.addRecommendation(perfumeName, product.getId());
                log.info("Linked recommendation (partial match): {} -> Product ID: {}", perfumeName, product.getId());
            } else {
                log.warn("Could not find product in database for recommendation: {}", perfumeName);
            }
        }
    }

    /**
     * Get product details for follow-up question about specific recommendation
     */
    private String getProductDetailsResponse(String userMessage, ConversationContext context) {
        log.info("=== PRODUCT DETAILS LOOKUP ===");
        log.info("User Query: {}", userMessage);
        log.info("Available Recommendations: {}", context.getStoredRecommendations());

        String messageLower = userMessage.toLowerCase();

        // Try to match user's mention to a recommended product
        for (String recommendedName : context.getStoredRecommendations()) {
            if (messageLower.contains(recommendedName.toLowerCase())) {
                Long productId = context.getProductIdByName(recommendedName);

                if (productId != null) {
                    try {
                        var product = productService.getProductEntityById(productId);
                        if (product != null) {
                            log.info("Matched Product: {}", recommendedName);
                            log.info("Product Details - ID: {}, Price: {}, Stock: {}",
                                    product.getId(), product.getPrice(), product.getStock());

                            return formatProductDetailsResponse(product);
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch product details for ID {}: {}", productId, e.getMessage());
                    }
                }
            }
        }

        log.info("==================================");
        return null; // No match found
    }

    /**
     * Format product details for chatbot response
     */
    private String formatProductDetailsResponse(com.perfume.shop.entity.Product product) {
        String response = "💎 **" + product.getName() + "**\n\n";
        response += "💰 **Price**: ₹" + product.getPrice() + "\n";

        // Clear stock indicator
        if (product.getStock() > 0) {
            response += "✅ **In Stock**: " + product.getStock() + " units available\n";
        } else {
            response += "❌ **Out of Stock**\n";
        }

        response += "🏷️ **Brand**: " + product.getBrand() + "\n";
        response += "📏 **Volume**: " + (product.getVolume() != null ? product.getVolume() + " ml" : "N/A") + "\n";
        response += "⭐ **Rating**: " + String.format("%.1f", product.getRating()) + "/5";

        if (product.getReviewCount() > 0) {
            response += " (" + product.getReviewCount() + " reviews)";
        }

        response += "\n\n**Description**:\n" + product.getDescription();

        return response;
    }

    /**
     * Capitalize first letter of string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty())
            return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
