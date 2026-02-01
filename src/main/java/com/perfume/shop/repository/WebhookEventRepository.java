package com.perfume.shop.repository;

import com.perfume.shop.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    
    /**
     * Find webhook event by Razorpay event ID for idempotency check
     */
    Optional<WebhookEvent> findByEventId(String eventId);
}
