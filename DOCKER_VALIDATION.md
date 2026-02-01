# Docker Configuration Validation Guide

Complete guide to validate and troubleshoot Docker setup for the Perfume Shop application.

## Pre-Flight Checklist

### System Prerequisites

```bash
#!/bin/bash
echo "=== Docker Configuration Validation ==="
echo ""

# Check Docker installation
echo "1. Checking Docker installation..."
if command -v docker &> /dev/null; then
    docker --version
    echo "✅ Docker is installed"
else
    echo "❌ Docker not found. Install from: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check Docker Compose
echo ""
echo "2. Checking Docker Compose..."
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    docker compose version
    echo "✅ Docker Compose is installed"
else
    echo "❌ Docker Compose not found. Install from: https://docs.docker.com/compose/install/"
    exit 1
fi

# Check Docker daemon
echo ""
echo "3. Checking Docker daemon..."
if docker ps &> /dev/null; then
    echo "✅ Docker daemon is running"
else
    echo "❌ Docker daemon not running. Start Docker and try again"
    exit 1
fi

# Check disk space
echo ""
echo "4. Checking available disk space..."
DISK_AVAILABLE=$(df /var | tail -1 | awk '{print $4}')
DISK_NEEDED=2097152  # 2GB in KB
if [ $DISK_AVAILABLE -gt $DISK_NEEDED ]; then
    echo "✅ Sufficient disk space: $(numfmt --to=iec $((DISK_AVAILABLE*1024))) available"
else
    echo "⚠️  Low disk space: $(numfmt --to=iec $((DISK_AVAILABLE*1024))) available (need 2GB)"
fi

# Check RAM
echo ""
echo "5. Checking available RAM..."
RAM_AVAILABLE=$(free -b | awk 'NR==2 {print $7}')
RAM_NEEDED=$((4294967296))  # 4GB in bytes
if [ $RAM_AVAILABLE -gt $RAM_NEEDED ]; then
    echo "✅ Sufficient RAM: $(numfmt --to=iec $RAM_AVAILABLE) available"
else
    echo "⚠️  Low RAM: $(numfmt --to=iec $RAM_AVAILABLE) available (need 4GB)"
fi

# Check ports availability
echo ""
echo "6. Checking required ports..."
PORTS=(3306 8080 9090 3000)
for port in "${PORTS[@]}"; do
    if ! nc -z localhost $port 2>/dev/null; then
        echo "✅ Port $port is available"
    else
        echo "⚠️  Port $port is in use"
    fi
done

echo ""
echo "=== All checks completed ==="
```

Save as `validate-docker-setup.sh` and run:

```bash
chmod +x validate-docker-setup.sh
./validate-docker-setup.sh
```

---

## Docker Files Validation

### docker-compose.yml Validation

```bash
# ===== Syntax validation =====
docker compose config

# Expected output: Valid docker-compose configuration in YAML format
# If errors appear, fix syntax issues

# ===== Verify service definitions =====
docker compose config | grep -E "services:|image:|build:|ports:"

# Should show all services (database, api, frontend)
```

### Dockerfile Validation

```bash
# ===== Check for common issues =====

# 1. Verify base image exists
docker pull eclipse-temurin:17-jdk-alpine

# 2. Build and test backend
docker build -t perfume-shop-api:test .

# 3. Check image
docker images | grep perfume-shop-api

# 4. Build frontend
docker build -t perfume-shop-frontend:test ./frontend

# 5. Verify no obvious issues
docker run --rm perfume-shop-api:test java -version
```

---

## Environment Configuration Validation

### .env File Validation

```bash
#!/bin/bash
# validate-env.sh

echo "=== Validating .env.production ==="

if [ ! -f .env.production ]; then
    echo "❌ .env.production not found"
    echo "   Run: cp .env.production.example .env.production"
    exit 1
fi

# Check required variables
REQUIRED_VARS=(
    "JWT_SECRET"
    "DATABASE_USERNAME"
    "DATABASE_PASSWORD"
    "MAIL_USERNAME"
    "MAIL_PASSWORD"
    "RAZORPAY_KEY_ID"
    "RAZORPAY_KEY_SECRET"
    "RAZORPAY_WEBHOOK_SECRET"
)

echo ""
echo "Checking required variables..."
MISSING=0
for var in "${REQUIRED_VARS[@]}"; do
    if grep -q "^${var}=" .env.production; then
        VALUE=$(grep "^${var}=" .env.production | cut -d'=' -f2)
        if [ -z "$VALUE" ] || [ "$VALUE" = "your-" ]; then
            echo "⚠️  $var is set but empty or placeholder"
            ((MISSING++))
        else
            echo "✅ $var is configured"
        fi
    else
        echo "❌ $var is missing"
        ((MISSING++))
    fi
done

if [ $MISSING -gt 0 ]; then
    echo ""
    echo "⚠️  $MISSING variable(s) need attention"
    echo "Edit .env.production and fill in all required values"
    exit 1
else
    echo ""
    echo "✅ All required variables are configured"
fi

# Specific validations
echo ""
echo "Additional validations..."

# Check JWT_SECRET length
JWT_SECRET=$(grep "^JWT_SECRET=" .env.production | cut -d'=' -f2)
if [ ${#JWT_SECRET} -lt 44 ]; then
    echo "⚠️  JWT_SECRET is ${#JWT_SECRET} chars (should be 44+ for 256 bits)"
fi

# Check password strength
DB_PASSWORD=$(grep "^DATABASE_PASSWORD=" .env.production | cut -d'=' -f2)
if [ ${#DB_PASSWORD} -lt 8 ]; then
    echo "⚠️  DATABASE_PASSWORD is ${#DB_PASSWORD} chars (should be 8+)"
fi

echo ""
echo "✅ Validation complete"
```

Run:
```bash
chmod +x validate-env.sh
./validate-env.sh
```

---

## Build Validation

### Backend Build Validation

```bash
# ===== Check Maven is installed =====
mvn --version

# ===== Build backend JAR =====
mvn clean package -DskipTests

# Expected output should include:
# - Downloading dependencies
# - Building perfume-shop-1.0.0.jar
# - BUILD SUCCESS

# ===== Verify JAR exists =====
ls -lh target/perfume-shop-*.jar

# Should show file size ~50-100MB

# ===== Test JAR integrity =====
jar tf target/perfume-shop-*.jar | head -20

# Should show JAR contents
```

### Frontend Build Validation (Optional)

```bash
cd frontend

# ===== Install dependencies =====
npm ci

# ===== Build for production =====
npm run build

# ===== Verify dist folder =====
ls -la dist/

# Should show index.html and asset files

cd ..
```

---

## Docker Compose Execution Validation

### Pre-Start Checks

```bash
# ===== Check docker-compose.yml syntax =====
docker compose config --quiet && echo "✅ Config valid" || echo "❌ Config invalid"

# ===== Pull base images (optional, compose will do this) =====
docker pull mysql:8.0-alpine
docker pull eclipse-temurin:17-jdk-alpine

# ===== Check available images =====
docker images | grep -E "mysql|temurin|nginx"
```

### Starting Services

```bash
# ===== Start with verbose output =====
docker compose --env-file .env.production up --build

# ===== In another terminal, monitor startup =====
docker compose logs -f

# Wait for these messages:
# - database: "MySQL Server has started"
# - api: "Started PerfumeShopApplication"
# - api: "Tomcat started on port(s): 8080"
```

### Health Check Monitoring

```bash
# ===== Monitor services during startup =====
watch -n 2 'docker compose ps'

# ===== Check individual service health =====
docker compose exec database mysql -u prod_user -p -e "SELECT 1;" 2>/dev/null && echo "✅ Database healthy" || echo "❌ Database unhealthy"

docker compose exec api curl -s http://localhost:8080/actuator/health 2>/dev/null | jq '.status' && echo "✅ API healthy" || echo "❌ API unhealthy"
```

---

## Runtime Validation

### Service Health Checks

```bash
#!/bin/bash
# health-check.sh

echo "=== Docker Services Health Check ==="
echo ""

# Check if services are running
echo "1. Checking service status..."
docker compose ps --quiet || (echo "❌ Services not running"; exit 1)

# Database health
echo ""
echo "2. Checking database..."
if docker compose exec database mysql -u prod_user -p"${DATABASE_PASSWORD}" -e "SELECT 1;" 2>/dev/null; then
    echo "✅ Database is healthy"
else
    echo "❌ Database is unhealthy"
    docker compose logs database | tail -20
fi

# API health endpoint
echo ""
echo "3. Checking API health endpoint..."
HEALTH=$(curl -s http://localhost:8080/actuator/health | jq -r '.status' 2>/dev/null)
if [ "$HEALTH" = "UP" ]; then
    echo "✅ API is healthy"
else
    echo "❌ API health: $HEALTH"
    docker compose logs api | grep -i "error\|exception" | tail -20
fi

# API connectivity
echo ""
echo "4. Checking API connectivity..."
if curl -s http://localhost:8080/actuator/info &>/dev/null; then
    echo "✅ API is responding"
else
    echo "❌ Cannot reach API"
fi

# Database connectivity from API
echo ""
echo "5. Checking database connectivity from API..."
if curl -s http://localhost:8080/actuator/health | jq '.components.db.status' | grep -q "UP"; then
    echo "✅ API can reach database"
else
    echo "❌ API cannot reach database"
fi

# Test data
echo ""
echo "6. Checking test data..."
if curl -s http://localhost:8080/api/products/featured | jq '.[0].name' &>/dev/null; then
    echo "✅ Test data available"
else
    echo "⚠️  No test data found"
fi

echo ""
echo "=== Health check complete ==="
```

Run:
```bash
chmod +x health-check.sh
./health-check.sh
```

---

## Port Verification

### Check Port Usage

```bash
# ===== Find processes using ports =====
lsof -i :3306   # MySQL
lsof -i :8080   # Spring Boot
lsof -i :9090   # Management
lsof -i :3000   # Frontend

# ===== Kill process using port (if needed) =====
kill -9 <PID>

# ===== Alternative: Change docker-compose.yml ports =====
# ports:
#   - "3307:3306"  # Use 3307 instead of 3306
#   - "8888:8080"  # Use 8888 instead of 8080

# ===== Test port connectivity =====
nc -zv localhost 8080
nc -zv localhost 3306
```

---

## Log Validation

### View Service Logs

```bash
# ===== Real-time logs =====
docker compose logs -f api       # API service logs
docker compose logs -f database  # Database logs

# ===== Last N lines =====
docker compose logs --tail=50 api
docker compose logs --tail=100 database

# ===== Filter by pattern =====
docker compose logs api | grep -i "error\|exception"
docker compose logs api | grep "Started"
docker compose logs database | grep "ready for connections"

# ===== Export logs to file =====
docker compose logs > logs.txt
docker compose logs api > api.log
docker compose logs database > database.log
```

### Common Log Patterns

```bash
# ===== API successful startup =====
docker compose logs api | grep "Started PerfumeShopApplication"

# ===== Database ready =====
docker compose logs database | grep "ready for connections"

# ===== Connection issues =====
docker compose logs api | grep "Connection refused\|Cannot get a connection"

# ===== Configuration errors =====
docker compose logs api | grep "property.*not found\|Invalid"

# ===== Authentication failures =====
docker compose logs database | grep "Access denied\|Failed"
```

---

## Network Validation

### Docker Network Inspection

```bash
# ===== List networks =====
docker network ls

# ===== Inspect perfume-network =====
docker network inspect perfume-network

# ===== Test inter-container connectivity =====
docker compose exec api ping database

# Expected: Should resolve 'database' hostname to container IP

# ===== Test DNS resolution =====
docker compose exec api nslookup database

# ===== Verify network isolation =====
docker compose exec database ping api
```

---

## Volume Validation

### Data Persistence Check

```bash
# ===== List volumes =====
docker volume ls | grep perfume

# ===== Inspect volume =====
docker volume inspect perfume_mysql-data

# ===== Check volume mount point =====
docker compose exec database ls -la /var/lib/mysql

# ===== Verify data persistence =====
docker compose exec database mysql -u prod_user -p -e "SELECT COUNT(*) FROM users;"

# Stop and restart container
docker compose restart database

# Data should still exist
docker compose exec database mysql -u prod_user -p -e "SELECT COUNT(*) FROM users;"
```

---

## Integration Testing

### Complete Workflow Test

```bash
#!/bin/bash
# integration-test.sh

API_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)
TEST_EMAIL="test-${TIMESTAMP}@example.com"

echo "=== Integration Test ==="
echo ""

# Step 1: Register user
echo "1. Registering user..."
REGISTER=$(curl -s -X POST "$API_URL/api/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"TestPassword123!\",
    \"firstName\": \"Test\",
    \"lastName\": \"User\",
    \"phoneNumber\": \"5555555555\"
  }")

TOKEN=$(echo $REGISTER | jq -r '.token // empty')
if [ -z "$TOKEN" ]; then
    echo "❌ Failed to register user"
    echo $REGISTER | jq .
    exit 1
fi
echo "✅ User registered: $TEST_EMAIL"

# Step 2: Get user profile
echo ""
echo "2. Getting user profile..."
PROFILE=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "$API_URL/api/users/profile")

USER_ID=$(echo $PROFILE | jq -r '.id // empty')
if [ -z "$USER_ID" ]; then
    echo "❌ Failed to get profile"
    exit 1
fi
echo "✅ Profile retrieved (ID: $USER_ID)"

# Step 3: Get featured products
echo ""
echo "3. Fetching featured products..."
PRODUCTS=$(curl -s "$API_URL/api/products/featured")
PRODUCT_COUNT=$(echo $PRODUCTS | jq 'length')
if [ "$PRODUCT_COUNT" -eq 0 ]; then
    echo "⚠️  No featured products"
else
    PRODUCT_ID=$(echo $PRODUCTS | jq -r '.[0].id')
    echo "✅ Found $PRODUCT_COUNT products (first ID: $PRODUCT_ID)"
fi

# Step 4: Add to cart
if [ ! -z "$PRODUCT_ID" ]; then
    echo ""
    echo "4. Adding product to cart..."
    CART=$(curl -s -X POST "$API_URL/api/cart" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"productId\": $PRODUCT_ID,
        \"quantity\": 1
      }")
    
    CART_COUNT=$(echo $CART | jq -r '.itemCount // empty')
    if [ ! -z "$CART_COUNT" ]; then
        echo "✅ Added to cart (items: $CART_COUNT)"
    else
        echo "⚠️  Could not verify cart update"
    fi
fi

# Step 5: Logout
echo ""
echo "5. Logging out..."
curl -s -X POST "$API_URL/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN" &>/dev/null
echo "✅ Logout successful"

echo ""
echo "=== Integration test complete ==="
```

Run:
```bash
chmod +x integration-test.sh
./integration-test.sh
```

---

## Troubleshooting Commands

### Quick Diagnostics

```bash
# ===== Show all Docker info =====
docker info

# ===== Check container resource usage =====
docker stats

# ===== Inspect specific container =====
docker compose ps -a
docker inspect <container_name>

# ===== Execute command in container =====
docker compose exec api java -version
docker compose exec database mysql --version

# ===== View container events =====
docker events

# ===== Check Docker disk usage =====
docker system df

# ===== View container file system =====
docker compose exec api ls -la /app
docker compose exec database ls -la /var/lib/mysql
```

---

## Performance Validation

### Resource Monitoring

```bash
# ===== Monitor resource usage =====
docker stats

# ===== Check container memory limit =====
docker compose exec api free -h

# ===== Check disk usage =====
docker compose exec database du -sh /var/lib/mysql

# ===== Monitor network traffic =====
docker stats --no-stream

# ===== Check Java heap usage =====
docker compose exec api jps -l
docker compose exec api jstat -gc -h10 <PID>
```

---

## Cleanup and Reset

```bash
# ===== Stop all services =====
docker compose down

# ===== Remove services and volumes (WARNING: deletes data) =====
docker compose down -v

# ===== Remove images =====
docker compose down --rmi all

# ===== Remove unused resources =====
docker system prune -a

# ===== Reset everything =====
docker compose down -v && \
docker system prune -a -f && \
docker compose build --no-cache && \
docker compose --env-file .env.production up
```

---

**Last Updated**: January 2026
**Validation Script Version**: 1.0
