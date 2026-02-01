package com.perfume.shop.service;

import com.perfume.shop.dto.DashboardOverviewResponse;
import com.perfume.shop.dto.OrderDetailsResponse;
import com.perfume.shop.dto.ProductRequest;
import com.perfume.shop.dto.ProductResponse;
import com.perfume.shop.dto.UserResponse;
import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.Product;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.OrderRepository;
import com.perfume.shop.repository.ProductRepository;
import com.perfume.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service for managing admin operations
 * Provides admin dashboard, product management, order management, and user management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {
    
    private final ProductService productService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    // ==========================================
    // DASHBOARD
    // ==========================================
    
    public DashboardOverviewResponse getDashboardOverview() {
        log.info("Generating dashboard overview");
        
        // User metrics
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByActive(true);
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        Long newUsersThisMonth = userRepository.countByCreatedAtAfter(oneMonthAgo);
        
        // Order metrics
        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PLACED);
        Long completedOrders = orderRepository.countByStatus(Order.OrderStatus.DELIVERED);
        
        BigDecimal totalRevenue = calculateTotalRevenue();
        BigDecimal revenueThisMonth = calculateRevenueForPeriod(oneMonthAgo, LocalDateTime.now());
        BigDecimal averageOrderValue = calculateAverageOrderValue();
        
        // Product metrics
        Long totalProducts = productRepository.count();
        Long activeProducts = productRepository.countByActiveTrue();
        Long outOfStockProducts = productRepository.countByStockLessThanEqual(0);
        Long lowStockProducts = productRepository.countByStockLessThanEqual(10) - outOfStockProducts;
        
        // Recent activity
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        Long recentOrdersCount = orderRepository.countByCreatedAtAfter(sevenDaysAgo);
        Long recentRegistrationsCount = userRepository.countByCreatedAtAfter(sevenDaysAgo);
        
        return DashboardOverviewResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .totalRevenue(totalRevenue)
                .revenueThisMonth(revenueThisMonth)
                .averageOrderValue(averageOrderValue)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStockProducts)
                .outOfStockProducts(outOfStockProducts)
                .recentOrdersCount(recentOrdersCount)
                .recentRegistrationsCount(recentRegistrationsCount)
                .build();
    }
    
    // ==========================================
    // PRODUCT MANAGEMENT
    // ==========================================
    
    public Page<ProductResponse> getAllProductsForAdmin(Pageable pageable) {
        log.info("Admin retrieving all products");
        return productService.getAllProductsAdmin(pageable);
    }
    
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Admin creating product: {}", request.getName());
        return productService.createProduct(request);
    }
    
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Admin updating product: {}", id);
        return productService.updateProduct(id, request);
    }
    
    public void deleteProduct(Long id) {
        log.info("Admin deleting product: {}", id);
        productService.deleteProduct(id);
    }
    
    public ProductResponse updateProductStock(Long id, Integer quantity) {
        log.info("Admin updating stock for product {}: {}", id, quantity);
        return productService.updateStock(id, quantity);
    }
    
    public ProductResponse toggleProductFeatured(Long id, Boolean featured) {
        log.info("Admin setting featured for product {}: {}", id, featured);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setFeatured(featured);
        return ProductResponse.fromEntity(productRepository.save(product));
    }
    
    // ==========================================
    // ORDER MANAGEMENT
    // ==========================================
    
    public Page<OrderDetailsResponse> getAllOrders(Pageable pageable) {
        log.info("Admin retrieving all orders");
        Page<Order> orders = orderRepository.findAll(pageable);
        List<OrderDetailsResponse> responses = orders.getContent().stream()
                .map(this::mapOrderToDetailsResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, orders.getTotalElements());
    }
    
    public OrderDetailsResponse getOrderDetails(Long id) {
        log.info("Admin retrieving order: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return mapOrderToDetailsResponse(order);
    }
    
    public OrderDetailsResponse updateOrderStatus(Long id, String statusStr) {
        log.info("Admin updating order {} status to: {}", id, statusStr);
        Order.OrderStatus status = Order.OrderStatus.valueOf(statusStr.toUpperCase());
        Order updated = orderService.updateOrderStatus(id, status);
        return mapOrderToDetailsResponse(updated);
    }
    
    // ==========================================
    // USER MANAGEMENT
    // ==========================================
    
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.info("Admin retrieving all users");
        Page<User> users = userRepository.findAll(pageable);
        List<UserResponse> responses = users.getContent().stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, users.getTotalElements());
    }
    
    public UserResponse getUserDetails(Long id) {
        log.info("Admin retrieving user: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapUserToResponse(user);
    }
    
    public UserResponse updateUserRole(Long id, String roleStr) {
        log.info("Admin updating user {} role to: {}", id, roleStr);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(User.Role.valueOf(roleStr.toUpperCase()));
        return mapUserToResponse(userRepository.save(user));
    }
    
    public UserResponse updateUserStatus(Long id, Boolean active) {
        log.info("Admin updating user {} status to: {}", id, active);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(active);
        return mapUserToResponse(userRepository.save(user));
    }
    
    public UserResponse getAdminProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
        return mapUserToResponse(admin);
    }
    
    // ==========================================
    // HELPER METHODS
    // ==========================================
    
    private BigDecimal calculateTotalRevenue() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateRevenueForPeriod(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    private BigDecimal calculateAverageOrderValue() {
        List<Order> orders = orderRepository.findAll();
        if (orders.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal total = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.divide(BigDecimal.valueOf(orders.size()), 2, java.math.RoundingMode.HALF_UP);
    }
    
    private OrderDetailsResponse mapOrderToDetailsResponse(Order order) {
        return OrderDetailsResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(order.getUser().getId())
                .userEmail(order.getUser().getEmail())
                .userName(order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().toString())
                .shippingAddress(order.getShippingAddress())
                .trackingNumber(order.getTrackingNumber())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    private UserResponse mapUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .city(user.getCity())
                .country(user.getCountry())
                .zipCode(user.getZipCode())
                .role(user.getRole().toString())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
