# Razorpay Integration - Architecture & Sequence Diagrams

## 1. High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (React)                            │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Checkout.jsx                                                │  │
│  │  - Shipping form                                             │  │
│  │  - Razorpay modal integration                               │  │
│  │  - Payment handler                                          │  │
│  └──────────────────────────────────────────────────────────────┘  │
└──────────────┬──────────────────────────────────────────────────────┘
               │
    ┌──────────┴──────────┬──────────────────┐
    │                     │                  │
    ↓                     ↓                  ↓
POST /api/orders/    POST /orders/verify-  ┌─────────────┐
checkout              payment              │ Razorpay    │
                                          │ Modal (JS)  │
┌─────────────────────────────────────────────────────────────────────┐
│                      BACKEND (Spring Boot)                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  OrderController                    PaymentController               │
│  ├─ createOrder()          ←────→   └─ handleRazorpayWebhook()    │
│  └─ verifyPayment()                    └─ handlePaymentFailed()    │
│                                                                      │
│  OrderService                        RazorpayService               │
│  ├─ createOrder()          ←────→   ├─ createRazorpayOrder()      │
│  └─ confirmPayment()                 ├─ verifyPaymentSignature()  │
│                                      └─ verifyWebhookSignature()   │
│                                                                      │
│  WebhookEventRepository (Idempotency Tracking)                     │
│  └─ findByEventId()  ←─────────────────────────────────────┐      │
│                                                              │      │
│  ProductRepository          OrderRepository                │      │
│  └─ Lock & deduct stock  ←─ findByRazorpayOrderId() ←──────┘      │
│                                                                      │
└──────────────┬──────────────────┬──────────────────────────────────┘
               │                  │
               ↓                  ↓
       ┌──────────────┐   ┌──────────────┐
       │  MySQL       │   │  Razorpay    │
       │  Database    │   │  API         │
       │              │   │              │
       │  orders      │   │  - Create    │
       │  products    │   │    order     │
       │  webhook     │   │  - Verify    │
       │  events      │   │    payment   │
       │  users       │   │  - Webhooks  │
       └──────────────┘   └──────────────┘
```

---

## 2. Payment Flow - Sequence Diagram

```
┌──────────┐         ┌────────────────┐        ┌──────────────────┐       ┌──────────┐
│ Frontend │         │ OrderController│        │ RazorpayService  │       │ Razorpay │
│          │         │                │        │                  │       │  Gateway │
└────┬─────┘         └────────┬───────┘        └──────────┬────────┘       └────┬─────┘
     │                        │                           │                      │
     │ POST /checkout         │                           │                      │
     ├───────────────────────→│                           │                      │
     │                        │                           │                      │
     │                        │ 1. Validate cart          │                      │
     │                        │    & stock                │                      │
     │                        │ 2. Lock prices            │                      │
     │                        │ 3. Create Order           │                      │
     │                        │    (PENDING)              │                      │
     │                        │                           │                      │
     │                        │ 4. Create Razorpay order  │                      │
     │                        ├──────────────────────────→│                      │
     │                        │                           │ createRazorpayOrder()│
     │                        │                           ├─────────────────────→│
     │                        │                           │                      │
     │                        │                           │ order_id + key_id   │
     │                        │←──────────────────────────┤←─────────────────────┤
     │                        │                           │                      │
     │ ← razorpayOrderResponse─                           │                      │
     │←──────────────────────┤                            │                      │
     │                        │                           │                      │
     │ Opens Razorpay Modal   │                           │                      │
     ├────────────────────────────────────────────────────────────────────────→  │
     │                        │                           │                      │
     │ User enters payment details                        │                      │
     │                        │                           │                      │
     │ Success → payment_id + signature                   │                      │
     │←──────────────────────────────────────────────────────────────────────────┤
     │                        │                           │                      │
     │ POST /verify-payment   │                           │                      │
     │ (order_id, payment_id, signature)                 │                      │
     ├───────────────────────→│                           │                      │
     │                        │ 5. Verify signature       │                      │
     │                        ├──────────────────────────→│                      │
     │                        │                           │                      │
     │                        │ HMAC-SHA256 verified ✓   │                      │
     │                        │←──────────────────────────┤                      │
     │                        │                           │                      │
     │                        │ 6. Confirm payment        │                      │
     │                        │    - Lock products        │                      │
     │                        │    - Validate stock       │                      │
     │                        │    - Deduct stock         │                      │
     │                        │    - Update order status  │                      │
     │                        │    - Clear cart           │                      │
     │                        │    - Send email           │                      │
     │                        │                           │                      │
     │ ← Order confirmed      │                           │                      │
     │←──────────────────────┤                            │                      │
     │                        │                           │                      │
     │ Redirect to order page │                           │                      │
     │                        │                           │                      │
```

---

## 3. Webhook Processing - Sequence Diagram

```
┌──────────────┐         ┌──────────────────┐         ┌──────────────────┐
│  Razorpay    │         │ PaymentController│         │  OrderService    │
│  Webhook     │         │                  │         │                  │
└────┬─────────┘         └────────┬─────────┘         └────────┬─────────┘
     │                            │                           │
     │ payment.authorized         │                           │
     │ (eventId, payload, sig)    │                           │
     ├───────────────────────────→│                           │
     │                            │                           │
     │                            │ 1. Verify signature       │
     │                            │    (constant-time)        │
     │                            │                           │
     │                            │ 2. Check idempotency      │
     │                            │    WebhookEvent table     │
     │                            │                           │
     │                            │ 3. If not processed:      │
     │                            │    - Create record        │
     │                            │    - Call confirmPayment()│
     │                            ├──────────────────────────→│
     │                            │                           │
     │                            │                           │ 4. Lock products
     │                            │                           │ 5. Validate stock
     │                            │                           │ 6. Deduct stock
     │                            │                           │ 7. Update status
     │                            │                           │ 8. Clear cart
     │                            │                           │
     │                            │ ← Order confirmed         │
     │                            │←──────────────────────────┤
     │                            │                           │
     │                            │ 9. Update WebhookEvent    │
     │                            │    (processed=true)       │
     │                            │                           │
     │ ← HTTP 200 OK              │                           │
     │←───────────────────────────┤                           │
     │                            │                           │
     │ (Razorpay confirms receipt) │                           │
     │                            │                           │
```

---

## 4. Stock Deduction Flow - Detailed

```
           Checkout Order Created
                  │
                  ↓
     ┌────────────────────────┐
     │ User Completes Payment │
     │ (Via Razorpay Modal)   │
     └────────────┬───────────┘
                  │
                  ↓
     ┌─────────────────────────────────┐
     │ POST /verify-payment called      │
     │ - Signature verified ✓          │
     └────────────┬────────────────────┘
                  │
                  ↓
     ┌──────────────────────────────────────────────────┐
     │ OrderService.confirmPayment()                    │
     │ @Transactional (Atomic Operation)               │
     └────────────┬─────────────────────────────────────┘
                  │
                  ├─→ 1. Find order by razorpayOrderId
                  │
                  ├─→ 2. Check idempotency
                  │      if status ≠ PENDING → return
                  │
                  ├─→ 3. Lock products (pessimistic write lock)
                  │      UPDATE products ... FOR UPDATE
                  │
                  ├─→ 4. Validate stock (again)
                  │      if insufficient → throw exception
                  │                         (rollback)
                  │
                  ├─→ 5. Deduct stock atomically
                  │      for each OrderItem:
                  │      ├─ product.stock -= item.quantity
                  │      └─ save to database
                  │
                  ├─→ 6. Update order status
                  │      status = PAYMENT_CONFIRMED
                  │
                  ├─→ 7. Save payment ID
                  │      razorpayPaymentId = payment_id
                  │
                  ├─→ 8. Clear cart
                  │      cart.items.clear()
                  │
                  └─→ 9. Send confirmation email
                         (async, non-blocking)
                  │
                  ↓
     ┌──────────────────────────────────────────────────┐
     │ Transaction Commits                              │
     │ (All operations persisted atomically)            │
     └──────────────────────────────────────────────────┘
                  │
                  ↓
            ✓ SUCCESS
            Stock deducted
            Order confirmed
            Cart cleared
```

---

## 5. Idempotency Mechanism

```
FIRST WEBHOOK ARRIVAL
├─ EventID = "evt_12345"
├─ Query WebhookEvent table
│  └─ NOT FOUND
├─ Create new WebhookEvent record
│  ├─ eventId = "evt_12345"
│  ├─ eventType = "payment.authorized"
│  ├─ payload = {...}
│  └─ processed = false
├─ Process webhook
│  └─ Call OrderService.confirmPayment()
├─ Update WebhookEvent
│  ├─ processed = true
│  └─ processingResult = "Success"
└─ Return HTTP 200 OK


SECOND WEBHOOK ARRIVAL (Retry)
├─ EventID = "evt_12345"
├─ Query WebhookEvent table
│  └─ FOUND (already exists)
├─ Check if already processed
│  └─ processed = true ✓
├─ Return HTTP 200 OK immediately
│  (No duplicate processing)
└─ Stock NOT deducted again ✓
```

---

## 6. Security Layers - Defense in Depth

```
┌─────────────────────────────────────────────────────────┐
│            LAYER 1: INPUT VALIDATION                    │
│  ├─ @Valid on all DTOs                                 │
│  ├─ Type checking                                       │
│  └─ Null checks                                         │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│      LAYER 2: SIGNATURE VERIFICATION (HMAC SHA256)      │
│  ├─ Verify payment.authorized_id | payment_id          │
│  ├─ Verify webhook payload                             │
│  └─ Prevent tampering                                  │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│  LAYER 3: TIMING ATTACK PREVENTION                      │
│  ├─ Constant-time comparison                           │
│  ├─ No early exit on mismatch                          │
│  └─ Prevent timing inference                           │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│      LAYER 4: IDEMPOTENCY CHECK                         │
│  ├─ WebhookEvent unique constraint on eventId          │
│  ├─ Check order status                                 │
│  └─ Prevent duplicate processing                       │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│   LAYER 5: PESSIMISTIC LOCKING & STOCK VALIDATION      │
│  ├─ Database-level locks                               │
│  ├─ Real-time stock check                              │
│  └─ Prevent overselling                                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│      LAYER 6: TRANSACTIONAL GUARANTEES                  │
│  ├─ @Transactional on all payment ops                  │
│  ├─ Atomic operations (all-or-nothing)                 │
│  └─ Automatic rollback on failure                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│       LAYER 7: AUTHORIZATION CHECKS                     │
│  ├─ User must own order                                │
│  ├─ JWT token validation                               │
│  └─ Prevent unauthorized access                        │
└─────────────────────────────────────────────────────────┘
                         ↓
            ✓ TRANSACTION SAFE
```

---

## 7. Database Schema - Relationships

```
┌──────────────────────┐
│      users           │
├──────────────────────┤
│ id (PK)              │
│ email                │
│ ...                  │
└──────┬───────────────┘
       │ 1:N
       │
       ↓
┌──────────────────────────────┐
│      orders                  │
├──────────────────────────────┤
│ id (PK)                      │
│ user_id (FK)                 │
│ orderNumber (unique)         │
│ status                       │
│ totalAmount                  │
│ paymentMethod                │
│ paymentIntentId (Stripe)     │
│ razorpayOrderId (NEW) ← ─ ┐  │
│ razorpayPaymentId (NEW) ← ┼──┤──────────────┐
│ ...                       │  │              │
└──────┬────────────────────┘  │              │
       │ 1:N                    │              │
       │                        │              │
       ↓                        │              │
┌──────────────────────────────┐   │           │
│   orderItems                 │              │
├──────────────────────────────┤  │           │
│ id (PK)                      │              │
│ order_id (FK)                │              │
│ product_id (FK)              │              │
│ quantity                     │              │
│ price (locked)               │              │
│ ...                          │              │
└──────┬───────────────────────┘              │
       │ N:1                                  │
       │                                      │
       ↓                                      │
┌──────────────────────┐                     │
│   products           │                     │
├──────────────────────┤                     │
│ id (PK)              │                     │
│ name                 │                     │
│ stock (deducted)  ←──┤─────────────────────┤
│ price                │                     │
│ discountPrice        │                     │
│ ...                  │                     │
└──────────────────────┘                     │
                                             │
                                    ┌────────┴────────────────┐
                                    │                         │
                                    ↓                         ↓
                        ┌──────────────────────────┐
                        │  webhookEvents (NEW)     │
                        ├──────────────────────────┤
                        │ id (PK)                  │
                        │ eventId (unique)         │
                        │ eventType                │
                        │ payload                  │
                        │ processed                │
                        │ processingResult         │
                        │ createdAt                │
                        │ processedAt              │
                        │ errorMessage             │
                        └──────────────────────────┘
```

---

## 8. Error Handling Flow

```
Payment Processing Error
    │
    ├─→ SignatureVerificationException
    │   └─→ Log & return 400 Bad Request
    │
    ├─→ RazorpayException (during order creation)
    │   └─→ Transaction rollback
    │       Order not created
    │
    ├─→ Stock validation failed
    │   └─→ For checkout:
    │       ├─ Throw exception
    │       └─ Order not persisted
    │
    │   └─→ For payment confirmation:
    │       ├─ Throw exception
    │       ├─ Order stays PENDING
    │       └─ Stock not deducted
    │
    ├─→ Database lock timeout
    │   └─→ Automatic retry by Spring
    │       (pessimistic lock)
    │
    └─→ Email sending failure
        └─→ Log warning
            (don't fail transaction)
            Order still confirmed
```

---

## Summary

This architecture provides:

✅ **Secure**: Multi-layer security (signatures, timing protection, locking)  
✅ **Reliable**: Idempotent webhooks, transactional guarantees  
✅ **Atomic**: Stock deduction guaranteed after payment  
✅ **Scalable**: Handles concurrent orders with database locks  
✅ **Maintainable**: Clear separation of concerns, comprehensive logging  

All diagrams align with the actual implementation in code.
