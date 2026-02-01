# 🚀 COMPLETE END-TO-END SETUP GUIDE

**Status**: Ready to Deploy  
**Method**: Docker Compose (No Manual Configuration Needed)  
**Time Required**: 5-15 minutes (after prerequisites)

---

## 📋 Prerequisites

Before you start, ensure you have:

### Required
- **Docker Desktop** (includes Docker + Docker Compose)
  - Download: https://www.docker.com/products/docker-desktop
  - Windows: Run installer, restart computer
  - Mac: Run installer, restart computer
  - Linux: `sudo apt install docker.io docker-compose`

- **Java 17+**
  - Download: https://jdk.java.net/
  - Or: `java -version` to check if already installed

- **Maven**
  - Download: https://maven.apache.org/
  - Or: `mvn --version` to check if already installed

### Recommended
- 4GB+ RAM
- 10GB+ free disk space
- Ports 3306, 8080, 9090 available

---

## 🎯 Quick Start - 5 Minutes

### Option 1: Automated Setup (Windows PowerShell)

1. **Open PowerShell as Administrator**
2. **Run the automated setup script:**
   ```powershell
   .\AUTOMATED_SETUP.ps1
   ```
3. **Follow the prompts** - the script will:
   - ✅ Validate all prerequisites
   - ✅ Generate secure environment variables
   - ✅ Create .env.production file
   - ✅ Build the backend JAR
   - ✅ Start the Docker containers

### Option 2: Automated Setup (macOS/Linux)

1. **Make script executable:**
   ```bash
   chmod +x AUTOMATED_SETUP.sh
   ```
2. **Run the script:**
   ```bash
   ./AUTOMATED_SETUP.sh
   ```

### Option 3: Manual Setup (5 Minutes)

If you prefer to do it yourself:

#### Step 1: Create Environment File (1 minute)
```bash
# Copy the example file
cp .env.production.example .env.production

# Generate a secure JWT secret
# Windows PowerShell:
[Convert]::ToBase64String((1..32 | ForEach-Object { [byte](Get-Random -Minimum 0 -Maximum 256) }))

# macOS/Linux:
openssl rand -base64 32

# Add this value to .env.production as JWT_SECRET=<value>
```

#### Step 2: Build Backend (3-5 minutes)
```bash
mvn clean package -DskipTests
# Wait for "BUILD SUCCESS"
```

#### Step 3: Start Docker (2 minutes)
```bash
docker compose --env-file .env.production up --build
# Wait for: "Started PerfumeShopApplication"
```

#### Step 4: Verify (1 minute)
In another terminal:
```bash
# Check health
curl http://localhost:8080/actuator/health

# Get products
curl http://localhost:8080/api/products/featured
```

---

## 📊 What Gets Created/Started

### Services (3 Total)

| Service | Port | Container | Status |
|---------|------|-----------|--------|
| **MySQL Database** | 3306 | mysql:8.0-alpine | Auto-started |
| **Spring Boot API** | 8080 | Custom (Java 17) | Auto-started |
| **React Frontend** | 3000 | (Optional) | Commented out |

### Files Created

| File | Purpose |
|------|---------|
| `.env.production` | Environment variables (created from .env.production.example) |
| `target/perfume-shop-1.0.0.jar` | Backend JAR (built by Maven) |
| Docker containers | MySQL and API containers (created by docker-compose) |

### Volumes Created

| Volume | Purpose | Persistence |
|--------|---------|-------------|
| `mysql-data` | Database files | ✅ Survives restart |
| `api-logs` | Application logs | ✅ Survives restart |

---

## ✅ Verification Steps

### 1. Check Services Running
```bash
docker compose ps
```
Expected output: 2 services with status "Up"

### 2. Check Database
```bash
docker compose exec database mysql -u prod_user -p perfume_shop -e "SELECT 1;"
# Enter password when prompted (from .env.production)
```
Expected: `1 | 1`

### 3. Check API Health
```bash
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP","components":{"db":{"status":"UP"},...}}`

### 4. Get Products
```bash
curl http://localhost:8080/api/products/featured
```
Expected: JSON array of products

### 5. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"Test123!@","firstName":"Test","lastName":"User","phoneNumber":"1234567890"}'
```
Expected: User created with JWT token

---

## 🛑 Stop & Cleanup

### Stop Services (Keep Data)
```bash
docker compose down
# Data in volumes is preserved
```

### Stop & Delete Everything
```bash
docker compose down -v
# ⚠️ WARNING: This deletes all data!
```

### View Logs
```bash
# All services
docker compose logs

# Specific service
docker compose logs -f api
docker compose logs -f database

# Last 100 lines
docker compose logs --tail 100
```

### Restart a Service
```bash
docker compose restart api
docker compose restart database
```

---

## 🔧 Common Commands

### Development Workflow
```bash
# Start services in background
docker compose --env-file .env.production up -d

# Check status
docker compose ps

# View logs
docker compose logs -f api

# Run commands in container
docker compose exec api bash
docker compose exec database mysql -u prod_user -p perfume_shop

# Stop services
docker compose down
```

### Troubleshooting
```bash
# Check if ports are in use
lsof -i :8080          # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Check Docker daemon status
docker ps

# Check Docker Compose configuration
docker compose config

# Rebuild images
docker compose build --no-cache

# Remove unused images
docker system prune
```

---

## 📝 Environment Variables Explained

### Critical Variables (Must Configure for Production)
- **JWT_SECRET** - Cryptographic key for JWT tokens (auto-generated)
- **DATABASE_PASSWORD** - MySQL password (auto-generated)
- **MAIL_USERNAME** - Email address for sending emails
- **MAIL_PASSWORD** - Email account password or app-specific password
- **RAZORPAY_KEY_ID** - Payment provider credentials
- **RAZORPAY_KEY_SECRET** - Payment provider credentials

### Important Variables (Pre-configured)
- **CORS_ORIGINS** - Allowed frontend domains
- **FRONTEND_URL** - Your frontend URL
- **MAIL_HOST** - SMTP server (default: smtp.gmail.com)
- **MAIL_PORT** - SMTP port (default: 587)

### Optional Variables
- 40+ additional variables for fine-tuning
- See ENVIRONMENT_VARIABLES.md for complete reference

---

## 🌐 Accessing the Application

### Local Machine
- **API**: http://localhost:8080
- **Health**: http://localhost:8080/actuator/health
- **Products**: http://localhost:8080/api/products/featured
- **Swagger**: http://localhost:8080/swagger-ui.html (if enabled)

### From Another Machine on Same Network
Replace `localhost` with your machine's IP address:
- **API**: http://192.168.x.x:8080 (where 192.168.x.x is your IP)

### From Inside Container
Use service name instead of localhost:
- **API**: http://api:8080
- **Database**: database:3306

---

## 🔒 Security Considerations

### For Development ✅
The setup uses test values for email and payment providers - fine for development!

### For Production ⚠️
Before deploying to production, update:

1. **Email Settings**
   ```
   MAIL_USERNAME=your-real-email@gmail.com
   MAIL_PASSWORD=your-gmail-app-password  # NOT your regular password!
   ```

2. **Payment Provider Credentials**
   ```
   RAZORPAY_KEY_ID=rzp_live_xxxxx        # From Razorpay dashboard
   RAZORPAY_KEY_SECRET=xxxxx             # From Razorpay dashboard
   RAZORPAY_WEBHOOK_SECRET=xxxxx         # From Razorpay webhooks
   ```

3. **HTTPS/SSL**
   - Enable HTTPS on your domain
   - Update CORS_ORIGINS to use https://
   - Update FRONTEND_URL to use https://

4. **Strong Passwords**
   - DATABASE_PASSWORD: 16+ random characters
   - JWT_SECRET: Already 256 bits of randomness

5. **Secure Storage**
   - Never commit .env.production to git
   - Use a secrets manager (AWS Secrets, Vault, etc.) for prod
   - Rotate credentials regularly

---

## 📊 Performance Tips

### For Better Performance
```bash
# Increase memory for Docker
# Docker Desktop Settings → Resources → Memory: 4GB+

# Increase database connections if slow
DATABASE_POOL_SIZE=30  # in .env.production

# Enable compression
COMPRESSION_ENABLED=true

# Optimize JVM
JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"
```

### Monitor Resource Usage
```bash
# Real-time stats
docker stats

# Disk usage
docker system df

# Memory usage
docker compose exec api java -XshowSettings:vm -version
```

---

## 🆘 Troubleshooting

### Port Already in Use
```bash
# Find what's using port 8080
lsof -i :8080              # macOS/Linux
netstat -ano | findstr :8080  # Windows

# Kill the process
kill -9 <PID>              # macOS/Linux
taskkill /PID <PID> /F     # Windows

# Or use different port in docker-compose.yml
# Change line: "8080:8080" to "8081:8080"
```

### Docker Won't Start
```bash
# Check Docker daemon
docker ps

# Start Docker Desktop (if not running)
# Then try again
```

### Database Connection Error
```bash
# Check if database is healthy
docker compose ps

# View database logs
docker compose logs database

# Restart database
docker compose restart database
```

### API Not Responding
```bash
# Check API logs
docker compose logs api

# Check health endpoint
curl http://localhost:8080/actuator/health

# Restart API
docker compose restart api
```

### Can't Connect from Another Machine
```bash
# Check your firewall allows port 8080
# Check your machine's IP address:
# Windows: ipconfig
# macOS/Linux: ifconfig

# Use IP instead of localhost:
# curl http://192.168.1.100:8080/api/products/featured
```

For more issues, see: [DOCKER_VALIDATION.md](DOCKER_VALIDATION.md)

---

## 📚 Additional Resources

- **Setup Documentation**: [RUN_CHECKLIST.md](RUN_CHECKLIST.md)
- **Environment Variables**: [ENVIRONMENT_VARIABLES.md](ENVIRONMENT_VARIABLES.md)
- **Troubleshooting**: [DOCKER_VALIDATION.md](DOCKER_VALIDATION.md)
- **Architecture**: [COMPLETE_SETUP_SUMMARY.md](COMPLETE_SETUP_SUMMARY.md)
- **Quick Reference**: [DOCKER_QUICK_REFERENCE.md](DOCKER_QUICK_REFERENCE.md)

---

## ✨ What's Included

### Code & Configuration
✅ Spring Boot API (Java 17, production-ready)  
✅ MySQL Database (8.0, persistent storage)  
✅ React Frontend (optional, commented out)  
✅ Docker Compose (3 services)  
✅ Nginx Configuration (for frontend)  

### Documentation
✅ Setup guides (manual and automated)  
✅ Environment variable reference  
✅ Troubleshooting procedures  
✅ Architecture documentation  
✅ Deployment checklist  

### Security
✅ Non-root containers  
✅ Environment-based secrets  
✅ Password encryption  
✅ JWT token management  
✅ CORS protection  

### Features
✅ Health checks (all services)  
✅ Data persistence (volumes)  
✅ Network isolation  
✅ Logging (file-based)  
✅ Email integration  
✅ Payment gateway integration  

---

## 🎉 You're All Set!

The automated setup script handles everything. Just run it and follow the prompts!

### Next Steps:
1. Run: `.\AUTOMATED_SETUP.ps1` (PowerShell) or `./AUTOMATED_SETUP.sh` (macOS/Linux)
2. Answer the prompts
3. Wait for "SETUP COMPLETE!"
4. Start containers: `docker compose --env-file .env.production up --build`
5. Verify: `curl http://localhost:8080/actuator/health`

That's it! Your application is running in Docker! 🚀
