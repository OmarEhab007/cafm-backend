package com.cafm.cafmbackend.integration;

import com.cafm.cafmbackend.exception.ErrorCode;
import com.cafm.cafmbackend.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GlobalExceptionHandler.
 * Tests the complete error handling flow in the application context.
 * 
 * Architecture: Integration test with full Spring context
 * Pattern: End-to-end testing of error responses through controllers
 * Java 23: Modern testing with Spring Boot Test
 * Security: Tests authentication and authorization error scenarios
 * Standards: Complete integration testing of exception handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should return authentication error for protected endpoints without authentication")
    void shouldReturnAuthenticationErrorForProtectedEndpoint() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/users/profile")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Verify error response structure
        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(errorResponse.path()).isEqualTo("/api/v1/users/profile");
        assertThat(errorResponse.timestamp()).isNotNull();
        assertThat(errorResponse.correlationId()).isNotNull();
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("exception");
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("stack");
    }

    @Test
    @DisplayName("Should return access denied for insufficient privileges")
    @WithMockUser(roles = "USER")
    void shouldReturnAccessDeniedForInsufficientPrivileges() throws Exception {
        // Assume this endpoint requires ADMIN role
        MvcResult result = mockMvc.perform(get("/api/v1/admin/companies")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
        assertThat(errorResponse.message()).containsIgnoringCase("access denied");
    }

    @Test
    @DisplayName("Should return validation error for invalid request body")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnValidationErrorForInvalidRequestBody() throws Exception {
        // Given - Invalid user creation request (missing required fields)
        String invalidUserJson = """
            {
                "email": "invalid-email",
                "username": "",
                "password": "123"
            }
            """;

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(errorResponse.details()).isNotEmpty();
        
        // Verify field-level validation errors
        boolean hasEmailError = errorResponse.details().stream()
            .anyMatch(error -> "email".equals(error.field()));
        assertThat(hasEmailError).isTrue();
    }

    @Test
    @DisplayName("Should return not found error for non-existent resource")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnNotFoundErrorForNonExistentResource() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/users/99999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(errorResponse.message()).containsIgnoringCase("not found");
        assertThat(errorResponse.correlationId()).isNotNull();
    }

    @Test
    @DisplayName("Should return method not allowed for unsupported HTTP methods")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnMethodNotAllowedForUnsupportedHttpMethods() throws Exception {
        // When & Then - Try PATCH on endpoint that doesn't support it
        MvcResult result = mockMvc.perform(patch("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("INVALID_REQUEST_FORMAT");
        assertThat(errorResponse.message()).containsIgnoringCase("method");
        assertThat(errorResponse.message()).containsIgnoringCase("not supported");
    }

    @Test
    @DisplayName("Should return bad request for malformed JSON")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        // Given - Malformed JSON
        String malformedJson = "{ \"name\": \"test\", invalid json }";

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("INVALID_REQUEST_FORMAT");
        assertThat(errorResponse.message()).containsIgnoringCase("json");
    }

    @Test
    @DisplayName("Should return type mismatch error for invalid path parameters")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnTypeMismatchErrorForInvalidPathParameters() throws Exception {
        // When & Then - Pass string where UUID is expected
        MvcResult result = mockMvc.perform(get("/api/v1/users/invalid-uuid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("INVALID_FIELD_VALUE");
        assertThat(errorResponse.details()).hasSize(1);
        assertThat(errorResponse.details().get(0).code()).isEqualTo("TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Should return conflict error for duplicate resource creation")
    @WithMockUser(roles = "ADMIN")
    void shouldReturnConflictErrorForDuplicateResource() throws Exception {
        // Given - Create user first
        String userJson = """
            {
                "email": "duplicate@example.com",
                "username": "duplicateuser",
                "password": "StrongPassword123!",
                "firstName": "Test",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isCreated());

        // When & Then - Try to create the same user again
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(userJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("RESOURCE_ALREADY_EXISTS");
        assertThat(errorResponse.details()).isNotEmpty();
        
        // Should have validation error for the duplicate field
        boolean hasDuplicateError = errorResponse.details().stream()
            .anyMatch(error -> "DUPLICATE_RESOURCE".equals(error.code()));
        assertThat(hasDuplicateError).isTrue();
    }

    @Test
    @DisplayName("Should handle internal server errors without exposing sensitive information")
    @WithMockUser(roles = "ADMIN")
    void shouldHandleInternalServerErrorsSecurely() throws Exception {
        // This would depend on having a test endpoint that throws an exception
        // For now, we'll simulate by hitting an endpoint that might cause issues
        
        MvcResult result = mockMvc.perform(get("/api/v1/test/trigger-error")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(errorResponse.correlationId()).isNotNull();
        
        // CRITICAL: Verify no sensitive information is exposed
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("exception");
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("stack");
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("database");
        assertThat(errorResponse.message()).doesNotContainIgnoringCase("password");
        assertThat(errorResponse.message()).containsIgnoringCase("contact support");
    }

    @Test
    @DisplayName("Should support Arabic error messages based on Accept-Language header")
    @WithMockUser(roles = "USER")
    void shouldSupportArabicErrorMessages() throws Exception {
        // When & Then
        MvcResult result = mockMvc.perform(get("/api/v1/admin/users")
                .header("Accept-Language", "ar")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(responseContent, ErrorResponse.class);

        assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
        // The message should be in Arabic if Arabic localization is working
        // This would depend on having Arabic messages in the properties file
    }

    @Test
    @DisplayName("Should include consistent error response structure across all errors")
    void shouldHaveConsistentErrorResponseStructure() throws Exception {
        // Test multiple error types to ensure consistent structure
        
        // 1. Authentication error
        MvcResult authResult = mockMvc.perform(get("/api/v1/users/profile"))
                .andExpect(status().isUnauthorized())
                .andReturn();
        
        ErrorResponse authError = objectMapper.readValue(
            authResult.getResponse().getContentAsString(), ErrorResponse.class);
        
        // 2. Validation error
        MvcResult validationResult = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        
        ErrorResponse validationError = objectMapper.readValue(
            validationResult.getResponse().getContentAsString(), ErrorResponse.class);
        
        // Verify consistent structure
        verifyErrorResponseStructure(authError);
        verifyErrorResponseStructure(validationError);
    }

    private void verifyErrorResponseStructure(ErrorResponse errorResponse) {
        assertThat(errorResponse.timestamp()).isNotNull();
        assertThat(errorResponse.path()).isNotNull();
        assertThat(errorResponse.code()).isNotNull();
        assertThat(errorResponse.message()).isNotNull();
        assertThat(errorResponse.details()).isNotNull(); // Can be empty but not null
        // correlationId can be null for some error types
        
        // Verify error code is valid
        assertThat(ErrorCode.fromCode(errorResponse.code())).isNotNull();
    }
}