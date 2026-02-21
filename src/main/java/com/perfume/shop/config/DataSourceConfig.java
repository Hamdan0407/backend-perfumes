package com.perfume.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
@Slf4j
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("--- PRODUCTION DATASOURCE INITIALIZATION ---");

        String databaseUrl = System.getenv("DATABASE_URL");
        String springUrl = System.getenv("SPRING_DATASOURCE_URL");
        String jdbcUrlEnv = System.getenv("JDBC_DATABASE_URL");

        String finalUrl = "";
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");

        // 1. Prioritize explicit SPRING_DATASOURCE_URL if it looks like a JDBC URL
        if (springUrl != null && springUrl.startsWith("jdbc:postgresql://")) {
            log.info("Using SPRING_DATASOURCE_URL from environment.");
            finalUrl = springUrl;
        }
        // 2. Then try JDBC_DATABASE_URL
        else if (jdbcUrlEnv != null && jdbcUrlEnv.startsWith("jdbc:postgresql://")) {
            log.info("Using JDBC_DATABASE_URL from environment.");
            finalUrl = jdbcUrlEnv;
        }
        // 3. Then parse DATABASE_URL (Railway format)
        else if (databaseUrl != null
                && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                log.info("Converting DATABASE_URL to JDBC format...");
                URI dbUri = new URI(databaseUrl);
                String userInfo = dbUri.getUserInfo();

                if (userInfo != null && userInfo.contains(":")) {
                    username = userInfo.split(":")[0];
                    password = userInfo.split(":")[1];
                }

                finalUrl = String.format("jdbc:postgresql://%s:%d%s",
                        dbUri.getHost(),
                        dbUri.getPort() == -1 ? 5432 : dbUri.getPort(),
                        dbUri.getPath());

            } catch (URISyntaxException e) {
                log.error("Failed to parse DATABASE_URL", e);
            }
        }
        // 4. Fallback to constructing from components
        else {
            String host = System.getenv("PGHOST");
            String port = System.getenv("PGPORT");
            String db = System.getenv("PGDATABASE");
            username = (username == null) ? System.getenv("PGUSER") : username;
            password = (password == null) ? System.getenv("PGPASSWORD") : password;

            if (host != null && db != null) {
                log.info("Constructing JDBC URL from host/port/db variables.");
                finalUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port != null ? port : "5432", db);
            }
        }

        // Ensure sslmode=require if it's a postgresql connection
        if (!finalUrl.isEmpty() && !finalUrl.contains("sslmode=")) {
            finalUrl += (finalUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        if (finalUrl.isEmpty()) {
            log.error("CRITICAL: No valid database configuration found! HikariCP will likely fail.");
            // Fallback to a default just to avoid immediate null crash, though it will
            // still fail connection
            finalUrl = "jdbc:postgresql://localhost:5432/perfume_shop";
        }

        log.info("Resolved JDBC URL: {}", finalUrl.replaceAll(":([^/@]+)@", ":****@")); // Mask password if present in
                                                                                        // URL

        return DataSourceBuilder.create()
                .url(finalUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
