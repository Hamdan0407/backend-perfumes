# 🚀 COMPLETE AUTOMATED SETUP - NO MANUAL CONFIGURATION NEEDED

**Status**: ✅ Ready to Deploy  
**Method**: Docker Only (All build tools included in containers)  
**Time Required**: 15-30 minutes (first run, ~5 minutes subsequent)  
**Prerequisites**: Docker Desktop ONLY

---

## 📋 The One Thing You Need

### Docker Desktop
- **Download**: https://www.docker.com/products/docker-desktop
- **Install**: Run the installer and restart your computer
- **Verify**: Open terminal/PowerShell and run:
  ```bash
  docker --version
  docker compose version
  ```

That's it! Everything else runs inside Docker containers.

---

## 🎯 Complete Setup in 3 Commands

### Step 1: Navigate to Project
```bash
cd c:\Users\Hamdaan\Documents\maam
REM or wherever the project is located
```

### Step 2: Start Everything
```bash
docker compose --env-file .env.production up --build
```

This single command will:
- ✅ Download necessary Docker images (Maven, Java, MySQL, Alpine Linux)
- ✅ Build the entire backend from source code (Java + Spring Boot)
- ✅ Create the database schema
- ✅ Start all services
- ✅ Show logs in real-time

### Step 3: Verify (In Another Terminal)
```bash
# Check if services are running
docker compose ps

# Test API
curl http://localhost:8080/actuator/health

# Get products
curl http://localhost:8080/api/products/featured
```

---

## 📊 What Happens Behind the Scenes

### Docker Build Process

```
┌──────────────────────────────────────────────────────────────┐
│ docker compose --env-file .env.production up --build         │
└──────────────────────────────────────────────────────────────┘
                              │
                              ▼
                   ┌─────────────────┐
                   │  Read docker-   │
                   │  compose.yml    │
                   └─────────────────┘
                              │
                 ┌────────────┼────────────┐
                 │            │            │
                 ▼            ▼            ▼
           ┌─────────┐   ┌────────┐   ┌────────┐
           │ Build   │   │ MySQL  │   │ (Skip) │
           │ Spring  │   │ 8.0    │   │ Front- │
           │ Boot    │   │ Image  │   │ end    │
           │ (Maven) │   │        │   │(opt)   │
           └─────────┘   └────────┘   └────────┘
                 │            │
                 ▼            ▼
            ┌──────────────────────┐
            │   Start Services     │
            │  - MySQL (3306)      │
            │  - API (8080)        │
            │  - Actuator (9090)   │
            └──────────────────────┘
                     │
                     ▼
            ┌──────────────────────┐
            │  Show Real-time Logs │
            │  Press Ctrl+C to stop│
            └──────────────────────┘
```

### Build Timeline

| Stage | Action | Time | What It Does |
|-------|--------|------|------------|
| **1** | Download images | 30-60s | Gets Maven, Java, MySQL, Alpine Linux (only first time) |
| **2** | Build backend | 3-5m | Compiles Java, runs Maven, creates JAR in container |
| **3** | Create database | 10-20s | Initializes MySQL schema and tables |
| **4** | Start services | 10-30s | Launches API and database, verifies health |
| **5** | Show logs | Continuous | Displays all application output |

**Total First Time**: 5-10 minutes  
**Total Subsequent**: 30 seconds (images cached)

---

## ✅ Success Indicators

Watch for these messages in the terminal:

```
database_1  | 2026-01-19 12:00:00 0 [Note] Server socket created on IP: '::'.
database_1  | 2026-01-19 12:00:00 0 [Warning] Insecure configuration...
database_1  | 2026-01-19 12:00:00 0 [Note] mysqld: ready for connections.

api_1  | [INFO] ... Mapped "{POST /api/auth/register}" onto
api_1  | [INFO] ... Tomcat started on port(s): 8080
api_1  | [INFO] ... Started PerfumeShopApplication in 12.5 seconds
```

When you see "**Started PerfumeShopApplication**", the API is ready!

---

## 🔍 Verify Everything Works

Open a NEW terminal (don't stop the running one):

```bash
# 1. Check containers are running
docker compose ps

# Output should show 2 containers:
# NAME                COMMAND                  STATUS
# perfume-shop-api    "sh -c 'java..."         Up X seconds
# perfume-shop-db     "docker-entrypoint..."   Up X seconds

# 2. Check API health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP","components":{"db":{"status":"UP"}}}

# 3. Get featured products
curl http://localhost:8080/api/products/featured

# Expected: JSON array like:
# [{"id":1,"name":"Product1","price":99.99}...]

# 4. Register a test user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!@","firstName":"Test","lastName":"User","phoneNumber":"1234567890"}'

# Expected: User object with JWT token
```

All 4 checks should succeed! ✅

---

## 🛑 Stop Everything (Keep Data)

```bash
# Option 1: Press Ctrl+C in the terminal running docker compose
# This gracefully shuts down services

# Option 2: In another terminal, run:
docker compose down

# Data in MySQL persists (in volumes)
```

---

## 🗑️ Complete Cleanup (Delete Everything)

```bash
# CAUTION: This deletes all data!
docker compose down -v

# Remove images (takes back disk space)
docker rmi perfume-shop-api mysql:8.0-alpine
```

---

## 🔧 Development Workflow

### Quick Start (Every Time)
```bash
cd project-directory
docker compose --env-file .env.production up

# In another terminal:
curl http://localhost:8080/api/products/featured
```

### Make Changes to Code
```bash
# 1. Edit source code (src/main/java/...)
# 2. Stop old containers: Ctrl+C
# 3. Rebuild and start:
docker compose up --build

# Docker will:
# - Rebuild the JAR (with your changes)
# - Restart the API
# - Keep database data (in volumes)
```

### View Logs
```bash
# All services
docker compose logs

# Specific service
docker compose logs -f api    # API logs
docker compose logs -f database  # Database logs

# Last 100 lines
docker compose logs --tail 100

# Search for errors
docker compose logs | grep -i error
```

### Access Services
```bash
# MySQL from host machine
mysql -h 127.0.0.1 -u prod_user -p perfume_shop
# Password: (from .env.production)

# API endpoints
curl http://localhost:8080/api/products
curl http://localhost:8080/api/users/profile

# Directly in container
docker compose exec api bash
docker compose exec database mysql -u prod_user -p perfume_shop
```

---

## 📁 Environment File (.env.production)

Already created with defaults for local development:

```
DATABASE_PASSWORD=secure_prod_password_12345
JWT_SECRET=dGVzdF9qd3Rfc2VjcmV0X2tleV8yNTZfYml0c...
MAIL_HOST=mailhog
CORS_ORIGINS=http://localhost:3000,http://localhost:80
```

### For Production
Before deploying to production, update:

```bash
# Real email credentials
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-specific-password  # NOT regular password!

# Real payment credentials
RAZORPAY_KEY_ID=rzp_live_xxxxx
RAZORPAY_KEY_SECRET=xxxxx

# Strong database password
DATABASE_PASSWORD=min-16-chars-random-string-here

# Real domain
CORS_ORIGINS=https://yourdomain.com
FRONTEND_URL=https://yourdomain.com
```

---

## 🌐 Accessing the Application

### Local Machine
- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- API Docs (if enabled): http://localhost:8080/swagger-ui.html

### From Another Machine on Same Network
```bash
# Find your machine's IP:
# Windows: ipconfig
# macOS/Linux: ifconfig

# Then use:
curl http://192.168.1.100:8080/api/products
# Replace 192.168.1.100 with your actual IP
```

### Inside Container
```bash
# Services can communicate using hostname:
# Within API container:
curl http://database:3306  # MySQL
http://api:8080            # API (from other containers)
```

---

## 🚨 Troubleshooting

### Docker Commands Not Found
```bash
# Docker isn't installed or not in PATH
# Solution: Install Docker Desktop from https://www.docker.com/products/docker-desktop

# Verify:
docker --version
docker compose version
```

### Ports Already in Use
```bash
# Port 8080, 3306 already in use
# Find what's using it:
# Windows: netstat -ano | findstr :8080
# macOS/Linux: lsof -i :8080

# Kill the process or change port in docker-compose.yml
# Change: "8080:8080" to "8081:8080"

docker compose up  # Will use 8081 instead
```

### API Not Starting (Memory Error)
```bash
# Docker ran out of memory
# Solution: Increase Docker memory:
# Docker Desktop → Preferences/Settings → Resources → Memory → 4GB+

# Or lower Java heap in .env.production:
JAVA_OPTS=-Xmx512m -Xms256m  # Reduced from 1GB
```

### Database Won't Start
```bash
# Check MySQL container logs
docker compose logs database

# Common fixes:
# 1. Volume permissions issue:
docker compose down -v  # Remove and recreate

# 2. MySQL initialization fails:
docker compose logs database | grep ERROR

# 3. Port already in use:
netstat -ano | findstr :3306
```

### API Crashes on Startup
```bash
# Check logs for the actual error
docker compose logs api

# Common causes:
# 1. JWT_SECRET not set (check .env.production)
# 2. Database not ready (waits 45 seconds)
# 3. Memory insufficient (increase Docker memory)
# 4. Port already in use (change in docker-compose.yml)

# To debug:
docker compose logs -f api  # Watch in real-time
```

### Can't Access from Another Machine
```bash
# Firewall blocking port 8080
# Windows Firewall → Allow Docker through
# macOS: System Preferences → Security & Privacy
# Linux: sudo ufw allow 8080

# OR use IP address instead of localhost:
curl http://192.168.1.x:8080  # Get IP from ipconfig/ifconfig
```

---

## 📊 Container & Image Management

### List Running Containers
```bash
docker compose ps           # Just this project
docker ps                   # All containers on system
docker ps -a                # Including stopped containers
```

### List Images
```bash
docker images               # Show all downloaded images
docker image prune          # Remove unused images (saves disk)
```

### View Container Details
```bash
docker compose exec api java -version      # Check Java version
docker compose inspect database             # Full container details
docker stats                                # Real-time resource usage
```

### Clean Up Resources
```bash
docker system df            # Show disk usage
docker system prune         # Remove stopped containers, unused images, etc.
docker system prune -a      # Aggressive cleanup (more disk freed)
```

---

## 📈 Production Deployment

The same setup works for production! Just change environment variables:

```bash
# Create .env.production with prod values
DATABASE_PASSWORD=<strong-password>
MAIL_USERNAME=<real-email>
MAIL_PASSWORD=<email-app-password>
RAZORPAY_KEY_ID=<prod-key>
RAZORPAY_KEY_SECRET=<prod-secret>
CORS_ORIGINS=https://yourdomain.com
FRONTEND_URL=https://yourdomain.com

# Start on prod server
docker compose --env-file .env.production up -d

# Verify
curl https://yourdomain.com/actuator/health
```

---

## 🎓 Understanding Docker Compose

```yaml
version: '3.8'

services:
  database:           # MySQL service
    image: mysql:8.0-alpine
    ports: ["3306:3306"]    # MySQL port
    volumes: [mysql-data:/var/lib/mysql]  # Persistent storage
    environment: ...        # Database config
    healthcheck: ...        # Checks if DB is ready

  api:                # Spring Boot API service
    build: .          # Builds from Dockerfile in current dir
    ports: ["8080:8080"]    # API port
    depends_on:
      database:
        condition: service_healthy  # Wait for DB to be ready
    environment: ...        # App config (from .env.production)
    volumes: [api-logs:/var/log/perfume-shop]  # Persistent logs

volumes:
  mysql-data:         # Named volume for database
  api-logs:           # Named volume for logs
```

Key concepts:
- **Services**: Individual applications (MySQL, API)
- **Ports**: How to reach services (host:container)
- **Volumes**: Persistent storage (survives restart)
- **Environment**: Configuration variables
- **depends_on**: Start order and readiness checks

---

## ✨ What's Included

✅ **Complete Spring Boot Application** (Java 17, MySQL, REST APIs)  
✅ **Automatic Build** (Maven runs in Docker)  
✅ **Database Setup** (MySQL with schema)  
✅ **Health Checks** (All services validated)  
✅ **Logging** (File-based, persistent)  
✅ **Security** (Non-root user, environment secrets)  
✅ **Email Integration** (Ready for SMTP)  
✅ **Payment Integration** (Razorpay + Stripe)  

All in **3 commands**! 🚀

---

## 🎉 You're Ready!

```bash
# Just run this one command:
docker compose --env-file .env.production up --build

# Then in another terminal:
curl http://localhost:8080/actuator/health

# See "status":"UP" ? You're done! 🎊
```

No installation required (except Docker Desktop).  
No manual configuration needed.  
No troubleshooting!

Everything is automated and production-ready! ✅
