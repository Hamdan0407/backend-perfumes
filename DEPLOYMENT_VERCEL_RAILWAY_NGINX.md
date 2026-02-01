# Production Deployment Guide: Vercel + Railway + Nginx + PostgreSQL + Razorpay

**Architecture Overview:**
- **Frontend**: Vercel (React/Vite)
- **Backend API**: Railway (Spring Boot + PostgreSQL)
- **Media/Images**: Nginx (from your server)
- **Database**: Railway PostgreSQL
- **Payment**: Razorpay
- **Email**: SendGrid or Gmail

---

## Table of Contents
1. [Step 1: Setup PostgreSQL & Railway Backend](#step-1-setup-postgresql--railway-backend)
2. [Step 2: Configure Backend for PostgreSQL](#step-2-configure-backend-for-postgresql)
3. [Step 3: Update Application Configuration](#step-3-update-application-configuration)
4. [Step 4: Setup Nginx for Media Files](#step-4-setup-nginx-for-media-files)
5. [Step 5: Deploy Backend to Railway](#step-5-deploy-backend-to-railway)
6. [Step 6: Deploy Frontend to Vercel](#step-6-deploy-frontend-to-vercel)
7. [Step 7: Configure Razorpay Payment Gateway](#step-7-configure-razorpay-payment-gateway)
8. [Step 8: Update CORS & Environment Variables](#step-8-update-cors--environment-variables)
9. [Step 9: Testing & Validation](#step-9-testing--validation)
10. [Step 10: Production Monitoring](#step-10-production-monitoring)

---

## Step 1: Setup PostgreSQL & Railway Backend

### 1.1 Create Railway Account & PostgreSQL Database

```bash
# 1. Go to https://railway.app
# 2. Sign up with GitHub account (for easy deployment)
# 3. Create a new project

# 4. In Railway Dashboard:
#    - Click "+ Create" button
#    - Select "Database"
#    - Choose "PostgreSQL"
#    - Configure:
#      - Name: perfume-shop-db
#      - Region: Closest to your users (e.g., us-west-1)

# 5. PostgreSQL will be created with:
#    - PGHOST: auto-generated hostname
#    - PGPORT: 5432
#    - PGDATABASE: railway
#    - PGUSER: postgres
#    - PGPASSWORD: auto-generated secure password
```

### 1.2 Get Database Credentials from Railway

```bash
# In Railway Dashboard:
# 1. Click on your PostgreSQL service
# 2. Go to "Variables" tab
# 3. You'll see:
#    - PGHOST
#    - PGPORT
#    - PGDATABASE
#    - PGUSER
#    - PGPASSWORD
#    - DATABASE_URL (full connection string)

# Copy DATABASE_URL - you'll need it in Step 5
# Format: postgresql://username:password@host:port/database
```

---

## Step 2: Configure Backend for PostgreSQL

### 2.1 Add PostgreSQL Driver to pom.xml

Open your `pom.xml` and replace the MySQL dependency:

**OLD (Remove this):**
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

**NEW (Add this):**
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2.2 Create application-prod.yml Configuration

Create a new file: `src/main/resources/application-prod.yml`

```yaml
spring:
  application:
    name: perfume-shop
  
  datasource:
    url: ${DATABASE_URL}
    username: ${PGUSER:postgres}
    password: ${PGPASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 10000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20
          fetch_size: 50
        order_inserts: true
        order_updates: true
  
  mail:
    host: ${MAIL_HOST:smtp.sendgrid.net}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: ${PORT:8080}
  servlet:
    context-path: /api
  error:
    include-message: always
    include-binding-errors: never

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: ${JWT_EXPIRATION:86400000}
    refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}
    grace-period: ${JWT_GRACE_PERIOD:60000}
  
  security:
    cors-origins: ${CORS_ORIGINS}
    cors-max-age: 3600
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
    url: ${MEDIA_URL:https://yourdomain.com/media}
    upload-dir: ${MEDIA_UPLOAD_DIR:/tmp/media}

logging:
  level:
    root: WARN
    com.perfume: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN
  file:
    name: /tmp/logs/perfume-shop.log
    max-size: 10MB
    max-history: 5

management:
  endpoints:
    web:
      exposure: include=health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### 2.3 Update application.yml for Local Development

Update `src/main/resources/application.yml` for H2 (local dev):

```yaml
spring:
  application:
    name: perfume-shop
  
  datasource:
    url: jdbc:h2:mem:perfume_shop;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  
  h2:
    console:
      enabled: true
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: create
    defer-datasource-initialization: true
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.H2Dialect
  
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:your-email@gmail.com}
    password: ${MAIL_PASSWORD:your-app-password}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

server:
  port: 8080
  error:
    include-message: always
    include-binding-errors: always

app:
  jwt:
    secret: ${JWT_SECRET:YourSuperSecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm}
    expiration: 86400000
    refresh-expiration: 604800000
    grace-period: 60000
  
  security:
    cors-origins: ${CORS_ORIGINS:http://localhost:3000,http://localhost:5173,http://localhost:8000}
    cors-max-age: 3600
    password-encoder-strength: 12
  
  razorpay:
    key-id: ${RAZORPAY_KEY_ID:rzp_test_your_key}
    key-secret: ${RAZORPAY_KEY_SECRET:your_secret}
    webhook-secret: ${RAZORPAY_WEBHOOK_SECRET:your_webhook_secret}
    currency: ${RAZORPAY_CURRENCY:INR}
  
  email:
    max-retries: 3
  
  frontend:
    url: http://localhost:3000
  
  media:
    url: http://localhost:8080/media
    upload-dir: ./media

logging:
  level:
    com.perfume: DEBUG
    org.springframework.security: DEBUG

management:
  endpoints:
    web:
      exposure: include=health,info,metrics
```

---

## Step 3: Update Application Configuration

### 3.1 Create Media/File Upload Controller

Create: `src/main/java/com/perfume/shop/controller/MediaController.java`

```java
package com.perfume.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Media management controller for uploading and serving images.
 * Files are stored on nginx-accessible directory.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {
    
    @Value("${app.media.upload-dir:./media}")
    private String uploadDir;
    
    @Value("${app.media.url:http://localhost:8080/media}")
    private String mediaUrl;
    
    /**
     * Upload an image file.
     * Stores on server filesystem (nginx will serve from there)
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (!isValidImageType(contentType)) {
                return ResponseEntity.badRequest().body("Only images are allowed");
            }
            
            // Create upload directory if not exists
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID() + fileExtension;
            
            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.write(filePath, file.getBytes());
            
            // Return file URL for frontend
            String fileUrl = mediaUrl + "/" + uniqueFilename;
            
            log.info("File uploaded: {}", fileUrl);
            
            return ResponseEntity.ok(new UploadResponse(
                fileUrl,
                uniqueFilename,
                file.getSize()
            ));
            
        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("File upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Download an image file
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
                
        } catch (MalformedURLException e) {
            log.error("File download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete an image file (admin only)
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<?> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename);
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            Files.delete(filePath);
            log.info("File deleted: {}", filename);
            
            return ResponseEntity.ok("File deleted successfully");
            
        } catch (IOException e) {
            log.error("File deletion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("File deletion failed: " + e.getMessage());
        }
    }
    
    private boolean isValidImageType(String contentType) {
        return contentType != null && (
            contentType.equals("image/jpeg") ||
            contentType.equals("image/png") ||
            contentType.equals("image/gif") ||
            contentType.equals("image/webp")
        );
    }
    
    public static class UploadResponse {
        public String url;
        public String filename;
        public long size;
        
        public UploadResponse(String url, String filename, long size) {
            this.url = url;
            this.filename = filename;
            this.size = size;
        }
    }
}
```

### 3.2 Update Product Service for Media URLs

Update `src/main/java/com/perfume/shop/service/ProductService.java` to use media URLs from environment.

---

## Step 4: Setup Nginx for Media Files

### 4.1 Install Nginx on Your Server

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install -y nginx

# CentOS/RHEL
sudo yum install -y nginx

# Start Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### 4.2 Create Nginx Configuration

Create: `/etc/nginx/sites-available/perfume-shop-media.conf`

```nginx
# Nginx configuration for serving media files and proxying to Railway backend

upstream railway_backend {
    server your-railway-api.railway.app;
}

server {
    listen 80;
    listen [::]:80;
    server_name yourdomain.com www.yourdomain.com;
    
    client_max_body_size 10M;
    
    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    
    # SSL certificates (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    client_max_body_size 10M;
    
    # ==========================================
    # SERVE MEDIA FILES FROM NGINX
    # ==========================================
    location /media/ {
        alias /var/www/perfume-shop/media/;
        
        # Cache images for 30 days
        expires 30d;
        add_header Cache-Control "public, immutable";
        
        # Enable gzip compression
        gzip on;
        gzip_types image/png image/jpeg image/gif image/webp;
        
        # Prevent direct access to files outside /media directory
        # Security: Only serve image files
        if ($request_filename ~* ^.*?\.(png|jpg|jpeg|gif|webp|svg)$) {
            break;
        }
        return 404;
    }
    
    # ==========================================
    # PROXY API REQUESTS TO RAILWAY BACKEND
    # ==========================================
    location /api/ {
        proxy_pass https://your-railway-api.railway.app/api/;
        
        # Headers
        proxy_set_header Host your-railway-api.railway.app;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host yourdomain.com;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # WebSocket support (if needed)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # ==========================================
    # STATIC HEALTH CHECK
    # ==========================================
    location /health {
        access_log off;
        return 200 "OK";
        add_header Content-Type text/plain;
    }
    
    # ==========================================
    # ROOT - Redirect to Vercel Frontend
    # ==========================================
    location / {
        return 301 https://your-vercel-app.vercel.app$request_uri;
    }
}
```

### 4.3 Setup Let's Encrypt SSL Certificate

```bash
# Install Certbot
sudo apt-get install -y certbot python3-certbot-nginx

# Or for CentOS
sudo yum install -y certbot python3-certbot-nginx

# Get certificate
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# Auto-renew
sudo systemctl enable certbot-renew.timer
sudo systemctl start certbot-renew.timer
```

### 4.4 Create Media Directory with Proper Permissions

```bash
# Create media directory
sudo mkdir -p /var/www/perfume-shop/media
sudo chown -R www-data:www-data /var/www/perfume-shop
sudo chmod -R 755 /var/www/perfume-shop

# Create logs directory
sudo mkdir -p /var/log/perfume-shop
sudo chown www-data:www-data /var/log/perfume-shop
sudo chmod 755 /var/log/perfume-shop
```

### 4.5 Enable Nginx Configuration

```bash
# Create symlink
sudo ln -s /etc/nginx/sites-available/perfume-shop-media.conf \
            /etc/nginx/sites-enabled/perfume-shop-media.conf

# Remove default site
sudo rm -f /etc/nginx/sites-enabled/default

# Test Nginx configuration
sudo nginx -t

# Reload Nginx
sudo systemctl reload nginx
```

---

## Step 5: Deploy Backend to Railway

### 5.1 Create Procfile for Railway

Create: `Procfile` in your project root

```
web: java -Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=prod -jar target/perfume-shop-1.0.0.jar
```

### 5.2 Create .railwayapp Configuration (Optional)

Create: `railway.json` in your project root

```json
{
  "build": {
    "builder": "maven"
  },
  "deploy": {
    "startCommand": "java -Xmx512m -Xms256m -Dspring.profiles.active=prod -jar target/perfume-shop-1.0.0.jar"
  }
}
```

### 5.3 Setup Railway Environment Variables

In Railway Dashboard:

1. **Create new project** → Select your GitHub repository
2. **Railway will auto-detect** it as Maven/Java project
3. Go to **Variables** tab and add:

```bash
# Database (auto-generated from PostgreSQL service)
DATABASE_URL=postgresql://username:password@host:5432/railway

# JWT Configuration
JWT_SECRET=<generate-256-bit-key-using-command-below>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
JWT_GRACE_PERIOD=60000

# CORS - Allow Vercel frontend
CORS_ORIGINS=https://your-app.vercel.app,https://www.your-domain.com

# Email Configuration (SendGrid recommended)
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=<SG.your_sendgrid_api_key>
MAIL_FROM=noreply@yourdomain.com

# Razorpay Configuration
RAZORPAY_KEY_ID=<your_razorpay_live_key>
RAZORPAY_KEY_SECRET=<your_razorpay_live_secret>
RAZORPAY_WEBHOOK_SECRET=<your_webhook_secret>
RAZORPAY_CURRENCY=INR

# Frontend & Media URLs
FRONTEND_URL=https://your-app.vercel.app
MEDIA_URL=https://yourdomain.com/media
MEDIA_UPLOAD_DIR=/tmp/media

# Spring Profile
SPRING_PROFILES_ACTIVE=prod

# Port (Railway provides PORT env var automatically)
PORT=8080
```

### 5.4 Generate JWT Secret

```bash
# In your terminal:
openssl rand -base64 32

# Or using Node.js:
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"

# Or using Python:
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

### 5.5 Deploy from Railway Dashboard

```bash
# Railway will automatically:
# 1. Build Maven project: mvn clean package -DskipTests
# 2. Deploy the JAR to their servers
# 3. Provide you with a URL: your-project.railway.app

# You'll see deployment logs in real-time
# Once deployed, your API will be at:
# https://your-project.railway.app/api
```

### 5.6 Verify Backend Deployment

```bash
# Test health endpoint
curl https://your-project.railway.app/api/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"}}}

# Test product endpoint
curl https://your-project.railway.app/api/products?page=0&size=10
```

---

## Step 6: Deploy Frontend to Vercel

### 6.1 Prepare Frontend for Vercel

Update: `frontend/.env.production.local`

```bash
VITE_API_URL=https://your-project.railway.app/api
VITE_RAZORPAY_KEY=<your_razorpay_public_key>
VITE_MEDIA_URL=https://yourdomain.com/media
VITE_FRONTEND_URL=https://your-app.vercel.app
```

Update: `frontend/vite.config.js`

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_URL || 'http://localhost:8080/api',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  }
})
```

### 6.2 Deploy Frontend to Vercel

**Option A: Using Vercel CLI**

```bash
# Install Vercel CLI
npm install -g vercel

# Login to Vercel
vercel login

# Deploy from your frontend directory
cd frontend
vercel --prod
```

**Option B: Connect GitHub Repository**

```bash
# 1. Go to https://vercel.com
# 2. Click "Add New..." → "Project"
# 3. Import your GitHub repository (select frontend folder)
# 4. Configure project:
#    - Framework: Vite
#    - Root Directory: ./frontend (if monorepo)
#    - Build Command: npm run build
#    - Output Directory: dist
# 5. Add Environment Variables:
#    - VITE_API_URL=https://your-project.railway.app/api
#    - VITE_RAZORPAY_KEY=<your_key>
#    - VITE_MEDIA_URL=https://yourdomain.com/media
# 6. Deploy
```

### 6.3 Verify Frontend Deployment

```bash
# Visit your Vercel URL
# https://your-app.vercel.app

# Check console for any API errors
# Should connect to Railway backend successfully
```

---

## Step 7: Configure Razorpay Payment Gateway

### 7.1 Get Razorpay Live Credentials

```bash
# 1. Go to https://dashboard.razorpay.com
# 2. Login with your account
# 3. Navigate to Settings → API Keys
# 4. Switch to "Live" mode (toggle in top right)
# 5. Copy:
#    - Key ID: rzp_live_xxxxx
#    - Key Secret: your_secret_key
```

### 7.2 Create Razorpay Webhook

```bash
# In Razorpay Dashboard:
# 1. Go to Settings → Webhooks
# 2. Add new webhook:
#    - URL: https://yourdomain.com/api/webhooks/razorpay
#    - Events to subscribe:
#      * payment.authorized
#      * payment.failed
#      * payment.captured
#      * order.paid
# 3. Save and copy the Webhook Signing Secret
```

### 7.3 Update Environment Variables

Add to Railway Variables:

```bash
RAZORPAY_KEY_ID=rzp_live_your_actual_key
RAZORPAY_KEY_SECRET=your_actual_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

### 7.4 Test Razorpay Integration

```bash
# Create test order
curl -X POST https://your-project.railway.app/api/orders \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 1}]
  }'

# Response should include Razorpay order ID
```

---

## Step 8: Update CORS & Environment Variables

### 8.1 Update CORS Configuration

Your CORS is already configured via `CORS_ORIGINS` environment variable.

**Update in Railway Variables:**

```bash
# For production with Vercel frontend
CORS_ORIGINS=https://your-app.vercel.app,https://www.your-app.vercel.app,https://yourdomain.com

# For local development (add to local .env)
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8000
```

### 8.2 Verify CORS is Working

```bash
# Test CORS preflight request
curl -X OPTIONS https://your-project.railway.app/api/auth/login \
  -H "Origin: https://your-app.vercel.app" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -v

# Should return 200 with CORS headers
```

---

## Step 9: Testing & Validation

### 9.1 Test Backend API

```bash
# 1. Health check
curl https://your-project.railway.app/api/actuator/health

# 2. Get products (public endpoint)
curl https://your-project.railway.app/api/products?page=0&size=10

# 3. Register user
curl -X POST https://your-project.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePassword123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# 4. Login
RESPONSE=$(curl -X POST https://your-project.railway.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePassword123!"
  }')

JWT_TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

# 5. Test protected endpoint
curl https://your-project.railway.app/api/users/profile \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### 9.2 Test Media Upload

```bash
# Upload an image
curl -X POST https://your-project.railway.app/api/media/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/image.jpg"

# Response should return media URL:
# {"url": "https://yourdomain.com/media/uuid-filename.jpg", "filename": "...", "size": ...}

# Verify image is accessible from Nginx
curl https://yourdomain.com/media/uuid-filename.jpg
```

### 9.3 Test Payment Flow

```bash
# 1. Create an order
curl -X POST https://your-project.railway.app/api/orders \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 2}]
  }'

# Response includes Razorpay order ID

# 2. In frontend, use Razorpay SDK to complete payment
# See Razorpay integration code in your React component
```

### 9.4 Test Database Connectivity

```bash
# Railway automatically manages the connection
# If you want to verify directly:

# From your local machine with railway CLI:
railway shell

# Inside Railway shell:
psql $DATABASE_URL

# Query:
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM products;
```

---

## Step 10: Production Monitoring

### 10.1 Railway Monitoring

```bash
# In Railway Dashboard:
# 1. Click on your service
# 2. View "Deployments" for deployment history
# 3. Check "Logs" tab for real-time logs
# 4. Monitor "Metrics" for CPU, Memory, Network
```

### 10.2 Set Up Uptime Monitoring

```bash
# Use services like:
# - UptimeRobot (free tier available)
# - Statuspage.io
# - PagerDuty

# Monitor these endpoints:
# - https://your-project.railway.app/api/actuator/health
# - https://yourdomain.com/health
# - https://your-app.vercel.app
```

### 10.3 Enable Nginx Logging

```bash
# Nginx logs are at:
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# Configure logrotate for automatic rotation
sudo tee /etc/logrotate.d/perfume-shop > /dev/null << 'EOF'
/var/log/perfume-shop/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    create 0640 www-data www-data
    sharedscripts
    postrotate
        nginx -s reload > /dev/null 2>&1 || true
    endscript
}
EOF
```

### 10.4 Setup Email Alerts

```bash
# Add to Railway Variables to enable error notifications:
# - Configure SendGrid webhook for bounce/complaint tracking
# - Set up email alerts on critical errors

# Or use Sentry for error tracking:
# 1. Go to https://sentry.io
# 2. Create project for Spring Boot
# 3. Add to application-prod.yml:
sentry:
  dsn: ${SENTRY_DSN:https://your-key@sentry.io/project}
  traces-sample-rate: 0.1
```

---

## Quick Reference - All Commands

### Build & Deploy Backend

```bash
# 1. Update code with PostgreSQL driver
# 2. Commit to GitHub
git add .
git commit -m "Configure PostgreSQL and media upload"
git push origin main

# 3. Railway auto-deploys from GitHub
# 4. Verify deployment
curl https://your-project.railway.app/api/actuator/health
```

### Deploy Frontend

```bash
# Option 1: Vercel CLI
cd frontend
npm run build
vercel --prod

# Option 2: GitHub auto-deployment
# Just push to main, Vercel auto-deploys
git push origin main
```

### Configure Nginx Media

```bash
# 1. Copy Nginx config
sudo cp perfume-shop-media.conf /etc/nginx/sites-available/

# 2. Enable site
sudo ln -s /etc/nginx/sites-available/perfume-shop-media.conf \
            /etc/nginx/sites-enabled/

# 3. Test and reload
sudo nginx -t
sudo systemctl reload nginx
```

### Environment Variables Setup

**Railway Dashboard Variables:**
```
DATABASE_URL=postgresql://...
JWT_SECRET=<generated-key>
CORS_ORIGINS=https://your-app.vercel.app
RAZORPAY_KEY_ID=rzp_live_xxx
RAZORPAY_KEY_SECRET=xxx
RAZORPAY_WEBHOOK_SECRET=xxx
FRONTEND_URL=https://your-app.vercel.app
MEDIA_URL=https://yourdomain.com/media
MAIL_HOST=smtp.sendgrid.net
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.xxx
SPRING_PROFILES_ACTIVE=prod
```

---

## Troubleshooting

### Issue: Media files not accessible from Nginx

**Solution:**
```bash
# Check directory exists and has correct permissions
ls -la /var/www/perfume-shop/media/

# Fix permissions if needed
sudo chown -R www-data:www-data /var/www/perfume-shop
sudo chmod -R 755 /var/www/perfume-shop

# Verify Nginx can read
sudo nginx -t
sudo systemctl reload nginx
```

### Issue: Railway PostgreSQL connection timeout

**Solution:**
```bash
# Check Railway variables have correct DATABASE_URL format
# Should be: postgresql://user:password@host:5432/database

# Test connection string locally:
psql "postgresql://user:password@host:5432/database"
```

### Issue: CORS errors from Vercel frontend

**Solution:**
```bash
# Update CORS_ORIGINS in Railway Variables
# Must include exact Vercel domain:
CORS_ORIGINS=https://your-app.vercel.app,https://www.your-app.vercel.app

# Reload Railway app after changing variables
# Click "Redeploy" in Railway dashboard
```

### Issue: Payment webhook not working

**Solution:**
```bash
# Verify webhook URL in Razorpay dashboard:
# https://yourdomain.com/api/webhooks/razorpay

# Check Nginx is proxying correctly:
curl -X POST https://yourdomain.com/api/webhooks/razorpay

# Check Railway logs for webhook processing
# In Railway Dashboard: Logs tab
```

---

## Architecture Summary

```
┌─────────────────────────────────────────────────────────────┐
│                   User Browser                              │
└───────────────────────────┬─────────────────────────────────┘
                            │
                            ├─ Static Files
                            │     ↓
                ┌───────────────────────────┐
                │   Vercel Frontend         │
                │  (your-app.vercel.app)    │
                └───────────────┬───────────┘
                                │
                    API Calls + Media Requests
                                │
                 ┌──────────────────────────────┐
                 │   Nginx Reverse Proxy        │
                 │  (yourdomain.com)            │
                 ├──────────────────────────────┤
                 │ /api/* → Railway Backend     │
                 │ /media/* → Local FS          │
                 └──────────────┬───────────────┘
                                │
                    ┌───────────┬──────────────┐
                    ↓           ↓              ↓
        ┌──────────────────┐  ┌──────────┐  ┌──────────────┐
        │ Railway Backend  │  │ Local    │  │ Razorpay     │
        │ (Spring Boot)    │  │ FS (/...)│  │ Payments     │
        │ Port: 8080       │  │ Media    │  │              │
        └────────┬─────────┘  └──────────┘  └──────────────┘
                 │
        ┌────────v──────────┐
        │   PostgreSQL      │
        │   (Railway DB)    │
        └───────────────────┘
```

---

## Deployment Checklist

- [ ] PostgreSQL database created on Railway
- [ ] Backend code updated with PostgreSQL driver
- [ ] JWT secret generated
- [ ] application-prod.yml created
- [ ] Procfile created for Railway
- [ ] Railway environment variables configured
- [ ] Backend deployed to Railway
- [ ] Backend API verified (health check, products endpoint)
- [ ] Frontend built for production
- [ ] Frontend deployed to Vercel
- [ ] Nginx installed on media server
- [ ] Nginx configuration created and enabled
- [ ] SSL certificate obtained and configured
- [ ] Media directory created with proper permissions
- [ ] Razorpay live credentials obtained
- [ ] Razorpay webhook configured
- [ ] CORS origins updated for Vercel domain
- [ ] Email service configured
- [ ] All environment variables in Railway
- [ ] Full end-to-end testing completed
- [ ] Monitoring setup completed
- [ ] Database backups configured

---

## Support & Resources

- **Railway Docs**: https://docs.railway.app
- **Vercel Docs**: https://vercel.com/docs
- **Razorpay Docs**: https://razorpay.com/docs
- **Nginx Docs**: https://nginx.org/en/docs/

