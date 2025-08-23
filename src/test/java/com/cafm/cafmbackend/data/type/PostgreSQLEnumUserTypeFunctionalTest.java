package com.cafm.cafmbackend.data.type;

import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for PostgreSQL enum UserType implementations.
 * 
 * Purpose: Verify proper handling of PostgreSQL enum types without @ColumnTransformer
 * Pattern: Direct functional testing of enum conversion logic
 * Java 23: JUnit 5 with modern test patterns and display names
 * Architecture: Testing database type conversion layer functionality
 * Standards: Comprehensive test coverage for enum conversion scenarios
 * 
 * This test validates the core functionality of our PostgreSQL enum handling
 * without requiring complex database mocking or actual database connections.
 */
@DisplayName("PostgreSQL Enum UserType Functional Tests")
class PostgreSQLEnumUserTypeFunctionalTest {
    
    private final UserTypeUserType userTypeUserType = new UserTypeUserType();
    private final UserStatusUserType userStatusUserType = new UserStatusUserType();
    
    @Test
    @DisplayName("UserType - Should return correct SQL type and class")
    void userType_shouldReturnCorrectSqlTypeAndClass() {
        assertEquals(java.sql.Types.OTHER, userTypeUserType.getSqlType());
        assertEquals(UserType.class, userTypeUserType.returnedClass());
        assertFalse(userTypeUserType.isMutable());
    }
    
    @Test
    @DisplayName("UserType - Should handle enum equals and hashCode correctly")
    void userType_shouldHandleEqualsAndHashCodeCorrectly() {
        assertTrue(userTypeUserType.equals(UserType.ADMIN, UserType.ADMIN));
        assertFalse(userTypeUserType.equals(UserType.ADMIN, UserType.SUPERVISOR));
        assertTrue(userTypeUserType.equals(null, null));
        assertFalse(userTypeUserType.equals(UserType.ADMIN, null));
        
        assertEquals(UserType.ADMIN.hashCode(), userTypeUserType.hashCode(UserType.ADMIN));
        assertEquals(0, userTypeUserType.hashCode(null));
    }
    
    @Test
    @DisplayName("UserType - Should handle deep copy correctly")
    void userType_shouldHandleDeepCopyCorrectly() {
        UserType original = UserType.SUPERVISOR;
        UserType copy = userTypeUserType.deepCopy(original);
        
        assertSame(original, copy); // Enums are immutable
        assertNull(userTypeUserType.deepCopy(null));
    }
    
    @Test
    @DisplayName("UserType - Should handle disassemble and assemble correctly")
    void userType_shouldHandleDisassembleAndAssembleCorrectly() {
        UserType original = UserType.TECHNICIAN;
        
        String disassembled = (String) userTypeUserType.disassemble(original);
        assertEquals("TECHNICIAN", disassembled);
        
        UserType reassembled = userTypeUserType.assemble(disassembled, null);
        assertEquals(original, reassembled);
        
        assertNull(userTypeUserType.disassemble(null));
        assertNull(userTypeUserType.assemble(null, null));
    }
    
    @Test
    @DisplayName("UserType - Should handle replace correctly")
    void userType_shouldHandleReplaceCorrectly() {
        UserType original = UserType.ADMIN;
        UserType target = UserType.SUPERVISOR;
        
        UserType result = userTypeUserType.replace(original, target, null);
        
        assertSame(original, result); // Should return original since enums are immutable
    }
    
    @Test
    @DisplayName("UserType - Should convert enum to database value correctly")
    void userType_shouldConvertEnumToDatabaseValue() {
        // Test the protected method via the public interface
        for (UserType userType : UserType.values()) {
            // Test that conversion works by creating a UserTypeUserType instance
            // and verifying the enum values map correctly to their database representations
            String dbValue = userType.getDbValue();
            UserType converted = UserType.fromDbValue(dbValue);
            assertEquals(userType, converted);
        }
    }
    
    @Test
    @DisplayName("UserType - Should handle all enum values correctly")
    void userType_shouldHandleAllEnumValues() {
        UserType[] allTypes = UserType.values();
        
        for (UserType type : allTypes) {
            assertNotNull(type);
            assertNotNull(type.getDbValue());
            assertFalse(type.getDbValue().trim().isEmpty());
            
            // Verify round-trip conversion
            UserType converted = UserType.fromDbValue(type.getDbValue());
            assertEquals(type, converted);
        }
    }
    
    @Test
    @DisplayName("UserStatus - Should return correct SQL type and class")
    void userStatus_shouldReturnCorrectSqlTypeAndClass() {
        assertEquals(java.sql.Types.OTHER, userStatusUserType.getSqlType());
        assertEquals(UserStatus.class, userStatusUserType.returnedClass());
        assertFalse(userStatusUserType.isMutable());
    }
    
    @Test
    @DisplayName("UserStatus - Should handle all enum values correctly")
    void userStatus_shouldHandleAllEnumValues() {
        UserStatus[] allStatuses = UserStatus.values();
        
        for (UserStatus status : allStatuses) {
            assertNotNull(status);
            assertNotNull(status.getDbValue());
            assertFalse(status.getDbValue().trim().isEmpty());
            
            // Verify round-trip conversion
            UserStatus converted = UserStatus.fromDbValue(status.getDbValue());
            assertEquals(status, converted);
        }
    }
    
    @Test
    @DisplayName("UserStatus - Should handle enum equals and hashCode correctly")
    void userStatus_shouldHandleEqualsAndHashCodeCorrectly() {
        assertTrue(userStatusUserType.equals(UserStatus.ACTIVE, UserStatus.ACTIVE));
        assertFalse(userStatusUserType.equals(UserStatus.ACTIVE, UserStatus.SUSPENDED));
        assertTrue(userStatusUserType.equals(null, null));
        assertFalse(userStatusUserType.equals(UserStatus.ACTIVE, null));
        
        assertEquals(UserStatus.ACTIVE.hashCode(), userStatusUserType.hashCode(UserStatus.ACTIVE));
        assertEquals(0, userStatusUserType.hashCode(null));
    }
    
    @Test
    @DisplayName("Should create valid PGObject for database operations")
    void shouldCreateValidPGObjectForDatabaseOperations() throws SQLException {
        // Test that we can create proper PGobject instances for database operations
        UserType testUserType = UserType.SUPERVISOR;
        UserStatus testUserStatus = UserStatus.ACTIVE;
        
        // Create PGobject for UserType
        PGobject userTypePG = new PGobject();
        userTypePG.setType("user_type");
        userTypePG.setValue(testUserType.getDbValue());
        
        assertEquals("user_type", userTypePG.getType());
        assertEquals("SUPERVISOR", userTypePG.getValue());
        
        // Create PGobject for UserStatus
        PGobject userStatusPG = new PGobject();
        userStatusPG.setType("user_status");
        userStatusPG.setValue(testUserStatus.getDbValue());
        
        assertEquals("user_status", userStatusPG.getType());
        assertEquals("ACTIVE", userStatusPG.getValue());
    }
    
    @Test
    @DisplayName("Should handle invalid enum values gracefully")
    void shouldHandleInvalidEnumValuesGracefully() {
        // Test UserType with invalid value
        assertThrows(IllegalArgumentException.class, 
            () -> UserType.fromDbValue("INVALID_TYPE"));
        
        // Test UserStatus with invalid value
        assertThrows(IllegalArgumentException.class, 
            () -> UserStatus.fromDbValue("INVALID_STATUS"));
    }
    
    @Test
    @DisplayName("Should handle null and empty values gracefully")
    void shouldHandleNullAndEmptyValuesGracefully() {
        // Test UserType with null/empty values
        assertNull(UserType.fromDbValue(null));
        
        // Test UserStatus with null value
        assertThrows(IllegalArgumentException.class, 
            () -> UserStatus.fromDbValue(null));
        
        // Test the UserType implementations handle null correctly
        assertEquals(0, userTypeUserType.hashCode(null));
        assertEquals(0, userStatusUserType.hashCode(null));
        
        assertNull(userTypeUserType.deepCopy(null));
        assertNull(userStatusUserType.deepCopy(null));
    }
    
    @Test
    @DisplayName("Should demonstrate proper database value mapping")
    void shouldDemonstrateDatabaseValueMapping() {
        // Verify that all enum values have proper database representations
        assertDatabaseMapping(UserType.VIEWER, "VIEWER");
        assertDatabaseMapping(UserType.TECHNICIAN, "TECHNICIAN");
        assertDatabaseMapping(UserType.SUPERVISOR, "SUPERVISOR");
        assertDatabaseMapping(UserType.ADMIN, "ADMIN");
        assertDatabaseMapping(UserType.SUPER_ADMIN, "SUPER_ADMIN");
        
        assertDatabaseMapping(UserStatus.PENDING_VERIFICATION, "PENDING_VERIFICATION");
        assertDatabaseMapping(UserStatus.ACTIVE, "ACTIVE");
        assertDatabaseMapping(UserStatus.INACTIVE, "INACTIVE");
        assertDatabaseMapping(UserStatus.SUSPENDED, "SUSPENDED");
        assertDatabaseMapping(UserStatus.LOCKED, "LOCKED");
        assertDatabaseMapping(UserStatus.ARCHIVED, "ARCHIVED");
    }
    
    private void assertDatabaseMapping(UserType userType, String expectedDbValue) {
        assertEquals(expectedDbValue, userType.getDbValue());
        assertEquals(userType, UserType.fromDbValue(expectedDbValue));
    }
    
    private void assertDatabaseMapping(UserStatus userStatus, String expectedDbValue) {
        assertEquals(expectedDbValue, userStatus.getDbValue());
        assertEquals(userStatus, UserStatus.fromDbValue(expectedDbValue));
    }
}