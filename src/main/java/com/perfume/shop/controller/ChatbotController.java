package com.perfume.shop.controller;

import com.perfume.shop.dto.ChatbotRequest;
import com.perfume.shop.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ChatbotController {
    
    private final ChatbotService chatbotService;
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatbotRequest request) {
        try {
            String response = chatbotService.processMessage(
                request.getMessage(),
                request.getConversationId()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", response);
            result.put("conversationId", request.getConversationId());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Sorry, I couldn't process your request. Please try again.");
            return ResponseEntity.badRequest().body(error);
        }
    }
}
