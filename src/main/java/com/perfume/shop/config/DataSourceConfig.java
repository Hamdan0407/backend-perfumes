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
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            databaseUrl = System.getenv("SPRING_DATASOURCE_URL");
        }

        log.info("Initializing production DataSource...");

        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                log.info("Detected Railway-style DATABASE_URL. Converting to JDBC...");

                URI dbUri = new URI(databaseUrl);
                String userInfo = dbUri.getUserInfo();

                String username = "";
                String password = "";

                if (userInfo != null && userInfo.contains(":")) {
                    username = userInfo.split(":")[0];
                    password = userInfo.split(":")[1];
                } else if (userInfo != null) {
                    username = userInfo;
                }

                String host = dbUri.getHost();
                int port = dbUri.getPort();
                String path = dbUri.getPath();

                // Construct JDBC URL: jdbc:postgresql://host:port/path?sslmode=require
                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port == -1 ? 5432 : port)
                        .append(path);

                if (!path.contains("?")) {
                    jdbcUrl.append("?sslmode=require");
                } else if (!path.contains("sslmode")) {
                    jdbcUrl.append("&sslmode=require");
                }

                log.info("Final JDBC URL: {}", jdbcUrl.toString());

                return DataSourceBuilder.create()
                        .url(jdbcUrl.toString())
                        .username(username)
                        .password(password)
                        .driverClassName("org.postgresql.Driver")
                        .build();

            } catch (URISyntaxException e) {
                log.error("Failed to parse DATABASE_URL: {}. Falling back to default configuration.", databaseUrl, e);
            }
        } else {
            log.info("No DATABASE_URL found or it's already in JDBC format. Letting Spring handle it.");
        }

        // Return null to let the default Spring Boot DataSource auto-configuration take
        // over
        // OR we can explicitly build one using common environment variables
        return DataSourceBuilder.create()
                .url(System.getenv("SPRING_DATASOURCE_URL"))
                .username(System.getenv("SPRING_DATASOURCE_USERNAME"))
                .password(System.getenv("SPRING_DATASOURCE_PASSWORD"))
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
