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
    
    /**
     * Create a Razorpay order for payment processing.
     * Amount should be in smallest currency unit (paise for INR, cents for USD).
     * 
     * @param request RazorpayOrderRequest containing amount, receipt, and customer details
     * @return RazorpayOrderResponse with order details for frontend
     * @throws RuntimeException if order creation fails
     */
    public RazorpayOrderResponse createRazorpayOrder(RazorpayOrderRequest request) {
        try {
            log.info("Razorpay Key ID: {}", razorpayKeyId);
            log.info("Razorpay Key Secret length: {}", razorpayKeySecret != null ? razorpayKeySecret.length() : 0);
            
            // Check if keys are configured (detect placeholder values)
            boolean isDemoMode = razorpayKeyId == null || 
                                 razorpayKeyId.isEmpty() ||
                                 razorpayKeyId.startsWith("${") || 
                                 razorpayKeyId.contains("your_razorpay") ||
                                 razorpayKeyId.equals("rzp_test_your_razorpay_key_id") ||
                                 razorpayKeySecret == null || 
                                 razorpayKeySecret.isEmpty() ||
                                 razorpayKeySecret.startsWith("${") ||
                                 razorpayKeySecret.contains("your_razorpay") ||
                                 razorpayKeySecret.equals("your_razorpay_key_secret");
            
            log.info("Demo mode: {}", isDemoMode);
            
            if (isDemoMode) {
                // Demo mode - generate mock order ID
                log.warn("Razorpay not configured. Using demo mode with mock order ID");
                String mockOrderId = "order_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
                return RazorpayOrderResponse.builder()
                        .razorpayOrderId(mockOrderId)
                        .razorpayKeyId("rzp_test_demo_mode")
                        .amount(request.getAmount())
                        .currency(request.getCurrency())
                        .customerEmail(request.getCustomerEmail())
                        .customerName(request.getCustomerName())
                        .customerPhone(request.getCustomerPhone())
                        .build();
            }
            
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount()); // Amount in paise/cents
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getReceipt()); // Our order number
            orderRequest.put("partial", false); // Full payment required
            orderRequest.put("first_payment_min_amount", request.getAmount()); // Full amount required
            
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
            log.error("Razorpay order creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    /**
     * Verify payment signature using HMAC SHA256.
     * This ensures the payment webhook came from Razorpay and wasn't tampered with.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * In demo mode, accepts any signature for testing purposes.
     * 
     * Signature calculation:
     * HMAC-SHA256(razorpay_order_id + "|" + razorpay_payment_id, webhook_secret)
     * 
     * @param request RazorpayPaymentVerificationRequest containing payment details
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyPaymentSignature(RazorpayPaymentVerificationRequest request) {
        try {
            // Check for demo mode - accept demo signatures
            boolean isDemoMode = razorpayKeyId == null || 
                                 razorpayKeyId.contains("your_razorpay") ||
                                 webhookSecret == null ||
                                 webhookSecret.contains("your_razorpay");
            
            if (isDemoMode) {
                // In demo mode, accept demo signatures
                if (request.getRazorpaySignature() != null && 
                    request.getRazorpaySignature().startsWith("demo_")) {
                    log.info("Demo mode: Accepting demo signature for payment: {}", request.getRazorpayPaymentId());
                    return true;
                }
            }
            
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
     * Ensures webhook payload is authentic and unmodified.
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * Signature is calculated on the entire webhook payload:
     * HMAC-SHA256(payload, webhook_secret)
     * 
     * @param payload Raw webhook payload as string
     * @param signatureHeader X-Razorpay-Signature header value
     * @return true if signature is valid, false otherwise
     */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            if (signatureHeader == null || signatureHeader.isEmpty()) {
                log.warn("Webhook signature header is missing");
                return false;
            }
            
            String expectedSignature = generateHmacSha256(payload, webhookSecret);
            
            // Use constant-time comparison to prevent timing attacks
            boolean isValid = CryptoUtil.constantTimeEquals(expectedSignature, signatureHeader);
            
            if (isValid) {
                log.debug("Webhook signature verified");
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
