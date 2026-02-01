package com.perfume.shop.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses across all API endpoints.
 * 
 * Handles:
 * - Application exceptions (custom business logic exceptions)
 * - Validation exceptions (request validation failures)
 * - Authentication exceptions (invalid credentials)
 * - Security exceptions (authorization failures)
 * - Generic exceptions (unexpected errors)
 * 
 * Returns ErrorResponse with:
 * - HTTP status code
 * - Error type classification
 * - User-friendly message
 * - Field-level errors (if applicable)
 * - Request path and timestamp for debugging
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * Handle custom ApplicationException and its subclasses
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            ApplicationException ex,
            WebRequest request) {
        
        log.warn("ApplicationException: {} [{}]", ex.getErrorType(), ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                ex.getHttpStatus(),
                ex.getErrorType().name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }
    
    /**
     * Handle validation errors from @Valid annotation
     * Extracts field-level validation errors for client feedback
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.warn("Validation failed for request: {}", getRequestPath(request));
        
        // Extract field-level errors
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
            log.debug("Field validation error: {} = {}", fieldName, errorMessage);
        });
        
        ErrorResponse response = ErrorResponse.ofValidation(
                "Request validation failed. Please check the errors below.",
                fieldErrors,
                getRequestPath(request)
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle email already exists (conflict)
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex,
            WebRequest request) {
        
        log.warn("Email conflict: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                ErrorType.CONFLICT.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Handle user not found
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex,
            WebRequest request) {
        
        log.warn("User not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ErrorType.NOT_FOUND.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle password policy violations
     */
    @ExceptionHandler(PasswordPolicyException.class)
    public ResponseEntity<ErrorResponse> handlePasswordPolicyException(
            PasswordPolicyException ex,
            WebRequest request) {
        
        log.warn("Password policy violation: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                ErrorType.PRECONDITION_FAILED.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Handle authentication failures (invalid credentials)
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        
        log.warn("Authentication failed: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorType.AUTHENTICATION_ERROR.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Handle Spring Security BadCredentialsException
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {
        
        log.warn("Bad credentials provided");
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                ErrorType.AUTHENTICATION_ERROR.name(),
                "Invalid email or password",
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Handle Spring Security UsernameNotFoundException
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex,
            WebRequest request) {
        
        log.warn("User not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ErrorType.NOT_FOUND.name(),
                "User not found",
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle insufficient permission (authorization failure)
     */
    @ExceptionHandler(InsufficientPermissionException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientPermissionException(
            InsufficientPermissionException ex,
            WebRequest request) {
        
        log.warn("Authorization denied: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                ErrorType.AUTHORIZATION_ERROR.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * Handle resource not found (generic 404)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ErrorType.NOT_FOUND.name(),
                ex.getMessage(),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle 404 not found (endpoint doesn't exist)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            WebRequest request) {
        
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                ErrorType.NOT_FOUND.name(),
                String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()),
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    /**
     * Handle all other RuntimeExceptions
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        log.error("Unexpected runtime exception", ex);
        
        // Generic message to avoid leaking internal details
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorType.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please try again later.",
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Handle all other exceptions not handled by specific handlers
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected exception", ex);
        
        // Generic message to avoid leaking internal details
        ErrorResponse response = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ErrorType.INTERNAL_ERROR.name(),
                "An unexpected error occurred. Please try again later.",
                getRequestPath(request)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * Extract request path from WebRequest
     */
    private String getRequestPath(WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
}
