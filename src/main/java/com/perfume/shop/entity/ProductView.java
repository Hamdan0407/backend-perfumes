package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_views", indexes = {
    @Index(name = "idx_product_view_user", columnList = "user_id"),
    @Index(name = "idx_product_view_session", columnList = "session_id"),
    @Index(name = "idx_product_view_product", columnList = "product_id"),
    @Index(name = "idx_product_view_timestamp", columnList = "viewed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
    
    @PrePersist
    protected void onCreate() {
        viewedAt = LocalDateTime.now();
    }
}
