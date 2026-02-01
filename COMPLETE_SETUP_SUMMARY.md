# Complete Docker Production Setup Summary

End-to-end guide for the Perfume Shop Docker production setup with all required configurations, environment variables, and verification steps.

## Overview

This document summarizes the complete Docker-based production setup for the Perfume Shop application. It includes:

- **docker-compose.yml**: Multi-container orchestration
- **Environment Variables**: All required secrets and configuration
- **Verification Procedures**: Step-by-step validation
- **Troubleshooting Guide**: Common issues and solutions

## Quick Start (5 Minutes)

```bash
# 1. Clone repository
git clone <repo> && cd perfume-shop

# 2. Create environment file
cp .env.production.example .env.production

# 3. Edit environment file with your values
nano .env.production

# 4. Build backend JAR (if not already built)
mvn clean package -DskipTests

# 5. Start all services
docker compose --env-file .env.production up --build

# 6. Verify in another terminal
curl http://localhost:8080/actuator/health
```

---

## File Structure

```
perfume-shop/
├── docker-compose.yml              # ✅ UPDATED - Multi-container setup
├── Dockerfile                       # Backend image definition
├── frontend/
│   └── Dockerfile                  # Frontend image definition
├── .env.production.example          # Template for environment variables
├── .env.production                  # ⚠️ CREATE THIS - Your secrets
├── target/
│   └── perfume-shop-1.0.0.jar      # ⚠️ BUILD THIS - mvn package
├── src/main/resources/
│   ├── application.yml              # Development config
│   └── application-prod.yml         # Production config (uses env vars)
├── RUN_CHECKLIST.md                 # ✅ NEW - Step-by-step verification
├── ENVIRONMENT_VARIABLES.md         # ✅ NEW - All variables reference
├── DOCKER_VALIDATION.md             # ✅ NEW - Validation procedures
└── COMPLETE_SETUP_SUMMARY.md        # This file
```

---

## Service Architecture

### Services Running in Docker

```
┌─────────────────────────────────────────────────────────────┐
│  Docker Compose Network (perfume-network)                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌────────────────────┐  ┌────────────────────┐              │
│  │  MySQL Database    │  │  Spring Boot API   │              │
│  ├────────────────────┤  ├────────────────────┤              │
│  │ Host: database     │  │ Host: api          │              │
│  │ Port: 3306         │  │ Port: 8080         │              │
│  │ Status: Healthy    │  │ Status: Healthy    │              │
│  └────────────────────┘  └────────────────────┘              │
│           ↕                       ↕                            │
│    Persistently stores      Uses application-prod.yml        │
│    user, product, order     Loads from environment vars      │
│    data in mysql-data vol   Writes logs to api-logs vol      │
│                                                               │
│  Optional:                                                    │
│  ┌────────────────────┐                                       │
│  │  React Frontend    │  (Can be built and added)            │
│  ├────────────────────┤                                       │
│  │ Host: frontend     │                                       │
│  │ Port: 3000 → 80    │                                       │
│  └────────────────────┘                                       │
│                                                               │
└─────────────────────────────────────────────────────────────┘
                            ↕
            Host Machine (Your Computer)
    Port Mappings:
    - 3306 → MySQL (localhost:3306)
    - 8080 → API (localhost:8080)
    - 9090 → Actuator (localhost:9090)
    - 3000 → Frontend (localhost:3000, if enabled)
```

---

## Configuration Overview

### 1. docker-compose.yml Changes

**What's New/Updated:**

1. **Enhanced database service** with:
   - Detailed comments
   - Resource limits (optional)
   - Better health checks
   - Volume mounts for SQL initialization

2. **Enhanced API service** with:
   - Comprehensive environment variables
   - Clear comments for each variable
   - Token expiration settings
   - Payment gateway integration
   - Logging configuration
   - Health checks with proper timing

3. **Frontend service** (commented out):
   - Ready to uncomment
   - Nginx + React configuration
   - Proper API URL configuration

4. **Volumes and Networks**:
   - MySQL data persistence
   - API logs persistence
   - Bridge network for inter-container communication

---

### 2. Environment Variables (Required)

**Critical Variables (Must Provide):**

```bash
# Database Credentials
DATABASE_USERNAME=prod_user           # Min 3 chars
DATABASE_PASSWORD=secure_password      # Min 8 chars

# JWT Secret (256+ bits)
JWT_SECRET=$(openssl rand -base64 32) # Generated key

# Email Configuration
MAIL_USERNAME=your-email@gmail.com    # Gmail app password
MAIL_PASSWORD=xxxx xxxx xxxx xxxx     # 16-char app password

# Payment Gateway (Razorpay)
RAZORPAY_KEY_ID=rzp_test_xxx          # From dashboard
RAZORPAY_KEY_SECRET=xxx               # From dashboard
RAZORPAY_WEBHOOK_SECRET=xxx           # From dashboard

# Security
CORS_ORIGINS=http://localhost:3000    # Frontend URL
```

**Important Variables (Recommended):**

```bash
DATABASE_URL=jdbc:mysql://database:3306/perfume_shop?useSSL=false&serverTimezone=UTC
FRONTEND_URL=http://localhost:3000
RAZORPAY_CURRENCY=INR
```

**Optional Variables (Have Defaults):**

```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
LOG_LEVEL=INFO
DATABASE_POOL_SIZE=20
PASSWORD_ENCODER_STRENGTH=12
```

---

## Step-by-Step Setup

### Step 1: Prepare Environment Variables

```bash
# Create .env.production file
cp .env.production.example .env.production

# Edit with your actual values
nano .env.production

# Minimum required values:
# JWT_SECRET=<generate with openssl>
# DATABASE_USERNAME=prod_user
# DATABASE_PASSWORD=<secure_password>
# MAIL_USERNAME=<your_email>
# MAIL_PASSWORD=<app_password>
# RAZORPAY_KEY_ID=<your_key>
# RAZORPAY_KEY_SECRET=<your_secret>
# RAZORPAY_WEBHOOK_SECRET=<your_webhook_secret>
```

### Step 2: Build Backend JAR

```bash
# Build with Maven
mvn clean package -DskipTests

# Verify JAR exists
ls -lh target/perfume-shop-1.0.0.jar

# Should be 50-100MB
```

### Step 3: Start Docker Compose

```bash
# Option 1: Build during startup (recommended)
docker compose --env-file .env.production up --build

# Option 2: Build separately
docker compose --env-file .env.production build
docker compose --env-file .env.production up

# Option 3: Run in background
docker compose --env-file .env.production up -d
docker compose logs -f
```

### Step 4: Verify Services

```bash
# Check all services are running
docker compose ps

# Expected output:
# NAME              STATUS
# perfume-shop-db   Up 2 minutes (healthy)
# perfume-shop-api  Up 1 minute (healthy)

# Test database
docker compose exec database mysql -u prod_user -p"${DATABASE_PASSWORD}" -e "SELECT 1;"

# Test API
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{...}}
```

### Step 5: Test API Endpoints

```bash
# Get featured products
curl http://localhost:8080/api/products/featured | jq .

# Register test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "1234567890"
  }' | jq .

# Expected response includes token, user, expiresIn
```

---

## Environment Variable Categories

### Backend Server
```
SPRING_PROFILES_ACTIVE=prod    # Activates application-prod.yml
PORT=8080
SERVER_PORT=8080
ENVIRONMENT=production
```

### Database
```
DATABASE_URL=jdbc:mysql://database:3306/perfume_shop?...
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=secure_password
DATABASE_POOL_SIZE=20
DATABASE_CONNECTION_TIMEOUT=10000
DATABASE_MAX_IDLE_TIME=300000
```

### JWT & Security
```
JWT_SECRET=base64_encoded_256_bits
JWT_EXPIRATION=86400000        # 24 hours in ms
JWT_REFRESH_EXPIRATION=604800000 # 7 days in ms
JWT_GRACE_PERIOD=60000         # 1 minute in ms
CORS_ORIGINS=http://localhost:3000
PASSWORD_ENCODER_STRENGTH=12
```

### Email
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx
MAIL_FROM=noreply@perfume.com
EMAIL_MAX_RETRIES=3
EMAIL_RETRY_DELAY=5000
```

### Payment - Razorpay
```
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=xxx
RAZORPAY_WEBHOOK_SECRET=xxx
RAZORPAY_CURRENCY=INR
```

### Payment - Stripe (Optional)
```
STRIPE_API_KEY=sk_test_xxx
STRIPE_PUBLISHABLE_KEY=pk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx
```

### Frontend
```
FRONTEND_URL=http://localhost:3000
```

### Logging
```
LOG_LEVEL=INFO
LOG_DIR=/var/log/perfume-shop
LOG_MAX_FILE_SIZE=100MB
LOG_MAX_BACKUP_INDEX=7
SPRING_LOG_LEVEL=WARN
SPRING_SECURITY_LOG_LEVEL=WARN
```

---

## Port Mapping

| Internal Port | Host Port | Service | Purpose |
|---------------|-----------|---------|---------|
| 3306 | 3306 | MySQL | Database |
| 8080 | 8080 | Spring Boot | API endpoints |
| 9090 | 9090 | Spring Boot | Actuator/Management |
| 80 | 3000 | Nginx | Frontend (optional) |

**If ports are in use:**

```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>

# Or change in docker-compose.yml:
# ports:
#   - "8888:8080"  # Use 8888 instead of 8080
```

---

## Verification URLs

### Local Development

| Endpoint | URL | Method | Authentication |
|----------|-----|--------|-----------------|
| Health | http://localhost:8080/actuator/health | GET | No |
| Info | http://localhost:8080/actuator/info | GET | No |
| Register | http://localhost:8080/api/auth/register | POST | No |
| Login | http://localhost:8080/api/auth/login | POST | No |
| Products | http://localhost:8080/api/products | GET | No |
| Featured | http://localhost:8080/api/products/featured | GET | No |
| Profile | http://localhost:8080/api/users/profile | GET | Yes (Bearer token) |
| Cart | http://localhost:8080/api/cart | GET/POST | Yes |

### Example Requests

**Register User:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "1234567890"
  }'
```

**Get Featured Products:**
```bash
curl http://localhost:8080/api/products/featured | jq .
```

**Add to Cart (Authenticated):**
```bash
curl -X POST http://localhost:8080/api/cart \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'
```

---

## Troubleshooting Quick Guide

### Issue: "Database connection refused"

```bash
# Check database is running
docker compose ps database

# View database logs
docker compose logs database | tail -50

# Verify database password is correct in .env.production
grep DATABASE_PASSWORD .env.production

# Rebuild
docker compose down && \
docker compose --env-file .env.production up --build
```

### Issue: "JWT_SECRET not set"

```bash
# Generate new secret
openssl rand -base64 32

# Add to .env.production
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET" >> .env.production

# Restart
docker compose restart api
```

### Issue: "Port already in use"

```bash
# Option 1: Kill existing process
kill -9 $(lsof -ti:8080)

# Option 2: Use different port in docker-compose.yml
# Change: "8080:8080" to "8888:8080"
# Then rebuild

# Option 3: Check what's using port
lsof -i :8080
```

### Issue: "CORS error in browser"

```bash
# Update .env.production
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# Restart API
docker compose restart api

# Verify
curl -H "Origin: http://localhost:3000" -i http://localhost:8080/api/products
```

### Issue: "Email authentication failed"

```bash
# For Gmail: Use App Password, not regular password
# 1. Go to: https://myaccount.google.com/apppasswords
# 2. Generate app password
# 3. Update .env.production:
MAIL_PASSWORD=xxxx xxxx xxxx xxxx

# Restart
docker compose restart api
```

---

## Monitoring and Logs

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f api
docker compose logs -f database

# Last N lines
docker compose logs --tail=100 api

# Filter by keyword
docker compose logs api | grep -i "error\|exception"
```

### Health Checks

```bash
# Quick health check
curl -s http://localhost:8080/actuator/health | jq .status

# Detailed health
curl -s http://localhost:8080/actuator/health | jq .

# Database status
curl -s http://localhost:8080/actuator/health | jq '.components.db.status'
```

### Resource Monitoring

```bash
# Monitor container resources
docker stats

# Check container logs size
du -h $(docker inspect --format='{{.LogPath}}' perfume-shop-api)

# Monitor network traffic
docker stats perfume-shop-api perfume-shop-db
```

---

## Cleanup and Shutdown

### Stop Services

```bash
# Stop all containers (data preserved)
docker compose down

# View stopped services
docker compose ps -a
```

### Remove Everything

```bash
# Remove containers and volumes (WARNING: deletes data)
docker compose down -v

# Remove images
docker compose down --rmi all

# Clean up unused Docker resources
docker system prune -a
```

### Backup Data

```bash
# Backup MySQL data before cleanup
docker compose exec database mysqldump -u prod_user -p perfume_shop > backup.sql

# Backup named volume
docker run --rm -v perfume_mysql-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/mysql-backup.tar.gz /data

# List volumes
docker volume ls | grep perfume
```

---

## Deployment Checklist

Before deploying to production:

- [ ] Generate secure JWT_SECRET: `openssl rand -base64 32`
- [ ] Set strong DATABASE_PASSWORD (16+ chars, mixed case, numbers, symbols)
- [ ] Configure MAIL credentials (Gmail, SendGrid, AWS SES, or Mailgun)
- [ ] Get RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET from Razorpay dashboard
- [ ] Get RAZORPAY_WEBHOOK_SECRET from Razorpay webhooks
- [ ] Set correct CORS_ORIGINS for frontend URL
- [ ] Set FRONTEND_URL for API responses
- [ ] Test locally: `docker compose up --build`
- [ ] Verify all services are healthy
- [ ] Test API endpoints with curl
- [ ] Test user registration and login
- [ ] Verify database persistence
- [ ] Check logs for errors
- [ ] Review security settings:
  - [ ] PASSWORD_ENCODER_STRENGTH=12
  - [ ] SSL/TLS enabled in DATABASE_URL
  - [ ] CORS_ORIGINS restrictive
  - [ ] JWT_SECRET strong and secure
  - [ ] Email password is app-specific token

---

## Production Considerations

### Database

- Use managed database service (AWS RDS, Google Cloud SQL, Azure Database)
- Enable automated backups
- Set up read replicas for high availability
- Use SSL/TLS for connections
- Monitor query performance

### API Server

- Use reverse proxy (Nginx, HAProxy)
- Enable HTTPS/TLS
- Configure rate limiting
- Set up monitoring and alerting
- Implement auto-scaling
- Use health checks for load balancer

### Email

- Use dedicated email service (SendGrid, AWS SES)
- Set up SPF, DKIM, DMARC records
- Monitor deliverability
- Keep logs for compliance

### Security

- Use environment variables from secrets manager
- Never commit .env files to git
- Rotate JWT_SECRET periodically
- Monitor access logs
- Set up intrusion detection
- Enable audit logging

### Monitoring

- Use container orchestration (Kubernetes)
- Set up ELK stack for logs
- Use Prometheus for metrics
- Set up alert rules
- Monitor error rates
- Track API latency

---

## File Reference

### New/Updated Files

| File | Purpose | Last Updated |
|------|---------|--------------|
| docker-compose.yml | Container orchestration | Enhanced with detailed env vars |
| RUN_CHECKLIST.md | Step-by-step verification | New, comprehensive |
| ENVIRONMENT_VARIABLES.md | All environment variable docs | New, complete reference |
| DOCKER_VALIDATION.md | Validation and troubleshooting | New, extensive |
| COMPLETE_SETUP_SUMMARY.md | This file | New overview |

### Existing Files Used

| File | Purpose |
|------|---------|
| Dockerfile | Backend image definition |
| frontend/Dockerfile | Frontend image definition |
| .env.production.example | Environment template |
| application-prod.yml | Spring Boot production config |
| pom.xml | Maven build configuration |
| frontend/package.json | NPM dependencies |

---

## Getting Help

1. **Check logs first**: `docker compose logs api`
2. **Review RUN_CHECKLIST.md**: Step-by-step verification guide
3. **Check ENVIRONMENT_VARIABLES.md**: Detailed variable documentation
4. **Run validation scripts**: `./validate-docker-setup.sh`
5. **Check DOCKER_VALIDATION.md**: Troubleshooting guide

---

## Summary Statistics

- **Services**: 2 core (MySQL, Spring Boot) + 1 optional (Frontend)
- **Environment Variables**: 25+ configurable
- **Required Secrets**: 7 critical
- **Ports**: 4 exposed
- **Volumes**: 2 named (data persistence)
- **Documentation Files**: 4 comprehensive guides
- **Build Time**: 5-10 minutes (first run)
- **Startup Time**: 30-60 seconds (after build)

---

**Version**: 1.0
**Last Updated**: January 2026
**Spring Boot Version**: 3.2.1
**Docker Compose Version**: 3.8+
**MySQL Version**: 8.0
**Java Version**: 17
