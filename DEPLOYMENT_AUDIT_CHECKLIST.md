# Production Deployment Audit Checklist

**Deployment Strategy**: 
- Frontend: Vercel
- Backend: Railway  
- Database: PostgreSQL
- Payment: Razorpay
- Media: Nginx (from repo)

**Date**: January 30, 2026

---

## ⚠️ CRITICAL ISSUES FOUND

### 1. **Database Configuration - NEEDS CHANGE**
**Status**: ❌ Not Ready

**Current Issue**:
- Application is configured for **H2 Database** (in-memory)
- Using MySQL dependencies in pom.xml
- Need to switch to **PostgreSQL** for Railway deployment

**Required Actions**:
- [ ] Add PostgreSQL driver dependency to pom.xml
- [ ] Create `application-prod.yml` with PostgreSQL configuration
- [ ] Update database URL format for Railway
- [ ] Test PostgreSQL dialect configuration

---

### 2. **Frontend URL - NEEDS UPDATE**
**Status**: ❌ Not Ready

**Current Issue**:
```yaml
# application.yml
frontend:
  url: http://localhost:3000
```

**Problem**: This is hardcoded for local development

**Required Actions**:
- [ ] Update to use environment variable: `${FRONTEND_URL}`
- [ ] Set to Vercel deployment URL (e.g., `https://yourdomain.vercel.app`)
- [ ] Ensure CORS_ORIGINS includes Vercel domain

---

### 3. **CORS Configuration - NEEDS UPDATE**
**Status**: ⚠️ Partially Ready

**Current Issue**:
```yaml
cors-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
```

**Problem**: Default values are local, need production Vercel URL

**Required Actions**:
- [ ] Update `CORS_ORIGINS` environment variable to: `https://yourdomain.vercel.app,https://www.yourdomain.vercel.app`
- [ ] This must match your Vercel deployment URL exactly

---

### 4. **Media File Serving - NEEDS CONFIGURATION**
**Status**: ⚠️ Partially Ready

**Current Setup**:
- MediaController exists at `/api/media`
- Default upload directory: `./media`
- Media URL configured as: `http://localhost:8080/media`

**Issues for Nginx Setup**:
- [ ] Media upload path needs to be persistent volume/directory that Nginx can serve
- [ ] Need to configure Nginx to serve from `/media` directory
- [ ] Media URL must point to Nginx (https://yourdomain.com/media)
- [ ] Configure CORS headers for media files

---

### 5. **Railway Environment Variables - NEEDS SETUP**
**Status**: ❌ Not Ready

**Railway doesn't support `.env` files by default**

**Required Actions**:
- [ ] Add all environment variables in Railway dashboard
- [ ] Generate strong secrets (JWT, DB password)
- [ ] Configure payment gateway keys (Razorpay)
- [ ] Set CORS origins for Vercel

---

### 6. **Razorpay Configuration - READY ✅**
**Status**: ✅ Ready

- RazorpayService is implemented
- Payment endpoints configured
- Webhook handling is in place
- Just need to add live API keys in Railway

---

### 7. **Payment Webhook URLs - NEEDS UPDATE**
**Status**: ⚠️ Needs Configuration

**Current Code**: 
- Webhooks configured at `/api/payment/webhook/**`
- `/api/razorpay/webhook/**`

**Required Actions**:
- [ ] In Razorpay dashboard, set webhook URL to: `https://yourrailwayapp.up.railway.app/api/razorpay/webhook/payment`
- [ ] Ensure webhook secret matches `RAZORPAY_WEBHOOK_SECRET`

---

### 8. **PostgreSQL DDL Configuration - NEEDS UPDATE**
**Status**: ❌ Not Configured

**Current Setting** (application-dev.yml):
```yaml
jpa:
  hibernate:
    ddl-auto: create-drop
```

**Required Actions for Production**:
- [ ] Create `application-prod.yml` with: `ddl-auto: validate`
- [ ] Run database migrations separately
- [ ] Never use `create`, `create-drop`, or `update` in production

---

---

## ✅ WHAT'S ALREADY GOOD

1. **Razorpay Integration** ✅
   - Service is fully implemented
   - Webhook handling configured
   - Just needs live API keys

2. **JWT Authentication** ✅
   - Properly configured with environment variables
   - Token expiry handling implemented

3. **Security** ✅
   - CORS properly configured with environment variables
   - JWT filters in place
   - Password encryption ready

4. **Email Configuration** ✅
   - SMTP configured for Gmail
   - Retry logic implemented
   - Environment variables ready

---

## 🔧 STEP-BY-STEP FIX PROCESS

### Step 1: Add PostgreSQL Dependency
```xml
<!-- In pom.xml, add: -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.6.0</version>
    <scope>runtime</scope>
</dependency>
```

---

### Step 2: Create application-prod.yml
```yaml
spring:
  application:
    name: perfume-shop
  
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: ${PORT:8080}
  error:
    include-message: never
    include-binding-errors: never
    include-stacktrace: never

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:86400000}
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
  
  security:
    cors-origins: ${CORS_ORIGINS}
    password-encoder-strength: 12
  
  razorpay:
    key-id: ${RAZORPAY_KEY_ID}
    key-secret: ${RAZORPAY_KEY_SECRET}
    webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}
    currency: ${RAZORPAY_CURRENCY:INR}
  
  email:
    max-retries: 3
  
  frontend:
    url: ${FRONTEND_URL}
  
  media:
    upload-dir: ${MEDIA_UPLOAD_DIR:/tmp/media}
    url: ${MEDIA_URL}

logging:
  level:
    root: WARN
    com.perfume: INFO
    org.springframework.web.cors: WARN
```

---

### Step 3: Update Frontend URL Configuration

In `src/main/resources/application.yml`, change:
```yaml
# FROM:
frontend:
  url: http://localhost:3000

# TO:
frontend:
  url: ${FRONTEND_URL:http://localhost:3000}
```

---

### Step 4: Update MediaController

The MediaController already has the right setup:
- Upload directory: `${app.media.upload-dir:./media}`
- Media URL: `${app.media.url:http://localhost:8080/media}`

Just need to configure these in Railway environment variables.

---

### Step 5: Nginx Configuration for Media Files

Create nginx.conf to serve media files:

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # API proxy to Railway backend
    location /api/ {
        proxy_pass https://yourrailwayapp.up.railway.app;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Serve media files from filesystem
    location /media/ {
        # Make sure this directory has uploaded files
        alias /var/www/perfume-shop/media/;
        
        # Cache media files for 30 days
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # Allow CORS for media
        add_header Access-Control-Allow-Origin "*";
        add_header Access-Control-Allow-Methods "GET, OPTIONS";
    }

    # Health check endpoint
    location /health {
        access_log off;
        proxy_pass https://yourrailwayapp.up.railway.app/health;
    }
}
```

---

### Step 6: Configure Railway Environment Variables

Add these to Railway dashboard (Variables section):

```
DATABASE_URL=postgresql://user:password@host:5432/perfume_shop
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
PORT=8080
ENVIRONMENT=production
SPRING_PROFILES_ACTIVE=prod

JWT_SECRET=your_256_bit_base64_secret
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

CORS_ORIGINS=https://yourdomain.vercel.app,https://www.yourdomain.vercel.app
FRONTEND_URL=https://yourdomain.vercel.app

RAZORPAY_KEY_ID=rzp_live_your_key
RAZORPAY_KEY_SECRET=your_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
RAZORPAY_CURRENCY=INR

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@yourdomain.com

MEDIA_UPLOAD_DIR=/tmp/media
MEDIA_URL=https://yourdomain.com/media

LOG_LEVEL=INFO
```

---

### Step 7: Build & Deploy to Railway

```bash
# Ensure PostgreSQL driver is included in build
mvn clean package -DskipTests -Pproduction

# Deploy to Railway (via CLI or GitHub integration)
railway up
```

---

### Step 8: Configure Nginx Server

Deploy your repo to the Nginx server with media directory:

```bash
# SSH to Nginx server
ssh your_server

# Clone repo
git clone https://github.com/yourusername/perfume-shop.git /var/www/perfume-shop

# Create media directory
mkdir -p /var/www/perfume-shop/media
chmod 755 /var/www/perfume-shop/media

# Set permissions
chown -R www-data:www-data /var/www/perfume-shop/media

# Install Nginx configuration
sudo cp /var/www/perfume-shop/nginx.conf /etc/nginx/sites-available/perfume-shop
sudo ln -s /etc/nginx/sites-available/perfume-shop /etc/nginx/sites-enabled/

# Test & reload Nginx
sudo nginx -t
sudo systemctl reload nginx
```

---

### Step 9: Update Frontend Environment Variables (Vercel)

In your React project, create `.env.production`:

```
VITE_API_URL=https://yourrailwayapp.up.railway.app/api
VITE_MEDIA_URL=https://yourdomain.com/media
VITE_RAZORPAY_KEY=your_razorpay_public_key
VITE_ENVIRONMENT=production
```

---

### Step 10: Configure Razorpay Webhooks

1. Go to Razorpay Dashboard → Settings → Webhooks
2. Add webhook:
   - **URL**: `https://yourrailwayapp.up.railway.app/api/razorpay/webhook/payment`
   - **Events**: payment.authorized, payment.failed, payment.captured
   - Save webhook secret from response

---

---

## 📋 FINAL DEPLOYMENT CHECKLIST

### Backend (Railway)
- [ ] PostgreSQL dependency added to pom.xml
- [ ] application-prod.yml created with PostgreSQL config
- [ ] All environment variables set in Railway dashboard
- [ ] JWT_SECRET generated and set
- [ ] Database URL configured for Railway PostgreSQL
- [ ] CORS_ORIGINS includes Vercel domain
- [ ] RAZORPAY keys configured (live mode)
- [ ] Application built and deployed
- [ ] Health check endpoint responding: https://yourrailwayapp.up.railway.app/actuator/health

### Frontend (Vercel)
- [ ] React app built for production
- [ ] .env.production configured with Railway API URL
- [ ] API_URL points to Railway backend
- [ ] MEDIA_URL points to Nginx media server
- [ ] Deployed to Vercel
- [ ] Accessible at yourdomain.vercel.app

### Nginx Server (Media Files)
- [ ] Server deployed with repo
- [ ] Media directory created and writable
- [ ] nginx.conf configured for API proxying
- [ ] Media file serving configured
- [ ] CORS headers enabled for media
- [ ] SSL certificate installed
- [ ] Nginx reloaded

### Razorpay
- [ ] Live API keys obtained
- [ ] Keys configured in Railway
- [ ] Webhook URL registered
- [ ] Webhook secret configured in Railway

### Testing
- [ ] Backend health check passes
- [ ] Vercel app loads without errors
- [ ] API requests from Vercel reach Railway backend
- [ ] Media files can be uploaded via /api/media/upload
- [ ] Media files served from Nginx /media/
- [ ] Razorpay payment process completes
- [ ] CORS headers present on responses

---

## 🚨 THINGS NOT TO FORGET

1. **Never commit `.env` files to GitHub** - Use Railway's Variables section
2. **Generate strong secrets** - Use: `openssl rand -base64 32`
3. **PostgreSQL is case-sensitive** for credentials
4. **Media upload directory must be writable** by Railway app
5. **Nginx must have read access** to media directory
6. **CORS must include Vercel domain** - test with curl
7. **Razorpay webhook secret** - keep it secret, use environment variable
8. **Database backups** - configure in Railway PostgreSQL settings
9. **Test payment flow completely** before going live
10. **Monitor logs** - Railway provides log streaming

---

## 📞 TROUBLESHOOTING

### Database Connection Error
```
Error: FATAL: database "perfume_shop" does not exist
```
**Fix**: Create database in Railway PostgreSQL first

### CORS Error from Vercel
```
Access-Control-Allow-Origin: yourdomain.vercel.app not allowed
```
**Fix**: Update `CORS_ORIGINS` in Railway variables

### Media Upload Returns 404
```
GET /media/image.jpg returns 404
```
**Fix**: Check Nginx media directory path and permissions

### Razorpay Payment Fails
```
Webhook signature verification failed
```
**Fix**: Ensure `RAZORPAY_WEBHOOK_SECRET` matches in Railway

---

## ✅ SUMMARY

**Current Status**: 🟡 **80% Ready**

**What Works**:
- ✅ Razorpay integration code
- ✅ Security & JWT setup
- ✅ Email configuration
- ✅ Media upload controller

**What Needs Fixing**:
- ❌ PostgreSQL configuration
- ❌ production.yml file
- ❌ Frontend URL configuration
- ❌ Railway environment setup
- ❌ Nginx media configuration

**Estimated Time to Production**: **2-3 hours** with the provided steps

