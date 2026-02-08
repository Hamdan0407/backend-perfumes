package com.perfume.shop.controller;

import com.perfume.shop.entity.User;
import com.perfume.shop.entity.Wishlist;
import com.perfume.shop.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class WishlistController {
    
    private final WishlistService wishlistService;
    
    @PostMapping("/{productId}")
    public ResponseEntity<Wishlist> addToWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(wishlistService.addToWishlist(user, productId));
    }
    
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId
    ) {
        wishlistService.removeFromWishlist(user, productId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    public ResponseEntity<List<Wishlist>> getUserWishlist(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(wishlistService.getUserWishlist(user));
    }
    
    @GetMapping("/product-ids")
    public ResponseEntity<List<Long>> getUserWishlistProductIds(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(wishlistService.getUserWishlistProductIds(user));
    }
    
    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkInWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId
    ) {
        boolean inWishlist = wishlistService.isInWishlist(user, productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getWishlistCount(
            @AuthenticationPrincipal User user
    ) {
        long count = wishlistService.getWishlistCount(user);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
