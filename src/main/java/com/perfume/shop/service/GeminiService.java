package com.perfume.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfume.shop.dto.ConversationContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini AI Service - Integrates Google Gemini API for intelligent chatbot
 * responses
 * Provides dynamic, context-aware responses for perfume consultation
 */
@Slf4j
@Service
public class GeminiService {

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${app.gemini.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProductService productService;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    public GeminiService(ProductService productService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.productService = productService;
    }

    /**
     * Generate AI response with conversation context
     */
    public String generateResponse(String userMessage, ConversationContext context) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini AI is disabled or API key not configured");
            return null; // Fall back to rule-based system
        }

        try {
            // Build conversation history for context
            String conversationHistory = buildConversationHistory(context);

            // Build system prompt with product catalog context
            String systemPrompt = buildSystemPrompt(context);

            // Create the full prompt
            String fullPrompt = systemPrompt + "\n\n" +
                    "Conversation History:\n" + conversationHistory + "\n\n" +
                    "User: " + userMessage + "\n\n" +
                    "Assistant:";

            // Call Gemini API
            String response = callGeminiAPI(fullPrompt);

            if (response != null && !response.isEmpty()) {
                log.info("Gemini AI response generated successfully");
                return response;
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
        }

        return null; // Fall back to rule-based system
    }

    /**
     * Build system prompt with perfume consultant persona and product context
     */
    private String buildSystemPrompt(ConversationContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are Sophia, an expert perfume consultant at Parfumé luxury fragrance boutique. ");
        prompt.append(
                "You are warm, knowledgeable, and passionate about helping customers find their perfect scent.\n\n");

        prompt.append("YOUR PERSONALITY:\n");
        prompt.append("- Friendly and approachable, use emojis naturally (🌸, 💎, ✨)\n");
        prompt.append("- Expert in fragrance notes, occasions, and scent families\n");
        prompt.append("- Ask thoughtful questions to understand preferences\n");
        prompt.append("- Provide personalized recommendations based on customer needs\n");
        prompt.append("- Keep responses concise (2-4 sentences max)\n\n");

        prompt.append("CONVERSATION GUIDELINES:\n");
        prompt.append("- Gather preferences: occasion, scent type (floral/woody/fresh/oriental/fruity), budget\n");
        prompt.append("- Recommend products from our catalog (see below)\n");
        prompt.append("- Answer questions about fragrance notes, longevity, application\n");
        prompt.append("- Be helpful with pricing, stock, and product details\n\n");

        // Add current preferences if available
        if (context != null && context.getPreferences() != null && !context.getPreferences().isEmpty()) {
            prompt.append("CUSTOMER PREFERENCES COLLECTED:\n");
            context.getPreferences()
                    .forEach((key, value) -> prompt.append("- ").append(key).append(": ").append(value).append("\n"));
            prompt.append("\n");
        }

        // Add product catalog context (top products)
        prompt.append("OUR PRODUCT CATALOG (Sample):\n");
        try {
            var products = productService.getAllProducts();
            int count = 0;
            for (var product : products) {
                if (count >= 10)
                    break; // Limit to 10 products to save tokens
                prompt.append("- ").append(product.getName())
                        .append(" by ").append(product.getBrand())
                        .append(" (₹").append(product.getPrice()).append(")")
                        .append(" - ").append(product.getCategory())
                        .append(product.getStock() > 0 ? " [IN STOCK]" : " [OUT OF STOCK]")
                        .append("\n");
                count++;
            }
        } catch (Exception e) {
            log.warn("Could not fetch product catalog for AI context");
        }

        prompt.append("\nIMPORTANT: When recommending products, ONLY suggest items from our catalog above. ");
        prompt.append(
                "If asked about a product not in our catalog, politely say it's not available and suggest alternatives.\n");

        return prompt.toString();
    }

    /**
     * Build conversation history from context
     */
    private String buildConversationHistory(ConversationContext context) {
        if (context == null || context.getMessages() == null || context.getMessages().isEmpty()) {
            return "";
        }

        StringBuilder history = new StringBuilder();
        // Get last 5 messages for context (to avoid token limits)
        int start = Math.max(0, context.getMessages().size() - 5);
        for (int i = start; i < context.getMessages().size(); i++) {
            var msg = context.getMessages().get(i);
            history.append(msg.getRole().equals("user") ? "User: " : "Assistant: ")
                    .append(msg.getContent())
                    .append("\n");
        }

        return history.toString();
    }

    /**
     * Call Gemini API with the prompt
     */
    private String callGeminiAPI(String prompt) {
        try {
            String url = String.format(GEMINI_API_URL, model, apiKey);

            // Build request body
            Map<String, Object> requestBody = new HashMap<>();

            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();

            List<Map<String, String>> parts = new ArrayList<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);

            content.put("parts", parts);
            contents.add(content);

            requestBody.put("contents", contents);

            // Add generation config for better responses
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 500);
            generationConfig.put("topP", 0.8);
            generationConfig.put("topK", 40);
            requestBody.put("generationConfig", generationConfig);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Make API call
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseGeminiResponse(response.getBody());
            } else {
                log.error("Gemini API returned status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error calling Gemini API: ", e);
        }

        return null;
    }

    /**
     * Parse Gemini API response to extract text
     */
    private String parseGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    return text.trim();
                }
            }

            log.warn("Could not parse Gemini response: {}", responseBody);
        } catch (Exception e) {
            log.error("Error parsing Gemini response: ", e);
        }

        return null;
    }

    /**
     * Check if Gemini AI is enabled and configured
     */
    public boolean isEnabled() {
        return enabled && apiKey != null && !apiKey.isEmpty();
    }
}
