package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track webhook events for idempotency.
 * Prevents duplicate processing of the same webhook event.
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_event_id", columnList = "eventId"),
    @Index(name = "idx_webhook_event_type", columnList = "eventType"),
    @Index(name = "idx_webhook_processed", columnList = "processed"),
    @Index(name = "idx_webhook_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String eventId; // Razorpay event ID
    
    @Column(nullable = false, length = 50)
    private String eventType; // payment.authorized, payment.failed, etc.
    
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload; // Raw webhook payload
    
    @Column(nullable = false)
    private Boolean processed = false;
    
    @Column(length = 500)
    private String processingResult;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime processedAt = null;
    
    @Column(length = 1000)
    private String errorMessage;
}
