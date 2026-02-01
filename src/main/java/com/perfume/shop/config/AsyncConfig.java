package com.perfume.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution.
 * Enables @Async annotation processing with thread pool executor.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {
    
    /**
     * Configure thread pool for email sending tasks
     * Prevents blocking of main request thread
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core threads that are always active
        executor.setCorePoolSize(5);
        
        // Maximum threads allowed
        executor.setMaxPoolSize(20);
        
        // Queue capacity before creating new threads
        executor.setQueueCapacity(100);
        
        // Thread name prefix for debugging
        executor.setThreadNamePrefix("email-task-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Wait up to 30 seconds for tasks to complete
        executor.setAwaitTerminationSeconds(30);
        
        // Reject policy: run in caller thread if queue is full
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        executor.initialize();
        
        log.info("Email executor configured: coreSize=5, maxSize=20, queueCapacity=100");
        return executor;
    }
    
    /**
     * Configure thread pool for email retry tasks
     * Separate from main email executor for better resource management
     */
    @Bean(name = "emailRetryExecutor")
    public Executor emailRetryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Smaller thread pool for retry tasks
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        
        executor.setThreadNamePrefix("email-retry-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        
        executor.initialize();
        
        log.info("Email retry executor configured: coreSize=2, maxSize=5, queueCapacity=50");
        return executor;
    }
}
