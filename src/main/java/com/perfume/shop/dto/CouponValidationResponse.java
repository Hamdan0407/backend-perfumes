package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidationResponse {
    
    private Boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private CouponResponse coupon;
    
    public static CouponValidationResponse success(
            CouponResponse coupon, 
            BigDecimal discountAmount, 
            BigDecimal originalAmount
    ) {
        return CouponValidationResponse.builder()
                .valid(true)
                .message("Coupon applied successfully!")
                .discountAmount(discountAmount)
                .originalAmount(originalAmount)
                .finalAmount(originalAmount.subtract(discountAmount))
                .coupon(coupon)
                .build();
    }
    
    public static CouponValidationResponse error(String message) {
        return CouponValidationResponse.builder()
                .valid(false)
                .message(message)
                .build();
    }
}
