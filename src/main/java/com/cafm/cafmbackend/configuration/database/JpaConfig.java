package com.cafm.cafmbackend.configuration.database;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA configuration for the application.
 * 
 * Architecture: Configures JPA auditing, repositories, and transactions
 * Pattern: Configuration class following Spring Boot conventions
 * Java 23: Uses Optional pattern matching where applicable
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.cafm.cafmbackend.infrastructure.persistence.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JpaProperties jpaProperties;
    
    /**
     * Create the main EntityManagerFactory bean
     */
    @Bean("entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder) {
        return builder
                .dataSource(dataSource)
                .packages("com.cafm.cafmbackend.infrastructure.persistence.entity")
                .persistenceUnit("default")
                .properties(jpaProperties.getProperties())
                .build();
    }
    
    /**
     * Create the JPA transaction manager
     */
    @Bean("transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
    
    /**
     * Create shared entity manager for Spring Data JPA
     * Purpose: Provides shared EntityManager for repository proxies
     * Pattern: Spring Data JPA shared entity manager
     * Java 23: Uses factory pattern for entity manager creation
     * Architecture: JPA infrastructure layer
     * Standards: Spring Data JPA conventions
     * Bean Name: Must follow 'jpaSharedEM_' + entityManagerFactoryRef convention
     */
    @Bean("jpaSharedEM_entityManagerFactory")
    public EntityManager sharedEntityManager(@Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }
    
    /**
     * Provides the current auditor for JPA auditing.
     * Used to populate createdBy and modifiedBy fields automatically.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return new AuditorAware<UUID>() {
            @Override
            public Optional<UUID> getCurrentAuditor() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                
                if (authentication == null || !authentication.isAuthenticated()) {
                    return Optional.empty();
                }
                
                // Get user ID from authentication
                // This will be implemented properly when we add JWT authentication
                Object principal = authentication.getPrincipal();
                
                if (principal instanceof String && "anonymousUser".equals(principal)) {
                    return Optional.empty();
                }
                
                // For now, return empty until we implement proper authentication
                return Optional.empty();
            }
        };
    }
}