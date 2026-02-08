package com.perfume.shop.service;

import com.perfume.shop.dto.ProductResponse;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.ProductView;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductViewService {
    
    private final ProductViewRepository productViewRepository;
    private final ProductService productService;
    
    /**
     * Track product view - async to not block main request
     */
    @Async
    @Transactional
    public void trackProductView(Long productId, User user, String sessionId) {
        try {
            Product product = productService.getProductEntityById(productId);
            
            // Check if user/session already viewed this product recently (within last hour)
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            
            if (user != null) {
                // For authenticated users
                var recentView = productViewRepository
                    .findFirstByUserIdAndProductIdOrderByViewedAtDesc(user.getId(), productId);
                
                if (recentView.isPresent() && recentView.get().getViewedAt().isAfter(oneHourAgo)) {
                    // Update existing view timestamp
                    recentView.get().setViewedAt(LocalDateTime.now());
                    productViewRepository.save(recentView.get());
                    return;
                }
            } else if (sessionId != null) {
                // For guest users
                var recentView = productViewRepository
                    .findFirstBySessionIdAndProductIdOrderByViewedAtDesc(sessionId, productId);
                
                if (recentView.isPresent() && recentView.get().getViewedAt().isAfter(oneHourAgo)) {
                    // Update existing view timestamp
                    recentView.get().setViewedAt(LocalDateTime.now());
                    productViewRepository.save(recentView.get());
                    return;
                }
            }
            
            // Create new view record
            ProductView productView = ProductView.builder()
                    .product(product)
                    .user(user)
                    .sessionId(sessionId)
                    .build();
            
            productViewRepository.save(productView);
            log.debug("Tracked product view: productId={}, userId={}, sessionId={}", 
                     productId, user != null ? user.getId() : null, sessionId);
            
        } catch (Exception e) {
            log.error("Failed to track product view: {}", e.getMessage());
        }
    }
    
    /**
     * Get recently viewed products for user or guest
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getRecentlyViewed(User user, String sessionId, int limit) {
        List<Product> products;
        
        if (user != null) {
            products = productViewRepository.findRecentlyViewedByUserId(user.getId());
        } else if (sessionId != null) {
            products = productViewRepository.findRecentlyViewedBySessionId(sessionId);
        } else {
            return List.of();
        }
        
        return products.stream()
                .limit(limit)
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Cleanup old product views (older than 90 days)
     */
    @Transactional
    public void cleanupOldViews() {
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        productViewRepository.deleteByViewedAtBefore(ninetyDaysAgo);
        log.info("Cleaned up product views older than 90 days");
    }
    
    /**
     * Get view count for a product
     */
    public Long getViewCount(Long productId) {
        return productViewRepository.countByProductId(productId);
    }
}
