package com.perfume.shop.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration
 * Provides interactive API documentation at /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.name:Perfume Shop API}")
    private String appName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .version(appVersion)
                        .description("Production-ready perfume e-commerce platform API. " +
                                "Supports user authentication, product management, shopping cart, " +
                                "order processing, payment integration (Razorpay), and admin operations.")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@perfumeshop.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.perfumeshop.com")
                                .description("Production Server")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication token. Obtain from /api/auth/login or /api/auth/register")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
