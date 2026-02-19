package com.perfume.shop.service;

import com.perfume.shop.entity.Cart;
import com.perfume.shop.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling abandoned cart recovery
 * Automatically sends emails to users who abandoned their carts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartService {

    private final CartRepository cartRepository;
    private final EmailService emailService;

    /**
     * Check for abandoned carts every 5 minutes
     * Sends recovery emails for carts abandoned for 30+ minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void processAbandonedCarts() {
        log.info("🔍 Checking for abandoned carts...");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Cart> abandonedCarts = cartRepository.findAbandonedCarts(threshold);

        if (abandonedCarts.isEmpty()) {
            log.debug("No abandoned carts found");
            return;
        }

        log.info("📧 Found {} abandoned cart(s), sending recovery emails...", abandonedCarts.size());

        int successCount = 0;
        int failureCount = 0;

        for (Cart cart : abandonedCarts) {
            try {
                emailService.sendAbandonedCartEmail(cart);
                cart.setAbandonedEmailSent(true);
                cartRepository.save(cart);
                successCount++;
                log.info("✅ Sent abandoned cart email to: {}", cart.getUser().getEmail());
            } catch (Exception e) {
                failureCount++;
                log.error("❌ Failed to send abandoned cart email for cart ID {}: {}",
                        cart.getId(), e.getMessage());
            }
        }

        log.info("📊 Abandoned cart processing complete: {} sent, {} failed",
                successCount, failureCount);
    }
}
