package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for analytics chart data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDataPoint {
    private String label;           // e.g., "2024-01", "Jan", "Week 1"
    private LocalDate date;         // Actual date for sorting
    private Long orderCount;        // Number of orders
    private BigDecimal revenue;     // Total revenue
    private BigDecimal value;       // Generic value field for flexibility
}
