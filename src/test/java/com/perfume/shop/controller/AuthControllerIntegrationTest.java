package com.perfume.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.perfume.shop.dto.LoginRequest;
import com.perfume.shop.dto.RegisterRequest;
import com.perfume.shop.entity.User;
import com.perfume.shop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests the full authentication flow with real Spring context
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        userRepository.deleteAll();
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterSuccess() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("StrongPass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setPhoneNumber("+919876543210");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should reject registration with existing email")
    void testRegisterDuplicateEmail() throws Exception {
        // Given - Create existing user
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setEmail("existing@example.com");
        firstRequest.setPassword("StrongPass123!");
        firstRequest.setFirstName("Existing");
        firstRequest.setLastName("User");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        // When - Try to register with same email
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setEmail("existing@example.com");
        duplicateRequest.setPassword("AnotherPass123!");
        duplicateRequest.setFirstName("Another");
        duplicateRequest.setLastName("User");

        // Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with weak password")
    void testRegisterWeakPassword() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("weak");
        request.setFirstName("Test");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLoginSuccess() throws Exception {
        // Given - Register a user first
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("login@example.com");
        registerRequest.setPassword("ValidPass123!");
        registerRequest.setFirstName("Login");
        registerRequest.setLastName("Test");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When - Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@example.com");
        loginRequest.setPassword("ValidPass123!");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("Should reject login with invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        // Given - Register a user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("user@example.com");
        registerRequest.setPassword("CorrectPass123!");
        registerRequest.setFirstName("User");
        registerRequest.setLastName("Test");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        // When - Try to login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("WrongPassword123!");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should reject login for non-existent user")
    void testLoginUserNotFound() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@example.com");
        loginRequest.setPassword("SomePass123!");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== TOKEN REFRESH TESTS ====================

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() throws Exception {
        // Given - Register and get refresh token
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("refresh@example.com");
        registerRequest.setPassword("ValidPass123!");
        registerRequest.setFirstName("Refresh");
        registerRequest.setLastName("Test");

        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @DisplayName("Should reject registration with missing fields")
    void testRegisterMissingFields() throws Exception {
        // Given - Request with missing required fields
        RegisterRequest request = new RegisterRequest();
        request.setEmail("incomplete@example.com");
        // Missing password, firstName, lastName

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject registration with invalid email format")
    void testRegisterInvalidEmail() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("ValidPass123!");
        request.setFirstName("Test");
        request.setLastName("User");

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
