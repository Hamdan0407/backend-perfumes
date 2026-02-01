package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Dashboard overview metrics for admin dashboard
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardOverviewResponse {
    
    // User metrics
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersThisMonth;
    
    // Order metrics
    private Long totalOrders;
    private Long pendingOrders;
    private Long completedOrders;
    private BigDecimal totalRevenue;
    private BigDecimal revenueThisMonth;
    private BigDecimal averageOrderValue;
    
    // Product metrics
    private Long totalProducts;
    private Long activeProducts;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    
    // Recent activity
    private Long recentOrdersCount;  // Last 7 days
    private Long recentRegistrationsCount;  // Last 7 days
}
