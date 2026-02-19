package com.perfume.shop.repository;

import com.perfume.shop.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /**
     * Find all reviews for a product
     */
    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * Find review by user and product (to check if already reviewed)
     */
    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);

    /**
     * Find all reviews by a user
     */
    List<ProductReview> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get average rating for a product
     */
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Double getAverageRating(@Param("productId") Long productId);

    /**
     * Count reviews for a product
     */
    long countByProductId(Long productId);

    /**
     * Get rating distribution for a product
     */
    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r WHERE r.product.id = :productId GROUP BY r.rating")
    List<Object[]> getRatingDistribution(@Param("productId") Long productId);
}
