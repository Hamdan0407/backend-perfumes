package com.perfume.shop.dto;

import com.perfume.shop.entity.Coupon;
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
public class CouponResponse {

    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usageLimitPerUser;
    private Integer usedCount;
    private Integer remainingUses;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
    private Boolean isExpired;
    private Boolean isUsedUp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CouponResponse fromEntity(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        int remaining = coupon.getUsageLimit() - coupon.getUsedCount();

        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minOrderAmount(coupon.getMinOrderAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimit(coupon.getUsageLimit())
                .usageLimitPerUser(coupon.getUsageLimitPerUser()) // Mapped new field
                .usedCount(coupon.getUsedCount())
                .remainingUses(remaining)
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .isExpired(now.isAfter(coupon.getValidUntil()))
                .isUsedUp(remaining <= 0)
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
