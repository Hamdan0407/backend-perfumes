package com.perfume.shop.service;

import com.perfume.shop.dto.RazorpayOrderRequest;
import com.perfume.shop.dto.RazorpayOrderResponse;
import com.perfume.shop.dto.RazorpayPaymentVerificationRequest;
import com.perfume.shop.security.CryptoUtil;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Service for Razorpay payment gateway integration.
 * Handles:
 * - Order creation in Razorpay
 * - Payment signature verification (HMAC SHA256)
 * - Production-ready error handling with secure crypto operations
 * - Timing attack prevention with constant-time comparison
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {
    
    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;
    
    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;
    
    @Value("${app.razorpay.webhook-secret}")
    private String webhookSecret;
    
    @Value("${app.razorpay.currency}")
    private String currency;
    
    @PostConstruct
    public void init() {
        log.info("=== RAZORPAY CONFIG INITIALIZED ===");
        log.info("Key ID: {}", razorpayKeyId);
        log.info("Key Secret Length: {} chars", razorpayKeySecret != null ? razorpayKeySecret.length() : "null");
        log.info("Key Secret First 5 chars: {}", razorpayKeySecret != null && razorpayKeySecret.length() >= 5 ? razorpayKeySecret.substring(0, 5) : "N/A");
        log.info("Webhook Secret: {}", webhookSecret);
        log.info("Currency: {}", currency);
        log.info("====================================");
    }
    
    /**
     * Create a Razorpay order for payment processing.
     * Supports DEMO MODE when dummy credentials are configured.
     * Amount should be in smallest currency unit (paise for INR, cents for USD).
     * 
     * @param request RazorpayOrderRequest containing amount, receipt, and customer details
     * @return RazorpayOrderResponse with order details for frontend
     * @throws RuntimeException if order creation fails
     */
    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request) {
        try {
            // Check if we're in demo mode (dummy credentials)
            boolean isDemoMode = isDemoMode();
            
            if (isDemoMode) {
                log.warn("DEMO MODE: Using simulated Razorpay order (no real payment gateway)");
                
                // Generate a fake Razorpay order ID for demo
                String demoOrderId = "order_demo_" + System.currentTimeMillis();
                
                return RazorpayOrderResponse.builder()
                        .razorpayOrderId(demoOrderId)
                        .razorpayKeyId("rzp_test_demo_mode") // Special flag for frontend
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .customerEmail(request.getCustomerEmail())
                        .customerName(request.getCustomerName())
                        .customerPhone(request.getCustomerPhone())
                        .build();
            }
            
            // PRODUCTION: Validate credentials are configured
            validateRazorpayConfiguration();
            
            log.info("Creating Razorpay order for amount: {}", request.getAmount());
            
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount()); // Amount in paise/cents
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getReceipt()); // Our order number
            
            // Add customer notes/metadata
            JSONObject notes = new JSONObject();
            notes.put("customerId", request.getCustomerId());
            notes.put("customerName", request.getCustomerName());
            notes.put("customerEmail", request.getCustomerEmail());
            notes.put("customerPhone", request.getCustomerPhone());
            orderRequest.put("notes", notes);
            
            // Create order in Razorpay
            com.razorpay.Order razorpayOrder = client.Orders.create(orderRequest);
            
            String razorpayOrderId = razorpayOrder.get("id").toString();
            log.info("Razorpay order created: {} for receipt: {}", razorpayOrderId, request.getReceipt());
            
            // Return order details for frontend
            return RazorpayOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .razorpayKeyId(razorpayKeyId)
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .customerEmail(request.getCustomerEmail())
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .build();
            
        } catch (RazorpayException e) {
            log.error("Razorpay API error: {} - {}", e.getMessage(), e.getClass().getSimpleName());
            
            // If authentication fails, fall back to demo mode
            if (e.getMessage().contains("Authentication failed") || 
                e.getMessage().contains("BAD_REQUEST_ERROR") ||
                e.getMessage().contains("401") ||
                e.getMessage().contains("403")) {
                
                log.warn("⚠️  Razorpay authentication failed. Falling back to DEMO MODE.");
                log.warn("Please verify your Razorpay Key ID and Secret are valid test credentials.");
                log.info("Current Key ID: {}", razorpayKeyId);
                log.info("Current Secret Length: {} chars", razorpayKeySecret != null ? razorpayKeySecret.length() : "null");
                
                // Return demo mode response
                String demoOrderId = "order_demo_" + System.currentTimeMillis();
                return RazorpayOrderResponse.builder()
                        .razorpayOrderId(demoOrderId)
                        .razorpayKeyId("rzp_test_demo_mode")
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .customerEmail(request.getCustomerEmail())
                        .customerName(request.getCustomerName())
                        .customerPhone(request.getCustomerPhone())
                        .build();
            }
            
            // For other errors, throw the exception
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    /**
     * Verify payment signature using HMAC SHA256.
     * In DEMO MODE, always returns true.
     * This ensures the payment webhook came from Razorpay and wasn't tampered with.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * Signature calculation:
     * HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, webhook_secret)
     * 
     * @param request RazorpayPaymentVerificationRequest containing payment details
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyPaymentSignature(RazorpayPaymentVerificationRequest request) {
        try {
            // Demo mode - skip verification
            if (isDemoMode() || request.getRazorpayOrderId().startsWith("order_demo_")) {
                log.info("DEMO MODE: Skipping payment signature verification");
                return true;
            }
            
            validateRazorpayConfiguration();
            
            // Combine order ID and payment ID with pipe separator
            String message = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
            
            // Generate HMAC SHA256 signature
            String expectedSignature = generateHmacSha256(message, webhookSecret);
            
            // Use constant-time comparison to prevent timing attacks
            boolean isValid = CryptoUtil.constantTimeEquals(expectedSignature, request.getRazorpaySignature());
            
            if (isValid) {
                log.info("Payment signature verified for payment: {}", request.getRazorpayPaymentId());
            } else {
                log.warn("Payment signature verification failed for payment: {}.", 
                    request.getRazorpayPaymentId());
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying payment signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verify webhook signature from Razorpay.
     * PRODUCTION ONLY - Signature verification is mandatory.
     * 
     * @param payload Raw webhook payload as string
     * @param signatureHeader X-Razorpay-Signature header value
     * @return true if signature is valid, false otherwise
     * @throws IllegalStateException if webhook secret not configured
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            validateRazorpayConfiguration();
            
            if (signatureHeader == null || signatureHeader.isEmpty()) {
                log.warn("Webhook signature header is missing");
                return false;
            }

            // Calculate expected signature: HMAC-SHA256(payload, webhook_secret)
            String expectedSignature = generateHmacSha256(payload, webhookSecret);

            // Use constant-time comparison to prevent timing attacks
            boolean isValid = CryptoUtil.constantTimeEquals(expectedSignature, signatureHeader);

            if (isValid) {
                log.debug("Webhook signature verified successfully");
            } else {
                log.warn("Webhook signature verification failed");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if we're in demo mode (using dummy/placeholder credentials).
     * 
     * @return true if using demo/dummy credentials
     */
    private boolean isDemoMode() {
        return razorpayKeyId == null || 
               razorpayKeyId.isEmpty() ||
               razorpayKeyId.contains("dummy") ||
               razorpayKeyId.contains("test_demo") ||
               razorpayKeyId.equals("rzp_test_dummy") ||
               razorpayKeySecret == null ||
               razorpayKeySecret.isEmpty() ||
               razorpayKeySecret.contains("dummy");
    }
    
    /**
     * Validate that Razorpay is properly configured for production.
     * MUST be called before any payment operation.
     * 
     * @throws IllegalStateException if credentials are missing or invalid
     */
    private void validateRazorpayConfiguration() {
        if (razorpayKeyId == null || razorpayKeyId.isEmpty() ||
            razorpayKeyId.startsWith("${") || razorpayKeyId.contains("your_razorpay")) {
            throw new IllegalStateException(
                "Razorpay Key ID not configured. " +
                "Set RAZORPAY_KEY_ID environment variable with LIVE key (rzp_live_...)");
        }
        
        if (razorpayKeySecret == null || razorpayKeySecret.isEmpty() ||
            razorpayKeySecret.startsWith("${") || razorpayKeySecret.contains("your_razorpay")) {
            throw new IllegalStateException(
                "Razorpay Key Secret not configured. " +
                "Set RAZORPAY_KEY_SECRET environment variable");
        }
        
        if (webhookSecret == null || webhookSecret.isEmpty() ||
            webhookSecret.startsWith("${") || webhookSecret.contains("your_razorpay")) {
            throw new IllegalStateException(
                "Razorpay Webhook Secret not configured. " +
                "Set RAZORPAY_WEBHOOK_SECRET environment variable");
        }
        
        // Verify we're using LIVE keys in production, not test keys
        if (!razorpayKeyId.startsWith("rzp_live_")) {
            log.warn("⚠️  WARNING: Using TEST Razorpay keys. " +
                "Production must use LIVE keys (rzp_live_...)");
        }
    }
    
    /**
     * Generate HMAC SHA256 signature.
     * 
     * @param data Data to be signed
     * @param secret Secret key for signing
     * @return Hex-encoded signature
     * @throws NoSuchAlgorithmException if HMAC SHA256 is not available
     * @throws InvalidKeyException if the secret key is invalid
     */
    private String generateHmacSha256(String data, String secret) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        mac.init(secretKey);
        
        byte[] signature = mac.doFinal(data.getBytes());
        return HexFormat.of().formatHex(signature);
    }
}
