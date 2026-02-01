package com.perfume.shop.repository;

import com.perfume.shop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    
    Boolean existsByUserIdAndProductId(Long userId, Long productId);
}
