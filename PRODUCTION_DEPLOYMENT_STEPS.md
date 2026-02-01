# Production Deployment Steps - Parfumé E-Commerce Platform

**Project**: Perfume Shop (Parfumé) - Full-Stack E-Commerce Application  
**Technology**: Spring Boot 3.2.1 + React + MySQL + Docker  
**Last Updated**: January 2026

---

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Phase 1: Infrastructure Setup](#phase-1-infrastructure-setup)
3. [Phase 2: Database Configuration](#phase-2-database-configuration)
4. [Phase 3: Security & Environment Variables](#phase-3-security--environment-variables)
5. [Phase 4: Backend Application Build & Deployment](#phase-4-backend-application-build--deployment)
6. [Phase 5: Frontend Deployment](#phase-5-frontend-deployment)
7. [Phase 6: Payment Gateway Integration](#phase-6-payment-gateway-integration)
8. [Phase 7: Email Service Setup](#phase-7-email-service-setup)
9. [Phase 8: Monitoring & Logging](#phase-8-monitoring--logging)
10. [Phase 9: Testing & Validation](#phase-9-testing--validation)
11. [Phase 10: Post-Deployment](#phase-10-post-deployment)

---

## Pre-Deployment Checklist

### Required Access & Accounts

- [ ] **AWS Account** (or cloud provider of choice - AWS/Azure/GCP)
- [ ] **Domain Name** (e.g., yourdomain.com)
- [ ] **SSL Certificate** (obtained from Let's Encrypt or AWS Certificate Manager)
- [ ] **Stripe Account** (Live mode, not test)
- [ ] **Razorpay Account** (Live mode API keys)
- [ ] **Email Service** (Gmail, SendGrid, AWS SES)
- [ ] **GitHub Repository** (source code backup)
- [ ] **Docker Hub Account** (optional, for private registry)
- [ ] **Monitoring Service** (CloudWatch, New Relic, DataDog)

### Code Review Checklist

- [ ] All environment variables set to use production values
- [ ] No hardcoded secrets in codebase
- [ ] Error responses don't expose stack traces
- [ ] CORS origins properly configured
- [ ] Logging level set to INFO/WARN (not DEBUG)
- [ ] Database DDL-Auto set to `validate` (never `create` or `update`)
- [ ] Unit and integration tests pass
- [ ] Security dependencies up to date

### Assumptions

- You have Docker and Docker Compose installed locally
- You have Maven 3.9+ and Java 17+ installed
- You have AWS CLI configured or access to your chosen cloud platform
- You have SSH access to your production server(s)

---

## Phase 1: Infrastructure Setup

### Step 1.1: Choose Deployment Method

**Option A: Using Docker on AWS EC2** (Recommended for simplicity)
- EC2 instance (t3.medium or larger)
- Docker and Docker Compose installed
- Security groups configured

**Option B: Using AWS Elastic Beanstalk**
- Managed platform
- Auto-scaling configured
- No server management needed

**Option C: Using Kubernetes (EKS)**
- For large-scale deployments
- Complex but highly scalable

**For this guide, we'll use Option A (Docker on EC2).**

### Step 1.2: Launch EC2 Instance

```bash
# AWS CLI command to launch EC2 instance
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --key-name your-key-pair \
  --security-groups perfume-shop-security-group \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=perfume-shop-prod}]'
```

### Step 1.3: Configure Security Group

```bash
# Allow HTTP (80)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow HTTPS (443)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Allow SSH (22) - from your IP only
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxx \
  --protocol tcp \
  --port 22 \
  --cidr YOUR_IP/32
```

### Step 1.4: Connect to EC2 Instance

```bash
# SSH into the instance
ssh -i /path/to/your-key.pem ec2-user@your-ec2-ip

# Update system packages
sudo yum update -y

# Install Docker
sudo yum install -y docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Install git for cloning repository
sudo yum install -y git

# Verify installations
docker --version
docker-compose --version
```

### Step 1.5: Configure Reverse Proxy (Nginx)

```bash
# Install Nginx
sudo yum install -y nginx

# Create Nginx configuration
sudo tee /etc/nginx/conf.d/perfume-shop.conf > /dev/null << 'EOF'
upstream perfume_api {
    server api:8080;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    client_max_body_size 10M;

    location / {
        proxy_pass http://perfume_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 60s;
        proxy_connect_timeout 10s;
    }

    location /health {
        proxy_pass http://perfume_api/actuator/health;
        access_log off;
    }
}
EOF

# Enable Nginx
sudo systemctl start nginx
sudo systemctl enable nginx
```

### Step 1.6: Set Up SSL/TLS Certificate (Let's Encrypt)

```bash
# Install Certbot
sudo yum install -y certbot python3-certbot-nginx

# Obtain certificate
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com

# Update Nginx configuration for HTTPS
sudo tee /etc/nginx/conf.d/perfume-shop.conf > /dev/null << 'EOF'
upstream perfume_api {
    server api:8080;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    client_max_body_size 10M;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    location / {
        proxy_pass http://perfume_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 60s;
        proxy_connect_timeout 10s;
    }

    location /health {
        proxy_pass http://perfume_api/actuator/health;
        access_log off;
    }
}
EOF

# Reload Nginx
sudo systemctl reload nginx

# Auto-renew certificates
sudo systemctl enable certbot-renew.timer
sudo systemctl start certbot-renew.timer
```

---

## Phase 2: Database Configuration

### Step 2.1: Choose Database Service

**Option A: Managed AWS RDS (Recommended)**
```bash
# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier perfume-shop-db \
  --db-instance-class db.t3.micro \
  --engine mysql \
  --engine-version 8.0 \
  --allocated-storage 100 \
  --master-username admin \
  --master-user-password 'YOUR_SECURE_PASSWORD' \
  --db-name perfume_shop \
  --publicly-accessible false \
  --multi-az true \
  --backup-retention-period 7 \
  --storage-encrypted true
```

**Option B: Docker MySQL in Docker Compose**
```bash
# Handled in Phase 4, Step 4.1
```

### Step 2.2: Create Database User & Permissions

**If using RDS:**

```bash
# Connect to RDS instance
mysql -h perfume-shop-db.c9akciq32.us-east-1.rds.amazonaws.com \
       -u admin -p perfume_shop

# In MySQL CLI, create production user:
CREATE USER 'prod_user'@'%' IDENTIFIED BY 'strong_password_min_16_chars';
GRANT ALL PRIVILEGES ON perfume_shop.* TO 'prod_user'@'%';
FLUSH PRIVILEGES;
```

**If using Docker:**
```bash
# Database is created automatically in docker-compose.yml
```

### Step 2.3: Initialize Database Schema

```bash
# Connection string format:
jdbc:mysql://perfume-shop-db.c9akciq32.us-east-1.rds.amazonaws.com:3306/perfume_shop?useSSL=true&serverTimezone=UTC

# Application will create tables on first run with DDL-Auto set to 'validate'
# Initial startup creates the schema if it doesn't exist
```

### Step 2.4: Database Backup Configuration

```bash
# For RDS: Configure automated backups (done in Step 2.1)

# For Docker: Create backup script
sudo tee /home/ec2-user/backup-database.sh > /dev/null << 'EOF'
#!/bin/bash
BACKUP_DIR="/var/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

docker compose exec -T database mysqldump \
  -u prod_user -p$DATABASE_PASSWORD perfume_shop \
  > $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql

# Keep only last 7 backups
find $BACKUP_DIR -name "perfume_shop_*.sql" -mtime +7 -delete
EOF

sudo chmod +x /home/ec2-user/backup-database.sh

# Add to crontab for daily backups at 2 AM
(crontab -l 2>/dev/null; echo "0 2 * * * /home/ec2-user/backup-database.sh") | crontab -
```

---

## Phase 3: Security & Environment Variables

### Step 3.1: Generate Required Secrets

```bash
# SSH into EC2 instance
ssh -i /path/to/your-key.pem ec2-user@your-ec2-ip

# 1. Generate JWT Secret (256-bit)
openssl rand -base64 32
# Output example: AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQr

# 2. Generate secure passwords
openssl rand -base64 16
# Output example: XyZ123AbCdEfGhIj

# Note: Save all generated secrets in a secure location
```

### Step 3.2: Create .env.production File

```bash
# On your EC2 instance, create the environment file
sudo mkdir -p /app/perfume-shop
cd /app/perfume-shop

cat > .env.production << 'EOF'
# ==========================================
# SERVER CONFIGURATION
# ==========================================
ENVIRONMENT=production
PORT=8080
SPRING_PROFILES_ACTIVE=prod

# ==========================================
# DATABASE CONFIGURATION
# ==========================================
DATABASE_URL=jdbc:mysql://perfume-shop-db.c9akciq32.us-east-1.rds.amazonaws.com:3306/perfume_shop?useSSL=true&serverTimezone=UTC
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=YOUR_GENERATED_DB_PASSWORD
DATABASE_POOL_SIZE=20
DATABASE_CONNECTION_TIMEOUT=10000
DATABASE_MAX_IDLE_TIME=300000

# ==========================================
# JWT CONFIGURATION
# ==========================================
JWT_SECRET=YOUR_GENERATED_JWT_SECRET
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
JWT_GRACE_PERIOD=60000

# ==========================================
# SECURITY CONFIGURATION
# ==========================================
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
PASSWORD_ENCODER_STRENGTH=12
CORS_MAX_AGE=3600

# ==========================================
# EMAIL CONFIGURATION
# ==========================================
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=YOUR_EMAIL_APP_PASSWORD
MAIL_FROM=noreply@yourdomain.com
MAIL_FROM_NAME=Parfumé
EMAIL_MAX_RETRIES=3
EMAIL_RETRY_DELAY=5000

# ==========================================
# PAYMENT GATEWAY - STRIPE
# ==========================================
STRIPE_API_KEY=sk_live_YOUR_ACTUAL_STRIPE_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_STRIPE_WEBHOOK_SECRET

# ==========================================
# PAYMENT GATEWAY - RAZORPAY
# ==========================================
RAZORPAY_KEY_ID=rzp_live_YOUR_RAZORPAY_KEY
RAZORPAY_KEY_SECRET=YOUR_RAZORPAY_SECRET
RAZORPAY_WEBHOOK_SECRET=YOUR_RAZORPAY_WEBHOOK_SECRET
RAZORPAY_CURRENCY=INR

# ==========================================
# FRONTEND CONFIGURATION
# ==========================================
FRONTEND_URL=https://yourdomain.com

# ==========================================
# LOGGING CONFIGURATION
# ==========================================
LOG_LEVEL=INFO
LOG_DIR=/var/log/perfume-shop
LOG_MAX_FILE_SIZE=100MB
LOG_MAX_BACKUP_INDEX=7
SPRING_LOG_LEVEL=WARN
EOF

# Set restricted permissions
sudo chmod 600 .env.production
sudo chown ec2-user:ec2-user .env.production
```

### Step 3.3: Store Secrets Securely

```bash
# Option 1: AWS Secrets Manager (Recommended)
aws secretsmanager create-secret \
  --name perfume-shop/prod \
  --description "Production environment variables for Perfume Shop" \
  --secret-string file://.env.production

# Option 2: AWS Systems Manager Parameter Store
aws ssm put-parameter \
  --name /perfume-shop/prod/jwt-secret \
  --value "YOUR_JWT_SECRET" \
  --type SecureString

# Option 3: Local .env file (less secure, but convenient for this setup)
# Already created above - ensure proper file permissions
```

### Step 3.4: Create docker-compose.yml for Production

```bash
cat > /app/perfume-shop/docker-compose.yml << 'EOF'
version: '3.8'

services:
  database:
    image: mysql:8.0
    container_name: perfume-shop-db
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: perfume_shop
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD:-root}
      MYSQL_USER: ${DATABASE_USERNAME}
      MYSQL_PASSWORD: ${DATABASE_PASSWORD}
      TZ: 'UTC'
    
    ports:
      - "3307:3306"
    
    volumes:
      - mysql-data:/var/lib/mysql
    
    networks:
      - perfume-network
    
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M

  api:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        JAR_FILE: target/perfume-shop-1.0.0.jar
    
    container_name: perfume-shop-api
    restart: unless-stopped
    
    depends_on:
      database:
        condition: service_started
    
    environment:
      SPRING_PROFILES_ACTIVE: prod
      PORT: 8080
      SERVER_PORT: 8080
      ENVIRONMENT: production
      
      # Database
      DATABASE_URL: ${DATABASE_URL}
      DATABASE_USERNAME: ${DATABASE_USERNAME}
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      DATABASE_POOL_SIZE: ${DATABASE_POOL_SIZE:-20}
      
      # JWT
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: ${JWT_EXPIRATION:-86400000}
      JWT_REFRESH_EXPIRATION: ${JWT_REFRESH_EXPIRATION:-604800000}
      
      # Security
      CORS_ORIGINS: ${CORS_ORIGINS}
      PASSWORD_ENCODER_STRENGTH: ${PASSWORD_ENCODER_STRENGTH:-12}
      
      # Email
      MAIL_HOST: ${MAIL_HOST:-smtp.gmail.com}
      MAIL_PORT: ${MAIL_PORT:-587}
      MAIL_USERNAME: ${MAIL_USERNAME}
      MAIL_PASSWORD: ${MAIL_PASSWORD}
      MAIL_FROM: ${MAIL_FROM}
      
      # Payments
      STRIPE_API_KEY: ${STRIPE_API_KEY}
      STRIPE_WEBHOOK_SECRET: ${STRIPE_WEBHOOK_SECRET}
      RAZORPAY_KEY_ID: ${RAZORPAY_KEY_ID}
      RAZORPAY_KEY_SECRET: ${RAZORPAY_KEY_SECRET}
      RAZORPAY_WEBHOOK_SECRET: ${RAZORPAY_WEBHOOK_SECRET}
      
      # Frontend
      FRONTEND_URL: ${FRONTEND_URL}
      
      # Logging
      LOG_LEVEL: ${LOG_LEVEL:-INFO}
      LOG_DIR: ${LOG_DIR:-/var/log/perfume-shop}
      
      # JVM
      JAVA_OPTS: "-Xmx1g -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    
    ports:
      - "8080:8080"
      - "9090:9090"
    
    volumes:
      - /var/log/perfume-shop:/var/log/perfume-shop
      - ./logs:/app/logs
    
    networks:
      - perfume-network
    
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 45s
    
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G

volumes:
  mysql-data:
    driver: local

networks:
  perfume-network:
    driver: bridge
EOF
```

### Step 3.5: Set File Permissions

```bash
# Secure the environment file
sudo chmod 600 /app/perfume-shop/.env.production
sudo chown ec2-user:ec2-user /app/perfume-shop/.env.production

# Secure the entire directory
sudo chmod 755 /app/perfume-shop
```

---

## Phase 4: Backend Application Build & Deployment

### Step 4.1: Clone Repository

```bash
cd /app/perfume-shop

# Clone the repository (replace with your repo)
git clone https://github.com/yourusername/perfume-shop.git .

# Or if already in repo, just pull latest
git pull origin main
```

### Step 4.2: Build Docker Image

```bash
cd /app/perfume-shop

# Build the Docker image
docker build -t perfume-shop:latest .

# Or with version tag
docker build -t perfume-shop:1.0.0 .
docker tag perfume-shop:1.0.0 perfume-shop:latest

# Verify image was created
docker images | grep perfume-shop
```

### Step 4.3: Start Services with Docker Compose

```bash
cd /app/perfume-shop

# Load environment variables and start services
set -a
source .env.production
set +a

# Start services (builds images if needed)
docker compose up -d

# Check if services are running
docker compose ps

# View logs
docker compose logs -f api
```

### Step 4.4: Verify Backend Startup

```bash
# Wait 30-45 seconds for startup
sleep 45

# Check health endpoint
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"}...}}

# Check specific endpoints
curl http://localhost:8080/actuator/info
curl http://localhost:8080/api/products?page=0&size=10
```

### Step 4.5: Configure Log Rotation

```bash
# Create log directory
sudo mkdir -p /var/log/perfume-shop
sudo chown ec2-user:ec2-user /var/log/perfume-shop
sudo chmod 755 /var/log/perfume-shop

# Configure logrotate
sudo tee /etc/logrotate.d/perfume-shop > /dev/null << 'EOF'
/var/log/perfume-shop/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    create 0644 ec2-user ec2-user
    sharedscripts
    postrotate
        docker compose exec -T api kill -HUP 1
    endscript
}
EOF
```

### Step 4.6: Create Systemd Service (Optional - for container management)

```bash
# Create systemd service to auto-start containers on reboot
sudo tee /etc/systemd/system/perfume-shop.service > /dev/null << 'EOF'
[Unit]
Description=Perfume Shop Docker Compose Application
Requires=docker.service
After=docker.service

[Service]
Type=simple
WorkingDirectory=/app/perfume-shop
User=ec2-user
ExecStart=/usr/local/bin/docker-compose up
ExecStop=/usr/local/bin/docker-compose down
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable perfume-shop.service
# Don't start yet, we'll do it after frontend setup
```

---

## Phase 5: Frontend Deployment

### Step 5.1: Build Frontend Application

**On your local machine (or CI/CD pipeline):**

```bash
cd /path/to/frontend

# Install dependencies
npm install

# Create production build
npm run build

# Output will be in 'dist' or 'build' folder
```

### Step 5.2: Deploy Frontend to S3 + CloudFront

**Option A: AWS S3 + CloudFront (Recommended)**

```bash
# Create S3 bucket
aws s3 mb s3://perfume-shop-frontend --region us-east-1

# Block public access (CloudFront will serve)
aws s3api put-bucket-public-access-block \
  --bucket perfume-shop-frontend \
  --public-access-block-configuration \
  "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

# Upload build files
aws s3 sync dist/ s3://perfume-shop-frontend/

# Create CloudFront distribution
aws cloudfront create-distribution \
  --origin-domain-name perfume-shop-frontend.s3.amazonaws.com \
  --default-root-object index.html \
  --cache-behaviors file:///path/to/cache-config.json
```

**Option B: Deploy to EC2 Instance (Simpler)**

```bash
# On your EC2 instance
cd /app/perfume-shop

mkdir -p frontend

# Upload your built frontend files to EC2
# From your local machine:
scp -r -i /path/to/your-key.pem dist/* \
  ec2-user@your-ec2-ip:/app/perfume-shop/frontend/

# Or clone from GitHub if versioned
cd /app/perfume-shop
git clone https://github.com/yourusername/perfume-shop-frontend.git frontend-source
cd frontend-source
npm install && npm run build
cp -r dist/* ../frontend/
```

### Step 5.3: Serve Frontend with Nginx

**Update Nginx configuration to serve frontend:**

```bash
sudo tee /etc/nginx/conf.d/perfume-shop.conf > /dev/null << 'EOF'
upstream perfume_api {
    server api:8080;
}

server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;
    client_max_body_size 10M;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Serve frontend static files
    root /app/perfume-shop/frontend;
    
    location / {
        # Try to serve static files, fallback to index.html for SPA routing
        try_files $uri $uri/ /index.html;
    }

    # API proxy
    location /api/ {
        proxy_pass http://perfume_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 60s;
        proxy_connect_timeout 10s;
    }

    # Health check (no logging)
    location /health {
        proxy_pass http://perfume_api/actuator/health;
        access_log off;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
EOF

# Reload Nginx
sudo systemctl reload nginx
```

---

## Phase 6: Payment Gateway Integration

### Step 6.1: Stripe Integration

**1. Get Live API Keys:**
- Go to https://dashboard.stripe.com/apikeys
- Switch to Live mode (toggle in top right)
- Copy **Secret Key** and **Publishable Key**

**2. Create Webhook Endpoint:**
```bash
# In Stripe Dashboard: Developers > Webhooks
# Endpoint URL: https://yourdomain.com/api/webhooks/stripe
# Select events:
# - charge.succeeded
# - charge.failed
# - customer.subscription.updated

# Save the webhook signing secret
```

**3. Update Environment Variables:**
```bash
# In .env.production
STRIPE_API_KEY=sk_live_YOUR_ACTUAL_KEY
STRIPE_WEBHOOK_SECRET=whsec_YOUR_ACTUAL_SECRET
```

**4. Test Stripe Integration:**
```bash
# Using Stripe test card
curl -X POST https://yourdomain.com/api/checkout/payment \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethodId": "pm_card_visa"
  }'
```

### Step 6.2: Razorpay Integration

**1. Get Live API Keys:**
- Go to https://dashboard.razorpay.com
- Navigate to Settings > API Keys
- Copy **Key ID** and **Key Secret**

**2. Create Webhook:**
```bash
# In Razorpay Dashboard: Settings > Webhooks
# Endpoint URL: https://yourdomain.com/api/webhooks/razorpay
# Select events:
# - payment.authorized
# - payment.failed
# - order.paid

# Save the webhook secret
```

**3. Update Environment Variables:**
```bash
# In .env.production
RAZORPAY_KEY_ID=rzp_live_YOUR_ACTUAL_KEY
RAZORPAY_KEY_SECRET=YOUR_ACTUAL_SECRET
RAZORPAY_WEBHOOK_SECRET=YOUR_ACTUAL_WEBHOOK_SECRET
RAZORPAY_CURRENCY=INR
```

**4. Test Razorpay Integration:**
```bash
# Using Razorpay test account
curl -X POST https://yourdomain.com/api/checkout/razorpay \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "amount": 5000,
    "currency": "INR"
  }'
```

---

## Phase 7: Email Service Setup

### Step 7.1: Configure Gmail (Simple Option)

**1. Enable 2-Factor Authentication:**
- Go to https://myaccount.google.com/security
- Enable 2-Step Verification

**2. Generate App Password:**
- Go to https://myaccount.google.com/apppasswords
- Select "Mail" and "Windows Computer"
- Google will generate a 16-character password

**3. Update Environment Variables:**
```bash
# In .env.production
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=YOUR_16_CHAR_APP_PASSWORD
MAIL_FROM=noreply@yourdomain.com
MAIL_FROM_NAME=Parfumé
```

### Step 7.2: Configure SendGrid (Recommended for Production)

**1. Create SendGrid Account:**
- Go to https://sendgrid.com
- Create account and verify domain

**2. Create API Key:**
- Go to Settings > API Keys
- Create new key with "Mail Send" permission

**3. Update Environment Variables:**
```bash
# In .env.production
MAIL_HOST=smtp.sendgrid.net
MAIL_PORT=587
MAIL_USERNAME=apikey
MAIL_PASSWORD=SG.YOUR_SENDGRID_API_KEY
MAIL_FROM=noreply@yourdomain.com
MAIL_FROM_NAME=Parfumé
```

### Step 7.3: Test Email Configuration

```bash
# Restart containers to pick up new env vars
cd /app/perfume-shop
docker compose restart api

# Check logs for email sending
docker compose logs -f api | grep -i "mail\|email"

# Make a test order to trigger email
curl -X POST https://yourdomain.com/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 1}],
    "email": "test@example.com"
  }'
```

---

## Phase 8: Monitoring & Logging

### Step 8.1: Configure Application Logging

**Update application-prod.yml:**

```yaml
logging:
  level:
    root: WARN
    com.perfume: INFO
    org.springframework: WARN
    org.springframework.security: WARN
    org.hibernate: WARN
  file:
    name: /var/log/perfume-shop/application.log
    max-size: 100MB
    max-history: 7
    total-size-cap: 1GB
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 7
```

### Step 8.2: Set Up CloudWatch Monitoring (AWS)

```bash
# Install CloudWatch agent
cd /opt
sudo wget https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm
sudo rpm -U ./amazon-cloudwatch-agent.rpm

# Configure CloudWatch
sudo tee /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json > /dev/null << 'EOF'
{
  "logs": {
    "logs_collected": {
      "files": {
        "collect_list": [
          {
            "file_path": "/var/log/perfume-shop/application.log",
            "log_group_name": "/aws/perfume-shop/application",
            "log_stream_name": "{instance_id}"
          },
          {
            "file_path": "/var/log/docker-compose.log",
            "log_group_name": "/aws/perfume-shop/docker",
            "log_stream_name": "{instance_id}"
          }
        ]
      }
    }
  },
  "metrics": {
    "namespace": "PerfumeShop",
    "metrics_collected": {
      "cpu": {
        "measurement": [
          {
            "name": "cpu_usage_idle",
            "rename": "CPU_IDLE",
            "unit": "Percent"
          },
          {
            "name": "cpu_usage_iowait",
            "rename": "CPU_IOWAIT",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60,
        "totalcpu": false
      },
      "mem": {
        "measurement": [
          {
            "name": "mem_used_percent",
            "rename": "MEM_USED",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60
      },
      "disk": {
        "measurement": [
          {
            "name": "used_percent",
            "rename": "DISK_USED",
            "unit": "Percent"
          }
        ],
        "metrics_collection_interval": 60,
        "paths": ["/"]
      }
    }
  }
}
EOF

# Start CloudWatch agent
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
  -a fetch-config \
  -m ec2 \
  -s \
  -c file:/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json
```

### Step 8.3: Set Up Alarms

```bash
# CPU usage alarm
aws cloudwatch put-metric-alarm \
  --alarm-name perfume-shop-high-cpu \
  --alarm-description "Alert when CPU > 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT_ID:perfume-shop-alerts

# Memory usage alarm
aws cloudwatch put-metric-alarm \
  --alarm-name perfume-shop-high-memory \
  --alarm-description "Alert when Memory > 85%" \
  --metric-name MEM_USED \
  --namespace PerfumeShop \
  --statistic Average \
  --period 300 \
  --threshold 85 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT_ID:perfume-shop-alerts

# Disk usage alarm
aws cloudwatch put-metric-alarm \
  --alarm-name perfume-shop-disk-full \
  --alarm-description "Alert when disk > 90%" \
  --metric-name DISK_USED \
  --namespace PerfumeShop \
  --statistic Average \
  --period 300 \
  --threshold 90 \
  --comparison-operator GreaterThanThreshold \
  --alarm-actions arn:aws:sns:us-east-1:ACCOUNT_ID:perfume-shop-alerts
```

---

## Phase 9: Testing & Validation

### Step 9.1: Health Checks

```bash
# Check API health
curl -i https://yourdomain.com/actuator/health
# Expected: 200 OK

# Check database connectivity
curl -i https://yourdomain.com/actuator/health/db
# Expected: 200 OK with "UP" status

# Check info endpoint
curl https://yourdomain.com/actuator/info | jq
```

### Step 9.2: Functional Testing

```bash
# 1. Test User Registration
curl -X POST https://yourdomain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123!",
    "firstName": "Test",
    "lastName": "User"
  }'

# 2. Test Login
RESPONSE=$(curl -X POST https://yourdomain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "SecurePassword123!"
  }')

JWT_TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

# 3. Test Protected Endpoint
curl https://yourdomain.com/api/users/profile \
  -H "Authorization: Bearer $JWT_TOKEN"

# 4. Test Product Listing
curl https://yourdomain.com/api/products?page=0&size=10

# 5. Test Order Creation
curl -X POST https://yourdomain.com/api/orders \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [{"productId": 1, "quantity": 1}]
  }'
```

### Step 9.3: Load Testing

```bash
# Install Apache Bench
sudo yum install -y httpd-tools

# Run basic load test
ab -n 1000 -c 10 https://yourdomain.com/

# Using Hey tool (better metrics)
go install github.com/rakyll/hey@latest
hey -n 1000 -c 10 -m GET https://yourdomain.com/api/products?page=0&size=10
```

### Step 9.4: Security Testing

```bash
# 1. Check for HTTPS enforcement
curl -I http://yourdomain.com/
# Should redirect to https

# 2. Check security headers
curl -I https://yourdomain.com/ | grep -i "strict-transport\|x-frame\|x-content"

# 3. Test CORS
curl -H "Origin: https://example.com" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -X OPTIONS https://yourdomain.com/api/auth/login -v

# 4. Check JWT validation
curl https://yourdomain.com/api/users/profile \
  -H "Authorization: Bearer invalid_token"
# Expected: 401 Unauthorized
```

### Step 9.5: Database Testing

```bash
# Connect to production database
mysql -h perfume-shop-db.c9akciq32.us-east-1.rds.amazonaws.com \
      -u prod_user -p perfume_shop

# Check table creation
SHOW TABLES;

# Verify sample data
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM orders;
```

---

## Phase 10: Post-Deployment

### Step 10.1: Backup Strategy

```bash
# Daily database backup
sudo tee /home/ec2-user/backup-database.sh > /dev/null << 'EOF'
#!/bin/bash
BACKUP_DIR="/var/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
mkdir -p $BACKUP_DIR

docker compose -f /app/perfume-shop/docker-compose.yml exec -T database mysqldump \
  -u prod_user -p$(grep DATABASE_PASSWORD /app/perfume-shop/.env.production | cut -d '=' -f2) \
  perfume_shop > $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql

# Compress and upload to S3
gzip $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql
aws s3 cp $BACKUP_DIR/perfume_shop_$TIMESTAMP.sql.gz \
  s3://perfume-shop-backups/

# Keep only last 7 backups locally
find $BACKUP_DIR -name "perfume_shop_*.sql.gz" -mtime +7 -delete
EOF

sudo chmod +x /home/ec2-user/backup-database.sh

# Schedule daily at 2 AM
(crontab -l 2>/dev/null; echo "0 2 * * * /home/ec2-user/backup-database.sh") | crontab -
```

### Step 10.2: Update Strategy

```bash
# Create update script
cat > /home/ec2-user/update-perfume-shop.sh << 'EOF'
#!/bin/bash
set -e

cd /app/perfume-shop

# Pull latest code
git pull origin main

# Load env vars
set -a
source .env.production
set +a

# Build new image
docker build -t perfume-shop:latest .

# Stop old containers gracefully
docker compose down

# Start new containers
docker compose up -d

# Wait for health check
for i in {1..30}; do
  if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "Service is healthy"
    exit 0
  fi
  echo "Waiting for service to be ready... ($i/30)"
  sleep 2
done

echo "Service failed to start"
exit 1
EOF

chmod +x /home/ec2-user/update-perfume-shop.sh
```

### Step 10.3: Monitoring Dashboard

```bash
# Create simple health monitoring script
cat > /home/ec2-user/monitor-health.sh << 'EOF'
#!/bin/bash

while true; do
  clear
  echo "=== Perfume Shop Health Status ==="
  echo "Time: $(date)"
  echo ""
  
  # Container status
  echo "Docker Containers:"
  docker compose ps
  echo ""
  
  # API health
  echo "API Health:"
  curl -s http://localhost:8080/actuator/health | jq '.status'
  echo ""
  
  # Database status
  echo "Database Status:"
  docker compose exec -T database mysqladmin ping -u prod_user -p$(grep DATABASE_PASSWORD /app/perfume-shop/.env.production | cut -d '=' -f2)
  echo ""
  
  # System metrics
  echo "System Metrics:"
  echo "CPU: $(top -bn1 | grep "Cpu(s)" | sed "s/.*, *\([0-9.]*\)%* id.*/\1/" | awk '{print 100 - $1"%"}')"
  echo "Memory: $(free | grep Mem | awk '{printf("%.2f%%", $3/$2 * 100)}')"
  echo "Disk: $(df / | tail -1 | awk '{print $5}')"
  echo ""
  
  echo "Press Ctrl+C to exit"
  sleep 10
done
EOF

chmod +x /home/ec2-user/monitor-health.sh
```

### Step 10.4: Documentation

```bash
# Create deployment documentation
cat > /app/perfume-shop/DEPLOYMENT_LOG.md << 'EOF'
# Production Deployment Log

## Deployment Date
$(date)

## Server Information
- Instance Type: t3.medium
- Region: us-east-1
- IP: $(aws ec2 describe-instances --filters "Name=tag:Name,Values=perfume-shop-prod" --query "Reservations[0].Instances[0].PublicIpAddress" --output text)

## Endpoints
- API: https://yourdomain.com/api
- Health: https://yourdomain.com/actuator/health
- Admin API: https://yourdomain.com/api/admin

## Database
- Host: perfume-shop-db.c9akciq32.us-east-1.rds.amazonaws.com:3306
- Database: perfume_shop
- User: prod_user

## Services
- API: Running (Docker container: perfume-shop-api)
- Database: Running (Docker container: perfume-shop-db)
- Nginx: Running (Reverse proxy)

## Environment
- Spring Profile: prod
- Log Level: INFO
- Java Version: 17

## Backup Location
- S3 Bucket: perfume-shop-backups
- Local Backup: /var/backups/mysql

## Deployment Checklist
- [x] Infrastructure setup
- [x] Database configuration
- [x] Environment variables
- [x] Backend build & deployment
- [x] Frontend deployment
- [x] Payment gateways
- [x] Email service
- [x] Monitoring
- [x] Testing

## Known Issues
(List any known issues or workarounds)

## Next Steps
1. Monitor logs
2. Perform daily backups
3. Check metrics daily
EOF
```

### Step 10.5: Runbook

```bash
# Create operations runbook
cat > /app/perfume-shop/RUNBOOK.md << 'EOF'
# Production Operations Runbook

## Common Operations

### View Logs
docker compose logs -f api

### Restart Services
docker compose restart api
docker compose restart database

### Stop Services
docker compose down

### Start Services
cd /app/perfume-shop
set -a
source .env.production
set +a
docker compose up -d

### Create Database Backup
/home/ec2-user/backup-database.sh

### Update Application
/home/ec2-user/update-perfume-shop.sh

### Check Health
curl https://yourdomain.com/actuator/health

### Monitor System
/home/ec2-user/monitor-health.sh

## Troubleshooting

### API Not Responding
1. Check container: docker compose ps
2. View logs: docker compose logs api
3. Check database: docker compose logs database
4. Restart: docker compose restart api

### Database Connection Error
1. Check database status: docker compose ps
2. Verify credentials in .env.production
3. Check network: docker network ls
4. Restart database: docker compose restart database

### Email Not Sending
1. Check credentials: grep MAIL_ .env.production
2. View logs: docker compose logs api | grep -i mail
3. Test connection: telnet smtp.gmail.com 587

### Payment Not Processing
1. Check Stripe/Razorpay keys: grep STRIPE_\|RAZORPAY_ .env.production
2. View webhook logs: docker compose logs api | grep -i webhook
3. Test payment endpoint: curl https://yourdomain.com/api/checkout/payment
EOF
```

---

## Summary Checklist

### Pre-Deployment
- [ ] All prerequisites met (accounts, domains, credentials)
- [ ] Code reviewed and tested locally
- [ ] Environment variables file created

### Infrastructure
- [ ] EC2 instance launched
- [ ] Security groups configured
- [ ] SSH access verified
- [ ] Docker installed and verified
- [ ] Nginx installed and configured
- [ ] SSL certificate obtained

### Database
- [ ] Database service created (RDS or Docker)
- [ ] User and permissions set
- [ ] Backup strategy configured

### Backend
- [ ] Docker image built
- [ ] Containers started successfully
- [ ] Health endpoints verified
- [ ] Logs configured and rotating

### Frontend
- [ ] Frontend built
- [ ] Files deployed
- [ ] Nginx serving frontend
- [ ] SPA routing configured

### External Services
- [ ] Stripe live keys configured
- [ ] Razorpay live keys configured
- [ ] Email service configured
- [ ] Webhooks set up

### Testing
- [ ] Health checks passing
- [ ] API endpoints responding
- [ ] Authentication working
- [ ] Payment integration working
- [ ] Email sending verified
- [ ] Load testing completed

### Monitoring
- [ ] CloudWatch configured
- [ ] Alarms created
- [ ] Log aggregation set up
- [ ] Health checks configured

### Post-Deployment
- [ ] Backup automation running
- [ ] Update strategy in place
- [ ] Documentation complete
- [ ] Team trained on runbook
- [ ] Monitoring dashboard accessible

---

## Emergency Contacts & Resources

- **AWS Support**: https://console.aws.amazon.com/support
- **Stripe Support**: https://support.stripe.com
- **Razorpay Support**: https://razorpay.com/support
- **SendGrid Support**: https://support.sendgrid.com

---

## Quick Reference Commands

```bash
# View all running containers
docker compose ps

# View logs
docker compose logs -f

# SSH into API container
docker compose exec api /bin/bash

# SSH into Database
docker compose exec database mysql -u prod_user -p perfume_shop

# Restart all services
docker compose restart

# Stop all services
docker compose down

# Start all services
docker compose up -d

# Check application health
curl https://yourdomain.com/actuator/health

# View system metrics
/home/ec2-user/monitor-health.sh
```

---

**Last Updated**: January 2026
**Deployment Guide Version**: 1.0
**Application Version**: 1.0.0

