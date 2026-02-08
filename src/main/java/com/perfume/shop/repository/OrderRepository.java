package com.perfume.shop.repository;

import com.perfume.shop.entity.Order;
import com.perfume.shop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

       @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END " +
                     "FROM OrderItem oi " +
                     "WHERE oi.order.user.id = :userId " +
                     "AND oi.product.id = :productId")
       boolean hasUserPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

       // Count orders with a specific prefix (for sequential order number generation)
       Long countByOrderNumberStartingWith(String prefix);

       Long countByStatus(Order.OrderStatus status);

       // Admin methods
       Long countByCreatedAtAfter(LocalDateTime date);

       List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

       // Analytics methods
       @Query("SELECT CAST(o.createdAt AS date) as orderDate, " +
                     "COUNT(o) as orderCount, " +
                     "SUM(o.totalAmount) as revenue " +
                     "FROM Order o " +
                     "WHERE o.createdAt >= :startDate " +
                     "GROUP BY CAST(o.createdAt AS date) " +
                     "ORDER BY orderDate")
       List<Object[]> findDailySalesData(@Param("startDate") LocalDateTime startDate);

       @Query("SELECT YEAR(o.createdAt) as year, " +
                     "MONTH(o.createdAt) as month, " +
                     "COUNT(o) as orderCount, " +
                     "SUM(o.totalAmount) as revenue " +
                     "FROM Order o " +
                     "WHERE o.createdAt >= :startDate " +
                     "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
                     "ORDER BY year, month")
       List<Object[]> findMonthlySalesData(@Param("startDate") LocalDateTime startDate);
}
