# Razorpay Payment Gateway Integration Guide

## Overview

This document provides a comprehensive guide for the Razorpay payment gateway integration in the Perfume Shop application. The integration includes order creation, payment verification, webhook handling, and production-ready security features.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Setup & Configuration](#setup--configuration)
3. [Payment Flow](#payment-flow)
4. [Backend Implementation](#backend-implementation)
5. [Frontend Implementation](#frontend-implementation)
6. [Webhook Handling](#webhook-handling)
7. [Security Features](#security-features)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)
10. [Production Deployment](#production-deployment)

---

## Architecture

### System Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        FRONTEND (React)                          │
├─────────────────────────────────────────────────────────────────┤
│ 1. User enters shipping info                                     │
│ 2. Calls POST /api/orders/checkout                              │
│ 3. Receives Razorpay Order ID + Public Key                      │
│ 4. Opens Razorpay Modal                                          │
│ 5. User completes payment                                        │
│ 6. Calls POST /api/orders/verify-payment (signature + IDs)      │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                        BACKEND (Spring Boot)                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│ POST /api/orders/checkout                                        │
│  ├─ Validate cart and stock (pessimistic lock)                  │
│  ├─ Lock prices                                                  │
│  ├─ Create Order entity (PENDING status)                        │
│  ├─ Create Razorpay Order (via RazorpayService)                 │
│  └─ Return Razorpay Order ID + Key to frontend                  │
│                                                                   │
│ POST /api/orders/verify-payment                                  │
│  ├─ Verify payment signature (HMAC SHA256)                      │
│  ├─ Call OrderService.confirmPayment()                          │
│  ├─ Lock products again + validate stock                        │
│  ├─ Atomically deduct stock                                      │
│  ├─ Update order status to PAYMENT_CONFIRMED                    │
│  ├─ Clear user's cart                                            │
│  └─ Send confirmation email                                      │
│                                                                   │
│ POST /api/payment/razorpay/webhook                              │
│  ├─ Verify webhook signature                                     │
│  ├─ Check idempotency (WebhookEvent table)                      │
│  ├─ Handle payment.authorized event                             │
│  └─ Redundant confirmation (if frontend verification fails)     │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓ ↑
┌─────────────────────────────────────────────────────────────────┐
│                    Razorpay Payment Gateway                       │
├─────────────────────────────────────────────────────────────────┤
│ ├─ Creates payment order with unique ID                         │
│ ├─ Hosts payment modal                                           │
│ ├─ Processes payment securely                                    │
│ ├─ Returns payment ID + signature                               │
│ └─ Sends webhook on payment success/failure                     │
└─────────────────────────────────────────────────────────────────┘
```

---

## Setup & Configuration

### 1. Prerequisites

- Razorpay Account (https://dashboard.razorpay.com)
- Java 17+
- Spring Boot 3.2+
- MySQL 8.0+

### 2. Razorpay Account Setup

1. Create account on [Razorpay Dashboard](https://dashboard.razorpay.com)
2. Navigate to **Settings → API Keys**
3. Note your **Key ID** (public key) and **Key Secret** (private key)
4. Use **Test Mode** initially, switch to **Live Mode** for production

### 3. Environment Variables

Add these variables to your `.env` file or system environment:

```bash
# Razorpay Keys (Test Mode)
export RAZORPAY_KEY_ID=rzp_test_your_key_id
export RAZORPAY_KEY_SECRET=your_key_secret_here
export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret_here
export RAZORPAY_CURRENCY=INR

# Razorpay Keys (Live Mode) - Use these in production only
export RAZORPAY_KEY_ID=rzp_live_your_key_id
export RAZORPAY_KEY_SECRET=your_live_key_secret_here
export RAZORPAY_WEBHOOK_SECRET=your_live_webhook_secret_here
```

### 4. Application Configuration

**application.yml**:
```yaml
app:
  razorpay:
    key-id: ${RAZORPAY_KEY_ID:rzp_test_your_razorpay_key_id}
    key-secret: ${RAZORPAY_KEY_SECRET:your_razorpay_key_secret}
    webhook-secret: ${RAZORPAY_WEBHOOK_SECRET:your_razorpay_webhook_secret}
    currency: ${RAZORPAY_CURRENCY:INR}
```

### 5. Webhook Configuration

1. Go to **Settings → Webhooks** in Razorpay Dashboard
2. Add webhook URL: `https://your-domain.com/api/payment/razorpay/webhook`
3. Select events:
   - `payment.authorized` - Payment successful
   - `payment.failed` - Payment failed
4. Copy the **Webhook Signing Secret** and set as `RAZORPAY_WEBHOOK_SECRET`

---

## Payment Flow

### Step 1: Order Creation (Frontend → Backend)

```javascript
// Frontend: Checkout.jsx
const response = await api.post('/orders/checkout', {
  shippingAddress: '123 Main St',
  shippingCity: 'Mumbai',
  shippingCountry: 'India',
  shippingZipCode: '400001',
  shippingPhone: '+91-9876543210'
});

// Response contains:
{
  "razorpayOrderId": "order_1A2B3C4D5E6F7G",
  "razorpayKeyId": "rzp_test_1234567890",
  "amount": 49999,  // In paise (₹499.99)
  "currency": "INR",
  "orderId": 123,   // Our internal order ID
  "orderNumber": "ORD-1234567-ABC123",
  "customerEmail": "customer@example.com",
  "customerName": "John Doe",
  "customerPhone": "+91-9876543210"
}
```

### Step 2: Payment Processing

```javascript
// Frontend: Opens Razorpay Checkout Modal
const razorpay = new Razorpay({
  key: response.razorpayKeyId,
  amount: response.amount,
  currency: response.currency,
  order_id: response.razorpayOrderId,
  handler: async (paymentResponse) => {
    // User successfully paid
    // Verify on backend
  }
});
razorpay.open();
```

### Step 3: Payment Verification (Frontend → Backend)

```javascript
// Frontend: After successful payment
const verification = await api.post('/api/orders/verify-payment', {
  razorpayOrderId: 'order_1A2B3C4D5E6F7G',
  razorpayPaymentId: 'pay_1X2Y3Z4A5B6C7D',
  razorpaySignature: 'hmac_sha256_signature_here'
});
```

### Step 4: Backend Payment Confirmation

```
Backend flow:
1. Verify signature (RazorpayService.verifyPaymentSignature())
2. Lock products pessimistically
3. Validate stock availability
4. Atomically deduct stock
5. Update order status to PAYMENT_CONFIRMED
6. Clear user's cart
7. Send confirmation email
8. Return confirmed order
```

---

## Backend Implementation

### RazorpayService

**Location**: `src/main/java/com/perfume/shop/service/RazorpayService.java`

Key methods:

#### 1. Create Razorpay Order

```java
public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request)
```

- Converts amount to paise (multiply by 100)
- Creates order in Razorpay system
- Returns order ID and public key for frontend

#### 2. Verify Payment Signature

```java
public boolean verifyPaymentSignature(RazorpayPaymentVerificationRequest request)
```

- Calculates HMAC-SHA256 signature
- Uses constant-time comparison to prevent timing attacks
- Returns true only if signature matches

#### 3. Verify Webhook Signature

```java
public boolean verifyWebhookSignature(String payload, String signatureHeader)
```

- Validates webhook authenticity
- Prevents replay attacks
- Uses constant-time comparison

### OrderService Updates

**Key changes**:

1. **createOrder()** now:
   - Returns `RazorpayOrderResponse` instead of `PaymentIntentResponse`
   - Creates Razorpay order instead of Stripe Payment Intent
   - Saves `razorpayOrderId` to Order entity

2. **confirmPayment()** now accepts:
   - `razorpayOrderId`: For finding the order
   - `razorpayPaymentId`: Stored in order for reference

### PaymentController

**Razorpay Webhook Endpoint**:

```java
POST /api/payment/razorpay/webhook
```

- Verifies webhook signature
- Checks idempotency using `WebhookEvent` table
- Handles `payment.authorized` and `payment.failed` events
- Logs all webhook events for auditing

### Order Entity Updates

New fields added:
- `razorpayOrderId`: Razorpay Order ID
- `razorpayPaymentId`: Razorpay Payment ID

---

## Frontend Implementation

### Checkout Component Structure

**File**: `frontend/src/pages/Checkout.jsx`

#### Key Features:

1. **Shipping Information Form**
   - Address, City, Country, Zip Code, Phone
   - Validation before submission

2. **Razorpay Payment Form**
   - Dynamically loads Razorpay script
   - Opens modal with payment options
   - Handles payment callbacks

3. **Payment Verification**
   - Verifies signature on backend
   - Handles success/failure cases
   - Navigates to order details on success

### Integration Steps

1. **Install Razorpay SDK** (loaded dynamically):
   ```html
   <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
   ```

2. **Open Razorpay Modal**:
   ```javascript
   const razorpay = new Razorpay({
     key: keyId,
     order_id: orderId,
     amount: amount,
     handler: (response) => { /* ... */ }
   });
   razorpay.open();
   ```

3. **Verify Payment**:
   ```javascript
   await api.post('/api/orders/verify-payment', {
     razorpayOrderId: order_id,
     razorpayPaymentId: payment_id,
     razorpaySignature: signature
   });
   ```

---

## Webhook Handling

### Idempotency Mechanism

The system uses `WebhookEvent` table to track processed events:

```
┌────────────────────┐
│  WebhookEvent      │
├────────────────────┤
│ id (PK)            │
│ eventId (unique)   │  ← Razorpay event ID
│ eventType          │  ← payment.authorized, etc.
│ payload            │  ← Full webhook JSON
│ processed          │  ← true/false
│ processingResult   │  ← Success/error message
│ createdAt          │
│ processedAt        │
│ errorMessage       │
└────────────────────┘
```

### Flow:

1. Webhook arrives with eventId from Razorpay
2. Check if eventId already exists in WebhookEvent table
3. If exists → Already processed, return success (idempotent)
4. If not exists → Process event:
   - Create WebhookEvent record
   - Handle based on event type
   - Update record with result
5. Return success to Razorpay (always return 200 OK)

### Retry Policy

Razorpay retries webhook with exponential backoff:
- 1st attempt: Immediate
- 2nd attempt: 5 minutes
- 3rd attempt: 30 minutes
- 4th attempt: 2 hours
- Continues for 3 days

---

## Security Features

### 1. Signature Verification (HMAC SHA256)

**Purpose**: Ensures request came from Razorpay

**Implementation**:
```java
private String generateHmacSha256(String data, String secret)
    throws NoSuchAlgorithmException, InvalidKeyException {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
    mac.init(secretKey);
    byte[] signature = mac.doFinal(data.getBytes());
    return HexFormat.of().formatHex(signature);
}
```

**For Payment Verification**:
- Data: `razorpay_order_id|razorpay_payment_id`
- Secret: `app.razorpay.webhook-secret`

**For Webhook Verification**:
- Data: Full webhook JSON payload
- Secret: `app.razorpay.webhook-secret`

### 2. Constant-Time Comparison

**Purpose**: Prevents timing attacks

**Implementation**:
```java
public static boolean constantTimeEquals(String a, String b) {
    if (a == null || b == null) return a == b;
    
    byte[] aBytes = a.getBytes();
    byte[] bBytes = b.getBytes();
    
    int result = aBytes.length ^ bBytes.length;
    for (int i = 0; i < aBytes.length && i < bBytes.length; i++) {
        result |= aBytes[i] ^ bBytes[i];
    }
    return result == 0;
}
```

Prevents attackers from inferring signatures by measuring response time.

### 3. Idempotency

- Every webhook event has unique eventId
- Track processed events to prevent duplicate stock deduction
- Always return 200 OK to Razorpay (even if already processed)

### 4. Pessimistic Locking

- Lock products before stock validation
- Prevents race conditions in concurrent scenarios
- Ensures atomic stock deduction

### 5. Transactional Guarantees

- Entire payment confirmation wrapped in @Transactional
- Automatic rollback on any exception
- ACID properties maintained

### 6. Environment Variables

- Store all secrets as environment variables
- Never hardcode keys in source code
- Separate test and production keys

### 7. Input Validation

- All DTOs use @Valid annotation
- Backend validates before processing
- Prevent injection attacks

---

## Testing

### 1. Test Mode Setup

Use Razorpay test credentials:

```bash
export RAZORPAY_KEY_ID=rzp_test_xxxxx
export RAZORPAY_KEY_SECRET=xxxxxx
export RAZORPAY_WEBHOOK_SECRET=whsec_test_xxxxx
```

### 2. Test Payment Cards

Razorpay provides test cards:

| Card Number | Expiry | CVV | Status |
|-------------|--------|-----|--------|
| 4111 1111 1111 1111 | 12/25 | 123 | Success |
| 4111 1111 1111 1112 | 12/25 | 123 | Failure |
| 4242 4242 4242 4242 | 12/25 | 123 | Success |

### 3. Testing Webhooks Locally

Use Razorpay webhook testing tools or curl:

```bash
curl -X POST http://localhost:8080/api/payment/razorpay/webhook \
  -H "Content-Type: application/json" \
  -H "X-Razorpay-Signature: test_signature" \
  -d '{"event":"payment.authorized","payload":{"payment":{"entity":{"id":"pay_xxxx","order_id":"order_xxxx"}}}}'
```

### 4. Test Scenarios

#### Scenario 1: Successful Payment
1. Create order → Get Razorpay Order ID
2. Use test card (success)
3. Verify signature
4. Confirm payment
5. Verify stock deducted

#### Scenario 2: Failed Payment
1. Create order
2. Use test card (failure)
3. Order remains PENDING
4. Cart not cleared

#### Scenario 3: Webhook Idempotency
1. Send same webhook twice
2. First time: Process payment
3. Second time: Return success without processing

#### Scenario 4: Race Condition
1. Concurrent stock validation
2. Verify pessimistic lock works
3. Only one succeeds, other fails

---

## Troubleshooting

### Issue: "Razorpay order creation failed"

**Causes**:
- Invalid credentials (KEY_ID, KEY_SECRET)
- Network connectivity issue
- Invalid amount format

**Solution**:
1. Verify environment variables
2. Check Razorpay dashboard for active API keys
3. Ensure amount is in paise (multiply by 100)

### Issue: "Payment signature verification failed"

**Causes**:
- Wrong webhook secret
- Signature tampered
- Clock skew (if webhook-to-backend delayed)

**Solution**:
1. Verify RAZORPAY_WEBHOOK_SECRET is correct
2. Enable detailed logging to see expected vs actual signature
3. Check server time synchronization

### Issue: "Webhook not processed"

**Causes**:
- Webhook endpoint not accessible
- Signature verification failing
- Database connection issue

**Solution**:
1. Check firewall/proxy rules
2. Verify webhook logs in Razorpay dashboard
3. Check application logs for errors
4. Test webhook endpoint with curl

### Issue: "Stock deducted multiple times"

**Causes**:
- Webhook idempotency not working
- Frontend verification and webhook both processed

**Solution**:
1. Check WebhookEvent table for duplicate eventIds
2. Verify confirmPayment() idempotency check
3. Review logs for multiple confirmation calls

### Issue: "Order remains PENDING after payment"

**Causes**:
- Stock validation failed
- Payment verification not called
- Async issue (webhook not processed yet)

**Solution**:
1. Check Order.razorpayPaymentId field (should be filled)
2. Wait a few seconds for webhook processing
3. Check application logs for validation failures
4. Manually confirm payment if webhook fails

---

## Production Deployment

### Pre-Deployment Checklist

- [ ] Switch to production Razorpay keys
- [ ] Update RAZORPAY_KEY_ID to live key
- [ ] Update RAZORPAY_KEY_SECRET to live key
- [ ] Update RAZORPAY_WEBHOOK_SECRET to live webhook secret
- [ ] Update RAZORPAY_CURRENCY to production currency
- [ ] Test full payment flow with small amount
- [ ] Verify webhook endpoint is publicly accessible
- [ ] Enable HTTPS for all endpoints
- [ ] Set up monitoring/alerting on webhooks
- [ ] Configure backup webhook URL (if available)
- [ ] Review and test error scenarios
- [ ] Set up database backup strategy

### Environment Variables (Production)

```bash
# Use live keys only
export RAZORPAY_KEY_ID=rzp_live_xxxxx
export RAZORPAY_KEY_SECRET=xxxxx_live_secret
export RAZORPAY_WEBHOOK_SECRET=whsec_live_xxxxx
export RAZORPAY_CURRENCY=INR

# Other critical variables
export JWT_SECRET=production_secret_key
export DATABASE_URL=production_mysql_url
export MAIL_USERNAME=noreply@perfumeshop.com
export MAIL_PASSWORD=production_app_password
```

### Monitoring

Monitor these metrics:

1. **Webhook Processing**:
   - Webhook received count
   - Webhook processing time
   - Failed webhook count
   - Idempotency cache hits

2. **Payment Processing**:
   - Successful payment count
   - Failed payment count
   - Average payment confirmation time
   - Stock deduction errors

3. **Database**:
   - WebhookEvent table size
   - Order processing latency
   - Stock update latency
   - Lock wait times

### Logging

Enable detailed logging for debugging:

```yaml
logging:
  level:
    com.perfume.shop.service.RazorpayService: DEBUG
    com.perfume.shop.service.OrderService: DEBUG
    com.perfume.shop.controller.PaymentController: DEBUG
```

### Disaster Recovery

1. **Payment Failed but Customer Charged**:
   - Check Razorpay dashboard for actual payment status
   - If captured: Manual refund via Razorpay
   - Update order status manually

2. **Webhook Never Received**:
   - Manual confirmation via `/api/orders/verify-payment`
   - Or check Razorpay dashboard and update order status

3. **Database Corruption**:
   - Restore from latest backup
   - Re-verify recent orders against Razorpay dashboard

---

## Additional Resources

- [Razorpay API Documentation](https://razorpay.com/docs/api/)
- [Razorpay Webhook Documentation](https://razorpay.com/docs/webhooks/)
- [Razorpay Orders API](https://razorpay.com/docs/api/orders/)
- [HMAC SHA256 Security](https://owasp.org/www-community/attacks/Timing_attack)
- [Idempotency Best Practices](https://stripe.com/blog/idempotency)

---

## Summary

This Razorpay integration provides:

✅ **Secure**: HMAC SHA256 signature verification + constant-time comparison  
✅ **Reliable**: Idempotent webhooks + pessimistic locking + transactional guarantees  
✅ **Atomic**: Stock deduction guaranteed after payment  
✅ **Production-Ready**: Environment variable separation + comprehensive logging  
✅ **Scalable**: Handles concurrent orders with proper locking  
✅ **Maintainable**: Clear separation of concerns + comprehensive documentation  

For questions or issues, refer to the troubleshooting section or check application logs.
