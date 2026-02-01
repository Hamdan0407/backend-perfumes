# Production Configuration Summary

Quick reference guide for all production configuration files and settings.

## File Structure

```
perfume-shop/
├── .env.production.example          # Backend environment variables template
├── src/main/resources/
│   ├── application.yml              # Development configuration
│   ├── application-prod.yml         # Production configuration
│   └── logback-spring.xml           # Logging configuration
├── frontend/
│   ├── .env.production.example      # Frontend environment variables template
│   ├── vite.config.js               # Frontend build configuration
│   ├── Dockerfile                   # Frontend Docker image
│   └── nginx.conf                   # Frontend web server config
├── Dockerfile                        # Backend Docker image
├── docker-compose.yml               # Docker Compose production setup
├── .github/workflows/
│   └── production.yml               # CI/CD pipeline
├── PRODUCTION_DEPLOYMENT.md         # Deployment guide
└── pom.xml                          # Maven dependencies
```

## Configuration Files

### Backend Configuration

#### 1. **application-prod.yml**
**Location:** `src/main/resources/application-prod.yml`

**Key Sections:**
```yaml
spring.datasource      # Database connection pooling
spring.jpa             # Hibernate performance tuning
spring.mail            # Email configuration
spring.compression     # Response compression
server.tomcat          # Tomcat server tuning
server.error           # Error handling
management             # Actuator endpoints
app.jwt                # JWT token settings
app.security           # Security headers and CORS
app.stripe             # Stripe payment gateway
app.razorpay           # Razorpay payment gateway
logging                # Log levels and file output
```

**Active Profile:** Set via environment variable
```bash
export SPRING_PROFILES_ACTIVE=prod
```

#### 2. **logback-spring.xml**
**Location:** `src/main/resources/logback-spring.xml`

**Features:**
- Async file appending for performance
- Rolling file appenders with daily rotation
- Separate error and security logs
- Spring profile-specific logging levels
- Log retention: 7 days / 5GB total
- Archive compression with gzip

**Log Files Created:**
```
/var/log/perfume-shop/
├── perfume-shop.log        # Application logs (100MB max)
├── error.log               # Error logs only
├── security.log            # Security audit events
├── access.log              # HTTP access logs
└── archive/                # Compressed archives (7 days)
```

#### 3. **.env.production.example**
**Location:** `.env.production.example`

**Environment Variables:**
```bash
# Server
PORT=8080
ENVIRONMENT=production

# Database
DATABASE_URL=jdbc:mysql://host:3306/perfume_shop?...
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=<16+ char password>
DATABASE_POOL_SIZE=20

# JWT
JWT_SECRET=<base64 256-bit key>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Security
CORS_ORIGINS=https://yourdomain.com
PASSWORD_ENCODER_STRENGTH=12

# Email
MAIL_HOST=smtp.example.com
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=<app-password>

# Payment Gateways
STRIPE_API_KEY=sk_live_...
RAZORPAY_KEY_ID=rzp_live_...

# Frontend
FRONTEND_URL=https://yourdomain.com

# Logging
LOG_DIR=/var/log/perfume-shop
LOG_LEVEL=INFO
```

### Frontend Configuration

#### 1. **vite.config.js**
**Location:** `frontend/vite.config.js`

**Production Features:**
- Code splitting (vendor chunks)
- Tree shaking (unused code removal)
- Terser minification
- Console/debugger removal
- Asset hashing (cache busting)
- CSS code splitting
- Optimized chunk sizes

**Build Command:**
```bash
npm run build
```

**Output:** `frontend/dist/`

#### 2. **.env.production.example**
**Location:** `frontend/.env.production.example`

**Frontend Variables:**
```bash
REACT_APP_API_URL=https://api.yourdomain.com/api
REACT_APP_STRIPE_PUBLISHABLE_KEY=pk_live_...
REACT_APP_ENVIRONMENT=production
REACT_APP_DEBUG_MODE=false
```

#### 3. **Dockerfile** (Frontend)
**Location:** `frontend/Dockerfile`

**Build Process:**
1. Multi-stage build (builder + nginx)
2. Node 18 Alpine for small image size
3. Nginx Alpine for serving static files
4. Health check endpoint

#### 4. **nginx.conf**
**Location:** `frontend/nginx.conf`

**Features:**
- Gzip compression
- Cache busting for assets
- React Router SPA routing (all routes → index.html)
- Security headers
- Static file caching (1 year for assets)
- Performance optimizations

## Docker Deployment

### Backend Dockerfile
**Location:** `Dockerfile`

**Specifications:**
- Base: Eclipse Temurin JDK 17 Alpine
- Non-root user: perfume
- Exposed ports: 8080 (API), 9090 (Management)
- Health check: Every 30s
- JVM memory: -Xmx1g -Xms512m

### Docker Compose
**Location:** `docker-compose.yml`

**Services:**
- **database**: MySQL 8.0 Alpine with health checks
- **api**: Spring Boot application with environment variables
- **frontend** (optional): Nginx serving React app
- **nginx** (optional): Reverse proxy with SSL

**Volumes:**
- mysql-data: Database persistence
- api-logs: Application logs

**Usage:**
```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop services
docker-compose down
```

## CI/CD Pipeline

### GitHub Actions Workflow
**Location:** `.github/workflows/production.yml`

**Pipeline Stages:**

1. **Backend Tests**
   - Maven test execution
   - MySQL service container
   - Code quality checks

2. **Frontend Tests**
   - npm dependencies install
   - Build verification

3. **Backend Build & Push**
   - Build JAR artifact
   - Docker image build
   - Push to Docker Hub with tags:
     - `yourdockerhub/perfume-shop-api:${SHA}`
     - `yourdockerhub/perfume-shop-api:latest`

4. **Frontend Build & Push**
   - Multi-stage Docker build
   - Push with same tag scheme

5. **Security Scan**
   - Trivy vulnerability scanning
   - Results uploaded to GitHub Security tab

6. **Deploy to Production**
   - SSH into production server
   - Pull and start Docker containers
   - Run smoke tests

**Required GitHub Secrets:**
```
DOCKERHUB_USERNAME      # Docker Hub username
DOCKERHUB_TOKEN         # Docker Hub access token
DEPLOY_KEY             # SSH private key
DEPLOY_USER            # SSH username (e.g., ubuntu)
DEPLOY_HOST            # Server IP/domain
```

## Security Configuration

### Security Headers (Set in Application)
```yaml
Strict-Transport-Security: max-age=31536000
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
```

### Database Security
- SSL connection required
- Strong password (16+ characters)
- Connection pool size limited
- Read-only user for specific operations (optional)

### API Authentication
- JWT with HS256 algorithm
- Access token: 24 hours
- Refresh token: 7 days
- Token refresh queue for concurrent requests

### CORS Configuration
```yaml
CORS_ORIGINS: https://yourdomain.com,https://www.yourdomain.com
CORS_MAX_AGE: 3600 seconds
```

## Performance Tuning

### Database Connection Pool
```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 10s
  idle-timeout: 5 minutes
  max-lifetime: 30 minutes
```

### Tomcat Server
```yaml
server.tomcat:
  max-threads: 200
  accept-count: 100
  connection-timeout: 20s
  keep-alive-timeout: 60s
```

### JVM Tuning
```bash
JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### Compression
- Min response size: 1KB
- Supported types: JSON, HTML, CSS, JavaScript, XML
- Nginx gzip level: 6

## Logging Strategy

### Log Levels
- **Application**: INFO
- **Spring Framework**: WARN
- **Hibernate**: WARN
- **Database Pool**: WARN
- **Third-party**: WARN

### Log Retention
- Max file size: 100 MB
- Retention period: 7 days
- Total capacity: 5 GB
- Compression: gzip

### Log Files
- **perfume-shop.log**: Main application logs
- **error.log**: Errors only (for alerting)
- **security.log**: Authentication and authorization events
- **access.log**: HTTP request/response logs

## Monitoring & Health Checks

### Health Check Endpoints
```bash
# Health status
GET /actuator/health
Response: {"status":"UP","components":{...}}

# Application info
GET /actuator/info

# Metrics
GET /actuator/metrics

# Database connections
GET /actuator/metrics/db.connection.active

# Memory usage
GET /actuator/metrics/jvm.memory.used
```

### Expected Response Times
- Health check: < 100ms
- Product list: < 500ms
- Product detail: < 300ms
- Authentication: < 200ms

## Deployment Checklist

- [ ] Database created and credentials configured
- [ ] JWT secret generated (256-bit)
- [ ] Email SMTP configured
- [ ] Payment gateways configured (Stripe, Razorpay)
- [ ] CORS origins set to production domain
- [ ] Environment variables set
- [ ] Database backups configured
- [ ] Log directory created with proper permissions
- [ ] HTTPS/TLS configured
- [ ] Security headers enabled
- [ ] Monitoring and alerting configured
- [ ] Disaster recovery plan documented
- [ ] Load testing performed
- [ ] Security testing completed

## File Permissions

```bash
# Log directory
chmod 755 /var/log/perfume-shop
chown perfume:perfume /var/log/perfume-shop

# JAR file
chmod 755 /opt/perfume-shop/perfume-shop-1.0.0.jar

# Systemd service
chmod 644 /etc/systemd/system/perfume-shop.service

# Backup script
chmod 755 /opt/perfume-shop/backup.sh
```

## Database Initialization

```sql
CREATE DATABASE perfume_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'prod_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON perfume_shop.* TO 'prod_user'@'localhost';
FLUSH PRIVILEGES;

-- Verify timezone support
SELECT @@time_zone;
```

## Backup Strategy

```bash
#!/bin/bash
# Daily backup script
BACKUP_DIR="/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mysqldump -u prod_user -p perfume_shop | \
    gzip > $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql.gz

# Keep only 7 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

## Scaling Considerations

### Horizontal Scaling
- Load balancer (Nginx, HAProxy)
- Multiple API instances
- Shared database (RDS, managed MySQL)
- Redis cache (optional)

### Vertical Scaling
- Increase JVM heap size (-Xmx)
- Increase connection pool size
- Increase Tomcat thread count
- Upgrade server resources

## Troubleshooting

**Container won't start:**
```bash
docker logs perfume-shop-api
# Check environment variables, database connectivity
```

**High memory usage:**
```bash
docker stats perfume-shop-api
# Monitor JVM heap, consider increasing -Xmx
```

**Database connection errors:**
```bash
docker logs perfume-shop-db
# Check credentials, network connectivity
```

**Slow requests:**
```bash
# Check metrics endpoint
curl http://localhost:9090/actuator/metrics/http.server.requests
```

---

## Summary

This production configuration provides:

✅ **Security**: JWT auth, CORS, password hashing, HTTPS-ready
✅ **Performance**: Connection pooling, compression, caching, async logging
✅ **Reliability**: Health checks, graceful shutdown, error handling
✅ **Observability**: Comprehensive logging, metrics, monitoring endpoints
✅ **Scalability**: Docker containerization, load balancer ready
✅ **Maintainability**: Configuration templates, deployment automation
✅ **Compliance**: Secure defaults, audit logging, data protection

All configuration files are template-based. Replace placeholders with your actual values before deployment.
