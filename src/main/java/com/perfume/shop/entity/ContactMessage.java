package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store contact form submissions
 */
@Entity
@Table(name = "contact_messages", indexes = {
        @Index(name = "idx_contact_email", columnList = "email"),
        @Index(name = "idx_contact_status", columnList = "status"),
        @Index(name = "idx_contact_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.NEW;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime respondedAt;

    @Column(length = 500)
    private String adminNotes;

    public enum MessageStatus {
        NEW, // Just received
        READ, // Admin has read
        RESPONDED, // Admin has responded
        ARCHIVED // Archived
    }
}
