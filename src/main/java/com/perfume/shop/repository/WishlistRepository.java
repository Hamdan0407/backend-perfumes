package com.perfume.shop.repository;

import com.perfume.shop.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    List<Wishlist> findByUserIdOrderByAddedAtDesc(Long userId);
    
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    @Query("SELECT w.product.id FROM Wishlist w WHERE w.user.id = :userId")
    List<Long> findProductIdsByUserId(Long userId);
    
    long countByUserId(Long userId);
}
