# Authentication UX Flow Diagrams

## 1. User Journey: New User Registration

```
START
  ↓
┌─────────────────────┐
│ User visits app     │
│ /register page      │
└──────────┬──────────┘
           ↓
┌─────────────────────────────────────┐
│ User fills registration form:       │
│ - First Name                        │
│ - Last Name                         │
│ - Email                             │
│ - Password                          │
│ - Confirm Password                  │
└──────────┬──────────────────────────┘
           ↓
┌─────────────────────┐
│ Click "Create      │
│ Account"           │
└──────────┬──────────┘
           ↓
    [API CALL]
    POST /api/auth/register
           ↓
    ┌──────────────┐
    │ Backend:     │
    │ - Validates  │
    │ - Creates    │
    │ - Returns    │
    │   tokens     │
    └──────────────┘
           ↓
┌─────────────────────────────────────┐
│ SUCCESS MESSAGE:                    │
│ "Account created successfully!      │
│  Please login with your credentials"│
└──────────┬──────────────────────────┘
           ↓
    [IMPORTANT]
    Clear all localStorage tokens
           ↓
    [DELAY 1.5s]
    Show success toast
           ↓
    navigate('/login')
           ↓
┌─────────────────────┐
│ Login page loads    │
│ (user sees it)      │
└──────────┬──────────┘
           ↓
┌─────────────────────────────┐
│ User fills login form:      │
│ - Email                     │
│ - Password                  │
└──────────┬──────────────────┘
           ↓
┌─────────────────────┐
│ Click "Sign In"     │
└──────────┬──────────┘
           ↓
    [API CALL]
    POST /api/auth/login
           ↓
    ┌──────────────┐
    │ Backend:     │
    │ - Validates  │
    │ - Returns    │
    │   tokens     │
    └──────────────┘
           ↓
┌──────────────────────────────────────┐
│ Frontend: Call authStore.login()     │
│                                      │
│ login(userData, token, refresh, ...)│
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Auth Store:                          │
│ - Save user data                     │
│ - Save tokens to localStorage        │
│ - Set isAuthenticated = true         │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ navigate('/')                        │
│ (React Router - no page reload)      │
└──────────┬──────────────────────────┘
           ↓
┌─────────────────────┐
│ HOME PAGE LOADED    │
│ User logged in ✓    │
└─────────────────────┘
           ↓
        SUCCESS
```

---

## 2. User Journey: Returning User

```
START
  ↓
┌─────────────────────────────────────┐
│ User opens browser                  │
│ localhost:3000                      │
└──────────┬──────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│ React loads, App.jsx mounts         │
└──────────┬──────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│ useEffect hook runs (once):         │
│                                     │
│ if (!sessionInitialized) {          │
│   initializeSession()               │
│ }                                   │
└──────────┬──────────────────────────┘
           ↓
┌─────────────────────────────────────┐
│ Auth Store: initializeSession()     │
│                                     │
│ Check localStorage for:             │
│ - user                              │
│ - accessToken                       │
│ - refreshToken                      │
│ - tokenExpiresAt                    │
└──────────┬──────────────────────────┘
           ↓
    ┌──────────────────┐
    │ Data found?      │
    └──┬──────────────┬┘
       │ NO           │ YES
       │              │
       ↓              ↓
    [STAY]      ┌──────────────────┐
    LOGGED      │ Parse & Validate │
    OUT         │ Token not expired?│
       │        └────────┬─────────┘
       │                 │
       │        ┌────────┴────────┐
       │        │ YES            NO
       │        │                │
       │        ↓                ↓
       │    ┌─────────────┐  [STAY]
       │    │ Restore     │  LOGGED
       │    │ Session     │  OUT
       │    └─────────────┘
       │
       ↓
┌──────────────────────────────────────┐
│ Auth Store updated:                  │
│ - isAuthenticated = true             │
│ - user = {...}                       │
│ - tokens = {...}                     │
│ - sessionInitialized = true          │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ App Renders with logged-in state     │
└──────────┬──────────────────────────┘
           ↓
    ┌──────────────┐
    │ Render Home  │
    │ Page or      │
    │ Redirect to  │
    │ Login        │
    └──────────────┘
           ↓
┌──────────────────────────────────────┐
│ User Experience:                     │
│ Instantly sees:                      │
│ - Home page (if tokens valid)        │
│ OR                                   │
│ - Login page (if tokens expired)     │
│                                      │
│ NO LOGIN REQUIRED if tokens valid ✓  │
└──────────────────────────────────────┘
```

---

## 3. Token Lifecycle

```
┌─────────────────────┐
│ USER LOGS IN        │
└──────────┬──────────┘
           ↓
┌──────────────────────────────────────┐
│ Backend generates tokens:            │
│                                      │
│ Access Token:                        │
│ - Expiry: 24 hours from now         │
│ - Used for: API requests            │
│ - Stored: localStorage              │
│                                      │
│ Refresh Token:                       │
│ - Expiry: 7 days from now           │
│ - Used for: Refresh access token   │
│ - Stored: localStorage              │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Frontend stores both tokens:         │
│                                      │
│ localStorage.setItem('accessToken')  │
│ localStorage.setItem('refreshToken') │
│ localStorage.setItem('tokenExpiresAt')│
└──────────┬──────────────────────────┘
           ↓
    WAIT ⏳
    (1-23 hours)
           ↓
┌──────────────────────────────────────┐
│ User makes API request               │
│                                      │
│ Access Token Expires in < 1 minute   │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Axios interceptor detects:           │
│ "Token expires soon!"                │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Automatic token refresh triggered    │
│ (BEFORE token actually expires)      │
└──────────┬──────────────────────────┘
           ↓
    [API CALL]
    POST /api/auth/refresh-token
    { refreshToken: "..." }
           ↓
    ┌──────────────┐
    │ Backend:     │
    │ - Validates  │
    │   refresh    │
    │ - Generates  │
    │   new tokens │
    │ - Returns    │
    │   both       │
    └──────────────┘
           ↓
┌──────────────────────────────────────┐
│ Frontend updates tokens:             │
│                                      │
│ localStorage.setItem('accessToken')  │
│ localStorage.setItem('refreshToken') │
│ localStorage.setItem('tokenExpiresAt')│
│                                      │
│ authStore.updateTokens(...)          │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Tokens now valid for another:        │
│                                      │
│ Access Token: 24 hours               │
│ Refresh Token: 7 days                │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Original API request retried:        │
│                                      │
│ + Authorization: Bearer <NEW_TOKEN>  │
│ REQUEST SUCCEEDS ✓                   │
└──────────┬──────────────────────────┘
           ↓
    USER UNAWARE
    (Entire refresh: 50-200ms)
           ↓
    CONTINUE USING APP

---

ALTERNATIVE: Refresh token also expires

┌──────────────────────────────────────┐
│ Refresh token expires (7 days)       │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ API request fails (401):             │
│ "Unable to refresh token"            │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ Axios interceptor response handler:  │
│                                      │
│ - Clear all tokens from localStorage │
│ - Clear auth store                   │
│ - Redirect to /login                 │
│ - Show: "Session expired"            │
└──────────┬──────────────────────────┘
           ↓
┌──────────────────────────────────────┐
│ USER SEES:                           │
│ "Your session has expired.           │
│  Please log in again."               │
│                                      │
│ Must login again ✓                   │
└──────────────────────────────────────┘
```

---

## 4. Request Flow with Token Handling

```
┌─────────────────────┐
│ Component Makes     │
│ API Request         │
│ (e.g., GET /orders) │
└──────────┬──────────┘
           ↓
┌──────────────────────────────────────┐
│ Axios Request Interceptor:           │
│                                      │
│ 1. Get token from localStorage       │
│ 2. Add to request header:            │
│    Authorization: Bearer <TOKEN>     │
│ 3. Send request                      │
└──────────┬──────────────────────────┘
           ↓
    [NETWORK]
           ↓
    ┌──────────────────┐
    │ Backend receives │
    │ request with     │
    │ token header     │
    └────────┬─────────┘
             ↓
    ┌──────────────────────────────────┐
    │ Backend:                         │
    │ - Validates token signature      │
    │ - Checks token expiration        │
    │ - Checks user permissions        │
    └────────┬────────────┬──────┬─────┘
             │            │      │
        VALID        INVALID   ERROR
             │            │      │
             ↓            ↓      ↓
        ┌────────┐   ┌────┐  ┌─────┐
        │ Process│   │401 │  │500  │
        │request │   └──┬─┘  └──┬──┘
        └────┬───┘      │       │
             │          │       │
    [NETWORK]          [NETWORK]
             │          │       │
             ↓          ↓       ↓
    ┌────────────────────────────────────┐
    │ Axios Response Interceptor:        │
    └────────┬────────┬──────────────┬───┘
             │        │              │
           200       401            OTHER
             │        │              │
             ↓        ↓              ↓
        ┌────────┐  [REFRESH]  ┌─────────┐
        │ Return │  TOKEN      │Return   │
        │ data   │  LOGIC ①    │error    │
        └────────┘  (see ①)    └─────────┘
             │
             ↓
    ┌──────────────────┐
    │ Component gets   │
    │ data             │
    │ (success) ✓      │
    └──────────────────┘

① TOKEN REFRESH LOGIC (if 401):

    Is this an auth endpoint?
    (/auth/login, /auth/register, /auth/refresh-token)
    ↓
    YES → return error immediately
    ↓
    NO → Check if refresh in progress?
    ↓
    YES → Queue this request, wait for refresh
    ↓
    NO → Start refresh process:
    
    1. Get refreshToken from localStorage
    2. POST /auth/refresh-token
    3. Update tokens in localStorage
    4. Update auth store
    5. Retry original request with new token
    6. Process queued requests
    
    Result: Original request retried and succeeds ✓
```

---

## 5. Session State Diagram

```
                    ┌──────────────────┐
                    │   INITIALIZING   │
                    │   (app mount)    │
                    └────────┬─────────┘
                             │
                    Check localStorage
                             │
                  ┌──────────┼──────────┐
                  │          │          │
            No data      Valid      Expired
                  │       session   tokens
                  │          │          │
                  ↓          ↓          ↓
            ┌─────────┐ ┌────────┐ ┌──────────┐
            │ LOGGED  │ │LOGGED  │ │ LOGGED   │
            │ OUT     │ │IN ✓    │ │ OUT      │
            │         │ │        │ │ (expired)│
            └────┬────┘ └───┬────┘ └─────┬────┘
                 │          │            │
                 │          │            │
        (show login)   (render app)   (show login)
                 │          │            │
                 ↓          ↓            ↓
            ┌──────────────────────────────────┐
            │ App fully rendered & ready       │
            └────────────┬─────────────────────┘
                         │
              ┌──────────┼──────────┐
              │          │          │
            Click     Click      Make API
            "Sign In"  "Logout"   Request
              │          │          │
              ↓          ↓          ↓
        ┌──────────┐ ┌────────┐ ┌──────────┐
        │LOGGING   │ │LOGGING │ │ Check    │
        │IN        │ │OUT     │ │ token    │
        │(waiting) │ │        │ │ expiry   │
        └─────┬────┘ └───┬────┘ └────┬─────┘
              │          │            │
    (success) │          │    (valid or refresh)
              ↓          ↓            ↓
        ┌──────────┐ ┌────────┐ ┌──────────┐
        │LOGGED    │ │LOGGED  │ │ Continue │
        │IN ✓      │ │OUT ✓   │ │ request  │
        └──────────┘ └────────┘ └──────────┘
              │          │
              │          │
         (show app)  (show login)

Legend:
✓ = Success
→ = Transition
```

---

## 6. Component Dependencies

```
App.jsx
  │
  ├── useAuthStore.initializeSession() [on mount]
  │
  ├── Routes
  │   ├── /register → Register.jsx
  │   │   └── authAPI.register()
  │   │       └── API response
  │   │           └── localStorage clear
  │   │               └── navigate('/login')
  │   │
  │   ├── /login → Login.jsx
  │   │   ├── useAuthStore.login()
  │   │   │   ├── localStorage save
  │   │   │   └── navigate('/')
  │   │   │
  │   │   └── handleGoogleSuccess
  │   │       └── authAPI.loginWithGoogle()
  │   │
  │   ├── / → Home (public)
  │   │
  │   ├── /cart → PrivateRoute
  │   │   └── Cart.jsx
  │   │       └── Uses authAPI (protected)
  │   │
  │   └── /admin → AdminRoute
  │       └── AdminPanel.jsx
  │           └── Uses authAPI (protected)
  │
  └── Navbar
      ├── Displays user info (if logged in)
      └── Logout button
          └── authStore.logout()

Auth Flow:
  authAPI.js
    ├── login()
    ├── register()
    ├── refreshToken()
    └── logout()
        │
        └── Uses axios instance
            │
            └── axios.js
                ├── Request interceptor
                │   └── Add token to header
                │
                └── Response interceptor
                    ├── Handle 401
                    ├── Refresh token if needed
                    └── Retry request

Data Persistence:
  authStore.js (Zustand)
    ├── State in memory
    │
    └── Persist middleware
        └── localStorage
            ├── accessToken
            ├── refreshToken
            ├── user
            ├── tokenExpiresAt
            └── auth-storage (JSON)
```

---

## 7. Timeline: Complete User Session

```
Time Event                              State
──── ────────────────────────────────── ─────────────
0:00 User opens http://localhost:3000  Loading...

0.1s App.jsx mounts                      Loading...

0.2s useEffect runs:                     
     initializeSession()                Initializing...

0.3s Auth store checks localStorage     
     (no session found)                 Not Logged In

0.5s App renders with login option      Not Logged In ✓

     [User clicks "Sign Up"]
     
     Register.jsx loads                  Ready to Signup

1.0s User fills form & submits          Processing...

1.5s API request sent to backend        Processing...

2.0s Backend validates & creates        Processing...

2.5s Response received with tokens      Success!

2.6s Frontend clears old tokens         Cleanup

2.7s Success toast shown for 1.5s       Redirecting...

4.2s navigate('/login') executed        On Login Page ✓

     [User enters credentials]
     
4.5s User clicks "Sign In"              Processing...

5.0s API request sent to backend        Processing...

5.5s Backend validates credentials      Processing...

6.0s Response received with tokens      Success!

6.1s authStore.login() called           Storing...

6.2s Tokens stored in localStorage      Stored ✓

6.3s isAuthenticated = true             Authenticated ✓

6.4s navigate('/') executed             Redirecting...

6.5s Home page renders                  Home ✓

     [User uses app]
     
30:0 User makes API request             Working...

30.1s Access token check: expires in    
      < 60 seconds                      Refreshing...

30.2s Auto token refresh triggered      Processing...

30.3s POST /auth/refresh-token sent     Processing...

30.8s New tokens received & stored      Refreshed ✓

30.9s Original request retried          Processing...

31.0s Request succeeds with new token   Success ✓

     [User continues using app]
     
     [User closes browser]
     
LATER

23:45 User opens browser again          Loading...

23:46 localhost:3000 opened             Loading...

23:47 App.jsx mounts                    Initializing...

23:48 initializeSession() runs          Checking...

23:49 localStorage has valid tokens     Found!

23:50 Session restored from localStorage Restoring...

23:51 Auth store populated with data    Restored ✓

23:52 App renders with user logged in   Home ✓

      (NO login required!)

     [User navigates without issues]
     
     [User clicks logout]
     
24:00 Logout triggered                  Processing...

24.1s All tokens cleared from           
      localStorage                      Clearing...

24.2s Auth store cleared                Cleared ✓

24.3s navigate('/login') executed       Redirecting...

24.4s Login page rendered               Not Logged In ✓
```

---

These diagrams show the complete authentication flow, token lifecycle, and user journeys through the improved authentication system.