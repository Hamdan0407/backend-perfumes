# ============================================================================
# STAGE 1: BUILD THE APPLICATION WITH MAVEN
# ============================================================================
FROM maven:3.9-eclipse-temurin-22-alpine AS builder

WORKDIR /build

# Copy source code
COPY pom.xml .
COPY src src

# Build the JAR with lower memory to avoid OOM
ENV MAVEN_OPTS="-Xmx1024m -Xms512m"
RUN mvn -q clean package -DskipTests

# ============================================================================
# STAGE 2: RUNTIME IMAGE (Final)
# ============================================================================
FROM eclipse-temurin:22-jdk-alpine

LABEL maintainer="Perfume Shop <support@perfume.com>"
LABEL description="Production-ready Perfume Shop API"

WORKDIR /app

# Create app user for security
RUN addgroup -S perfume && adduser -S perfume -G perfume

# Create log directory
RUN mkdir -p /var/log/perfume-shop && \
    chown -R perfume:perfume /var/log/perfume-shop && \
    chmod 755 /var/log/perfume-shop

# Copy JAR from builder stage
COPY --from=builder /build/target/perfume-shop-1.0.0.jar app.jar

# Change ownership
RUN chown -R perfume:perfume /app

# Switch to non-root user
USER perfume

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Expose ports


# Environment variables (can be overridden at runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV LOG_DIR=/var/log/perfume-shop
ENV SPRING_DATASOURCE_URL="jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}"
ENV JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"]
