package com.perfume.shop.service;

import com.perfume.shop.dto.ProductReviewRequest;
import com.perfume.shop.dto.ProductReviewResponse;
import com.perfume.shop.entity.*;
import com.perfume.shop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Submit a product review (only for purchased products)
     */
    @Transactional
    public ProductReviewResponse submitReview(ProductReviewRequest request, String userEmail) {
        log.info("User {} submitting review for product {}", userEmail, request.getProductId());

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verify order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Order does not belong to user");
        }

        // Verify order contains the product - Use Repository instead of traversing
        // graph to avoid LazyInitializationException
        boolean productInOrder = orderRepository.existsByIdAndItemsProductId(order.getId(), request.getProductId());

        if (!productInOrder) {
            // Fallback for older orders or if custom query fails
            log.warn("Direct DB check failed for Order {} Product {}. Checking in-memory items.", order.getId(),
                    request.getProductId());
            boolean foundInMemory = order.getItems().stream()
                    .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));

            if (!foundInMemory) {
                throw new RuntimeException("Product not found in this order");
            }
        }

        // Check if user already reviewed this product
        // Check if user already reviewed this product
        reviewRepository.findByProductIdAndUserId(request.getProductId(), user.getId())
                .ifPresent(existing -> {
                    log.warn("User {} already reviewed product {}", user.getId(), request.getProductId());
                    throw new RuntimeException("You have already reviewed this product");
                });

        // Get product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Create review
        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .order(order)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .verified(true)
                .build();

        ProductReview saved = reviewRepository.save(review);
        log.info("Review submitted successfully: ID {}", saved.getId());

        return mapToResponse(saved);
    }

    /**
     * Get all reviews for a product
     */
    public Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get review statistics for a product
     */
    public Map<String, Object> getReviewStats(Long productId) {
        Map<String, Object> stats = new HashMap<>();

        Double avgRating = reviewRepository.getAverageRating(productId);
        long totalReviews = reviewRepository.countByProductId(productId);
        List<Object[]> distribution = reviewRepository.getRatingDistribution(productId);

        stats.put("averageRating", avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
        stats.put("totalReviews", totalReviews);

        // Rating distribution
        Map<Integer, Long> ratingDist = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDist.put(i, 0L);
        }
        for (Object[] row : distribution) {
            ratingDist.put((Integer) row[0], (Long) row[1]);
        }
        stats.put("ratingDistribution", ratingDist);

        return stats;
    }

    /**
     * Check if user can review a product
     */
    public boolean canUserReview(Long productId, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        // Check if already reviewed
        if (reviewRepository.findByProductIdAndUserId(productId, user.getId()).isPresent()) {
            return false;
        }

        // Check if user has purchased the product
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return orders.stream()
                .anyMatch(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getId().equals(productId)));
    }

    /**
     * Update a review
     */
    @Transactional
    public ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify review belongs to user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setComment(request.getComment());
        review.setUpdatedAt(LocalDateTime.now());

        ProductReview updated = reviewRepository.save(review);
        log.info("Review updated: ID {}", updated.getId());

        return mapToResponse(updated);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));

        // Verify review belongs to user
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
        log.info("Review deleted: ID {}", reviewId);
    }

    /**
     * Map entity to response DTO
     */
    private ProductReviewResponse mapToResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verified(review.getVerified())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
