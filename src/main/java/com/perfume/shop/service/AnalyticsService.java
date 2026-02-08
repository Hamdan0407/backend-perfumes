package com.perfume.shop.service;

import com.perfume.shop.dto.AnalyticsDataPoint;
import com.perfume.shop.dto.TopProductDTO;
import com.perfume.shop.repository.OrderItemRepository;
import com.perfume.shop.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for analytics and dashboard metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    
    /**
     * Get daily sales data for the last N days
     */
    public List<AnalyticsDataPoint> getDailySalesData(int days) {
        log.info("Fetching daily sales data for last {} days", days);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> rawData = orderRepository.findDailySalesData(startDate);
        
        List<AnalyticsDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        
        for (Object[] row : rawData) {
            LocalDate date;
            if (row[0] instanceof java.sql.Date) {
                date = ((java.sql.Date) row[0]).toLocalDate();
            } else if (row[0] instanceof LocalDate) {
                date = (LocalDate) row[0];
            } else {
                date = LocalDate.parse(row[0].toString());
            }
            Long orderCount = ((Number) row[1]).longValue();
            BigDecimal revenue = (BigDecimal) row[2];
            
            dataPoints.add(AnalyticsDataPoint.builder()
                    .label(date.format(formatter))
                    .date(date)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .build());
        }
        
        log.info("Retrieved {} daily data points", dataPoints.size());
        return dataPoints;
    }
    
    /**
     * Get monthly sales data for the last N months
     */
    public List<AnalyticsDataPoint> getMonthlySalesData(int months) {
        log.info("Fetching monthly sales data for last {} months", months);
        
        LocalDateTime startDate = LocalDateTime.now().minusMonths(months);
        List<Object[]> rawData = orderRepository.findMonthlySalesData(startDate);
        
        List<AnalyticsDataPoint> dataPoints = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");
        
        for (Object[] row : rawData) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            Long orderCount = ((Number) row[2]).longValue();
            BigDecimal revenue = (BigDecimal) row[3];
            
            LocalDate date = LocalDate.of(year, month, 1);
            
            dataPoints.add(AnalyticsDataPoint.builder()
                    .label(date.format(formatter))
                    .date(date)
                    .orderCount(orderCount)
                    .revenue(revenue)
                    .build());
        }
        
        log.info("Retrieved {} monthly data points", dataPoints.size());
        return dataPoints;
    }
    
    /**
     * Get top selling products for a given time period
     */
    public List<TopProductDTO> getTopSellingProducts(int limit, int daysBack) {
        log.info("Fetching top {} selling products from last {} days", limit, daysBack);
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        List<Object[]> rawData = orderItemRepository.findTopSellingProducts(
                startDate, 
                PageRequest.of(0, limit)
        );
        
        List<TopProductDTO> topProducts = new ArrayList<>();
        
        for (Object[] row : rawData) {
            Long productId = ((Number) row[0]).longValue();
            String productName = (String) row[1];
            String brand = (String) row[2];
            String imageUrl = (String) row[3];
            BigDecimal price = (BigDecimal) row[4];
            Integer stock = (Integer) row[5];
            Long salesCount = ((Number) row[6]).longValue();
            BigDecimal revenue = (BigDecimal) row[7];
            
            topProducts.add(TopProductDTO.builder()
                    .id(productId)
                    .name(productName)
                    .brand(brand)
                    .imageUrl(imageUrl)
                    .price(price)
                    .stock(stock)
                    .salesCount(salesCount)
                    .revenue(revenue)
                    .build());
        }
        
        log.info("Retrieved {} top products", topProducts.size());
        return topProducts;
    }
    
    /**
     * Get revenue summary for different time periods
     */
    public BigDecimal getRevenueForPeriod(int daysBack) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(daysBack);
        List<Object[]> data = orderRepository.findDailySalesData(startDate);
        
        return data.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
