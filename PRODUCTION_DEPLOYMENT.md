# Production Deployment Configuration Guide

Complete guide for preparing and deploying the Parfumé e-commerce platform to production.

## Table of Contents

1. [Environment Variables](#environment-variables)
2. [Backend Configuration](#backend-configuration)
3. [Frontend Configuration](#frontend-configuration)
4. [Logging Setup](#logging-setup)
5. [Database Setup](#database-setup)
6. [Security Checklist](#security-checklist)
7. [Deployment Steps](#deployment-steps)
8. [Monitoring & Maintenance](#monitoring--maintenance)

---

## Environment Variables

### Backend Environment Variables (`.env.production`)

All backend configuration is managed through environment variables and `application-prod.yml`.

**Required Variables:**

```bash
# Server
ENVIRONMENT=production
PORT=8080

# Database
DATABASE_URL=jdbc:mysql://prod-db.example.com:3306/perfume_shop?useSSL=true&serverTimezone=UTC
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=<strong-password-min-16-chars>
DATABASE_POOL_SIZE=20

# JWT Authentication
JWT_SECRET=<base64-256-bit-secret-key>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Security
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
PASSWORD_ENCODER_STRENGTH=12

# Email
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=<email-app-password>
MAIL_FROM=noreply@yourdomain.com

# Payment Gateways
STRIPE_API_KEY=sk_live_<actual-key>
STRIPE_WEBHOOK_SECRET=whsec_<actual-secret>
RAZORPAY_KEY_ID=rzp_live_<actual-key>
RAZORPAY_KEY_SECRET=<actual-secret>

# Frontend
FRONTEND_URL=https://yourdomain.com

# Logging
LOG_DIR=/var/log/perfume-shop
LOG_LEVEL=INFO
```

### Frontend Environment Variables (`.env.production.local`)

```bash
REACT_APP_API_URL=https://api.yourdomain.com/api
REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_live_<actual-key>
REACT_APP_ENVIRONMENT=production
REACT_APP_DEBUG_MODE=false
```

### Generating JWT Secret

Generate a secure 256-bit secret key:

```bash
# Linux/Mac
openssl rand -base64 32

# Or using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Or using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

---

## Backend Configuration

### Spring Profile Activation

Production uses the `prod` Spring profile. Set it in your deployment:

**Using environment variable:**
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**Using Java system property:**
```bash
java -Dspring.profiles.active=prod -jar perfume-shop-1.0.0.jar
```

**Using application-prod.yml:**
The file `application-prod.yml` contains all production-specific settings.

### Key Production Settings

**Database Configuration:**
- DDL Auto: `validate` (never `update` or `create`)
- Connection pooling: 20 connections max
- Connection timeout: 10 seconds
- Idle timeout: 5 minutes

**JPA/Hibernate:**
- Show SQL: `false`
- Format SQL: `false`
- Batch size: 20
- Fetch size: 50

**Logging:**
- Root level: `INFO`
- Spring level: `WARN`
- Application level: `INFO`
- File output with rotation

**Server:**
- Tomcat max threads: 200
- Keep-alive timeout: 60 seconds
- Error detail level: Minimal (no stack traces)

### Actuator Endpoints

Production exposes minimal actuator endpoints:

```
GET  /actuator/health           - Application health check
GET  /actuator/info             - Application info
GET  /actuator/metrics          - Metrics
```

Management endpoints run on separate port 9090.

---

## Frontend Configuration

### Build Configuration

The `vite.config.js` includes production optimizations:

**Optimization Features:**
- Tree shaking: Removes unused code
- Code splitting: Vendor chunks for better caching
- Minification: Using Terser
- Console removal: No console logs in production
- Asset hashing: For cache busting

**Build Command:**
```bash
npm run build
```

**Output Structure:**
```
dist/
├── index.html
├── js/
│   ├── vendor-react.[hash].js
│   ├── vendor-axios.[hash].js
│   ├── index.[hash].js
│   └── ...
├── css/
│   └── [name].[hash].css
└── images/
    └── ...
```

### Environment Variables

Create `.env.production.local` with:

```env
REACT_APP_API_URL=https://api.yourdomain.com/api
REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_live_XXX
REACT_APP_ENVIRONMENT=production
REACT_APP_DEBUG_MODE=false
```

### API Configuration

The axios instance in `frontend/src/api/axios.js` uses:

```javascript
const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
```

Ensure this points to your production backend.

---

## Logging Setup

### Logback Configuration

The `logback-spring.xml` provides comprehensive logging:

**Log Files Created:**
- `/var/log/perfume-shop/perfume-shop.log` - Application logs
- `/var/log/perfume-shop/error.log` - Error logs only
- `/var/log/perfume-shop/security.log` - Security events
- `/var/log/perfume-shop/access.log` - HTTP access logs
- `/var/log/perfume-shop/archive/` - Compressed rotated logs

**Log Rotation:**
- Max file size: 100 MB
- Max history: 7 days
- Total cap: 5 GB
- Compression: gzip

**Log Levels:**
- Root: `INFO`
- Application: `INFO`
- Spring: `WARN`
- Hibernate: `WARN`

**Async Logging:**
- Queue size: 512
- Non-blocking performance
- Includes MDC context

### Viewing Logs

```bash
# Last 100 lines
tail -100 /var/log/perfume-shop/perfume-shop.log

# Real-time monitoring
tail -f /var/log/perfume-shop/perfume-shop.log

# Search for errors
grep ERROR /var/log/perfume-shop/perfume-shop.log

# Security audit log
cat /var/log/perfume-shop/security.log
```

---

## Database Setup

### Pre-deployment Checklist

1. **Create database:**
```sql
CREATE DATABASE perfume_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. **Create database user:**
```sql
CREATE USER 'prod_user'@'localhost' IDENTIFIED BY 'strong-password';
GRANT ALL PRIVILEGES ON perfume_shop.* TO 'prod_user'@'localhost';
FLUSH PRIVILEGES;
```

3. **Enable required MySQL settings:**
```sql
-- Check timezone support
SELECT @@time_zone;

-- Set timezone if needed
SET GLOBAL time_zone = 'UTC';
```

4. **Backup strategy:**
```bash
# Daily backup script
#!/bin/bash
BACKUP_DIR="/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mysqldump -u prod_user -p perfume_shop | gzip > $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql.gz

# Keep only last 7 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

### Connection Pool Configuration

**application-prod.yml settings:**
```yaml
datasource:
  hikari:
    maximum-pool-size: 20
    minimum-idle: 5
    connection-timeout: 10000ms
    idle-timeout: 300000ms  # 5 minutes
    max-lifetime: 1800000ms # 30 minutes
```

---

## Security Checklist

### Essential Security Settings

- [ ] **JWT Secret**: Generated using cryptographically secure method
- [ ] **HTTPS Only**: All traffic encrypted with TLS 1.2+
- [ ] **CORS Origins**: Restricted to your domain(s)
- [ ] **Password Hashing**: BCrypt strength 12
- [ ] **Database Password**: Strong, 16+ characters
- [ ] **Email Credentials**: App-specific passwords (not account password)
- [ ] **API Keys**: Live keys only, never test keys
- [ ] **Webhook Secrets**: Verified on each request

### Security Headers

Add to web server (Nginx/Apache):

```nginx
# HTTP Strict Transport Security
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# Content Security Policy
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';" always;

# X-Content-Type-Options
add_header X-Content-Type-Options "nosniff" always;

# X-Frame-Options
add_header X-Frame-Options "SAMEORIGIN" always;

# X-XSS-Protection
add_header X-XSS-Protection "1; mode=block" always;
```

### Endpoint Security

**Protected Routes (require authentication):**
- POST `/api/cart`
- PUT `/api/cart/items/{id}`
- DELETE `/api/cart/items/{id}`
- POST `/api/orders`
- GET `/api/orders`
- GET `/api/users/profile`
- PUT `/api/users/profile`

**Public Routes (no authentication):**
- GET `/api/products`
- GET `/api/products/{id}`
- GET `/api/products/featured`
- POST `/api/auth/login`
- POST `/api/auth/register`

**Admin Only Routes:**
- POST `/api/admin/products`
- PUT `/api/admin/products/{id}`
- DELETE `/api/admin/products/{id}`

---

## Deployment Steps

### Backend Deployment (Maven)

1. **Build JAR:**
```bash
mvn clean package -DskipTests -Pproduction
```

2. **Copy to production server:**
```bash
scp target/perfume-shop-1.0.0.jar user@prod-server:/opt/perfume-shop/
```

3. **Create systemd service** (`/etc/systemd/system/perfume-shop.service`):
```ini
[Unit]
Description=Perfume Shop API
After=network.target

[Service]
Type=simple
User=app
WorkingDirectory=/opt/perfume-shop
EnvironmentFile=/opt/perfume-shop/.env.production
ExecStart=/usr/bin/java -Dspring.profiles.active=prod -jar perfume-shop-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

4. **Start service:**
```bash
sudo systemctl daemon-reload
sudo systemctl enable perfume-shop
sudo systemctl start perfume-shop
```

5. **Monitor:**
```bash
sudo systemctl status perfume-shop
journalctl -u perfume-shop -f
```

### Frontend Deployment (Vite)

1. **Build:**
```bash
REACT_APP_API_URL=https://api.yourdomain.com/api npm run build
```

2. **Deploy to CDN or static host:**
```bash
# Using AWS S3
aws s3 sync dist/ s3://your-bucket/ --delete

# Using Netlify
netlify deploy --prod --dir=dist

# Using GitHub Pages
npm run build && git add dist/ && git commit && git push
```

3. **Configure web server** (Nginx):
```nginx
server {
    listen 443 ssl;
    server_name yourdomain.com;
    
    root /var/www/parfume;
    index index.html;
    
    # React Router SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API proxy
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # Security headers
    add_header Strict-Transport-Security "max-age=31536000" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
}
```

---

## Monitoring & Maintenance

### Health Checks

```bash
# Application health
curl https://yourdomain.com/actuator/health

# Response:
# {"status":"UP","components":{...}}
```

### Performance Monitoring

Monitor these metrics:

```bash
# Response times
curl https://yourdomain.com/actuator/metrics/http.server.requests

# Database connections
curl https://yourdomain.com/actuator/metrics/db.connection.active

# Memory usage
curl https://yourdomain.com/actuator/metrics/jvm.memory.used
```

### Log Monitoring

```bash
# Watch for errors
grep ERROR /var/log/perfume-shop/perfume-shop.log | tail -20

# Monitor security events
tail -f /var/log/perfume-shop/security.log
```

### Database Maintenance

```bash
# Check database size
mysql -u prod_user -p -e "SELECT table_name, ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb FROM information_schema.TABLES WHERE table_schema = 'perfume_shop' ORDER BY size_mb DESC;"

# Optimize tables
OPTIMIZE TABLE users, products, orders, ...;

# Run backups
bash /opt/perfume-shop/backup.sh
```

### Dependency Updates

- Check for security updates monthly
- Test updates in staging environment first
- Use `mvn dependency-check` to scan for vulnerabilities

### Troubleshooting

**Database Connection Issues:**
```bash
# Check connectivity
mysql -h prod-db.example.com -u prod_user -p perfume_shop -e "SELECT 1"

# Check pool status in logs
grep HikariPool /var/log/perfume-shop/perfume-shop.log
```

**Memory Issues:**
```bash
# Check JVM memory
jps -l
jmap -heap <pid>

# Increase if needed (systemd service)
# ExecStart=/usr/bin/java -Xmx2g -Xms1g ...
```

**Log Space Issues:**
```bash
# Check disk usage
du -sh /var/log/perfume-shop/

# Manually clean old logs
find /var/log/perfume-shop/archive/ -name "*.gz" -mtime +7 -delete
```

---

## Environment-Specific Configuration

### Development (`application.yml`)
- Debug logging
- Show SQL statements
- CORS: localhost
- Dev email settings

### Staging (`application-staging.yml`)
- Info logging
- Production-like database
- Staging domain CORS
- Test payment gateways

### Production (`application-prod.yml`)
- Info logging only
- Validate schema only
- Production domain CORS
- Live payment gateways
- Comprehensive security

---

## Summary

**Before deploying to production:**

1. ✅ Set all environment variables
2. ✅ Configure database with strong credentials
3. ✅ Generate secure JWT secret
4. ✅ Set up logging directories with proper permissions
5. ✅ Configure email service
6. ✅ Configure payment gateway credentials
7. ✅ Set CORS origins
8. ✅ Enable HTTPS/TLS
9. ✅ Configure backups
10. ✅ Test in staging environment
11. ✅ Monitor logs and metrics
12. ✅ Set up alerting for errors

All configuration files are provided in the repository. Customize them for your deployment environment.
