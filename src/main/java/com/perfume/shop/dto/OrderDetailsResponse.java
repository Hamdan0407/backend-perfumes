package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detailed order response for admin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailsResponse {
    
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String trackingNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
