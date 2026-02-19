package com.perfume.shop.service;

import com.perfume.shop.dto.CouponRequest;
import com.perfume.shop.dto.CouponResponse;
import com.perfume.shop.dto.CouponValidationResponse;
import com.perfume.shop.entity.Coupon;
import com.perfume.shop.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final com.perfume.shop.repository.CouponUsageRepository couponUsageRepository;

    /**
     * Get all coupons
     */
    public List<CouponResponse> getAllCoupons() {
        try {
            return couponRepository.findAll().stream()
                    .map(CouponResponse::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not fetch coupons from database (table might not exist yet): {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Get active coupons only
     */
    public List<CouponResponse> getActiveCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return couponRepository.findAllActiveCoupons(now).stream()
                .map(CouponResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get coupon by ID
     */
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
        return CouponResponse.fromEntity(coupon);
    }

    /**
     * Create new coupon
     */
    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        // Check if code already exists
        if (couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new RuntimeException("Coupon code already exists");
        }

        // Validate dates
        if (request.getValidUntil().isBefore(request.getValidFrom())) {
            throw new RuntimeException("Valid until date must be after valid from date");
        }

        // Validate percentage discount
        if ("PERCENTAGE".equals(request.getDiscountType()) &&
                request.getDiscountValue().compareTo(new BigDecimal(100)) > 0) {
            throw new RuntimeException("Percentage discount cannot exceed 100%");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(Coupon.DiscountType.valueOf(request.getDiscountType()))
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .usageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : 1)
                .usedCount(0)
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        coupon = couponRepository.save(coupon);
        log.info("Created coupon: {}", coupon.getCode());

        return CouponResponse.fromEntity(coupon);
    }

    /**
     * Update existing coupon
     */
    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        // Check if new code conflicts with another coupon
        if (!coupon.getCode().equalsIgnoreCase(request.getCode()) &&
                couponRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new RuntimeException("Coupon code already exists");
        }

        // Validate dates
        if (request.getValidUntil().isBefore(request.getValidFrom())) {
            throw new RuntimeException("Valid until date must be after valid from date");
        }

        // Validate percentage discount
        if ("PERCENTAGE".equals(request.getDiscountType()) &&
                request.getDiscountValue().compareTo(new BigDecimal(100)) > 0) {
            throw new RuntimeException("Percentage discount cannot exceed 100%");
        }

        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(Coupon.DiscountType.valueOf(request.getDiscountType()));
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) {
            coupon.setActive(request.getActive());
        }

        coupon = couponRepository.save(coupon);
        log.info("Updated coupon: {}", coupon.getCode());

        return CouponResponse.fromEntity(coupon);
    }

    /**
     * Delete coupon
     */
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        couponRepository.delete(coupon);
        log.info("Deleted coupon: {}", coupon.getCode());
    }

    /**
     * Toggle coupon active status
     */
    @Transactional
    public CouponResponse toggleCouponStatus(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        coupon.setActive(!coupon.getActive());
        coupon = couponRepository.save(coupon);
        log.info("Toggled coupon {} status to: {}", coupon.getCode(), coupon.getActive());

        return CouponResponse.fromEntity(coupon);
    }

    /**
     * Validate and calculate discount for a coupon code
     */
    public CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount) {
        // For backwards compatibility or when user is not logged in yet
        return validateCoupon(code, orderAmount, null);
    }

    /**
     * Validate and calculate discount for a coupon code with user check
     */
    public CouponValidationResponse validateCoupon(String code, BigDecimal orderAmount,
            com.perfume.shop.entity.User user) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElse(null);

        if (coupon == null) {
            return CouponValidationResponse.error("Invalid coupon code");
        }

        if (!coupon.getActive()) {
            return CouponValidationResponse.error("This coupon is no longer active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getValidFrom())) {
            return CouponValidationResponse.error("This coupon is not yet valid");
        }

        if (now.isAfter(coupon.getValidUntil())) {
            return CouponValidationResponse.error("This coupon has expired");
        }

        if (coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return CouponValidationResponse.error("This coupon has reached its global usage limit");
        }

        // Check per-user usage limit if user is provided
        if (user != null && coupon.getUsageLimitPerUser() != null) {
            long userUsageCount = couponUsageRepository.countByCouponAndUser(coupon, user);
            if (userUsageCount >= coupon.getUsageLimitPerUser()) {
                return CouponValidationResponse.error("You have already used this coupon the maximum allowed times");
            }
        }

        if (coupon.getMinOrderAmount() != null &&
                orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return CouponValidationResponse.error(
                    String.format("Minimum order amount of ₹%.2f required", coupon.getMinOrderAmount()));
        }

        try {
            BigDecimal discountAmount = coupon.calculateDiscount(orderAmount);
            return CouponValidationResponse.success(
                    CouponResponse.fromEntity(coupon),
                    discountAmount,
                    orderAmount);
        } catch (Exception e) {
            return CouponValidationResponse.error(e.getMessage());
        }
    }

    /**
     * Apply coupon (increment usage count and record user usage)
     */
    @Transactional
    public void applyCoupon(String code, com.perfume.shop.entity.User user, com.perfume.shop.entity.Order order) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        if (!coupon.isValid()) {
            throw new RuntimeException("Coupon is not valid");
        }

        // Double check per-user limit to prevent race conditions
        if (user != null && coupon.getUsageLimitPerUser() != null) {
            long userUsageCount = couponUsageRepository.countByCouponAndUser(coupon, user);
            if (userUsageCount >= coupon.getUsageLimitPerUser()) {
                throw new RuntimeException("Coupon usage limit reached for this user");
            }
        }

        coupon.incrementUsage();
        couponRepository.save(coupon);

        // Record usage if user is provided
        if (user != null && order != null) {
            com.perfume.shop.entity.CouponUsage usage = com.perfume.shop.entity.CouponUsage.builder()
                    .coupon(coupon)
                    .user(user)
                    .order(order)
                    .usedAt(LocalDateTime.now())
                    .build();
            couponUsageRepository.save(usage);
        }

        log.info("Applied coupon: {} (usage: {}/{}) for user {}",
                coupon.getCode(), coupon.getUsedCount(), coupon.getUsageLimit(),
                user != null ? user.getId() : "anonymous");
    }
}
