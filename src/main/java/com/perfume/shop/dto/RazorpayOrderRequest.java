package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating Razorpay Orders
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderRequest {
    
    private Long amount; // Amount in smallest currency unit (paise for INR, cents for USD)
    
    private String currency;
    
    private String receipt; // Order number for reference
    
    private String customerId;
    
    private String customerName;
    
    private String customerEmail;
    
    private String customerPhone;
}
