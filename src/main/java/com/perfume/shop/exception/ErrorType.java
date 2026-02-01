package com.perfume.shop.exception;

/**
 * Error type classification for consistent client-side error handling.
 * 
 * Each error type allows clients to handle specific error scenarios:
 * - VALIDATION_ERROR: User input validation failed
 * - AUTHENTICATION_ERROR: Missing or invalid credentials
 * - AUTHORIZATION_ERROR: Valid credentials but insufficient permissions
 * - NOT_FOUND: Resource not found
 * - CONFLICT: Resource already exists (duplicate)
 * - PRECONDITION_FAILED: Business logic validation failed
 * - INTERNAL_ERROR: Unexpected server error
 */
public enum ErrorType {
    VALIDATION_ERROR,
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR,
    NOT_FOUND,
    CONFLICT,
    PRECONDITION_FAILED,
    INTERNAL_ERROR
}
