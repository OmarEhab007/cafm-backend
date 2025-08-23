package com.cafm.cafmbackend.service;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.data.repository.CompanyRepository;
import com.cafm.cafmbackend.data.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for the security methods implemented in UserService and CompanyService.
 * 
 * Purpose: Verify that tenant isolation security methods work correctly
 * Pattern: Unit testing with mocked dependencies for isolation
 * Java 23: Modern test patterns with descriptive test names
 * Architecture: Tests cover the security boundary enforcement
 * Standards: Comprehensive test coverage for security-critical functionality
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Security Methods Test Suite")
class SecurityMethodsTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserService userService;
    
    @InjectMocks
    private CompanyService companyService;
    
    private Company company1;
    private Company company2;
    private User adminUser1;
    private User supervisorUser1;
    private User technicianUser1;
    private User adminUser2;
    private UUID userId1;
    private UUID userId2;
    private UUID companyId1;
    private UUID companyId2;
    
    @BeforeEach
    void setUp() {
        // Test data setup
        companyId1 = UUID.randomUUID();
        companyId2 = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        
        // Company 1
        company1 = new Company();
        company1.setId(companyId1);
        company1.setName("Company 1");
        
        // Company 2
        company2 = new Company();
        company2.setId(companyId2);
        company2.setName("Company 2");
        
        // User in Company 1 (Admin)
        adminUser1 = new User();
        adminUser1.setId(userId1);
        adminUser1.setEmail("admin1@company1.com");
        adminUser1.setUserType(UserType.ADMIN);
        adminUser1.setCompany(company1);
        adminUser1.setStatus(UserStatus.ACTIVE);
        
        // User in Company 1 (Supervisor)
        supervisorUser1 = new User();
        supervisorUser1.setId(UUID.randomUUID());
        supervisorUser1.setEmail("supervisor1@company1.com");
        supervisorUser1.setUserType(UserType.SUPERVISOR);
        supervisorUser1.setCompany(company1);
        supervisorUser1.setStatus(UserStatus.ACTIVE);
        
        // User in Company 1 (Technician)
        technicianUser1 = new User();
        technicianUser1.setId(UUID.randomUUID());
        technicianUser1.setEmail("technician1@company1.com");
        technicianUser1.setUserType(UserType.TECHNICIAN);
        technicianUser1.setCompany(company1);
        technicianUser1.setStatus(UserStatus.ACTIVE);
        
        // User in Company 2 (Admin)
        adminUser2 = new User();
        adminUser2.setId(userId2);
        adminUser2.setEmail("admin2@company2.com");
        adminUser2.setUserType(UserType.ADMIN);
        adminUser2.setCompany(company2);
        adminUser2.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("isSameCompany should return true for users in same company")
    void testIsSameCompany_SameCompany_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        when(userRepository.findById(supervisorUser1.getId()))
            .thenReturn(Optional.of(supervisorUser1));
        
        // Act
        boolean result = userService.isSameCompany(supervisorUser1.getId(), "admin1@company1.com");
        
        // Assert
        assertTrue(result, "Users from same company should return true");
        verify(userRepository).findByEmailIgnoreCase("admin1@company1.com");
        verify(userRepository).findById(supervisorUser1.getId());
    }

    @Test
    @DisplayName("isSameCompany should return false for users in different companies")
    void testIsSameCompany_DifferentCompany_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        when(userRepository.findById(userId2))
            .thenReturn(Optional.of(adminUser2));
        
        // Act
        boolean result = userService.isSameCompany(userId2, "admin1@company1.com");
        
        // Assert
        assertFalse(result, "Users from different companies should return false");
        verify(userRepository).findByEmailIgnoreCase("admin1@company1.com");
        verify(userRepository).findById(userId2);
    }

    @Test
    @DisplayName("isSameCompany should return false when current user not found")
    void testIsSameCompany_CurrentUserNotFound_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("nonexistent@company.com"))
            .thenReturn(Optional.empty());
        
        // Act
        boolean result = userService.isSameCompany(userId1, "nonexistent@company.com");
        
        // Assert
        assertFalse(result, "Should return false when current user not found");
        verify(userRepository).findByEmailIgnoreCase("nonexistent@company.com");
        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("isSameCompany should return false when target user not found")
    void testIsSameCompany_TargetUserNotFound_ReturnsFalse() {
        // Arrange
        UUID nonExistentUserId = UUID.randomUUID();
        when(userRepository.findByEmailIgnoreCase("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        when(userRepository.findById(nonExistentUserId))
            .thenReturn(Optional.empty());
        
        // Act
        boolean result = userService.isSameCompany(nonExistentUserId, "admin1@company1.com");
        
        // Assert
        assertFalse(result, "Should return false when target user not found");
        verify(userRepository).findByEmailIgnoreCase("admin1@company1.com");
        verify(userRepository).findById(nonExistentUserId);
    }

    @Test
    @DisplayName("canManageUser should return true for admin managing any user in same company")
    void testCanManageUser_AdminManagingSameCompany_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        when(userRepository.findById(technicianUser1.getId()))
            .thenReturn(Optional.of(technicianUser1));
        
        // Act
        boolean result = userService.canManageUser(technicianUser1.getId(), "admin1@company1.com");
        
        // Assert
        assertTrue(result, "Admin should be able to manage any user in same company");
    }

    @Test
    @DisplayName("canManageUser should return true for supervisor managing technician in same company")
    void testCanManageUser_SupervisorManagingTechnician_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("supervisor1@company1.com"))
            .thenReturn(Optional.of(supervisorUser1));
        when(userRepository.findById(technicianUser1.getId()))
            .thenReturn(Optional.of(technicianUser1));
        
        // Act
        boolean result = userService.canManageUser(technicianUser1.getId(), "supervisor1@company1.com");
        
        // Assert
        assertTrue(result, "Supervisor should be able to manage technician in same company");
    }

    @Test
    @DisplayName("canManageUser should return false for supervisor trying to manage admin")
    void testCanManageUser_SupervisorManagingAdmin_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("supervisor1@company1.com"))
            .thenReturn(Optional.of(supervisorUser1));
        when(userRepository.findById(adminUser1.getId()))
            .thenReturn(Optional.of(adminUser1));
        
        // Act
        boolean result = userService.canManageUser(adminUser1.getId(), "supervisor1@company1.com");
        
        // Assert
        assertFalse(result, "Supervisor should not be able to manage admin");
    }

    @Test
    @DisplayName("canManageUser should return false for technician trying to manage other users")
    void testCanManageUser_TechnicianManagingOthers_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("technician1@company1.com"))
            .thenReturn(Optional.of(technicianUser1));
        when(userRepository.findById(supervisorUser1.getId()))
            .thenReturn(Optional.of(supervisorUser1));
        
        // Act
        boolean result = userService.canManageUser(supervisorUser1.getId(), "technician1@company1.com");
        
        // Assert
        assertFalse(result, "Technician should not be able to manage other users");
    }

    @Test
    @DisplayName("canManageUser should return true for self-management")
    void testCanManageUser_SelfManagement_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("technician1@company1.com"))
            .thenReturn(Optional.of(technicianUser1));
        when(userRepository.findById(technicianUser1.getId()))
            .thenReturn(Optional.of(technicianUser1));
        
        // Act
        boolean result = userService.canManageUser(technicianUser1.getId(), "technician1@company1.com");
        
        // Assert
        assertTrue(result, "Users should be able to manage themselves");
    }

    @Test
    @DisplayName("belongsToCompany should return true for user in specified company")
    void testBelongsToCompany_UserInCompany_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmail("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        
        // Act
        boolean result = companyService.belongsToCompany(companyId1, "admin1@company1.com");
        
        // Assert
        assertTrue(result, "User should belong to their assigned company");
        verify(userRepository).findByEmail("admin1@company1.com");
    }

    @Test
    @DisplayName("belongsToCompany should return false for user not in specified company")
    void testBelongsToCompany_UserNotInCompany_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmail("admin1@company1.com"))
            .thenReturn(Optional.of(adminUser1));
        
        // Act
        boolean result = companyService.belongsToCompany(companyId2, "admin1@company1.com");
        
        // Assert
        assertFalse(result, "User should not belong to different company");
        verify(userRepository).findByEmail("admin1@company1.com");
    }

    @Test
    @DisplayName("belongsToCompany should return false when user not found")
    void testBelongsToCompany_UserNotFound_ReturnsFalse() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@company.com"))
            .thenReturn(Optional.empty());
        
        // Act
        boolean result = companyService.belongsToCompany(companyId1, "nonexistent@company.com");
        
        // Assert
        assertFalse(result, "Should return false when user not found");
        verify(userRepository).findByEmail("nonexistent@company.com");
    }

    @Test
    @DisplayName("Security methods should handle null parameters gracefully")
    void testSecurityMethods_NullParameters_ReturnsFalse() {
        // Test isSameCompany with null parameters
        assertFalse(userService.isSameCompany(null, "admin1@company1.com"), 
                   "isSameCompany should return false for null userId");
        assertFalse(userService.isSameCompany(userId1, null), 
                   "isSameCompany should return false for null email");
        
        // Test canManageUser with null parameters
        assertFalse(userService.canManageUser(null, "admin1@company1.com"), 
                   "canManageUser should return false for null userId");
        assertFalse(userService.canManageUser(userId1, null), 
                   "canManageUser should return false for null email");
        
        // Test belongsToCompany with null parameters
        assertFalse(companyService.belongsToCompany(null, "admin1@company1.com"), 
                   "belongsToCompany should return false for null companyId");
        assertFalse(companyService.belongsToCompany(companyId1, null), 
                   "belongsToCompany should return false for null email");
    }
}