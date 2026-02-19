package com.perfume.shop.controller;

import com.perfume.shop.dto.ProductReviewRequest;
import com.perfume.shop.dto.ProductReviewResponse;
import com.perfume.shop.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class ProductReviewController {

    private final ProductReviewService reviewService;

    /**
     * Submit a product review
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ProductReviewResponse> submitReview(
            @Valid @RequestBody ProductReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            log.info("Review submission request from: {}", userDetails.getUsername());
            ProductReviewResponse response = reviewService.submitReview(request, userDetails.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error submitting review: ", e);
            return ResponseEntity.badRequest().body(null); // Or return a custom error object if frontend expects it
        } catch (Exception e) {
            log.error("Unexpected error submitting review: ", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get reviews for a product
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<Page<ProductReviewResponse>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductReviewResponse> reviews = reviewService.getProductReviews(
                productId, PageRequest.of(page, size));
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review statistics for a product
     */
    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<Map<String, Object>> getReviewStats(@PathVariable Long productId) {
        Map<String, Object> stats = reviewService.getReviewStats(productId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Check if user can review a product
     */
    @GetMapping("/can-review/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean canReview = reviewService.canUserReview(productId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    /**
     * Update a review
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ProductReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProductReviewResponse response = reviewService.updateReview(
                reviewId, request, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        reviewService.deleteReview(reviewId, userDetails.getUsername());
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}
