# Rate Limiting Configuration

## Overview
Rate limiting has been implemented to protect the API from abuse and brute force attacks.

## Rate Limits

| Endpoint Type | Limit | Window | Purpose |
|--------------|-------|--------|---------|
| `/api/auth/**` | 5 requests | 1 minute | Prevent brute force login attempts |
| `/api/admin/**` | 50 requests | 1 minute | Moderate admin endpoint usage |
| `/api/**` (other) | 100 requests | 1 minute | General API protection |

## How It Works

1. **Token Bucket Algorithm**: Uses Bucket4j library with token bucket algorithm
2. **Per-IP Tracking**: Rate limits are tracked per client IP address
3. **In-Memory Cache**: Uses Caffeine cache for fast, in-memory storage
4. **Automatic Reset**: Tokens refill every minute

## Response Headers

When rate limiting is active, the following headers are added to responses:

```
X-RateLimit-Limit: 100          # Maximum requests allowed
X-RateLimit-Remaining: 95       # Remaining requests in current window
X-RateLimit-Reset: 60           # Seconds until limit resets
```

## Rate Limit Exceeded Response

When the limit is exceeded, the API returns:

**Status Code:** `429 Too Many Requests`

**Response Body:**
```json
{
  "error": "Too Many Requests",
  "message": "API rate limit exceeded. Try again in 1 minute.",
  "retryAfter": 60
}
```

**Headers:**
```
Retry-After: 60
```

## Configuration

### Adjust Rate Limits

Edit `RateLimitService.java` to change limits:

```java
// Authentication endpoints: 5 requests per minute
private Bucket createAuthBucket() {
    Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
}

// API endpoints: 100 requests per minute
private Bucket createApiBucket() {
    Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
}

// Admin endpoints: 50 requests per minute
private Bucket createAdminBucket() {
    Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
    return Bucket.builder().addLimit(limit).build();
}
```

### Disable Rate Limiting

To disable rate limiting for testing, comment out the filter registration in `RateLimitConfig.java`:

```java
// @Bean
// public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
//     ...
// }
```

## Testing Rate Limits

### Test Authentication Endpoint

```bash
# Send 6 requests quickly (limit is 5)
for i in {1..6}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"password"}' \
    -w "\nStatus: %{http_code}\n"
done

# 6th request should return 429
```

### Test API Endpoint

```bash
# Send 101 requests quickly (limit is 100)
for i in {1..101}; do
  curl http://localhost:8080/api/products \
    -w "\nStatus: %{http_code}\n"
done

# 101st request should return 429
```

### Check Rate Limit Headers

```bash
curl -I http://localhost:8080/api/products

# Response headers:
# X-RateLimit-Limit: 100
# X-RateLimit-Remaining: 99
# X-RateLimit-Reset: 60
```

## Monitoring

### View Cache Statistics

The `RateLimitService` provides statistics:

```java
Map<String, Object> stats = rateLimitService.getStatistics();
// Returns:
// {
//   "authCacheSize": 10,
//   "apiCacheSize": 50,
//   "adminCacheSize": 5
// }
```

### Clear Rate Limits for IP

```java
rateLimitService.clearRateLimits("192.168.1.1");
```

## Production Considerations

### 1. Distributed Systems

For multi-instance deployments, consider using Redis for distributed rate limiting:

```java
// Use Redis instead of Caffeine
@Bean
public ProxyManager<String> proxyManager(RedissonClient redisson) {
    return new RedissonBasedProxyManager(redisson);
}
```

### 2. IP Detection Behind Proxy

The filter checks `X-Forwarded-For` and `X-Real-IP` headers for proxied requests.

Ensure your load balancer/proxy sets these headers:

```nginx
# Nginx configuration
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
```

### 3. Whitelist IPs

To whitelist specific IPs (e.g., monitoring services), modify `RateLimitFilter.java`:

```java
private static final Set<String> WHITELISTED_IPS = Set.of(
    "127.0.0.1",
    "your-monitoring-ip"
);

@Override
protected void doFilterInternal(...) {
    String clientIp = getClientIp(request);
    
    if (WHITELISTED_IPS.contains(clientIp)) {
        filterChain.doFilter(request, response);
        return;
    }
    
    // Continue with rate limiting...
}
```

## Files Created

1. **`RateLimitService.java`** - Core rate limiting logic with Bucket4j
2. **`RateLimitFilter.java`** - HTTP filter to intercept requests
3. **`RateLimitConfig.java`** - Spring configuration to register filter
4. **`pom.xml`** - Added Bucket4j and Caffeine dependencies

## Dependencies Added

```xml
<!-- Rate Limiting with Bucket4j -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>

<!-- Caffeine Cache for Rate Limiting -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

## Security Benefits

✅ **Brute Force Protection**: Limits login attempts to 5 per minute  
✅ **DDoS Mitigation**: Prevents API abuse and excessive requests  
✅ **Resource Protection**: Ensures fair usage across all clients  
✅ **Cost Control**: Reduces infrastructure costs from excessive API calls  

## Next Steps

- [ ] Monitor rate limit violations in production logs
- [ ] Adjust limits based on actual usage patterns
- [ ] Consider implementing user-based rate limiting (in addition to IP-based)
- [ ] Set up alerts for repeated rate limit violations
