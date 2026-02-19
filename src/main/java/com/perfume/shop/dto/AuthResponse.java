package com.perfume.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 * Returns user info, access token, refresh token, and token expiration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token; // Access token (JWT)

    private String refreshToken; // Refresh token (longer expiration)

    @Builder.Default
    private String type = "Bearer"; // Token type

    private Long expiresIn; // Expiration time in seconds

    private Long id; // User ID

    private String email; // User email

    private String firstName; // User first name

    private String lastName; // User last name

    private String role; // User role (USER, ADMIN)
}
