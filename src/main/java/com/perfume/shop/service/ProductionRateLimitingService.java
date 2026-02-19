package com.perfume.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Production Rate Limiting Service using Redis
 * Implements sliding window rate limiting for API endpoints
 */
@Service
@RequiredArgsConstructor
@Profile("production")
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ProductionRateLimitingService {

    private final RedisTemplate<String, String> sessionRedisTemplate;

    @Value("${app.rate-limiting.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limiting.requests-per-hour:1000}")
    private int requestsPerHour;

    @Value("${app.rate-limiting.burst-capacity:10}")
    private int burstCapacity;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String MINUTE_SUFFIX = ":minute";
    private static final String HOUR_SUFFIX = ":hour";
    private static final String BURST_SUFFIX = ":burst";

    /**
     * Check if request is allowed under rate limits
     * 
     * @param clientId Client identifier (IP address or user ID)
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String clientId) {
        return isAllowed(clientId, "default");
    }

    /**
     * Check if request is allowed under rate limits for specific endpoint
     * 
     * @param clientId Client identifier (IP address or user ID)
     * @param endpoint Endpoint identifier for different rate limits
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String clientId, String endpoint) {
        try {
            // Check burst limit (very short term - 10 seconds)
            if (!checkBurstLimit(clientId, endpoint)) {
                log.warn("Burst limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
                return false;
            }

            // Check per-minute limit
            if (!checkMinuteLimit(clientId, endpoint)) {
                log.warn("Per-minute limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
                return false;
            }

            // Check per-hour limit
            if (!checkHourLimit(clientId, endpoint)) {
                log.warn("Per-hour limit exceeded for client: {} on endpoint: {}", clientId, endpoint);
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking rate limits for client: {}, allowing request", clientId, e);
            return true; // Fail open - allow request if Redis is down
        }
    }

    /**
     * Get current rate limit status for client
     * 
     * @param clientId Client identifier
     * @return Rate limit status information
     */
    public RateLimitStatus getRateLimitStatus(String clientId) {
        try {
            String minuteKey = RATE_LIMIT_PREFIX + clientId + MINUTE_SUFFIX;
            String hourKey = RATE_LIMIT_PREFIX + clientId + HOUR_SUFFIX;
            String burstKey = RATE_LIMIT_PREFIX + clientId + BURST_SUFFIX;

            String minuteCount = sessionRedisTemplate.opsForValue().get(minuteKey);
            String hourCount = sessionRedisTemplate.opsForValue().get(hourKey);
            String burstCount = sessionRedisTemplate.opsForValue().get(burstKey);

            return RateLimitStatus.builder()
                    .requestsThisMinute(minuteCount != null ? Integer.parseInt(minuteCount) : 0)
                    .requestsThisHour(hourCount != null ? Integer.parseInt(hourCount) : 0)
                    .requestsThisBurst(burstCount != null ? Integer.parseInt(burstCount) : 0)
                    .minuteLimit(requestsPerMinute)
                    .hourLimit(requestsPerHour)
                    .burstLimit(burstCapacity)
                    .build();

        } catch (Exception e) {
            log.error("Error getting rate limit status for client: {}", clientId, e);
            return RateLimitStatus.builder().build();
        }
    }

    private boolean checkBurstLimit(String clientId, String endpoint) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + endpoint + BURST_SUFFIX;
        return checkLimit(key, burstCapacity, Duration.ofSeconds(10));
    }

    private boolean checkMinuteLimit(String clientId, String endpoint) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + endpoint + MINUTE_SUFFIX;
        return checkLimit(key, requestsPerMinute, Duration.ofMinutes(1));
    }

    private boolean checkHourLimit(String clientId, String endpoint) {
        String key = RATE_LIMIT_PREFIX + clientId + ":" + endpoint + HOUR_SUFFIX;
        return checkLimit(key, requestsPerHour, Duration.ofHours(1));
    }

    private boolean checkLimit(String key, int limit, Duration window) {
        String currentCountStr = sessionRedisTemplate.opsForValue().get(key);
        int currentCount = currentCountStr != null ? Integer.parseInt(currentCountStr) : 0;

        if (currentCount >= limit) {
            return false;
        }

        // Increment counter
        if (currentCount == 0) {
            // Set with expiration
            sessionRedisTemplate.opsForValue().set(key, "1", window.toMillis(), TimeUnit.MILLISECONDS);
        } else {
            // Increment existing
            sessionRedisTemplate.opsForValue().increment(key);
        }

        return true;
    }

    /**
     * Rate limit status information
     */
    @lombok.Builder
    @lombok.Data
    public static class RateLimitStatus {
        private int requestsThisMinute;
        private int requestsThisHour;
        private int requestsThisBurst;
        private int minuteLimit;
        private int hourLimit;
        private int burstLimit;

        public boolean isMinuteLimitExceeded() {
            return requestsThisMinute >= minuteLimit;
        }

        public boolean isHourLimitExceeded() {
            return requestsThisHour >= hourLimit;
        }

        public boolean isBurstLimitExceeded() {
            return requestsThisBurst >= burstLimit;
        }

        public int getRemainingMinuteRequests() {
            return Math.max(0, minuteLimit - requestsThisMinute);
        }

        public int getRemainingHourRequests() {
            return Math.max(0, hourLimit - requestsThisHour);
        }
    }
}