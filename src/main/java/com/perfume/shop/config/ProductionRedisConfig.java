package com.perfume.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Configuration for Production
 * Provides caching, session storage, and rate limiting
 */
@Configuration
@EnableCaching
@Profile("production")
@Slf4j
public class ProductionRedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Redis Connection Factory for production
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection: {}:{}", redisHost, redisPort);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);

        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }

        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory(config);
        jedisConnectionFactory.afterPropertiesSet();

        return jedisConnectionFactory;
    }

    /**
     * Redis Template for general operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serialization for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Cache Manager with different TTL for different cache types
     */
    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Redis Cache Manager for production");

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default 10 minutes
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Configure specific TTLs for different cache types
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Product cache - 1 hour
        cacheConfigurations.put("products", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Categories cache - 2 hours
        cacheConfigurations.put("categories", defaultConfig.entryTtl(Duration.ofHours(2)));

        // User sessions - 30 minutes
        cacheConfigurations.put("user-sessions", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Search results - 15 minutes
        cacheConfigurations.put("search-results", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        // Featured products - 6 hours
        cacheConfigurations.put("featured-products", defaultConfig.entryTtl(Duration.ofHours(6)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Redis Template for session management
     */
    @Bean
    public RedisTemplate<String, String> sessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}