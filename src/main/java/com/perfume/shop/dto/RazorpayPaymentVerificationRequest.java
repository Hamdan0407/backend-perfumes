package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying Razorpay payment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayPaymentVerificationRequest {
    
    private String razorpayPaymentId;
    
    private String razorpayOrderId;
    
    private String razorpaySignature;
}
