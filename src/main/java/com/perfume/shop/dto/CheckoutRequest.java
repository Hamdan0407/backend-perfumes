package com.perfume.shop.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    
    @NotBlank(message = "Recipient name is required")
    private String recipientName;
    
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;
    
    @NotBlank(message = "City is required")
    private String shippingCity;
    
    @NotBlank(message = "Country is required")
    private String shippingCountry;
    
    @NotBlank(message = "Zip code is required")
    private String shippingZipCode;
    
    private String shippingPhone;
    
    private String couponCode;
}
