# 🚀 DEPLOYMENT QUICK REFERENCE CARD

## Critical Files Modified

✅ `src/main/resources/application.yml` - Frontend URL environment variable
✅ `src/main/resources/application-dev.yml` - Media configuration
✅ `src/main/resources/application-prod.yml` - PostgreSQL & production settings
✅ `pom.xml` - PostgreSQL driver already included
✅ `nginx-media.conf` - Nginx configuration for media serving

---

## 3 Main Deployment Targets

### 🎨 **FRONTEND** → Vercel
```
Repo: React Frontend
Auto-Deploy: Yes (GitHub integration)
URL: https://yourdomain.vercel.app
Cost: Free-$20/month
```

### 🔧 **BACKEND** → Railway  
```
Repo: Spring Boot Perfume Shop
Auto-Deploy: Yes (GitHub integration)
URL: https://your-railway-app.up.railway.app
Database: PostgreSQL (auto-provisioned)
Cost: $7/month
```

### 🖥️ **MEDIA** → Nginx
```
Server: Your VPS/EC2
Path: /var/www/perfume-shop/media
URL: https://yourdomain.com/media
Cost: $5-10/month
```

---

## Secrets You Need to Generate

```bash
# Generate JWT Secret (run once)
openssl rand -base64 32

# Example output:
# AbCdEfGhIjKlMnOpQrStUvWxYzAbCdEfGhIjKlMnOpQrStUvWxYz=

# Get Razorpay Keys
# https://dashboard.razorpay.com (Live mode)
```

---

## Railway Environment Variables

```
DATABASE_URL=postgresql://... (auto-generated)
PORT=8080
SPRING_PROFILES_ACTIVE=prod

JWT_SECRET=your_generated_secret
JWT_EXPIRATION=86400000

CORS_ORIGINS=https://yourdomain.vercel.app,https://yourdomain.com
FRONTEND_URL=https://yourdomain.vercel.app

RAZORPAY_KEY_ID=rzp_live_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...

MEDIA_URL=https://yourdomain.com/media
```

---

## Nginx Server Setup

```bash
# 1. SSH to server
ssh user@your-server

# 2. Install dependencies
sudo apt update && sudo apt install -y nginx git certbot

# 3. Clone repo & setup media
git clone your-repo /var/www/perfume-shop
mkdir -p /var/www/perfume-shop/media && chmod 755 media

# 4. Setup Nginx
sudo cp /var/www/perfume-shop/nginx-media.conf \
        /etc/nginx/sites-available/perfume-shop

# 5. Edit config (replace domain & Railway URL)
sudo nano /etc/nginx/sites-available/perfume-shop

# 6. Enable & start
sudo ln -s /etc/nginx/sites-available/perfume-shop \
           /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl start nginx

# 7. Get SSL certificate
sudo certbot certonly --standalone -d yourdomain.com
```

---

## Test Deployment

```bash
# Test Backend
curl https://your-railway-app.up.railway.app/actuator/health

# Test Frontend
curl https://yourdomain.vercel.app

# Test Media
curl -X POST https://your-railway-app.up.railway.app/api/media/upload \
  -F "file=@image.jpg"

# Test CORS
curl -H "Origin: https://yourdomain.vercel.app" \
  https://your-railway-app.up.railway.app/api/products
```

---

## File Upload Flow

```
1. User uploads image in React app (Vercel)
   ↓
2. POST https://your-railway-app.up.railway.app/api/media/upload
   ↓
3. Spring Boot saves to /tmp/media
   ↓
4. Returns URL: https://yourdomain.com/media/abc123.jpg
   ↓
5. Nginx serves from /var/www/perfume-shop/media/abc123.jpg
   ↓
6. Browser displays image with CORS headers
```

---

## Razorpay Webhook Setup

```
1. Go to: https://dashboard.razorpay.com
2. Settings → Webhooks → Add New
3. URL: https://your-railway-app.up.railway.app/api/razorpay/webhook/payment
4. Events: payment.authorized, payment.failed, order.paid
5. Copy webhook secret → Add to Railway
```

---

## Troubleshooting Checklist

| Issue | Check |
|-------|-------|
| API 502 Bad Gateway | Railway logs, DATABASE_URL set |
| CORS Error | CORS_ORIGINS includes Vercel domain |
| Media 404 | Nginx config path, permissions |
| Payment fails | RAZORPAY_WEBHOOK_SECRET match |
| Frontend 404 | Vercel deployment complete |
| Database error | PostgreSQL plugin added in Railway |

---

## Critical Settings

```yaml
# application-prod.yml
ddl-auto: validate              # ✅ Never auto-update!
datasource.driver: postgresql   # ✅ PostgreSQL dialect
cors-origins: env variable      # ✅ Configured
frontend.url: env variable      # ✅ Configured
media.upload-dir: env variable  # ✅ Configured
media.url: env variable         # ✅ Configured
```

---

## Timeline

| Task | Time | Status |
|------|------|--------|
| Generate secrets | 5 min | ⏳ Do now |
| Deploy Railway | 10 min | ⏳ Do second |
| Deploy Vercel | 5 min | ⏳ Do third |
| Setup Nginx | 30 min | ⏳ Do last |
| Testing | 15 min | ⏳ Final |
| **TOTAL** | **~1 hour** | 🎉 Go live! |

---

## URLs You'll Have

```
Frontend:       https://yourdomain.vercel.app
Backend:        https://your-railway-app.up.railway.app
API:            https://your-railway-app.up.railway.app/api
Health:         https://your-railway-app.up.railway.app/actuator/health
Media:          https://yourdomain.com/media
Products:       https://your-railway-app.up.railway.app/api/products
```

---

## Emergency Rollback

```bash
# If something breaks, Railway keeps old deployments
# In Railway dashboard: Deployments → Select previous → Re-deploy

# For Nginx
git revert last-commit
git push
```

---

## Key Passwords/Secrets (SAVE SOMEWHERE SECURE)

```
JWT_SECRET:           [GENERATE & SAVE]
DATABASE_PASSWORD:    [RAILWAY AUTO-GENERATES]
RAZORPAY_KEY_ID:      [GET FROM DASHBOARD]
RAZORPAY_KEY_SECRET:  [GET FROM DASHBOARD]
RAZORPAY_WEBHOOK_SECRET: [GET FROM WEBHOOK SETUP]
```

---

## Monitoring After Deployment

```bash
# Railway Backend
# Dashboard → Project → API → Logs (real-time)

# Vercel Frontend
# Dashboard → Project → Deployments → View logs

# Nginx Server
ssh user@server
tail -f /var/log/nginx/perfume-shop-access.log
tail -f /var/log/nginx/perfume-shop-error.log

# Database
# Railway → PostgreSQL → Logs
```

---

## Auto-Updates After Deployment

```bash
# Backend (Railway)
# Auto-deploys on push to main branch
git push origin main → Railway deploys automatically

# Frontend (Vercel)
# Auto-deploys on push to main branch  
git push origin main → Vercel deploys automatically

# Nginx (Manual)
ssh user@server
cd /var/www/perfume-shop
git pull origin main
sudo systemctl reload nginx
```

---

## Cost Summary

| Service | Monthly | Annual | Notes |
|---------|---------|--------|-------|
| Railway | $7 | $84 | Includes free db backup |
| Vercel | Free | Free | Free tier sufficient |
| Nginx | $5-10 | $60-120 | Basic VPS tier |
| Domain | - | $12 | Registrar |
| **Total** | **$12-17** | **$156-216** | Dirt cheap! 💰 |

---

## You're Ready!

✅ Code configured for PostgreSQL  
✅ Environment variables setup  
✅ Nginx configuration created  
✅ Razorpay integrated  
✅ Media upload configured  
✅ Security hardened  

**Next: Follow DEPLOYMENT_VERCEL_RAILWAY_NGINX.md for step-by-step guide**

