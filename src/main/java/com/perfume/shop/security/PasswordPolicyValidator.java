package com.perfume.shop.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Password policy validator enforcing security requirements.
 * 
 * Password must contain:
 * - Minimum 6 characters
 * - At least one letter (uppercase or lowercase)
 * - At least one digit OR special character
 * 
 * In development mode (disable-password-policy: true), only checks minimum length.
 */
@Component
@Slf4j
public class PasswordPolicyValidator {
    
    private static final int MIN_LENGTH = 6;
    private static final int MAX_LENGTH = 128;
    
    @Value("${app.security.disable-password-policy:false}")
    private boolean disablePasswordPolicy;
    
    // Pattern: at least one letter and one digit/special char, min 6 chars
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*[\\d@$!%*?&])[A-Za-z\\d@$!%*?&]{6,128}$"
    );
    
    /**
     * Validate password against security policy
     * @param password Password to validate
     * @return true if valid, false otherwise
     * @throws PasswordPolicyException if password doesn't meet requirements
     */
    public void validate(String password) throws PasswordPolicyException {
        // In development mode, skip complex validation
        if (disablePasswordPolicy) {
            log.debug("Password policy validation disabled (development mode)");
            if (password == null || password.isBlank()) {
                throw new PasswordPolicyException("Password cannot be empty");
            }
            if (password.length() < MIN_LENGTH) {
                throw new PasswordPolicyException("Password must be at least " + MIN_LENGTH + " characters");
            }
            return;
        }
        
        // Production validation
        if (password == null || password.isBlank()) {
            throw new PasswordPolicyException("Password cannot be empty");
        }
        
        if (password.length() < MIN_LENGTH) {
            throw new PasswordPolicyException("Password must be at least " + MIN_LENGTH + " characters");

        }
        
        if (password.length() > MAX_LENGTH) {
            throw new PasswordPolicyException("Password cannot exceed " + MAX_LENGTH + " characters");
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new PasswordPolicyException(
                    "Password must contain: letters and at least one digit or special character (@$!%*?&)"
            );
        }
        
        // Check for common weak passwords
        if (isCommonPassword(password)) {
            throw new PasswordPolicyException("Password is too common. Choose a more unique password");
        }
        
        // Check for sequential characters
        if (containsSequentialChars(password)) {
            throw new PasswordPolicyException("Password contains sequential characters (abc, 123, etc.)");
        }
        
        log.debug("Password validation successful");
    }
    
    /**
     * Check if password is in common passwords list
     */
    private boolean isCommonPassword(String password) {
        String[] commonPasswords = {
                "password123", "admin123", "qwerty123", "12345678",
                "letmein", "welcome123", "monkey123", "sunshine123"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.contains(common)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check for sequential characters like abc, 123, xyz
     */
    private boolean containsSequentialChars(String password) {
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            // Check sequential alphabet or digits
            if ((c2 == c1 + 1 && c3 == c2 + 1) || (c2 == c1 - 1 && c3 == c2 - 1)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Custom exception for password policy violations
     */
    public static class PasswordPolicyException extends RuntimeException {
        public PasswordPolicyException(String message) {
            super(message);
        }
        
        public PasswordPolicyException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
