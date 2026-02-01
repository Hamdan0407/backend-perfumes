package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication fails (invalid credentials).
 * 
 * HTTP Status: 401 Unauthorized
 * Error Type: AUTHENTICATION_ERROR
 */
public class AuthenticationException extends ApplicationException {
    
    public AuthenticationException(String message) {
        super(
                message,
                ErrorType.AUTHENTICATION_ERROR,
                HttpStatus.UNAUTHORIZED.value()
        );
    }
    
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }
    
    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Token has expired. Please login again");
    }
    
    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid or malformed token");
    }
    
    public static AuthenticationException tokenRefreshFailed() {
        return new AuthenticationException("Token refresh failed. Please login again");
    }
}
