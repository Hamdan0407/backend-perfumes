package com.perfume.shop.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_number", columnList = "orderNumber"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_payment_intent", columnList = "paymentIntentId"),
        @Index(name = "idx_order_razorpay_id", columnList = "razorpayOrderId"),
        @Index(name = "idx_order_created", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = { "items" })
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private User user;

    @Column(length = 100)
    private String customerName;

    @Column(length = 150)
    private String customerEmail;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal tax;

    @Column(nullable = false)
    private BigDecimal shippingCost;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(length = 50)
    private String couponCode;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private String shippingAddress;

    @Column(length = 100)
    private String shippingRecipientName;

    @Column(nullable = false)
    private String shippingCity;

    @Column(nullable = false)
    private String shippingCountry;

    @Column(nullable = false)
    private String shippingZipCode;

    private String shippingPhone;

    @Column(nullable = false, length = 50)
    private String paymentMethod;

    @Column(length = 100)
    private String paymentIntentId;

    @Column(length = 100)
    private String razorpayOrderId; // Razorpay Order ID for webhook handling

    @Column(length = 100)
    private String razorpayPaymentId; // Razorpay Payment ID after successful payment

    @Column(length = 100)
    private String trackingNumber;

    public enum OrderStatus {
        PLACED, // Order created after payment confirmation
        CONFIRMED, // Admin confirmed the order
        PACKED, // Order packed and ready
        HANDOVER, // Package handed over to courier partner
        SHIPPED, // Order shipped
        OUT_FOR_DELIVERY, // Order is out for delivery
        DELIVERED, // Order delivered to customer
        CANCELLED, // Order cancelled
        REFUNDED // Refund processed
    }
}
