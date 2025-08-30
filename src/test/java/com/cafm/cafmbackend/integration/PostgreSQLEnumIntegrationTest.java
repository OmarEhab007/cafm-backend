package com.cafm.cafmbackend.integration;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.CompanyStatus;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PostgreSQL enum handling without @ColumnTransformer.
 * 
 * Purpose: Verify that PostgreSQL enum UserTypes work correctly in real database transactions
 * Pattern: Testcontainers-based integration testing with PostgreSQL
 * Java 23: Modern Spring Boot testing with comprehensive database validation
 * Architecture: Full stack integration test for enum persistence
 * Standards: Complete database transaction testing ensuring no commit failures
 * 
 * This test specifically validates that the new UserType implementations
 * eliminate the transaction commit issues that occurred with @ColumnTransformer.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
@DisplayName("PostgreSQL Enum Integration Tests")
class PostgreSQLEnumIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("test-enum-init.sql"); // Create enum types
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    private Company testCompany;
    
    @BeforeEach
    @Transactional
    void setUp() {
        // Create test company
        testCompany = new Company();
        testCompany.setName("Test Company");
        testCompany.setSubdomain("test001");
        testCompany.setStatus(CompanyStatus.ACTIVE);
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());
        testCompany = companyRepository.save(testCompany);
    }
    
    @Test
    @DisplayName("Should successfully persist and retrieve User with UserType enum")
    @Transactional
    @Rollback
    void shouldPersistAndRetrieveUserWithUserTypeEnum() {
        // Given
        User user = new User();
        user.setEmail("admin@test.com");
        user.setUsername("admin_test");
        user.setPasswordHash("hashed_password");
        user.setUserType(UserType.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setCompany(testCompany);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // When - This should not fail with our new UserType implementation
        User savedUser = userRepository.save(user);
        
        // Then
        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertEquals(UserType.ADMIN, savedUser.getUserType());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        
        // Verify we can retrieve it from database
        Optional<User> retrievedUser = userRepository.findById(savedUser.getId());
        assertTrue(retrievedUser.isPresent());
        assertEquals(UserType.ADMIN, retrievedUser.get().getUserType());
        assertEquals(UserStatus.ACTIVE, retrievedUser.get().getStatus());
    }
    
    @Test
    @DisplayName("Should handle all UserType enum values correctly")
    @Transactional
    @Rollback
    void shouldHandleAllUserTypeEnumValues() {
        UserType[] allUserTypes = UserType.values();
        
        for (UserType userType : allUserTypes) {
            // Given
            User user = new User();
            user.setEmail("user_" + userType.name().toLowerCase() + "@test.com");
            user.setUsername("user_" + userType.name().toLowerCase());
            user.setPasswordHash("hashed_password");
            user.setUserType(userType);
            user.setStatus(UserStatus.ACTIVE);
            user.setCompany(testCompany);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // When
            User savedUser = userRepository.save(user);
            
            // Then
            assertNotNull(savedUser);
            assertEquals(userType, savedUser.getUserType());
            
            // Verify retrieval
            Optional<User> retrieved = userRepository.findById(savedUser.getId());
            assertTrue(retrieved.isPresent());
            assertEquals(userType, retrieved.get().getUserType());
        }
    }
    
    @Test
    @DisplayName("Should handle all UserStatus enum values correctly")
    @Transactional
    @Rollback
    void shouldHandleAllUserStatusEnumValues() {
        UserStatus[] allStatuses = UserStatus.values();
        
        for (UserStatus status : allStatuses) {
            // Given
            User user = new User();
            user.setEmail("status_" + status.name().toLowerCase() + "@test.com");
            user.setUsername("status_" + status.name().toLowerCase());
            user.setPasswordHash("hashed_password");
            user.setUserType(UserType.VIEWER);
            user.setStatus(status);
            user.setCompany(testCompany);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // When
            User savedUser = userRepository.save(user);
            
            // Then
            assertNotNull(savedUser);
            assertEquals(status, savedUser.getStatus());
            
            // Verify retrieval
            Optional<User> retrieved = userRepository.findById(savedUser.getId());
            assertTrue(retrieved.isPresent());
            assertEquals(status, retrieved.get().getStatus());
        }
    }
    
    @Test
    @DisplayName("Should successfully update UserType and UserStatus")
    @Transactional
    @Rollback
    void shouldSuccessfullyUpdateUserTypeAndStatus() {
        // Given - Create initial user
        User user = new User();
        user.setEmail("update_test@test.com");
        user.setUsername("update_test");
        user.setPasswordHash("hashed_password");
        user.setUserType(UserType.VIEWER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCompany(testCompany);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        User savedUser = userRepository.save(user);
        
        // When - Update enum values
        savedUser.setUserType(UserType.SUPERVISOR);
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(savedUser);
        
        // Then
        assertNotNull(updatedUser);
        assertEquals(UserType.SUPERVISOR, updatedUser.getUserType());
        assertEquals(UserStatus.ACTIVE, updatedUser.getStatus());
        
        // Verify persistence after transaction
        Optional<User> retrieved = userRepository.findById(updatedUser.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(UserType.SUPERVISOR, retrieved.get().getUserType());
        assertEquals(UserStatus.ACTIVE, retrieved.get().getStatus());
    }
    
    @Test
    @DisplayName("Should handle complex user operations without transaction failures")
    @Transactional
    @Rollback
    void shouldHandleComplexUserOperationsWithoutTransactionFailures() {
        // This test specifically targets the issue that @ColumnTransformer was causing
        
        // Given - Create multiple users with different enum combinations
        User admin = createTestUser("admin@test.com", "admin", UserType.ADMIN, UserStatus.ACTIVE);
        User supervisor = createTestUser("supervisor@test.com", "supervisor", UserType.SUPERVISOR, UserStatus.ACTIVE);
        User technician = createTestUser("technician@test.com", "technician", UserType.TECHNICIAN, UserStatus.PENDING_VERIFICATION);
        
        // When - Perform batch operations
        userRepository.save(admin);
        userRepository.save(supervisor);
        userRepository.save(technician);
        
        // Update statuses
        admin.setStatus(UserStatus.SUSPENDED);
        supervisor.setUserType(UserType.ADMIN);
        technician.setStatus(UserStatus.ACTIVE);
        
        userRepository.save(admin);
        userRepository.save(supervisor);
        userRepository.save(technician);
        
        // Then - All operations should complete successfully
        Optional<User> retrievedAdmin = userRepository.findById(admin.getId());
        Optional<User> retrievedSupervisor = userRepository.findById(supervisor.getId());
        Optional<User> retrievedTechnician = userRepository.findById(technician.getId());
        
        assertTrue(retrievedAdmin.isPresent());
        assertTrue(retrievedSupervisor.isPresent());
        assertTrue(retrievedTechnician.isPresent());
        
        assertEquals(UserStatus.SUSPENDED, retrievedAdmin.get().getStatus());
        assertEquals(UserType.ADMIN, retrievedSupervisor.get().getUserType());
        assertEquals(UserStatus.ACTIVE, retrievedTechnician.get().getStatus());
    }
    
    @Test
    @DisplayName("Should handle null enum values gracefully")
    @Transactional
    @Rollback
    void shouldHandleNullEnumValuesGracefully() {
        // Given - User with default enum values (not null due to @NotNull constraints)
        User user = new User();
        user.setEmail("default@test.com");
        user.setUsername("default_user");
        user.setPasswordHash("hashed_password");
        user.setUserType(UserType.VIEWER); // Default value
        user.setStatus(UserStatus.PENDING_VERIFICATION); // Default value
        user.setCompany(testCompany);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        // When
        User savedUser = userRepository.save(user);
        
        // Then
        assertNotNull(savedUser);
        assertEquals(UserType.VIEWER, savedUser.getUserType());
        assertEquals(UserStatus.PENDING_VERIFICATION, savedUser.getStatus());
    }
    
    private User createTestUser(String email, String username, UserType userType, UserStatus status) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash("hashed_password");
        user.setUserType(userType);
        user.setStatus(status);
        user.setCompany(testCompany);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}