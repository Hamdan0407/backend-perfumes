package com.perfume.shop.controller;

import com.perfume.shop.dto.ChatRequest;
import com.perfume.shop.dto.ChatResponse;
import com.perfume.shop.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Chatbot Controller
 * Handles all chatbot-related API endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Main chat endpoint with conversation state management
     * POST /api/chatbot/chat
     * 
     * @param request Chat request containing user message and optional conversationId
     * @return Chat response with bot's reply and conversation context
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            log.info("Received chat request: {}", request.getMessage());
            
            // Generate or use existing conversationId
            String conversationId = request.getConversationId();
            if (conversationId == null || conversationId.isEmpty()) {
                conversationId = UUID.randomUUID().toString();
            }
            
            // Call chatbot service with state management
            String botResponse = chatbotService.chat(
                request.getMessage(),
                conversationId,
                request.getContext()
            );
            
            ChatResponse response = ChatResponse.builder()
                .message(botResponse)
                .conversationId(conversationId)
                .status("success")
                .timestamp(System.currentTimeMillis())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat request: ", e);
            ChatResponse errorResponse = ChatResponse.builder()
                .message("Sorry, I encountered an error. Please try again.")
                .status("error")
                .timestamp(System.currentTimeMillis())
                .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Quick recommendation endpoint
     * GET /api/chatbot/recommend?type=floral
     * 
     * @param type Fragrance type (floral, woody, fresh, oriental, fruity)
     * @return Quick recommendation
     */
    @GetMapping("/recommend")
    public ResponseEntity<ChatResponse> quickRecommend(@RequestParam String type) {
        try {
            log.info("Requested quick recommendation for type: {}", type);
            
            String recommendation = chatbotService.quickRecommendation(type);
            
            ChatResponse response = ChatResponse.builder()
                .message(recommendation)
                .status("success")
                .timestamp(System.currentTimeMillis())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recommendation: ", e);
            ChatResponse errorResponse = ChatResponse.builder()
                .message("Could not get recommendation. Please try again.")
                .status("error")
                .timestamp(System.currentTimeMillis())
                .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Analyze scent preference
     * GET /api/chatbot/analyze-preference?preference=fresh&light
     * 
     * @param preference User's scent preference
     * @return Analysis and recommendations
     */
    @GetMapping("/analyze-preference")
    public ResponseEntity<ChatResponse> analyzePreference(@RequestParam String preference) {
        try {
            log.info("Analyzing preference: {}", preference);
            
            String analysis = chatbotService.analyzeScentPreference(preference);
            
            ChatResponse response = ChatResponse.builder()
                .message(analysis)
                .status("success")
                .timestamp(System.currentTimeMillis())
                .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error analyzing preference: ", e);
            ChatResponse errorResponse = ChatResponse.builder()
                .message("Could not analyze preference. Please try again.")
                .status("error")
                .timestamp(System.currentTimeMillis())
                .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check endpoint
     * GET /api/chatbot/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running");
    }
}
