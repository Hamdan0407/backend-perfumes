# Environment Variables Reference

Complete documentation of all required and optional environment variables for the Perfume Shop application across all services (Backend, Frontend, MySQL, Payment Gateways, Email, JWT, etc.).

## Table of Contents

1. [Quick Reference](#quick-reference)
2. [Backend Configuration](#backend-configuration)
3. [Database Configuration](#database-configuration)
4. [JWT Configuration](#jwt-configuration)
5. [Security & CORS](#security--cors)
6. [Email Configuration](#email-configuration)
7. [Payment Gateway Configuration](#payment-gateway-configuration)
8. [Frontend Configuration](#frontend-configuration)
9. [Logging Configuration](#logging-configuration)
10. [Validation & Troubleshooting](#validation--troubleshooting)

---

## Quick Reference

### Environment Variables by Priority

**CRITICAL (Must Have)**:
- `JWT_SECRET` - JWT signing key (256+ bits)
- `DATABASE_USERNAME` - MySQL username
- `DATABASE_PASSWORD` - MySQL password
- `MAIL_USERNAME` - Email address for sending
- `MAIL_PASSWORD` - Email password/app token
- `RAZORPAY_KEY_ID` - Razorpay public key
- `RAZORPAY_KEY_SECRET` - Razorpay secret key

**IMPORTANT (Strongly Recommended)**:
- `CORS_ORIGINS` - Allowed frontend origins
- `RAZORPAY_WEBHOOK_SECRET` - Webhook verification
- `DATABASE_URL` - Database connection string
- `FRONTEND_URL` - Frontend application URL

**OPTIONAL (Defaults Available)**:
- `MAIL_HOST` - Email SMTP host
- `MAIL_PORT` - Email SMTP port
- `LOG_LEVEL` - Application log level
- `STRIPE_API_KEY` - Stripe payment key

---

## Backend Configuration

### Server Configuration

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | String | `prod` | ✅ | Spring profile to activate (prod, dev, test) |
| `PORT` | Integer | 8080 | ❌ | Spring Boot application port |
| `SERVER_PORT` | Integer | 8080 | ❌ | Tomcat server port |
| `ENVIRONMENT` | String | `production` | ❌ | Environment name (production, staging, development) |

**Example:**
```bash
export SPRING_PROFILES_ACTIVE=prod
export PORT=8080
export ENVIRONMENT=production
```

---

## Database Configuration

### MySQL Connection

| Variable | Type | Default | Required | Description | Format |
|----------|------|---------|----------|-------------|--------|
| `DATABASE_URL` | String | localhost | ✅ | MySQL JDBC connection string | `jdbc:mysql://host:port/database?useSSL=false&serverTimezone=UTC` |
| `DATABASE_USERNAME` | String | `root` | ✅ | MySQL username | alphanumeric, min 3 chars |
| `DATABASE_PASSWORD` | String | `root` | ✅ | MySQL password | min 8 chars, special chars recommended |
| `DATABASE_POOL_SIZE` | Integer | 20 | ❌ | Connection pool maximum size | 10-50 |
| `DATABASE_CONNECTION_TIMEOUT` | Integer | 10000 | ❌ | Connection timeout (ms) | 5000-30000 |
| `DATABASE_MAX_IDLE_TIME` | Integer | 300000 | ❌ | Max idle timeout (ms) | 60000-600000 |

### Local Development (MySQL on localhost)

```bash
export DATABASE_URL="jdbc:mysql://localhost:3306/perfume_shop?useSSL=false&serverTimezone=UTC"
export DATABASE_USERNAME="root"
export DATABASE_PASSWORD="root"
```

### Docker Compose (Internal Service)

```bash
# Using docker service name 'database' as hostname
export DATABASE_URL="jdbc:mysql://database:3306/perfume_shop?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export DATABASE_USERNAME="prod_user"
export DATABASE_PASSWORD="secure_password_min_8_chars"
```

### Production (Managed Database Service)

```bash
# AWS RDS Example
export DATABASE_URL="jdbc:mysql://perfume-db-prod.c9akciq32.us-east-1.rds.amazonaws.com:3306/perfume_shop?useSSL=true&serverTimezone=UTC"
export DATABASE_USERNAME="admin"
export DATABASE_PASSWORD="your-secure-password"
export DATABASE_POOL_SIZE=20

# Google Cloud SQL Example
export DATABASE_URL="jdbc:mysql://10.20.30.40:3306/perfume_shop?useSSL=true&serverTimezone=UTC"
export DATABASE_USERNAME="root"
export DATABASE_PASSWORD="your-secure-password"
```

### Connection Pool Sizing

```bash
# Development (5-10 concurrent users)
export DATABASE_POOL_SIZE=10

# Staging (50-100 concurrent users)
export DATABASE_POOL_SIZE=20

# Production (200+ concurrent users)
export DATABASE_POOL_SIZE=30
```

### Validation

```bash
# Test connection string locally
mysql -h $DATABASE_HOST -u $DATABASE_USERNAME -p$DATABASE_PASSWORD -e "SELECT 1;"

# From Docker
docker compose exec database mysql -u prod_user -p perfume_shop -e "SELECT 1;"
```

---

## JWT Configuration

### JWT Token Settings

| Variable | Type | Default | Required | Description | Example |
|----------|------|---------|----------|-------------|---------|
| `JWT_SECRET` | String | ❌ NONE | ✅ | Secret key for signing JWTs (256+ bits) | base64 encoded 32+ bytes |
| `JWT_EXPIRATION` | Integer | 86400000 | ❌ | Access token expiration (milliseconds) | 86400000 = 24 hours |
| `JWT_REFRESH_EXPIRATION` | Integer | 604800000 | ❌ | Refresh token expiration (ms) | 604800000 = 7 days |
| `JWT_GRACE_PERIOD` | Integer | 60000 | ❌ | Grace period for token refresh (ms) | 60000 = 1 minute |

### Generating JWT Secret

```bash
# Generate 256-bit (32 bytes) base64-encoded secret
openssl rand -base64 32
# Example output: aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0uV1wX2yZ3aBcD4eF5g=

# Alternative: Generate 512-bit for extra security
openssl rand -base64 64

# Another alternative: Using dd
dd if=/dev/urandom bs=1 count=32 2>/dev/null | base64

# Store in environment
export JWT_SECRET=$(openssl rand -base64 32)
```

### Token Expiration Times

```bash
# Development (short-lived for testing)
export JWT_EXPIRATION=3600000         # 1 hour
export JWT_REFRESH_EXPIRATION=86400000 # 24 hours

# Production (standard)
export JWT_EXPIRATION=86400000        # 24 hours
export JWT_REFRESH_EXPIRATION=604800000 # 7 days

# Production (high security)
export JWT_EXPIRATION=3600000         # 1 hour
export JWT_REFRESH_EXPIRATION=259200000 # 3 days
```

### Grace Period

```bash
# Time to refresh token before expiration without re-authentication
export JWT_GRACE_PERIOD=60000  # 1 minute before actual expiry
```

### Validation

```bash
# Verify JWT_SECRET is set and valid
if [ -z "$JWT_SECRET" ]; then
  echo "ERROR: JWT_SECRET not set"
  exit 1
fi

# Check length (should be at least 32 bytes base64 = 256 bits)
JWT_SECRET_LENGTH=$(echo -n "$JWT_SECRET" | wc -c)
if [ $JWT_SECRET_LENGTH -lt 44 ]; then
  echo "WARNING: JWT_SECRET might be too short ($JWT_SECRET_LENGTH chars)"
fi

# Test JWT functionality
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@","firstName":"Test","lastName":"User"}' | jq '.token' | head -c 50
```

---

## Security & CORS

### CORS Configuration

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `CORS_ORIGINS` | String | `http://localhost:3000,http://localhost:5173` | ✅ | Comma-separated allowed origins |
| `CORS_MAX_AGE` | Integer | 3600 | ❌ | CORS preflight cache duration (seconds) |

### CORS Origins Examples

```bash
# Development (localhost)
export CORS_ORIGINS="http://localhost:3000,http://localhost:5173"

# Production (single domain)
export CORS_ORIGINS="https://yourdomain.com"

# Production (with subdomains)
export CORS_ORIGINS="https://yourdomain.com,https://www.yourdomain.com,https://app.yourdomain.com"

# Production (with staging)
export CORS_ORIGINS="https://yourdomain.com,https://staging.yourdomain.com,https://app.yourdomain.com"

# Multiple domains (be restrictive!)
export CORS_ORIGINS="https://yourdomain.com,https://partner.com"
```

### Security Settings

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `PASSWORD_ENCODER_STRENGTH` | Integer | 12 | ❌ | BCrypt strength (10-12 recommended) |

```bash
# Password encoder strength (BCrypt rounds)
# 10 = ~10ms per hash (development)
# 12 = ~100ms per hash (production - recommended)
# 14 = ~1000ms per hash (high security, very slow)

export PASSWORD_ENCODER_STRENGTH=12
```

---

## Email Configuration

### SMTP Settings

| Variable | Type | Default | Required | Description | Format |
|----------|------|---------|----------|-------------|--------|
| `MAIL_HOST` | String | `smtp.gmail.com` | ❌ | SMTP server hostname | domain.com or IP |
| `MAIL_PORT` | Integer | 587 | ❌ | SMTP port | 25, 465, 587, 2525 |
| `MAIL_USERNAME` | String | ❌ NONE | ✅ | Email address/username | valid email |
| `MAIL_PASSWORD` | String | ❌ NONE | ✅ | Email password/token | app-specific password |
| `MAIL_FROM` | String | `noreply@perfume.com` | ❌ | Sender email address | valid email |
| `MAIL_FROM_NAME` | String | `Parfumé` | ❌ | Sender display name | max 50 chars |

### Email Retry Settings

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `EMAIL_MAX_RETRIES` | Integer | 3 | ❌ | Failed email retry attempts |
| `EMAIL_RETRY_DELAY` | Integer | 5000 | ❌ | Delay between retries (ms) |

### Gmail SMTP Configuration

```bash
# Gmail requires App Password (not regular password)
# Setup:
# 1. Enable 2-Factor Authentication: https://myaccount.google.com/security
# 2. Generate App Password: https://myaccount.google.com/apppasswords
# 3. Select: Mail and Windows Computer
# 4. Use the generated 16-char password

export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT=587
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="xxxx xxxx xxxx xxxx"  # 16 chars, generated from Google
export MAIL_FROM="your-email@gmail.com"
```

### SendGrid Configuration

```bash
# SendGrid: https://sendgrid.com
# 1. Create API key at https://app.sendgrid.com/settings/api_keys
# 2. Use 'apikey' as username and the API key as password

export MAIL_HOST="smtp.sendgrid.net"
export MAIL_PORT=587
export MAIL_USERNAME="apikey"
export MAIL_PASSWORD="SG.xxxxxxxxxxxxxxxxxxxxxxxx"
export MAIL_FROM="noreply@yourdomain.com"
```

### AWS SES Configuration

```bash
# AWS SES: https://aws.amazon.com/ses/
# 1. Verify email: https://console.aws.amazon.com/ses/
# 2. Get SMTP credentials from AWS SES console

export MAIL_HOST="email-smtp.us-east-1.amazonaws.com"
export MAIL_PORT=587
export MAIL_USERNAME="AKIAIOSFODNN7EXAMPLE"
export MAIL_PASSWORD="BExxxxxxxxxxxxxxxxxxxxxxxxxxxx"
export MAIL_FROM="noreply@yourdomain.com"
```

### Mailgun Configuration

```bash
# Mailgun: https://www.mailgun.com/
# 1. Create account and verify domain
# 2. Get SMTP credentials from Mailgun dashboard

export MAIL_HOST="smtp.mailgun.org"
export MAIL_PORT=587
export MAIL_USERNAME="postmaster@mg.yourdomain.com"
export MAIL_PASSWORD="your-mailgun-password"
export MAIL_FROM="noreply@mg.yourdomain.com"
```

### Testing Email Configuration

```bash
# Test email configuration endpoint
curl http://localhost:8080/actuator/health/mail

# Send test email (requires authentication)
curl -X POST http://localhost:8080/api/mail/test \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"recipientEmail": "test@example.com"}'
```

---

## Payment Gateway Configuration

### Razorpay Configuration

| Variable | Type | Default | Required | Description | Source |
|----------|------|---------|----------|-------------|--------|
| `RAZORPAY_KEY_ID` | String | ❌ NONE | ✅ | Razorpay Key ID (public key) | https://dashboard.razorpay.com/app/keys |
| `RAZORPAY_KEY_SECRET` | String | ❌ NONE | ✅ | Razorpay Key Secret | https://dashboard.razorpay.com/app/keys |
| `RAZORPAY_WEBHOOK_SECRET` | String | ❌ NONE | ✅ | Webhook signing secret | https://dashboard.razorpay.com/app/settings/webhooks |
| `RAZORPAY_CURRENCY` | String | `INR` | ❌ | Currency code | INR, USD, EUR, etc. |

### Getting Razorpay Credentials

```bash
# 1. Create account: https://razorpay.com
# 2. Go to Settings > API Keys: https://dashboard.razorpay.com/app/keys
# 3. Copy Key ID and Key Secret

# For testing: Use Test Keys
# For production: Switch to Live Keys in dashboard

# Example credentials (test mode)
export RAZORPAY_KEY_ID="rzp_test_1234567890abcd"
export RAZORPAY_KEY_SECRET="abcdefghijklmnop1234"

# Get webhook secret
# 1. Go to Settings > Webhooks: https://dashboard.razorpay.com/app/settings/webhooks
# 2. Add webhook: http://yourdomain.com/webhooks/razorpay
# 3. Copy signing secret
export RAZORPAY_WEBHOOK_SECRET="webhook_signing_secret_xyz"

# Set currency for your region
export RAZORPAY_CURRENCY="INR"  # India
export RAZORPAY_CURRENCY="USD"  # United States
```

### Stripe Configuration (Optional)

| Variable | Type | Default | Required | Description | Source |
|----------|------|---------|----------|-------------|--------|
| `STRIPE_API_KEY` | String | ❌ NONE | ❌ | Stripe Secret Key | https://dashboard.stripe.com/apikeys |
| `STRIPE_PUBLISHABLE_KEY` | String | ❌ NONE | ❌ | Stripe Publishable Key | https://dashboard.stripe.com/apikeys |
| `STRIPE_WEBHOOK_SECRET` | String | ❌ NONE | ❌ | Stripe Webhook Secret | https://dashboard.stripe.com/webhooks |

### Getting Stripe Credentials

```bash
# 1. Create account: https://stripe.com
# 2. Go to Dashboard > API Keys: https://dashboard.stripe.com/apikeys
# 3. Get Secret Key and Publishable Key

# For testing: Use Test Keys
# For production: Switch to Live Keys

export STRIPE_API_KEY="sk_test_abcdefghijklmnop1234567890"
export STRIPE_PUBLISHABLE_KEY="pk_test_abcdefghijklmnop1234567890"

# Get webhook secret from: https://dashboard.stripe.com/webhooks
export STRIPE_WEBHOOK_SECRET="whsec_abcdefghijklmnop1234567890"
```

---

## Frontend Configuration

### Frontend Environment Variables

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `REACT_APP_API_URL` | String | `http://localhost:8080/api` | ❌ | Backend API base URL |
| `VITE_API_URL` | String | `http://localhost:8080/api` | ❌ | Vite backend API URL |
| `FRONTEND_URL` | String | `http://localhost:3000` | ✅ | Frontend public URL |
| `NODE_ENV` | String | `production` | ❌ | Node environment |

### Frontend Development

```bash
# .env.local (Vite development)
VITE_API_URL=http://localhost:8080/api
```

### Frontend Production

```bash
# .env.production (Vite production build)
VITE_API_URL=https://api.yourdomain.com/api

# Docker environment
export REACT_APP_API_URL=http://api:8080/api
export VITE_API_URL=http://api:8080/api
```

---

## Logging Configuration

### Log Level Settings

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `LOG_LEVEL` | String | `INFO` | ❌ | Global log level |
| `APP_LOG_LEVEL` | String | `INFO` | ❌ | Application log level |
| `SPRING_LOG_LEVEL` | String | `WARN` | ❌ | Spring framework log level |
| `SPRING_SECURITY_LOG_LEVEL` | String | `WARN` | ❌ | Spring Security log level |
| `SPRING_WEB_LOG_LEVEL` | String | `WARN` | ❌ | Spring Web log level |

### Log Levels (from highest to lowest)

```bash
# ERROR: Only errors
export LOG_LEVEL=ERROR

# WARN: Warnings and errors (production default)
export LOG_LEVEL=WARN

# INFO: General info, warnings, errors (recommended)
export LOG_LEVEL=INFO

# DEBUG: Detailed debugging info
export LOG_LEVEL=DEBUG

# TRACE: Very detailed logging
export LOG_LEVEL=TRACE
```

### Log File Configuration

| Variable | Type | Default | Required | Description |
|----------|------|---------|----------|-------------|
| `LOG_DIR` | String | `/var/log/perfume-shop` | ❌ | Log file directory |
| `LOG_MAX_FILE_SIZE` | String | `100MB` | ❌ | Max log file size |
| `LOG_MAX_BACKUP_INDEX` | Integer | 7 | ❌ | Number of backup files |

```bash
# Log directory
export LOG_DIR=/var/log/perfume-shop
export LOG_DIR=./logs
export LOG_DIR=/home/ubuntu/logs

# File rotation
export LOG_MAX_FILE_SIZE=100MB
export LOG_MAX_BACKUP_INDEX=7  # Keep 7 days of logs

# Docker volume for logs
# volumes:
#   - api-logs:/var/log/perfume-shop
```

---

## Validation & Troubleshooting

### Environment Variable Checklist

```bash
#!/bin/bash
# Run this script to validate all required variables

required_vars=(
  "JWT_SECRET"
  "DATABASE_USERNAME"
  "DATABASE_PASSWORD"
  "MAIL_USERNAME"
  "MAIL_PASSWORD"
  "RAZORPAY_KEY_ID"
  "RAZORPAY_KEY_SECRET"
)

missing=()
for var in "${required_vars[@]}"; do
  if [ -z "${!var}" ]; then
    missing+=("$var")
  fi
done

if [ ${#missing[@]} -gt 0 ]; then
  echo "❌ Missing required variables:"
  printf '%s\n' "${missing[@]}"
  exit 1
else
  echo "✅ All required variables are set"
fi

# Check JWT_SECRET length
if [ ${#JWT_SECRET} -lt 44 ]; then
  echo "⚠️  JWT_SECRET might be too short (${#JWT_SECRET} chars, should be 44+)"
fi

# Check password length
if [ ${#DATABASE_PASSWORD} -lt 8 ]; then
  echo "⚠️  DATABASE_PASSWORD might be too weak (${#DATABASE_PASSWORD} chars, should be 8+)"
fi

echo "✅ Validation complete"
```

### Testing Environment Variables

```bash
# Display all set environment variables
env | grep -E "DATABASE_|JWT_|MAIL_|RAZORPAY_|CORS_"

# Display specific variable
echo $JWT_SECRET
echo $DATABASE_USERNAME

# Test database connection
mysql -h $DATABASE_HOST -u $DATABASE_USERNAME -p"$DATABASE_PASSWORD" -e "SELECT 1;"

# Test API with specific variables
curl -s http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("system")) | .properties'

# View active Spring profile
curl -s http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("profiles"))'
```

### Common Variable Mistakes

| Mistake | Problem | Solution |
|---------|---------|----------|
| `JWT_SECRET` not set | Token validation fails | Generate: `openssl rand -base64 32` |
| `DATABASE_PASSWORD` too short | Connection refused | Use min 8 characters |
| `MAIL_PASSWORD` (Gmail) is regular password | Authentication fails | Use App Password from Google |
| `RAZORPAY_KEY_ID` from Live dashboard used in Test | Payment fails | Use Test Keys for development |
| `CORS_ORIGINS` doesn't include frontend | CORS errors in browser | Add frontend URL to list |
| `JWT_SECRET` changed mid-session | Tokens become invalid | Use consistent secret |

---

## Complete .env Example

```bash
# Copy to .env.production and fill in your values

# ===== REQUIRED =====
JWT_SECRET=aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0uV1wX2yZ3aBcD4eF5g
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=your-secure-password-16-chars
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
RAZORPAY_KEY_ID=rzp_test_1234567890abcd
RAZORPAY_KEY_SECRET=abcdefghijklmnop1234
RAZORPAY_WEBHOOK_SECRET=webhook_signing_secret

# ===== RECOMMENDED =====
DATABASE_URL=jdbc:mysql://database:3306/perfume_shop?useSSL=false&serverTimezone=UTC
CORS_ORIGINS=http://localhost:3000,http://localhost:5173
FRONTEND_URL=http://localhost:3000
RAZORPAY_WEBHOOK_SECRET=webhook_secret_from_dashboard

# ===== OPTIONAL =====
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_FROM=noreply@perfume.com
LOG_LEVEL=INFO
DATABASE_POOL_SIZE=20
PASSWORD_ENCODER_STRENGTH=12
```

---

**Reference Date**: January 2026
**Spring Boot Version**: 3.2.1
**Application Name**: Perfume Shop
