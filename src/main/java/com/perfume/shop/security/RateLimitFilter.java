package com.perfume.shop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting Filter
 * 
 * Intercepts HTTP requests and applies rate limiting based on endpoint type:
 * - /api/auth/** endpoints: 5 requests per minute (strict for security)
 * - /api/admin/** endpoints: 50 requests per minute
 * - Other /api/** endpoints: 100 requests per minute
 * 
 * Rate limits are tracked per client IP address.
 * When limit is exceeded, returns 429 Too Many Requests with Retry-After
 * header.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        // Determine rate limit type based on endpoint
        boolean allowed;
        String limitType;

        if (requestUri.startsWith("/api/auth/")) {
            // Strict rate limiting for authentication endpoints
            allowed = rateLimitService.allowAuthRequest(clientIp);
            limitType = "auth";

            if (!allowed) {
                log.warn("Rate limit exceeded for auth endpoint. IP: {}, URI: {}", clientIp, requestUri);
                sendRateLimitResponse(response, "Authentication rate limit exceeded. Try again in 1 minute.", 60);
                return;
            }
        } else if (requestUri.startsWith("/api/admin/")) {
            // Moderate rate limiting for admin endpoints
            allowed = rateLimitService.allowAdminRequest(clientIp);
            limitType = "admin";

            if (!allowed) {
                log.warn("Rate limit exceeded for admin endpoint. IP: {}, URI: {}", clientIp, requestUri);
                sendRateLimitResponse(response, "Admin rate limit exceeded. Try again in 1 minute.", 60);
                return;
            }
        } else if (requestUri.startsWith("/api/")) {
            // General rate limiting for API endpoints
            allowed = rateLimitService.allowApiRequest(clientIp);
            limitType = "api";

            if (!allowed) {
                log.warn("Rate limit exceeded for API endpoint. IP: {}, URI: {}", clientIp, requestUri);
                sendRateLimitResponse(response, "API rate limit exceeded. Try again in 1 minute.", 60);
                return;
            }
        } else {
            // No rate limiting for non-API endpoints
            filterChain.doFilter(request, response);
            return;
        }

        // Add rate limit headers to response
        addRateLimitHeaders(response, clientIp, limitType);

        // Continue with the request
        filterChain.doFilter(request, response);
    }

    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxied requests
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Send rate limit exceeded response
     */
    private void sendRateLimitResponse(HttpServletResponse response, String message, int retryAfterSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        String jsonResponse = String.format(
                "{\"error\": \"Too Many Requests\", \"message\": \"%s\", \"retryAfter\": %d}",
                message, retryAfterSeconds);

        response.getWriter().write(jsonResponse);
    }

    /**
     * Add rate limit information headers to response
     */
    private void addRateLimitHeaders(HttpServletResponse response, String clientIp, String limitType) {
        long remaining;
        int limit;

        switch (limitType) {
            case "auth":
                remaining = rateLimitService.getAuthRemainingTokens(clientIp);
                limit = 5;
                break;
            case "admin":
                remaining = rateLimitService.getAdminRemainingTokens(clientIp);
                limit = 50;
                break;
            case "api":
            default:
                remaining = rateLimitService.getApiRemainingTokens(clientIp);
                limit = 100;
                break;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", "60"); // Reset in 60 seconds
    }
}
