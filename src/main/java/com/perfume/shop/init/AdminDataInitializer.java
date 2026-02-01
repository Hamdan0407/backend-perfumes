package com.perfume.shop.init;

import com.perfume.shop.entity.User;
import com.perfume.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes default admin user on application startup
 * 
 * Default Admin Credentials (DEMO MODE):
 * Email: admin@perfumeshop.local
 * Password: admin123456
 * 
 * IMPORTANT: Change password in production!
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        try {
            // Check if admin user already exists
            String adminEmail = "admin@perfumeshop.local";
            
            if (userRepository.existsByEmail(adminEmail)) {
                log.info("Default admin user already exists");
                return;
            }
            
            // Create default admin user
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode("admin123456"))
                    .firstName("Admin")
                    .lastName("User")
                    .phoneNumber("+1-555-0000")
                    .address("123 Admin Street")
                    .city("Admin City")
                    .country("Admin Country")
                    .zipCode("00000")
                    .role(User.Role.ADMIN)
                    .active(true)
                    .build();
            
            userRepository.save(admin);
            log.info("Default admin user created successfully");
            log.info("Admin Email: {}", adminEmail);
            log.info("⚠️  IMPORTANT: Change default admin password in production!");
            
        } catch (Exception e) {
            log.error("Error initializing admin data", e);
        }
    }
}
