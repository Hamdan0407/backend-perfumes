package com.perfume.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.perfume.shop.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.perfume.shop.repository")
public class PerfumeShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(PerfumeShopApplication.class, args);
    }

    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner diagnosticRunner(
            jakarta.persistence.EntityManager entityManager) {
        return args -> {
            System.out.println("========== JPA DIAGNOSTIC START ==========");
            System.out.println("Entities detected by EntityManager:");
            try {
                entityManager.getMetamodel().getEntities().forEach(entity -> System.out
                        .println(" - Detected: " + entity.getName() + " [" + entity.getJavaType().getName() + "]"));
            } catch (Exception e) {
                System.out.println("Error scanning entities: " + e.getMessage());
            }
            System.out.println("========== JPA DIAGNOSTIC END ==========");
        };
    }
}
