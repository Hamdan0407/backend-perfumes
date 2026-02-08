package com.perfume.shop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Coupon entity for discount codes.
 * Supports percentage and fixed amount discounts.
 */
@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    @Column(nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal minOrderAmount;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal maxDiscountAmount;
    
    @Column(nullable = false)
    private Integer usageLimit;
    
    @Column(nullable = false)
    private Integer usedCount = 0;
    
    @Column(nullable = false)
    private LocalDateTime validFrom;
    
    @Column(nullable = false)
    private LocalDateTime validUntil;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum DiscountType {
        PERCENTAGE,  // e.g., 10% off
        FIXED_AMOUNT // e.g., ₹100 off
    }
    
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return active 
            && now.isAfter(validFrom) 
            && now.isBefore(validUntil)
            && usedCount < usageLimit;
    }
    
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid()) {
            throw new RuntimeException("Coupon is not valid");
        }
        
        if (minOrderAmount != null && orderAmount.compareTo(minOrderAmount) < 0) {
            throw new RuntimeException("Minimum order amount not met");
        }
        
        BigDecimal discount;
        if (discountType == DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(discountValue.divide(new BigDecimal(100)));
        } else {
            discount = discountValue;
        }
        
        // Apply max discount limit if set
        if (maxDiscountAmount != null && discount.compareTo(maxDiscountAmount) > 0) {
            discount = maxDiscountAmount;
        }
        
        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        
        return discount;
    }
    
    public void incrementUsage() {
        this.usedCount++;
    }
}
