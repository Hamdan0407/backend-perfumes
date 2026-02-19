package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Product Review/Rating Entity
 * Only users who purchased the product can leave a review
 */
@Entity
@Table(name = "product_reviews", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id",
        "user_id" }), indexes = {
                @Index(name = "idx_review_product", columnList = "product_id"),
                @Index(name = "idx_review_user", columnList = "user_id"),
                @Index(name = "idx_review_rating", columnList = "rating"),
                @Index(name = "idx_review_created", columnList = "created_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = true; // Verified purchase

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime updatedAt;

    // Helper method to check if rating is valid
    public boolean isValidRating() {
        return rating != null && rating >= 1 && rating <= 5;
    }
}
