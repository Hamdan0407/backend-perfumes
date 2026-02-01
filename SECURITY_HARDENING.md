# Spring Security Hardening Guide

This document outlines all security enhancements implemented in the Perfume Shop application, including CORS configuration, endpoint authorization, password policies, and token management.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Password Policies](#password-policies)
4. [Token Management](#token-management)
5. [CORS Configuration](#cors-configuration)
6. [Endpoint Security](#endpoint-security)
7. [Frontend Integration](#frontend-integration)
8. [Security Best Practices](#security-best-practices)
9. [Configuration Reference](#configuration-reference)
10. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Security Stack

- **Framework**: Spring Security 6.x with Spring Boot 3.2.1
- **Authentication**: Stateless JWT (JSON Web Tokens) using JJWT library
- **Password Encoding**: BCrypt with configurable strength (default: 12)
- **Token Strategy**: Access token (24h) + Refresh token (7d) with grace period
- **CORS Policy**: Environment-configurable, restrictive origin whitelist
- **Authorization**: Role-based access control (RBAC) + fine-grained endpoint patterns

### Key Components

| Component | Purpose | Location |
|-----------|---------|----------|
| `SecurityConfig` | Central Spring Security configuration | `security/SecurityConfig.java` |
| `JwtService` | JWT generation, validation, and refresh | `security/JwtService.java` |
| `JwtAuthenticationFilter` | Extract and validate JWT from requests | `security/JwtAuthenticationFilter.java` |
| `PasswordPolicyValidator` | Enforce strong password requirements | `security/PasswordPolicyValidator.java` |
| `AuthService` | Register, login, and token refresh logic | `service/AuthService.java` |
| `AuthController` | REST endpoints for auth operations | `controller/AuthController.java` |

---

## Authentication & Authorization

### Authentication Flow

**Login:**
```
1. Client sends POST /api/auth/login with email & password
2. AuthService.login() authenticates credentials via AuthenticationManager
3. On success: Generate access token (24h) + refresh token (7d)
4. Return AuthResponse with tokens and user information
5. Client stores tokens (access token in memory, refresh token in secure storage)
```

**Registration:**
```
1. Client sends POST /api/auth/register with email, password, firstName, lastName
2. AuthService.register() validates password against policy
3. Check for duplicate email
4. Create user with BCrypt-encoded password
5. Generate access token + refresh token
6. Return AuthResponse
```

**Token Refresh:**
```
1. Client sends POST /api/auth/refresh-token?refreshToken=<token>
2. JwtService validates refresh token (type, signature, expiration)
3. AuthService.refreshToken() generates new access + refresh tokens
4. Return AuthResponse with new tokens
5. Client updates stored tokens
```

### Authorization Model

#### Role-Based Access Control (RBAC)

Two roles defined in the system:

- **USER**: Default role for registered customers
- **ADMIN**: Administrative access to management endpoints

#### Endpoint Authorization Matrix

| Endpoint Pattern | HTTP Method | Required Auth | Required Role | Notes |
|------------------|-------------|---------------|---------------|-------|
| `/api/auth/register` | POST | ❌ None | — | Public registration |
| `/api/auth/login` | POST | ❌ None | — | Public login |
| `/api/auth/refresh-token` | POST | ❌ None | — | Requires valid refresh token |
| `/api/products/**` | GET | ❌ None | — | Product browsing (public) |
| `/api/products/**` | POST/PUT/DELETE | ✅ Yes | ADMIN | Product management |
| `/api/admin/**` | ALL | ✅ Yes | ADMIN | Admin dashboard & controls |
| `/api/cart/**` | ALL | ✅ Yes | USER | Shopping cart (authenticated) |
| `/api/orders/**` | ALL | ✅ Yes | USER | Order history (authenticated) |
| `/api/checkout/**` | ALL | ✅ Yes | USER | Checkout process (authenticated) |
| `/api/users/profile` | GET | ✅ Yes | USER | User profile (authenticated) |
| `/api/users/password/**` | POST/PUT | ✅ Yes | USER | Password change (authenticated) |
| `/api/reviews` | GET | ❌ None | — | Public review reading |
| `/api/reviews` | POST/PUT/DELETE | ✅ Yes | USER | Review creation/modification |
| `/health` | GET | ❌ None | — | Health check endpoint |
| All other endpoints | ALL | ✅ Yes | USER | Default: authenticated |

---

## Password Policies

### Requirements

All user passwords must comply with the following policy:

#### Length
- **Minimum**: 8 characters
- **Maximum**: 128 characters

#### Complexity
- **Uppercase Letters**: At least one (A-Z)
- **Lowercase Letters**: At least one (a-z)
- **Digits**: At least one (0-9)
- **Special Characters**: At least one (@$!%*?&)

#### Example Valid Passwords
✅ `MySecure@Pass123`
✅ `Perfume!Shop#2024`
✅ `P@ssw0rd$Secure`

#### Example Invalid Passwords
❌ `password123` — Missing uppercase, special char
❌ `Password123` — Missing special character
❌ `Pass@123` — Too short (7 chars)
❌ `PASSWORD@123` — Missing lowercase

### Weak Password Checks

Passwords are rejected if they match common patterns:

**Common Password Detection:**
- Variations of "password123", "admin123", "qwerty", "letmein", "welcome", etc.

**Sequential Character Detection:**
- Rejects patterns like "abc123", "xyz789", "123456"
- Prevents keyboards patterns like "qwerty" or "asdfgh"

### Implementation

Password validation occurs in:
1. **Registration** (`AuthService.register()`) — Before account creation
2. **PasswordPolicyValidator** — Reusable validation component

```java
// In registration flow:
authService.register(registerRequest);
// Internally calls:
passwordPolicyValidator.validate(password); // Throws PasswordPolicyException if invalid
```

---

## Token Management

### Access Tokens

**Purpose**: Authenticate API requests
**Expiration**: 24 hours (86400000 milliseconds)
**Storage**: Client-side (recommended: memory/session storage)
**Usage**: Include in Authorization header: `Authorization: Bearer <access_token>`

**Claims**:
```json
{
  "sub": "user@example.com",
  "iat": 1234567890,
  "exp": 1234654290,
  "iss": "perfume-shop"
}
```

### Refresh Tokens

**Purpose**: Obtain new access tokens without re-authenticating
**Expiration**: 7 days (604800000 milliseconds)
**Storage**: Client-side (recommended: secure http-only cookie or localStorage)
**Usage**: Include in request: `POST /api/auth/refresh-token?refreshToken=<refresh_token>`

**Claims**:
```json
{
  "sub": "user@example.com",
  "type": "refresh",
  "iat": 1234567890,
  "exp": 1235172690,
  "iss": "perfume-shop"
}
```

### Grace Period

**What is it?** A 1-minute window allowing token refresh even after expiration
**Purpose**: Handle network latency and clock skew between client/server
**Implementation**: JwtService checks token expiration with grace period

**Behavior**:
```
Access Token Expires at: 2024-01-01 10:00:00
Grace Period Ends at:    2024-01-01 10:01:00

✅ 10:00:30 — Token is expired but within grace period → Can refresh
❌ 10:02:00 — Token is expired and past grace period → Must login again
```

### Token Response

After successful login/register/refresh, the response includes:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "expiresIn": 86400,
  "id": 1,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER"
}
```

**Field Descriptions**:
- `token`: Access token for API requests
- `refreshToken`: Token for obtaining new access tokens
- `type`: Always "Bearer" (for Authorization header format)
- `expiresIn`: Access token lifetime in seconds
- Other fields: User information for UI display

### Handling Token Expiration (Client-Side)

**When Access Token Expires:**

```javascript
// Scenario 1: Token expires during API request
GET /api/orders → 401 Unauthorized

// Client Response:
1. Detect 401 response
2. Extract refresh token from storage
3. POST /api/auth/refresh-token?refreshToken=<token>
4. On success: Update stored tokens
5. Retry original request with new token
6. On failure: Redirect to login
```

**X-Token-Expiring Header:**

If an access token is within the grace period, the server returns:
```
X-Token-Expiring: true
```

This allows the client to proactively refresh before the request fails.

---

## CORS Configuration

### What is CORS?

Cross-Origin Resource Sharing (CORS) controls which domains can access your API. Without proper CORS:
- Browsers block requests from frontend on different domain
- API remains inaccessible to legitimate frontend applications

### Configuration

CORS settings are environment-based:

**application.yml:**
```yaml
app:
  security:
    cors-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
    cors-max-age: 3600 # CORS preflight cache (1 hour)
```

**Environment Variables (Production):**
```bash
# .env or server environment
CORS_ORIGINS=https://www.perfume-shop.com,https://app.perfume-shop.com
```

### CORS Policy Details

| Setting | Value | Reason |
|---------|-------|--------|
| **Allowed Origins** | Configurable | Only specified frontend domains |
| **Allowed Methods** | GET, POST, PUT, DELETE, PATCH, OPTIONS | All standard REST methods |
| **Allowed Headers** | Authorization, Content-Type, Accept, X-Requested-With | Required for auth + content negotiation |
| **Allow Credentials** | true | Allows cookies/auth headers |
| **Max Age** | 3600 seconds | Cache preflight 1 hour to reduce requests |

### CORS Preflight

Browser automatically sends OPTIONS request before PUT/DELETE:
```
OPTIONS /api/products/123
Access-Control-Request-Method: PUT
Access-Control-Request-Headers: Authorization

← Server Response:
Access-Control-Allow-Origin: http://localhost:3000
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, Accept, X-Requested-With
Access-Control-Max-Age: 3600
```

### CORS Errors (Client-Side)

If you see browser error:
```
Access to XMLHttpRequest from origin 'http://localhost:3000' has been blocked by CORS policy
```

**Solutions**:
1. Verify frontend domain is in `app.security.cors-origins`
2. Ensure `Authorization` header is in allowed headers
3. Check `Content-Type` is `application/json`
4. Restart backend after updating environment variables

---

## Endpoint Security

### Public Endpoints (No Authentication)

```
GET    /api/products              # List all products
GET    /api/products/:id          # Get product details
GET    /api/reviews               # List reviews (read-only)
POST   /api/auth/register         # Create new account
POST   /api/auth/login            # Authenticate user
POST   /api/auth/refresh-token    # Refresh access token
GET    /health                    # Health check
```

### Protected Endpoints (Authentication Required)

```
# User Profile
GET    /api/users/profile
POST   /api/users/password/change
POST   /api/users/password/reset

# Shopping Cart
GET    /api/cart
POST   /api/cart
PUT    /api/cart/items/:id
DELETE /api/cart/items/:id

# Orders
GET    /api/orders
GET    /api/orders/:id
POST   /api/checkout
POST   /api/orders/:id/cancel

# Reviews (authenticated write, public read)
POST   /api/reviews               # Create review (auth required)
PUT    /api/reviews/:id           # Update review (auth required)
DELETE /api/reviews/:id           # Delete review (auth required)
```

### Admin-Only Endpoints

```
# Product Management
POST   /api/admin/products
PUT    /api/admin/products/:id
DELETE /api/admin/products/:id

# Order Management
GET    /api/admin/orders
GET    /api/admin/orders/:id
POST   /api/admin/orders/:id/ship
POST   /api/admin/orders/:id/cancel

# User Management (if implemented)
GET    /api/admin/users
GET    /api/admin/users/:id
POST   /api/admin/users/:id/disable
```

### Request with Authentication

```bash
# Include Authorization header with Bearer token
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
     https://api.perfume-shop.com/api/cart
```

### Error Responses

| Status | Error | Cause | Solution |
|--------|-------|-------|----------|
| 401 | Unauthorized | Missing/invalid token | Login again |
| 401 | Token expired | Access token lifetime exceeded | Use refresh token |
| 403 | Forbidden | Valid token but insufficient role | Use admin account |
| 404 | Not Found | Endpoint doesn't exist | Check URL |
| 500 | Server Error | Backend error | Check server logs |

---

## Frontend Integration

### Token Storage Strategy

```javascript
// Option 1: Memory + localStorage (Recommended for security)
const tokens = {
  accessToken: null,    // Keep in memory only (cleared on refresh)
  refreshToken: null    // Keep in localStorage (persists across page reloads)
};

// Option 2: localStorage only (Convenient but less secure)
localStorage.setItem('accessToken', token);

// Option 3: http-only cookies (Most secure, requires backend support)
// Backend sets: Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict
```

### Login Flow Implementation

```javascript
async function login(email, password) {
  try {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });
    
    if (!response.ok) {
      throw new Error('Login failed');
    }
    
    const authResponse = await response.json();
    
    // Store tokens
    sessionStorage.setItem('accessToken', authResponse.token);
    localStorage.setItem('refreshToken', authResponse.refreshToken);
    localStorage.setItem('expiresIn', authResponse.expiresIn);
    
    // Schedule token refresh before expiration
    scheduleTokenRefresh(authResponse.expiresIn);
    
    return authResponse;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
}
```

### API Request with Authorization

```javascript
async function apiRequest(endpoint, options = {}) {
  const accessToken = sessionStorage.getItem('accessToken');
  
  if (!accessToken) {
    // Try to refresh
    const refreshed = await refreshTokenIfNeeded();
    if (!refreshed) {
      redirectToLogin();
      return;
    }
  }
  
  const response = await fetch(
    `http://localhost:8080${endpoint}`,
    {
      ...options,
      headers: {
        ...options.headers,
        'Authorization': `Bearer ${accessToken}`,
        'Content-Type': 'application/json'
      }
    }
  );
  
  // Check for token expiration
  if (response.status === 401) {
    const refreshed = await refreshTokenIfNeeded();
    if (refreshed) {
      // Retry request with new token
      return apiRequest(endpoint, options);
    } else {
      redirectToLogin();
    }
  }
  
  return response;
}
```

### Token Refresh Implementation

```javascript
async function refreshTokenIfNeeded() {
  const refreshToken = localStorage.getItem('refreshToken');
  const expiresIn = parseInt(localStorage.getItem('expiresIn'));
  
  if (!refreshToken) {
    return false;
  }
  
  // Check if token expires in next 5 minutes
  if (Date.now() < (expiresIn * 1000 - 5 * 60 * 1000)) {
    return true; // Token still valid
  }
  
  try {
    const response = await fetch(
      'http://localhost:8080/api/auth/refresh-token',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      }
    );
    
    if (!response.ok) {
      // Refresh token invalid or expired
      clearTokens();
      return false;
    }
    
    const authResponse = await response.json();
    sessionStorage.setItem('accessToken', authResponse.token);
    localStorage.setItem('refreshToken', authResponse.refreshToken);
    
    return true;
  } catch (error) {
    console.error('Token refresh error:', error);
    return false;
  }
}
```

### React Example with Axios

```javascript
// src/api/axios.js
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

// Request interceptor: Add Authorization header
api.interceptors.request.use(
  config => {
    const accessToken = sessionStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

// Response interceptor: Handle token expiration
api.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(
          'http://localhost:8080/api/auth/refresh-token',
          { refreshToken }
        );
        
        sessionStorage.setItem('accessToken', response.data.token);
        localStorage.setItem('refreshToken', response.data.refreshToken);
        
        // Retry original request
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed, redirect to login
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;
```

---

## Security Best Practices

### 1. Password Management

**For Users:**
- ✅ Use strong, unique passwords (8+ characters with complexity)
- ❌ Never share passwords
- ❌ Don't reuse passwords across sites
- ✅ Consider password managers (1Password, LastPass, Bitwarden)

**For Developers:**
- ✅ Passwords are BCrypt-encoded (salted, irreversible)
- ✅ Password policy enforced server-side
- ❌ Never log passwords
- ❌ Never transmit passwords over HTTP (always use HTTPS)

### 2. Token Security

**For Users:**
- ✅ Keep tokens secret (never share in chats/emails)
- ✅ Logout on shared computers
- ✅ Clear browser cache/cookies when needed

**For Developers:**
- ✅ Use HTTPS in production (not HTTP)
- ✅ Store refresh tokens securely (http-only cookies preferred)
- ✅ Keep access tokens short-lived (24 hours)
- ✅ Implement token rotation for refresh tokens
- ❌ Never expose tokens in URL parameters
- ❌ Don't store sensitive data in JWT claims (they're encoded, not encrypted)

### 3. CORS Security

**Configuration:**
- ✅ Whitelist specific trusted origins only
- ❌ Never use `*` (allow all) in production
- ✅ Use HTTPS URLs in production
- ✅ Review CORS origins quarterly

### 4. API Security

**Implementation:**
- ✅ Always validate input on server-side
- ✅ Return generic error messages (don't reveal internals)
- ✅ Use proper HTTP status codes
- ✅ Implement rate limiting (IP-based, per-user)
- ✅ Log security events (failed logins, permission denied)
- ❌ Never trust client-side validation alone

### 5. HTTPS/TLS

**Production Requirements:**
- ✅ Use HTTPS for all API endpoints
- ✅ Use valid SSL/TLS certificate
- ✅ Enable HSTS header (Strict-Transport-Security)
- ✅ Use TLS 1.2 or higher
- ❌ Never send credentials over HTTP

### 6. Secret Management

**Environment Variables:**
```bash
# Production .env (never commit to git)
JWT_SECRET=your-256-bit-secret-key-here
CORS_ORIGINS=https://www.perfume-shop.com
MAIL_PASSWORD=your-app-password
STRIPE_API_KEY=sk_live_...
```

**Git Security:**
- ✅ Add `.env` to `.gitignore`
- ✅ Use separate secrets for dev/prod
- ✅ Rotate secrets regularly
- ❌ Never commit secrets to git

### 7. Account Security

**For Users:**
- ✅ Use strong, unique passwords
- ✅ Enable 2FA if available (future enhancement)
- ✅ Verify email address after registration
- ✅ Use recovery codes safely

**For Developers:**
- ✅ Validate email addresses (optional: send verification email)
- ✅ Implement account lockout after failed attempts (future)
- ✅ Log suspicious activities
- ✅ Provide password reset functionality

---

## Configuration Reference

### application.yml

```yaml
app:
  jwt:
    secret: ${JWT_SECRET:your-256-bit-secret-key}
    expiration: 86400000          # 24 hours (milliseconds)
    refresh-expiration: 604800000 # 7 days (milliseconds)
    grace-period: 60000           # 1 minute (milliseconds)
  
  security:
    cors-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
    cors-max-age: 3600            # CORS preflight cache (seconds)
    password-encoder-strength: 12 # BCrypt strength (10-12 recommended)
```

### Environment Variables (Production)

```bash
# JWT Configuration
JWT_SECRET=your-very-long-secret-key-at-least-256-bits

# CORS Configuration
CORS_ORIGINS=https://www.perfume-shop.com,https://app.perfume-shop.com

# Email Configuration
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# Payment Gateways
STRIPE_API_KEY=sk_live_your_stripe_key
RAZORPAY_KEY_ID=rzp_live_your_key_id

# Frontend URL
FRONTEND_URL=https://www.perfume-shop.com
```

### SecurityConfig Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `app.security.cors-origins` | String | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed origins |
| `app.security.cors-max-age` | Integer | `3600` | CORS preflight cache in seconds |
| `app.security.password-encoder-strength` | Integer | `12` | BCrypt strength (10-14 valid range) |
| `app.jwt.expiration` | Long | `86400000` | Access token TTL in milliseconds |
| `app.jwt.refresh-expiration` | Long | `604800000` | Refresh token TTL in milliseconds |
| `app.jwt.grace-period` | Long | `60000` | Grace period in milliseconds |

---

## Troubleshooting

### Common Issues

#### 1. "Invalid password format" on Registration

**Problem**: User gets error even though password seems strong

**Causes**:
- Missing uppercase letter (A-Z)
- Missing lowercase letter (a-z)
- Missing digit (0-9)
- Missing special character (@$!%*?&)
- Less than 8 characters

**Solution**: Use password like `MyPassword@123`

---

#### 2. "401 Unauthorized" on Protected Endpoints

**Problem**: API returns 401 even with valid login

**Causes**:
- Missing Authorization header
- Incorrect Bearer prefix
- Token expired
- Token invalid/tampered

**Solutions**:
```javascript
// Check token is present
const token = sessionStorage.getItem('accessToken');
console.log('Token:', token);

// Correct header format
headers: {
  'Authorization': 'Bearer ' + token  // Note the space after "Bearer"
}

// Re-login if token expired
```

---

#### 3. "CORS Error" in Browser

**Problem**: Browser blocks request with CORS error

**Causes**:
- Frontend URL not in `CORS_ORIGINS`
- Content-Type not in allowed headers
- Using wrong HTTP method

**Solutions**:
1. Check environment variable:
```bash
echo $CORS_ORIGINS
# Should include your frontend domain
```

2. Update for development:
```yaml
app:
  security:
    cors-origins: http://localhost:3000,http://localhost:5173
```

3. Restart backend after configuration change

---

#### 4. "Token Expired" Error

**Problem**: API returns token expired, but token seems recent

**Causes**:
- Token is actually expired (24 hours old)
- Server/client clock skew
- Grace period expired (token >24h 1min old)

**Solutions**:
```javascript
// Use refresh token to get new token
const newTokens = await refreshTokenIfNeeded();

// Or re-login
await login(email, password);
```

---

#### 5. "Invalid refresh token" on Refresh

**Problem**: Refresh token is rejected

**Causes**:
- Refresh token expired (>7 days old)
- Refresh token was tampered/modified
- Refresh token is actually an access token (wrong type)
- Signature mismatch

**Solutions**:
```javascript
// User must login again
window.location.href = '/login';

// Clear old tokens
sessionStorage.clear();
localStorage.clear();
```

---

#### 6. BCrypt Encoding Takes Too Long

**Problem**: Password encoding causes slow registration (>1 second)

**Causes**:
- BCrypt strength too high (>12)
- Server running on slow hardware

**Solutions**:
```yaml
app:
  security:
    password-encoder-strength: 10  # Reduce from 12
```

**Strength Guide**:
- 10: ~10ms (fast, acceptable security)
- 12: ~100ms (balanced, recommended)
- 14: ~1s (secure, slow)

---

### Debugging

#### Enable Security Logging

In `application.yml`:
```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.web.cors: DEBUG
    com.perfume: DEBUG
```

#### Check Token Contents

```javascript
// Decode JWT (client-side - for debugging only!)
function decodeToken(token) {
  const parts = token.split('.');
  const payload = JSON.parse(atob(parts[1]));
  console.log('Token Payload:', payload);
  console.log('Expires At:', new Date(payload.exp * 1000));
}

decodeToken(sessionStorage.getItem('accessToken'));
```

#### Verify CORS Configuration

```bash
# Test CORS preflight
curl -i -X OPTIONS http://localhost:8080/api/cart \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET"

# Should include:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
```

---

## Summary

The Perfume Shop backend now implements comprehensive Spring Security hardening with:

✅ **Password Policies**: 8+ characters with complexity requirements
✅ **Token Management**: 24-hour access tokens + 7-day refresh tokens with grace periods
✅ **CORS Configuration**: Environment-based, restrictive origin whitelist
✅ **Endpoint Authorization**: Fine-grained role-based access control
✅ **Error Handling**: Proper HTTP status codes and error messages
✅ **Production Ready**: Uses BCrypt encoding, stateless architecture, HTTPS-ready

For any security questions or concerns, consult this guide or review the implementation in the code.

**Last Updated**: January 2024
**Security Version**: 1.0
