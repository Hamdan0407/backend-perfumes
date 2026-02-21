package com.perfume.shop.init;

import com.perfume.shop.entity.User;
import com.perfume.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Production Data Initializer
 * Creates essential admin user for production environment
 * NO demo data, NO sample products
 */
@Component
@Profile("production")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ProductionDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@perfumeshop.local}")
    private String adminEmail;

    @Value("${app.admin.password:admin123456}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        log.info("Production data initializer starting...");

        createAdminUser();

        log.info("Production data initialization completed");
    }

    private void createAdminUser() {
        // Check if admin user already exists
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        // Validate admin password strength
        if (isWeakPassword(adminPassword)) {
            log.error("SECURITY WARNING: Admin password is weak! Please change it immediately.");
            log.error("Current password appears to be a default placeholder.");
        }

        // Create admin user
        User adminUser = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .firstName("Mohammed")
                .lastName("Hamdaan")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(adminUser);

        log.info("✅ Created production admin user: {}", adminEmail);
        log.info("🔒 IMPORTANT: Change the default admin password immediately after first login!");
    }

    private boolean isWeakPassword(String password) {
        return password == null ||
                password.length() < 12 ||
                password.equals("CHANGE_ME_SECURE_PASSWORD") ||
                password.equals("password") ||
                password.equals("123456") ||
                !password.matches(".*[a-z].*") || // No lowercase
                !password.matches(".*[0-9].*"); // No numbers
    }
}