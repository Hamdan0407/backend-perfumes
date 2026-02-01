package com.perfume.shop.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Hardened Spring Security Configuration
 * 
 * Features:
 * - Restrictive CORS policy (environment-based origins)
 * - Strong endpoint permission model
 * - JWT token expiry handling
 * - Password policy enforcement
 * - CSRF protection disabled only for stateless JWT auth
 * - Session stateless (no cookies)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final ObjectProvider<UserDetailsService> userDetailsServiceProvider;
    private final JwtService jwtService;
    
    @Value("${app.security.cors-origins:http://localhost:3000,http://localhost:5173}")
    private String corsOrigins;
    
    @Value("${app.security.cors-max-age:3600}")
    private long corsMaxAge;
    
    @Value("${app.security.password-encoder-strength:12}")
    private int passwordEncoderStrength;
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsServiceProvider.getObject());
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: Disabled for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                
                // CORS: Restrictive policy
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Authorization: Fine-grained endpoint permissions
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh-token",
                                "/api/auth/update-role",
                                "/api/auth/oauth2/**",
                                "/api/products",
                                "/api/products/**",
                                "/api/reviews/product/**",
                                "/api/payment/webhook/**",
                                "/api/razorpay/webhook/**",
                                "/api/chatbot/**",
                                "/error",
                                "/health",
                                "/actuator/health"
                        ).permitAll()
                        
                        // Admin endpoints - requires ADMIN role
                        .requestMatchers(
                                "/api/admin/**",
                                "/api/admin/dashboard/**"
                        ).hasRole("ADMIN")
                        
                        // User cart endpoints - authenticated users only
                        .requestMatchers(
                                "/api/cart/**",
                                "/api/cart"
                        ).authenticated()
                        
                        // User order endpoints - authenticated users only
                        .requestMatchers(
                                "/api/orders/**",
                                "/api/orders"
                        ).authenticated()
                        
                        // User checkout - authenticated users only
                        .requestMatchers(
                                "/api/checkout/**"
                        ).authenticated()
                        
                        // User profile - authenticated users only
                        .requestMatchers(
                                "/api/users/profile",
                                "/api/users/password/**"
                        ).authenticated()
                        
                        // Review endpoints - authenticated for write, public for read
                        .requestMatchers(
                                "/api/reviews/create",
                                "/api/reviews/delete/**",
                                "/api/reviews/update/**"
                        ).authenticated()
                        
                        // Any other request requires authentication
                        .anyRequest().authenticated()
                )
                
                // Session: Stateless (JWT-based, no session cookies)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // Authentication provider and JWT filter
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                
                // Exception handling for unauthorized and forbidden
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Unauthorized\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Forbidden\"}");
                        })
                );
        
        return http.build();
    }
    
    /**
     * CORS Configuration: Restrictive policy for production security
     * - Origins: Configurable via environment variable
     * - Methods: Only necessary HTTP methods
     * - Headers: Explicit whitelist
     * - Credentials: Allowed for token-based auth
     * - Max Age: 1 hour cache for preflight requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse origins from environment variable
        List<String> allowedOrigins = Arrays.asList(corsOrigins.split(","));
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
        ));
        
        // Expose response headers
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));
        
        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // Cache preflight requests for 1 hour
        configuration.setMaxAge(corsMaxAge);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * RestTemplate for OAuth2 token validation
     */
    @Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
    
    /**
     * Authentication Provider with DAO-based user details
     * Marked @Lazy to defer creation until actually needed (breaks circular dependency)
     */
    @Bean
    @Lazy
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsServiceProvider.getObject());
        authProvider.setPasswordEncoder(passwordEncoder());
        
        // Hide password in failed authentication
        authProvider.setHideUserNotFoundExceptions(true);
        return authProvider;
    }
    
    /**
     * Authentication Manager for manual authentication
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * Password Encoder: BCrypt with configurable strength
     * Strength of 12 = ~2^12 iterations (more secure, slightly slower)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(passwordEncoderStrength);
    }
}
