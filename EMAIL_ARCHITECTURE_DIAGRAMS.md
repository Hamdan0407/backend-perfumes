# Email System Architecture & Flow Diagrams

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     PERFUME SHOP APPLICATION                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────┐    ┌──────────────────────────┐  │
│  │   REST API Controllers   │    │   Spring Data JPA        │  │
│  │  ├─ OrderController      │    │  ├─ OrderRepository     │  │
│  │  ├─ CartController       │    │  ├─ UserRepository      │  │
│  │  └─ PaymentController    │    │  └─ ProductRepository   │  │
│  └────────────┬─────────────┘    └──────────────────────────┘  │
│               │                                                  │
│               ▼                                                  │
│  ┌──────────────────────────┐                                   │
│  │   Service Layer          │                                   │
│  │  ├─ OrderService         │◄────────────────────────────┐    │
│  │  ├─ CartService          │                             │    │
│  │  ├─ ProductService       │                      Creates Orders
│  │  └─ RazorpayService      │                             │    │
│  └────────────┬─────────────┘                             │    │
│               │                                           │    │
│               ▼                                           │    │
│  ┌──────────────────────────┐                             │    │
│  │   EMAIL SERVICE LAYER    │◄────────────────────────────┘   │
│  │  ┌────────────────────┐  │                                  │
│  │  │ EmailService       │  │  ← Async email sending           │
│  │  │ ├─ confirmation()  │  │  ← Status updates                │
│  │  │ ├─ statusUpdate()  │  │  ← Shipping notifications       │
│  │  │ └─ shipping()      │  │                                  │
│  │  └──────────┬─────────┘  │                                  │
│  │             │             │                                  │
│  │             ▼             │                                  │
│  │  ┌────────────────────┐  │                                  │
│  │  │ AsyncConfig        │  │  ← Thread pool management        │
│  │  │ ├─ emailExecutor   │  │    (5-20 threads)               │
│  │  │ │   (100 queue)    │  │                                  │
│  │  │ └─ retryExecutor   │  │    (2-5 threads)                │
│  │  │     (50 queue)     │  │                                  │
│  │  └────────────────────┘  │                                  │
│  └────────────┬─────────────┘                                  │
│               │                                                │
│               ▼                                                │
│  ┌──────────────────────────────────┐                         │
│  │ EMAIL RETRY SCHEDULER            │ ← @Scheduled every 5min│
│  │ (EmailRetryScheduler)            │                         │
│  │ ├─ retryFailedEmails()           │                         │
│  │ ├─ cleanupFailedEmails()         │                         │
│  │ └─ getPendingEmailCount()        │                         │
│  └────────────┬─────────────────────┘                         │
│               │                                                │
└───────────────┼────────────────────────────────────────────────┘
                │
                ▼
    ┌────────────────────────────┐
    │   SPRING MAIL SENDER       │
    │   (JavaMailSender)         │
    │   ├─ Gmail SMTP            │
    │   │  smtp.gmail.com:587    │
    │   └─ TLS enabled           │
    └────────────┬───────────────┘
                │
                ▼
    ┌────────────────────────────┐
    │   EXTERNAL SMTP SERVER     │
    │   (Gmail / Custom)         │
    └────────────────────────────┘
                │
                ▼
    ┌────────────────────────────┐
    │   USER MAILBOX             │
    │   ├─ Order confirmations   │
    │   ├─ Status updates        │
    │   └─ Shipping notifs       │
    └────────────────────────────┘

    ┌─────────────────────────────────┐
    │   DATABASE (MySQL)              │
    ├─────────────────────────────────┤
    │ ┌──────────────────────────┐   │
    │ │ email_events             │   │
    │ │ ├─ id (PK)              │   │
    │ │ ├─ order_id (FK)        │   │ ← Tracks all email attempts
    │ │ ├─ email_type           │   │   with retry metadata
    │ │ ├─ status (PENDING...)  │   │
    │ │ ├─ attemptCount         │   │
    │ │ ├─ nextRetryAt          │   │
    │ │ ├─ lastError            │   │
    │ │ └─ indexes (order, type)│   │
    │ └──────────────────────────┘   │
    └─────────────────────────────────┘
```

## Email Sending Flow

```
USER PLACES ORDER
        │
        ▼
    OrderController.checkout()
        │
        ▼
    OrderService.createOrder()
        │
        ├─► Save to database
        │
        ├─► Call emailService.sendOrderConfirmation(order)
        │   [Returns immediately - ASYNC]
        │
        └─► Return OrderResponse to client
                │
                │ (User gets response in <1ms)
                │
        ┌───────┴──────────────────────────────────┐
        │ (Email sends in background thread)      │
        │                                          │
        ▼                                          ▼
    EmailService.sendOrderConfirmation()      Client receives
        │                                     response
        ├─► emailEventRepository.save()
        │   (status=PENDING, attempt=0)
        │
        ├─► JavaMailSender.send()
        │
        ├── SUCCESS ──────────►  emailEventRepository.save()
        │                        (status=SENT, sentAt=now)
        │
        └─ FAILURE ──────────►  handleEmailFailure()
                                ├─ Increment attemptCount
                                ├─ Capture lastError
                                │
                                ├─ IF attemptCount < maxRetries
                                │  ├─ Calculate nextRetryAt
                                │  │  (5 min * 3^(attempt-1))
                                │  ├─ Keep status=PENDING
                                │  └─ Schedule for retry
                                │
                                └─ IF attemptCount >= maxRetries
                                   ├─ Set status=FAILED
                                   └─ Log error
```

## Retry Scheduler Flow

```
APPLICATION START
        │
        ▼
    PerfumeShopApplication
    ├─ @EnableAsync
    ├─ @EnableScheduling ◄─ Enables scheduler
    └─ @EnableJpaAuditing

        │
        ▼
    EmailRetryScheduler
    @Scheduled(fixedDelay=300000, initialDelay=60000)
        │
        ├─ Wait 1 minute (initial delay)
        │
        └─ Then every 5 minutes:
            │
            ├─► Query database:
            │   SELECT * FROM email_events
            │   WHERE status = 'PENDING'
            │   AND attemptCount < maxRetries
            │   AND nextRetryAt <= NOW()
            │
            ├─► For each pending email:
            │   │
            │   ├─► Call emailService.retryFailedEmail(event)
            │   │   [Async execution via emailRetryExecutor]
            │   │
            │   └─► Send email again
            │       ├─ Success ──► status=SENT
            │       └─ Failure ──► schedule next retry
            │
            └─► Wait 5 minutes, repeat

Timeline Example:
═══════════════════════════════════════════════════════════════

00:00 ▶ Order placed
      ▶ Confirmation email sent
      ▶ FAILS: SMTP timeout
      ▶ EmailEvent: PENDING, attempt=1, nextRetry=00:05

00:01 ▶ Scheduler starts (initialDelay=1min)

00:05 ▶ Scheduler runs (fixedDelay=5min)
      ▶ Finds email ready for retry
      ▶ Resend email
      ▶ FAILS: Server temporarily down
      ▶ EmailEvent: PENDING, attempt=2, nextRetry=00:20

00:10 ▶ Scheduler runs again
      ▶ nextRetryAt=00:20, email not ready yet
      ▶ Skips

00:20 ▶ Scheduler runs
      ▶ nextRetryAt=00:20, email READY
      ▶ Resend email
      ▶ SUCCESS!
      ▶ EmailEvent: SENT, sentAt=00:20, attempt=3

User receives email at 00:20 (20 minutes after order, due to retries)
```

## Database Schema

```
orders (existing)
    ├─ id (PK)
    ├─ user_id (FK)
    ├─ order_number
    ├─ status
    ├─ total_amount
    └─ created_at
            │
            │ 1:N relationship
            │
            ▼
email_events (NEW)
    ├─ id (PK)                    ◄─ Auto-increment
    ├─ order_id (FK) ─────────────► orders.id
    ├─ email_type                 ◄─ VARCHAR: CONFIRMATION, STATUS_UPDATE, SHIPPING
    ├─ recipient_email            ◄─ VARCHAR: target email
    ├─ status                     ◄─ ENUM: PENDING, SENT, FAILED, BOUNCED
    ├─ attemptCount               ◄─ INT: number of send attempts
    ├─ maxRetries                 ◄─ INT: max retry attempts (default=3)
    ├─ lastError                  ◄─ VARCHAR(1000): last error message
    ├─ nextRetryAt                ◄─ DATETIME: when to retry next
    ├─ sentAt                     ◄─ DATETIME: when successfully sent
    ├─ createdAt                  ◄─ DATETIME: event creation timestamp
    │
    └─ Indexes:
       ├─ idx_email_order_id      ◄─ (order_id) for querying by order
       ├─ idx_email_type          ◄─ (email_type) for filtering by type
       ├─ idx_email_status        ◄─ (status) for finding PENDING/FAILED
       └─ idx_email_created       ◄─ (created_at) for time-based queries
```

## Thread Pool Architecture

```
┌──────────────────────────────────────────────────────────┐
│              AsyncConfig (Spring Configuration)          │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─ EMAIL EXECUTOR ─────────────────────────────────┐  │
│  │ (Bean name: "emailExecutor")                     │  │
│  │                                                   │  │
│  │ Core Threads:       5 (always active)            │  │
│  │ Max Threads:       20 (peak capacity)            │  │
│  │ Queue Capacity:   100 (buffered tasks)           │  │
│  │ Queue Policy:  CallerRunsPolicy (overflow)       │  │
│  │                                                   │  │
│  │ Used for:                                         │  │
│  │ ├─ @Async("emailExecutor")                       │  │
│  │ │  sendOrderConfirmation()                       │  │
│  │ ├─ sendOrderStatusUpdate()                       │  │
│  │ └─ sendShippingNotification()                    │  │
│  │                                                   │  │
│  │ Thread Name: email-task-1, email-task-2, ...    │  │
│  └───────────────────────────────────────────────────┘  │
│                                                           │
│  ┌─ EMAIL RETRY EXECUTOR ────────────────────────────┐  │
│  │ (Bean name: "emailRetryExecutor")                 │  │
│  │                                                   │  │
│  │ Core Threads:       2 (always active)            │  │
│  │ Max Threads:        5 (peak capacity)            │  │
│  │ Queue Capacity:    50 (buffered tasks)           │  │
│  │ Queue Policy:  CallerRunsPolicy (overflow)       │  │
│  │                                                   │  │
│  │ Used for:                                         │  │
│  │ └─ @Async("emailRetryExecutor")                  │  │
│  │    retryFailedEmail()                            │  │
│  │                                                   │  │
│  │ Thread Name: email-retry-task-1, ...             │  │
│  └───────────────────────────────────────────────────┘  │
│                                                           │
└──────────────────────────────────────────────────────────┘

Task Distribution:
═════════════════

emailExecutor queue (100 capacity)
│
├─► Task: sendOrderConfirmation(order1)
├─► Task: sendOrderConfirmation(order2)
├─► Task: sendOrderStatusUpdate(order3)
├─► Task: sendShippingNotification(order4)
└─► ... up to 100 pending tasks

emailRetryExecutor queue (50 capacity)
│
├─► Task: retryFailedEmail(event1)
├─► Task: retryFailedEmail(event2)
└─► ... up to 50 pending retries

Both use CallerRunsPolicy:
If queue full → task runs in caller thread (request thread)
             → graceful degradation, no rejection
```

## Status Transitions

```
Email Event Lifecycle:

Created
  │
  ├─ EmailEvent {
  │    status: PENDING
  │    attempt: 0
  │    nextRetryAt: null
  │  }
  │
  ▼
Send Attempted
  │
  ├─ SUCCESS ──────► SENT
  │                  └─ EmailEvent {
  │                      status: SENT
  │                      sentAt: now
  │                      attempt: 1
  │                    }
  │                  └─ END (email delivered)
  │
  ├─ FAILURE (attempt 1 of 3) ──────► PENDING
  │                                    └─ EmailEvent {
  │                                        status: PENDING
  │                                        attempt: 1
  │                                        nextRetryAt: now + 5min
  │                                        lastError: "SMTP timeout"
  │                                      }
  │                                    └─ Retry scheduled
  │                                       │
  │                                       └─ Wait 5 minutes
  │                                          │
  │                                          ▼
  │                                       Retry #1
  │                                       │
  │                                       ├─ SUCCESS ──► SENT ✓
  │                                       │
  │                                       ├─ FAILURE (attempt 2 of 3)
  │                                       │  └─ nextRetryAt: now + 15min
  │                                       │     │
  │                                       │     └─ Wait 15 minutes
  │                                       │        │
  │                                       │        ▼
  │                                       │     Retry #2
  │                                       │     │
  │                                       │     ├─ SUCCESS ──► SENT ✓
  │                                       │     │
  │                                       │     └─ FAILURE (attempt 3 of 3)
  │                                       │        └─ nextRetryAt: now + 45min
  │                                       │           │
  │                                       │           └─ Wait 45 minutes
  │                                       │              │
  │                                       │              ▼
  │                                       │           Retry #3 (FINAL)
  │                                       │           │
  │                                       │           ├─ SUCCESS ──► SENT ✓
  │                                       │           │
  │                                       │           └─ FAILURE ──► FAILED
  │                                       │              └─ EmailEvent {
  │                                       │                  status: FAILED
  │                                       │                  attempt: 3
  │                                       │                  lastError: msg
  │                                       │                }
  │                                       │              └─ END (no more retries)
  │                                       │                 └─ Admin must
  │                                       │                    investigate
  │
  └─ Alternative Path:
     Optional: Admin manually resets
     EmailEvent.attempt = 0
     EmailEvent.status = PENDING
     └─ Manual retry via API
```

## Configuration Flow

```
application.yml
│
├─ spring.mail.* ────► JavaMailSender
│  ├─ host: smtp.gmail.com
│  ├─ port: 587
│  ├─ username: ${MAIL_USERNAME}
│  ├─ password: ${MAIL_PASSWORD}
│  └─ properties: TLS enabled
│
└─ app.email.* ──────► EmailService
   └─ max-retries: 3 ◄─ Injected via @Value
                       └─ Used in retryFailedEmail()

Environment Variables
│
├─ MAIL_USERNAME ────► application.yml substitution
├─ MAIL_PASSWORD ────► application.yml substitution
└─ (Optional: JVM properties, docker env, etc.)

AsyncConfig
│
└─ Programmatically configured
   ├─ No application.yml needed
   ├─ Thread pool sizes hardcoded
   └─ Can be overridden in code if needed
```

---

**Last Updated**: January 2024
**Diagram Type**: Architecture and Flow Documentation
