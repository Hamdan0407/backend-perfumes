package com.perfume.shop.config;

import com.perfume.shop.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate Limiting Configuration
 * 
 * Registers the RateLimitFilter to intercept all API requests
 * and apply rate limiting based on endpoint type.
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private final RateLimitFilter rateLimitFilter;

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration() {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(rateLimitFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1); // Execute before other filters
        registration.setName("rateLimitFilter");
        return registration;
    }
}
