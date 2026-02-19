# QUICK START - PRODUCTION DEPLOYMENT

## 📋 Prerequisites
- Docker & Docker Compose installed
- Generated JWT secret: `openssl rand -base64 32`
- Live Razorpay keys (NOT test keys: `rzp_live_...`)
- Email service configured (Gmail App Password or corporate SMTP)
- Database access (MySQL or PostgreSQL)
- Redis access or local Redis deployment

---

## ⚡ 5-MINUTE SETUP

### Step 1: Create Production Environment
```bash
cp .env.production.example .env.production
chmod 600 .env.production
```

### Step 2: Configure Secrets
Edit `.env.production` and update ALL `CHANGE_ME` values:

**CRITICAL - Must Update:**
```
DATABASE_URL=jdbc:mysql://your-db:3306/perfume_shop
DATABASE_USERNAME=your_user
DATABASE_PASSWORD=YOUR_SECURE_PASSWORD

REDIS_HOST=your-redis-host
REDIS_PASSWORD=YOUR_SECURE_PASSWORD

JWT_SECRET=YOUR_256BIT_SECRET  # Use: openssl rand -base64 32

RAZORPAY_KEY_ID=rzp_live_YOUR_KEY  # ✅ LIVE KEY (NOT test)
RAZORPAY_KEY_SECRET=YOUR_SECRET
RAZORPAY_WEBHOOK_SECRET=YOUR_SECRET

MAIL_USERNAME=noreply@yourdomain.com
MAIL_PASSWORD=YOUR_APP_PASSWORD

CORS_ORIGINS=https://yourdomain.com
FRONTEND_URL=https://yourdomain.com
```

### Step 3: Build & Deploy
```bash
# Build Docker images
docker-compose build

# Start services (Redis + Database + API)
docker-compose up -d

# Verify services are running
docker-compose ps

# Check health
curl http://localhost:8080/actuator/health
```

### Step 4: Test
```bash
# Test API
curl http://localhost:8080/api/products

# Test login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@yourdomain.com","password":"your_password"}'
```

---

## 🔍 Verification Checklist

```
[ ] .env.production created with all secrets
[ ] JWT_SECRET is 256+ bits (generated via openssl)
[ ] RAZORPAY_KEY_ID starts with rzp_live_ (NOT test)
[ ] MAIL credentials verified
[ ] CORS_ORIGINS matches production domain
[ ] Docker images built successfully
[ ] All services running and healthy:
    - docker-compose ps shows all services UP
    - curl /actuator/health returns {"status":"UP"}
[ ] Database populated:
    - Verify tables exist
    - Verify admin user created
[ ] Redis operational:
    - redis-cli -h redis -a password ping = PONG
```

---

## 📊 Real-time Monitoring

```bash
# Watch service status
docker-compose ps

# View live logs
docker logs -f perfume-shop-api

# Check resource usage
docker stats perfume-shop-api

# Health status
curl http://localhost:8080/actuator/health | jq

# Metrics
curl http://localhost:8080/actuator/metrics | jq
```

---

## 🚨 Emergency Troubleshooting

### Issue: Services not starting
```bash
# Check logs for startup errors
docker-compose logs api | tail -50

# Restart all services
docker-compose restart

# Full reset (⚠️ deletes data)
docker-compose down -v
docker-compose up -d
```

### Issue: Razorpay validation failed
```bash
# Verify keys are set correctly
grep RAZORPAY_ .env.production | head -3

# Check key format
echo "RAZORPAY_KEY_ID=rzp_live_..." > .env.production
echo "RAZORPAY_KEY_SECRET=..." >> .env.production

# Restart API
docker-compose restart api
```

### Issue: Redis connection refused
```bash
# Verify Redis is running
docker-compose ps redis

# Check Redis logs
docker logs perfume-shop-redis | tail -20

# Test connection
docker exec perfume-shop-redis redis-cli ping
```

### Issue: Database connection timeout
```bash
# Verify database is healthy
docker-compose ps database

# Test database connection
docker exec perfume-shop-db mysql -u root -p -e "SELECT 1"

# Check database logs
docker logs perfume-shop-db | tail -20
```

---

## 📚 Full Documentation

- **Detailed Setup:** See `PRODUCTION_MIGRATION_GUIDE.md`
- **Architecture:** See `PRODUCTION_TRANSFORMATION_SUMMARY.md`
- **Configuration:** See `application-prod.yml`
- **Troubleshooting:** See PRODUCTION_MIGRATION_GUIDE.md Phase 11

---

## 🔐 Security Reminders

⚠️ **Before Deploying:**
```
[ ] .env.production is in .gitignore (not committed)
[ ] File permissions are 600 (chmod 600 .env.production)
[ ] No hardcoded passwords in code
[ ] JWT_SECRET is unique and 256+ bits
[ ] Database password is STRONG (16+ chars, mixed case, symbols)
[ ] RAZORPAY using LIVE keys (not test)
[ ] CORS_ORIGINS restricted to production domain
[ ] SSL/TLS certificates are valid
[ ] Backup strategy documented
```

---

## ✅ Post-Deployment Tasks

1. **Setup Monitoring**
   - Prometheus or CloudWatch
   - Alert on errors >1%
   - Alert on response time >500ms

2. **Configure Backups**
   - Database backup every 6 hours
   - Redis backup every 24 hours
   - Test restore procedure

3. **Enable Logging**
   - Aggregate logs to ELK or Cloud provider
   - Set retention to 30 days
   - Archive to S3

4. **Setup Disaster Recovery**
   - Document RTO (Recovery Time Objective)
   - Document RPO (Recovery Point Objective)
   - Test recovery monthly

---

## 🎯 Expected Performance

| Metric | Target |
|--------|--------|
| API Response Time | <200ms |
| DB Query Time | <50ms |
| Concurrent Users | 1000+ |
| Uptime SLA | 99.9% |
| Payment Success | >99.5% |
| Email Delivery | >99% |

---

## 🚀 Deployment Tips

**Use staging before production:**
```bash
# Deploy to staging first
SPRING_PROFILES_ACTIVE=prod docker-compose up -d

# Test everything
curl http://staging.yourdomain.com/api/products
# ... more tests ...

# Once verified, deploy to production
```

**Zero-downtime deployment:**
```bash
# Update configuration
nano .env.production

# Gracefully restart (in-flight requests complete)
docker-compose restart api

# Verify health after restart
curl http://localhost:8080/actuator/health
```

---

## 📞 Support

For issues, check:
1. Logs: `docker logs perfume-shop-api`
2. Troubleshooting: `PRODUCTION_MIGRATION_GUIDE.md` Phase 11
3. Health: `curl http://localhost:8080/actuator/health`
4. Database: Check connection and schema
5. Redis: Verify connectivity and memory

**No More Demo Mode** - System is production-ready. Deploy with confidence! 🎉
