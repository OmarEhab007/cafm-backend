package com.cafm.cafmbackend.configuration.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Optimized database connection pool configuration.
 * 
 * Purpose: Configure HikariCP for optimal database connection pooling
 * Pattern: Connection pool with performance tuning
 * Java 23: Uses modern configuration patterns
 * Architecture: Database layer optimization
 * Standards: Production-ready connection pool settings
 */
@Configuration
public class DataSourceConfig {
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;
    
    // HikariCP settings
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${spring.datasource.hikari.auto-commit:true}")
    private boolean autoCommit;
    
    @Value("${spring.datasource.hikari.pool-name:CafmHikariPool}")
    private String poolName;
    
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;
    
    @Value("${spring.datasource.hikari.register-mbeans:false}")
    private boolean registerMbeans;
    
    /**
     * Configure optimized HikariCP data source
     */
    @Bean
    @Primary
    @ConditionalOnProperty(
        prefix = "spring.datasource.hikari",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Basic configuration
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // Pool configuration
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setAutoCommit(autoCommit);
        config.setPoolName(poolName);
        
        // Performance optimizations
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // PostgreSQL connection properties
        config.addDataSourceProperty("stringtype", "unspecified");
        config.addDataSourceProperty("ApplicationName", "CAFM-Backend");
        
        // Connection pool metrics
        config.setMetricRegistry(null); // Can be configured with Micrometer if needed
        config.setHealthCheckRegistry(null); // Can be configured for health checks
        
        // Initialize pool eagerly
        config.setInitializationFailTimeout(60000);
        
        // Register MBeans for monitoring - respect application property to prevent conflicts
        config.setRegisterMbeans(registerMbeans);
        
        return new HikariDataSource(config);
    }
    
    /**
     * Configure connection pool monitoring
     */
    // Disabled for now - may cause circular dependency
    // @Bean
    // public HikariPoolMonitor hikariPoolMonitor(DataSource dataSource) {
    //     if (dataSource instanceof HikariDataSource) {
    //         return new HikariPoolMonitor((HikariDataSource) dataSource);
    //     }
    //     return null;
    // }
    
    /**
     * Inner class for monitoring HikariCP metrics
     */
    public static class HikariPoolMonitor {
        private final HikariDataSource dataSource;
        
        public HikariPoolMonitor(HikariDataSource dataSource) {
            this.dataSource = dataSource;
        }
        
        public int getActiveConnections() {
            return dataSource.getHikariPoolMXBean().getActiveConnections();
        }
        
        public int getIdleConnections() {
            return dataSource.getHikariPoolMXBean().getIdleConnections();
        }
        
        public int getTotalConnections() {
            return dataSource.getHikariPoolMXBean().getTotalConnections();
        }
        
        public int getThreadsAwaitingConnection() {
            return dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();
        }
        
        public void logPoolStats() {
            System.out.println("HikariCP Pool Stats:");
            System.out.println("  Active: " + getActiveConnections());
            System.out.println("  Idle: " + getIdleConnections());
            System.out.println("  Total: " + getTotalConnections());
            System.out.println("  Waiting: " + getThreadsAwaitingConnection());
        }
    }
}