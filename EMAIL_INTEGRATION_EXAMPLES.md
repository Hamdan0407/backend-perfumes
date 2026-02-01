# Email Integration Examples

## Integration Points

The email system integrates seamlessly with existing services. Here are the key integration points and examples.

## OrderService Integration

### Sending Confirmation Email on Order Creation

**Current behavior:** When an order is created, confirmation email is automatically sent.

```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private EmailService emailService;
    
    public Order createOrder(CheckoutRequest request, User user) {
        // ... validate and create order ...
        
        // Save order
        Order order = orderRepository.save(order);
        
        // Send confirmation email (async, non-blocking)
        emailService.sendOrderConfirmation(order);
        
        // Return immediately without waiting for email
        return order;
    }
}
```

**What happens:**
1. Order saved to database
2. sendOrderConfirmation() is called (returns immediately)
3. Email sends in background thread
4. If it fails, automatically retried with backoff
5. All attempts tracked in email_events table

### Sending Status Update on Order Status Change

When order status changes (e.g., Processing → Shipped):

```java
@Service
@Transactional
public class OrderService {
    
    @Autowired
    private EmailService emailService;
    
    public void updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        // Update status
        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);
        
        // Send status update email (async)
        emailService.sendOrderStatusUpdate(updated);
    }
}
```

### Sending Shipping Notification

When order ships with tracking number:

```java
@Service
@Transactional
public class ShippingService {
    
    @Autowired
    private EmailService emailService;
    
    public void shipOrder(Long orderId, String trackingNumber) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow();
        
        // Update shipping details
        order.setTrackingNumber(trackingNumber);
        order.setStatus("SHIPPED");
        Order updated = orderRepository.save(order);
        
        // Send shipping notification
        emailService.sendShippingNotification(updated);
    }
}
```

## Admin Operations

### Bulk Email Operations

Send updates to multiple orders:

```java
@Service
public class AdminService {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Send status update to all pending orders
    public void notifyAllPendingOrders(String message) {
        List<Order> pendingOrders = orderRepository
            .findByStatus("PENDING");
        
        for (Order order : pendingOrders) {
            // Send emails async, all in parallel
            emailService.sendOrderStatusUpdate(order);
        }
        
        // All emails sent in background, returns immediately
    }
}
```

### Retry Specific Failed Email

Manually retry a specific email event:

```java
@Service
public class AdminService {
    
    @Autowired
    private EmailEventRepository emailEventRepository;
    
    @Autowired
    private EmailService emailService;
    
    public void retryEmail(Long emailEventId) {
        EmailEvent event = emailEventRepository.findById(emailEventId)
            .orElseThrow(() -> new EmailEventNotFoundException(emailEventId));
        
        // Reset for retry if max retries exceeded
        if (event.getAttemptCount() >= event.getMaxRetries()) {
            event.setAttemptCount(0);
            event.setStatus(EmailEvent.EmailStatus.PENDING);
            emailEventRepository.save(event);
        }
        
        // Retry the email
        emailService.retryFailedEmail(event);
    }
}
```

## Monitoring and Reporting

### Email Statistics Service

```java
@Service
public class EmailStatsService {
    
    @Autowired
    private EmailEventRepository emailEventRepository;
    
    /**
     * Get email delivery statistics
     */
    public EmailStatsDTO getEmailStats() {
        List<EmailEvent> all = emailEventRepository.findAll();
        
        long sent = all.stream()
            .filter(e -> e.getStatus() == EmailEvent.EmailStatus.SENT)
            .count();
        
        long pending = all.stream()
            .filter(e -> e.getStatus() == EmailEvent.EmailStatus.PENDING)
            .count();
        
        long failed = all.stream()
            .filter(e -> e.getStatus() == EmailEvent.EmailStatus.FAILED)
            .count();
        
        return EmailStatsDTO.builder()
            .totalEmails(all.size())
            .sent(sent)
            .pending(pending)
            .failed(failed)
            .successRate((double) sent / all.size() * 100)
            .build();
    }
    
    /**
     * Get emails by order
     */
    public List<EmailEvent> getOrderEmailHistory(Long orderId) {
        List<Order> orders = /* ... */;
        
        return emailEventRepository.findByOrderId(orderId);
    }
    
    /**
     * Get failed emails for analysis
     */
    public List<EmailEvent> getFailedEmails() {
        return emailEventRepository.findFailedEmails();
    }
    
    /**
     * Get pending retry count
     */
    public long getPendingRetryCount() {
        return emailEventRepository.findPendingEmailsForRetry().size();
    }
}
```

## REST API Endpoints

### Email Status Endpoint

```java
@RestController
@RequestMapping("/api/orders/{orderId}/emails")
public class OrderEmailController {
    
    @Autowired
    private EmailEventRepository emailEventRepository;
    
    /**
     * Get all email attempts for an order
     */
    @GetMapping
    public ResponseEntity<List<EmailEventResponse>> getOrderEmails(
            @PathVariable Long orderId) {
        
        List<EmailEvent> events = emailEventRepository
            .findByOrderId(orderId);
        
        List<EmailEventResponse> responses = events.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * Retry a specific failed email
     */
    @PostMapping("/{emailEventId}/retry")
    public ResponseEntity<EmailEventResponse> retryEmail(
            @PathVariable Long orderId,
            @PathVariable Long emailEventId) {
        
        EmailEvent event = emailEventRepository.findById(emailEventId)
            .orElseThrow(() -> new EmailEventNotFoundException(emailEventId));
        
        // Verify it belongs to this order
        if (!event.getOrder().getId().equals(orderId)) {
            return ResponseEntity.badRequest().build();
        }
        
        // Reset attempt count if maxed out
        if (event.getAttemptCount() >= event.getMaxRetries()) {
            event.setAttemptCount(0);
            event.setStatus(EmailEvent.EmailStatus.PENDING);
            event = emailEventRepository.save(event);
        }
        
        // Retry
        emailService.retryFailedEmail(event);
        
        return ResponseEntity.ok(toResponse(event));
    }
    
    private EmailEventResponse toResponse(EmailEvent event) {
        return EmailEventResponse.builder()
            .id(event.getId())
            .emailType(event.getEmailType())
            .recipientEmail(event.getRecipientEmail())
            .status(event.getStatus().toString())
            .attemptCount(event.getAttemptCount())
            .maxRetries(event.getMaxRetries())
            .lastError(event.getLastError())
            .nextRetryAt(event.getNextRetryAt())
            .sentAt(event.getSentAt())
            .createdAt(event.getCreatedAt())
            .build();
    }
}
```

## DTO Definitions

### EmailEventResponse.java

```java
package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEventResponse {
    private Long id;
    private String emailType;
    private String recipientEmail;
    private String status;
    private Integer attemptCount;
    private Integer maxRetries;
    private String lastError;
    private LocalDateTime nextRetryAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
```

### EmailStatsDTO.java

```java
package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStatsDTO {
    private Long totalEmails;
    private Long sent;
    private Long pending;
    private Long failed;
    private Double successRate;
}
```

## Error Handling

### Custom Exception

```java
package com.perfume.shop.exception;

public class EmailEventNotFoundException extends RuntimeException {
    public EmailEventNotFoundException(Long id) {
        super("Email event not found: " + id);
    }
}
```

### Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(EmailEventNotFoundException.class)
    public ResponseEntity<ApiResponse> handleEmailEventNotFound(
            EmailEventNotFoundException ex) {
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .build());
    }
}
```

## Testing Examples

### Unit Test

```java
@SpringBootTest
@Transactional
public class EmailServiceTest {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private EmailEventRepository emailEventRepository;
    
    @MockBean
    private JavaMailSender mailSender;
    
    @Test
    public void testOrderConfirmationEmailCreatesEvent() throws MessagingException {
        // Given
        Order order = createTestOrder();
        
        // When
        emailService.sendOrderConfirmation(order);
        
        // Wait for async execution
        Thread.sleep(500);
        
        // Then
        List<EmailEvent> events = emailEventRepository
            .findByOrderIdAndEmailType(order.getId(), "CONFIRMATION");
        
        assertEquals(1, events.size());
        assertEquals(EmailEvent.EmailStatus.PENDING, events.get(0).getStatus());
    }
    
    @Test
    public void testFailedEmailSchedulesRetry() throws MessagingException {
        // Given
        Order order = createTestOrder();
        doThrow(new MessagingException("SMTP Error"))
            .when(mailSender).send(any(MimeMessage.class));
        
        // When
        emailService.sendOrderConfirmation(order);
        Thread.sleep(500);
        
        // Then
        List<EmailEvent> events = emailEventRepository
            .findByOrderIdAndEmailType(order.getId(), "CONFIRMATION");
        
        EmailEvent event = events.get(0);
        assertEquals(EmailEvent.EmailStatus.PENDING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertNotNull(event.getNextRetryAt());
    }
}
```

### Integration Test

```java
@SpringBootTest
@Transactional
public class EmailRetrySchedulerTest {
    
    @Autowired
    private EmailRetryScheduler emailRetryScheduler;
    
    @Autowired
    private EmailEventRepository emailEventRepository;
    
    @Test
    public void testRetrySchedulerProcessesPendingEmails() {
        // Given
        EmailEvent event = EmailEvent.builder()
            .emailType("CONFIRMATION")
            .status(EmailEvent.EmailStatus.PENDING)
            .attemptCount(1)
            .maxRetries(3)
            .nextRetryAt(LocalDateTime.now().minusMinutes(1))
            .build();
        
        emailEventRepository.save(event);
        
        // When
        emailRetryScheduler.retryFailedEmails();
        
        // Then - event should have been processed
        EmailEvent updated = emailEventRepository.findById(event.getId()).orElseThrow();
        // Status depends on mail send result (mocked in test)
    }
}
```

## Usage Checklist

Before using the email system in production:

- [ ] Set MAIL_USERNAME and MAIL_PASSWORD environment variables
- [ ] Configure appropriate max-retries for your use case
- [ ] Test email sending with real user addresses
- [ ] Monitor email_events table for issues
- [ ] Set up database backups for email_events
- [ ] Configure email alerts for FAILED status accumulation
- [ ] Review and customize email templates
- [ ] Test retry scheduler by simulating SMTP failures
- [ ] Document email sending SLA for business stakeholders
- [ ] Set up monitoring dashboard for email metrics

## Common Patterns

### Pattern: Don't wait for email before responding

❌ **WRONG:**
```java
emailService.sendOrderConfirmation(order); // synchronous wait
return orderResponse; // delayed response
```

✅ **CORRECT:**
```java
emailService.sendOrderConfirmation(order); // async, returns immediately
return orderResponse; // fast response to client
```

### Pattern: Email is best-effort

Understand that emails may be delayed (retry queue) or fail. Don't depend on email for critical operations:

❌ **WRONG:**
```java
emailService.sendOrderConfirmation(order); // what if fails?
order.setEmailSent(true);
return order;
```

✅ **CORRECT:**
```java
emailService.sendOrderConfirmation(order); // fire and forget
// Email status is tracked separately in email_events
return order;
```

### Pattern: Provide visibility to user

Let users see email status in their order page:

```java
// In OrderDetailController
public OrderDetailResponse getOrderDetail(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    
    // Get email status
    List<EmailEvent> emails = emailEventRepository
        .findByOrderId(orderId);
    
    return OrderDetailResponse.builder()
        .order(order)
        .emailEvents(emails)  // Include email status
        .build();
}
```
