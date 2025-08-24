package com.cafm.cafmbackend.config;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.SqlTypes;

/**
 * Custom H2 Dialect for testing that handles PostgreSQL enum types.
 * 
 * Purpose: Maps PostgreSQL enum types to VARCHAR in H2 for testing compatibility
 * Pattern: Custom dialect for seamless PostgreSQL-to-H2 test migration
 * Java 23: Uses modern switch expressions for type mapping
 * Architecture: Test infrastructure component for database compatibility
 * Standards: Follows Hibernate dialect extension patterns
 */
public class H2TestDialect extends H2Dialect {

    /**
     * Constructor for H2TestDialect.
     */
    public H2TestDialect() {
        super();
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        // Map PostgreSQL enum types to VARCHAR for H2 compatibility
        switch (sqlTypeCode) {
            case SqlTypes.OTHER:
                return "varchar(255)"; // Map enum types to VARCHAR
            default:
                return super.columnType(sqlTypeCode);
        }
    }
}