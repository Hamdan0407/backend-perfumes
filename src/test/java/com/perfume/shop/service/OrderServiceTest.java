package com.perfume.shop.service;

import com.perfume.shop.dto.CheckoutRequest;
import com.perfume.shop.dto.RazorpayOrderRequest;
import com.perfume.shop.dto.RazorpayOrderResponse;
import com.perfume.shop.entity.*;
import com.perfume.shop.repository.CartRepository;
import com.perfume.shop.repository.OrderHistoryRepository;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for OrderService
 * Covers: order creation, payment confirmation, stock management, idempotency, rollback
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderHistoryRepository orderHistoryRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RazorpayService razorpayService;

    @Mock
    private CouponService couponService;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;
    private Order testOrder;
    private CheckoutRequest checkoutRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();
        testUser.setId(1L);

        testProduct = Product.builder()
                .name("Test Perfume")
                .price(new BigDecimal("100.00"))
                .stock(50)
                .active(true)
                .build();
        testProduct.setId(1L);

        testCartItem = CartItem.builder()
                .product(testProduct)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .build();
        testCartItem.setId(1L);

        testCart = Cart.builder()
                .user(testUser)
                .items(new ArrayList<>(Arrays.asList(testCartItem)))
                .build();
        testCart.setId(1L);

        testCartItem.setCart(testCart);

        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setShippingAddress("123 Test St");
        checkoutRequest.setShippingCity("Mumbai");
        checkoutRequest.setShippingCountry("India");
        checkoutRequest.setShippingZipCode("400001");
        checkoutRequest.setShippingPhone("+919876543210");

        testOrder = Order.builder()
                .user(testUser)
                .orderNumber("ORD-20260213-001")
                .subtotal(new BigDecimal("200.00"))
                .tax(new BigDecimal("36.00"))
                .shippingCost(new BigDecimal("10.00"))
                .totalAmount(new BigDecimal("246.00"))
                .status(Order.OrderStatus.PLACED)
                .razorpayOrderId("order_test_123")
                .items(new ArrayList<>())
                .build();
        testOrder.setId(1L);
    }

    // ==================== ORDER CREATION TESTS ====================

    @Test
    @DisplayName("Should create order successfully with valid cart")
    void testCreateOrderSuccess() {
        // Given
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productRepository.findAllByIdWithLock(anyList())).thenReturn(Arrays.asList(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderRepository.countByOrderNumberStartingWith(anyString())).thenReturn(0L);

        RazorpayOrderResponse razorpayResponse = RazorpayOrderResponse.builder()
                .razorpayOrderId("order_test_123")
                .razorpayKeyId("rzp_test_key")
                .amount(24600L)
                .currency("INR")
                .build();

        when(razorpayService.createRazorpayOrder(any(RazorpayOrderRequest.class)))
                .thenReturn(razorpayResponse);

        // When
        RazorpayOrderResponse response = orderService.createOrder(testUser, checkoutRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRazorpayOrderId()).isEqualTo("order_test_123");
        verify(orderRepository, times(2)).save(any(Order.class)); // Once for order, once for razorpay ID
        verify(razorpayService).createRazorpayOrder(any(RazorpayOrderRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when cart is empty")
    void testCreateOrderEmptyCart() {
        // Given
        testCart.getItems().clear();
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, checkoutRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart is empty");
    }

    @Test
    @DisplayName("Should throw exception when cart not found")
    void testCreateOrderCartNotFound() {
        // Given
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, checkoutRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cart not found");
    }

    @Test
    @DisplayName("Should throw exception when product stock insufficient")
    void testCreateOrderInsufficientStock() {
        // Given
        testProduct.setStock(1); // Less than cart quantity (2)
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productRepository.findAllByIdWithLock(anyList())).thenReturn(Arrays.asList(testProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, checkoutRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock validation failed");
    }

    @Test
    @DisplayName("Should throw exception when product is inactive")
    void testCreateOrderInactiveProduct() {
        // Given
        testProduct.setActive(false);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(productRepository.findAllByIdWithLock(anyList())).thenReturn(Arrays.asList(testProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.createOrder(testUser, checkoutRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no longer available");
    }

    // ==================== PAYMENT CONFIRMATION TESTS ====================

    @Test
    @DisplayName("Should confirm payment successfully and deduct stock")
    void testConfirmPaymentSuccess() {
        // Given
        OrderItem orderItem = OrderItem.builder()
                .product(testProduct)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .build();
        orderItem.setId(1L);
        testOrder.getItems().add(orderItem);

        when(orderRepository.findByRazorpayOrderId("order_test_123")).thenReturn(Optional.of(testOrder));
        when(productRepository.findAllByIdWithLock(anyList())).thenReturn(Arrays.asList(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(testCart);
        doNothing().when(emailService).sendOrderConfirmation(any(Order.class));
        doNothing().when(emailService).sendAdminOrderNotification(any(Order.class));

        // When
        Order confirmedOrder = orderService.confirmPayment("order_test_123", "pay_test_456");

        // Then
        assertThat(confirmedOrder).isNotNull();
        assertThat(confirmedOrder.getRazorpayPaymentId()).isEqualTo("pay_test_456");
        verify(productRepository).save(argThat(product -> product.getStock() == 48)); // 50 - 2
        verify(emailService).sendOrderConfirmation(testOrder);
        verify(emailService).sendAdminOrderNotification(testOrder);
    }

    @Test
    @DisplayName("Should handle idempotency - skip if already confirmed")
    void testConfirmPaymentIdempotency() {
        // Given
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        when(orderRepository.findByRazorpayOrderId("order_test_123")).thenReturn(Optional.of(testOrder));

        // When
        Order result = orderService.confirmPayment("order_test_123", "pay_test_456");

        // Then
        assertThat(result.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
        verify(productRepository, never()).save(any(Product.class)); // Stock not deducted again
        verify(emailService, never()).sendOrderConfirmation(any(Order.class));
    }

    @Test
    @DisplayName("Should throw exception when order not found for payment confirmation")
    void testConfirmPaymentOrderNotFound() {
        // Given
        when(orderRepository.findByRazorpayOrderId("invalid_order")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> orderService.confirmPayment("invalid_order", "pay_test_456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should throw exception when payment ID is null")
    void testConfirmPaymentNullPaymentId() {
        // Given
        when(orderRepository.findByRazorpayOrderId("order_test_123")).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.confirmPayment("order_test_123", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("payment ID is required");
    }

    @Test
    @DisplayName("Should throw exception when stock becomes unavailable after payment")
    void testConfirmPaymentStockUnavailable() {
        // Given
        OrderItem orderItem = OrderItem.builder()
                .product(testProduct)
                .quantity(100) // More than available stock
                .price(new BigDecimal("100.00"))
                .build();
        orderItem.setId(1L);
        testOrder.getItems().add(orderItem);

        when(orderRepository.findByRazorpayOrderId("order_test_123")).thenReturn(Optional.of(testOrder));
        when(productRepository.findAllByIdWithLock(anyList())).thenReturn(Arrays.asList(testProduct));

        // When & Then
        assertThatThrownBy(() -> orderService.confirmPayment("order_test_123", "pay_test_456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock validation failed");
    }

    // ==================== ORDER CANCELLATION TESTS ====================

    @Test
    @DisplayName("Should cancel order and restore stock")
    void testCancelOrderSuccess() {
        // Given
        OrderItem orderItem = OrderItem.builder()
                .product(testProduct)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .build();
        orderItem.setId(1L);
        testOrder.getItems().add(orderItem);
        testOrder.setStatus(Order.OrderStatus.PLACED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Order cancelledOrder = orderService.cancelOrder(1L, testUser);

        // Then
        assertThat(cancelledOrder.getStatus()).isEqualTo(Order.OrderStatus.CANCELLED);
        verify(productRepository).save(argThat(product -> product.getStock() == 52)); // 50 + 2 restored
    }

    @Test
    @DisplayName("Should throw exception when cancelling shipped order")
    void testCancelOrderShipped() {
        // Given
        testOrder.setStatus(Order.OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot cancel order in current status");
    }

    // ==================== STATUS UPDATE TESTS ====================

    @Test
    @DisplayName("Should update order status successfully")
    void testUpdateOrderStatus() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(emailService).sendOrderStatusUpdate(any(Order.class));

        // When
        Order updatedOrder = orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        // Then
        assertThat(updatedOrder.getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
        verify(emailService).sendOrderStatusUpdate(testOrder);
    }

    @Test
    @DisplayName("Should restore stock when order is cancelled from placed status")
    void testUpdateOrderStatusRestoreStock() {
        // Given
        OrderItem orderItem = OrderItem.builder()
                .product(testProduct)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .build();
        orderItem.setId(1L);
        testOrder.getItems().add(orderItem);
        testOrder.setStatus(Order.OrderStatus.PLACED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        doNothing().when(emailService).sendOrderStatusUpdate(any(Order.class));

        // When
        orderService.updateOrderStatus(1L, Order.OrderStatus.CANCELLED);

        // Then
        verify(productRepository).save(argThat(product -> product.getStock() == 52)); // Stock restored
    }

    // ==================== GET ORDER TESTS ====================

    @Test
    @DisplayName("Should get order by ID for authorized user")
    void testGetOrderByIdSuccess() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Order order = orderService.getOrderById(1L, testUser);

        // Then
        assertThat(order).isNotNull();
        assertThat(order.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when getting order for unauthorized user")
    void testGetOrderByIdUnauthorized() {
        // Given
        User anotherUser = User.builder().email("another@example.com").build();
        anotherUser.setId(2L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> orderService.getOrderById(1L, anotherUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("Should get user orders")
    void testGetUserOrders() {
        // Given
        when(orderRepository.findByUserOrderByCreatedAtDesc(testUser))
                .thenReturn(Arrays.asList(testOrder));

        // When
        List<Order> orders = orderService.getUserOrders(testUser);

        // Then
        assertThat(orders).isNotNull();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUser().getId()).isEqualTo(testUser.getId());
    }
}
