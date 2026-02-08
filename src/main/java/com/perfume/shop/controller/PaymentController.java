package com.perfume.shop.controller;

import com.perfume.shop.dto.ApiResponse;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderHistory;
import com.perfume.shop.entity.WebhookEvent;
import com.perfume.shop.repository.OrderHistoryRepository;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.WebhookEventRepository;
import com.perfume.shop.service.OrderService;
import com.perfume.shop.service.RazorpayService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Payment controller handling both Stripe and Razorpay payment webhooks.
 * Implements idempotency to prevent duplicate webhook processing.
 */
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final WebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final OrderHistoryRepository orderHistoryRepository;
    
    @Value("${app.stripe.webhook-secret}")
    private String stripeWebhookSecret;
    
    @Value("${app.razorpay.webhook-secret}")
    private String razorpayWebhookSecret;
    
    /**
     * Razorpay webhook endpoint for payment authorization events.
     * Handles payment.authorized and payment.failed events.
     * Implements comprehensive idempotency and error handling.
     */
    @PostMapping("/razorpay/webhook")
    public ResponseEntity<ApiResponse> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        log.info("Received Razorpay webhook");

        try {
            // Parse the webhook payload first
            JSONObject event = new JSONObject(payload);
            String eventId = event.getString("id");
            String eventType = event.getString("event");

            log.info("Processing Razorpay event: {} of type: {}", eventId, eventType);

            // Check idempotency - has this event been processed?
            var existingEvent = webhookEventRepository.findByEventId(eventId);
            if (existingEvent.isPresent()) {
                WebhookEvent existing = existingEvent.get();
                if (existing.getProcessed()) {
                    log.info("Webhook event {} already processed successfully. Returning success for idempotency.", eventId);
                    return ResponseEntity.ok(ApiResponse.success("Webhook processed"));
                } else {
                    log.warn("Webhook event {} was previously processed but failed. Reprocessing...", eventId);
                    // Allow reprocessing of failed events
                }
            }

            // Verify webhook signature after idempotency check (signature verification is expensive)
            if (!razorpayService.verifyWebhookSignature(payload, signature)) {
                log.warn("Razorpay webhook signature verification failed for event: {}", eventId);
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Invalid signature"));
            }

            // Create or update webhook event record
            WebhookEvent webhookEvent = existingEvent.orElse(new WebhookEvent());
            webhookEvent.setEventId(eventId);
            webhookEvent.setEventType(eventType);
            webhookEvent.setPayload(payload);
            webhookEvent.setProcessed(false);
            webhookEvent = webhookEventRepository.save(webhookEvent);

            // Process based on event type
            ApiResponse response;
            if ("payment.authorized".equals(eventType)) {
                response = handlePaymentAuthorized(event, webhookEvent);
            } else if ("payment.failed".equals(eventType)) {
                response = handlePaymentFailed(event, webhookEvent);
            } else if ("order.paid".equals(eventType)) {
                response = handleOrderPaid(event, webhookEvent);
            } else {
                log.debug("Unhandled event type: {}", eventType);
                webhookEvent.setProcessed(true);
                webhookEvent.setProcessingResult("Event type not handled: " + eventType);
                webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
                webhookEventRepository.save(webhookEvent);
                response = ApiResponse.success("Event type not handled");
            }

            return ResponseEntity.ok(response);

        } catch (org.json.JSONException e) {
            log.error("Invalid JSON in webhook payload", e);
            return ResponseEntity.status(400)
                    .body(ApiResponse.error("Invalid JSON payload"));
        } catch (Exception e) {
            log.error("Unexpected error processing Razorpay webhook", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Internal server error processing webhook"));
        }
    }
    
    /**
     * Handle payment.authorized event from Razorpay.
     * Confirms the order and deducts stock atomically.
     * Returns ApiResponse for better error handling.
     */
    private ApiResponse handlePaymentAuthorized(JSONObject event, WebhookEvent webhookEvent) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject payment = payload.getJSONObject("payment");
            JSONObject entity = payment.getJSONObject("entity");

            String razorpayPaymentId = entity.getString("id");
            String razorpayOrderId = entity.getString("order_id");
            String status = entity.getString("status");

            log.info("Payment authorized: {} for order: {} with status: {}",
                    razorpayPaymentId, razorpayOrderId, status);

            // Verify payment status
            if (!"authorized".equals(status)) {
                log.warn("Payment {} has status {} instead of authorized", razorpayPaymentId, status);
                webhookEvent.setProcessed(true);
                webhookEvent.setProcessingResult("Payment status is " + status + ", not authorized");
                webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
                webhookEventRepository.save(webhookEvent);
                return ApiResponse.success("Payment not in authorized status");
            }

            // Confirm payment - this will atomically deduct stock
            Order order = orderService.confirmPayment(razorpayOrderId, razorpayPaymentId);

            // Mark webhook event as processed
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessingResult("Payment confirmed successfully for order: " + order.getOrderNumber());
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            log.info("Payment confirmation completed for order: {}", order.getOrderNumber());
            return ApiResponse.success("Payment confirmed and order processed");

        } catch (Exception e) {
            log.error("Error confirming payment for webhook event: {}", webhookEvent.getId(), e);

            webhookEvent.setProcessed(false);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            return ApiResponse.error("Failed to process payment authorization: " + e.getMessage());
        }
    }
    
    /**
     * Handle payment.failed event from Razorpay.
     * Logs the failure and updates order status if needed.
     * Returns ApiResponse for consistent response handling.
     */
    private ApiResponse handlePaymentFailed(JSONObject event, WebhookEvent webhookEvent) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject payment = payload.getJSONObject("payment");
            JSONObject entity = payment.getJSONObject("entity");

            String razorpayPaymentId = entity.getString("id");
            String razorpayOrderId = entity.optString("order_id", "N/A");
            String reason = entity.optString("description", "Unknown reason");
            String errorCode = entity.optString("error_code", "N/A");

            log.warn("Payment failed: {} for order: {}. Reason: {} (Error Code: {})",
                    razorpayPaymentId, razorpayOrderId, reason, errorCode);

            // Try to update order status if we have the order ID
            if (!"N/A".equals(razorpayOrderId)) {
                try {
                    Order order = orderRepository.findByRazorpayOrderId(razorpayOrderId)
                            .orElse(null);

                    if (order != null && order.getStatus() == Order.OrderStatus.PLACED) {
                        // Update order status to failed
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        orderRepository.save(order);

                        // Create history entry
                        createOrderHistoryEntry(order, Order.OrderStatus.CANCELLED,
                                "SYSTEM", "Payment failed: " + reason);

                        log.info("Order {} marked as cancelled due to payment failure", order.getOrderNumber());
                    }
                } catch (Exception e) {
                    log.error("Failed to update order status for failed payment: {}", razorpayOrderId, e);
                }
            }

            // Mark webhook event as processed
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessingResult("Payment failure logged: " + reason + " (Code: " + errorCode + ")");
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            return ApiResponse.success("Payment failure logged");

        } catch (Exception e) {
            log.error("Error processing payment failed event", e);

            webhookEvent.setProcessed(false);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            return ApiResponse.error("Failed to process payment failure: " + e.getMessage());
        }
    }

    /**
     * Handle order.paid event from Razorpay.
     * This is an additional confirmation that the order has been paid.
     */
    private ApiResponse handleOrderPaid(JSONObject event, WebhookEvent webhookEvent) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject order = payload.getJSONObject("order");
            JSONObject entity = order.getJSONObject("entity");

            String razorpayOrderId = entity.getString("id");
            String status = entity.getString("status");

            log.info("Order paid event received: {} with status: {}", razorpayOrderId, status);

            // Mark webhook event as processed
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessingResult("Order paid event processed: " + status);
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            return ApiResponse.success("Order paid event processed");

        } catch (Exception e) {
            log.error("Error processing order paid event", e);

            webhookEvent.setProcessed(false);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setProcessedAt(java.time.LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);

            return ApiResponse.error("Failed to process order paid event: " + e.getMessage());
        }
    }
    
    /**
     * Stripe webhook endpoint (kept for backward compatibility)
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid signature"));
        }
        
        try {
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    
                    if (paymentIntent != null) {
                        log.info("Stripe payment succeeded: {}", paymentIntent.getId());
                        orderService.confirmPaymentByStripe(paymentIntent.getId());
                    }
                    break;
                    
                case "payment_intent.payment_failed":
                    PaymentIntent failedIntent = (PaymentIntent) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    
                    if (failedIntent != null) {
                        log.warn("Stripe payment failed: {}", failedIntent.getId());
                    }
                    break;
                    
                default:
                    log.debug("Unhandled Stripe event type: {}", event.getType());
            }
            
            return ResponseEntity.ok(ApiResponse.success("Webhook received"));
            
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to process webhook: " + e.getMessage()));
        }
    }

    /**
     * Create order history entry for webhook events
     */
    private void createOrderHistoryEntry(Order order, Order.OrderStatus status, String updatedBy, String notes) {
        OrderHistory history = OrderHistory.builder()
                .order(order)
                .status(status)
                .timestamp(java.time.LocalDateTime.now())
                .notes(notes)
                .updatedBy(updatedBy)
                .build();

        orderHistoryRepository.save(history);
    }

    /**
     * Scheduled task to retry failed webhook events.
     * Runs every 5 minutes to retry events that failed processing.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void retryFailedWebhookEvents() {
        log.info("Checking for failed webhook events to retry...");

        try {
            // Find failed webhook events from the last 24 hours
            var failedEvents = webhookEventRepository.findAll().stream()
                    .filter(event -> !event.getProcessed() &&
                                   event.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(1)))
                    .toList();

            if (failedEvents.isEmpty()) {
                log.debug("No failed webhook events to retry");
                return;
            }

            log.info("Found {} failed webhook events to retry", failedEvents.size());

            for (WebhookEvent event : failedEvents) {
                try {
                    log.info("Retrying failed webhook event: {}", event.getEventId());

                    JSONObject eventJson = new JSONObject(event.getPayload());
                    String eventType = eventJson.getString("event");

                    ApiResponse result;
                    if ("payment.authorized".equals(eventType)) {
                        result = handlePaymentAuthorized(eventJson, event);
                    } else if ("payment.failed".equals(eventType)) {
                        result = handlePaymentFailed(eventJson, event);
                    } else if ("order.paid".equals(eventType)) {
                        result = handleOrderPaid(eventJson, event);
                    } else {
                        log.debug("Skipping retry for unhandled event type: {}", eventType);
                        continue;
                    }

                    if (result.getSuccess()) {
                        log.info("Successfully retried webhook event: {}", event.getEventId());
                    } else {
                        log.warn("Retry failed for webhook event: {} - {}", event.getEventId(), result.getMessage());
                    }

                } catch (Exception e) {
                    log.error("Error retrying webhook event: {}", event.getEventId(), e);
                    event.setErrorMessage("Retry failed: " + e.getMessage());
                    event.setProcessedAt(java.time.LocalDateTime.now());
                    webhookEventRepository.save(event);
                }
            }

        } catch (Exception e) {
            log.error("Error in webhook retry task", e);
        }
    }
}
