package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when user is not found in the system.
 * 
 * HTTP Status: 404 Not Found
 * Error Type: NOT_FOUND
 */
public class UserNotFoundException extends ApplicationException {
    
    public UserNotFoundException(String email) {
        super(
                String.format("User with email '%s' not found", email),
                ErrorType.NOT_FOUND,
                HttpStatus.NOT_FOUND.value()
        );
    }
    
    public UserNotFoundException(String message, ErrorType errorType) {
        super(message, errorType, HttpStatus.NOT_FOUND.value());
    }
}
