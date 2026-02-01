package com.perfume.shop.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when email is already registered in the system.
 * 
 * HTTP Status: 409 Conflict
 * Error Type: CONFLICT
 */
public class EmailAlreadyExistsException extends ApplicationException {
    
    public EmailAlreadyExistsException(String email) {
        super(
                String.format("Email '%s' is already registered", email),
                ErrorType.CONFLICT,
                HttpStatus.CONFLICT.value()
        );
    }
}
