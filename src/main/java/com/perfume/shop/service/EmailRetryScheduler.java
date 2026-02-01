package com.perfume.shop.service;

import com.perfume.shop.entity.EmailEvent;
import com.perfume.shop.repository.EmailEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for retrying failed emails.
 * 
 * Runs every 5 minutes to:
 * - Find emails ready for retry (nextRetryAt <= now)
 * - Attempt to resend them
 * - Update status based on result
 * - Use exponential backoff between retries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailRetryScheduler {
    
    private final EmailEventRepository emailEventRepository;
    private final EmailService emailService;
    
    /**
     * Retry emails that are pending and ready for retry.
     * Runs every 5 minutes (300000 milliseconds).
     * Initial delay of 1 minute before first run.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void retryFailedEmails() {
        log.debug("Email retry scheduler started");
        
        try {
            // Find all emails that are ready for retry
            List<EmailEvent> emailsToRetry = emailEventRepository.findPendingEmailsForRetry();
            
            if (emailsToRetry.isEmpty()) {
                log.debug("No emails ready for retry");
                return;
            }
            
            log.info("Found {} emails ready for retry", emailsToRetry.size());
            
            // Attempt to resend each email
            for (EmailEvent emailEvent : emailsToRetry) {
                try {
                    log.debug("Retrying email: id={}, orderId={}, type={}, attempt={}/{}", 
                            emailEvent.getId(),
                            emailEvent.getOrder().getId(),
                            emailEvent.getEmailType(),
                            emailEvent.getAttemptCount(),
                            emailEvent.getMaxRetries());
                    
                    // Call email service to retry
                    emailService.retryFailedEmail(emailEvent);
                    
                } catch (Exception e) {
                    log.error("Error during email retry process: id={}, orderId={}", 
                            emailEvent.getId(), 
                            emailEvent.getOrder().getId(), 
                            e);
                }
            }
            
            log.debug("Email retry scheduler completed");
            
        } catch (Exception e) {
            log.error("Fatal error in email retry scheduler", e);
        }
    }
    
    /**
     * Cleanup task to mark permanently failed emails.
     * Runs daily at 2 AM.
     * Marks emails as FAILED if they've exceeded max retries.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupFailedEmails() {
        log.info("Email cleanup scheduler started");
        
        try {
            // This is handled automatically by the EmailService
            // when it detects attemptCount >= maxRetries
            
            // You could add additional cleanup logic here:
            // - Archive old email events
            // - Send alerts for permanently failed emails
            // - Generate reports
            
            log.info("Email cleanup scheduler completed");
            
        } catch (Exception e) {
            log.error("Fatal error in email cleanup scheduler", e);
        }
    }
    
    /**
     * Health check - returns count of pending emails.
     * Useful for monitoring.
     */
    public long getPendingEmailCount() {
        List<EmailEvent> pending = emailEventRepository.findPendingEmailsForRetry();
        return pending.size();
    }
}
