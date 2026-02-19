# 🚀 Quick Deployment Guide (Railway + Vercel)

## ⏱️ Time Estimate: 30-45 minutes

---

## 📋 Prerequisites
- ✅ Code pushed to GitHub: https://github.com/Hamdan0407/Perfume
- ✅ Railway account (free): https://railway.app
- ✅ Vercel account (free): https://vercel.com

---

## 🔧 Part 1: Deploy Backend to Railway (20 mins)

### Step 1: Create Railway Project
1. Go to https://railway.app
2. Click "Start a New Project"
3. Select "Deploy from GitHub repo"
4. Choose `Hamdan0407/Perfume`
5. Railway auto-detects Spring Boot ✅

### Step 2: Add PostgreSQL Database
1. In your Railway project, click "+ New"
2. Select "Database" → "PostgreSQL"
3. Railway creates database automatically ✅

### Step 3: Add Redis (Optional - for caching)
1. Click "+ New" again
2. Select "Database" → "Redis"
3. Railway creates Redis automatically ✅

### Step 4: Configure Environment Variables
Click on your Spring Boot service → "Variables" → Add these:

```bash
# Database (Auto-filled by Railway)
DATABASE_URL=<auto-filled>

# JWT Secret (IMPORTANT: Generate new one)
JWT_SECRET=<run: openssl rand -base64 64>
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# Razorpay (Use test keys)
RAZORPAY_KEY_ID=rzp_test_YOUR_KEY
RAZORPAY_KEY_SECRET=YOUR_SECRET
RAZORPAY_WEBHOOK_SECRET=YOUR_WEBHOOK_SECRET

# Email (Gmail)
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Google AI (Optional)
GOOGLE_AI_API_KEY=your_gemini_key

# CORS (Add your Vercel URL after frontend deployment)
CORS_ALLOWED_ORIGINS=http://localhost:3000,https://your-app.vercel.app

# Admin
ADMIN_EMAIL=admin@perfumeshop.com
ADMIN_PASSWORD=Admin@123
```

### Step 5: Deploy
1. Railway automatically builds and deploys
2. Wait 3-5 minutes
3. Copy your Railway URL (e.g., `https://perfume-production.up.railway.app`)

---

## 🎨 Part 2: Deploy Frontend to Vercel (10 mins)

### Step 1: Update Frontend API URL
1. Open `frontend/axios.js`
2. Change `baseURL` to your Railway URL:
```javascript
baseURL: 'https://perfume-production.up.railway.app/api'
```
3. Commit and push:
```bash
git add frontend/axios.js
git commit -m "Update API URL for production"
git push origin main
```

### Step 2: Deploy to Vercel
```bash
cd frontend
npm install -g vercel
vercel --prod
```

Follow prompts:
- Link to existing project? **No**
- Project name? **perfume-shop**
- Directory? **./frontend**
- Build command? **npm run build**
- Output directory? **dist**

### Step 3: Get Your URL
Vercel gives you: `https://perfume-shop.vercel.app`

### Step 4: Update CORS in Railway
1. Go back to Railway
2. Update `CORS_ALLOWED_ORIGINS` to include your Vercel URL
3. Railway auto-redeploys

---

## ✅ Verification (5 mins)

1. **Open your Vercel URL**: `https://perfume-shop.vercel.app`
2. **Test:**
   - ✅ Homepage loads
   - ✅ Products display
   - ✅ Login/Register works
   - ✅ Add to cart works
   - ✅ Checkout creates order (with professional ID: ORD-20260208-001)

---

## 🎯 What Your Client Gets

1. **Live URLs:**
   - Frontend: `https://perfume-shop.vercel.app`
   - Backend API: `https://perfume-production.up.railway.app`

2. **Features:**
   - ✅ Professional order IDs (ORD-YYYYMMDD-XXX)
   - ✅ Rate limiting (security)
   - ✅ PostgreSQL database (persistent data)
   - ✅ Redis caching (fast performance)
   - ✅ Monitoring ready (Prometheus/Grafana)
   - ✅ Production-grade security

3. **Free Tier Limits:**
   - Railway: $5 free credit/month (~500 hours)
   - Vercel: Unlimited bandwidth for personal projects
   - PostgreSQL: 1GB storage
   - Redis: 100MB storage

---

## 🆘 Troubleshooting

### Backend won't start?
- Check Railway logs: Click service → "Deployments" → View logs
- Common issue: Missing environment variables

### Frontend can't connect to backend?
- Check CORS settings in Railway
- Verify `axios.js` has correct Railway URL

### Database connection error?
- Railway auto-configures DATABASE_URL
- Make sure PostgreSQL service is running

---

## 📞 Support

If deployment fails, check:
1. Railway build logs
2. Vercel deployment logs
3. Browser console for frontend errors

**Estimated Total Time: 30-45 minutes**
**Deadline: 4:00 PM (You have 2.5 hours buffer!)**
