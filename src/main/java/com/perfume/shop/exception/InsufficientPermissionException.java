package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when user lacks required permissions (authorization failure).
 * 
 * HTTP Status: 403 Forbidden
 * Error Type: AUTHORIZATION_ERROR
 */
public class InsufficientPermissionException extends ApplicationException {
    
    public InsufficientPermissionException(String message) {
        super(
                message,
                ErrorType.AUTHORIZATION_ERROR,
                HttpStatus.FORBIDDEN.value()
        );
    }
    
    public static InsufficientPermissionException requiresRole(String role) {
        return new InsufficientPermissionException(
                String.format("This operation requires %s role", role)
        );
    }
    
    public static InsufficientPermissionException accessDenied() {
        return new InsufficientPermissionException("You do not have permission to perform this action");
    }
}
