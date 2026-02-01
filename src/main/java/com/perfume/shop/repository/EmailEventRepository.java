package com.perfume.shop.repository;

import com.perfume.shop.entity.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {
    
    /**
     * Find all emails ready for retry (time has come and not exceeded max retries)
     */
    @Query("""
        SELECT e FROM EmailEvent e 
        WHERE e.status = 'PENDING' 
        AND e.attemptCount < e.maxRetries 
        AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= CURRENT_TIMESTAMP)
        ORDER BY e.nextRetryAt ASC NULLS FIRST
    """)
    List<EmailEvent> findPendingEmailsForRetry();
    
    /**
     * Find emails by order ID and type
     */
    List<EmailEvent> findByOrderIdAndEmailType(Long orderId, String emailType);
    
    /**
     * Find failed emails
     */
    @Query("""
        SELECT e FROM EmailEvent e 
        WHERE e.status = 'FAILED' 
        ORDER BY e.createdAt DESC
    """)
    List<EmailEvent> findFailedEmails();
    
    /**
     * Count pending emails for an order
     */
    long countByOrderIdAndStatus(Long orderId, EmailEvent.EmailStatus status);
    
    /**
     * Find by order ID
     */
    List<EmailEvent> findByOrderId(Long orderId);
}
