package com.perfume.shop.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Bucket4j
 * 
 * Implements token bucket algorithm for API rate limiting:
 * - Authentication endpoints: 5 requests per minute
 * - General API endpoints: 100 requests per minute
 * - Admin endpoints: 50 requests per minute
 * 
 * Rate limits are tracked per IP address and stored in-memory using Caffeine
 * cache.
 */
@Service
@Slf4j
public class RateLimitService {

    // Cache for storing rate limit buckets per IP
    private final Cache<String, Bucket> authBucketCache;
    private final Cache<String, Bucket> apiBucketCache;
    private final Cache<String, Bucket> adminBucketCache;

    public RateLimitService() {
        // Initialize caches with 1 hour expiration
        this.authBucketCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(10000)
                .build();

        this.apiBucketCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(10000)
                .build();

        this.adminBucketCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(10000)
                .build();
    }

    /**
     * Check if request is allowed for authentication endpoints
     * Limit: 5 requests per minute
     */
    public boolean allowAuthRequest(String clientIp) {
        Bucket bucket = authBucketCache.get(clientIp, k -> createAuthBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if request is allowed for general API endpoints
     * Limit: 100 requests per minute
     */
    public boolean allowApiRequest(String clientIp) {
        Bucket bucket = apiBucketCache.get(clientIp, k -> createApiBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Check if request is allowed for admin endpoints
     * Limit: 50 requests per minute
     */
    public boolean allowAdminRequest(String clientIp) {
        Bucket bucket = adminBucketCache.get(clientIp, k -> createAdminBucket());
        return bucket.tryConsume(1);
    }

    /**
     * Get remaining tokens for authentication endpoint
     */
    public long getAuthRemainingTokens(String clientIp) {
        Bucket bucket = authBucketCache.getIfPresent(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : 5;
    }

    /**
     * Get remaining tokens for API endpoint
     */
    public long getApiRemainingTokens(String clientIp) {
        Bucket bucket = apiBucketCache.getIfPresent(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : 100;
    }

    /**
     * Get remaining tokens for admin endpoint
     */
    public long getAdminRemainingTokens(String clientIp) {
        Bucket bucket = adminBucketCache.getIfPresent(clientIp);
        return bucket != null ? bucket.getAvailableTokens() : 50;
    }

    /**
     * Create bucket for authentication endpoints
     * 5 requests per minute with refill of 5 tokens every minute
     */
    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for general API endpoints
     * 100 requests per minute with refill of 100 tokens every minute
     */
    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for admin endpoints
     * 50 requests per minute with refill of 50 tokens every minute
     */
    private Bucket createAdminBucket() {
        Bandwidth limit = Bandwidth.classic(50, Refill.intervally(50, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Clear all rate limit buckets for a specific IP
     * Useful for testing or manual reset
     */
    public void clearRateLimits(String clientIp) {
        authBucketCache.invalidate(clientIp);
        apiBucketCache.invalidate(clientIp);
        adminBucketCache.invalidate(clientIp);
        log.info("Cleared rate limits for IP: {}", clientIp);
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
                "authCacheSize", authBucketCache.estimatedSize(),
                "apiCacheSize", apiBucketCache.estimatedSize(),
                "adminCacheSize", adminBucketCache.estimatedSize());
    }
}
