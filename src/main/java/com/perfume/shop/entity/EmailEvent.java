package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track email sending attempts for reliability and audit.
 * Supports retry logic and delivery confirmation.
 */
@Entity
@Table(name = "email_events", indexes = {
        @Index(name = "idx_email_order_id", columnList = "order_id"),
        @Index(name = "idx_email_type", columnList = "emailType"),
        @Index(name = "idx_email_status", columnList = "status"),
        @Index(name = "idx_email_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 50)
    private String emailType; // CONFIRMATION, STATUS_UPDATE, SHIPPING_NOTIFICATION

    @Column(nullable = false)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status; // PENDING, SENT, FAILED

    @Column(nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(length = 1000)
    private String lastError;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    @Builder.Default
    private LocalDateTime nextRetryAt = null;

    @Column(nullable = true)
    @Builder.Default
    private LocalDateTime sentAt = null;

    public enum EmailStatus {
        PENDING, // Waiting to be sent
        SENT, // Successfully sent
        FAILED, // Failed after max retries
        BOUNCED // Email address invalid
    }

    /**
     * Check if this email can be retried
     */
    public boolean canRetry() {
        return status == EmailStatus.PENDING && attemptCount < maxRetries;
    }

    /**
     * Check if it's time to retry
     */
    public boolean isReadyForRetry() {
        return canRetry() && (nextRetryAt == null || LocalDateTime.now().isAfter(nextRetryAt));
    }
}
