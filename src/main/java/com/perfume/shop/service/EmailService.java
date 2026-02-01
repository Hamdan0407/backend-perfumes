package com.perfume.shop.service;

import com.perfume.shop.entity.EmailEvent;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderItem;
import com.perfume.shop.repository.EmailEventRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Email service with reliable delivery.
 * Features:
 * - Async execution (non-blocking)
 * - Retry logic with exponential backoff
 * - Email event tracking for audit and recovery
 * - Transactional guarantees
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final EmailEventRepository emailEventRepository;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.email.max-retries:3}")
    private Integer maxRetries;
    
    private static final long RETRY_DELAY_SECONDS = 300; // 5 minutes for first retry
    
    /**
     * Send a generic email (used for password reset, etc.)
     */
    public void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
        log.info("Sent email to {}: {}", to, subject);
    }
    
    /**
     * Send order confirmation email asynchronously
     * Persists email event for tracking and retry
     */
    @Transactional
    @Async(value = "emailExecutor")
    public void sendOrderConfirmation(Order order) {
        EmailEvent emailEvent = createEmailEvent(
                order,
                "CONFIRMATION",
                order.getUser().getEmail()
        );
        
        try {
            sendOrderConfirmationEmail(order, emailEvent);
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }
    
    /**
     * Send order status update email asynchronously
     */
    @Transactional
    @Async(value = "emailExecutor")
    public void sendOrderStatusUpdate(Order order) {
        EmailEvent emailEvent = createEmailEvent(
                order,
                "STATUS_UPDATE",
                order.getUser().getEmail()
        );
        
        try {
            sendOrderStatusUpdateEmail(order, emailEvent);
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }
    
    /**
     * Send shipping notification email asynchronously
     */
    @Transactional
    @Async(value = "emailExecutor")
    public void sendShippingNotification(Order order) {
        EmailEvent emailEvent = createEmailEvent(
                order,
                "SHIPPING_NOTIFICATION",
                order.getUser().getEmail()
        );
        
        try {
            sendShippingNotificationEmail(order, emailEvent);
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }
    
    /**
     * Retry failed emails (called by retry scheduler)
     * Only retries emails that are within retry limit
     */
    @Transactional
    @Async(value = "emailRetryExecutor")
    public void retryFailedEmail(EmailEvent emailEvent) {
        log.debug("Retrying email: {} (attempt {}/{})", 
                emailEvent.getId(), 
                emailEvent.getAttemptCount(), 
                emailEvent.getMaxRetries());
        
        try {
            // Fetch fresh order data
            Order order = emailEvent.getOrder();
            
            // Send based on email type
            switch (emailEvent.getEmailType()) {
                case "CONFIRMATION" -> sendOrderConfirmationEmail(order, emailEvent);
                case "STATUS_UPDATE" -> sendOrderStatusUpdateEmail(order, emailEvent);
                case "SHIPPING_NOTIFICATION" -> sendShippingNotificationEmail(order, emailEvent);
                default -> throw new IllegalArgumentException("Unknown email type: " + emailEvent.getEmailType());
            }
            
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }
    
    /**
     * Send order confirmation email
     */
    private void sendOrderConfirmationEmail(Order order, EmailEvent emailEvent) throws MessagingException {
        String htmlContent = buildOrderConfirmationEmail(order);
        
        sendMimeEmail(
                order.getUser().getEmail(),
                "Order Confirmation - " + order.getOrderNumber(),
                htmlContent,
                emailEvent
        );
    }
    
    /**
     * Send order status update email
     */
    private void sendOrderStatusUpdateEmail(Order order, EmailEvent emailEvent) throws MessagingException {
        String htmlContent = buildStatusUpdateEmail(order);
        
        sendMimeEmail(
                order.getUser().getEmail(),
                "Order Status Update - " + order.getOrderNumber(),
                htmlContent,
                emailEvent
        );
    }
    
    /**
     * Send shipping notification email
     */
    private void sendShippingNotificationEmail(Order order, EmailEvent emailEvent) throws MessagingException {
        String htmlContent = buildShippingEmail(order);
        
        sendMimeEmail(
                order.getUser().getEmail(),
                "Your Order Has Shipped - " + order.getOrderNumber(),
                htmlContent,
                emailEvent
        );
    }
    
    /**
     * Send MIME email with retry tracking
     */
    private void sendMimeEmail(String to, String subject, String htmlContent, EmailEvent emailEvent) 
            throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            // Send email
            mailSender.send(message);
            
            // Mark as sent
            markEmailSent(emailEvent);
            
            log.info("Email sent successfully: type={}, orderId={}, to={}", 
                    emailEvent.getEmailType(), 
                    emailEvent.getOrder().getId(), 
                    to);
            
        } catch (MessagingException e) {
            log.error("Failed to send email: type={}, orderId={}, to={}", 
                    emailEvent.getEmailType(), 
                    emailEvent.getOrder().getId(), 
                    to, 
                    e);
            throw e;
        }
    }
    
    /**
     * Create email event record for tracking
     */
    private EmailEvent createEmailEvent(Order order, String emailType, String recipientEmail) {
        EmailEvent emailEvent = EmailEvent.builder()
                .order(order)
                .emailType(emailType)
                .recipientEmail(recipientEmail)
                .status(EmailEvent.EmailStatus.PENDING)
                .attemptCount(0)
                .maxRetries(maxRetries)
                .build();
        
        return emailEventRepository.save(emailEvent);
    }
    
    /**
     * Mark email as successfully sent
     */
    private void markEmailSent(EmailEvent emailEvent) {
        emailEvent.setStatus(EmailEvent.EmailStatus.SENT);
        emailEvent.setAttemptCount(emailEvent.getAttemptCount() + 1);
        emailEvent.setSentAt(LocalDateTime.now());
        
        emailEventRepository.save(emailEvent);
    }
    
    /**
     * Handle email sending failure with retry logic
     * Uses exponential backoff: 5 min, 15 min, 45 min
     */
    private void handleEmailFailure(EmailEvent emailEvent, Exception e) {
        emailEvent.setAttemptCount(emailEvent.getAttemptCount() + 1);
        emailEvent.setLastError(truncateError(e.getMessage()));
        
        // Check if we can retry
        if (emailEvent.getAttemptCount() >= emailEvent.getMaxRetries()) {
            emailEvent.setStatus(EmailEvent.EmailStatus.FAILED);
            log.error("Email failed permanently after {} attempts: type={}, orderId={}, error={}", 
                    emailEvent.getAttemptCount(),
                    emailEvent.getEmailType(), 
                    emailEvent.getOrder().getId(), 
                    e.getMessage());
        } else {
            // Calculate exponential backoff: 5 min * 3^(attempt-1)
            long delaySeconds = RETRY_DELAY_SECONDS * (long) Math.pow(3, emailEvent.getAttemptCount() - 1);
            LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);
            
            emailEvent.setNextRetryAt(nextRetry);
            
            log.warn("Email will be retried: type={}, orderId={}, attempt={}/{}, nextRetry={}, error={}", 
                    emailEvent.getEmailType(),
                    emailEvent.getOrder().getId(),
                    emailEvent.getAttemptCount(),
                    emailEvent.getMaxRetries(),
                    nextRetry,
                    e.getMessage());
        }
        
        emailEventRepository.save(emailEvent);
    }
    
    /**
     * Truncate error message to fit in database column
     */
    private String truncateError(String error) {
        if (error == null) return null;
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }
    
    // ==================== Email Template Methods ====================
    
    private String buildOrderConfirmationEmail(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            items.append(String.format(
                    "<tr><td>%s</td><td>%d</td><td>₹%.2f</td></tr>",
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice()
            ));
        }
        
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4A5568;">Thank you for your order!</h2>
                        <p>Hi %s,</p>
                        <p>Your order has been confirmed and will be shipped soon.</p>
                        
                        <div style="background: #F7FAFC; padding: 15px; border-radius: 5px; margin: 20px 0;">
                            <h3>Order Details</h3>
                            <p><strong>Order Number:</strong> %s</p>
                            <p><strong>Order Date:</strong> %s</p>
                        </div>
                        
                        <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                            <thead>
                                <tr style="background: #EDF2F7;">
                                    <th style="padding: 10px; text-align: left;">Product</th>
                                    <th style="padding: 10px; text-align: left;">Quantity</th>
                                    <th style="padding: 10px; text-align: left;">Price</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>
                        
                        <div style="text-align: right; margin-top: 20px;">
                            <p><strong>Subtotal:</strong> ₹%.2f</p>
                            <p><strong>Tax:</strong> ₹%.2f</p>
                            <p><strong>Shipping:</strong> ₹%.2f</p>
                            <h3 style="color: #2D3748;">Total: ₹%.2f</h3>
                        </div>
                        
                        <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #E2E8F0;">
                            <h3>Shipping Address</h3>
                            <p>%s<br>%s, %s %s</p>
                        </div>
                        
                        <p style="margin-top: 30px;">Thank you for shopping with us!</p>
                    </div>
                </body>
                </html>
                """,
                order.getUser().getEmail().split("@")[0],
                order.getOrderNumber(),
                order.getCreatedAt(),
                items.toString(),
                order.getSubtotal(),
                order.getTax(),
                order.getShippingCost(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingCountry(),
                order.getShippingZipCode()
        );
    }
    
    private String buildStatusUpdateEmail(Order order) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4A5568;">Order Status Update</h2>
                        <p>Hi %s,</p>
                        <p>Your order <strong>%s</strong> status has been updated to: <strong>%s</strong></p>
                        <p>Thank you for your patience!</p>
                    </div>
                </body>
                </html>
                """,
                order.getUser().getEmail().split("@")[0],
                order.getOrderNumber(),
                order.getStatus()
        );
    }
    
    private String buildShippingEmail(Order order) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #4A5568;">Your Order Has Shipped!</h2>
                        <p>Hi %s,</p>
                        <p>Great news! Your order <strong>%s</strong> has been shipped.</p>
                        <p><strong>Tracking Number:</strong> %s</p>
                        <p>You can expect delivery within 3-5 business days.</p>
                        <p>Thank you for your purchase!</p>
                    </div>
                </body>
                </html>
                """,
                order.getUser().getEmail().split("@")[0],
                order.getOrderNumber(),
                order.getTrackingNumber()
        );
    }
}