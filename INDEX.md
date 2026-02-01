# 📖 PERFUME SHOP - DOCUMENTATION INDEX

**Complete setup guide for running the Perfume Shop application in DEMO mode**

---

## 🚀 QUICK START (Choose Your Path)

### "Just Run It!" (30 seconds)
You want to start the app NOW:
1. **Read**: [DEMO_QUICK_REFERENCE.md](DEMO_QUICK_REFERENCE.md) (1 min)
2. **Run**: `docker compose --env-file .env.demo up --build`
3. **Done!**

### "I Want to Understand It" (15 minutes)
You want to know what's happening:
1. **Read**: [DEMO_SETUP_COMPLETE.md](DEMO_SETUP_COMPLETE.md) (5 min)
2. **Read**: [DEMO_RUN.md](DEMO_RUN.md) (10 min)
3. **Run**: `docker compose --env-file .env.demo up --build`
4. **Test**: Use the endpoints and test credentials

### "I Want All the Details" (30 minutes)
You want comprehensive information:
1. **Read**: [DEMO_SETUP_COMPLETE.md](DEMO_SETUP_COMPLETE.md) (5 min)
2. **Read**: [DEMO_RUN.md](DEMO_RUN.md) (20 min)
3. **Explore**: Source code & configuration files
4. **Run**: `docker compose --env-file .env.demo up --build`
5. **Customize**: Edit `.env.demo` or `application-demo.yml` as needed

---

## 📚 DOCUMENTATION FILES

### Essential Files

| File | Purpose | Read Time | Start Here? |
|------|---------|-----------|------------|
| **DEMO_QUICK_REFERENCE.md** | One-page cheat sheet with all essentials | 1 min | ✅ YES |
| **DEMO_RUN.md** | Complete guide with examples and troubleshooting | 15 min | ✅ After quick ref |
| **DEMO_SETUP_COMPLETE.md** | Technical details and validation report | 10 min | For understanding |

### Configuration Files (Don't Edit Unless You Know What You're Doing!)

| File | Purpose | Status |
|------|---------|--------|
| `.env.demo` | Environment variables (pre-configured) | Pre-done ✅ |
| `application-demo.yml` | Spring Boot demo profile (NEW!) | Pre-done ✅ |
| `docker-compose.yml` | Service orchestration | Pre-done ✅ |
| `Dockerfile` | Multi-stage build | Pre-done ✅ |

---

## ⚡ THE COMMAND (Copy & Paste)

```bash
docker compose --env-file .env.demo up --build
```

That's all you need! ✨

---

## 🎯 WHAT YOU GET

### Immediately Available
- ✅ Spring Boot API on http://localhost:8080
- ✅ MySQL database on localhost:3306
- ✅ All endpoints working
- ✅ User authentication (JWT)
- ✅ Shopping cart & checkout
- ✅ Order management

### Pre-Configured
- ✅ Demo user: demo@example.com / Demo@123456
- ✅ Razorpay TEST mode (no real charges)
- ✅ Email logging to console
- ✅ Fresh database (recreated each startup)
- ✅ All health checks

### No Setup Required
- ✅ No MySQL installation
- ✅ No Java installation
- ✅ No Maven installation
- ✅ No configuration needed
- ✅ No credentials to generate

---

## 🌐 ACCESS POINTS

After running the command:

| Service | URL | Purpose |
|---------|-----|---------|
| **API Root** | http://localhost:8080 | Main backend |
| **Health Check** | http://localhost:8080/actuator/health | Server status |
| **Featured Products** | http://localhost:8080/api/products/featured | Get featured items |
| **All Products** | http://localhost:8080/api/products | Browse catalog |
| **MySQL Database** | localhost:3306 | Direct DB access |

### Test Data
```
Email: demo@example.com
Password: Demo@123456
```

---

## 📋 SETUP VALIDATION

### Pre-Launch Checks ✅
- [x] docker-compose.yml YAML syntax: **VALIDATED**
- [x] Environment variables: **PRE-CONFIGURED**
- [x] Spring Boot profiles: **READY**
- [x] Service dependencies: **CONFIGURED**
- [x] Database initialization: **AUTOMATIC**
- [x] Email configuration: **CONSOLE LOGGING**
- [x] Payment gateway: **TEST MODE**

### Ready to Deploy?
**YES!** Run: `docker compose --env-file .env.demo up --build`

---

## 🛠️ TROUBLESHOOTING QUICK GUIDE

### Port Already in Use?
Edit `docker-compose.yml`:
- Change `8080:8080` to `8081:8080` for API
- Change `3306:3306` to `3307:3306` for MySQL

### Build Takes Forever?
This is normal! Maven is downloading dependencies (~5-8 min first time only).

### Can't Access API?
```bash
# Check if containers are running
docker compose ps

# View logs
docker compose logs -f api

# Wait longer - initial startup takes ~15 minutes
```

### Want a Fresh Start?
```bash
docker compose down -v
docker compose up --build
```

**For more troubleshooting**, see [DEMO_RUN.md](DEMO_RUN.md#troubleshooting)

---

## 📁 PROJECT STRUCTURE

```
maam/
├── Configuration
│   ├── .env.demo                  ← Environment variables (DEMO)
│   ├── docker-compose.yml         ← Service orchestration
│   ├── Dockerfile                 ← Multi-stage build
│   └── pom.xml                    ← Maven dependencies
│
├── Documentation (READ THESE!)
│   ├── DEMO_QUICK_REFERENCE.md    ← Start here! (1 min)
│   ├── DEMO_RUN.md                ← Full guide (15 min)
│   ├── DEMO_SETUP_COMPLETE.md     ← Details (10 min)
│   ├── INDEX.md                   ← This file
│   └── (15+ other guides)
│
├── Source Code
│   ├── src/main/java/com/perfume/shop/
│   │   ├── PerfumeShopApplication.java  ← Main class
│   │   ├── controller/                  ← API endpoints
│   │   ├── service/                     ← Business logic
│   │   ├── entity/                      ← Database models
│   │   └── repository/                  ← Data access
│   │
│   └── src/main/resources/
│       ├── application.yml              ← Default config
│       ├── application-prod.yml         ← Production config
│       └── application-demo.yml         ← DEMO config (NEW!)
│
└── Frontend (Optional)
    └── frontend/
        ├── src/
        ├── package.json
        └── vite.config.js
```

---

## ⏱️ TIMING EXPECTATIONS

### First Run
```
Docker image downloads:    30-60 seconds
Maven build & compile:     5-8 minutes
Database initialization:   2-3 seconds
Services startup:          10-15 seconds
─────────────────────────
Total time:                ~10-15 minutes

Look for: "Started PerfumeShopApplication"
```

### Subsequent Runs
```
Services startup:          ~30 seconds
(Everything cached, just restart)
```

---

## 🔐 SECURITY & DEMO MODE

### Safe for Local Testing ✅
- Demo credentials are placeholder values
- No real customer data exposed
- Test Razorpay keys don't charge money
- All configuration local to Docker

### NOT Safe for Production ❌
- Update credentials for production deployment
- Use real database (AWS RDS, etc.)
- Update email service (real SMTP)
- Use production Razorpay keys

---

## 📊 DEMO MODE CONFIGURATION

### What's Different from Production?

| Feature | Demo Mode | Production |
|---------|-----------|------------|
| **Database** | Fresh each startup | Persistent |
| **Email** | Console logging | Real SMTP |
| **Payments** | TEST keys | LIVE keys |
| **DB Schema** | create-drop | validate |
| **Setup** | 1 command | Multi-step |
| **Logging** | INFO level | WARNING level |

### Files That Enable Demo Mode

1. **.env.demo**
   - Pre-configured environment variables
   - Sets SPRING_PROFILES_ACTIVE=demo

2. **application-demo.yml** (NEW!)
   - Spring Boot loads this when profile=demo
   - Configures: email disabled, fresh DB, test keys

3. **docker-compose.yml**
   - Loads .env.demo as environment file
   - Passes SPRING_PROFILES_ACTIVE to Spring Boot

---

## 🎓 HOW IT WORKS

### The Magic: Spring Profiles

1. **docker-compose.yml** loads `.env.demo`
2. **.env.demo** sets `SPRING_PROFILES_ACTIVE=demo`
3. Spring Boot reads `application-demo.yml`
4. Demo configuration overrides defaults:
   - Database: `create-drop` (fresh each run)
   - Email: Console only (no SMTP)
   - Payments: TEST mode only
   - Logging: INFO level

### The Build: Multi-Stage Dockerfile

1. **Stage 1 (Builder)**
   - Downloads Maven & Java
   - Compiles your source code
   - Creates JAR file

2. **Stage 2 (Runtime)**
   - Downloads JDK only (smaller image)
   - Copies JAR from Stage 1
   - Runs Spring Boot application

---

## ✨ KEY FEATURES ENABLED

✅ **Complete Spring Boot API**
- All REST endpoints operational
- JWT authentication working
- Database persistence active
- Health checks configured

✅ **Local MySQL Database**
- Automatically initialized
- Pre-configured user/password
- Data persists in Docker volumes
- Fresh schema each startup (demo mode)

✅ **Razorpay Integration**
- TEST mode pre-configured
- No actual charges
- Perfect for testing checkout
- Webhook support ready

✅ **Email System**
- Console logging enabled
- No SMTP service needed
- Order confirmations logged
- Real-time visibility

✅ **Shopping Features**
- Browse products
- Add to cart
- Checkout process
- Order history
- User accounts

---

## 🚀 NEXT STEPS

### Now
1. Read [DEMO_QUICK_REFERENCE.md](DEMO_QUICK_REFERENCE.md) (1 min)
2. Run: `docker compose --env-file .env.demo up --build`
3. Wait for: "Started PerfumeShopApplication"
4. Test: http://localhost:8080/actuator/health

### After Verification
1. Create test account
2. Browse products
3. Add to cart
4. Test checkout
5. View order in database

### For Customization
1. Edit `.env.demo` for different values
2. Edit `application-demo.yml` for Spring Boot config
3. Edit source code in `src/`
4. Rebuild: `docker compose up --build`

### For Production
1. Create `.env.production` with real credentials
2. Use production profile: `SPRING_PROFILES_ACTIVE=prod`
3. Deploy to cloud platform
4. Monitor health checks

---

## 📞 HELP & SUPPORT

### Quick Help
→ See [DEMO_QUICK_REFERENCE.md](DEMO_QUICK_REFERENCE.md)

### Full Troubleshooting
→ See "Troubleshooting" section in [DEMO_RUN.md](DEMO_RUN.md)

### Common Issues

**Problem**: Port in use  
**Solution**: Edit docker-compose.yml and change ports

**Problem**: Build takes forever  
**Solution**: Wait! Maven downloads dependencies (~8 min first time)

**Problem**: Can't access API  
**Solution**: `docker compose logs api` to see errors

**Problem**: Database connection error  
**Solution**: Wait for "healthy" status in `docker compose ps`

---

## 📝 SUMMARY

### What Was Created
- ✅ `.env.demo` - Pre-configured environment
- ✅ `application-demo.yml` - Spring Boot demo profile
- ✅ Updated `docker-compose.yml` - Service orchestration
- ✅ 3 comprehensive documentation files

### What You Do
- Run: `docker compose --env-file .env.demo up --build`
- Wait: ~15 minutes first time
- Access: http://localhost:8080
- Test: Use demo credentials or create account
- Enjoy!

### What You Get
- Complete Spring Boot API
- Local MySQL database
- Razorpay TEST mode
- All endpoints working
- No configuration needed

---

## 🎉 YOU'RE ALL SET!

Start here: [DEMO_QUICK_REFERENCE.md](DEMO_QUICK_REFERENCE.md)

Then run:
```bash
docker compose --env-file .env.demo up --build
```

That's it! Happy coding! 🚀

---

**Version**: 1.0  
**Date**: January 19, 2026  
**Status**: ✅ Ready to Deploy
