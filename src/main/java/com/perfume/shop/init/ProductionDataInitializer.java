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
@Profile({ "production", "prod" })
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
        try {
            log.info("Production data initializer starting...");
            createAdminUser();
            log.info("Production data initialization completed");
        } catch (Exception e) {
            log.error(
                    "CRITICAL: Failed to initialize production admin data. The application will continue to start, but admin access may be affected.",
                    e);
        }
    }

    private void createAdminUser() {
        User adminUser = userRepository.findByEmail(adminEmail).orElse(null);

        if (adminUser != null) {
            log.info("Admin user found, ensuring credentials are up to date: {}", adminEmail);
            adminUser.setPassword(passwordEncoder.encode(adminPassword));
            adminUser.setRole(User.Role.ADMIN);
            adminUser.setActive(true);
            userRepository.save(adminUser);
            log.info("✅ Admin credentials synchronized for: {}", adminEmail);
            return;
        }

        // Validate admin password strength (logging only)
        if (isWeakPassword(adminPassword)) {
            log.warn("⚠️ Admin password is considered weak. Security improvement recommended.");
        }

        // Create new admin user
        adminUser = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .firstName("Mohammed")
                .lastName("Hamdaan")
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        userRepository.save(adminUser);
        log.info("✨ Created new production admin user: {}", adminEmail);
    }

    private boolean isWeakPassword(String password) {
        return password == null ||
                password.length() < 10 ||
                password.equals("CHANGE_ME_SECURE_PASSWORD") ||
                password.equals("password") ||
                password.equals("123456") ||
                !password.matches(".*[a-z].*") || // No lowercase
                !password.matches(".*[0-9].*"); // No numbers
    }
}