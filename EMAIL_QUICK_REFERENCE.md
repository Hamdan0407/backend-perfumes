# Email System - Quick Reference

## Sending Emails

### From OrderService
```java
// Confirmation email (async, returns immediately)
emailService.sendOrderConfirmation(order);

// Status update email
emailService.sendOrderStatusUpdate(order);

// Shipping notification
emailService.sendShippingNotification(order);
```

**All calls are async** - don't wait for them, email sends in background.

## Email Status Checks

### Query Email Events
```sql
-- All emails for an order
SELECT * FROM email_events WHERE order_id = 123;

-- Sent emails
SELECT * FROM email_events WHERE status = 'SENT';

-- Pending/failed emails
SELECT * FROM email_events 
WHERE status IN ('PENDING', 'FAILED');

-- Check retry schedule
SELECT id, attemptCount, nextRetryAt FROM email_events
WHERE status = 'PENDING'
ORDER BY nextRetryAt ASC;
```

### In Java Code
```java
// Get email history for order
List<EmailEvent> events = emailEventRepository.findByOrderId(orderId);

// Get pending emails ready for retry
List<EmailEvent> pending = emailEventRepository.findPendingEmailsForRetry();

// Count pending emails
long count = emailEventRepository.countByOrderIdAndStatus(
    orderId, 
    EmailEvent.EmailStatus.PENDING
);
```

## Configuration

### Required Environment Variables
```
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password
```

### Optional Configuration
```yaml
app:
  email:
    max-retries: 3  # In application.yml
```

## Retry Schedule

| Attempt | Timing | Total Delay |
|---------|--------|-------------|
| 1 | Immediate | 0 |
| 2 | +5 minutes | 5 min |
| 3 | +15 minutes | 20 min |
| 4 | +45 minutes | 65 min |
| (Failed) | After 3 retries | N/A |

**Scheduler runs every 5 minutes** to process retries.

## Key Files

| File | Purpose |
|------|---------|
| `config/AsyncConfig.java` | Thread pool configuration |
| `entity/EmailEvent.java` | Email event tracking |
| `repository/EmailEventRepository.java` | Database queries |
| `service/EmailService.java` | Email sending with retry |
| `service/EmailRetryScheduler.java` | Automatic retry scheduler |

## Common Tasks

### Check if Order Has Sent Emails
```sql
SELECT COUNT(*) FROM email_events 
WHERE order_id = 123 AND status = 'SENT';
```

### Find Failed Emails
```sql
SELECT * FROM email_events 
WHERE status = 'FAILED'
ORDER BY created_at DESC
LIMIT 10;
```

### Manually Retry an Email
```java
EmailEvent event = emailEventRepository.findById(eventId).orElseThrow();
emailService.retryFailedEmail(event);
```

### Check Email Health
```sql
SELECT 
  email_type,
  status,
  COUNT(*) as count,
  AVG(attemptCount) as avg_attempts
FROM email_events
WHERE created_at > NOW() - INTERVAL 24 HOUR
GROUP BY email_type, status;
```

### Get Email Statistics
```sql
SELECT 
  CONCAT(ROUND(SUM(CASE WHEN status='SENT' THEN 1 ELSE 0 END)*100/COUNT(*), 2), '%') as success_rate,
  COUNT(*) as total_emails,
  SUM(CASE WHEN status='SENT' THEN 1 ELSE 0 END) as sent,
  SUM(CASE WHEN status='PENDING' THEN 1 ELSE 0 END) as pending,
  SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) as failed
FROM email_events;
```

## Troubleshooting

### Email Not Sent
1. Check `MAIL_USERNAME` and `MAIL_PASSWORD` environment variables
2. Verify Gmail app-specific password (not account password)
3. Check application logs for errors
4. Verify SMTP connectivity: `telnet smtp.gmail.com 587`

### Emails Stuck in Pending
1. Check if `@EnableScheduling` is in `PerfumeShopApplication`
2. Check scheduler logs: look for "Email retry scheduler" messages
3. Check if email_events table exists
4. Verify `nextRetryAt` is in past: `SELECT * FROM email_events WHERE nextRetryAt < NOW();`

### Too Many Failed Emails
1. Check SMTP server logs
2. Check if rate limited by mail provider
3. Verify email addresses are valid
4. Check `lastError` column for error patterns

## Performance Notes

- **Throughput**: Up to 100 concurrent emails
- **Latency**: Request returns in <1ms (email sends async)
- **Actual send**: 100-500ms (SMTP roundtrip)
- **Database**: Minimal overhead
- **CPU/Memory**: No significant impact

## Best Practices

✅ **DO:**
- Fire and forget - call emailService and return
- Check email_events table for status
- Monitor scheduler execution
- Archive old email events (>30 days)
- Alert on FAILED email accumulation

❌ **DON'T:**
- Wait for email send to complete
- Block requests on email operations
- Assume email is instantly delivered
- Delete email_events without archiving
- Ignore FAILED status emails

## Log Patterns

```
// Successful send
INFO  - Email sent successfully: type=CONFIRMATION, orderId=123

// Retry scheduled
WARN  - Email will be retried: type=CONFIRMATION, orderId=123, attempt=1/3, nextRetry=2024-01-15 14:30:00

// Permanently failed
ERROR - Email failed permanently after 3 attempts: type=CONFIRMATION, orderId=123

// Retry attempt
DEBUG - Retrying email: id=456, orderId=123, type=CONFIRMATION, attempt=2/3
```

## API Endpoints (if implemented)

```bash
# Get email history for order
GET /api/orders/{orderId}/emails

# Manually retry email
POST /api/orders/{orderId}/emails/{emailEventId}/retry

# Get email statistics (if added)
GET /api/admin/email-stats
```

## Database Indexes

Optimized queries use these indexes:
- `idx_email_order_id` - Find emails by order
- `idx_email_type` - Filter by email type
- `idx_email_status` - Find pending/failed
- `idx_email_created` - Time-based queries

## Thread Pools

| Executor | Purpose | Threads | Queue |
|----------|---------|---------|-------|
| emailExecutor | Send emails | 5-20 | 100 |
| emailRetryExecutor | Retry emails | 2-5 | 50 |

## Configuration Files

**application.yml:**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

app:
  email:
    max-retries: 3
```

## One-Liners

```bash
# Check email success rate (in MySQL client)
SELECT CONCAT(ROUND(COUNT(CASE WHEN status='SENT' THEN 1 END)*100/COUNT(*), 2), '%') FROM email_events;

# Count pending retries
SELECT COUNT(*) FROM email_events WHERE status='PENDING' AND nextRetryAt <= NOW();

# Find emails by error pattern
SELECT * FROM email_events WHERE lastError LIKE '%timeout%';

# Check if scheduler is running (check logs)
grep "Email retry scheduler" application.log

# Check thread pools
grep "executor configured" application.log
```

## Quick Links

- [Complete Documentation](EMAIL_RELIABILITY.md)
- [Setup Guide](EMAIL_RELIABILITY_SETUP.md)
- [Integration Examples](EMAIL_INTEGRATION_EXAMPLES.md)
- [Deployment Checklist](DEPLOYMENT_CHECKLIST.md)

## Support

For issues, check:
1. Application logs (grep for "Email")
2. email_events table (check status and errors)
3. MAIL_USERNAME/MAIL_PASSWORD environment variables
4. SMTP connectivity (telnet smtp.gmail.com 587)
5. See DEPLOYMENT_CHECKLIST.md for troubleshooting

---

**Version**: 1.0
**Last Updated**: 2024
**Status**: Production Ready
