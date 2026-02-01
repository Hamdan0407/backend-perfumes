# Exception Handling & Validation - Quick Reference

## Exception Classes Quick Reference

| Exception Class | HTTP Status | Error Type | When to Use |
|---|---|---|---|
| `EmailAlreadyExistsException` | 409 | CONFLICT | Email already registered |
| `UserNotFoundException` | 404 | NOT_FOUND | User doesn't exist |
| `PasswordPolicyException` | 400 | PRECONDITION_FAILED | Password violates policy |
| `AuthenticationException` | 401 | AUTHENTICATION_ERROR | Invalid credentials or token |
| `InsufficientPermissionException` | 403 | AUTHORIZATION_ERROR | User lacks required role |
| `ResourceNotFoundException` | 404 | NOT_FOUND | Generic resource not found |
| `ApplicationException` | 400-500 | Various | Base exception class |

## Throwing Exceptions in Service

```java
// Email already exists
if (userRepository.existsByEmail(email)) {
    throw new EmailAlreadyExistsException(email);
}

// User not found
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new UserNotFoundException(email));

// Password policy violation
throw new PasswordPolicyException("Invalid password", details);

// Authentication failures
throw AuthenticationException.invalidCredentials();
throw AuthenticationException.tokenExpired();
throw AuthenticationException.invalidToken();
throw AuthenticationException.tokenRefreshFailed();

// Authorization failures
throw InsufficientPermissionException.requiresRole("ADMIN");
throw InsufficientPermissionException.accessDenied();

// Generic resource not found
throw new ResourceNotFoundException("Product", productId);
```

## Validation Annotations

```java
// Required, non-empty string
@NotBlank(message = "Email is required")

// Valid email format
@Email(message = "Email must be valid")

// String length constraints
@Size(min = 2, max = 50, message = "Name must be 2-50 chars")

// Regex pattern matching
@Pattern(regexp = "^[0-9]{7,20}$", message = "Invalid phone")

// Numeric range
@Min(0) @Max(100)

// Field must not be null
@NotNull

// Collection/array not empty
@NotEmpty

// String not null (allows empty)
@NotBlank
```

## Common HTTP Responses

### Success (200 OK)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { "token": "..." },
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Validation Error (400)
```json
{
  "status": 400,
  "errorType": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": {
    "email": "Email is required",
    "password": "Password must be 8+ characters"
  },
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Authentication Error (401)
```json
{
  "status": 401,
  "errorType": "AUTHENTICATION_ERROR",
  "message": "Invalid email or password",
  "path": "/api/auth/login",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Conflict Error (409)
```json
{
  "status": 409,
  "errorType": "CONFLICT",
  "message": "Email 'user@example.com' is already registered",
  "path": "/api/auth/register",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

### Not Found Error (404)
```json
{
  "status": 404,
  "errorType": "NOT_FOUND",
  "message": "User with email 'user@example.com' not found",
  "path": "/api/users/lookup",
  "timestamp": "2024-01-19T10:30:45.123456"
}
```

## Validation Constraints Cheat Sheet

| Constraint | Regex | Example |
|---|---|---|
| Email | `^[a-zA-Z0-9._%-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$` | `user@example.com` |
| Phone (7-20 digits) | `^[+]?[0-9]{7,20}$` | `+1234567890` |
| Alphanumeric | `^[a-zA-Z0-9]+$` | `User123` |
| No special chars | `^[a-zA-Z0-9\s]+$` | `User Name` |
| Strong password | See PasswordPolicyValidator | `Pass@123!` |

## Controller Example

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Validation annotations (@Valid) trigger automatic validation
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        // If validation fails: GlobalExceptionHandler.handleValidationExceptions()
        // If succeeds: authService.register() is called
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", response));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}
```

## Frontend Integration Pattern

```javascript
async function apiRequest(endpoint, method = 'GET', data = null) {
  try {
    const response = await fetch(endpoint, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: data ? JSON.stringify(data) : null
    });
    
    const result = await response.json();
    
    if (response.ok) {
      return { success: true, data: result.data };
    } else {
      return handleError(result);
    }
  } catch (err) {
    return { success: false, error: 'Network error' };
  }
}

function handleError(errorResponse) {
  const handlers = {
    'VALIDATION_ERROR': (err) => ({
      success: false,
      fieldErrors: err.fieldErrors
    }),
    'AUTHENTICATION_ERROR': (err) => ({
      success: false,
      message: err.message,
      action: 'redirect_to_login'
    }),
    'AUTHORIZATION_ERROR': (err) => ({
      success: false,
      message: err.message,
      action: 'show_access_denied'
    }),
    'CONFLICT': (err) => ({
      success: false,
      message: err.message,
      action: 'suggest_alternative'
    }),
    'NOT_FOUND': (err) => ({
      success: false,
      message: err.message,
      action: 'show_not_found'
    })
  };
  
  const handler = handlers[errorResponse.errorType];
  return handler ? handler(errorResponse) : {
    success: false,
    message: 'An error occurred'
  };
}
```

## Testing Commands

```bash
# Validation error
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"invalid"}'

# Conflict error
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"existing@example.com","password":"Pass@123",...}'

# Authentication error
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"wrong"}'

# Not found error
curl http://localhost:8080/api/users/999

# Success
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"correct"}'
```

## Error Handling Checklist

- ✅ All custom exceptions extend `ApplicationException`
- ✅ Each exception type has appropriate HTTP status code
- ✅ Error messages are user-friendly (no technical details)
- ✅ All exceptions are caught by `GlobalExceptionHandler`
- ✅ Validation errors include field-level details
- ✅ Errors logged appropriately (warn for expected, error for unexpected)
- ✅ No sensitive information in error messages
- ✅ Consistent error response format across all endpoints
- ✅ Proper error types for client-side handling

## File Locations

| File | Purpose |
|------|---------|
| `exception/ErrorResponse.java` | Error response DTO |
| `exception/ErrorType.java` | Error type enumeration |
| `exception/ApplicationException.java` | Base exception class |
| `exception/AuthenticationException.java` | Auth failures (401) |
| `exception/EmailAlreadyExistsException.java` | Duplicate email (409) |
| `exception/UserNotFoundException.java` | User not found (404) |
| `exception/PasswordPolicyException.java` | Password validation (400) |
| `exception/InsufficientPermissionException.java` | No permission (403) |
| `exception/ResourceNotFoundException.java` | Generic 404 |
| `exception/GlobalExceptionHandler.java` | Central exception handler |
| `dto/ApiResponse.java` | Success response DTO |
| `dto/LoginRequest.java` | Login validation |
| `dto/RegisterRequest.java` | Registration validation |

---

**Version**: 1.0 | **Date**: January 2024
