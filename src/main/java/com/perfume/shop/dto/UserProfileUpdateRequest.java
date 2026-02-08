package com.perfume.shop.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user profile information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileUpdateRequest {
    
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{7,20}$", message = "Please enter a valid phone number")
    private String phoneNumber;
    
    @NotBlank(message = "Address is required")
    @Size(min = 10, max = 255, message = "Address must be between 10 and 255 characters")
    private String address;
    
    @NotBlank(message = "City is required")
    @Size(min = 2, max = 50, message = "City must be between 2 and 50 characters")
    private String city;
    
    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 50, message = "Country must be between 2 and 50 characters")
    private String country;
    
    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^[0-9]{5,6}$", message = "Zip code must be 5-6 digits")
    private String zipCode;
}
