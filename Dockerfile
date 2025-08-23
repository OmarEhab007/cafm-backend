# Multi-stage build for CAFM Backend
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-23 AS builder

# Set working directory
WORKDIR /app

# Copy Maven files for dependency resolution
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:23-jre-alpine

# Install necessary packages
RUN apk add --no-cache \
    curl \
    bash \
    tzdata \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1000 -S cafm && \
    adduser -u 1000 -S cafm -G cafm

# Set working directory
WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/target/cafm-backend-*.jar app.jar

# Create necessary directories
RUN mkdir -p /app/logs /app/uploads /app/temp && \
    chown -R cafm:cafm /app

# Switch to non-root user
USER cafm

# JVM options for container environment
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+EnableDynamicAgentLoading \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]