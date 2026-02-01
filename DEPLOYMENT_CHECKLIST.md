# Email Reliability System - Deployment Checklist

## Pre-Deployment Verification

### Code Review
- [ ] Review EmailService changes in `service/EmailService.java`
- [ ] Review AsyncConfig in `config/AsyncConfig.java`
- [ ] Review EmailEvent entity in `entity/EmailEvent.java`
- [ ] Review EmailRetryScheduler in `service/EmailRetryScheduler.java`
- [ ] Verify PerfumeShopApplication has @EnableScheduling

### Build Verification
- [ ] Run `mvn clean compile` - should succeed with 0 errors
- [ ] Run `mvn clean package` - should succeed
- [ ] No compilation warnings related to email components

### Database Verification
- [ ] Verify MySQL is running and accessible
- [ ] Verify database `perfume_shop` exists
- [ ] Verify JPA will auto-create email_events table (ddl-auto: update)
- [ ] Check if existing migrations need email_events reference

## Configuration Setup

### Environment Variables (REQUIRED)
- [ ] Set `MAIL_USERNAME` to Gmail email address
- [ ] Set `MAIL_PASSWORD` to Gmail app-specific password (not account password)
- [ ] Both variables accessible to Spring application
- [ ] Variables configured in:
  - [ ] IDE run configuration
  - [ ] Docker/container environment
  - [ ] Production server environment

### Gmail Configuration (if using Gmail)
- [ ] Gmail account has 2-factor authentication enabled
- [ ] App-specific password generated from Google Account
- [ ] 16-character app password used (not 30-character)
- [ ] Password copied correctly without spaces/newlines

### application.yml
- [ ] Verify `app.email.max-retries: 3` is present
- [ ] Verify SMTP settings:
  ```yaml
  spring:
    mail:
      host: smtp.gmail.com
      port: 587
      username: ${MAIL_USERNAME}
      password: ${MAIL_PASSWORD}
  ```
- [ ] Verify properties file is being loaded

## Pre-Production Testing

### Unit Testing
- [ ] Run `mvn test` - all email-related tests pass
- [ ] Test EmailService independently
- [ ] Test EmailEvent entity creation
- [ ] Test EmailEventRepository queries

### Integration Testing
- [ ] Full application startup without errors
- [ ] Check logs for:
  - [ ] AsyncConfig initialization messages
  - [ ] @EnableScheduling activation
  - [ ] "Email executor configured" log message
  - [ ] "Email retry executor configured" log message

### Manual Email Test
- [ ] Create test order through API
- [ ] Verify confirmation email received within 5 seconds
- [ ] Check email_events table:
  ```sql
  SELECT COUNT(*) FROM email_events WHERE status = 'SENT';
  ```
  Should show 1 record

### Failure Scenario Test
- [ ] Temporarily change MAIL_PASSWORD to invalid value
- [ ] Create new test order
- [ ] Verify email_events shows:
  - [ ] status = PENDING
  - [ ] attemptCount = 1
  - [ ] nextRetryAt is set (5 minutes in future)
  - [ ] lastError captures error message
- [ ] Restore correct MAIL_PASSWORD
- [ ] Wait 5 minutes or manually trigger scheduler
- [ ] Verify retry succeeds and status = SENT

## Deployment Steps

### 1. Code Deployment
```bash
# Pull latest code with email system
git pull origin main

# Verify files are present
ls src/main/java/com/perfume/shop/config/AsyncConfig.java
ls src/main/java/com/perfume/shop/entity/EmailEvent.java
ls src/main/java/com/perfume/shop/repository/EmailEventRepository.java
ls src/main/java/com/perfume/shop/service/EmailRetryScheduler.java
```

### 2. Build Application
```bash
mvn clean package -DskipTests
```

### 3. Set Environment Variables
```bash
# On Linux/Mac
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-specific-password"

# On Windows PowerShell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-app-specific-password"

# In Docker
# Add to docker-compose.yml:
# environment:
#   - MAIL_USERNAME=your-email@gmail.com
#   - MAIL_PASSWORD=your-app-specific-password
```

### 4. Start Application
```bash
# Via Maven
mvn spring-boot:run

# Via JAR
java -jar target/perfume-shop-*.jar

# Via Docker
docker run -e MAIL_USERNAME=... -e MAIL_PASSWORD=... perfume-shop:latest
```

### 5. Verify Startup
Check logs for:
```
INFO: Email executor configured: coreSize=5, maxSize=20, queueCapacity=100
INFO: Email retry executor configured: coreSize=2, maxSize=5, queueCapacity=50
```

### 6. Health Check
- [ ] Application starts without errors
- [ ] All endpoints respond normally
- [ ] Database connection successful
- [ ] email_events table created (check via MySQL client)

## Post-Deployment Validation

### Immediate (First Hour)
- [ ] Monitor application logs for errors
- [ ] Check for any exceptions in email components
- [ ] Create test order and verify email sends
- [ ] Check email_events table has records
- [ ] Verify CPU and memory usage normal

### Short Term (First Day)
- [ ] Monitor email_events table growth
  ```sql
  SELECT email_type, COUNT(*) FROM email_events 
  GROUP BY email_type;
  ```
- [ ] Check for any FAILED status emails
- [ ] Verify scheduler is running (check for retry logs)
- [ ] Monitor SMTP quota usage (if applicable)

### Ongoing (First Week)
- [ ] Daily email statistics:
  ```sql
  SELECT 
    DATE(created_at) as date,
    COUNT(*) as total,
    SUM(CASE WHEN status='SENT' THEN 1 ELSE 0 END) as sent,
    SUM(CASE WHEN status='FAILED' THEN 1 ELSE 0 END) as failed
  FROM email_events
  GROUP BY DATE(created_at);
  ```
- [ ] Check for accumulation of PENDING emails
- [ ] Verify no emails stuck (stuck = PENDING and nextRetryAt in past)
- [ ] Review any error messages in lastError column

## Monitoring Setup

### Database Monitoring
```sql
-- Monitor email health (run daily)
SELECT 
  email_type,
  status,
  COUNT(*) as count,
  MAX(created_at) as latest
FROM email_events
GROUP BY email_type, status;

-- Check for stuck emails
SELECT * FROM email_events
WHERE status = 'PENDING' 
AND nextRetryAt < NOW() - INTERVAL 10 MINUTE;
```

### Application Logs
Configure log monitoring for:
- `ERROR` level messages from EmailService
- `WARN` level messages for retry attempts
- Check log aggregation tool (e.g., ELK, Splunk, CloudWatch)

### Alerts to Set Up
- [ ] Alert if FAILED email count > 10 in last hour
- [ ] Alert if PENDING email count > 100 (possible stuck emails)
- [ ] Alert if EmailRetryScheduler doesn't run for 15 minutes
- [ ] Alert if email_events table grows > 1M rows

## Rollback Plan

### If Critical Issue Detected
1. **Stop new email sends:**
   ```java
   // Disable in EmailService.java temporarily
   // OR set max-retries to 0 to prevent new sends
   ```

2. **Review email_events table:**
   ```sql
   SELECT * FROM email_events WHERE status = 'FAILED'
   LIMIT 10;
   ```

3. **Check application logs** for error patterns

4. **Options:**
   - Fix configuration (e.g., wrong password)
   - Disable scheduler: Remove @EnableScheduling
   - Revert code to previous version
   - Restore from backup

### Revert Procedure
```bash
# If code is the issue
git revert <commit-hash>
mvn clean package
# Restart application

# OR restore database
mysqldump perfume_shop > backup_$(date +%s).sql
# Apply previous backup
mysql perfume_shop < previous_backup.sql
```

## Documentation Handoff

### Provide to Team
- [ ] EMAIL_RELIABILITY.md - Complete technical docs
- [ ] EMAIL_RELIABILITY_SETUP.md - Quick start guide
- [ ] EMAIL_INTEGRATION_EXAMPLES.md - Code examples
- [ ] EMAIL_IMPLEMENTATION_SUMMARY.md - Summary of changes
- [ ] This checklist for future deployments

### Training
- [ ] Show team email_events table
- [ ] Demonstrate monitoring queries
- [ ] Walk through retry flow
- [ ] Show how to manually retry failed email
- [ ] Demonstrate troubleshooting approach

## Post-Deployment Documentation

### Create Runbook
Document standard operations:
- How to check email health
- How to manually retry failed email
- How to troubleshoot stuck emails
- Who to contact for SMTP issues
- How to adjust max-retries if needed

### Update System Architecture Diagram
- [ ] Add AsyncConfig to architecture
- [ ] Add EmailEvent to data model
- [ ] Add EmailRetryScheduler to background tasks

### Update README
- [ ] Add email configuration section
- [ ] Add environment variables section
- [ ] Add troubleshooting link

## Success Criteria

Application is successfully deployed when:
- [ ] No errors in application logs
- [ ] All email endpoints respond normally
- [ ] Test emails are received within 5 seconds
- [ ] email_events table contains records
- [ ] No FAILED status emails accumulating
- [ ] EmailRetryScheduler logs show normal execution
- [ ] Database performance unchanged
- [ ] All team members understand the system

## Support Contacts

Document contact information for:
- [ ] Java/Spring application owner
- [ ] Database administrator
- [ ] Email/SMTP provider support
- [ ] DevOps/Infrastructure team
- [ ] On-call engineer

---

## Notes for Next Deployment

_Add any deployment-specific notes here for next time_

1. 
2. 
3. 

---

**Deployment Date**: _______________
**Deployed By**: _______________
**Verified By**: _______________
**Sign-Off**: _______________
