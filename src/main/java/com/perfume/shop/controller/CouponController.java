package com.perfume.shop.controller;

import com.perfume.shop.dto.CouponResponse;
import com.perfume.shop.dto.CouponValidationResponse;
import com.perfume.shop.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Coupon Controller - Public endpoints for coupon validation and listing.
 */
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {
    
    private final CouponService couponService;
    
    /**
     * Get all active coupons (public view).
     * 
     * @return List of active and valid coupons
     */
    @GetMapping("/active")
    public ResponseEntity<List<CouponResponse>> getActiveCoupons() {
        return ResponseEntity.ok(couponService.getActiveCoupons());
    }
    
    /**
     * Validate coupon code and calculate discount.
     * 
     * @param code Coupon code
     * @param orderAmount Order subtotal amount
     * @return Validation result with discount details
     */
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestBody Map<String, Object> request
    ) {
        String code = (String) request.get("code");
        BigDecimal orderAmount = new BigDecimal(request.get("orderAmount").toString());
        
        return ResponseEntity.ok(couponService.validateCoupon(code, orderAmount));
    }
}
