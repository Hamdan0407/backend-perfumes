# Perfume Shop Production Deployment Guide

This guide will help you deploy your Spring Boot + React (Vite) startup project in production using Docker Compose, with secure environment variables, email/password reset, and cloud-ready configuration.

---

## 1. Prerequisites
- Docker & Docker Compose installed
- SMTP/email credentials (for password reset)
- Stripe/Razorpay keys (for payments)
- (Optional) Cloud server (AWS, DigitalOcean, etc.)

---

## 2. Environment Variables

### Backend (.env.production)
- Edit `.env.production` in the project root:
  - Set real values for:
    - `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
    - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
    - `JWT_SECRET` (use a strong random value)
    - `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, etc.
    - `RAZORPAY_KEY_ID`, `RAZORPAY_KEY_SECRET`, etc.

### Frontend (frontend/.env.production)
- Edit `frontend/.env.production`:
  - Set `VITE_API_URL` to your backend URL (e.g., `https://api.yourdomain.com/api`)
  - Set `VITE_STRIPE_PUBLISHABLE_KEY` to your Stripe publishable key

---

## 3. Build & Run (One Command)

From the project root, run:

```sh
docker compose up --build -d
```

- This will build and start the backend, frontend, database, and Nginx.
- The frontend will be available on port 80 (or your cloud server's public IP).
- The backend API will be available at `/api`.

---

## 4. Email/Password Reset
- Ensure your SMTP credentials are correct in `.env.production`.
- For Gmail, use an App Password (not your main password).
- For production, use a real SMTP provider (Gmail, Outlook, SendGrid, etc.).

---

## 5. Database
- By default, MySQL runs in a Docker container.
- Data is persisted in a Docker volume.
- For cloud, use a managed database and update `DATABASE_URL`.

---

## 6. Payments
- Set Stripe and/or Razorpay keys in `.env.production`.
- Test payments in sandbox mode before going live.

---

## 7. Logs & Monitoring
- Backend logs: `/var/log/perfume-shop/perfume-shop.log` (inside backend container)
- Frontend logs: Nginx logs in `/var/log/nginx/` (inside frontend container)

---

## 8. Stopping & Updating
- To stop: `docker compose down`
- To update: `git pull` (if using git), then `docker compose up --build -d`

---

## 9. Cloud Deployment
- Copy all files to your cloud server.
- Set up DNS for your domain to point to the server.
- Open ports 80 (HTTP) and 443 (HTTPS) in your firewall.
- (Optional) Set up SSL with a reverse proxy or Let's Encrypt.

---

## 10. Troubleshooting
- Check container logs: `docker compose logs <service>`
- Ensure all environment variables are set and correct.
- For email issues, check SMTP credentials and provider limits.

---

## 11. Secrets & Security
- Never commit real secrets to git.
- Use strong, unique passwords and JWT secrets.
- Rotate credentials regularly.

---

## 12. Support
- For help, check the README or contact your developer.

---

**You are ready to launch!**

---

