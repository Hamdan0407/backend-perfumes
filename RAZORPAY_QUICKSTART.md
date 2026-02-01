# Quick Start: Razorpay Integration

## 1. Get Razorpay Keys

1. Go to https://dashboard.razorpay.com/app/keys
2. Copy **Key ID** (test mode for development)
3. Copy **Key Secret**
4. Go to **Settings → Webhooks** → Add webhook URL: `https://yourdomain.com/api/payment/razorpay/webhook`
5. Copy **Webhook Secret**

## 2. Set Environment Variables

### Windows (PowerShell)
```powershell
$env:RAZORPAY_KEY_ID="rzp_test_xxxxx"
$env:RAZORPAY_KEY_SECRET="your_secret_key"
$env:RAZORPAY_WEBHOOK_SECRET="whsec_xxxxx"
$env:RAZORPAY_CURRENCY="INR"
```

### Linux/Mac (Bash)
```bash
export RAZORPAY_KEY_ID="rzp_test_xxxxx"
export RAZORPAY_KEY_SECRET="your_secret_key"
export RAZORPAY_WEBHOOK_SECRET="whsec_xxxxx"
export RAZORPAY_CURRENCY="INR"
```

### .env File
```
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=your_secret_key
RAZORPAY_WEBHOOK_SECRET=whsec_xxxxx
RAZORPAY_CURRENCY=INR
```

## 3. Test Payment Card

**Card Number**: 4111 1111 1111 1111  
**Expiry**: 12/25  
**CVV**: 123  
**Status**: Success ✅

## 4. Start Application

```bash
mvn spring-boot:run
```

## 5. Test Full Flow

1. Go to http://localhost:3000/checkout
2. Enter shipping details
3. Click "Continue to Payment"
4. Complete test payment with test card
5. Order should be confirmed with stock deducted

## 6. Verify Order

```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Key Files

- **Backend Service**: [RazorpayService.java](src/main/java/com/perfume/shop/service/RazorpayService.java)
- **Order Service**: [OrderService.java](src/main/java/com/perfume/shop/service/OrderService.java)
- **Payment Controller**: [PaymentController.java](src/main/java/com/perfume/shop/controller/PaymentController.java)
- **Frontend**: [Checkout.jsx](frontend/src/pages/Checkout.jsx)
- **Documentation**: [RAZORPAY_INTEGRATION.md](RAZORPAY_INTEGRATION.md)

## Important URLs

- **Razorpay Dashboard**: https://dashboard.razorpay.com
- **API Keys**: https://dashboard.razorpay.com/app/keys
- **Webhooks**: https://dashboard.razorpay.com/app/webhooks
- **Documentation**: https://razorpay.com/docs/

## Troubleshooting

### 1. "Order creation failed"
- Check RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET
- Verify API keys are active in Razorpay dashboard

### 2. "Signature verification failed"
- Check RAZORPAY_WEBHOOK_SECRET matches Razorpay dashboard
- Verify webhook URL is correct

### 3. "Stock not deducted"
- Check order status is PAYMENT_CONFIRMED
- Verify payment verification was called
- Check database WebhookEvent table

### 4. "Frontend doesn't see Razorpay modal"
- Check browser console for errors
- Verify Razorpay script loaded
- Check if razorpayKeyId is present in response

## Next Steps

1. Read full documentation in [RAZORPAY_INTEGRATION.md](RAZORPAY_INTEGRATION.md)
2. Set up production keys when ready
3. Configure webhooks for live environment
4. Enable monitoring and logging
5. Test error scenarios thoroughly

## Production Checklist

- [ ] Switch to live Razorpay keys
- [ ] Update webhook URL to production domain
- [ ] Enable HTTPS
- [ ] Set up monitoring
- [ ] Test refund flow
- [ ] Document disaster recovery procedures
- [ ] Set up regular backups
- [ ] Configure alerting on webhook failures

---

**Questions?** Check the full documentation in [RAZORPAY_INTEGRATION.md](RAZORPAY_INTEGRATION.md)
