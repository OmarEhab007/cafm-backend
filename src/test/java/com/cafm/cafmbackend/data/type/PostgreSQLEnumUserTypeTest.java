package com.cafm.cafmbackend.data.type;

import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostgreSQL enum UserType implementations.
 * 
 * Purpose: Verify proper handling of PostgreSQL enum types without @ColumnTransformer
 * Pattern: Comprehensive unit testing with mocked database interactions
 * Java 23: JUnit 5 with modern test patterns and display names
 * Architecture: Testing database type conversion layer
 * Standards: Complete test coverage for enum conversion scenarios
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgreSQL Enum UserType Tests")
class PostgreSQLEnumUserTypeTest {
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private PreparedStatement preparedStatement;
    
    @Mock
    private SharedSessionContractImplementor session;
    
    private UserTypeUserType userTypeUserType;
    private UserStatusUserType userStatusUserType;
    
    @BeforeEach
    void setUp() {
        userTypeUserType = new UserTypeUserType();
        userStatusUserType = new UserStatusUserType();
    }
    
    @Test
    @DisplayName("UserType - Should return correct SQL type")
    void userType_shouldReturnCorrectSqlType() {
        assertEquals(Types.OTHER, userTypeUserType.getSqlType());
    }
    
    @Test
    @DisplayName("UserType - Should return correct class")
    void userType_shouldReturnCorrectClass() {
        assertEquals(UserType.class, userTypeUserType.returnedClass());
    }
    
    @Test
    @DisplayName("UserType - Should handle null values correctly in nullSafeGet")
    void userType_shouldHandleNullValuesInNullSafeGet() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(null);
        when(resultSet.wasNull()).thenReturn(true);
        
        UserType result = userTypeUserType.nullSafeGet(resultSet, 1, session, null);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("UserType - Should convert PGobject to UserType in nullSafeGet")
    void userType_shouldConvertPGObjectToUserType() throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("user_type");
        pgObject.setValue("ADMIN");
        
        when(resultSet.getObject(1)).thenReturn(pgObject);
        when(resultSet.wasNull()).thenReturn(false);
        
        UserType result = userTypeUserType.nullSafeGet(resultSet, 1, session, null);
        
        assertEquals(UserType.ADMIN, result);
    }
    
    @Test
    @DisplayName("UserType - Should convert String to UserType in nullSafeGet")
    void userType_shouldConvertStringToUserType() throws SQLException {
        when(resultSet.getObject(1)).thenReturn("SUPERVISOR");
        when(resultSet.wasNull()).thenReturn(false);
        
        UserType result = userTypeUserType.nullSafeGet(resultSet, 1, session, null);
        
        assertEquals(UserType.SUPERVISOR, result);
    }
    
    @Test
    @DisplayName("UserType - Should handle null values correctly in nullSafeSet")
    void userType_shouldHandleNullValuesInNullSafeSet() throws SQLException {
        userTypeUserType.nullSafeSet(preparedStatement, null, 1, session);
        
        verify(preparedStatement).setNull(1, Types.OTHER);
        verify(preparedStatement, never()).setObject(anyInt(), any());
    }
    
    @Test
    @DisplayName("UserType - Should set PGobject correctly in nullSafeSet")
    void userType_shouldSetPGObjectCorrectly() throws SQLException {
        userTypeUserType.nullSafeSet(preparedStatement, UserType.TECHNICIAN, 1, session);
        
        verify(preparedStatement).setObject(eq(1), argThat(obj -> {
            if (obj instanceof PGobject pgObj) {
                return "user_type".equals(pgObj.getType()) && 
                       "TECHNICIAN".equals(pgObj.getValue());
            }
            return false;
        }));
    }
    
    @Test
    @DisplayName("UserStatus - Should return correct SQL type")
    void userStatus_shouldReturnCorrectSqlType() {
        assertEquals(Types.OTHER, userStatusUserType.getSqlType());
    }
    
    @Test
    @DisplayName("UserStatus - Should return correct class")
    void userStatus_shouldReturnCorrectClass() {
        assertEquals(UserStatus.class, userStatusUserType.returnedClass());
    }
    
    @Test
    @DisplayName("UserStatus - Should convert PGobject to UserStatus in nullSafeGet")
    void userStatus_shouldConvertPGObjectToUserStatus() throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("user_status");
        pgObject.setValue("ACTIVE");
        
        when(resultSet.getObject(1)).thenReturn(pgObject);
        when(resultSet.wasNull()).thenReturn(false);
        
        UserStatus result = userStatusUserType.nullSafeGet(resultSet, 1, session, null);
        
        assertEquals(UserStatus.ACTIVE, result);
    }
    
    @Test
    @DisplayName("UserStatus - Should set PGobject correctly in nullSafeSet")
    void userStatus_shouldSetPGObjectCorrectly() throws SQLException {
        userStatusUserType.nullSafeSet(preparedStatement, UserStatus.SUSPENDED, 1, session);
        
        verify(preparedStatement).setObject(eq(1), argThat(obj -> {
            if (obj instanceof PGobject pgObj) {
                return "user_status".equals(pgObj.getType()) && 
                       "SUSPENDED".equals(pgObj.getValue());
            }
            return false;
        }));
    }
    
    @Test
    @DisplayName("Should handle equals correctly")
    void shouldHandleEqualsCorrectly() {
        assertTrue(userTypeUserType.equals(UserType.ADMIN, UserType.ADMIN));
        assertFalse(userTypeUserType.equals(UserType.ADMIN, UserType.SUPERVISOR));
        assertTrue(userTypeUserType.equals(null, null));
        assertFalse(userTypeUserType.equals(UserType.ADMIN, null));
        assertFalse(userTypeUserType.equals(null, UserType.ADMIN));
    }
    
    @Test
    @DisplayName("Should handle hashCode correctly")
    void shouldHandleHashCodeCorrectly() {
        assertEquals(UserType.ADMIN.hashCode(), userTypeUserType.hashCode(UserType.ADMIN));
        assertEquals(0, userTypeUserType.hashCode(null));
    }
    
    @Test
    @DisplayName("Should handle deep copy correctly")
    void shouldHandleDeepCopyCorrectly() {
        UserType original = UserType.ADMIN;
        UserType copy = userTypeUserType.deepCopy(original);
        
        assertSame(original, copy); // Enums are immutable, should return same instance
        assertNull(userTypeUserType.deepCopy(null));
    }
    
    @Test
    @DisplayName("Should indicate enums are immutable")
    void shouldIndicateEnumsAreImmutable() {
        assertFalse(userTypeUserType.isMutable());
        assertFalse(userStatusUserType.isMutable());
    }
    
    @Test
    @DisplayName("Should handle disassemble and assemble correctly")
    void shouldHandleDisassembleAndAssembleCorrectly() {
        UserType original = UserType.ADMIN;
        
        String disassembled = (String) userTypeUserType.disassemble(original);
        assertEquals("ADMIN", disassembled);
        
        UserType reassembled = userTypeUserType.assemble(disassembled, null);
        assertEquals(original, reassembled);
        
        assertNull(userTypeUserType.disassemble(null));
        assertNull(userTypeUserType.assemble(null, null));
    }
    
    @Test
    @DisplayName("Should handle replace correctly")
    void shouldHandleReplaceCorrectly() {
        UserType original = UserType.ADMIN;
        UserType target = UserType.SUPERVISOR;
        
        UserType result = userTypeUserType.replace(original, target, null);
        
        assertSame(original, result); // Should return original since enums are immutable
    }
    
    @Test
    @DisplayName("Should throw exception for invalid database values")
    void shouldThrowExceptionForInvalidDatabaseValues() throws SQLException {
        when(resultSet.getObject(1)).thenReturn("INVALID_STATUS");
        when(resultSet.wasNull()).thenReturn(false);
        
        assertThrows(IllegalArgumentException.class, 
            () -> userStatusUserType.nullSafeGet(resultSet, 1, session, null));
    }
    
    @Test
    @DisplayName("Should throw exception for unsupported database object types")
    void shouldThrowExceptionForUnsupportedDatabaseObjectTypes() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(42); // Integer instead of String or PGobject
        when(resultSet.wasNull()).thenReturn(false);
        
        assertThrows(IllegalArgumentException.class, 
            () -> userTypeUserType.nullSafeGet(resultSet, 1, session, null));
    }
}