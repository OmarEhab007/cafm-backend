package com.cafm.cafmbackend.app.config;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

/**
 * Custom PostgreSQL dialect to handle enum types properly.
 * 
 * Purpose: Configures Hibernate to properly handle PostgreSQL enum types
 * Pattern: Custom dialect extension for database-specific features
 * Java 23: Uses modern Hibernate 6.x type system
 * Architecture: Database configuration layer
 * Standards: Hibernate dialect extension best practices
 */
public class PostgreSQLEnumDialect extends PostgreSQLDialect {
    
    public PostgreSQLEnumDialect() {
        super();
    }
    
    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.contributeTypes(typeContributions, serviceRegistry);
        
        // Register PostgreSQL enum type handler
        JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
                .getJdbcTypeRegistry();
        
        // Get the OTHER type descriptor for PostgreSQL enums
        JdbcType otherType = jdbcTypeRegistry.getDescriptor(SqlTypes.OTHER);
        
        // Register it for VARCHAR columns that are actually enums
        jdbcTypeRegistry.addDescriptor(Types.OTHER, otherType);
    }
}