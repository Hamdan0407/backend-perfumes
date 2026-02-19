# Quick Email Setup - 5 Minutes ⚡

## Step 1: Set Email Credentials (2 minutes)

### Option A: Gmail (Quick for Testing)

**Windows PowerShell:**
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-char-app-password"
```

**Get Gmail App Password:**
1. Visit: https://myaccount.google.com/apppasswords
2. Generate password for "Mail"
3. Copy 16-character password (e.g., `abcd efgh ijkl mnop`)
4. Paste in command above (no spaces)

### Option B: Skip Email Setup (Demo Mode)

If you don't want email right now:
- Email sending will fail gracefully
- Orders will still work perfectly
- Check `email_events` table to see pending emails
- Set up later when ready

## Step 2: Restart Backend (1 minute)

```powershell
# Stop current server (Ctrl+C)
# Then restart:
mvn spring-boot:run
```

## Step 3: Test Order (2 minutes)

1. Open http://localhost:3000
2. Add products to cart
3. Checkout (use demo payment if enabled)
4. Check your email inbox! 📧

## What to Expect

✅ **Email Sent Successfully:**
```log
INFO - Sent order confirmation email for order: ORD-XXX to user@email.com
```

⚠️ **Email Failed (Will Retry):**
```log
WARN - Failed to send order confirmation, will retry
```

❌ **No Credentials Set:**
```log
ERROR - Mail username or password not configured
```

## Quick Verification

**Check email event in database:**
```sql
SELECT * FROM email_events ORDER BY created_at DESC LIMIT 1;
```

**Check application logs:**
```bash
# Windows
Get-Content logs\spring.log -Tail 20

# Or check terminal output
```

## Email Preview

You'll receive a professional email with:
- ✅ Parfumé branding (gold + slate colors)
- ✅ Order confirmation badge
- ✅ Complete order details
- ✅ Item list with quantities
- ✅ GST (18%) breakdown
- ✅ Shipping address
- ✅ Contact information
- ✅ Professional footer

## Troubleshooting

### "Authentication Failed"
- Use app-specific password, not regular Gmail password
- Enable 2-factor authentication first
- Restart server after setting variables

### "No Email Received"
- Check spam/junk folder
- Check `email_events` table for status
- Verify MAIL_USERNAME and MAIL_PASSWORD are set
- Wait 5 minutes for first retry

### "Email Pending"
- Email will auto-retry in 5, 20, 65 minutes
- Check scheduler logs
- Normal if SMTP temporarily unavailable

## Production Ready

For production deployment:

**SendGrid (Recommended):**
```bash
export MAIL_HOST="smtp.sendgrid.net"
export MAIL_PORT="587"
export MAIL_USERNAME="apikey"
export MAIL_PASSWORD="SG.your-api-key"
```

## Full Documentation

See [ORDER_CONFIRMATION_EMAIL_SETUP.md](ORDER_CONFIRMATION_EMAIL_SETUP.md) for:
- Complete configuration guide
- Email template customization
- Monitoring and analytics
- Production deployment
- Advanced troubleshooting

---

**That's it!** 🎉 Your email system is ready to go!
