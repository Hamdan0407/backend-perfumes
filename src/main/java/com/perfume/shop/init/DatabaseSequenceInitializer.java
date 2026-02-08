package com.perfume.shop.init;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixes the H2 database IDENTITY sequence after import.sql populates data with explicit IDs.
 * This ensures new product inserts get IDs that don't conflict with existing data.
 * Only runs in demo profile with H2 database.
 */
@Component
@Profile("demo")
@Order(1) // Run after data initialization
@Slf4j
public class DatabaseSequenceInitializer implements CommandLineRunner {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Resetting H2 database sequences for demo mode...");
        
        try {
            // Get the max product ID and reset the sequence to start after it
            Object maxIdResult = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) FROM products"
            ).getSingleResult();
            
            long maxId = maxIdResult instanceof Number ? ((Number) maxIdResult).longValue() : 0;
            long newStartId = maxId + 100; // Add buffer to avoid conflicts
            
            // Reset the IDENTITY sequence for products table
            entityManager.createNativeQuery(
                "ALTER TABLE products ALTER COLUMN id RESTART WITH " + newStartId
            ).executeUpdate();
            
            log.info("Product sequence reset to start at {} (max existing ID: {})", newStartId, maxId);
            
            // Also reset users table sequence if needed
            Object maxUserIdResult = entityManager.createNativeQuery(
                "SELECT COALESCE(MAX(id), 0) FROM users"
            ).getSingleResult();
            
            long maxUserId = maxUserIdResult instanceof Number ? ((Number) maxUserIdResult).longValue() : 0;
            long newUserStartId = maxUserId + 100;
            
            entityManager.createNativeQuery(
                "ALTER TABLE users ALTER COLUMN id RESTART WITH " + newUserStartId
            ).executeUpdate();
            
            log.info("User sequence reset to start at {} (max existing ID: {})", newUserStartId, maxUserId);
            
        } catch (Exception e) {
            log.error("Failed to reset database sequences: {}", e.getMessage());
            // Don't fail startup, just log the error
        }
    }
}
