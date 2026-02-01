# Docker Setup & Verification Checklist

Complete step-by-step guide to run the Perfume Shop application using Docker Compose on a fresh machine.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Building the Application](#building-the-application)
4. [Starting with Docker Compose](#starting-with-docker-compose)
5. [Verification Steps](#verification-steps)
6. [Troubleshooting](#troubleshooting)
7. [Common Issues & Solutions](#common-issues--solutions)

---

## Prerequisites

### System Requirements

- **Docker**: Version 20.10+ ([Install](https://docs.docker.com/get-docker/))
- **Docker Compose**: Version 2.0+ ([Install](https://docs.docker.com/compose/install/))
- **Java 17+**: For building the backend (if building locally)
- **Maven 3.8+**: For building the backend JAR
- **Node.js 18+**: For building the frontend (optional)
- **Disk Space**: At least 2GB free
- **RAM**: At least 4GB available

### Quick Install (macOS/Linux)

```bash
# Install Docker (macOS)
brew install docker docker-compose

# Install Docker (Ubuntu/Debian)
sudo apt-get update
sudo apt-get install -y docker.io docker-compose

# Start Docker daemon (Linux)
sudo systemctl start docker
sudo systemctl enable docker

# Verify installations
docker --version          # Docker version 20.10+
docker compose version    # Docker Compose version 2.0+
java -version            # Java 17+
mvn --version            # Maven 3.8+
```

### Windows Setup

1. Install [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
2. Enable WSL 2 (Windows Subsystem for Linux)
3. Install Visual Studio Code with Docker extension (recommended)
4. Ensure Docker Desktop is running before proceeding

---

## Environment Setup

### Step 1: Clone the Repository

```bash
cd ~/projects
git clone <repository-url> perfume-shop
cd perfume-shop
```

### Step 2: Create Environment File

```bash
# Copy the example environment file
cp .env.production.example .env.production

# Edit with your actual secrets
nano .env.production  # or: code .env.production
```

### Step 3: Fill Required Environment Variables

Edit `.env.production` with these REQUIRED variables:

```dotenv
# ===== DATABASE =====
DATABASE_USERNAME=prod_user
DATABASE_PASSWORD=your-secure-database-password  # Min 16 characters
DB_ROOT_PASSWORD=root_password

# ===== JWT (Generate: openssl rand -base64 32) =====
JWT_SECRET=your-256-bit-base64-encoded-secret-key-replace-this

# ===== EMAIL =====
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password

# ===== RAZORPAY (Get from https://dashboard.razorpay.com) =====
RAZORPAY_KEY_ID=rzp_test_your_key
RAZORPAY_KEY_SECRET=your_secret_key
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# ===== SECURITY =====
CORS_ORIGINS=http://localhost:3000,http://localhost:5173

# ===== FRONTEND =====
FRONTEND_URL=http://localhost:3000
```

### Step 4: Validate Environment File

```bash
# Check all required variables are set
grep -E "^[A-Z_]+=" .env.production | wc -l

# Should show ~14+ variables
# If any are missing, edit the file and add them
```

---

## Building the Application

### Option A: Build Locally (Recommended for First Time)

```bash
# ===== BUILD BACKEND JAR =====
mvn clean package -DskipTests

# ===== BUILD FRONTEND (Optional - Docker builds it) =====
cd frontend
npm ci
npm run build
cd ..

# ===== Verify JARs created =====
ls -lah target/perfume-shop-*.jar
ls -lah frontend/dist/
```

### Option B: Build During Docker Compose

Docker will automatically build both if you skip local build:

```bash
# Skip this step and go directly to "Starting with Docker Compose"
# Docker will build on: docker compose up --build
```

---

## Starting with Docker Compose

### Quick Start (Recommended)

```bash
# ===== STEP 1: Build and start all services =====
docker compose --env-file .env.production up --build

# Expected output:
# - database: "MySQL Server has started"
# - api: "Started PerfumeShopApplication"
# This may take 2-5 minutes on first run

# ===== STEP 2: Leave this running and open another terminal =====
# Then proceed to "Verification Steps" section
```

### Alternative: Build and Start Separately

```bash
# ===== Build all services =====
docker compose --env-file .env.production build

# ===== Start services in background =====
docker compose --env-file .env.production up -d

# ===== View logs =====
docker compose logs -f

# ===== Stop services =====
docker compose down
```

### Without .env File (Shell Environment Variables)

```bash
# Set variables in shell before docker compose
export DATABASE_PASSWORD="secure_password"
export JWT_SECRET=$(openssl rand -base64 32)
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"
export RAZORPAY_KEY_ID="rzp_test_xxx"
export RAZORPAY_KEY_SECRET="xxx"
export RAZORPAY_WEBHOOK_SECRET="xxx"
export CORS_ORIGINS="http://localhost:3000"
export FRONTEND_URL="http://localhost:3000"

# Start with exported variables
docker compose up --build
```

---

## Verification Steps

### Phase 1: Service Health Checks (Immediate)

Run these checks while `docker compose up` is still running:

```bash
# ===== TERMINAL 2: Open new terminal =====

# ===== 1. Check all services are running =====
docker compose ps

# Expected output:
# CONTAINER ID  IMAGE              STATUS
# xxxxx         perfume-shop-db    Up 2 minutes (healthy)
# xxxxx         perfume-shop-api   Up 1 minute  (healthy)

# ===== 2. Check database health =====
docker compose exec database mysql -u prod_user -p"${DATABASE_PASSWORD}" \
  perfume_shop -e "SELECT 1 AS 'Database Connected';"

# Expected: "Database Connected | 1"

# ===== 3. Check API health endpoint =====
curl -s http://localhost:8080/actuator/health | jq .

# Expected JSON:
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "diskSpace": { "status": "UP" },
#     ...
#   }
# }

# ===== 4. Check API logs for errors =====
docker compose logs api | grep -i "error\|warn\|exception"

# Expected: Should show application startup messages, minimal errors
# Should see: "Started PerfumeShopApplication"
```

### Phase 2: API Endpoint Testing (After Health Checks Pass)

```bash
# ===== 1. Test public endpoints =====

# Get featured products
curl -s http://localhost:8080/api/products/featured | jq . | head -20

# Expected: Array of products with name, price, description, etc.

# ===== 2. Test authentication endpoints =====

# Create a test account
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPassword123!",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "1234567890"
  }' | jq .

# Expected response:
# {
#   "token": "eyJhbGc...",
#   "refreshToken": "...",
#   "user": { "email": "test@example.com", ... },
#   "expiresIn": 86400
# }

# Save the token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "TestPassword123!",
    "firstName": "Jane",
    "lastName": "Smith",
    "phoneNumber": "0987654321"
  }' | jq -r '.token')

echo "Token: $TOKEN"

# ===== 3. Test protected endpoints =====

# Get user profile (requires token from login/register)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/users/profile | jq .

# Expected: User object with email, firstName, lastName, etc.

# ===== 4. Test product endpoints =====

# Get product by ID (adjust ID to existing product)
curl -s http://localhost:8080/api/products/1 | jq .

# ===== 5. Test actuator endpoints =====

# Basic health
curl -s http://localhost:8080/actuator/health | jq .

# Detailed health (shows all components)
curl -s http://localhost:8080/actuator/health/details | jq .

# Application info
curl -s http://localhost:8080/actuator/info | jq .

# Metrics (if enabled)
curl -s http://localhost:8080/actuator/metrics | jq .
```

### Phase 3: Database Verification

```bash
# ===== 1. Connect to database =====
docker compose exec database mysql -u prod_user -p"${DATABASE_PASSWORD}" perfume_shop

# ===== Inside MySQL prompt (mysql>) =====

# Show tables
SHOW TABLES;

# Expected tables: users, products, cart, cart_items, orders, order_items, reviews, etc.

# Check user count
SELECT COUNT(*) as user_count FROM users;

# Check products
SELECT COUNT(*) as product_count FROM products;

# Exit MySQL
EXIT;
```

### Phase 4: Integration Test

```bash
# ===== Complete User Journey =====

# 1. Register new user
RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "integration-test@example.com",
    "password": "TestPassword123!",
    "firstName": "Integration",
    "lastName": "Test",
    "phoneNumber": "5555555555"
  }')

TOKEN=$(echo $RESPONSE | jq -r '.token')
USER_ID=$(echo $RESPONSE | jq -r '.user.id')

echo "Created user: $USER_ID"
echo "Token: $TOKEN"

# 2. Get featured products
FEATURED=$(curl -s http://localhost:8080/api/products/featured)
PRODUCT_ID=$(echo $FEATURED | jq -r '.[0].id')

echo "First featured product ID: $PRODUCT_ID"

# 3. Add product to cart
curl -s -X POST http://localhost:8080/api/cart \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"productId\": $PRODUCT_ID,
    \"quantity\": 2
  }" | jq .

# 4. Get cart
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/cart | jq .

# Expected: Cart with items, subtotal, tax, total

echo "✅ Integration test completed successfully!"
```

---

## Troubleshooting

### Check Service Status

```bash
# ===== View all running services =====
docker compose ps

# ===== View service logs =====
docker compose logs api              # API logs
docker compose logs database         # Database logs
docker compose logs -f api           # Follow API logs in real-time

# ===== View specific error lines =====
docker compose logs api | grep -i "error\|exception" | tail -20

# ===== Get container details =====
docker compose ps -a
docker compose exec api ps aux
docker compose exec database ps aux
```

### Reset Everything

```bash
# ===== Stop all services =====
docker compose down

# ===== Remove volumes (WARNING: deletes all data) =====
docker compose down -v

# ===== Remove all images =====
docker compose down --rmi all

# ===== Clean up unused Docker resources =====
docker system prune -a

# ===== Rebuild from scratch =====
docker compose --env-file .env.production up --build
```

---

## Common Issues & Solutions

### Issue 1: "Database connection refused"

**Symptoms:**
```
java.sql.SQLRecoverableException: IO Error: Connection refused
Error: Cannot access database at localhost:3306
```

**Solutions:**

```bash
# Check database service status
docker compose ps database

# If not running, start it
docker compose up database -d

# Wait for database to be healthy (30-60 seconds)
docker compose exec database mysqladmin ping -h localhost

# Check database logs
docker compose logs database

# If port 3306 is in use locally
# Edit docker-compose.yml and change port:
# ports:
#   - "3307:3306"  # Use 3307 instead

# Rebuild and restart
docker compose down
docker compose up --build -d
```

### Issue 2: "JWT_SECRET not provided"

**Symptoms:**
```
ERROR: app.jwt.secret: JWT_SECRET environment variable is not set
```

**Solutions:**

```bash
# Generate a new JWT secret
openssl rand -base64 32

# Add to .env.production
echo "JWT_SECRET=$(openssl rand -base64 32)" >> .env.production

# Or set in shell
export JWT_SECRET=$(openssl rand -base64 32)

# Restart services
docker compose restart api
```

### Issue 3: "Permission denied" on Linux

**Symptoms:**
```
permission denied while trying to connect to the Docker daemon
```

**Solutions:**

```bash
# Add user to docker group
sudo usermod -aG docker $USER

# Apply new group membership
newgrp docker

# Restart Docker
sudo systemctl restart docker

# Verify
docker ps
```

### Issue 4: "Out of memory" during build

**Symptoms:**
```
ERROR: Cannot allocate memory
BUILD FAILED
```

**Solutions:**

```bash
# Increase Docker memory allocation
# On Mac: Docker Desktop -> Settings -> Resources -> Memory: 4GB+
# On Linux: Edit /etc/docker/daemon.json and set "memory": "4g"

# Build backend locally first
mvn clean package -DskipTests

# Then use cached build
docker compose up -d
```

### Issue 5: "Port already in use"

**Symptoms:**
```
ERROR: listen tcp 0.0.0.0:8080: bind: address already in use
ERROR: listen tcp 0.0.0.0:3306: bind: address already in use
```

**Solutions:**

```bash
# Find process using the port
lsof -i :8080
lsof -i :3306

# Kill the process
kill -9 <PID>

# Or use different ports in docker-compose.yml
# ports:
#   - "8888:8080"  # Use 8888 on host
#   - "3307:3306"  # Use 3307 on host

# Rebuild
docker compose down
docker compose up --build -d
```

### Issue 6: "Email credentials invalid"

**Symptoms:**
```
javax.mail.AuthenticationFailedException: 535-5.7.1 Please log in with your app password
```

**Solutions:**

```bash
# Gmail requires App Password for SMTP, not regular password
# 1. Go to https://myaccount.google.com/apppasswords
# 2. Generate an app-specific password
# 3. Use that in MAIL_PASSWORD

# Alternative: Use a different email service
# - SendGrid: https://sendgrid.com
# - AWS SES: https://aws.amazon.com/ses/
# - Mailgun: https://www.mailgun.com/

# Test email credentials
docker compose exec api curl -s http://localhost:8080/actuator/health/mail
```

### Issue 7: "Razorpay API key invalid"

**Symptoms:**
```
com.razorpay.RazorpayClient: Invalid API key
```

**Solutions:**

```bash
# Check Razorpay credentials in .env.production
grep RAZORPAY .env.production

# Get correct credentials from:
# https://dashboard.razorpay.com/app/keys

# For testing, use test keys:
# https://razorpay.com/docs/api/authentication/

# Update environment variables
nano .env.production

# Restart API
docker compose restart api

# Check logs
docker compose logs api | grep -i razorpay
```

### Issue 8: "CORS errors in browser"

**Symptoms:**
```
Access to XMLHttpRequest at 'http://localhost:8080/api...'
has been blocked by CORS policy
```

**Solutions:**

```bash
# Update CORS_ORIGINS in .env.production
CORS_ORIGINS=http://localhost:3000,http://localhost:5173,http://your-domain.com

# Restart API
docker compose restart api

# Verify CORS headers
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: GET" \
     -i http://localhost:8080/api/products

# Should see Access-Control-Allow-Origin header
```

---

## Final Verification Checklist

```bash
# ===== Run this final checklist =====

# 1. All services running
docker compose ps
# All should show "Up" status

# 2. Database connected
docker compose exec database mysql -u prod_user -p -e "SELECT 1;"

# 3. API responding
curl http://localhost:8080/actuator/health

# 4. Database has tables
docker compose exec database mysql -u prod_user -p -e "SHOW TABLES FROM perfume_shop;"

# 5. Can create user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "final-test@example.com",
    "password": "TestPassword123!",
    "firstName": "Final",
    "lastName": "Test",
    "phoneNumber": "1111111111"
  }'

# 6. Can get products
curl http://localhost:8080/api/products/featured

echo "✅ All verification checks passed!"
```

---

## Production Deployment URLs

### Local Development

| Service | URL | Purpose |
|---------|-----|---------|
| **API** | http://localhost:8080 | Backend API |
| **Health** | http://localhost:8080/actuator/health | Service health |
| **Products** | http://localhost:8080/api/products | Product list |
| **Register** | http://localhost:8080/api/auth/register | User signup |
| **Login** | http://localhost:8080/api/auth/login | User login |
| **Frontend** | http://localhost:3000 | React app (if enabled) |

### Production Deployment

| Service | URL | Purpose |
|---------|-----|---------|
| **API** | https://api.yourdomain.com | Production backend |
| **Frontend** | https://yourdomain.com | Production frontend |
| **Email** | noreply@yourdomain.com | Transactional emails |

### Test API Endpoints

```bash
# ===== Quick API test script =====

API_URL="http://localhost:8080"
HEADERS="-H 'Content-Type: application/json'"

echo "1. Getting featured products..."
curl -s "$API_URL/api/products/featured" | jq '.[] | {id, name, price}' | head -10

echo -e "\n2. Registering new user..."
REGISTER=$(curl -s -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test-'$(date +%s)'@example.com",
    "password": "TestPassword123!",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "5555555555"
  }')

TOKEN=$(echo $REGISTER | jq -r '.token')
echo "Registered with token: ${TOKEN:0:20}..."

echo -e "\n3. Getting user profile..."
curl -s -H "Authorization: Bearer $TOKEN" \
  "$API_URL/api/users/profile" | jq '.{id, email, firstName, lastName}'

echo -e "\n✅ API tests completed!"
```

---

## Getting Help

If you encounter issues not covered here:

1. **Check logs first**: `docker compose logs api`
2. **Review environment variables**: `cat .env.production`
3. **Check Docker resources**: `docker stats`
4. **Verify network**: `docker compose exec api ping database`
5. **Check database access**: `docker compose exec database mysql -u prod_user -p`

For additional help, refer to:
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [Project Repository Issues](https://github.com/your-repo/issues)

---

**Last Updated**: January 2026
**Docker Compose Version**: 3.8+
**Compatible with**: Docker 20.10+
