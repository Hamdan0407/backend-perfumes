package com.perfume.shop.controller;

import com.perfume.shop.dto.ChangePasswordRequest;
import com.perfume.shop.dto.UserProfileUpdateRequest;
import com.perfume.shop.entity.User;
import com.perfume.shop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User Profile Controller - Manages user profile information
 * Endpoints for viewing and updating user details (address, phone, etc.)
 * All endpoints require authentication
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {
    
    private final UserService userService;
    
    /**
     * Get current user profile
     * 
     * @param user Authenticated user
     * @return User profile details
     */
    @GetMapping("/profile")
    public ResponseEntity<User> getProfile(@AuthenticationPrincipal User user) {
        log.info("Fetching profile for user: {}", user.getEmail());
        return ResponseEntity.ok(user);
    }
    
    /**
     * Update user profile information (address, phone, city, etc.)
     * 
     * @param user Authenticated user
     * @param request Profile update request
     * @return Updated user details
     */
    @PutMapping("/profile")
    public ResponseEntity<User> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        log.info("Updating profile for user: {}", user.getEmail());
        
        User updatedUser = userService.updateUserProfile(user.getId(), request);
        
        log.info("Profile updated successfully for user: {}", user.getEmail());
        return ResponseEntity.ok(updatedUser);
    }
    
    /**
     * Change user password
     * Validates current password before allowing change
     * 
     * @param user Authenticated user
     * @param request Change password request with current and new password
     * @return Success message
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        log.info("Changing password for user: {}", user.getEmail());
        
        // Validate new password matches confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password mismatch for user: {}", user.getEmail());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "New password and confirmation do not match"
            ));
        }
        
        userService.changePassword(user.getId(), request);
        
        log.info("Password changed successfully for user: {}", user.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Password changed successfully"
        ));
    }
}
