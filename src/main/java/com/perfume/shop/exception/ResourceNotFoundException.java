package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when resource is not found (generic).
 * 
 * HTTP Status: 404 Not Found
 * Error Type: NOT_FOUND
 */
public class ResourceNotFoundException extends ApplicationException {
    
    public ResourceNotFoundException(String resourceName, String identifier) {
        super(
                String.format("%s with ID '%s' not found", resourceName, identifier),
                ErrorType.NOT_FOUND,
                HttpStatus.NOT_FOUND.value()
        );
    }
    
    public ResourceNotFoundException(String message) {
        super(
                message,
                ErrorType.NOT_FOUND,
                HttpStatus.NOT_FOUND.value()
        );
    }
}
