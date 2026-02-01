package com.perfume.shop.repository;

import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Optional<Order> findByPaymentIntentId(String paymentIntentId);
    
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);
    
    Long countByStatus(Order.OrderStatus status);
    
    // Admin methods
    Long countByCreatedAtAfter(LocalDateTime date);
    
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
