# PostgreSQL Migration Guide

## 🎯 What Changed

Migrated from **H2 in-memory database** to **PostgreSQL** for production-ready persistent storage.

---

## 🚀 Quick Start

### Option 1: Using Docker (Recommended)

1. **Start PostgreSQL:**
```bash
docker-compose -f docker-compose.postgres.yml up -d
```

2. **Verify it's running:**
```bash
docker ps
```

3. **Start your Spring Boot app:**
```bash
mvn spring-boot:run
```

### Option 2: Local PostgreSQL Installation

1. **Install PostgreSQL 16:**
   - Windows: Download from https://www.postgresql.org/download/windows/
   - Mac: `brew install postgresql@16`
   - Linux: `sudo apt install postgresql-16`

2. **Create database:**
```bash
psql -U postgres
CREATE DATABASE perfume_shop;
\q
```

3. **Start your Spring Boot app:**
```bash
mvn spring-boot:run
```

---

## 🔧 Configuration

### Default Settings (Development)
```yaml
Database: perfume_shop
Host: localhost
Port: 5432
Username: postgres
Password: postgres
```

### Environment Variables (Production)
```bash
export DB_HOST=your-db-host
export DB_PORT=5432
export DB_NAME=perfume_shop
export DB_USERNAME=your-username
export DB_PASSWORD=your-secure-password
```

---

## 🎨 pgAdmin Access

Access the database GUI at: **http://localhost:5050**

**Login:**
- Email: `admin@perfume.com`
- Password: `admin`

**Add Server:**
1. Right-click "Servers" → "Register" → "Server"
2. **General Tab:**
   - Name: `Perfume Shop`
3. **Connection Tab:**
   - Host: `postgres` (if using Docker) or `localhost`
   - Port: `5432`
   - Database: `perfume_shop`
   - Username: `postgres`
   - Password: `postgres`

---

## 📊 Key Improvements

### Before (H2)
❌ In-memory - data lost on restart
❌ Not production-ready
❌ Limited features
❌ No persistence

### After (PostgreSQL)
✅ Persistent storage
✅ Production-ready
✅ Full SQL features
✅ Connection pooling (HikariCP)
✅ Better performance
✅ Scalable

---

## 🔍 Verify Migration

### Check Database Connection
```bash
# Using Docker
docker exec -it perfume_shop_postgres psql -U postgres -d perfume_shop

# Local installation
psql -U postgres -d perfume_shop
```

### List Tables
```sql
\dt
```

### Check Data
```sql
SELECT * FROM users LIMIT 5;
SELECT * FROM products LIMIT 5;
SELECT * FROM orders LIMIT 5;
```

---

## 🛠️ Troubleshooting

### Port 5432 Already in Use
```bash
# Find process using port 5432
netstat -ano | findstr :5432

# Kill the process (Windows)
taskkill /PID <process_id> /F

# Or change port in docker-compose.postgres.yml
ports:
  - "5433:5432"  # Use 5433 instead
```

### Connection Refused
```bash
# Check if PostgreSQL is running
docker ps

# Check logs
docker logs perfume_shop_postgres

# Restart container
docker-compose -f docker-compose.postgres.yml restart
```

### Data Not Persisting
- Check that `ddl-auto: update` is set (not `create-drop`)
- Verify Docker volume exists: `docker volume ls`

---

## 📝 Next Steps

1. ✅ PostgreSQL is now configured
2. ⏭️ Ready to implement abandoned cart recovery
3. ⏭️ Can add database migrations (Flyway/Liquibase) later

---

## 🗑️ Cleanup (Optional)

### Stop PostgreSQL
```bash
docker-compose -f docker-compose.postgres.yml down
```

### Remove Data (WARNING: Deletes all data)
```bash
docker-compose -f docker-compose.postgres.yml down -v
```

---

## 💡 Production Deployment

For production, use managed PostgreSQL:
- **Railway**: Automatic PostgreSQL provisioning
- **AWS RDS**: Managed PostgreSQL
- **Heroku Postgres**: Easy setup
- **DigitalOcean**: Managed databases

Just update environment variables with production credentials!
