package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for top product analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductDTO {
    private Long id;
    private String name;
    private String brand;
    private String imageUrl;
    private Long salesCount;        // Number of units sold
    private BigDecimal revenue;     // Total revenue from this product
    private BigDecimal price;       // Current price
    private Integer stock;          // Current stock level
}
