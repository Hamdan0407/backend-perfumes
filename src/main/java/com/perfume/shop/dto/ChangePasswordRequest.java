package com.perfume.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for changing user password
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {
    
    @NotBlank(message = "Current password is required")
    private String currentPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 128, message = "Password must be between 6 and 128 characters")
    private String newPassword;
    
    @NotBlank(message = "Please confirm your new password")
    private String confirmPassword;
}
