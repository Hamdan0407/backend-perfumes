package com.perfume.shop.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Development utility controller for fixing H2 database sequences.
 * Only available in demo profile.
 */
@RestController
@RequestMapping("/api/dev")
@Profile("demo")
@Slf4j
public class DatabaseUtilController {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @PostMapping("/fix-sequences")
    @Transactional
    public ResponseEntity<Map<String, Object>> fixSequences() {
        try {
            // Get the max product ID and reset the sequence
            Object maxProductIdResult = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) FROM products"
            ).getSingleResult();
            
            long maxProductId = maxProductIdResult instanceof Number ? 
                ((Number) maxProductIdResult).longValue() : 0;
            long newProductStartId = maxProductId + 100;
            
            entityManager.createNativeQuery(
                "ALTER TABLE products ALTER COLUMN id RESTART WITH " + newProductStartId
            ).executeUpdate();
            
            log.info("Product sequence reset to start at {} (max existing ID: {})", 
                newProductStartId, maxProductId);
            
            // Also reset users table sequence
            Object maxUserIdResult = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) FROM users"
            ).getSingleResult();
            
            long maxUserId = maxUserIdResult instanceof Number ? 
                ((Number) maxUserIdResult).longValue() : 0;
            long newUserStartId = maxUserId + 100;
            
            entityManager.createNativeQuery(
                "ALTER TABLE users ALTER COLUMN id RESTART WITH " + newUserStartId
            ).executeUpdate();
            
            log.info("User sequence reset to start at {} (max existing ID: {})", 
                newUserStartId, maxUserId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Database sequences reset successfully",
                "productSequenceStart", newProductStartId,
                "userSequenceStart", newUserStartId,
                "maxProductId", maxProductId,
                "maxUserId", maxUserId
            ));
            
        } catch (Exception e) {
            log.error("Failed to reset database sequences: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Failed to reset sequences",
                "message", e.getMessage()
            ));
        }
    }
}