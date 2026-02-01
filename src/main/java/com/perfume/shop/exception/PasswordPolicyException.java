package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when password fails policy validation.
 * 
 * HTTP Status: 400 Bad Request
 * Error Type: PRECONDITION_FAILED
 */
public class PasswordPolicyException extends ApplicationException {
    
    public PasswordPolicyException(String message) {
        super(
                message,
                ErrorType.PRECONDITION_FAILED,
                HttpStatus.BAD_REQUEST.value()
        );
    }
    
    public PasswordPolicyException(String message, String details) {
        super(
                message + ". " + details,
                ErrorType.PRECONDITION_FAILED,
                HttpStatus.BAD_REQUEST.value()
        );
    }
}
