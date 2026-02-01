package com.perfume.shop.exception;

/**
 * Base exception class for business logic exceptions.
 * 
 * Provides consistent exception handling with error types and HTTP status mapping.
 */
public class ApplicationException extends RuntimeException {
    
    private final ErrorType errorType;
    private final int httpStatus;
    
    /**
     * Constructor with error type and HTTP status
     * 
     * @param message User-friendly error message
     * @param errorType Classification of error for client handling
     * @param httpStatus HTTP status code to return
     */
    public ApplicationException(String message, ErrorType errorType, int httpStatus) {
        super(message);
        this.errorType = errorType;
        this.httpStatus = httpStatus;
    }
    
    /**
     * Constructor with message and error type (defaults to 400 Bad Request)
     */
    public ApplicationException(String message, ErrorType errorType) {
        this(message, errorType, 400);
    }
    
    /**
     * Constructor with message (defaults to INTERNAL_ERROR and 500)
     */
    public ApplicationException(String message) {
        this(message, ErrorType.INTERNAL_ERROR, 500);
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public int getHttpStatus() {
        return httpStatus;
    }
}
