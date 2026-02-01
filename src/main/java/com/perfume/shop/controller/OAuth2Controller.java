package com.perfume.shop.controller;

import com.perfume.shop.dto.AuthResponse;
import com.perfume.shop.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    /**
     * Handle Google OAuth2 token
     */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> authenticateGoogle(@RequestParam String token) {
        log.info("Google OAuth2 authentication attempt");
        AuthResponse response = oAuth2Service.authenticateWithGoogle(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle Facebook OAuth2 token
     */
    @PostMapping("/facebook")
    public ResponseEntity<AuthResponse> authenticateFacebook(@RequestParam String token) {
        log.info("Facebook OAuth2 authentication attempt");
        AuthResponse response = oAuth2Service.authenticateWithFacebook(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Handle Microsoft OAuth2 token
     */
    @PostMapping("/microsoft")
    public ResponseEntity<AuthResponse> authenticateMicrosoft(@RequestParam String token) {
        log.info("Microsoft OAuth2 authentication attempt");
        AuthResponse response = oAuth2Service.authenticateWithMicrosoft(token);
        return ResponseEntity.ok(response);
    }
}
