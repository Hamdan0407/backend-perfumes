package com.perfume.shop.controller;

import com.perfume.shop.dto.ProductVariantRequest;
import com.perfume.shop.dto.ProductVariantResponse;
import com.perfume.shop.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class ProductVariantController {

    private final ProductVariantService variantService;

    /**
     * Get all variants for a product (Public)
     */
    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> getProductVariants(@PathVariable Long productId) {
        List<ProductVariantResponse> variants = variantService.getVariantsByProduct(productId);
        return ResponseEntity.ok(variants);
    }

    /**
     * Create a new variant (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {

        ProductVariantResponse response = variantService.createVariant(productId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a variant (Admin only)
     */
    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {

        ProductVariantResponse response = variantService.updateVariant(variantId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a variant (Admin only)
     */
    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {

        variantService.deleteVariant(variantId);
        return ResponseEntity.noContent().build();
    }
}
