# 🎯 DEMO MODE - QUICK REFERENCE CARD

Print this page and keep it by your computer while running DEMO mode!

---

## ⚡ ONE-LINER TO RUN EVERYTHING

```bash
docker compose --env-file .env.demo up --build
```

**That's it!** No additional setup needed.

---

## 🌐 URLS TO ACCESS

| Service | URL | Purpose |
|---------|-----|---------|
| **API** | http://localhost:8080 | Main backend |
| **Health** | http://localhost:8080/actuator/health | Server status |
| **Products** | http://localhost:8080/api/products/featured | Get products |
| **Frontend** | http://localhost:3000 | React app (optional) |
| **MySQL** | localhost:3306 | Database (direct access) |

---

## 👤 TEST LOGIN

```
Email: demo@example.com
Password: Demo@123456
```

**Or create a new account** - email doesn't need to be real!

---

## 💳 PAYMENT TEST

Razorpay TEST mode (no real charges):
- Key ID: `rzp_test_placeholder_key_id`
- Key Secret: `rzp_test_placeholder_key_secret`

Add products → Checkout → Use test payment info

---

## 📊 DATABASE ACCESS

```bash
# Open MySQL shell
docker compose exec database mysql -u demo_user -p

# Password: demo_password_123
# Then type: use perfume_shop;
```

Common commands:
```sql
SHOW TABLES;           -- See all tables
SELECT * FROM users;   -- See users
SELECT * FROM orders;  -- See orders
```

---

## 📋 DOCKER COMMANDS

```bash
# View logs
docker compose logs -f api

# View just last 50 lines
docker compose logs --tail=50 api

# Check running services
docker compose ps

# Restart API
docker compose restart api

# Restart database
docker compose restart database

# Stop everything (keep data)
docker compose stop

# Stop and remove (keep data)
docker compose down

# Stop and DELETE DATA
docker compose down -v
```

---

## 🛑 STOP THE APP

Press **Ctrl+C** in the terminal where it's running, or:

```bash
docker compose down
```

---

## ⏱️ WAIT TIMES

- **First run**: 10-15 minutes (building)
- **After that**: 30 seconds (everything cached)

Look for: `Started PerfumeShopApplication`

---

## 🆘 HELP

| Problem | Solution |
|---------|----------|
| Port 8080 in use | Change in `docker-compose.yml` to `8081:8080` |
| Port 3306 in use | Change in `docker-compose.yml` to `3307:3306` |
| Build fails | Wait longer, try again |
| Can't connect | Check `docker compose ps` |
| Need fresh start | Run `docker compose down -v` then `up --build` |

---

## 📁 KEY FILES

- `.env.demo` ← Environment config
- `DEMO_RUN.md` ← Full documentation
- `docker-compose.yml` ← Service setup
- `Dockerfile` ← Build instructions

**Don't edit these!**

---

## 🚀 WHAT WORKS

✅ Browse products  
✅ Create account  
✅ Login & logout  
✅ Add to cart  
✅ Checkout (test mode)  
✅ View orders  
✅ View products  

---

## 📧 EMAIL IN LOGS

When orders are placed, email is logged to console:

```bash
docker compose logs -f api | grep -i email
```

---

## ✨ YOU'RE READY!

Run this command and start shopping:

```bash
docker compose --env-file .env.demo up --build
```

Have fun! 🎉
