# Quick Reference Card - Docker Setup

One-page quick reference for common Docker commands and configuration.

## Start/Stop Commands

```bash
# Start with .env file (RECOMMENDED)
docker compose --env-file .env.production up --build

# Start in background
docker compose --env-file .env.production up -d

# View logs
docker compose logs -f api           # API logs
docker compose logs -f database      # Database logs
docker compose logs -f               # All logs

# Stop all services
docker compose down

# Stop and remove volumes (DELETES DATA)
docker compose down -v
```

## Environment File Setup

```bash
# Copy template
cp .env.production.example .env.production

# REQUIRED variables (must fill in):
JWT_SECRET=<run: openssl rand -base64 32>
DATABASE_PASSWORD=<min 8 chars>
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx xxxx xxxx xxxx    # Gmail app password
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=xxx
RAZORPAY_WEBHOOK_SECRET=xxx

# CORS for frontend
CORS_ORIGINS=http://localhost:3000
```

## Quick Health Checks

```bash
# All services running?
docker compose ps

# Database OK?
curl -s http://localhost:8080/actuator/health/db | jq '.status'

# API OK?
curl -s http://localhost:8080/actuator/health | jq '.status'

# Can register?
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@","firstName":"Test","lastName":"User"}'
```

## Key Port Mappings

| Service | Port | Access |
|---------|------|--------|
| API | 8080 | http://localhost:8080 |
| Actuator | 9090 | http://localhost:9090 |
| MySQL | 3306 | localhost:3306 |
| Frontend | 3000 | http://localhost:3000 |

## Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| Port in use | `kill -9 $(lsof -ti:8080)` |
| JWT_SECRET not set | `export JWT_SECRET=$(openssl rand -base64 32)` |
| Database won't connect | `docker compose logs database \| tail -50` |
| CORS error | Update `CORS_ORIGINS` in `.env.production` |
| Out of memory | Increase Docker memory to 4GB+ |

## Database Access

```bash
# Connect to MySQL
docker compose exec database mysql -u prod_user -p perfume_shop

# Inside MySQL:
SHOW TABLES;
SELECT COUNT(*) FROM users;
EXIT;
```

## View Container Details

```bash
# List all containers
docker compose ps -a

# View container logs
docker compose logs api | grep -i error

# Execute command in container
docker compose exec api java -version

# Check resource usage
docker stats

# Remove old images
docker system prune -a
```

## Build Only

```bash
# Build backend JAR first
mvn clean package -DskipTests

# Verify JAR
ls -lh target/perfume-shop-*.jar

# Then compose will use it
docker compose build
```

## Troubleshooting Checklist

- [ ] `.env.production` file created with values
- [ ] `JWT_SECRET` is 44+ characters
- [ ] `DATABASE_PASSWORD` is 8+ characters
- [ ] `MAIL_PASSWORD` is Gmail app password (not regular password)
- [ ] `target/perfume-shop-1.0.0.jar` exists
- [ ] Docker daemon is running
- [ ] Ports 3306, 8080, 9090 are available
- [ ] `docker compose ps` shows all services UP
- [ ] Health check passes: `curl http://localhost:8080/actuator/health`

## Full Documentation

See these files for detailed information:

- **RUN_CHECKLIST.md**: Complete step-by-step guide
- **ENVIRONMENT_VARIABLES.md**: All variable documentation
- **DOCKER_VALIDATION.md**: Validation procedures
- **COMPLETE_SETUP_SUMMARY.md**: Full overview

## API Test Endpoints

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!@","firstName":"Test","lastName":"User","phoneNumber":"1234567890"}'

# Get featured products
curl http://localhost:8080/api/products/featured | jq .

# Health check
curl http://localhost:8080/actuator/health | jq .
```

---

**Quick Links:**
- Setup Guide: RUN_CHECKLIST.md
- Troubleshooting: DOCKER_VALIDATION.md
- All Variables: ENVIRONMENT_VARIABLES.md
- Full Overview: COMPLETE_SETUP_SUMMARY.md
