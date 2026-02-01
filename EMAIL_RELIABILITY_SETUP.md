# Email Reliability Implementation - Setup Guide

## What Was Implemented

A complete, production-ready email reliability system with:

✅ **Async Email Sending** - Non-blocking async execution
✅ **Automatic Retries** - Exponential backoff (5min, 15min, 45min)
✅ **Email Event Tracking** - Database persistence of all attempts
✅ **Scheduled Retry Processing** - Automatic retry scheduler (every 5 minutes)
✅ **Thread Pool Management** - Separate executors for send and retry operations
✅ **Graceful Error Handling** - Comprehensive logging and error capture

## Files Created/Modified

### New Files Created
1. **src/main/java/com/perfume/shop/config/AsyncConfig.java**
   - Thread pool configuration for async execution
   - Two executors: emailExecutor and emailRetryExecutor

2. **src/main/java/com/perfume/shop/entity/EmailEvent.java**
   - Entity for tracking email delivery attempts
   - Fields for retry logic, timestamps, error messages

3. **src/main/java/com/perfume/shop/repository/EmailEventRepository.java**
   - Custom queries for finding pending and failed emails
   - Optimized indexes for retry processing

4. **src/main/java/com/perfume/shop/service/EmailRetryScheduler.java**
   - Scheduled service for automatic retry processing
   - Runs every 5 minutes to retry failed emails

5. **EMAIL_RELIABILITY.md**
   - Comprehensive documentation
   - Architecture explanation, usage examples, troubleshooting

### Files Modified
1. **src/main/java/com/perfume/shop/service/EmailService.java**
   - Enhanced with @Async("emailExecutor") for explicit executor selection
   - Added EmailEvent tracking and persistence
   - Added retry logic with exponential backoff
   - Added retryFailedEmail() method for scheduled retries
   - Improved error handling and logging

2. **src/main/java/com/perfume/shop/PerfumeShopApplication.java**
   - Added @EnableScheduling for scheduled task support

3. **src/main/resources/application.yml**
   - Added app.email.max-retries configuration

## Quick Start

### 1. Database Initialization
The `EmailEvent` table will be created automatically on first run (ddl-auto: update).

Tables created:
```
email_events (with indexes on order_id, emailType, status, created_at)
```

### 2. Environment Configuration
Ensure these environment variables are set:

```bash
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password  # Gmail app-specific password, not account password
```

For Gmail:
1. Enable 2-factor authentication
2. Create app-specific password in Gmail security settings
3. Use the app-specific password (not your account password)

### 3. Start the Application
```bash
mvn spring-boot:run
```

Or via IDE: Run `PerfumeShopApplication.java`

### 4. Test Email Sending
Create an order to trigger:
- `sendOrderConfirmation()` → sends async, tracked in email_events
- On failure → automatically scheduled for retry
- Retry attempts with exponential backoff

## How It Works

### Email Sending Flow
```
1. Order Created
   ↓
2. EmailService.sendOrderConfirmation() [async]
   ├─ Create EmailEvent (status=PENDING)
   ├─ Send email via SMTP
   └─ Update status=SENT if successful
                OR
   └─ Update status=PENDING, set nextRetryAt if failed
   ↓
3. EmailRetryScheduler (runs every 5 mins)
   ├─ Query pending emails where nextRetryAt <= now
   ├─ Call emailService.retryFailedEmail()
   └─ Repeat steps 2-3 until success or max retries reached
```

### Retry Timeline Example
```
00:00 - Order placed, confirmation email sent
00:00 - Send fails (SMTP timeout)
        EmailEvent created: status=PENDING, attemptCount=1, nextRetryAt=00:05
00:05 - Scheduler runs, retry sent
00:05 - Send fails again
        EmailEvent updated: attemptCount=2, nextRetryAt=00:20
00:20 - Scheduler runs, retry sent
00:20 - Send succeeds!
        EmailEvent updated: status=SENT, sentAt=00:20
```

## Configuration Options

### Retry Settings (application.yml)
```yaml
app:
  email:
    max-retries: 3  # Attempt 1 + 3 retries = 4 attempts total
```

### Thread Pool Sizes (AsyncConfig.java)
For high email volume, modify:
```java
emailExecutor.setCorePoolSize(10);      // Increase if needed
emailExecutor.setMaxPoolSize(30);
emailExecutor.setQueueCapacity(200);
```

### Retry Schedule (EmailRetryScheduler.java)
Modify retry interval (currently 5 minutes):
```java
@Scheduled(fixedDelay = 300000, initialDelay = 60000)  // 5 min, 1 min initial
```

## Monitoring

### Check Email Status
```sql
-- All pending emails
SELECT * FROM email_events WHERE status = 'PENDING';

-- Failed emails
SELECT * FROM email_events WHERE status = 'FAILED';

-- Email summary
SELECT email_type, status, COUNT(*) 
FROM email_events 
GROUP BY email_type, status;
```

### View Logs
```
INFO  - Email sent successfully: type=CONFIRMATION, orderId=123, to=user@example.com
WARN  - Email will be retried: type=CONFIRMATION, orderId=123, attempt=1/3, nextRetry=...
ERROR - Email failed permanently after 3 attempts: type=CONFIRMATION, orderId=123
```

## Verify Installation

1. **Check AsyncConfig is loaded:**
   ```
   INFO: Email executor configured: coreSize=5, maxSize=20, queueCapacity=100
   INFO: Email retry executor configured: coreSize=2, maxSize=5, queueCapacity=50
   ```

2. **Check EmailRetryScheduler is active:**
   Look for scheduled task output in logs

3. **Create a test order:**
   - Place order through API
   - Check email_events table: should have new PENDING record
   - Monitor logs for send attempt

## API Integration

### Frontend (React) - Checkout Process

The frontend doesn't need changes. When checkout completes:
1. Order created on backend
2. EmailService.sendOrderConfirmation() called automatically
3. Frontend receives success response immediately (email sent async)
4. User gets confirmation email within seconds

### Backend - Adding Email to New Scenarios

To send emails for other events:

```java
// In your service
@Autowired
private EmailService emailService;

// Send email (async, non-blocking)
emailService.sendOrderConfirmation(order);
emailService.sendOrderStatusUpdate(order);
emailService.sendShippingNotification(order);
```

## Troubleshooting

### Emails not being sent
1. Check MAIL_USERNAME/MAIL_PASSWORD environment variables
2. Verify Gmail app-specific password (not regular password)
3. Check application logs for MessagingException
4. Verify SMTP connectivity: `telnet smtp.gmail.com 587`

### Emails stuck in PENDING
1. Check if @EnableScheduling is present in PerfumeShopApplication
2. Check scheduler logs
3. Verify database has email_events table
4. Check if nextRetryAt < current time for pending emails

### High email_events table growth
1. This is normal - events are kept for audit trail
2. Implement archival: move old events to history table after 30 days
3. Or reduce max-retries if appropriate

### Email sending very slow
1. Check SMTP server performance
2. Increase thread pool size in AsyncConfig
3. Check if queue is full (maxPoolSize reached)

## Performance Tuning

### For High Email Volume (>1000/day)
```java
emailExecutor.setCorePoolSize(10);
emailExecutor.setMaxPoolSize(50);
emailExecutor.setQueueCapacity(500);
```

### For High Reliability (Low Failure Tolerance)
```yaml
app:
  email:
    max-retries: 5  # More retry attempts
```

### For Fast Retries (Don't wait long)
Modify exponential backoff in EmailService:
```java
// Current: 300 * 3^(attempt-1) seconds
// For faster: 60 * 2^(attempt-1) seconds  // 1 min, 2 min, 4 min
```

## Next Steps

1. **Set up email credentials** - Set MAIL_USERNAME and MAIL_PASSWORD
2. **Test email sending** - Create test order and verify email received
3. **Monitor logs** - Watch application startup logs for AsyncConfig messages
4. **Verify database** - Check email_events table after first email
5. **Set up monitoring** - Create dashboard for email_events metrics

## Support

For detailed information, see **EMAIL_RELIABILITY.md** in the root directory.

Contains:
- Complete architecture documentation
- Database schema details
- Code examples and usage patterns
- Advanced configuration options
- Testing strategies
- Future enhancement ideas
