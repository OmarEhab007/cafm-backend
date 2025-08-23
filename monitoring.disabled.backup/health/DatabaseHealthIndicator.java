package com.cafm.cafmbackend.monitoring.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

/**
 * Database Health Indicator for CAFM Backend
 * 
 * Purpose: Monitors database connectivity and performance with detailed diagnostics
 * Pattern: Spring Boot HealthIndicator implementation for database monitoring
 * Java 23: Uses modern exception handling and try-with-resources
 * Architecture: Part of monitoring layer providing health checks for database
 * Standards: Implements Spring Boot Actuator health check standards
 */
@Component("database")
public class DatabaseHealthIndicator implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    private final DataSource dataSource;
    private final MeterRegistry meterRegistry;
    private final Timer healthCheckTimer;
    
    // Health check query - lightweight but comprehensive
    private static final String HEALTH_CHECK_QUERY = """
        SELECT 
            current_database() as database_name,
            current_user as current_user,
            pg_backend_pid() as backend_pid,
            version() as version,
            NOW() as current_time
        """;

    public DatabaseHealthIndicator(DataSource dataSource, MeterRegistry meterRegistry) {
        this.dataSource = dataSource;
        this.meterRegistry = meterRegistry;
        this.healthCheckTimer = Timer.builder("cafm.health.database.check")
            .description("Database health check execution time")
            .register(meterRegistry);
    }

    @Override
    public Health health() {
        return healthCheckTimer.recordCallable(this::performHealthCheck);
    }

    private Health performHealthCheck() {
        Instant startTime = Instant.now();
        
        try (Connection connection = dataSource.getConnection()) {
            
            // Test basic connectivity
            if (connection.isClosed()) {
                return Health.down()
                    .withDetail("error", "Database connection is closed")
                    .withDetail("timestamp", Instant.now())
                    .build();
            }

            // Test query execution
            DatabaseInfo dbInfo = executeHealthCheckQuery(connection);
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            // Check response time threshold (warn if > 1 second, down if > 5 seconds)
            Health.Builder healthBuilder;
            if (responseTime.toMillis() > 5000) {
                healthBuilder = Health.down()
                    .withDetail("error", "Database response time too slow");
            } else if (responseTime.toMillis() > 1000) {
                healthBuilder = Health.up()
                    .withDetail("warning", "Database response time is slow");
            } else {
                healthBuilder = Health.up();
            }

            // Add detailed information
            return healthBuilder
                .withDetail("database", dbInfo.databaseName())
                .withDetail("user", dbInfo.currentUser())
                .withDetail("backend_pid", dbInfo.backendPid())
                .withDetail("version", dbInfo.version())
                .withDetail("response_time_ms", responseTime.toMillis())
                .withDetail("connection_valid", true)
                .withDetail("last_check", Instant.now())
                .withDetail("connection_class", connection.getClass().getSimpleName())
                .build();

        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("error_code", e.getErrorCode())
                .withDetail("sql_state", e.getSQLState())
                .withDetail("timestamp", Instant.now())
                .withException(e)
                .build();
        } catch (Exception e) {
            logger.error("Unexpected error during database health check", e);
            
            return Health.down()
                .withDetail("error", "Unexpected error: " + e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .withDetail("timestamp", Instant.now())
                .build();
        }
    }

    private DatabaseInfo executeHealthCheckQuery(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(HEALTH_CHECK_QUERY);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                return new DatabaseInfo(
                    resultSet.getString("database_name"),
                    resultSet.getString("current_user"),
                    resultSet.getInt("backend_pid"),
                    resultSet.getString("version")
                );
            } else {
                throw new SQLException("Health check query returned no results");
            }
        }
    }

    /**
     * Database information record
     */
    private record DatabaseInfo(
        String databaseName,
        String currentUser,
        int backendPid,
        String version
    ) {}
}