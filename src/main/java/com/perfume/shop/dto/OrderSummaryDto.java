package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryDto {
    private Long id;
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private Integer itemCount;
}
