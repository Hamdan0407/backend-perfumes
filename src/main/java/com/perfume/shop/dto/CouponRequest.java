package com.perfume.shop.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRequest {
    
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code must contain only uppercase letters, numbers, hyphens and underscores")
    private String code;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    @NotNull(message = "Discount type is required")
    private String discountType; // PERCENTAGE or FIXED_AMOUNT
    
    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
    private BigDecimal discountValue;
    
    @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;
    
    @DecimalMin(value = "0.00", message = "Maximum discount amount cannot be negative")
    private BigDecimal maxDiscountAmount;
    
    @NotNull(message = "Usage limit is required")
    @Min(value = 1, message = "Usage limit must be at least 1")
    private Integer usageLimit;
    
    @NotNull(message = "Valid from date is required")
    private LocalDateTime validFrom;
    
    @NotNull(message = "Valid until date is required")
    private LocalDateTime validUntil;
    
    private Boolean active;
}
