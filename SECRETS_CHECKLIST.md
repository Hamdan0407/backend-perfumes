# Perfume Shop: Secrets Checklist

Before deploying, ensure you have set the following secrets in your .env.production files:

## Backend (.env.production)
- DATABASE_URL
- DATABASE_USERNAME
- DATABASE_PASSWORD
- JWT_SECRET (must be strong and unique)
- MAIL_HOST (e.g., smtp.gmail.com)
- MAIL_PORT (e.g., 587)
- MAIL_USERNAME (your email address)
- MAIL_PASSWORD (your email app password)
- STRIPE_API_KEY
- STRIPE_WEBHOOK_SECRET
- RAZORPAY_KEY_ID
- RAZORPAY_KEY_SECRET
- RAZORPAY_WEBHOOK_SECRET
- FRONTEND_URL (your deployed frontend URL)

## Frontend (frontend/.env.production)
- VITE_API_URL (your backend API URL)
- VITE_STRIPE_PUBLISHABLE_KEY

---

**Never commit real secrets to git.**

If you need example values, see the .env.production.example files.
