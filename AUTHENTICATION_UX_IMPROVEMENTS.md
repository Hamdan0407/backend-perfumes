# Authentication UX Improvements - Complete Guide

## Overview

The authentication system has been improved to follow industry best practices with proper signup/login separation, session persistence, and token refresh handling.

## Key Changes

### 1. **Signup Flow (No Auto-Login)**

**Before:**
- User signs up → auto-logged in → sent to home page
- Tokens stored but user experience was unclear

**After:**
- User signs up → success message → redirected to login page
- User must explicitly login with credentials
- Clear and intentional authentication flow

**Files Modified:**
- [Register.jsx](frontend/src/pages/Register.jsx)

**Changes:**
```javascript
// Removed useAuthStore import (no auto-login)
// Removed login() function call

// Instead, redirect to login with clear messaging:
toast.success('Account created successfully! Please login with your credentials.');

// Clear any stale tokens from previous session
localStorage.removeItem('accessToken');
localStorage.removeItem('token');
localStorage.removeItem('refreshToken');
localStorage.removeItem('user');
localStorage.removeItem('tokenExpiresAt');
localStorage.removeItem('auth-storage');

// Redirect with delay for UX
setTimeout(() => {
  navigate('/login', { replace: true });
}, 1500);
```

### 2. **Session Persistence (Automatic)**

**New Feature:**
- Sessions automatically restored from localStorage on page reload
- Token expiration checked and validated
- Corrupted session data automatically cleaned up

**Files Enhanced:**
- [authStore.js](frontend/src/store/authStore.js)
- [App.jsx](frontend/src/App.jsx)

**Auth Store Methods Added:**
```javascript
// Initialize session from localStorage on app load
initializeSession: () => {
  // Restores user, tokens, and isAuthenticated status
  // Validates token expiration
  // Sets sessionInitialized flag
}

// Checks token expiration with 1-minute buffer
isTokenExpired: () => {
  const timeUntilExpiry = tokenExpiresAt - Date.now();
  return timeUntilExpiry < 60 * 1000; // 1 minute buffer
}

// Gets current token safely
getAccessToken: () => {
  return isAuthenticated && !isTokenExpired() ? accessToken : null;
}
```

### 3. **Token Refresh Handling**

**Mechanism:**
- Access tokens expire in 24 hours
- Refresh tokens expire in 7 days
- Axios interceptor automatically refreshes tokens before expiry
- 1-minute buffer ensures refresh happens proactively

**Token Refresh Flow:**
1. Request fails with 401 Unauthorized
2. If token refresh already in progress, queue the request
3. Use refresh token to obtain new access token
4. Update tokens in localStorage and auth store
5. Retry original request with new token
6. Process all queued requests

**Error Handling:**
- If refresh fails, user logged out and redirected to login with "session expired" message
- No manual intervention required - happens automatically

### 4. **Session Initialization**

**App.jsx Enhancement:**
```javascript
useEffect(() => {
  const { sessionInitialized, initializeSession } = useAuthStore.getState();
  
  if (!sessionInitialized) {
    console.log('🔄 Initializing session from localStorage...');
    initializeSession();
  }
}, []);
```

**When user opens the app:**
1. App component mounts
2. Session initialization triggered
3. Auth store checks localStorage
4. User session restored if valid and not expired
5. App ready for use - no login required if session still valid

## Data Flow

### Signup Flow
```
User Signs Up
    ↓
Backend validates & creates user
    ↓
Returns tokens (but user NOT logged in)
    ↓
Frontend: Clear any stale tokens
    ↓
Frontend: Show success message
    ↓
Frontend: Redirect to Login page
    ↓
User logs in manually
    ↓
Auth store: login() called
    ↓
Tokens stored in localStorage
    ↓
User navigated to home page
```

### Login Flow
```
User Logs In
    ↓
Backend validates credentials
    ↓
Returns tokens (access + refresh)
    ↓
Frontend: Call login() with tokens
    ↓
Tokens stored in localStorage
    ↓
Auth store updated with user info
    ↓
User navigated to home page
```

### Session Persistence
```
User Closes Browser / Refreshes Page
    ↓
Browser localStorage preserved
    ↓
User Returns / Page Reloads
    ↓
App mounts → initializeSession() called
    ↓
Auth store checks localStorage
    ↓
Tokens validated (not expired)
    ↓
Session restored ✓
    ↓
User stays logged in (no login required)
```

### Token Refresh
```
API Request Fails (401 Unauthorized)
    ↓
Axios interceptor intercepts error
    ↓
Is it an auth endpoint? → Yes: Let it fail
    ↓
Is token refresh already in progress? → Yes: Queue request
    ↓
Get refresh token from localStorage
    ↓
POST /auth/refresh-token with refresh token
    ↓
Backend validates refresh token
    ↓
Returns new access + refresh tokens
    ↓
Update tokens in localStorage and auth store
    ↓
Retry original request with new token
    ↓
Request succeeds ✓
```

## Security Features

### 1. **Token Expiration Buffer**
- Access tokens checked 1 minute before actual expiry
- Prevents use of expired tokens
- Automatic refresh before expiry

### 2. **Token Storage**
- Tokens stored in localStorage (not cookies)
- Multiple copies for backward compatibility:
  - `accessToken` (primary)
  - `token` (backward compatibility)
  - `refreshToken` (always needed)
  - `tokenExpiresAt` (validation)

### 3. **Session Validation**
```javascript
// Token expiration check
isTokenExpired: () => {
  if (!tokenExpiresAt) return true;
  const timeUntilExpiry = tokenExpiresAt - Date.now();
  const BUFFER_MS = 60 * 1000; // 1 minute buffer
  return timeUntilExpiry < BUFFER_MS;
}
```

### 4. **Logout Safety**
- All tokens cleared from localStorage
- Auth store cleared
- User must login again for access

## Testing the Flow

### Test 1: Signup → Login
```bash
1. Go to /register
2. Fill in form and submit
3. See "Account created successfully!" message
4. Auto-redirected to /login
5. Login with same credentials
6. Logged in and sent to home page ✓
```

### Test 2: Session Persistence
```bash
1. Login at localhost:3000
2. Close browser completely
3. Reopen browser and go to localhost:3000
4. Should be logged in WITHOUT login page ✓
```

### Test 3: Page Refresh
```bash
1. Login to app
2. Press F5 to refresh page
3. Should remain logged in
4. Auth store initialized on mount ✓
```

### Test 4: Token Expiration & Refresh
```bash
1. Login and check localStorage
2. Note tokenExpiresAt value
3. Wait until close to expiry OR make API request
4. Request succeeds with auto-refreshed token ✓
5. Check localStorage - new token and expiry set ✓
```

### Test 5: Logout
```bash
1. Login to app
2. Click logout button
3. Check localStorage - all tokens gone ✓
4. Redirected to login page ✓
5. Cannot access protected routes ✓
```

## API Endpoints

### Authentication Endpoints
- `POST /api/auth/login` - Login with email/password
- `POST /api/auth/register` - Register new account
- `POST /api/auth/refresh-token` - Refresh access token
- `POST /auth/loginWithGoogle` - Google OAuth login

### Response Format
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "id": "123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "CUSTOMER"
}
```

## State Management

### Auth Store Fields
```javascript
{
  user: {
    id: "123",
    email: "user@example.com",
    firstName: "John",
    lastName: "Doe",
    role: "CUSTOMER"
  },
  accessToken: "eyJhbGciOiJIUzI1NiIs...",
  refreshToken: "eyJhbGciOiJIUzI1NiIs...",
  tokenExpiresAt: 1645432800000,
  isAuthenticated: true,
  sessionInitialized: true
}
```

### Persisted Fields
Only these fields are persisted to localStorage:
- `user`
- `accessToken`
- `refreshToken`
- `tokenExpiresAt`
- `isAuthenticated`
- `sessionInitialized`

## Error Handling

### Signup Errors
```javascript
// Backend validation errors caught and displayed
- Email already exists → 409 Conflict
- Invalid email format → 400 Bad Request
- Weak password → 400 Bad Request
- Missing required fields → 400 Bad Request
```

### Login Errors
```javascript
// Wrong credentials handled gracefully
- Invalid email/password → Show error message
- Account not found → Show error message
- Server error → Show error message
```

### Session Errors
```javascript
// Automatic session recovery
- Token expired → Auto-refresh via interceptor
- Refresh failed → Clear session, redirect to login with "session expired" message
- Corrupted session data → Clean up localStorage, reset auth state
```

## Environment Variables

```env
# Backend
JWT_EXPIRATION_HOURS=24
JWT_REFRESH_EXPIRATION_DAYS=7
JWT_SECRET=your-secret-key

# Frontend
VITE_API_URL=/api
```

## Backend Integration Points

### Session Validation
The backend validates:
1. JWT token signature and expiration
2. Refresh token validity
3. User status (active/inactive)
4. User permissions for requested resource

### Token Generation
Tokens created on:
- User login
- User registration
- Token refresh
- Google OAuth login

## Migration Guide

### For Existing Users
- Existing sessions in localStorage will be automatically validated
- If token expired, user will be prompted to login
- No manual migration needed

### For New Code
- Use `useAuthStore` for auth state
- Call `initializeSession()` in App.jsx (already done)
- Use `getAccessToken()` to get valid token
- Login/logout through auth store methods

## Best Practices

1. **Always validate tokens before use**
   ```javascript
   const token = authStore.getAccessToken();
   if (token) {
     // Use token - it's valid
   }
   ```

2. **Clear tokens on logout**
   ```javascript
   authStore.logout(); // Clears everything
   ```

3. **Handle 401 errors gracefully**
   ```javascript
   // Axios interceptor handles this automatically
   // User redirected to login with session expired message
   ```

4. **Don't store sensitive data in localStorage**
   ```javascript
   // Only tokens and user ID/email stored
   // No passwords, SSN, or payment info
   ```

5. **Test session persistence**
   ```javascript
   // Close entire browser, reopen
   // Session should be restored automatically
   ```

## Troubleshooting

### User stays logged in after logout
- Clear browser localStorage manually
- Check that `authStore.logout()` is being called
- Verify no localStorage items left with 'token' or 'user'

### User keeps getting logged out
- Check token expiration time
- Verify refresh token is being returned from backend
- Check browser console for error messages

### Session not persisting on page reload
- Verify localStorage is enabled in browser
- Check for browser extensions blocking localStorage
- Check browser console for errors during initialization

### Token refresh failing
- Verify refresh token is stored in localStorage
- Check backend `/auth/refresh-token` endpoint
- Verify refresh token hasn't expired (7 days)
- Check backend JWT configuration

## Summary

The improved authentication flow provides:
- ✅ Clear signup → login → home flow
- ✅ Automatic session persistence
- ✅ Transparent token refresh
- ✅ Secure token storage
- ✅ Proper error handling
- ✅ Industry best practices
- ✅ User-friendly UX

Users now have a seamless authentication experience with automatic session persistence and no manual token management required.