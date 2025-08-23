package com.cafm.cafmbackend.integration.controllers;

import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.dto.user.*;
import com.cafm.cafmbackend.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for UserController.
 * Tests CRUD operations, user management, and authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("cafm_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private static final String BASE_URL = "/api/v1/users";
    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final UUID TEST_COMPANY_ID = UUID.randomUUID();

    // ========== GET All Users Tests ==========

    @Test
    @Order(1)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get all users with pagination")
    void testGetAllUsers_Success() throws Exception {
        // Arrange
        List<UserResponseSimplified> userList = Arrays.asList(
                createTestUser("user1@example.com", "User", "One"),
                createTestUser("user2@example.com", "User", "Two")
        );
        Page<UserResponseSimplified> page = new PageImpl<>(userList, PageRequest.of(0, 20), 2);

        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].email").value("user1@example.com"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Order(2)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should allow supervisor to get users")
    void testGetAllUsers_AsSupervisor() throws Exception {
        // Arrange
        Page<UserResponseSimplified> page = new PageImpl<>(Collections.emptyList());
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    @WithMockUser(roles = "TECHNICIAN")
    @DisplayName("Should deny technician access to user list")
    void testGetAllUsers_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should search users by name or email")
    void testSearchUsers_Success() throws Exception {
        // Arrange
        List<UserResponseSimplified> searchResults = Arrays.asList(
                createTestUser("john@example.com", "John", "Doe")
        );
        Page<UserResponseSimplified> page = new PageImpl<>(searchResults);

        when(userService.searchUsers(eq("john"), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("search", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    @Order(5)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter users by type")
    void testGetUsersByType_Success() throws Exception {
        // Arrange
        List<UserResponseSimplified> supervisors = Arrays.asList(
                createTestUser("supervisor@example.com", "Super", "Visor")
        );
        Page<UserResponseSimplified> page = new PageImpl<>(supervisors);

        when(userService.getUsersByType(eq(UserType.SUPERVISOR), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("userType", "SUPERVISOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    @Order(6)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should filter users by status")
    void testGetUsersByStatus_Success() throws Exception {
        // Arrange
        List<UserResponseSimplified> activeUsers = Arrays.asList(
                createTestUser("active@example.com", "Active", "User")
        );
        Page<UserResponseSimplified> page = new PageImpl<>(activeUsers);

        when(userService.getUsersByStatus(eq(UserStatus.ACTIVE), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    // ========== GET User by ID Tests ==========

    @Test
    @Order(7)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get user by ID as admin")
    void testGetUserById_AsAdmin() throws Exception {
        // Arrange
        UserResponseSimplified user = createTestUser("test@example.com", "Test", "User");
        when(userService.getUserById(TEST_USER_ID)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TEST_USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @Order(8)
    @WithMockUser(roles = "SUPERVISOR", username = "supervisor@example.com")
    @DisplayName("Should get user by ID as supervisor from same company")
    void testGetUserById_AsSupervisor_SameCompany() throws Exception {
        // Arrange
        UserResponseSimplified user = createTestUser("test@example.com", "Test", "User");
        when(userService.isSameCompany(eq(TEST_USER_ID), eq("supervisor@example.com"))).thenReturn(true);
        when(userService.getUserById(TEST_USER_ID)).thenReturn(user);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + TEST_USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    @Order(9)
    @WithMockUser(roles = "SUPERVISOR", username = "supervisor@example.com")
    @DisplayName("Should deny supervisor access to user from different company")
    void testGetUserById_AsSupervisor_DifferentCompany() throws Exception {
        // Arrange
        when(userService.isSameCompany(eq(TEST_USER_ID), eq("supervisor@example.com"))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + TEST_USER_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 for non-existent user")
    void testGetUserById_NotFound() throws Exception {
        // Arrange
        when(userService.getUserById(any(UUID.class)))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()))
                .andExpect(status().is5xxServerError());
    }

    // ========== CREATE User Tests ==========

    @Test
    @Order(11)
    @WithMockUser(roles = "ADMIN", username = "admin@example.com")
    @DisplayName("Should create new user as admin")
    void testCreateUser_Success() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser@example.com",
                "Password123!",
                "New",
                "User",
                UserType.TECHNICIAN,
                TEST_COMPANY_ID,
                null
        );

        UserResponseSimplified createdUser = createTestUser("newuser@example.com", "New", "User");
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(createdUser);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    @Order(12)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should deny supervisor from creating users")
    void testCreateUser_AccessDenied() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "newuser@example.com",
                "Password123!",
                "New",
                "User",
                UserType.TECHNICIAN,
                TEST_COMPANY_ID,
                null
        );

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(13)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should fail to create user with duplicate email")
    void testCreateUser_DuplicateEmail() throws Exception {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(
                "existing@example.com",
                "Password123!",
                "New",
                "User",
                UserType.TECHNICIAN,
                TEST_COMPANY_ID,
                null
        );

        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().is5xxServerError());
    }

    // ========== UPDATE User Tests ==========

    @Test
    @Order(14)
    @WithMockUser(roles = "ADMIN", username = "admin@example.com")
    @DisplayName("Should update user as admin")
    void testUpdateUser_AsAdmin() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(
                "Updated",
                "User",
                "1234567890",
                null,
                null
        );

        UserResponseSimplified updatedUser = createTestUser("test@example.com", "Updated", "User");
        when(userService.updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @Order(15)
    @WithMockUser(roles = "SUPERVISOR", username = "supervisor@example.com")
    @DisplayName("Should update user as supervisor with permission")
    void testUpdateUser_AsSupervisor() throws Exception {
        // Arrange
        UserUpdateRequest request = new UserUpdateRequest(
                "Updated",
                "User",
                "1234567890",
                null,
                null
        );

        when(userService.canManageUser(eq(TEST_USER_ID), eq("supervisor@example.com"))).thenReturn(true);
        UserResponseSimplified updatedUser = createTestUser("test@example.com", "Updated", "User");
        when(userService.updateUser(eq(TEST_USER_ID), any(UserUpdateRequest.class))).thenReturn(updatedUser);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/" + TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== DELETE User Tests ==========

    @Test
    @Order(16)
    @WithMockUser(roles = "ADMIN", username = "admin@example.com")
    @DisplayName("Should delete user as admin")
    void testDeleteUser_Success() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(TEST_USER_ID);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + TEST_USER_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(TEST_USER_ID);
    }

    @Test
    @Order(17)
    @WithMockUser(roles = "SUPERVISOR")
    @DisplayName("Should deny supervisor from deleting users")
    void testDeleteUser_AccessDenied() throws Exception {
        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/" + TEST_USER_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ========== User Status Management Tests ==========

    @Test
    @Order(18)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should activate user")
    void testActivateUser_Success() throws Exception {
        // Arrange
        UserResponseSimplified activatedUser = createTestUser("test@example.com", "Test", "User");
        when(userService.activateUser(TEST_USER_ID)).thenReturn(activatedUser);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/activate")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(19)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should deactivate user")
    void testDeactivateUser_Success() throws Exception {
        // Arrange
        UserResponseSimplified deactivatedUser = createTestUser("test@example.com", "Test", "User");
        when(userService.deactivateUser(TEST_USER_ID)).thenReturn(deactivatedUser);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/deactivate")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(20)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should lock user account")
    void testLockUser_Success() throws Exception {
        // Arrange
        UserResponseSimplified lockedUser = createTestUser("test@example.com", "Test", "User");
        when(userService.lockUser(eq(TEST_USER_ID), anyString())).thenReturn(lockedUser);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/lock")
                        .param("reason", "Security violation")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(21)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should unlock user account")
    void testUnlockUser_Success() throws Exception {
        // Arrange
        UserResponseSimplified unlockedUser = createTestUser("test@example.com", "Test", "User");
        when(userService.unlockUser(TEST_USER_ID)).thenReturn(unlockedUser);

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/unlock")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    // ========== Password Reset Tests ==========

    @Test
    @Order(22)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should reset user password as admin")
    void testResetUserPassword_Success() throws Exception {
        // Arrange
        when(userService.resetUserPassword(TEST_USER_ID)).thenReturn("TempPass123!");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/reset-password")
                        .param("sendEmail", "false")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("TempPass123!"))
                .andExpect(jsonPath("$.emailSent").value(false));
    }

    @Test
    @Order(23)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should reset password and send email")
    void testResetUserPassword_WithEmail() throws Exception {
        // Arrange
        when(userService.resetUserPassword(TEST_USER_ID)).thenReturn("TempPass123!");

        // Act & Assert
        mockMvc.perform(post(BASE_URL + "/" + TEST_USER_ID + "/reset-password")
                        .param("sendEmail", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset email sent"))
                .andExpect(jsonPath("$.emailSent").value(true));
    }

    // ========== Company Users Tests ==========

    @Test
    @Order(24)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should get users by company as admin")
    void testGetUsersByCompany_AsAdmin() throws Exception {
        // Arrange
        List<UserResponseSimplified> companyUsers = Arrays.asList(
                createTestUser("user1@company.com", "User", "One"),
                createTestUser("user2@company.com", "User", "Two")
        );
        Page<UserResponseSimplified> page = new PageImpl<>(companyUsers);

        when(userService.getUsersByCompany(eq(TEST_COMPANY_ID), any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/company/" + TEST_COMPANY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    // ========== Profile Tests ==========

    @Test
    @Order(25)
    @WithMockUser(username = "user@example.com")
    @DisplayName("Should get current user profile")
    void testGetMyProfile_Success() throws Exception {
        // Arrange
        UserResponseSimplified profile = createTestUser("user@example.com", "Current", "User");
        when(userService.getUserByEmail("user@example.com")).thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @Order(26)
    @WithMockUser(username = "user@example.com")
    @DisplayName("Should update current user profile")
    void testUpdateMyProfile_Success() throws Exception {
        // Arrange
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "Updated",
                "Name",
                "9876543210",
                null
        );

        UserResponseSimplified updatedProfile = createTestUser("user@example.com", "Updated", "Name");
        when(userService.updateUserProfile(eq("user@example.com"), any(UserProfileUpdateRequest.class)))
                .thenReturn(updatedProfile);

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @Order(27)
    @DisplayName("Should require authentication for profile access")
    void testGetMyProfile_Unauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ========== Validation Tests ==========

    @Test
    @Order(28)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate email format in create request")
    void testCreateUser_InvalidEmail() throws Exception {
        // Arrange
        String invalidRequest = """
                {
                    "email": "invalid-email",
                    "password": "Password123!",
                    "firstName": "Test",
                    "lastName": "User",
                    "userType": "TECHNICIAN",
                    "companyId": "%s"
                }
                """.formatted(TEST_COMPANY_ID);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(29)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate password strength")
    void testCreateUser_WeakPassword() throws Exception {
        // Arrange
        String weakPasswordRequest = """
                {
                    "email": "test@example.com",
                    "password": "weak",
                    "firstName": "Test",
                    "lastName": "User",
                    "userType": "TECHNICIAN",
                    "companyId": "%s"
                }
                """.formatted(TEST_COMPANY_ID);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(weakPasswordRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(30)
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should validate required fields")
    void testCreateUser_MissingRequiredFields() throws Exception {
        // Arrange
        String incompleteRequest = """
                {
                    "email": "test@example.com"
                }
                """;

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incompleteRequest)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    // ========== Helper Methods ==========

    private UserResponseSimplified createTestUser(String email, String firstName, String lastName) {
        return new UserResponseSimplified(
                TEST_USER_ID,
                email,
                firstName,
                lastName,
                UserType.TECHNICIAN,
                TEST_COMPANY_ID,
                "Test Company",
                UserStatus.ACTIVE,
                false,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}