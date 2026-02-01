package com.perfume.shop.service;

import com.perfume.shop.dto.AuthResponse;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.UserRepository;
import com.perfume.shop.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.client-secret:}")
    private String googleClientSecret;

    @Value("${facebook.app-secret:}")
    private String facebookAppSecret;

    @Value("${microsoft.client-secret:}")
    private String microsoftClientSecret;

    /**
     * Authenticate with Google OAuth2 token
     */
    public AuthResponse authenticateWithGoogle(String idToken) {
        try {
            log.info("Authenticating with Google token");
            
            // Validate token with Google
            String googleTokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
            ResponseEntity<String> response = restTemplate.getForEntity(googleTokenInfoUrl, String.class);
            
            JsonNode tokenInfo = objectMapper.readTree(response.getBody());
            String email = tokenInfo.get("email").asText();
            String name = tokenInfo.get("name").asText();
            String googleId = tokenInfo.get("sub").asText();
            String picture = tokenInfo.get("picture") != null ? tokenInfo.get("picture").asText() : null;

            return findOrCreateOAuthUser(email, name, "google", googleId, picture);
        } catch (Exception e) {
            log.error("Google authentication failed", e);
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate with Facebook OAuth2 token
     */
    public AuthResponse authenticateWithFacebook(String accessToken) {
        try {
            log.info("Authenticating with Facebook token");
            
            // Get user info from Facebook
            String facebookGraphUrl = "https://graph.facebook.com/me?fields=id,email,name,picture&access_token=" + accessToken;
            ResponseEntity<String> response = restTemplate.getForEntity(facebookGraphUrl, String.class);
            
            JsonNode userInfo = objectMapper.readTree(response.getBody());
            String email = userInfo.get("email").asText();
            String name = userInfo.get("name").asText();
            String facebookId = userInfo.get("id").asText();
            String picture = userInfo.get("picture") != null && userInfo.get("picture").get("data") != null 
                ? userInfo.get("picture").get("data").get("url").asText() : null;

            return findOrCreateOAuthUser(email, name, "facebook", facebookId, picture);
        } catch (Exception e) {
            log.error("Facebook authentication failed", e);
            throw new RuntimeException("Facebook authentication failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate with Microsoft OAuth2 token
     */
    public AuthResponse authenticateWithMicrosoft(String accessToken) {
        try {
            log.info("Authenticating with Microsoft token");
            
            // Get user info from Microsoft Graph
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.getForEntity(
                "https://graph.microsoft.com/v1.0/me",
                String.class
            );
            
            JsonNode userInfo = objectMapper.readTree(response.getBody());
            String email = userInfo.get("userPrincipalName").asText();
            String name = userInfo.get("displayName").asText();
            String microsoftId = userInfo.get("id").asText();

            return findOrCreateOAuthUser(email, name, "microsoft", microsoftId, null);
        } catch (Exception e) {
            log.error("Microsoft authentication failed", e);
            throw new RuntimeException("Microsoft authentication failed: " + e.getMessage());
        }
    }

    /**
     * Find existing OAuth user or create new one
     */
    private AuthResponse findOrCreateOAuthUser(String email, String fullName, 
                                                String provider, String providerId, String profilePicture) {
        try {
            // Check if user exists by email
            Optional<User> existingUser = userRepository.findByEmail(email);
            
            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Update OAuth info if not set
                if (user.getOauthProvider() == null) {
                    user.setOauthProvider(provider);
                    user.setOauthId(providerId);
                }
                if (profilePicture != null && user.getProfilePictureUrl() == null) {
                    user.setProfilePictureUrl(profilePicture);
                }
            } else {
                // Create new user
                String[] nameParts = fullName.split(" ", 2);
                String firstName = nameParts[0];
                String lastName = nameParts.length > 1 ? nameParts[1] : "";

                user = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .oauthProvider(provider)
                    .oauthId(providerId)
                    .profilePictureUrl(profilePicture)
                    .password("") // No password for OAuth users
                    .role(User.Role.CUSTOMER)
                    .active(true)
                    .build();
            }

            user = userRepository.save(user);
            log.info("User authenticated via {}: {}", provider, email);

            // Generate JWT token
            String jwtToken = jwtService.generateToken(user);

            return AuthResponse.builder()
                .token(jwtToken)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().toString())
                .type("Bearer")
                .expiresIn(3600L) // 1 hour
                .build();
        } catch (Exception e) {
            log.error("Error finding or creating OAuth user", e);
            throw new RuntimeException("Failed to process OAuth user: " + e.getMessage());
        }
    }
}
