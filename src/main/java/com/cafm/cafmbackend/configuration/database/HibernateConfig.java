package com.cafm.cafmbackend.configuration.database;

import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.springframework.context.annotation.Configuration;

/**
 * Hibernate configuration for PostgreSQL enum types.
 * 
 * Purpose: Configure Hibernate to properly handle PostgreSQL enum types
 * Pattern: Type contributor for custom database types
 * Java 23: Uses modern Hibernate 6.x type system
 * Architecture: Database configuration layer
 * Standards: Hibernate type configuration best practices
 */
@Configuration
public class HibernateConfig implements TypeContributor {

    @Override
    public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        // Register PostgreSQL enum type handling
        JdbcType enumJdbcType = ObjectJdbcType.INSTANCE;
        typeContributions.contributeJdbcType(enumJdbcType);
    }
}