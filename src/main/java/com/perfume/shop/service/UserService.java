package com.perfume.shop.service;

import com.perfume.shop.dto.ChangePasswordRequest;
import com.perfume.shop.dto.UserProfileUpdateRequest;
import com.perfume.shop.entity.User;
import com.perfume.shop.exception.UserNotFoundException;
import com.perfume.shop.repository.UserRepository;
import com.perfume.shop.security.PasswordPolicyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service - Manages user-related operations
 * Handles profile updates, password changes, and user data management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    
    /**
     * Get user by ID
     * 
     * @param userId User ID
     * @return User entity
     */
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    
    /**
     * Update user profile information
     * 
     * @param userId User ID
     * @param request Profile update request
     * @return Updated user
     */
    @Transactional
    public User updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        User user = getUserById(userId);
        
        // Update profile information
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());
        user.setZipCode(request.getZipCode());
        
        User updatedUser = userRepository.save(user);
        log.info("User profile updated: {}", user.getEmail());
        
        return updatedUser;
    }
    
    /**
     * Change user password
     * Validates current password before allowing change
     * 
     * @param userId User ID
     * @param request Change password request
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            log.warn("Incorrect current password for user: {}", user.getEmail());
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Validate new password policy
        passwordPolicyValidator.validate(request.getNewPassword());
        
        // Verify passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirmation do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        log.info("Password changed successfully for user: {}", user.getEmail());
    }
}
