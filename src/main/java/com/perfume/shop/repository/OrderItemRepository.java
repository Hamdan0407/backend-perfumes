package com.perfume.shop.repository;

import com.perfume.shop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    // Analytics: Top selling products
    @Query("SELECT oi.product.id as productId, " +
           "oi.product.name as productName, " +
           "oi.product.brand as brand, " +
           "oi.product.imageUrl as imageUrl, " +
           "oi.product.price as price, " +
           "oi.product.stock as stock, " +
           "SUM(oi.quantity) as totalQuantity, " +
           "SUM(oi.quantity * oi.price) as totalRevenue " +
           "FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate " +
           "GROUP BY oi.product.id, oi.product.name, oi.product.brand, " +
           "oi.product.imageUrl, oi.product.price, oi.product.stock " +
           "ORDER BY totalQuantity DESC")
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate, 
                                          org.springframework.data.domain.Pageable pageable);
}
