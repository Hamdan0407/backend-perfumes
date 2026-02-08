package com.perfume.shop.service;

import com.perfume.shop.dto.ReviewRequest;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.Review;
import com.perfume.shop.entity.User;
import com.perfume.shop.exception.ResourceNotFoundException;
import com.perfume.shop.repository.ReviewRepository;
import com.perfume.shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final OrderRepository orderRepository;
    
    @Transactional
    public Review createReview(User user, ReviewRequest request) {
        Product product = productService.getProductEntityById(request.getProductId());
        
        // Check if user has purchased this product
        if (!orderRepository.hasUserPurchasedProduct(user.getId(), product.getId())) {
            throw new RuntimeException("You can only review products you have purchased");
        }
        
        // Check if user already reviewed this product
        if (reviewRepository.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new RuntimeException("You have already reviewed this product");
        }
        
        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .userName(user.getFirstName() + " " + user.getLastName())
                .build();
        
        review = reviewRepository.save(review);
        
        // Update product rating
        productService.updateProductRating(product.getId());
        
        return review;
    }
    
    public Page<Review> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    }
    
    @Transactional
    public Review updateReview(Long reviewId, User user, ReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);
        
        // Update product rating
        productService.updateProductRating(review.getProduct().getId());
        
        return review;
    }
    
    @Transactional
    public void deleteReview(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId.toString()));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        
        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);
        
        // Update product rating
        productService.updateProductRating(productId);
    }
}
