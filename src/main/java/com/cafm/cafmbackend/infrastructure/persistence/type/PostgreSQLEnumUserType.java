package com.cafm.cafmbackend.infrastructure.persistence.type;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Generic Hibernate UserType for PostgreSQL enum types.
 * 
 * Purpose: Handles PostgreSQL enum types without using @ColumnTransformer
 * Pattern: Template method pattern with generic enum conversion
 * Java 23: Modern generic type system with improved type inference
 * Architecture: Database type conversion layer for PostgreSQL enums
 * Standards: Hibernate 6.x UserType with proper PostgreSQL enum handling
 * 
 * This implementation eliminates the need for @ColumnTransformer annotations
 * that can cause transaction commit failures with PostgreSQL enum types.
 */
public abstract class PostgreSQLEnumUserType<E extends Enum<E>> implements UserType<E> {
    
    private final Class<E> enumClass;
    private final String postgresEnumType;
    
    protected PostgreSQLEnumUserType(Class<E> enumClass, String postgresEnumType) {
        this.enumClass = enumClass;
        this.postgresEnumType = postgresEnumType;
    }
    
    /**
     * Convert enum value to database string representation
     */
    protected abstract String convertToDatabaseValue(E enumValue);
    
    /**
     * Convert database string value to enum
     */
    protected abstract E convertToEnumValue(String databaseValue);
    
    @Override
    public int getSqlType() {
        return Types.OTHER;
    }
    
    @Override
    public Class<E> returnedClass() {
        return enumClass;
    }
    
    @Override
    public boolean equals(E x, E y) {
        return Objects.equals(x, y);
    }
    
    @Override
    public int hashCode(E x) {
        return x != null ? x.hashCode() : 0;
    }
    
    @Override
    public E nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) 
            throws SQLException {
        Object value = rs.getObject(position);
        
        if (rs.wasNull() || value == null) {
            return null;
        }
        
        String stringValue;
        if (value instanceof PGobject pgObject) {
            stringValue = pgObject.getValue();
        } else if (value instanceof String str) {
            stringValue = str;
        } else {
            throw new IllegalArgumentException(
                "Unable to convert database value of type " + value.getClass().getName() + 
                " to " + enumClass.getSimpleName()
            );
        }
        
        if (stringValue == null || stringValue.trim().isEmpty()) {
            return null;
        }
        
        return convertToEnumValue(stringValue);
    }
    
    @Override
    public void nullSafeSet(PreparedStatement st, E value, int index, SharedSessionContractImplementor session) 
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            try {
                PGobject pgObject = new PGobject();
                pgObject.setType(postgresEnumType);
                pgObject.setValue(convertToDatabaseValue(value));
                st.setObject(index, pgObject);
            } catch (SQLException e) {
                throw new SQLException("Error setting PostgreSQL enum value for " + enumClass.getSimpleName(), e);
            }
        }
    }
    
    @Override
    public E deepCopy(E value) {
        return value; // Enums are immutable
    }
    
    @Override
    public boolean isMutable() {
        return false; // Enums are immutable
    }
    
    @Override
    public Serializable disassemble(E value) {
        return value != null ? value.name() : null;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public E assemble(Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        
        String enumName = (String) cached;
        try {
            return Enum.valueOf(enumClass, enumName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to deserialize enum " + enumClass.getSimpleName() + 
                " with value: " + enumName, e);
        }
    }
    
    @Override
    public E replace(E original, E target, Object owner) {
        return original; // Enums are immutable
    }
}