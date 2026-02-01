# 🎉 PERFUME SHOP - DEMO MODE

**One command to run everything locally with Docker!**

---

## ⚡ QUICK START (30 SECONDS)

### Prerequisites
- Docker Desktop installed ([download here](https://www.docker.com/products/docker-desktop))

### Start the Application
```bash
cd c:\Users\Hamdaan\Documents\maam
docker compose --env-file .env.demo up --build
```

### Wait for Startup
Look for this message:
```
Started PerfumeShopApplication
```

This takes ~15 minutes on first run (downloads + builds), ~30 seconds on subsequent runs.

---

## 🌐 Access the Application

### API Server
- **URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Products**: http://localhost:8080/api/products/featured

### Frontend (Optional)
- **Uncomment in docker-compose.yml** to enable React frontend
- **URL**: http://localhost:3000

---

## 👤 TEST CREDENTIALS

### Create Account
1. Go to **Register** in your app
2. Use any email (doesn't need to be real)
3. Password: Any password (min 6 characters)

### Example Credentials
```
Email: demo@example.com
Password: Demo@123456
```

---

## 💳 PAYMENT TEST KEYS

These are Razorpay TEST keys (no real charges):

```
Key ID: rzp_test_placeholder_key_id
Key Secret: rzp_test_placeholder_key_secret
```

### Payment Simulation
1. Add products to cart
2. Proceed to checkout
3. Complete payment with test keys
4. Order confirmation logged to console

---

## 📧 EMAIL IN DEMO MODE

**Email is logged to console only** (no actual sending).

When an order is placed:
- Email event is logged to console
- See order confirmation in Docker logs:
  ```bash
  docker compose logs -f api | grep -i email
  ```

To actually send emails, update `.env.demo`:
```env
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
```

---

## 📊 DATABASE

### Access MySQL Directly
```bash
# Enter MySQL shell
docker compose exec database mysql -u demo_user -p perfume_shop
# Password: demo_password_123

# List tables
SHOW TABLES;

# View products
SELECT * FROM products;

# View orders
SELECT * FROM orders;
```

### Data Persistence
- Database data saved in Docker volume `mysql-data`
- Survives container restarts
- Deleted when you run: `docker compose down -v`

### Reset Database
```bash
# Stop containers
docker compose down

# Remove volumes (WARNING: deletes all data)
docker compose down -v

# Restart (creates fresh database)
docker compose up --build
```

---

## 🔍 API ENDPOINTS TO TEST

### Products
```bash
# Get featured products
curl http://localhost:8080/api/products/featured

# Get all products
curl http://localhost:8080/api/products

# Get product by ID
curl http://localhost:8080/api/products/1

# Search products
curl "http://localhost:8080/api/products?search=perfume"
```

### Authentication
```bash
# Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "Password123!"
  }'
```

### Cart
```bash
# Add to cart (requires auth token)
curl -X POST http://localhost:8080/api/cart/add \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 2
  }'

# Get cart
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🛠️ TROUBLESHOOTING

### Problem: Docker not found
**Solution**: 
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Restart your terminal

### Problem: Port 3306 already in use
```bash
# Stop existing MySQL
docker compose down

# Or use different port in docker-compose.yml:
# Change: 3306:3306
# To: 3307:3306 (new port: 3307)
```

### Problem: Port 8080 already in use
```bash
# Kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in docker-compose.yml:
# Change: 8080:8080
# To: 8081:8080 (new port: 8081)
```

### Problem: Build fails with "mvn not found"
**Solution**: This should not happen with Docker - check Docker is running

### Problem: API won't start after database is ready
```bash
# View API logs
docker compose logs api

# Wait longer for database to fully initialize
# First run takes longer - be patient!

# Force restart
docker compose restart api
```

### Problem: Can't access API at http://localhost:8080
```bash
# Check if containers are running
docker compose ps

# Should show:
# perfume-shop-db    running
# perfume-shop-api   running

# Check API logs
docker compose logs api

# Try health endpoint
curl http://localhost:8080/actuator/health
```

---

## 📋 DEMO MODE FEATURES

✅ **Complete Automation**
- Everything builds & runs with one command
- No configuration needed
- Fresh database on each start

✅ **Local Database**
- MySQL runs in Docker
- Data persists (unless you delete volumes)
- Accessible from your machine

✅ **Test Payment Gateway**
- Razorpay TEST keys pre-configured
- Orders created but no money charged
- Perfect for testing checkout flow

✅ **Console Email Logging**
- No SMTP/email service needed
- Email events logged to console
- See order confirmations in logs

✅ **API Ready**
- All endpoints available
- Health checks configured
- Real-time logs displayed

✅ **Secure Defaults**
- JWT authentication pre-configured
- Password encryption enabled
- CORS configured for localhost

---

## 📁 IMPORTANT FILES

| File | Purpose |
|------|---------|
| `.env.demo` | Demo environment variables (pre-configured) |
| `application-demo.yml` | Spring Boot demo configuration |
| `docker-compose.yml` | Service orchestration |
| `Dockerfile` | Multi-stage build (Maven + Java) |

**Don't edit these unless you know what you're doing!**

---

## 🚀 WHAT HAPPENS WHEN YOU RUN THE COMMAND

1. **Docker reads docker-compose.yml**
   - Finds 2 services: database (MySQL) and api (Spring Boot)

2. **For API Service**
   - Downloads Maven & Java (Alpine Linux)
   - Copies your source code
   - Compiles Java code: `mvn package -DskipTests`
   - Creates optimized JAR file

3. **For Database Service**
   - Downloads MySQL 8.0-Alpine image
   - Initializes database & tables
   - Sets up demo user: `demo_user`

4. **Services Start**
   - MySQL starts and waits for health check
   - API waits for MySQL to be healthy
   - Both connect via Docker network

5. **Verification**
   - Health checks confirm both services ready
   - Real-time logs show startup progress
   - Ready to accept requests on port 8080

---

## ⏱️ TIMING

| Step | Duration | Notes |
|------|----------|-------|
| Image downloads | 30-60s | Only first time |
| Maven build | 5-8 min | Only first time |
| DB initialization | 2-3s | Every time |
| Services startup | 10-15s | Every time |
| **Total first run** | **10-15 min** | Subsequent: ~30s |

---

## 🛑 STOPPING THE APPLICATION

### Stop but Keep Data
```bash
# Ctrl+C in terminal, or:
docker compose stop
```

### Stop and Remove Everything (Keeps Data)
```bash
docker compose down
```

### Stop and Delete Everything (INCLUDING DATA!)
```bash
docker compose down -v
```

---

## 📱 USING WITH FRONTEND

### Enable Frontend in docker-compose.yml
```yaml
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    ...
```

Then uncomment the entire `frontend:` section (remove the `#` symbols).

### Start with Frontend
```bash
docker compose up --build
```

### Access Frontend
- **URL**: http://localhost:3000
- **API**: Automatically connects to http://localhost:8080

---

## 🔐 SECURITY NOTES

⚠️ **DEMO MODE ONLY!**
- Credentials and keys shown in `.env.demo` are for local testing only
- Never commit real credentials to git
- Never use in production

✅ **Safe to Share**
- Demo credentials are placeholder values
- Test Razorpay keys don't charge money
- No real data exposed

---

## 📚 NEXT STEPS

### Want to Customize?
1. Edit `.env.demo` for configuration
2. Edit `application-demo.yml` for Spring Boot settings
3. Rebuild: `docker compose up --build`

### Want to Add Data?
1. Access MySQL (see Database section)
2. Insert products, categories, etc.
3. Data persists in volume

### Want to Modify Code?
1. Edit Java source files
2. Rebuild: `docker compose up --build`
3. Changes automatically compiled

### Want to Deploy?
1. Create `.env.production` with real credentials
2. Use real database (AWS RDS, etc.)
3. Deploy to cloud platform

---

## ❓ FREQUENTLY ASKED QUESTIONS

**Q: Does this work on Windows?**
A: Yes! Docker Desktop supports Windows 10/11. Make sure WSL2 is enabled.

**Q: Does this work on Mac?**
A: Yes! Docker Desktop for Mac works perfectly.

**Q: Does this work on Linux?**
A: Yes! Install Docker and Docker Compose via package manager.

**Q: Can I use a different database?**
A: Yes! Modify `docker-compose.yml` to use PostgreSQL, MongoDB, etc.

**Q: Can I run this without Docker?**
A: Yes, but you need to install Maven, Java, MySQL separately (more setup).

**Q: Why does build take so long?**
A: First time: Maven downloads 500+ dependencies (5-8 min). Subsequent runs use cache (30s).

**Q: How do I backup my data?**
A: Docker volume is stored locally - backup location depends on your Docker setup.

**Q: Can I run multiple instances?**
A: Yes! Change ports in docker-compose.yml for each instance.

**Q: How do I view database in GUI?**
A: Use MySQL Workbench or DBeaver - connect to localhost:3306

---

## 🎯 SUMMARY

✨ **One Command to Rule Them All:**
```bash
docker compose --env-file .env.demo up --build
```

✨ **Everything Included:**
- ✅ Database (MySQL)
- ✅ API Server (Spring Boot)
- ✅ Test Keys (Razorpay)
- ✅ Console Logging (Email)

✨ **Nothing Needed:**
- ❌ Maven installation
- ❌ Java installation
- ❌ Manual configuration
- ❌ External services

✨ **Access Points:**
- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Database: localhost:3306

🚀 **That's it! Happy coding!**

---

**Need help?** Check the troubleshooting section above or review Docker logs:
```bash
docker compose logs -f
```
