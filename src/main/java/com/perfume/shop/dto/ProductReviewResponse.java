package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReviewResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String userName;
    private Integer rating;
    private String title;
    private String comment;
    private Boolean verified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
