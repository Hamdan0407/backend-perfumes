package com.perfume.shop.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

        private final JwtService jwtService;
        private final UserDetailsService userDetailsService;

        @Value("${app.security.cors-origins:http://localhost:3000,http://localhost:5173}")
        private String corsOrigins;

        @Value("${app.security.cors-max-age:3600}")
        private long corsMaxAge;

        @Value("${app.security.password-encoder-strength:12}")
        private int passwordEncoderStrength;

        public SecurityConfig(JwtService jwtService, @Lazy UserDetailsService userDetailsService) {
                this.jwtService = jwtService;
                this.userDetailsService = userDetailsService;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.deny())
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .maxAgeInSeconds(31536000)
                                                                .includeSubDomains(true)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/api/auth/refresh-token",
                                                                "/api/auth/forgot-password",
                                                                "/api/auth/reset-password",
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
                                                                "/actuator/health",
                                                                "/actuator/info",
                                                                "/actuator/prometheus",
                                                                "/swagger-ui.html",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/api-docs/**")
                                                .permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/api/cart/**", "/api/cart").authenticated()
                                                .requestMatchers("/api/orders/**", "/api/orders").authenticated()
                                                .requestMatchers("/api/checkout/**").authenticated()
                                                .requestMatchers("/api/users/profile", "/api/users/password/**")
                                                .authenticated()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/reviews/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/reviews")
                                                .authenticated()
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/reviews/**")
                                                .authenticated()
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/reviews/**")
                                                .authenticated()
                                                .requestMatchers("/api/reviews/can-review/**").authenticated()
                                                .requestMatchers("/api/product/**").permitAll()
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
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
                                                }));

                return http.build();
        }

        @Bean
        public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                provider.setHideUserNotFoundExceptions(true);
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(passwordEncoderStrength);
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                List<String> allowedOrigins = Arrays.asList(corsOrigins.split(","));
                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(
                                Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
                configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(corsMaxAge);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public org.springframework.web.client.RestTemplate restTemplate() {
                return new org.springframework.web.client.RestTemplate();
        }
}
