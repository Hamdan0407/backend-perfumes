# PRODUCTION MIGRATION GUIDE - Complete Walkthrough

## Overview
This document guides you through migrating the Perfume Shop from demo/dev to **PRODUCTION-GRADE** software.

### Key Changes:
- ✅ Redis integration (mandatory for chatbot state & caching)
- ✅ Database: PostgreSQL/MySQL only (H2 removed)
- ✅ Production payment processing (real Razorpay keys)
- ✅ Real SMTP email (no console logging)
- ✅ Production security (no hardcoded secrets, no demo mode)
- ✅ Production monitoring (health checks, metrics)
- ✅ Docker with health checks and resource limits

---

## PHASE 1: Prerequisites & Preparation

### 1.1 Environment Setup
```bash
# 1. Ensure you have these tools installed
- Docker & Docker Compose 2.0+
- Maven 3.8.1+
- Java 17+
- PostgreSQL client (psql) or MySQL client (mysql)

# 2. Create production environment file
cp .env.production.example .env.production

# 3. Edit with your actual secrets
nano .env.production
```

### 1.2 Generate Required Secrets
```bash
# Generate JWT Secret (256+ bits)
openssl rand -base64 32

# Example output (use this in .env.production):
# ZrF7kX3mN9pQ5tL2sW8vC6bY7dE4fG1hJ5kM8nO2pR9sT5uV

# Verify length is 43+ characters (256+ bits in base64)
```

### 1.3 Secure Files
```bash
# Ensure .env.production is not tracked by git
echo ".env.production" >> .gitignore

# Set restrictive permissions
chmod 600 .env.production
chmod 600 .env.production.example

# Verify
ls -la .env.production
# Should show: -rw------- (600)
```

---

## PHASE 2: Database Setup (Production)

### 2.1 Choose Database
```bash
# Option A: PostgreSQL (Recommended for cloud)
# DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/perfume_shop
# DATABASE_USERNAME=prod_user
# DATABASE_PASSWORD=secure_password

# Option B: MySQL (Cloud or On-Premise)
# DATABASE_URL=jdbc:mysql://prod-db.example.com:3306/perfume_shop?useSSL=true
# DATABASE_USERNAME=prod_user
# DATABASE_PASSWORD=secure_password
```

### 2.2 Database Configuration Checklist
```
[ ] Database service is accessible from Docker network
[ ] Connection string is correct
[ ] Credentials are set in .env.production
[ ] Database character set: utf8mb4 (for emojis/special chars)
[ ] Max connections: 20+ for connection pool
[ ] Backups configured (critical!)
[ ] SSL/TLS enabled for remote connections
[ ] Network access restricted to app only
```

### 2.3 Initialize Database Schema
```bash
# If using Docker Compose:
docker-compose up database
# Wait for "ready for connections"

# Manually run init script:
mysql -h localhost -u root -p perfume_shop < init.sql

# Verify:
mysql -u prod_user -p perfume_shop
> SELECT COUNT(*) FROM products;
> EXIT;
```

---

## PHASE 3: Redis Setup (Production)

### 3.1 Redis Configuration
```
REDIS_HOST=redis  # Docker service name, or actual host
REDIS_PORT=6379
REDIS_PASSWORD=your_secure_redis_password
REDIS_DATABASE=0
```

### 3.2 Redis Security
```bash
# In docker-compose.yml:
# --requirepass ensures password authentication

# Connect to verify:
redis-cli -a your_secure_redis_password ping
# Expected: PONG

# Check memory and keys:
redis-cli -a your_secure_redis_password INFO memory
redis-cli -a your_secure_redis_password DBSIZE
```

### 3.3 Redis Persistence
```bash
# appendonly yes - writes all commands to disk
# Enables recovery after crash/restart
# Located at: redis-data volume

# Monitor in Docker:
docker exec perfume-shop-redis redis-cli INFO persistence
```

---

## PHASE 4: Security Configuration

### 4.1 JWT Secret
```bash
# CRITICAL: Must be unique, 256+ bits
# Change from demo value

# Generate new secret:
openssl rand -base64 32

# Add to .env.production:
JWT_SECRET=YOUR_GENERATED_SECRET

# Verify it's set:
grep JWT_SECRET .env.production | head -1
```

### 4.2 CORS Origins (Prevent CSRF)
```
CORS_ORIGINS=https://yourdomain.com

# Multiple origins (comma-separated):
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com,https://app.yourdomain.com

# DO NOT use:
CORS_ORIGINS=* (SECURITY RISK - allows any domain)
CORS_ORIGINS=http://localhost:3000 (dev/demo only)
```

### 4.3 Admin User Creation
```bash
# DO NOT use default demo credentials in production
# Admin users must be created securely

# Option 1: Use CLI tool (recommended)
docker exec perfume-shop-api java -cp /app.jar \
  com.perfume.shop.cli.AdminCreator \
  --email admin@yourdomain.com \
  --password YOUR_SECURE_PASSWORD

# Option 2: Manual database insert (if CLI not available)
mysql -u prod_user -p perfume_shop << EOF
INSERT INTO users (email, password, first_name, last_name, role, active, created_at)
VALUES (
  'admin@yourdomain.com',
  '\$2a\$12\$HASHED_PASSWORD_HERE',  # Use bcrypt hash
  'Admin',
  'User',
  'ADMIN',
  true,
  NOW()
);
EOF
```

---

## PHASE 5: Payment Gateway (Razorpay)

### 5.1 Razorpay Live Keys (CRITICAL)
```
# ❌ DO NOT USE TEST KEYS IN PRODUCTION
# ✅ USE LIVE KEYS ONLY (rzp_live_...)

# Get keys from: https://dashboard.razorpay.com/app/keys

RAZORPAY_KEY_ID=rzp_live_YOUR_LIVE_KEY_ID
RAZORPAY_KEY_SECRET=YOUR_LIVE_SECRET
RAZORPAY_WEBHOOK_SECRET=YOUR_WEBHOOK_SECRET
```

### 5.2 Webhook Configuration
```
1. Go to Razorpay Dashboard > Settings > Webhooks
2. Add webhook endpoint: https://yourdomain.com/api/webhooks/razorpay
3. Select events:
   - payment.authorized
   - payment.failed
   - order.paid
   - order.cancelled
4. Copy webhook secret and set in .env.production
5. Test webhook: Razorpay provides test button
```

### 5.3 Testing Live Payments
```bash
# Test with small amount (₹1)
# Use Razorpay test card:
Card Number: 4111 1111 1111 1111
Expiry: 12/25
CVV: 123

# Monitor in logs:
docker logs perfume-shop-api | grep -i razorpay

# Verify in Razorpay Dashboard > Payments
# Should show as "captured" after successful payment
```

### 5.4 Signature Verification
```
The app validates all Razorpay signatures using HMAC-SHA256.
If signature verification fails, payment is rejected.

Signature is calculated as:
HMAC-SHA256(order_id|payment_id, webhook_secret)

This prevents:
- Tampering with payment data
- Replay attacks
- Man-in-the-middle attacks
```

---

## PHASE 6: Email Configuration (Production SMTP)

### 6.1 Email Service Setup
```bash
# Option A: Gmail with App Password (recommended for small deployments)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=YOUR_APP_PASSWORD  # NOT your Gmail password!

# To generate Gmail App Password:
# 1. Enable 2FA on your Google Account
# 2. Go to https://myaccount.google.com/app-passwords
# 3. Select "Mail" and "Windows Computer"
# 4. Copy the 16-character password

# Option B: AWS SES (for high volume)
# MAIL_HOST=email-smtp.region.amazonaws.com
# MAIL_PORT=587
# MAIL_USERNAME=your-ses-username
# MAIL_PASSWORD=your-ses-password

# Option C: SendGrid, Mailgun, or corporate SMTP
# Contact your email service provider for configuration
```

### 6.2 Email Verification
```bash
# Test email sending:
curl -X POST http://localhost:8080/api/email/test \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","subject":"Test","body":"Test email"}'

# Check logs for success:
docker logs perfume-shop-api | grep -i "sent email"

# Important: Emails are sent asynchronously
# Check database for email events if stuck:
SELECT * FROM email_events WHERE status='PENDING' ORDER BY created_at DESC LIMIT 5;
```

### 6.3 Email Features
- ✅ Order confirmation emails
- ✅ Order status updates
- ✅ Shipping notifications
- ✅ Password reset emails
- ✅ Retry logic (3 attempts with exponential backoff)
- ✅ Email event tracking for audit

---

## PHASE 7: Chatbot & Redis Integration

### 7.1 Chatbot Session Storage
```
The chatbot now uses Redis for conversation state management:

Before (Demo): In-memory map → Lost when app restarts
After (Production): Redis → Persisted, scalable

Each user gets:
- Unique session ID
- Conversation history stored in Redis
- 24-hour automatic expiration
- Real-time availability lookups
```

### 7.2 Testing Chatbot
```bash
# 1. Start the app:
docker-compose up -d

# 2. Open chatbot interface:
# http://localhost:3000/chatbot

# 3. Test intent detection:
# "What's the price of Gucci Bloom?"
# Expected: Real product price + stock from database

# 4. Verify Redis storage:
redis-cli -a your_redis_password
> KEYS "chatbot:*"
> GET "chatbot:session:user123"
> EXIT

# 5. Check logs:
docker logs perfume-shop-api | grep -i "chatbot\|redis"
```

### 7.3 Conversation Persistence
```sql
-- View stored conversations:
SELECT 
  conversation_id,
  user_id,
  user_message,
  bot_response,
  user_intent,
  created_at
FROM conversation_history
ORDER BY created_at DESC
LIMIT 20;
```

---

## PHASE 8: Docker Deployment

### 8.1 Build Docker Images
```bash
# Create .env.production with all secrets

# Build images:
docker-compose build

# Expected output:
# Successfully built perfume-shop-api (image hash)
# Successfully built perfume-shop-db (image hash)
# Successfully built perfume-shop-redis (image hash)
```

### 8.2 Start Services
```bash
# Start all services with proper dependency management
docker-compose up -d

# Verify all services are running:
docker-compose ps

# Expected output:
# perfume-shop-api     │ running  │ healthy
# perfume-shop-db      │ running  │ healthy
# perfume-shop-redis   │ running  │ healthy
```

### 8.3 Health Checks
```bash
# Check API health:
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"},"redis":{"status":"UP"}}}

# Check individual services:
curl http://localhost:8080/actuator/health/db
curl http://localhost:8080/actuator/health/redis
curl http://localhost:8080/actuator/health/ping
```

### 8.4 Logs Monitoring
```bash
# View API logs (last 100 lines):
docker logs perfume-shop-api | tail -100

# Follow logs in real-time:
docker logs -f perfume-shop-api

# View database logs:
docker logs perfume-shop-db

# View Redis logs:
docker logs perfume-shop-redis

# Search for errors:
docker logs perfume-shop-api 2>&1 | grep -i "error\|exception"
```

---

## PHASE 9: Verification Checklist

### 9.1 API Endpoints
```bash
# 1. Health check
curl http://localhost:8080/actuator/health

# 2. List products
curl http://localhost:8080/api/products

# 3. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@yourdomain.com","password":"your_password"}'

# 4. Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productId":1,"quantity":1}]}'

# 5. Chatbot
curl -X POST http://localhost:8080/api/chatbot/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What is the price of Gucci Bloom?","userId":"test-user"}'
```

### 9.2 Database Verification
```bash
# Connect to database:
mysql -u prod_user -p perfume_shop

# Check data:
SELECT COUNT(*) as total_products FROM products;
SELECT COUNT(*) as total_users FROM users WHERE role='ADMIN';
SELECT COUNT(*) as total_orders FROM orders;
SELECT COUNT(*) as total_conversations FROM conversation_history;

# Check Redis integration:
SELECT * FROM conversation_history LIMIT 5;

# EXIT
```

### 9.3 Redis Verification
```bash
redis-cli -a your_redis_password

# Check databases
DBSIZE  # Should show number of keys

# Check chatbot sessions
KEYS "chatbot:session:*"

# Check product cache
KEYS "perfume:products:*"

# Monitor operations
MONITOR  # Shows all commands in real-time (Ctrl+C to stop)

# Check memory
INFO memory  # Shows used_memory, max_memory, etc.

# EXIT
```

### 9.4 Email Verification
```bash
# Check email configuration
curl -X POST http://localhost:8080/api/admin/test-email \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"recipient":"test@example.com"}'

# Check database for email events
mysql -u prod_user -p perfume_shop
SELECT * FROM email_events ORDER BY created_at DESC LIMIT 5;

# Verify Gmail received test email (check Inbox + Spam)
```

---

## PHASE 10: Monitoring & Maintenance

### 10.1 Real-time Monitoring
```bash
# CPU & Memory usage
docker stats perfume-shop-api

# Network traffic
docker stats --no-stream

# Process details
docker top perfume-shop-api

# Container events
docker events --filter "container=perfume-shop-api"
```

### 10.2 Database Backups
```bash
# Backup MySQL
docker exec perfume-shop-db mysqldump \
  -u prod_user -p$(grep DATABASE_PASSWORD .env.production | cut -d= -f2) \
  perfume_shop > backup-$(date +%Y%m%d-%H%M%S).sql

# Test restore
mysql -u prod_user -p perfume_shop < backup-20240101-120000.sql

# Automate with cron:
0 2 * * * docker exec perfume-shop-db mysqldump -u prod_user -p$PASSWORD perfume_shop > /backups/perfume-shop-$(date +\%Y\%m\%d-\%H\%M\%S).sql

# Backup Redis (copy data volume)
docker exec perfume-shop-redis redis-cli -a your_password BGSAVE
docker cp perfume-shop-redis:/data/dump.rdb ./redis-backup-$(date +%Y%m%d-%H%M%S).rdb
```

### 10.3 Log Rotation
```bash
# Configure Docker log rotation in docker-compose.yml:
# logging:
#   driver: "json-file"
#   options:
#     max-size: "100m"
#     max-file: "10"

# Verify:
docker inspect --format='{{json .HostConfig.LogConfig}}' perfume-shop-api | jq
```

### 10.4 Alerts & Monitoring
```
Set up monitoring for:
- API response times (target: <500ms)
- Error rates (target: <1%)
- Database connection pool usage (target: <80%)
- Redis memory usage (target: <80%)
- Disk space (alert: <20% free)
- CPU usage (alert: >80%)
- Memory usage (alert: >90%)

Recommended tools:
- Prometheus + Grafana (open source)
- AWS CloudWatch (if using AWS)
- New Relic (SaaS option)
- Datadog (comprehensive monitoring)
```

---

## PHASE 11: Troubleshooting

### Issue: "Connection refused" on port 8080
```bash
# Check if service started
docker-compose ps

# Check logs
docker logs perfume-shop-api | tail -50

# If JVM error, check heap size
docker exec perfume-shop-api java -version

# Increase heap if needed:
# Update JAVA_OPTS in docker-compose.yml:
# JAVA_OPTS: "-Xmx2g -Xms1g"
```

### Issue: "Redis connection failed"
```bash
# Verify Redis is running
docker-compose ps | grep redis

# Check Redis logs
docker logs perfume-shop-redis

# Test Redis connection
redis-cli -h redis -a your_password ping

# If fails, restart Redis:
docker-compose restart redis
docker-compose up -d api
```

### Issue: "Database connection failed"
```bash
# Check MySQL is running and healthy
docker-compose ps | grep database

# Test connection
docker exec perfume-shop-db mysql -u root -p -e "SELECT 1"

# Check credentials in .env.production
grep DATABASE_ .env.production

# Verify schema exists
docker exec perfume-shop-db mysql -u prod_user -p -e "SHOW DATABASES;"
```

### Issue: "Razorpay signature verification failed"
```bash
# Verify webhook secret is correct
grep RAZORPAY_WEBHOOK_SECRET .env.production

# Check if using LIVE keys (not test)
grep RAZORPAY_KEY_ID .env.production | grep -q "rzp_live_" && echo "Using LIVE keys" || echo "ERROR: Using test keys!"

# Restart API to reload secrets
docker-compose restart api

# Test payment again with small amount (₹1)
```

### Issue: "Emails not being sent"
```bash
# Check SMTP configuration
grep MAIL_ .env.production

# Verify credentials with telnet
docker run -it --rm alpine sh -c "apk add --no-cache curl && curl -v smtp://smtp.gmail.com:587"

# Check email queue in database
mysql -u prod_user -p perfume_shop
SELECT * FROM email_events WHERE status='FAILED' ORDER BY created_at DESC LIMIT 10;

# If stuck, manually retry:
UPDATE email_events SET status='PENDING', retry_count=0 WHERE status='FAILED';
```

---

## PHASE 12: Post-Deployment

### 12.1 DNS & SSL/TLS
```bash
# Update DNS to point to your server
yourdomain.com A 1.2.3.4

# Install SSL certificate
# Option 1: Let's Encrypt (free)
# Option 2: Purchase from certificate authority

# Configure in reverse proxy (Nginx/HAProxy):
# - Redirect HTTP → HTTPS
# - Set HSTS headers
```

### 12.2 Frontend Deployment
```bash
# Update frontend .env with production API URL
REACT_APP_API_URL=https://yourdomain.com/api
VITE_API_URL=https://yourdomain.com/api

# Build frontend
cd frontend
npm run build

# Deploy to CDN or static host
# - Vercel
# - Netlify
# - AWS S3 + CloudFront
# - Your own web server
```

### 12.3 Performance Tuning
```bash
# Database query optimization
# Add indexes for frequently queried columns:
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_user_email ON users(email);

# Redis memory optimization
# Monitor memory usage:
redis-cli -a password INFO memory

# If memory grows, consider:
# - Adjusting TTLs for less frequently accessed data
# - Using Redis eviction policies (allkeys-lru)
```

### 12.4 Security Hardening
```bash
# 1. Update security headers in Spring Security Config
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Strict-Transport-Security: max-age=31536000

# 2. Enable HTTPS only
# REDIRECT HTTP to HTTPS in all responses

# 3. Disable unnecessary endpoints
# Remove /h2-console, /swagger-ui in production

# 4. Set up Web Application Firewall (WAF)
# AWS WAF or Cloudflare
```

---

## SUMMARY: What Changed from Demo to Production

| Aspect | Demo | Production |
|--------|------|-----------|
| Database | H2 (in-memory) | MySQL/PostgreSQL |
| Caching | In-memory map | Redis |
| Chatbot State | Lost on restart | Redis persistence |
| Secrets | Hardcoded | Environment variables |
| Admin | Default credentials | Secure creation |
| Payments | Demo mode accepted | LIVE keys only |
| Email | Console output | Real SMTP |
| Logs | DEBUG level | INFO level |
| Security | Permissive CORS | Restricted origins |
| Health Checks | Basic | Comprehensive |
| Monitoring | None | Metrics + alerts |

---

## Getting Help

If you encounter issues:
1. Check logs: `docker logs perfume-shop-api`
2. Review this guide for troubleshooting
3. Check environment variables: `grep -E "^[A-Z]" .env.production`
4. Test connectivity: `docker-compose up -d && docker-compose ps`
5. Contact support with:
   - Error message
   - Logs (last 50 lines)
   - Environment details (OS, Docker version)
   - Steps to reproduce
