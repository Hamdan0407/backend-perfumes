package com.perfume.shop.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfume.shop.service.ProductionRateLimitingService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Production Rate Limiting Filter
 * Applied to all API endpoints to enforce rate limits
 */
@Component
@RequiredArgsConstructor
@Profile("production")
@ConditionalOnProperty(name = "app.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ProductionRateLimitingFilter implements Filter {
    
    private final ProductionRateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Skip rate limiting for health check endpoints
        String requestPath = httpRequest.getRequestURI();
        if (isExcludedPath(requestPath)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Get client identifier (prefer user ID, fallback to IP)
        String clientId = getClientIdentifier(httpRequest);
        String endpoint = getEndpointCategory(requestPath);
        
        // Check rate limits
        if (!rateLimitingService.isAllowed(clientId, endpoint)) {
            handleRateLimitExceeded(httpResponse, clientId);
            return;
        }
        
        // Add rate limit headers to response
        addRateLimitHeaders(httpResponse, clientId);
        
        // Continue with request
        chain.doFilter(request, response);
    }
    
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get user ID from JWT token in header
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                // Extract user ID from token (simplified - in real implementation, parse JWT)
                // For now, use IP address as fallback
            } catch (Exception e) {
                log.debug("Could not extract user ID from token, using IP address");
            }
        }
        
        // Fallback to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String getEndpointCategory(String requestPath) {
        if (requestPath.startsWith("/api/auth/")) {
            return "auth";
        } else if (requestPath.startsWith("/api/admin/")) {
            return "admin";
        } else if (requestPath.startsWith("/api/payment/")) {
            return "payment";
        } else if (requestPath.startsWith("/api/products/")) {
            return "products";
        } else if (requestPath.startsWith("/api/orders/")) {
            return "orders";
        }
        return "default";
    }
    
    private boolean isExcludedPath(String requestPath) {
        return requestPath.startsWith("/actuator/") ||
               requestPath.equals("/health") ||
               requestPath.equals("/") ||
               requestPath.startsWith("/static/") ||
               requestPath.startsWith("/favicon.ico");
    }
    
    private void addRateLimitHeaders(HttpServletResponse response, String clientId) {
        try {
            ProductionRateLimitingService.RateLimitStatus status = 
                rateLimitingService.getRateLimitStatus(clientId);
            
            response.setHeader("X-RateLimit-Limit-Minute", String.valueOf(status.getMinuteLimit()));
            response.setHeader("X-RateLimit-Remaining-Minute", String.valueOf(status.getRemainingMinuteRequests()));
            response.setHeader("X-RateLimit-Limit-Hour", String.valueOf(status.getHourLimit()));
            response.setHeader("X-RateLimit-Remaining-Hour", String.valueOf(status.getRemainingHourRequests()));
            
        } catch (Exception e) {
            log.debug("Could not add rate limit headers", e);
        }
    }
    
    private void handleRateLimitExceeded(HttpServletResponse response, String clientId) 
            throws IOException {
        
        log.warn("Rate limit exceeded for client: {}", clientId);
        
        response.setStatus(429); // 429 Too Many Requests
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("status", 429);
        errorResponse.put("timestamp", System.currentTimeMillis());
        
        try {
            ProductionRateLimitingService.RateLimitStatus status = 
                rateLimitingService.getRateLimitStatus(clientId);
            
            errorResponse.put("retryAfter", 60); // seconds
            errorResponse.put("limits", Map.of(
                "perMinute", status.getMinuteLimit(),
                "perHour", status.getHourLimit(),
                "remainingMinute", status.getRemainingMinuteRequests(),
                "remainingHour", status.getRemainingHourRequests()
            ));
            
            response.setHeader("Retry-After", "60");
            
        } catch (Exception e) {
            log.debug("Could not get rate limit status for error response", e);
        }
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}