package com.perfume.shop.service;

import com.perfume.shop.dto.AuthResponse;
import com.perfume.shop.dto.LoginRequest;
import com.perfume.shop.dto.RegisterRequest;
import com.perfume.shop.entity.Cart;
import com.perfume.shop.entity.User;
import com.perfume.shop.exception.AuthenticationException;
import com.perfume.shop.exception.EmailAlreadyExistsException;
import com.perfume.shop.exception.PasswordPolicyException;
import com.perfume.shop.exception.UserNotFoundException;
import com.perfume.shop.repository.CartRepository;
import com.perfume.shop.repository.UserRepository;
import com.perfume.shop.security.JwtService;
import com.perfume.shop.security.PasswordPolicyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService
 * 
 * Tests cover:
 * - User registration (success and failure scenarios)
 * - User login (success and failure scenarios)
 * - Token refresh
 * - Password reset flow
 * - Edge cases and validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectProvider<AuthenticationManager> authenticationManagerProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordPolicyValidator passwordPolicyValidator;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword123")
                .firstName("John")
                .lastName("Doe")
                .role(User.Role.CUSTOMER)
                .active(true)
                .build();
        testUser.setId(1L);

        // Setup register request
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("SecurePass123!");
        registerRequest.setFirstName("Jane");
        registerRequest.setLastName("Smith");
        registerRequest.setPhoneNumber("1234567890");

        // Setup login request
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Mock authentication manager provider
        when(authenticationManagerProvider.getObject()).thenReturn(authenticationManager);
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("Should successfully register new user")
    void testRegisterSuccess() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(jwtService.getTimeUntilExpiration(anyString())).thenReturn(3600000L);
        doNothing().when(passwordPolicyValidator).validate(anyString());

        // When
        AuthResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(passwordPolicyValidator).validate(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email is taken")
    void testRegisterEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw PasswordPolicyException when password is weak")
    void testRegisterWeakPassword() {
        // Given
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        doThrow(new PasswordPolicyValidator.PasswordPolicyException("Password too weak"))
                .when(passwordPolicyValidator).validate(registerRequest.getPassword());

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(PasswordPolicyException.class);
        
        verify(passwordPolicyValidator).validate(registerRequest.getPassword());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when first name is blank")
    void testRegisterBlankFirstName() {
        // Given
        registerRequest.setFirstName("");
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        doNothing().when(passwordPolicyValidator).validate(anyString());

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("First name is required");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when last name is blank")
    void testRegisterBlankLastName() {
        // Given
        registerRequest.setLastName("   ");
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        doNothing().when(passwordPolicyValidator).validate(anyString());

        // When & Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Last name is required");
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should successfully login user")
    void testLoginSuccess() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");
        when(jwtService.getTimeUntilExpiration(anyString())).thenReturn(3600000L);
        doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When
        AuthResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail(loginRequest.getEmail());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when credentials are invalid")
    void testLoginInvalidCredentials() {
        // Given
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class);
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user does not exist")
    void testLoginUserNotFound() {
        // Given
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());
        doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw AuthenticationException when account is disabled")
    void testLoginDisabledAccount() {
        // Given
        testUser.setActive(false);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        doNothing().when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // When & Then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("disabled");
    }

    // ==================== TOKEN REFRESH TESTS ====================

    @Test
    @DisplayName("Should successfully refresh access token")
    void testRefreshTokenSuccess() {
        // Given
        String refreshToken = "valid-refresh-token";
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(true);
        when(jwtService.generateToken(testUser)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");
        when(jwtService.getTimeUntilExpiration(anyString())).thenReturn(3600000L);

        // When
        AuthResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        
        verify(jwtService).isRefreshToken(refreshToken);
        verify(jwtService).extractUsername(refreshToken);
        verify(jwtService).isTokenValid(refreshToken, testUser);
    }

    @Test
    @DisplayName("Should throw AuthenticationException when using access token for refresh")
    void testRefreshTokenWithAccessToken() {
        // Given
        String accessToken = "access-token";
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(accessToken))
                .isInstanceOf(AuthenticationException.class);
        
        verify(jwtService).isRefreshToken(accessToken);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    @DisplayName("Should throw AuthenticationException when refresh token is expired")
    void testRefreshTokenExpired() {
        // Given
        String refreshToken = "expired-refresh-token";
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.extractUsername(refreshToken)).thenReturn(testUser.getEmail());
        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtService.isTokenValid(refreshToken, testUser)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(AuthenticationException.class);
    }

    // ==================== PASSWORD RESET TESTS ====================

    @Test
    @DisplayName("Should initiate password reset for existing user")
    void testInitiatePasswordResetSuccess() throws Exception {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        authService.initiatePasswordReset(email);

        // Then
        verify(userRepository).findByEmail(email);
        verify(userRepository).save(argThat(user -> 
            user.getResetToken() != null && user.getResetTokenExpiry() != null
        ));
        verify(emailService).sendEmail(eq(email), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not reveal user existence when email not found")
    void testInitiatePasswordResetNonExistentUser() throws Exception {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        authService.initiatePasswordReset(email);

        // Then
        verify(userRepository).findByEmail(email);
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reset password with valid token")
    void testResetPasswordSuccess() {
        // Given
        String token = "valid-reset-token";
        String newPassword = "NewSecurePass123!";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiry(Instant.now().plusSeconds(1800));
        
        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        doNothing().when(passwordPolicyValidator).validate(newPassword);

        // When
        authService.resetPassword(token, newPassword);

        // Then
        verify(userRepository).findByResetToken(token);
        verify(passwordPolicyValidator).validate(newPassword);
        verify(userRepository).save(argThat(user -> 
            user.getResetToken() == null && user.getResetTokenExpiry() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when reset token is invalid")
    void testResetPasswordInvalidToken() {
        // Given
        String token = "invalid-token";
        when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(token, "NewPass123!"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw exception when reset token is expired")
    void testResetPasswordExpiredToken() {
        // Given
        String token = "expired-token";
        testUser.setResetToken(token);
        testUser.setResetTokenExpiry(Instant.now().minusSeconds(1800)); // Expired
        
        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.resetPassword(token, "NewPass123!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    // ==================== ROLE UPDATE TESTS ====================

    @Test
    @DisplayName("Should successfully update user role")
    void testUpdateUserRoleSuccess() {
        // Given
        String email = "test@example.com";
        String newRole = "ADMIN";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        var result = authService.updateUserRole(email, newRole);

        // Then
        assertThat(result).containsEntry("email", email);
        assertThat(result).containsEntry("role", newRole);
        verify(userRepository).save(argThat(user -> user.getRole() == User.Role.ADMIN));
    }

    @Test
    @DisplayName("Should throw exception when updating to invalid role")
    void testUpdateUserRoleInvalid() {
        // Given
        String email = "test@example.com";
        String invalidRole = "SUPERUSER";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> authService.updateUserRole(email, invalidRole))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid role");
    }
}
