package com.perfume.shop.service;

import com.perfume.shop.entity.EmailEvent;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.OrderItem;
import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.CartItem;
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

import java.math.BigDecimal;
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
    private final InvoicePdfService invoicePdfService;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.max-retries:3}")
    private Integer maxRetries;

    private static final long RETRY_DELAY_SECONDS = 300; // 5 minutes for first retry

    /**
     * Send a generic email (used for password reset, etc.)
     */
    public void sendEmail(String to, String subject, String htmlBody) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("✅ Email sent successfully to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            // Log the email details for debugging in demo mode
            log.info("═══════════════════════════════════════════════════════");
            log.info("📧 EMAIL DETAILS (FAILED TO SEND)");
            log.info("═══════════════════════════════════════════════════════");
            log.info("To: {}", to);
            log.info("Subject: {}", subject);
            log.info("───────────────────────────────────────────────────────");
            log.info("Body:\n{}", htmlBody);
            log.info("═══════════════════════════════════════════════════════");
            throw new MessagingException("Failed to send email: " + e.getMessage(), e);
        }
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
                order.getUser().getEmail());

        try {
            sendOrderConfirmationEmail(order, emailEvent);
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }

    /**
     * Send admin notification email when order is placed
     */
    @Transactional
    @Async(value = "emailExecutor")
    public void sendAdminOrderNotification(Order order) {
        EmailEvent emailEvent = createEmailEvent(
                order,
                "ADMIN_NOTIFICATION",
                fromEmail);

        try {
            sendAdminNotificationEmail(order, emailEvent);
        } catch (Exception e) {
            handleEmailFailure(emailEvent, e);
        }
    }

    /**
     * Send order status update email asynchronously
     * Triggers for all meaningful status changes: PLACED, CONFIRMED, PACKED,
     * SHIPPED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED, REFUNDED
     */
    @Transactional
    @Async(value = "emailExecutor")
    public void sendOrderStatusUpdate(Order order) {
        try {
            // Check if this status change should trigger an email
            if (!isMeaningfulStatusChange(order.getStatus())) {
                log.debug("Skipping email for status: {} - not a meaningful change", order.getStatus());
                return;
            }

            EmailEvent emailEvent = createEmailEvent(
                    order,
                    "STATUS_UPDATE",
                    order.getUser().getEmail());

            log.info("Attempting to send status update email for order: {} with status: {}",
                    order.getOrderNumber(), order.getStatus());

            sendOrderStatusUpdateEmail(order, emailEvent);

            log.info("Status update email sent successfully for order: {} with status: {}",
                    order.getOrderNumber(), order.getStatus());
        } catch (Exception e) {
            log.error("Failed to send status update email for order: {}", order.getOrderNumber(), e);
            // Don't throw exception - let async execution handle it
        }
    }

    /**
     * Determine if a status change should trigger an email notification
     */
    private boolean isMeaningfulStatusChange(Order.OrderStatus status) {
        // Send emails for all these meaningful statuses
        return status == Order.OrderStatus.PLACED ||
                status == Order.OrderStatus.CONFIRMED ||
                status == Order.OrderStatus.PACKED ||
                status == Order.OrderStatus.SHIPPED ||
                status == Order.OrderStatus.OUT_FOR_DELIVERY ||
                status == Order.OrderStatus.DELIVERED ||
                status == Order.OrderStatus.CANCELLED ||
                status == Order.OrderStatus.REFUNDED;
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
                order.getUser().getEmail());

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
                emailEvent);
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
                emailEvent);
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
                emailEvent);
    }

    /**
     * Send MIME email with retry tracking and PDF attachment for order
     * confirmations
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

            // Attach PDF invoice for order confirmation and admin notification
            if (emailEvent.getEmailType().equals("CONFIRMATION")
                    || emailEvent.getEmailType().equals("ADMIN_NOTIFICATION")) {
                try {
                    byte[] pdfBytes = invoicePdfService.generateInvoicePdf(emailEvent.getOrder());
                    helper.addAttachment("Invoice_" + emailEvent.getOrder().getOrderNumber() + ".pdf",
                            () -> new java.io.ByteArrayInputStream(pdfBytes), "application/pdf");
                } catch (Exception e) {
                    log.warn("Failed to attach PDF invoice to email, continuing without attachment", e);
                }
            }

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
        if (error == null)
            return null;
        return error.length() > 1000 ? error.substring(0, 1000) : error;
    }

    // ==================== Email Template Methods ====================

    private String buildOrderConfirmationEmail(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            items.append(String.format(
                    """
                            <tr>
                                <td style="padding: 15px 10px; border-bottom: 1px solid #e5e7eb;">
                                    <div style="font-weight: 600; color: #111827; margin-bottom: 4px;">%s</div>
                                    <div style="font-size: 13px; color: #6b7280;">Qty: %d</div>
                                </td>
                                <td style="padding: 15px 10px; border-bottom: 1px solid #e5e7eb; text-align: right; font-weight: 600; color: #111827;">₹%.2f</td>
                            </tr>
                            """,
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice()));
        }

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Order Confirmation - MUWAS</title>
                        </head>
                        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f3f4f6;">
                            <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f3f4f6;">
                                <tr>
                                    <td align="center" style="padding: 40px 20px;">
                                        <!-- Main Container -->
                                        <table role="presentation" style="width: 100%%; max-width: 600px; border-collapse: collapse; background-color: #ffffff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border-radius: 8px; overflow: hidden;">

                                            <!-- Header Section -->
                                            <tr>
                                                <td style="background: linear-gradient(135deg, #1a202c 0%%, #2d3748 100%%); padding: 30px 40px; text-align: center;">
                                                    <h1 style="margin: 0; color: #f59e0b; font-size: 28px; font-weight: 700; letter-spacing: 1px;">MUWAS</h1>
                                                    <p style="margin: 8px 0 0 0; color: #e5e7eb; font-size: 14px;">Luxury Fragrances & Premium Scents</p>
                                                </td>
                                            </tr>

                                            <!-- Success Badge -->
                                            <tr>
                                                <td style="padding: 30px 40px 20px; text-align: center;">
                                                    <div style="display: inline-block; background-color: #dcfce7; color: #166534; padding: 8px 20px; border-radius: 20px; font-size: 14px; font-weight: 600;">
                                                        ✓ Order Placed Successfully
                                                    </div>
                                                </td>
                                            </tr>

                                            <!-- Greeting -->
                                            <tr>
                                                <td style="padding: 0 40px;">
                                                    <h2 style="margin: 0 0 12px 0; color: #111827; font-size: 24px; font-weight: 700;">Thank you for your order!</h2>
                                                    <p style="margin: 0; color: #6b7280; font-size: 15px; line-height: 1.6;">Hi <strong>%s</strong>, your order has been confirmed and is being processed. We'll notify you once it ships.</p>
                                                </td>
                                            </tr>

                                            <!-- Order Info Box -->
                                            <tr>
                                                <td style="padding: 25px 40px;">
                                                    <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #fef3c7; border-radius: 8px; border: 2px solid #f59e0b;">
                                                        <tr>
                                                            <td style="padding: 20px;">
                                                                <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                                    <tr>
                                                                        <td style="padding: 6px 0; color: #78350f; font-size: 13px; font-weight: 600; text-transform: uppercase;">Order Number</td>
                                                                        <td style="padding: 6px 0; text-align: right; color: #92400e; font-size: 16px; font-weight: 700;">%s</td>
                                                                    </tr>
                                                                    <tr>
                                                                        <td style="padding: 6px 0; color: #78350f; font-size: 13px; font-weight: 600; text-transform: uppercase;">Order Date</td>
                                                                        <td style="padding: 6px 0; text-align: right; color: #92400e; font-size: 14px; font-weight: 600;">%s</td>
                                                                    </tr>
                                                                </table>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>

                                            <!-- Order Items -->
                                            <tr>
                                                <td style="padding: 0 40px;">
                                                    <h3 style="margin: 0 0 15px 0; color: #111827; font-size: 18px; font-weight: 700; border-bottom: 2px solid #f59e0b; padding-bottom: 10px;">Order Items</h3>
                                                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                        %s
                                                    </table>
                                                </td>
                                            </tr>

                                            <!-- Price Summary -->
                                            <tr>
                                                <td style="padding: 25px 40px;">
                                                    <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                        <tr>
                                                            <td style="padding: 8px 0; color: #6b7280; font-size: 15px;">Subtotal</td>
                                                            <td style="padding: 8px 0; text-align: right; color: #111827; font-size: 15px; font-weight: 600;">₹%.2f</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 8px 0; color: #6b7280; font-size: 15px;">GST (18%%)</td>
                                                            <td style="padding: 8px 0; text-align: right; color: #111827; font-size: 15px; font-weight: 600;">₹%.2f</td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 8px 0; color: #6b7280; font-size: 15px;">Shipping</td>
                                                            <td style="padding: 8px 0; text-align: right; color: #111827; font-size: 15px; font-weight: 600;">₹%.2f</td>
                                                        </tr>
                                                        <tr>
                                                            <td colspan="2" style="padding: 15px 0 0 0;"><div style="border-top: 2px solid #e5e7eb;"></div></td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 15px 0 0 0; color: #111827; font-size: 18px; font-weight: 700;">Total Amount</td>
                                                            <td style="padding: 15px 0 0 0; text-align: right; color: #f59e0b; font-size: 24px; font-weight: 800;">₹%.2f</td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>

                                            <!-- Shipping Address -->
                                            <tr>
                                                <td style="padding: 25px 40px;">
                                                    <div style="background-color: #f9fafb; border-left: 4px solid #f59e0b; padding: 20px; border-radius: 6px;">
                                                        <h3 style="margin: 0 0 12px 0; color: #111827; font-size: 16px; font-weight: 700;">📦 Shipping Address</h3>
                                                        <p style="margin: 0; color: #374151; font-size: 14px; line-height: 1.8;">
                                                            %s<br>
                                                            %s, %s<br>
                                                            %s
                                                        </p>
                                                    </div>
                                                </td>
                                            </tr>

                                            <!-- Contact Information -->
                                            <tr>
                                                <td style="padding: 25px 40px 30px;">
                                                    <div style="background-color: #eff6ff; border: 1px solid #bfdbfe; padding: 20px; border-radius: 6px; text-align: center;">
                                                        <p style="margin: 0 0 10px 0; color: #1e40af; font-size: 14px; font-weight: 600;">Need Help?</p>
                                                        <p style="margin: 0; color: #3b82f6; font-size: 14px;">
                                                            📞 +91 9629004158 | ✉️ muwas2021@gmail.com
                                                        </p>
                                                    </div>
                                                </td>
                                            </tr>

                                            <!-- Footer -->
                                            <tr>
                                                <td style="background-color: #1f2937; padding: 30px 40px; text-align: center;">
                                                    <p style="margin: 0 0 10px 0; color: #9ca3af; font-size: 14px;">Thank you for shopping with <strong style="color: #f59e0b;">MUWAS</strong></p>
                                                    <p style="margin: 0; color: #6b7280; font-size: 12px;">This is an automated email. Please do not reply.</p>
                                                    <p style="margin: 15px 0 0 0; color: #4b5563; font-size: 11px;">
                                                        © 2026 MUWAS. All rights reserved.
                                                    </p>
                                                </td>
                                            </tr>

                                        </table>
                                    </td>
                                </tr>
                            </table>
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
                order.getShippingZipCode());
    }

    private String buildStatusUpdateEmail(Order order) {
        Order.OrderStatus status = order.getStatus();
        String statusMessage = getStatusMessage(status);
        String statusColor = getStatusColor(status);
        String statusEmoji = getStatusEmoji(status);
        String additionalInfo = getAdditionalStatusInfo(order);

        return String.format(
                """
                        <html>
                        <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                                <h2 style="color: %s; margin-bottom: 20px;">
                                    %s Order Status Update
                                </h2>

                                <p>Hi %s,</p>

                                <div style="background-color: #f5f5f5; padding: 15px; border-left: 4px solid %s; margin: 20px 0; border-radius: 4px;">
                                    <p><strong>Order Number:</strong> %s</p>
                                    <p><strong>Current Status:</strong> <span style="color: %s; font-weight: bold;">%s</span></p>
                                    <p><strong>Order Date:</strong> %s</p>
                                    <p><strong>Total Amount:</strong> Rs. %s</p>
                                </div>

                                <p>%s</p>

                                %s

                                <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #e0e0e0;">
                                    <p style="color: #666; font-size: 12px;">
                                        This is an automated notification. Please do not reply to this email.
                                    </p>
                                    <p style="color: #666; font-size: 12px;">
                                        If you have any questions, please contact our customer support.
                                    </p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                statusColor,
                statusEmoji,
                order.getUser().getFirstName(),
                statusColor,
                order.getOrderNumber(),
                statusColor,
                formatStatus(status),
                order.getCreatedAt(),
                order.getTotalAmount(),
                statusMessage,
                additionalInfo);
    }

    /**
     * Get customer-friendly message for each order status
     */
    private String getStatusMessage(Order.OrderStatus status) {
        return switch (status) {
            case PLACED -> "Thank you for your order! We have received your payment and your order is being processed.";
            case CONFIRMED ->
                "Great news! Your order has been confirmed by our team. We are preparing it for shipment.";
            case PACKED ->
                "Your order has been packed and is ready for shipment. It will be handed over to our courier soon.";
            case HANDOVER -> "Your order has been handed over to our courier partner. It will be shipped shortly.";
            case SHIPPED -> "Your order is on its way! Track your package using the tracking number below.";
            case OUT_FOR_DELIVERY -> "Your order is out for delivery today! Please be ready to receive it.";
            case DELIVERED -> "Congratulations! Your order has been delivered. We hope you enjoy your purchase!";
            case CANCELLED -> "Your order has been cancelled. If this was unexpected, please contact us immediately.";
            case REFUNDED ->
                "Your refund has been processed successfully. The amount will be credited to your account within 5-7 business days.";
        };
    }

    /**
     * Get status color for email formatting
     */
    private String getStatusColor(Order.OrderStatus status) {
        return switch (status) {
            case PLACED -> "#FF9800"; // Orange
            case CONFIRMED -> "#2196F3"; // Blue
            case PACKED -> "#673AB7"; // Purple
            case HANDOVER -> "#009688"; // Teal
            case SHIPPED -> "#00BCD4"; // Cyan
            case OUT_FOR_DELIVERY -> "#4CAF50"; // Green
            case DELIVERED -> "#388E3C"; // Dark Green
            case CANCELLED -> "#F44336"; // Red
            case REFUNDED -> "#FF5722"; // Deep Orange
        };
    }

    /**
     * Get emoji for status
     */
    private String getStatusEmoji(Order.OrderStatus status) {
        return switch (status) {
            case PLACED -> "📦";
            case CONFIRMED -> "✅";
            case PACKED -> "🎁";
            case HANDOVER -> "🚚";
            case SHIPPED -> "📮";
            case OUT_FOR_DELIVERY -> "🚀";
            case DELIVERED -> "🎉";
            case CANCELLED -> "❌";
            case REFUNDED -> "💰";
        };
    }

    /**
     * Get additional status-specific information
     */
    private String getAdditionalStatusInfo(Order order) {
        Order.OrderStatus status = order.getStatus();

        String trackingSection = "";
        if (order.getTrackingNumber() != null && !order.getTrackingNumber().trim().isEmpty()) {
            trackingSection = String.format("""
                    <div style="background-color: #f0f8ff; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>📍 Tracking Information:</strong></p>
                        <p>Tracking Number: <strong>%s</strong></p>
                    </div>
                    """, order.getTrackingNumber());
        }

        String orderItemsSection = "";
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            StringBuilder items = new StringBuilder("""
                    <div style="background-color: #fffacd; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>📋 Order Items:</strong></p>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                    """);

            for (OrderItem item : order.getItems()) {
                items.append(String.format(
                        "<li>%s x %d (Rs. %s)</li>",
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPrice()));
            }

            items.append("</ul></div>");
            orderItemsSection = items.toString();
        }

        String nextStepSection = "";
        if (status == Order.OrderStatus.OUT_FOR_DELIVERY) {
            nextStepSection = """
                    <div style="background-color: #e8f5e9; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>📞 What to do next:</strong></p>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                            <li>Please ensure someone is available to receive the package</li>
                            <li>Keep your phone accessible for delivery partner updates</li>
                            <li>If you're not available, you can contact your delivery partner using the tracking number</li>
                        </ul>
                    </div>
                    """;
        } else if (status == Order.OrderStatus.DELIVERED) {
            nextStepSection = """
                    <div style="background-color: #e8f5e9; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>⭐ Thank You!</strong></p>
                        <p>We appreciate your purchase! If you have any feedback or issues with your order, please contact us immediately.</p>
                    </div>
                    """;
        } else if (status == Order.OrderStatus.CANCELLED) {
            nextStepSection = """
                    <div style="background-color: #ffebee; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>⚠️ Important:</strong></p>
                        <p>If you didn't request this cancellation, please reach out to us immediately. We're happy to help!</p>
                    </div>
                    """;
        } else if (status == Order.OrderStatus.REFUNDED) {
            nextStepSection = """
                    <div style="background-color: #e8f5e9; padding: 15px; margin: 20px 0; border-radius: 4px;">
                        <p><strong>ℹ️  Refund Details:</strong></p>
                        <ul style="margin: 10px 0; padding-left: 20px;">
                            <li>Refund Amount: Rs. %s</li>
                            <li>Processing Time: 5-7 business days</li>
                            <li>Please check your bank account or payment method</li>
                        </ul>
                    </div>
                    """.formatted(order.getTotalAmount());
        }

        return trackingSection + orderItemsSection + nextStepSection;
    }

    /**
     * Format status for display
     */
    private String formatStatus(Order.OrderStatus status) {
        String statusStr = status.toString()
                .replace("_", " ")
                .toLowerCase();

        // Capitalize first letter of each word
        String[] words = statusStr.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    /**
     * Send admin notification email
     */
    private void sendAdminNotificationEmail(Order order, EmailEvent emailEvent) throws MessagingException {
        String htmlContent = buildAdminNotificationEmail(order);

        sendMimeEmail(
                fromEmail,
                "New Order Placed - " + order.getOrderNumber(),
                htmlContent,
                emailEvent);
    }

    private String buildAdminNotificationEmail(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            items.append(String.format("""
                    <tr>
                        <td style="padding: 10px; border-bottom: 1px solid #ddd;">%s</td>
                        <td style="padding: 10px; border-bottom: 1px solid #ddd; text-align: center;">%d</td>
                        <td style="padding: 10px; border-bottom: 1px solid #ddd; text-align: right;">₹%.2f</td>
                    </tr>
                    """,
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice()));
        }

        return String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                                .container { max-width: 700px; margin: 0 auto; padding: 20px; background: #f9fafb; }
                                .header { background: #1a202c; color: white; padding: 20px; border-radius: 5px 5px 0 0; }
                                .content { background: white; padding: 20px; border-radius: 0 0 5px 5px; }
                                .info-box { background: #fef3c7; padding: 15px; border-left: 4px solid #f59e0b; margin: 20px 0; }
                                table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                                th { background: #f3f4f6; padding: 10px; text-align: left; font-weight: bold; }
                                .total { font-size: 18px; font-weight: bold; color: #f59e0b; }
                                .footer { font-size: 12px; color: #6b7280; margin-top: 20px; text-align: center; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h2 style="margin: 0;">📦 New Order Received</h2>
                                </div>
                                <div class="content">
                                    <div class="info-box">
                                        <strong>Order #:</strong> %s<br>
                                        <strong>Customer:</strong> %s (%s)<br>
                                        <strong>Status:</strong> %s<br>
                                        <strong>Date:</strong> %s
                                    </div>

                                    <h3>Order Items:</h3>
                                    <table>
                                        <thead>
                                            <tr>
                                                <th>Product</th>
                                                <th>Qty</th>
                                                <th>Price</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            %s
                                        </tbody>
                                    </table>

                                    <h3>Shipping Address:</h3>
                                    <div style="background: #f3f4f6; padding: 15px; border-radius: 5px;">
                                        %s<br>
                                        %s, %s<br>
                                        %s<br>
                                        📞 %s
                                    </div>

                                    <h3>Order Summary:</h3>
                                    <table>
                                        <tr>
                                            <td>Subtotal:</td>
                                            <td style="text-align: right;">₹%.2f</td>
                                        </tr>
                                        <tr>
                                            <td>Shipping:</td>
                                            <td style="text-align: right;">₹%.2f</td>
                                        </tr>
                                        <tr>
                                            <td>Tax:</td>
                                            <td style="text-align: right;">₹%.2f</td>
                                        </tr>
                                        <tr style="border-top: 2px solid #ddd; font-weight: bold; font-size: 16px;">
                                            <td>Total:</td>
                                            <td style="text-align: right; color: #f59e0b;">₹%.2f</td>
                                        </tr>
                                    </table>

                                    <div class="footer">
                                        <p>This is an automated admin notification. Please log in to the admin panel for more details.</p>
                                    </div>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                order.getOrderNumber(),
                order.getUser().getFirstName() != null ? order.getUser().getFirstName() : "Customer",
                order.getUser().getEmail(),
                order.getStatus(),
                order.getCreatedAt(),
                items.toString(),
                order.getShippingAddress(),
                order.getShippingCity(),
                order.getShippingCountry(),
                order.getShippingZipCode(),
                order.getShippingPhone() != null ? order.getShippingPhone() : "N/A",
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTax(),
                order.getTotalAmount());
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
                order.getTrackingNumber());
    }

    /**
     * Send abandoned cart recovery email
     */
    public void sendAbandonedCartEmail(Cart cart) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(cart.getUser().getEmail());
            helper.setSubject("Don't forget your items! 🛒");

            String emailContent = buildAbandonedCartEmailContent(cart);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("✅ Abandoned cart email sent to: {}", cart.getUser().getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send abandoned cart email", e);
            throw new RuntimeException("Failed to send abandoned cart email", e);
        }
    }

    private String buildAbandonedCartEmailContent(Cart cart) {
        StringBuilder items = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            items.append(String.format(
                    """
                            <tr>
                                <td style="padding: 15px 10px; border-bottom: 1px solid #e5e7eb;">
                                    <div style="font-weight: 600; color: #111827; margin-bottom: 4px;">%s</div>
                                    <div style="font-size: 13px; color: #6b7280;">Qty: %d</div>
                                </td>
                                <td style="padding: 15px 10px; border-bottom: 1px solid #e5e7eb; text-align: right; font-weight: 600; color: #111827;">₹%.2f</td>
                            </tr>
                            """,
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getPrice()));
            total = total.add(item.getSubtotal());
        }

        return String.format(
                """
                        <!DOCTYPE html>
                        <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <title>Complete Your Purchase - MUWAS</title>
                        </head>
                        <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f3f4f6;">
                            <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f3f4f6;">
                                <tr>
                                    <td align="center" style="padding: 40px 20px;">
                                        <table role="presentation" style="width: 100%%; max-width: 600px; border-collapse: collapse; background-color: #ffffff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); border-radius: 8px; overflow: hidden;">

                                            <!-- Header -->
                                            <tr>
                                                <td style="background: linear-gradient(135deg, #1a202c 0%%, #2d3748 100%%); padding: 30px 40px; text-align: center;">
                                                    <h1 style="margin: 0; color: #f59e0b; font-size: 28px; font-weight: 700; letter-spacing: 1px;">MUWAS</h1>
                                                    <p style="margin: 8px 0 0 0; color: #e5e7eb; font-size: 14px;">Luxury Fragrances & Premium Scents</p>
                                                </td>
                                            </tr>

                                            <!-- Main Content -->
                                            <tr>
                                                <td style="padding: 40px;">
                                                    <h2 style="margin: 0 0 16px 0; color: #111827; font-size: 24px; font-weight: 700;">You left something behind! 🛍️</h2>
                                                    <p style="margin: 0 0 20px 0; color: #6b7280; font-size: 15px; line-height: 1.6;">
                                                        Hi <strong>%s</strong>, we noticed you left some items in your cart. Don't miss out on these amazing fragrances!
                                                    </p>

                                                    <!-- Cart Items -->
                                                    <div style="background: #f9fafb; padding: 20px; border-radius: 8px; margin: 20px 0;">
                                                        <h3 style="margin: 0 0 15px 0; color: #111827; font-size: 18px; font-weight: 700;">Your Cart Items:</h3>
                                                        <table role="presentation" style="width: 100%%; border-collapse: collapse;">
                                                            %s
                                                            <tr>
                                                                <td colspan="2" style="padding: 15px 10px 0 10px;">
                                                                    <div style="border-top: 2px solid #e5e7eb;"></div>
                                                                </td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding: 15px 10px 0 10px; color: #111827; font-size: 18px; font-weight: 700;">Total</td>
                                                                <td style="padding: 15px 10px 0 10px; text-align: right; color: #f59e0b; font-size: 24px; font-weight: 800;">₹%.2f</td>
                                                            </tr>
                                                        </table>
                                                    </div>

                                                    <!-- CTA Button -->
                                                    <div style="text-align: center; margin: 30px 0;">
                                                        <a href="http://localhost:3000/cart" style="background: #f59e0b; color: white; padding: 15px 40px; text-decoration: none; border-radius: 6px; font-weight: 700; font-size: 16px; display: inline-block; box-shadow: 0 2px 4px rgba(245, 158, 11, 0.3);">
                                                            Complete Your Purchase
                                                        </a>
                                                    </div>

                                                    <!-- Benefits -->
                                                    <div style="background: #eff6ff; border: 1px solid: #bfdbfe; padding: 20px; border-radius: 6px; margin: 20px 0;">
                                                        <p style="margin: 0 0 10px 0; color: #1e40af; font-size: 14px; font-weight: 600;">Why shop with us?</p>
                                                        <ul style="margin: 0; padding-left: 20px; color: #3b82f6; font-size: 14px;">
                                                            <li>✓ 100%% Authentic Fragrances</li>
                                                            <li>✓ Free Shipping on Orders Above ₹500</li>
                                                            <li>✓ Easy Returns & Exchanges</li>
                                                            <li>✓ Secure Payment Options</li>
                                                        </ul>
                                                    </div>

                                                    <p style="margin: 20px 0 0 0; color: #6b7280; font-size: 14px; text-align: center;">
                                                        Need help? Contact us at <a href="mailto:%s" style="color: #f59e0b; text-decoration: none;">%s</a>
                                                    </p>
                                                </td>
                                            </tr>

                                            <!-- Footer -->
                                            <tr>
                                                <td style="background-color: #1f2937; padding: 30px 40px; text-align: center;">
                                                    <p style="margin: 0 0 10px 0; color: #9ca3af; font-size: 14px;">Thank you for choosing <strong style="color: #f59e0b;">MUWAS</strong></p>
                                                    <p style="margin: 0; color: #6b7280; font-size: 12px;">This is an automated email. Please do not reply.</p>
                                                    <p style="margin: 15px 0 0 0; color: #4b5563; font-size: 11px;">© 2026 MUWAS. All rights reserved.</p>
                                                </td>
                                            </tr>

                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </body>
                        </html>
                        """,
                cart.getUser().getFirstName(),
                items.toString(),
                total,
                fromEmail,
                fromEmail);
    }
}