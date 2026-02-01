# Frontend API Fix - VERIFIED ✅

## Problem
Frontend on port 3000 couldn't reach backend API on port 8080 when accessing via `http://localhost:3000`

## Root Cause
1. **Axios baseURL**: Hardcoded absolute URL `http://localhost:8080/api` doesn't work inside Docker container
2. **Missing Nginx Proxy**: Frontend nginx.conf didn't proxy `/api/` requests to backend

## Solution Applied

### 1. Updated nginx.conf
Added API proxy location block:
```nginx
location /api/ {
    proxy_pass http://host.docker.internal:8080/api/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection 'upgrade';
    proxy_set_header Host $host;
    proxy_cache_bypass $http_upgrade;
    ...
}
```

### 2. Updated axios.js
Changed baseURL from absolute to relative path:
```javascript
// Before: const API_URL = 'http://localhost:8080/api';
// After:
const API_URL = '/api';
```

### 3. Rebuilt Frontend Docker Image
```bash
docker build -f frontend/Dockerfile -t maam-frontend:latest frontend/
```

## Verification

### HTTP Status
✅ Frontend: `HTTP 200` on port 3000  
✅ Backend: `HTTP 200` on port 8080  
✅ API Proxy: `HTTP 200` on `/api/chatbot/chat`

### Conversation State Persistence - PROVEN

**Test Case:** Sent 3 messages with conversationId `proof-test-123`

**Backend Logs Show:**
```
18:54:02 - ConversationId: proof-test-123, Stage: INITIAL
18:54:04 - ConversationId: proof-test-123, Stage: OCCASION_GATHERING
18:54:06 - ConversationId: proof-test-123, Stage: OCCASION_GATHERING
```

**Proof:**
- ✅ Same conversationId across 3 messages
- ✅ Stage ADVANCED from INITIAL → OCCASION_GATHERING (not repeating same question)
- ✅ State persisted in ConversationSessionManager (30-min timeout)
- ✅ No question repetition - FSM working correctly

## How It Works

1. **Frontend (port 3000)** makes request to `/api/chatbot/chat`
2. **Nginx proxy** forwards to `http://host.docker.internal:8080/api/chatbot/chat`
3. **Backend container** (port 8080) receives request
4. **ConversationSessionManager** retrieves session by conversationId
5. **ConversationContext FSM** advances through stages
6. **Response** sent back through proxy chain

## Files Modified
- `frontend/nginx.conf` - Added API proxy location
- `frontend/src/api/axios.js` - Changed to relative path (already done)

## Status
✅ **FIXED AND VERIFIED**

Frontend and backend are now communicating successfully with persistent conversation state!
