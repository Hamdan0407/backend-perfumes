package com.perfume.shop.controller;

import com.perfume.shop.dto.ApiResponse;
import com.perfume.shop.dto.RazorpayPaymentVerificationRequest;
import com.perfume.shop.entity.WebhookEvent;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    
    @Value("${app.stripe.webhook-secret}")
    private String stripeWebhookSecret;
    
    @Value("${app.razorpay.webhook-secret}")
    private String razorpayWebhookSecret;
    
    /**
     * Razorpay webhook endpoint for payment authorization events.
     * Handles payment.authorized event after successful payment.
     * Implements idempotency using webhook event ID tracking.
     */
    @PostMapping("/razorpay/webhook")
    public ResponseEntity<ApiResponse> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        log.info("Received Razorpay webhook");
        
        try {
            // Verify webhook signature
            if (!razorpayService.verifyWebhookSignature(payload, signature)) {
                log.warn("Razorpay webhook signature verification failed");
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Invalid signature"));
            }
            
            JSONObject event = new JSONObject(payload);
            String eventId = event.getString("id");
            String eventType = event.getString("event");
            
            log.info("Processing Razorpay event: {} of type: {}", eventId, eventType);
            
            // Check idempotency - has this event been processed?
            var existingEvent = webhookEventRepository.findByEventId(eventId);
            if (existingEvent.isPresent()) {
                log.info("Webhook event {} already processed. Returning success for idempotency.", eventId);
                return ResponseEntity.ok(ApiResponse.success("Webhook processed"));
            }
            
            // Create webhook event record for idempotency
            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(payload)
                    .processed(false)
                    .build();
            
            webhookEvent = webhookEventRepository.save(webhookEvent);
            
            // Process based on event type
            if ("payment.authorized".equals(eventType)) {
                handlePaymentAuthorized(event, webhookEvent);
            } else if ("payment.failed".equals(eventType)) {
                handlePaymentFailed(event, webhookEvent);
            } else {
                log.debug("Unhandled event type: {}", eventType);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Webhook received and processed"));
            
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to process webhook: " + e.getMessage()));
        }
    }
    
    /**
     * Handle payment.authorized event from Razorpay.
     * Confirms the order and deducts stock atomically.
     */
    private void handlePaymentAuthorized(JSONObject event, WebhookEvent webhookEvent) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject payment = payload.getJSONObject("payment");
            JSONObject entity = payment.getJSONObject("entity");
            
            String razorpayPaymentId = entity.getString("id");
            String razorpayOrderId = entity.getString("order_id");
            
            log.info("Payment authorized: {} for order: {}", razorpayPaymentId, razorpayOrderId);
            
            // Verify payment signature with order ID and payment ID
            RazorpayPaymentVerificationRequest verificationRequest = 
                    RazorpayPaymentVerificationRequest.builder()
                            .razorpayOrderId(razorpayOrderId)
                            .razorpayPaymentId(razorpayPaymentId)
                            .razorpaySignature(entity.getString("signature")) // If available in event
                            .build();
            
            // Confirm payment - this will atomically deduct stock
            orderService.confirmPayment(razorpayOrderId, razorpayPaymentId);
            
            // Mark webhook event as processed
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessingResult("Payment confirmed successfully");
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            
            log.info("Payment confirmation completed for order: {}", razorpayOrderId);
            
        } catch (Exception e) {
            log.error("Error confirming payment for webhook event: {}", webhookEvent.getId(), e);
            
            webhookEvent.setProcessed(false);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            
            throw new RuntimeException("Failed to process payment authorization: " + e.getMessage());
        }
    }
    
    /**
     * Handle payment.failed event from Razorpay.
     * Logs the failure but doesn't need to do anything else as order remains PENDING.
     */
    private void handlePaymentFailed(JSONObject event, WebhookEvent webhookEvent) {
        try {
            JSONObject payload = event.getJSONObject("payload");
            JSONObject payment = payload.getJSONObject("payment");
            JSONObject entity = payment.getJSONObject("entity");
            
            String razorpayPaymentId = entity.getString("id");
            String razorpayOrderId = entity.getString("order_id");
            String reason = entity.optString("description", "Unknown reason");
            
            log.warn("Payment failed: {} for order: {}. Reason: {}", 
                    razorpayPaymentId, razorpayOrderId, reason);
            
            // Mark webhook event as processed
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessingResult("Payment failure logged: " + reason);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            
        } catch (Exception e) {
            log.error("Error processing payment failed event", e);
            
            webhookEvent.setProcessed(false);
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            
            throw new RuntimeException("Failed to process payment failure: " + e.getMessage());
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
}
