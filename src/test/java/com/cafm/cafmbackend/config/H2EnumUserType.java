package com.cafm.cafmbackend.config;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

/**
 * Generic Hibernate UserType for enum types in H2 testing environment.
 * 
 * Purpose: Maps PostgreSQL enum types to VARCHAR in H2 for testing compatibility
 * Pattern: Template method pattern with generic enum conversion for H2
 * Java 23: Modern generic type system with improved type inference
 * Architecture: Test-specific database type conversion layer
 * Standards: Hibernate 6.x UserType with H2 VARCHAR mapping
 * 
 * This implementation converts PostgreSQL enum types to simple VARCHAR
 * storage in H2, allowing tests to run without PostgreSQL dependency.
 */
public abstract class H2EnumUserType<E extends Enum<E>> implements UserType<E> {
    
    private final Class<E> enumClass;
    
    protected H2EnumUserType(Class<E> enumClass) {
        this.enumClass = enumClass;
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
        return Types.VARCHAR; // Use VARCHAR instead of OTHER for H2
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
        String value = rs.getString(position);
        
        if (rs.wasNull() || value == null || value.trim().isEmpty()) {
            return null;
        }
        
        return convertToEnumValue(value.trim());
    }
    
    @Override
    public void nullSafeSet(PreparedStatement st, E value, int index, SharedSessionContractImplementor session) 
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, convertToDatabaseValue(value));
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