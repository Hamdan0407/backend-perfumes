# Global Exception Handling & Request Validation Guide

This document describes the comprehensive error handling and request validation system implemented across the Perfume Shop API.

## Table of Contents

1. [Overview](#overview)
2. [Error Response Format](#error-response-format)
3. [HTTP Status Codes](#http-status-codes)
4. [Error Types](#error-types)
5. [Custom Exceptions](#custom-exceptions)
6. [Request Validation](#request-validation)
7. [Validation Examples](#validation-examples)
8. [Error Handling Flow](#error-handling-flow)
9. [Client-Side Integration](#client-side-integration)
10. [Testing Error Scenarios](#testing-error-scenarios)

---

## Overview

The API implements a **three-layer error handling architecture**:

1. **Request Validation Layer** - Jakarta Bean Validation annotations on DTOs
2. **Business Logic Layer** - Custom exceptions with semantic error information
3. **Global Handler Layer** - Centralized exception handler with consistent responses

This ensures:
- ✅ Consistent error response format across all endpoints
- ✅ Type-safe error classification for client handling
- ✅ Field-level validation error details
- ✅ Proper HTTP status codes
- ✅ Meaningful, user-friendly error messages
- ✅ Detailed logging for debugging
- ✅ No sensitive information leakage

---

## Error Response Format

All API error responses follow a standard format via `ErrorResponse` DTO:

```json
{
  "status": 400,
  "errorType": "VALIDATION_ERROR",
  "message": "Request validation failed. Please check the errors below.",
  "fieldErrors": {
    "email": "Email must be a valid email address",
    "password": "Password is required",
    "firstName": "First name must be between 2 and 50 characters"
  },
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Response Fields

| Field | Type | Description | Always Present |
|-------|------|-------------|-----------------|
| `status` | int | HTTP status code (400, 401, 404, 500, etc.) | ✅ Yes |
| `errorType` | string | Error classification for client handling | ✅ Yes |
| `message` | string | User-friendly error message | ✅ Yes |
| `fieldErrors` | object | Field-level validation errors (key-value pairs) | ❌ Only for validation errors |
| `path` | string | Request URL path (for debugging) | ✅ Yes |
| `timestamp` | string | ISO 8601 timestamp when error occurred | ✅ Yes |
| `requestId` | string | Optional unique request ID for tracing | ❌ Only if configured |

### Example Success Response

For comparison, successful responses use `ApiResponse`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "email": "user@example.com"
  },
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

---

## HTTP Status Codes

The API uses standard HTTP status codes:

| Code | Name | Meaning | When Used |
|------|------|---------|-----------|
| 200 | OK | Request succeeded | Successful API call |
| 400 | Bad Request | Invalid request data | Validation fails |
| 401 | Unauthorized | Missing/invalid credentials | Invalid token, wrong password |
| 403 | Forbidden | Valid credentials, insufficient role | User lacks required role |
| 404 | Not Found | Resource doesn't exist | User/product/order not found |
| 409 | Conflict | Resource already exists | Email already registered |
| 500 | Server Error | Unexpected error | Unhandled exceptions |

### Status Code Examples

```bash
# 200 OK - Successful login
curl -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"user@example.com","password":"Password@123"}'
# Returns: 200 with tokens

# 400 Bad Request - Validation error
curl -X POST http://localhost:8080/api/auth/register \
  -d '{"email":"invalid","password":"weak"}'
# Returns: 400 with validation errors

# 401 Unauthorized - Invalid credentials
curl -X POST http://localhost:8080/api/auth/login \
  -d '{"email":"user@example.com","password":"wrongpassword"}'
# Returns: 401 with "Invalid email or password"

# 404 Not Found - Resource doesn't exist
curl http://localhost:8080/api/users/999
# Returns: 404 with "User with ID '999' not found"

# 409 Conflict - Email already exists
curl -X POST http://localhost:8080/api/auth/register \
  -d '{"email":"existing@example.com","password":"Password@123",...}'
# Returns: 409 with "Email 'existing@example.com' is already registered"
```

---

## Error Types

Error types classify the type of error for client-side handling. These allow frontend to determine if an error is user input error, authentication issue, server error, etc.

| Error Type | HTTP Status | Meaning | When Used | Client Action |
|-----------|-------------|---------|-----------|----------------|
| `VALIDATION_ERROR` | 400 | Request data validation failed | Invalid email, missing field, weak password | Show field errors to user |
| `AUTHENTICATION_ERROR` | 401 | Missing or invalid credentials | Wrong password, expired token, no token | Redirect to login |
| `AUTHORIZATION_ERROR` | 403 | Valid credentials but insufficient role | User accessing admin endpoint | Show "Access Denied" message |
| `NOT_FOUND` | 404 | Resource doesn't exist | User/product/order not found | Show "Not Found" message |
| `CONFLICT` | 409 | Resource already exists | Email already registered | Suggest different value |
| `PRECONDITION_FAILED` | 400 | Business logic validation failed | Password doesn't meet policy | Show specific requirements |
| `INTERNAL_ERROR` | 500 | Unexpected server error | Unhandled exception | Show generic error message |

### Client-Side Error Handling Example

```javascript
// Handle different error types
async function apiCall(endpoint, options) {
  try {
    const response = await fetch(endpoint, options);
    
    if (!response.ok) {
      const error = await response.json();
      
      switch (error.errorType) {
        case 'VALIDATION_ERROR':
          // Show field errors to user
          showValidationErrors(error.fieldErrors);
          break;
          
        case 'AUTHENTICATION_ERROR':
          // Redirect to login
          window.location.href = '/login';
          break;
          
        case 'AUTHORIZATION_ERROR':
          // Show permission denied message
          showMessage('You do not have permission to perform this action');
          break;
          
        case 'NOT_FOUND':
          // Show not found message
          showMessage(`Not found: ${error.message}`);
          break;
          
        case 'CONFLICT':
          // Suggest alternative (e.g., use different email)
          showMessage(`This already exists: ${error.message}`);
          break;
          
        case 'INTERNAL_ERROR':
          // Show generic error message
          showMessage('An unexpected error occurred. Please try again.');
          break;
      }
    }
  } catch (err) {
    console.error('Network error:', err);
  }
}
```

---

## Custom Exceptions

All business logic exceptions extend `ApplicationException` and carry error type and HTTP status information.

### Exception Hierarchy

```
Exception (Java)
  └─ RuntimeException
      └─ ApplicationException (base custom exception)
          ├─ EmailAlreadyExistsException (409 CONFLICT)
          ├─ UserNotFoundException (404 NOT_FOUND)
          ├─ PasswordPolicyException (400 PRECONDITION_FAILED)
          ├─ AuthenticationException (401 AUTHENTICATION_ERROR)
          ├─ InsufficientPermissionException (403 AUTHORIZATION_ERROR)
          └─ ResourceNotFoundException (404 NOT_FOUND)
```

### Exception Classes

#### 1. ApplicationException (Base)
```java
public class ApplicationException extends RuntimeException {
    private final ErrorType errorType;
    private final int httpStatus;
    
    public ApplicationException(String message, ErrorType errorType, int httpStatus)
    public ApplicationException(String message, ErrorType errorType)
    public ApplicationException(String message)
}
```

**Usage**:
```java
throw new ApplicationException(
    "Something went wrong",
    ErrorType.PRECONDITION_FAILED,
    400
);
```

#### 2. EmailAlreadyExistsException (409)
```java
public class EmailAlreadyExistsException extends ApplicationException {
    public EmailAlreadyExistsException(String email)
}
```

**Usage**:
```java
if (userRepository.existsByEmail(email)) {
    throw new EmailAlreadyExistsException(email);
    // Response: "Email 'user@example.com' is already registered"
}
```

#### 3. UserNotFoundException (404)
```java
public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(String email)
    public UserNotFoundException(String message, ErrorType errorType)
}
```

**Usage**:
```java
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UserNotFoundException(email));
    // Response: "User with email 'user@example.com' not found"
```

#### 4. PasswordPolicyException (400)
```java
public class PasswordPolicyException extends ApplicationException {
    public PasswordPolicyException(String message)
    public PasswordPolicyException(String message, String details)
}
```

**Usage**:
```java
try {
    passwordPolicyValidator.validate(password);
} catch (PasswordPolicyException e) {
    throw new PasswordPolicyException(
        "Password does not meet security requirements",
        e.getMessage()
    );
}
```

#### 5. AuthenticationException (401)
```java
public class AuthenticationException extends ApplicationException {
    public AuthenticationException(String message)
    public static AuthenticationException invalidCredentials()
    public static AuthenticationException tokenExpired()
    public static AuthenticationException invalidToken()
    public static AuthenticationException tokenRefreshFailed()
}
```

**Usage**:
```java
// Using static factory methods
throw AuthenticationException.invalidCredentials();
throw AuthenticationException.tokenExpired();
throw AuthenticationException.invalidToken();
throw AuthenticationException.tokenRefreshFailed();

// Using constructor
throw new AuthenticationException("Custom error message");
```

#### 6. InsufficientPermissionException (403)
```java
public class InsufficientPermissionException extends ApplicationException {
    public InsufficientPermissionException(String message)
    public static InsufficientPermissionException requiresRole(String role)
    public static InsufficientPermissionException accessDenied()
}
```

**Usage**:
```java
if (!user.hasRole("ADMIN")) {
    throw InsufficientPermissionException.requiresRole("ADMIN");
    // Response: "This operation requires ADMIN role"
}
```

#### 7. ResourceNotFoundException (404)
```java
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resourceName, String identifier)
    public ResourceNotFoundException(String message)
}
```

**Usage**:
```java
Product product = productRepository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    // Response: "Product with ID '123' not found"
```

---

## Request Validation

Request validation happens at two levels:

### 1. Field-Level Validation (DTOs)

Using Jakarta Bean Validation annotations on request DTOs:

**LoginRequest**:
```java
@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password must be provided")
    private String password;
}
```

**RegisterRequest**:
```java
@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String password;
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Pattern(regexp = "^[+]?[0-9]{7,20}$", message = "Phone number must be valid (7-20 digits, optional + prefix)")
    private String phoneNumber;
}
```

**Validation Annotations**:

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@NotBlank` | Field is required and not empty | `@NotBlank(message = "Email is required")` |
| `@Email` | Field is valid email format | `@Email(message = "Email must be valid")` |
| `@Size` | String/Collection size constraints | `@Size(min = 2, max = 50)` |
| `@Pattern` | Field matches regex pattern | `@Pattern(regexp = "^[0-9]+$")` |
| `@Min` / `@Max` | Numeric value range | `@Min(0) @Max(100)` |
| `@NotNull` | Field cannot be null | `@NotNull` |

### 2. Business Logic Validation (Services)

Custom validation in service layer for complex rules:

```java
@Service
public class AuthService {
    public AuthResponse register(RegisterRequest request) {
        // Field validation passed (by @Valid on controller parameter)
        
        // Business validation:
        
        // 1. Email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }
        
        // 2. Password policy
        try {
            passwordPolicyValidator.validate(request.getPassword());
        } catch (PasswordPolicyException e) {
            throw new PasswordPolicyException(...);
        }
        
        // 3. Non-empty names
        if (request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        
        // ... continue with registration
    }
}
```

---

## Validation Examples

### Successful Validation

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "MySecure@Password123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "+1234567890"
  }'
```

**Response (200 OK)**:
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe"
  },
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Validation Error - Missing Fields

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "status": 400,
  "errorType": "VALIDATION_ERROR",
  "message": "Request validation failed. Please check the errors below.",
  "fieldErrors": {
    "password": "Password is required",
    "firstName": "First name is required",
    "lastName": "Last name is required"
  },
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Validation Error - Invalid Email

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "password123"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "status": 400,
  "errorType": "VALIDATION_ERROR",
  "message": "Request validation failed. Please check the errors below.",
  "fieldErrors": {
    "email": "Email must be a valid email address"
  },
  "path": "/api/auth/login",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Validation Error - Invalid Phone Format

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "MySecure@Password123",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "invalid"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "status": 400,
  "errorType": "VALIDATION_ERROR",
  "message": "Request validation failed. Please check the errors below.",
  "fieldErrors": {
    "phoneNumber": "Phone number must be valid (7-20 digits, optional + prefix)"
  },
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Business Logic Error - Email Already Exists

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "existing@example.com",
    "password": "MySecure@Password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Response (409 Conflict)**:
```json
{
  "status": 409,
  "errorType": "CONFLICT",
  "message": "Email 'existing@example.com' is already registered",
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Authentication Error - Invalid Credentials

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "wrongpassword"
  }'
```

**Response (401 Unauthorized)**:
```json
{
  "status": 401,
  "errorType": "AUTHENTICATION_ERROR",
  "message": "Invalid email or password",
  "path": "/api/auth/login",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Business Logic Error - Password Policy Violation

**Request**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "weak",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

**Response (400 Bad Request)**:
```json
{
  "status": 400,
  "errorType": "PRECONDITION_FAILED",
  "message": "Password does not meet security requirements. Password must contain uppercase, lowercase, digit and special character (@$!%*?&)",
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

---

## Error Handling Flow

The error handling flow shows how exceptions are caught and converted to responses:

```
Request → Controller
  ↓
  @Valid annotation triggers validation
  ↓
  Field validation passes/fails?
  ├─ FAILS → MethodArgumentNotValidException
  │         ↓
  │         GlobalExceptionHandler.handleValidationExceptions()
  │         ↓
  │         Return 400 VALIDATION_ERROR with fieldErrors
  │
  └─ PASSES → Service method called
            ↓
            Business logic validation
            ↓
            Exception thrown?
            ├─ YES → Custom exception (e.g., EmailAlreadyExistsException)
            │        ↓
            │        GlobalExceptionHandler.handleApplicationException()
            │        ↓
            │        Return appropriate HTTP status with ErrorResponse
            │
            └─ NO → Operation succeeds
                    ↓
                    Return 200 OK with ApiResponse
```

### Handler Precedence

When an exception is thrown, handlers are checked in this order:

1. Specific exception handlers (EmailAlreadyExistsException, UserNotFoundException, etc.)
2. Generic ApplicationException handler
3. Spring framework exceptions (BadCredentialsException, UsernameNotFoundException, etc.)
4. MethodArgumentNotValidException (validation errors)
5. Generic Exception handler (fallback)

---

## Client-Side Integration

### JavaScript/Fetch Example

```javascript
async function register(formData) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData)
    });
    
    const data = await response.json();
    
    if (response.ok) {
      // Success case
      console.log('Registered:', data);
      // Store tokens, redirect to login, etc.
    } else {
      // Error case
      handleError(data);
    }
  } catch (err) {
    console.error('Network error:', err);
    showError('Network connection failed');
  }
}

function handleError(errorResponse) {
  switch (errorResponse.errorType) {
    case 'VALIDATION_ERROR':
      // Show field-level errors
      Object.entries(errorResponse.fieldErrors).forEach(([field, message]) => {
        showFieldError(field, message);
      });
      break;
      
    case 'CONFLICT':
      // Email already exists - suggest action
      showError(`${errorResponse.message}. Please use a different email or login.`);
      break;
      
    case 'AUTHENTICATION_ERROR':
      // Wrong password
      showError(errorResponse.message);
      break;
      
    case 'AUTHORIZATION_ERROR':
      // No permission
      showError(errorResponse.message);
      redirectToHome();
      break;
      
    case 'NOT_FOUND':
      // Resource not found
      showError(errorResponse.message);
      break;
      
    case 'PRECONDITION_FAILED':
      // Business logic error (e.g., password policy)
      showError(errorResponse.message);
      break;
      
    case 'INTERNAL_ERROR':
      // Server error
      showError('An unexpected error occurred. Please try again later.');
      break;
  }
}
```

### React Hook Example

```javascript
import { useState } from 'react';

function LoginForm() {
  const [errors, setErrors] = useState({});
  const [generalError, setGeneralError] = useState('');
  
  async function handleLogin(email, password) {
    setErrors({});
    setGeneralError('');
    
    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      
      const data = await response.json();
      
      if (response.ok) {
        // Success
        localStorage.setItem('token', data.data.token);
        window.location.href = '/dashboard';
      } else {
        // Error
        if (data.errorType === 'VALIDATION_ERROR') {
          setErrors(data.fieldErrors);
        } else {
          setGeneralError(data.message);
        }
      }
    } catch (err) {
      setGeneralError('Network error. Please try again.');
    }
  }
  
  return (
    <form onSubmit={(e) => {
      e.preventDefault();
      const email = e.target.email.value;
      const password = e.target.password.value;
      handleLogin(email, password);
    }}>
      {generalError && <div className="error">{generalError}</div>}
      
      <input name="email" placeholder="Email" />
      {errors.email && <span className="field-error">{errors.email}</span>}
      
      <input name="password" type="password" placeholder="Password" />
      {errors.password && <span className="field-error">{errors.password}</span>}
      
      <button type="submit">Login</button>
    </form>
  );
}
```

---

## Testing Error Scenarios

### Test Tools

Use curl, Postman, or REST Client to test error scenarios:

**VS Code REST Client** (`.http` file):
```http
### Test validation error
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "invalid-email",
  "password": "weak"
}

### Test conflict error
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "existing@example.com",
  "password": "MySecure@Password123",
  "firstName": "John",
  "lastName": "Doe"
}

### Test authentication error
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "nonexistent@example.com",
  "password": "anypassword"
}

### Test authorization error (requires valid token)
GET http://localhost:8080/api/admin/dashboard
Authorization: Bearer invalid_token
```

### Manual Testing Checklist

- [ ] Register with invalid email format → 400 VALIDATION_ERROR
- [ ] Register with short password → 400 VALIDATION_ERROR
- [ ] Register with missing fields → 400 VALIDATION_ERROR
- [ ] Register with existing email → 409 CONFLICT
- [ ] Register with weak password → 400 PRECONDITION_FAILED
- [ ] Login with wrong password → 401 AUTHENTICATION_ERROR
- [ ] Login with non-existent email → 401 AUTHENTICATION_ERROR
- [ ] Access protected endpoint without token → 401 AUTHENTICATION_ERROR
- [ ] Access protected endpoint with invalid token → 401 AUTHENTICATION_ERROR
- [ ] Access admin endpoint as USER → 403 AUTHORIZATION_ERROR
- [ ] Request non-existent endpoint → 404 NOT_FOUND
- [ ] Request non-existent resource (user ID) → 404 NOT_FOUND

---

## Summary

The comprehensive error handling and validation system provides:

✅ **Consistent Error Format** - All errors follow standard `ErrorResponse` structure
✅ **Semantic Error Types** - Clients can programmatically handle different error types
✅ **Field-Level Validation** - Detailed feedback on which fields are invalid
✅ **Business Logic Validation** - Complex rules enforced at service layer
✅ **Proper HTTP Status Codes** - Correct status for each error type
✅ **User-Friendly Messages** - No technical jargon or internal details
✅ **Comprehensive Logging** - All errors logged for debugging
✅ **Security** - No sensitive information leakage in error messages

**Last Updated**: January 2024
**Version**: 1.0
