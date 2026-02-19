package com.perfume.shop.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * 
 * Responsibilities:
 * - Extract JWT from Authorization header (Bearer token)
 * - Validate token signature and expiration
 * - Handle grace period for token refresh
 * - Set authenticated user in SecurityContext
 * - Handle token validation exceptions
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_TOKEN_START = 7;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            // No token provided - continue without authentication
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }
            
            // Extract token
            final String jwt = authHeader.substring(BEARER_TOKEN_START);
            
            // Validate token and set authentication
            // Always continue the filter chain - let Spring Security's
            // permitAll/authenticated rules decide, not this filter.
            // If token is invalid, the security context stays empty and
            // Spring will deny protected endpoints with 401.
            validateAndAuthenticateToken(jwt, request, response);
            filterChain.doFilter(request, response);

        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Unexpected error in JWT filter: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }
    
    /**
     * Validate token and set authentication in SecurityContext
     */
    private boolean validateAndAuthenticateToken(String jwt, HttpServletRequest request, HttpServletResponse response) {
        try {
            final String userEmail = jwtService.extractUsername(jwt);
            
            // Skip if already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load user details
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                
                // Check if token is valid
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    
                    // Check if token is within grace period
                    if (jwtService.isTokenExpired(jwt) && jwtService.isWithinGracePeriod(jwt)) {
                        log.debug("Token within grace period for user: {}", userEmail);
                        response.addHeader("X-Token-Expiring", "true");
                    }
                    
                    // Set authentication
                    UsernamePasswordAuthenticationToken authToken = 
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.debug("User authenticated: {}", userEmail);
                    return true;
                } else {
                    log.warn("Token invalid for user: {}", userEmail);
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("Token authentication failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Handle token validation errors
     */
    private void handleTokenError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
