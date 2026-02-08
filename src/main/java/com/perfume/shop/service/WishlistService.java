package com.perfume.shop.service;

import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.User;
import com.perfume.shop.entity.Wishlist;
import com.perfume.shop.repository.ProductRepository;
import com.perfume.shop.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public Wishlist addToWishlist(User user, Long productId) {
        // Check if already in wishlist
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new RuntimeException("Product already in wishlist");
        }
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        
        log.info("User {} added product {} to wishlist", user.getId(), productId);
        return wishlistRepository.save(wishlist);
    }
    
    @Transactional
    public void removeFromWishlist(User user, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
        log.info("User {} removed product {} from wishlist", user.getId(), productId);
    }
    
    public List<Wishlist> getUserWishlist(User user) {
        return wishlistRepository.findByUserIdOrderByAddedAtDesc(user.getId());
    }
    
    public List<Long> getUserWishlistProductIds(User user) {
        return wishlistRepository.findProductIdsByUserId(user.getId());
    }
    
    public boolean isInWishlist(User user, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }
    
    public long getWishlistCount(User user) {
        return wishlistRepository.countByUserId(user.getId());
    }
}
