package com.cafm.cafmbackend.exception;

import com.cafm.cafmbackend.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.Set;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for GlobalExceptionHandler.
 * Tests all exception scenarios and security considerations.
 * 
 * Architecture: Unit test class for exception handling
 * Pattern: Test-driven approach with mock dependencies
 * Java 23: Modern testing with JUnit 5 and AssertJ
 * Security: Validates no sensitive information is exposed
 * Standards: Complete test coverage for all exception types
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private ErrorMessageResolver messageResolver;

    private GlobalExceptionHandler globalExceptionHandler;
    private MockHttpServletRequest request;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler(auditService, messageResolver);
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        objectMapper = new ObjectMapper();
        
        // Default message resolver behavior
        when(messageResolver.getMessage(anyString(), anyString(), any()))
            .thenAnswer(invocation -> invocation.getArgument(1)); // Return default message
    }

    @Test
    @DisplayName("Should handle authentication exceptions with proper security logging")
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException authException = new AuthenticationException("Authentication failed") {};
        when(messageResolver.getMessage(eq("error.authentication.failed"), anyString(), any()))
            .thenReturn("Authentication failed. Please check your credentials.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAuthenticationException(authException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(errorResponse.message()).isEqualTo("Authentication failed. Please check your credentials.");
        assertThat(errorResponse.path()).isEqualTo("/api/v1/test");
        assertThat(errorResponse.correlationId()).isNotNull();
        assertThat(errorResponse.timestamp()).isNotNull();
        
        // Verify no sensitive information is exposed
        assertThat(errorResponse.message()).doesNotContain("Exception");
        assertThat(errorResponse.message()).doesNotContain("stack");
    }

    @Test
    @DisplayName("Should handle access denied exceptions with audit logging")
    void shouldHandleAccessDeniedException() {
        // Given
        AccessDeniedException accessDeniedException = new AccessDeniedException("Access denied");
        when(messageResolver.getMessage(eq("error.access.denied"), anyString(), any()))
            .thenReturn("Access denied. You do not have sufficient privileges for this operation.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAccessDeniedException(accessDeniedException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("ACCESS_DENIED");
        assertThat(errorResponse.path()).isEqualTo("/api/v1/test");
        assertThat(errorResponse.correlationId()).isNotNull();
        
        // Verify security audit logging
        verify(auditService).logSecurityEvent(any(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle multi-tenant violations with critical security logging")
    void shouldHandleMultiTenantViolationException() {
        // Given
        MultiTenantViolationException tenantException = MultiTenantViolationException
            .crossTenantAccess("tenant1", "tenant2");
        when(messageResolver.getMessage(eq("error.tenant.violation"), anyString(), any()))
            .thenReturn("Access denied: Invalid tenant context");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleMultiTenantViolationException(tenantException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("TENANT_ISOLATION_VIOLATION");
        assertThat(errorResponse.message()).isEqualTo("Access denied: Invalid tenant context");
        assertThat(errorResponse.correlationId()).isNotNull();
        
        // Verify critical security audit logging
        verify(auditService).logSecurityEvent(any(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle validation exceptions with detailed field errors")
    void shouldHandleMethodArgumentNotValidException() {
        // Given
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "testObject");
        bindingResult.addError(new FieldError("testObject", "email", "invalid@", false, null, null, "Invalid email format"));
        bindingResult.addError(new FieldError("testObject", "name", "", false, null, null, "Name is required"));
        
        MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        when(messageResolver.getMessage(eq("error.validation.failed"), anyString(), any()))
            .thenReturn("Validation failed. Please check your input.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleMethodArgumentNotValidException(validationException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(errorResponse.details()).hasSize(2);
        
        // Check validation errors
        ValidationError emailError = errorResponse.details().stream()
            .filter(e -> "email".equals(e.field()))
            .findFirst()
            .orElseThrow();
        assertThat(emailError.message()).isEqualTo("Invalid email format");
        assertThat(emailError.value()).isEqualTo("invalid@");
    }

    @Test
    @DisplayName("Should handle constraint violations with field details")
    void shouldHandleConstraintViolationException() {
        // Given
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path propertyPath = mock(Path.class);
        ConstraintDescriptor<?> constraintDescriptor = mock(ConstraintDescriptor.class);
        
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.toString()).thenReturn("email");
        when(violation.getInvalidValue()).thenReturn("invalid-email");
        when(violation.getMessage()).thenReturn("Email format is invalid");
        when(violation.getConstraintDescriptor()).thenReturn((ConstraintDescriptor) constraintDescriptor);
        when(constraintDescriptor.getAnnotation()).thenReturn(mock(java.lang.annotation.Annotation.class));
        when(constraintDescriptor.getAnnotation().annotationType()).thenReturn((Class) Override.class);
        
        ConstraintViolationException constraintException = new ConstraintViolationException(Set.of(violation));
        when(messageResolver.getMessage(eq("error.constraint.violation"), anyString(), any()))
            .thenReturn("Input validation failed. Please check your data.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleConstraintViolationException(constraintException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("CONSTRAINT_VIOLATION");
        assertThat(errorResponse.details()).hasSize(1);
        
        ValidationError validationError = errorResponse.details().get(0);
        assertThat(validationError.field()).isEqualTo("email");
        assertThat(validationError.message()).isEqualTo("Email format is invalid");
    }

    @Test
    @DisplayName("Should handle custom validation exceptions")
    void shouldHandleValidationException() {
        // Given
        List<ValidationError> validationErrors = List.of(
            ValidationError.of("username", "Username already exists", "DUPLICATE_USERNAME")
        );
        ValidationException validationException = new ValidationException(
            "Username validation failed", validationErrors);
        when(messageResolver.getMessage(eq("error.validation.custom"), anyString(), any()))
            .thenReturn("Username validation failed");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationException(validationException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("VALIDATION_FAILED");
        assertThat(errorResponse.details()).hasSize(1);
        assertThat(errorResponse.details().get(0).code()).isEqualTo("DUPLICATE_USERNAME");
    }

    @Test
    @DisplayName("Should handle resource not found exceptions")
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException notFoundException = new ResourceNotFoundException("User", "id", "123");
        when(messageResolver.getMessage(eq("error.resource.not.found"), anyString(), any()))
            .thenReturn("The requested resource was not found");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleResourceNotFoundException(notFoundException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(errorResponse.message()).isEqualTo("The requested resource was not found");
    }

    @Test
    @DisplayName("Should handle duplicate resource exceptions with validation details")
    void shouldHandleDuplicateResourceException() {
        // Given
        DuplicateResourceException duplicateException = new DuplicateResourceException("User", "email", "test@example.com");
        when(messageResolver.getMessage(eq("error.resource.duplicate"), anyString(), any()))
            .thenReturn("The resource already exists");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleDuplicateResourceException(duplicateException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("RESOURCE_ALREADY_EXISTS");
        assertThat(errorResponse.details()).hasSize(1);
        
        ValidationError validationError = errorResponse.details().get(0);
        assertThat(validationError.field()).isEqualTo("email");
        assertThat(validationError.value()).isEqualTo("test@example.com");
        assertThat(validationError.code()).isEqualTo("DUPLICATE_RESOURCE");
    }

    @Test
    @DisplayName("Should handle business logic exceptions")
    void shouldHandleBusinessLogicException() {
        // Given
        BusinessLogicException businessException = BusinessLogicException
            .invalidState("WorkOrder", "123", "OPEN", "ASSIGNED");
        when(messageResolver.getMessage(eq("error.business.logic"), anyString(), any()))
            .thenReturn(businessException.getMessage());

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleBusinessLogicException(businessException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("BUSINESS_RULE_VIOLATION");
        assertThat(errorResponse.message()).contains("WorkOrder");
        assertThat(errorResponse.message()).contains("OPEN");
        assertThat(errorResponse.message()).contains("ASSIGNED");
    }

    @Test
    @DisplayName("Should handle data integrity violations with meaningful messages")
    void shouldHandleDataIntegrityViolationException() {
        // Given
        DataIntegrityViolationException dataException = new DataIntegrityViolationException(
            "unique constraint violation on email");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleDataIntegrityViolationException(dataException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("UNIQUE_CONSTRAINT_VIOLATION");
        assertThat(errorResponse.message()).isEqualTo("The provided data conflicts with existing records");
    }

    @Test
    @DisplayName("Should handle HTTP message not readable exceptions")
    void shouldHandleHttpMessageNotReadableException() {
        // Given
        HttpMessageNotReadableException messageException = new HttpMessageNotReadableException("Invalid JSON", (org.springframework.http.HttpInputMessage) null);
        when(messageResolver.getMessage(eq("error.request.format"), anyString(), any()))
            .thenReturn("Invalid request format. Please check your JSON payload.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleHttpMessageNotReadableException(messageException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("INVALID_REQUEST_FORMAT");
        assertThat(errorResponse.message()).contains("JSON");
    }

    @Test
    @DisplayName("Should handle method argument type mismatch exceptions")
    void shouldHandleMethodArgumentTypeMismatchException() {
        // Given
        MethodArgumentTypeMismatchException typeMismatchException = new MethodArgumentTypeMismatchException(
            "invalid-id", Long.class, "id", null, new NumberFormatException("Invalid number"));
        when(messageResolver.getMessage(eq("error.argument.type.mismatch"), anyString(), any()))
            .thenReturn("Invalid value for parameter 'id'. Expected type: Long");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleMethodArgumentTypeMismatchException(typeMismatchException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("INVALID_FIELD_VALUE");
        assertThat(errorResponse.details()).hasSize(1);
        
        ValidationError validationError = errorResponse.details().get(0);
        assertThat(validationError.field()).isEqualTo("id");
        assertThat(validationError.code()).isEqualTo("TYPE_MISMATCH");
    }

    @Test
    @DisplayName("Should handle generic exceptions as internal server errors with no sensitive info")
    void shouldHandleGenericException() {
        // Given
        RuntimeException genericException = new RuntimeException("Some internal error with sensitive info");
        when(messageResolver.getMessage(eq("error.internal.server"), anyString(), any()))
            .thenReturn("An unexpected error occurred. Please contact support if the problem persists.");

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleGenericException(genericException, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(errorResponse.correlationId()).isNotNull();
        
        // CRITICAL: Verify no sensitive information is exposed
        assertThat(errorResponse.message()).doesNotContain("sensitive");
        assertThat(errorResponse.message()).doesNotContain("internal error");
        assertThat(errorResponse.message()).contains("contact support");
        
        // Verify system error audit logging
        verify(auditService).logSecurityEvent(any(), anyString(), any());
    }

    @Test
    @DisplayName("Should generate unique correlation IDs for request tracing")
    void shouldGenerateUniqueCorrelationIds() {
        // Given
        RuntimeException exception1 = new RuntimeException("Error 1");
        RuntimeException exception2 = new RuntimeException("Error 2");

        // When
        ResponseEntity<ErrorResponse> response1 = globalExceptionHandler
            .handleGenericException(exception1, request);
        ResponseEntity<ErrorResponse> response2 = globalExceptionHandler
            .handleGenericException(exception2, request);

        // Then
        assertThat(response1.getBody().correlationId()).isNotNull();
        assertThat(response2.getBody().correlationId()).isNotNull();
        assertThat(response1.getBody().correlationId())
            .isNotEqualTo(response2.getBody().correlationId());
    }

    @Test
    @DisplayName("Should handle null request gracefully")
    void shouldHandleNullRequestGracefully() {
        // Given
        AuthenticationException authException = new AuthenticationException("Authentication failed") {};
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleAuthenticationException(authException, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("Should sanitize sensitive field values in validation errors")
    void shouldSanitizeSensitiveFieldValues() {
        // Given
        List<ValidationError> validationErrors = List.of(
            ValidationError.of("password", "secretPassword123", "Password is too weak", "WEAK_PASSWORD"),
            ValidationError.of("token", "jwt-token-value", "Token is invalid", "INVALID_TOKEN")
        );
        ValidationException validationException = new ValidationException(
            "Validation failed", validationErrors);

        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler
            .handleValidationException(validationException, request);

        // Then
        ErrorResponse errorResponse = response.getBody();
        assertThat(errorResponse.details()).hasSize(2);
        
        // Verify sensitive values are hidden
        ValidationError passwordError = errorResponse.details().stream()
            .filter(e -> "password".equals(e.field()))
            .findFirst()
            .orElseThrow();
        assertThat(passwordError.value()).isEqualTo("[HIDDEN]");
        
        ValidationError tokenError = errorResponse.details().stream()
            .filter(e -> "token".equals(e.field()))
            .findFirst()
            .orElseThrow();
        assertThat(tokenError.value()).isEqualTo("[HIDDEN]");
    }
}