# Email Reliability System Documentation

## Overview

The Perfume Shop application implements a **production-ready, reliable email system** with:

- **Async Execution**: Non-blocking email sending using Spring's `@Async` annotation
- **Automatic Retry Logic**: Exponential backoff (5min, 15min, 45min) for failed emails
- **Persistent Tracking**: Database records for all email delivery attempts
- **Transactional Consistency**: Ensures email events are tracked even if sending fails
- **Graceful Degradation**: Failed emails don't block order processing

## Architecture

### Components

#### 1. **AsyncConfig** (`config/AsyncConfig.java`)
Configures two separate thread pool executors for async operations:

**Email Executor** (`emailExecutor`)
- Core threads: 5
- Max threads: 20
- Queue capacity: 100
- Purpose: Primary email sending operations
- Thread name prefix: `email-task-`

**Email Retry Executor** (`emailRetryExecutor`)
- Core threads: 2
- Max threads: 5
- Queue capacity: 50
- Purpose: Scheduled retry operations
- Thread name prefix: `email-retry-task-`

**Features:**
- CallerRunsPolicy for queue overflow (prevents blocking)
- Graceful shutdown (waits 30 seconds for tasks)
- Debug-friendly thread naming

#### 2. **EmailEvent Entity** (`entity/EmailEvent.java`)
Tracks each email sending attempt with:

**Fields:**
- `id`: Unique identifier (auto-increment)
- `order`: Reference to Order (many-to-one)
- `emailType`: CONFIRMATION, STATUS_UPDATE, SHIPPING_NOTIFICATION
- `recipientEmail`: Target email address
- `status`: PENDING, SENT, FAILED, BOUNCED
- `attemptCount`: Number of send attempts
- `maxRetries`: Maximum retry attempts (default: 3)
- `lastError`: Last error message (1000 char limit)
- `nextRetryAt`: When to retry next (exponential backoff)
- `sentAt`: Timestamp when successfully sent
- `createdAt`: Auto-generated creation timestamp

**Indexes:**
- order_id (find emails by order)
- emailType (filter by type)
- status (find pending/failed)
- created_at (time-based queries)

**Helper Methods:**
```java
boolean canRetry() // Check if can attempt retry
boolean isReadyForRetry() // Check if time has come for retry
```

#### 3. **EmailEventRepository** (`repository/EmailEventRepository.java`)
Custom JPA repository for email event queries:

```java
List<EmailEvent> findPendingEmailsForRetry()
    // Find emails ready for retry (time passed, not max retries)

List<EmailEvent> findByOrderIdAndEmailType(Long orderId, String emailType)
    // Find specific email types for an order

List<EmailEvent> findFailedEmails()
    // Get permanently failed emails

long countByOrderIdAndStatus(Long orderId, EmailStatus status)
    // Count pending emails per order
```

#### 4. **EmailService** (`service/EmailService.java`)
Enhanced email sending service with retry tracking:

**Public Methods:**
```java
@Async("emailExecutor")
void sendOrderConfirmation(Order order)
    // Send order confirmation, track event, handle failures

@Async("emailExecutor")
void sendOrderStatusUpdate(Order order)
    // Send status update with retry tracking

@Async("emailExecutor")
void sendShippingNotification(Order order)
    // Send shipping notification with retry tracking

@Async("emailRetryExecutor")
void retryFailedEmail(EmailEvent emailEvent)
    // Retry a specific email event
```

**Flow:**

1. **Create Email Event**
   - Create EmailEvent record with status=PENDING
   - Save to database for tracking
   - Return immediately (async)

2. **Attempt Send**
   - Build HTML content
   - Send via JavaMailSender
   - Mark as SENT with timestamp on success

3. **Handle Failure**
   - Increment attemptCount
   - Capture error message
   - If attemptCount < maxRetries:
     - Calculate nextRetryAt (exponential backoff)
     - Keep status as PENDING
     - Schedule for retry
   - If attemptCount >= maxRetries:
     - Mark status as FAILED
     - Log permanently failed

4. **Exponential Backoff Calculation**
   ```
   delaySeconds = 300 * 3^(attemptCount-1)
   Attempt 1: 5 minutes
   Attempt 2: 15 minutes (5 * 3)
   Attempt 3: 45 minutes (15 * 3)
   ```

#### 5. **EmailRetryScheduler** (`service/EmailRetryScheduler.java`)
Scheduled service that automatically retries failed emails:

**Scheduled Tasks:**

```java
@Scheduled(fixedDelay = 300000, initialDelay = 60000)
void retryFailedEmails()
    // Runs every 5 minutes
    // Waits 1 minute before first run
    // Finds and retries all ready emails
```

**Example Timeline:**
```
00:00 - Order placed, confirmation email fails (error in SMTP)
00:01 - EmailRetryScheduler starts (initialDelay)
00:01 - Finds pending email, nextRetryAt = 00:05, retries
00:01 - Still fails, nextRetryAt = 00:20
00:05 - Scheduler runs again
00:05 - Email nextRetryAt is 00:20, skips
00:20 - Scheduler runs
00:20 - Email nextRetryAt is 00:20, retries
00:20 - Email sent successfully, status = SENT
```

## Configuration

### application.yml

```yaml
app:
  email:
    max-retries: 3  # Maximum retry attempts
```

### Environment Variables

Email credentials are configured via environment variables:

```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # Gmail app-specific password
```

### SMTP Settings (Gmail)

Pre-configured for Gmail SMTP:
- Host: smtp.gmail.com
- Port: 587
- Authentication: Enabled
- TLS: Enabled

To use other email providers, update `application.yml` mail properties.

## Usage

### Sending Emails

**Automatic (during order operations):**

Emails are automatically sent when:
- Order is confirmed → sendOrderConfirmation()
- Order status changes → sendOrderStatusUpdate()
- Order ships → sendShippingNotification()

**All calls are async** - they don't block the main request.

Example from OrderService:
```java
@Transactional
public Order createOrder(CheckoutRequest request, User user) {
    // ... create order ...
    
    // Send confirmation async (non-blocking)
    emailService.sendOrderConfirmation(order);
    
    // Return immediately
    return order;
}
```

### Email Event Tracking

**Query sent emails:**
```java
List<EmailEvent> events = emailEventRepository
    .findByOrderIdAndEmailType(orderId, "CONFIRMATION");
```

**Count pending emails:**
```java
long pending = emailEventRepository
    .countByOrderIdAndStatus(orderId, EmailEvent.EmailStatus.PENDING);
```

**Get permanently failed:**
```java
List<EmailEvent> failed = emailEventRepository.findFailedEmails();
```

## Failure Scenarios

### Transient Failures (Retryable)
- SMTP server timeout
- Network temporarily unavailable
- Server rate limiting
- Database briefly down

**Behavior:** Keep status PENDING, schedule retry with backoff

### Permanent Failures (Not Retried)
- Invalid email address format
- Email address doesn't exist
- Domain not found
- Authentication failure

**Behavior:** Could be enhanced to detect and mark as BOUNCED

## Monitoring

### Logging

All email operations are logged:

```
INFO - Email sent successfully: type=CONFIRMATION, orderId=123, to=user@example.com
WARN - Email will be retried: type=CONFIRMATION, orderId=123, attempt=1/3, nextRetry=2024-01-15 14:30:00
ERROR - Email failed permanently after 3 attempts: type=CONFIRMATION, orderId=123
```

### Database Queries

**Find all pending emails:**
```sql
SELECT * FROM email_events 
WHERE status = 'PENDING' 
AND attempt_count < max_retries 
AND next_retry_at <= NOW()
ORDER BY next_retry_at ASC;
```

**Failed emails by order:**
```sql
SELECT * FROM email_events 
WHERE order_id = 123 
AND status = 'FAILED';
```

**Email statistics:**
```sql
SELECT 
    email_type,
    status,
    COUNT(*) as count
FROM email_events
GROUP BY email_type, status;
```

## Performance Characteristics

### Throughput
- Primary executor can handle ~100 emails concurrently (5 core, 20 max, 100 queue)
- Retry executor handles ~5 concurrent retries
- Queue-based design prevents resource exhaustion

### Latency
- Email send request: < 1ms (async, returns immediately)
- Actual send in background: 100-500ms (SMTP roundtrip)
- Retry scheduling: Runs every 5 minutes

### Database
- Minimal overhead: Single INSERT on send attempt, single UPDATE on completion
- Indexes optimize retry queries
- Email events grow linearly with order volume

## Error Handling

### Recoverable Errors
- MessagingException with transient message
- Network timeouts
- SMTP temporary failures

→ Logged as WARN, scheduled for retry

### Non-Recoverable Errors
- Invalid configuration (wrong password)
- Invalid email format
- Domain not found

→ Logged as ERROR, marked FAILED after max retries

## Database Schema

```sql
CREATE TABLE email_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    email_type VARCHAR(50) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    last_error VARCHAR(1000),
    next_retry_at DATETIME,
    sent_at DATETIME,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_email_order_id (order_id),
    INDEX idx_email_type (email_type),
    INDEX idx_email_status (status),
    INDEX idx_email_created (created_at)
);
```

## Testing

### Unit Test Example

```java
@Test
void testOrderConfirmationEmailRetry() {
    // Create order
    Order order = createTestOrder();
    
    // Send email (will fail in test)
    emailService.sendOrderConfirmation(order);
    
    // Verify email event created
    List<EmailEvent> events = emailEventRepository
        .findByOrderIdAndEmailType(order.getId(), "CONFIRMATION");
    
    assertEquals(1, events.size());
    assertEquals(EmailEvent.EmailStatus.PENDING, events.get(0).getStatus());
    assertEquals(1, events.get(0).getAttemptCount());
    assertNotNull(events.get(0).getNextRetryAt());
}
```

### Integration Test Example

```java
@Test
@Transactional
void testEmailRetryScheduler() {
    // Create failed email event
    EmailEvent event = createTestEmailEvent();
    event.setStatus(EmailEvent.EmailStatus.PENDING);
    event.setNextRetryAt(LocalDateTime.now().minusMinutes(1));
    emailEventRepository.save(event);
    
    // Run scheduler
    emailRetryScheduler.retryFailedEmails();
    
    // Verify retry attempted
    EmailEvent updated = emailEventRepository.findById(event.getId()).orElseThrow();
    assertEquals(2, updated.getAttemptCount());
}
```

## Best Practices

1. **Don't block on email**: Always use async sends, never call synchronously
2. **Monitor failures**: Regularly check email_events table for FAILED status
3. **Alert on accumulation**: Alert if pending email count exceeds threshold
4. **Archive old events**: Archive email_events older than 30 days to manage table size
5. **Test failure scenarios**: Mock SMTP failures to verify retry logic
6. **Update templates**: Keep email HTML templates fresh and mobile-responsive
7. **Rate limiting**: Monitor mail server for rate limits, adjust backoff if needed

## Troubleshooting

### Emails not sending at all
1. Check application logs for errors
2. Verify MAIL_USERNAME and MAIL_PASSWORD environment variables
3. Ensure Gmail app-specific password is used (not account password)
4. Check if SMTP port 587 is accessible

### Emails stuck in PENDING
1. Check EmailRetryScheduler logs
2. Verify database has email_events table
3. Check if @EnableScheduling is present on main application
4. Verify mail server is accessible

### Frequent retries
1. Check SMTP server status
2. Verify network connectivity
3. Consider increasing RETRY_DELAY_SECONDS in EmailService
4. Check mail server logs for rate limiting

### High database growth
1. Implement email_events archival strategy
2. Reduce max-retries if appropriate
3. Analyze which email types are failing
4. Monitor disk space on database server

## Future Enhancements

- [ ] Dead letter queue for permanently failed emails
- [ ] Email template database (dynamic templates)
- [ ] SMS fallback for critical notifications
- [ ] Email bounce handling (BOUNCED status)
- [ ] Analytics dashboard for email metrics
- [ ] Support for email attachments
- [ ] Bulk email operations with rate limiting
- [ ] Email preference management (user can opt out)
