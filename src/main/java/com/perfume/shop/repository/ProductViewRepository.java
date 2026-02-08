package com.perfume.shop.repository;

import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    
    // Find recent view by user and product
    Optional<ProductView> findFirstByUserIdAndProductIdOrderByViewedAtDesc(Long userId, Long productId);
    
    // Find recent view by session and product
    Optional<ProductView> findFirstBySessionIdAndProductIdOrderByViewedAtDesc(String sessionId, Long productId);
    
    // Get recently viewed products for authenticated user
    @Query("SELECT pv.product FROM ProductView pv WHERE pv.user.id = :userId " +
           "AND pv.product.active = true " +
           "GROUP BY pv.product.id " +
           "ORDER BY MAX(pv.viewedAt) DESC")
    List<Product> findRecentlyViewedByUserId(@Param("userId") Long userId);
    
    // Get recently viewed products for guest (by session)
    @Query("SELECT pv.product FROM ProductView pv WHERE pv.sessionId = :sessionId " +
           "AND pv.product.active = true " +
           "GROUP BY pv.product.id " +
           "ORDER BY MAX(pv.viewedAt) DESC")
    List<Product> findRecentlyViewedBySessionId(@Param("sessionId") String sessionId);
    
    // Delete old views (cleanup)
    void deleteByViewedAtBefore(LocalDateTime date);
    
    // Count views for a product
    Long countByProductId(Long productId);
}
