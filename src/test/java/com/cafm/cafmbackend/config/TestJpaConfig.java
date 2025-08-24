package com.cafm.cafmbackend.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test-specific JPA configuration for H2 compatibility.
 * 
 * Purpose: Configures Hibernate to handle PostgreSQL enums as VARCHAR in H2
 * Pattern: Test configuration override with profile activation
 * Java 23: Modern configuration class with dependency injection
 * Architecture: Test infrastructure configuration layer
 * Standards: Spring Boot test configuration best practices
 * 
 * This configuration ensures that PostgreSQL enum types are properly mapped
 * to VARCHAR types when running tests with H2 database.
 */
@TestConfiguration
@Profile("test")
public class TestJpaConfig {
    // Configuration handled by H2TestDialect and application-test.yml
}