# Security Components Inventory

This file provides a quick reference to all security components and their locations.

## Core Security Components

### 1. SecurityConfig.java
**Location**: `src/main/java/com/perfume/shop/security/SecurityConfig.java`
**Purpose**: Central Spring Security configuration with CORS, authorization, and exception handling
**Key Features**:
- Environment-based CORS configuration
- Fine-grained endpoint authorization (8 patterns)
- Custom exception handling (401/403)
- JWT authentication chain
- BCrypt password encoder with configurable strength

**Key Methods**:
- `filterChain()` - Configure security filter chain
- `authenticationProvider()` - Configure authentication provider
- `passwordEncoder()` - BCrypt encoder configuration
- `authenticationManager()` - Spring authentication manager

**Dependencies Injected**:
- `JwtAuthenticationFilter` - JWT validation filter
- `UserDetailsService` - User loading service

---

### 2. JwtService.java
**Location**: `src/main/java/com/perfume/shop/security/JwtService.java`
**Purpose**: JWT token generation, validation, and refresh with lifecycle management
**Key Features**:
- Access token generation (24 hours)
- Refresh token generation (7 days) with NEW type claim
- Token validation with grace period support
- Expiration checking with exception handling
- Token claim extraction and manipulation

**Key Methods**:
- `generateToken(UserDetails)` - Generate access token
- `generateRefreshToken(UserDetails)` - Generate refresh token (NEW)
- `isTokenValid(token, userDetails)` - Validate with grace period
- `isTokenExpired(token)` - Check expiration
- `isWithinGracePeriod(token)` - Check grace period (NEW)
- `getTimeUntilExpiration(token)` - Get remaining time (NEW)
- `isRefreshToken(token)` - Check token type (NEW)
- `extractUsername(token)` - Extract user email
- `extractClaim(token, claimsResolver)` - Extract any claim
- `extractExpiration(token)` - Extract expiration time

**Configuration Values**:
- `app.jwt.secret` - HMAC key for signing (environment variable)
- `app.jwt.expiration` - Access token TTL (86400000 ms = 24h)
- `app.jwt.refresh-expiration` - Refresh token TTL (604800000 ms = 7d)
- `app.jwt.grace-period` - Grace period (60000 ms = 1 min)

**Token Claims**:
```json
{
  "sub": "user@example.com",
  "type": "refresh",  // Only in refresh tokens
  "iat": 1704067200,
  "exp": 1704153600,
  "iss": "perfume-shop"
}
```

---

### 3. JwtAuthenticationFilter.java
**Location**: `src/main/java/com/perfume/shop/security/JwtAuthenticationFilter.java`
**Purpose**: Extract and validate JWT from HTTP requests, set SecurityContext authentication
**Key Features**:
- Bearer token extraction from Authorization header
- Token validation with error handling
- Grace period detection with X-Token-Expiring header response
- Clear error messages and logging
- Constants for header management

**Key Methods**:
- `doFilterInternal()` - Main filter logic
- `validateAndAuthenticateToken()` - Token validation and context setup (NEW)

**Constants**:
- `AUTHORIZATION_HEADER` = "Authorization"
- `BEARER_PREFIX` = "Bearer "
- `BEARER_TOKEN_START` = 7

**Error Response**:
```json
{
  "error": "Invalid or expired token"
}
```

**Grace Period Header**:
```
X-Token-Expiring: true
```
Client can use this to proactively refresh before next request fails.

**Dependencies Injected**:
- `JwtService` - Token validation
- `UserDetailsService` - User loading

---

### 4. PasswordPolicyValidator.java
**Location**: `src/main/java/com/perfume/shop/security/PasswordPolicyValidator.java`
**Purpose**: Enforce strong password requirements and reject weak patterns (NEW)
**Key Features**:
- Length validation (8-128 characters)
- Complexity requirements (uppercase, lowercase, digit, special char)
- Common password detection (password123, admin123, etc.)
- Sequential character detection (abc, 123, xyz patterns)
- Custom exception for policy violations

**Key Methods**:
- `validate(password)` - Main validation method
- `isComplexEnough()` - Check complexity
- `isCommonPassword()` - Check weak passwords
- `hasSequentialCharacters()` - Check sequential patterns

**Validation Rules**:
1. Length: 8-128 characters
2. Uppercase: At least one (A-Z)
3. Lowercase: At least one (a-z)
4. Digit: At least one (0-9)
5. Special: At least one (@$!%*?&)
6. Not common: Excludes password123, admin123, welcome, letmein, etc.
7. No sequential: Excludes abc, 123, xyz, qwerty, asdfgh, etc.

**Custom Exception**:
- `PasswordPolicyException` - Thrown when password violates policy

**Valid Examples**:
- `MySecure@Pass123`
- `Perfume!Shop#2024`
- `P@ssw0rd$Secure`

---

## Service Components

### AuthService.java
**Location**: `src/main/java/com/perfume/shop/service/AuthService.java`
**Purpose**: Handle authentication logic including registration, login, and token refresh
**Key Features**:
- User registration with password policy validation
- Credential authentication with error handling
- Token generation for both access and refresh
- NEW: Token refresh endpoint implementation
- Account status validation
- Input validation

**Key Methods**:
- `register(RegisterRequest)` - Create user account
- `login(LoginRequest)` - Authenticate user
- `refreshToken(String)` - Obtain new tokens (NEW)
- `loadUserByUsername(String)` - UserDetailsService implementation

**Registration Flow**:
1. Validate email uniqueness
2. Validate password with PasswordPolicyValidator
3. Validate firstName and lastName
4. Encode password with BCrypt
5. Create User entity
6. Create Cart for user
7. Generate tokens
8. Return AuthResponse

**Login Flow**:
1. Authenticate with AuthenticationManager
2. Load User details
3. Validate account is active
4. Generate tokens
5. Return AuthResponse

**Refresh Flow** (NEW):
1. Validate refresh token type
2. Extract username from token
3. Load User
4. Validate token still valid
5. Generate new tokens
6. Return AuthResponse

**Dependencies Injected**:
- `UserRepository` - User data access
- `CartRepository` - Cart creation
- `AuthenticationManager` - Credential authentication
- `JwtService` - Token generation
- `UserDetailsService` - User loading
- `PasswordPolicyValidator` - Password validation (NEW)

---

## DTO Components

### AuthResponse.java
**Location**: `src/main/java/com/perfume/shop/dto/AuthResponse.java`
**Purpose**: Return authentication response with tokens and user information
**Fields**:
- `token` (String) - Access token for API requests
- `refreshToken` (String) - Refresh token (NEW)
- `type` (String) - Token type (always "Bearer")
- `expiresIn` (Long) - Token expiration in seconds (NEW)
- `id` (Long) - User ID
- `email` (String) - User email
- `firstName` (String) - User first name
- `lastName` (String) - User last name
- `role` (String) - User role (USER/ADMIN)

**Example Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "expiresIn": 86400,
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

---

## Controller Components

### AuthController.java
**Location**: `src/main/java/com/perfume/shop/controller/AuthController.java`
**Purpose**: REST API endpoints for authentication
**Endpoints**:

| Method | Endpoint | Purpose | Security |
|--------|----------|---------|----------|
| POST | `/api/auth/register` | Create new account | Public |
| POST | `/api/auth/login` | Authenticate user | Public |
| POST | `/api/auth/refresh-token` | Refresh access token | Public (with valid refresh token) |

**Endpoint Details**:

1. **POST /api/auth/register**
   - Input: RegisterRequest (email, password, firstName, lastName)
   - Output: AuthResponse with tokens
   - Errors: 400 (validation), 409 (duplicate email)
   - Password validated against PasswordPolicyValidator

2. **POST /api/auth/login**
   - Input: LoginRequest (email, password)
   - Output: AuthResponse with tokens
   - Errors: 401 (invalid credentials)
   - Validates account is active

3. **POST /api/auth/refresh-token** (NEW)
   - Input: refreshToken query parameter
   - Output: AuthResponse with new tokens
   - Errors: 401 (invalid/expired token)
   - Validates refresh token type and signature

**Dependencies Injected**:
- `AuthService` - Authentication logic

---

## Configuration Components

### application.yml
**Location**: `src/main/resources/application.yml`
**Security Configuration**:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:default-secret-key}
    expiration: 86400000           # 24 hours (milliseconds)
    refresh-expiration: 604800000  # 7 days (milliseconds)
    grace-period: 60000            # 1 minute (milliseconds)
  
  security:
    cors-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
    cors-max-age: 3600             # 1 hour (seconds)
    password-encoder-strength: 12  # BCrypt strength (10-14)
```

**Environment Variables** (Production):
```bash
JWT_SECRET=your-256-bit-secret-key
CORS_ORIGINS=https://www.perfume-shop.com,https://app.perfume-shop.com
```

---

## Security Endpoints Authorization

### PUBLIC Endpoints (No Authentication)
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh-token
GET    /api/products/**
GET    /api/reviews
GET    /health
```

### PROTECTED Endpoints (Authentication Required)
```
GET    /api/users/profile
POST   /api/users/password/change
POST   /api/users/password/reset
GET    /api/cart
POST   /api/cart
PUT    /api/cart/items/{id}
DELETE /api/cart/items/{id}
GET    /api/orders
GET    /api/orders/{id}
POST   /api/checkout
```

### ADMIN Endpoints (ADMIN Role Required)
```
POST   /api/admin/products
PUT    /api/admin/products/{id}
DELETE /api/admin/products/{id}
GET    /api/admin/orders
GET    /api/admin/users
```

---

## Security Flow Diagrams

### Registration Flow
```
User (email, password, name)
  ↓
AuthController.register()
  ↓
AuthService.register()
  ├─ UserRepository.findByEmail() → Check uniqueness
  ├─ PasswordPolicyValidator.validate() → Check password strength
  ├─ User.setPassword(BCrypt.encode()) → Encode password
  ├─ UserRepository.save() → Save user
  ├─ CartRepository.save() → Create cart
  ├─ JwtService.generateToken() → Access token
  ├─ JwtService.generateRefreshToken() → Refresh token
  └─ Return AuthResponse
```

### Login Flow
```
User (email, password)
  ↓
AuthController.login()
  ↓
AuthService.login()
  ├─ AuthenticationManager.authenticate() → Verify credentials
  ├─ UserRepository.findByEmail() → Load user
  ├─ User.isActive() → Validate account
  ├─ JwtService.generateToken() → Access token
  ├─ JwtService.generateRefreshToken() → Refresh token
  └─ Return AuthResponse
```

### Token Refresh Flow
```
Client (refreshToken)
  ↓
AuthController.refreshToken()
  ↓
AuthService.refreshToken()
  ├─ JwtService.isRefreshToken() → Validate type
  ├─ JwtService.extractUsername() → Get user
  ├─ UserRepository.findByEmail() → Load user
  ├─ JwtService.isTokenValid() → Validate signature/expiry
  ├─ JwtService.generateToken() → New access token
  ├─ JwtService.generateRefreshToken() → New refresh token
  └─ Return AuthResponse
```

### API Request Flow
```
Client (Authorization: Bearer <token>)
  ↓
JwtAuthenticationFilter.doFilterInternal()
  ├─ Extract Authorization header
  ├─ Validate Bearer format
  ├─ JwtService.isTokenValid() → Validate signature/expiry
  ├─ UserDetailsService.loadUserByUsername() → Load user
  ├─ Set SecurityContext authentication
  └─ Request continues to endpoint
    ↓
    SecurityConfig authorization check
      ├─ Check endpoint pattern
      ├─ Check required roles
      └─ Allow or deny request
```

---

## Error Handling

### Status Codes

| Code | Error | When | Solution |
|------|-------|------|----------|
| 400 | Bad Request | Invalid input | Check request format |
| 401 | Unauthorized | Missing/invalid token | Login again |
| 403 | Forbidden | Valid token but insufficient role | Use appropriate role |
| 404 | Not Found | Endpoint doesn't exist | Check URL |
| 409 | Conflict | Email already exists | Use different email |
| 500 | Server Error | Backend error | Check server logs |

### Common Exceptions

| Exception | Cause | Location |
|-----------|-------|----------|
| `PasswordPolicyException` | Password violates policy | PasswordPolicyValidator |
| `InvalidJwtException` | Invalid token signature | JwtService |
| `ExpiredJwtException` | Token lifetime exceeded | JwtService |
| `AuthenticationException` | Invalid credentials | AuthService.login() |
| `UsernameNotFoundException` | Email not found | AuthService |

---

## Testing Checklist

### Functional Tests
- [ ] Register with strong password → Success
- [ ] Register with weak password → PasswordPolicyException
- [ ] Register with existing email → Conflict (409)
- [ ] Login with valid credentials → Success with tokens
- [ ] Login with invalid credentials → Unauthorized (401)
- [ ] Refresh token → New tokens generated
- [ ] Access protected endpoint with token → Success
- [ ] Access protected endpoint without token → Unauthorized (401)
- [ ] Expired token → Unauthorized (401)
- [ ] Refresh token within grace period → Accepted

### Security Tests
- [ ] CORS allows configured origin
- [ ] CORS blocks non-configured origin
- [ ] CORS preflight handled correctly
- [ ] Password BCrypt encoded (not plain text)
- [ ] Token signature verified
- [ ] Token expiration checked
- [ ] Admin endpoints require ADMIN role
- [ ] User endpoints require any authentication
- [ ] Public endpoints accessible without auth

### Performance Tests
- [ ] Registration <1s (excluding BCrypt)
- [ ] Login <500ms
- [ ] Token refresh <300ms
- [ ] Protected API call <100ms

---

## Production Checklist

### Security Configuration
- [ ] `JWT_SECRET` set to 256+ bit random value
- [ ] `CORS_ORIGINS` set to production domain only
- [ ] `password-encoder-strength` optimized (10-12)
- [ ] HTTPS enabled (not HTTP)
- [ ] TLS 1.2+ configured

### Deployment
- [ ] Environment variables set on server
- [ ] Secrets not in code/logs
- [ ] Error messages generic (no stack traces)
- [ ] Logging configured (DEBUG OFF in prod)
- [ ] Database backup configured

### Monitoring
- [ ] Failed login attempts logged
- [ ] Token errors logged
- [ ] API requests logged
- [ ] Exception handler working
- [ ] Alerts configured for security events

---

## Quick Reference

### Quick Commands

**Build**:
```bash
mvn clean compile
```

**Run**:
```bash
mvn spring-boot:run
```

**Test Registration**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Strong@Pass123","firstName":"John","lastName":"Doe"}'
```

**Test Login**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Strong@Pass123"}'
```

**Test Protected Endpoint**:
```bash
curl -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/cart
```

### Key Passwords for Testing

**Valid**:
- MySecure@Pass123
- Perfume!Shop#2024
- Test@Password123
- Secure!@#$%123

**Invalid**:
- password123 (missing uppercase, special char)
- Password123 (missing special char)
- Pass@123 (too short)
- PASSWORD@123 (missing lowercase)

---

**Last Updated**: January 2024
**Version**: 1.0
**Status**: Complete & Production Ready
