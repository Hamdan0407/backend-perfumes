package com.perfume.shop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Token Service with enhanced security
 * 
 * Features:
 * - Access token generation (short-lived, 24 hours)
 * - Refresh token support (longer-lived, 7 days)
 * - Token expiry validation with grace period
 * - Token claim extraction
 * - Secure signature verification (HS256)
 */
@Service
@Slf4j
public class JwtService {
    
    @Value("${app.jwt.secret}")
    private String secretKey;
    
    @Value("${app.jwt.expiration:86400000}")
    private Long jwtExpiration; // Access token: 24 hours
    
    @Value("${app.jwt.refresh-expiration:604800000}")
    private Long refreshTokenExpiration; // Refresh token: 7 days
    
    @Value("${app.jwt.grace-period:60000}")
    private Long gracePeriodMs; // 1 minute grace period for token refresh
    
    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        try {
            final Claims claims = extractAllClaims(token);
            return claimsResolver.apply(claims);
        } catch (ExpiredJwtException e) {
            log.warn("Token is expired, but able to extract claims");
            return claimsResolver.apply(e.getClaims());
        }
    }
    
    /**
     * Extract token expiration time
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Generate access token for user
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails, jwtExpiration);
    }
    
    /**
     * Generate token with extra claims
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return generateToken(extraClaims, userDetails, jwtExpiration);
    }
    
    /**
     * Generate refresh token (longer expiration)
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return generateToken(claims, userDetails, refreshTokenExpiration);
    }
    
    /**
     * Build token with specified expiration
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Validate token against user details
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean isExpired = isTokenExpired(token);
            
            if (isExpired && !isWithinGracePeriod(token)) {
                log.warn("Token expired for user: {}", username);
                return false;
            }
            
            return username.equals(userDetails.getUsername());
        } catch (ExpiredJwtException e) {
            if (isWithinGracePeriod(e.getClaims().getExpiration())) {
                log.debug("Token within grace period, allowing refresh");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
    
    /**
     * Check if token is within grace period (allows refresh without full re-login)
     */
    public boolean isWithinGracePeriod(String token) {
        try {
            Date expiration = extractExpiration(token);
            return isWithinGracePeriod(expiration);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Check if expiration time is within grace period
     */
    private boolean isWithinGracePeriod(Date expirationTime) {
        if (expirationTime == null) {
            return false;
        }
        long expirationMs = expirationTime.getTime();
        long currentMs = System.currentTimeMillis();
        long timeSinceExpiry = currentMs - expirationMs;
        
        return timeSinceExpiry >= 0 && timeSinceExpiry <= gracePeriodMs;
    }
    
    /**
     * Get time until token expiration in milliseconds
     */
    public long getTimeUntilExpiration(String token) {
        try {
            Date expiration = extractExpiration(token);
            long expirationMs = expiration.getTime();
            long currentMs = System.currentTimeMillis();
            return Math.max(0, expirationMs - currentMs);
        } catch (Exception e) {
            return -1;
        }
    }
    
    /**
     * Check if token is a refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Get signing key from secret
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
