package com.cafm.cafmbackend.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 * Tests exception creation, factory methods, and context information.
 * 
 * Architecture: Unit test class for custom exceptions
 * Pattern: Test-driven approach for exception behavior
 * Java 23: Modern testing with factory method validation
 * Security: Tests tenant isolation context in exceptions
 * Standards: Complete test coverage for custom exceptions
 */
@DisplayName("Custom Exception Tests")
class CustomExceptionTests {

    @Test
    @DisplayName("Should create MultiTenantViolationException with basic information")
    void shouldCreateMultiTenantViolationExceptionWithBasicInfo() {
        // Given
        String message = "Tenant violation occurred";

        // When
        MultiTenantViolationException exception = new MultiTenantViolationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getUserTenantId()).isNull();
        assertThat(exception.getRequestedTenantId()).isNull();
        assertThat(exception.getResourceType()).isNull();
        assertThat(exception.getResourceId()).isNull();
    }

    @Test
    @DisplayName("Should create MultiTenantViolationException with tenant context")
    void shouldCreateMultiTenantViolationExceptionWithTenantContext() {
        // Given
        String message = "Cross-tenant access violation";
        String userTenantId = "tenant-1";
        String requestedTenantId = "tenant-2";

        // When
        MultiTenantViolationException exception = new MultiTenantViolationException(
            message, userTenantId, requestedTenantId);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getUserTenantId()).isEqualTo(userTenantId);
        assertThat(exception.getRequestedTenantId()).isEqualTo(requestedTenantId);
    }

    @Test
    @DisplayName("Should create MultiTenantViolationException with full context")
    void shouldCreateMultiTenantViolationExceptionWithFullContext() {
        // Given
        String message = "Resource access violation";
        String userTenantId = "tenant-1";
        String requestedTenantId = "tenant-2";
        String resourceType = "Report";
        String resourceId = "report-123";

        // When
        MultiTenantViolationException exception = new MultiTenantViolationException(
            message, userTenantId, requestedTenantId, resourceType, resourceId);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getUserTenantId()).isEqualTo(userTenantId);
        assertThat(exception.getRequestedTenantId()).isEqualTo(requestedTenantId);
        assertThat(exception.getResourceType()).isEqualTo(resourceType);
        assertThat(exception.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    @DisplayName("Should create cross-tenant access violation using factory method")
    void shouldCreateCrossTenantAccessViolationUsingFactory() {
        // Given
        String userTenantId = "tenant-1";
        String requestedTenantId = "tenant-2";

        // When
        MultiTenantViolationException exception = MultiTenantViolationException
            .crossTenantAccess(userTenantId, requestedTenantId);

        // Then
        assertThat(exception.getMessage()).contains("Cross-tenant access denied");
        assertThat(exception.getMessage()).contains(userTenantId);
        assertThat(exception.getMessage()).contains(requestedTenantId);
        assertThat(exception.getUserTenantId()).isEqualTo(userTenantId);
        assertThat(exception.getRequestedTenantId()).isEqualTo(requestedTenantId);
    }

    @Test
    @DisplayName("Should create resource access violation using factory method")
    void shouldCreateResourceAccessViolationUsingFactory() {
        // Given
        String userTenantId = "tenant-1";
        String requestedTenantId = "tenant-2";
        String resourceType = "WorkOrder";
        String resourceId = "wo-456";

        // When
        MultiTenantViolationException exception = MultiTenantViolationException
            .resourceAccess(userTenantId, requestedTenantId, resourceType, resourceId);

        // Then
        assertThat(exception.getMessage()).contains("Tenant isolation violation");
        assertThat(exception.getMessage()).contains(resourceType);
        assertThat(exception.getMessage()).contains(resourceId);
        assertThat(exception.getResourceType()).isEqualTo(resourceType);
        assertThat(exception.getResourceId()).isEqualTo(resourceId);
    }

    @Test
    @DisplayName("Should create missing tenant context violation using factory method")
    void shouldCreateMissingTenantContextViolation() {
        // When
        MultiTenantViolationException exception = MultiTenantViolationException
            .missingTenantContext();

        // Then
        assertThat(exception.getMessage()).contains("Tenant context is required");
    }

    @Test
    @DisplayName("Should create BusinessLogicException with basic information")
    void shouldCreateBusinessLogicExceptionWithBasicInfo() {
        // Given
        String message = "Business rule violated";

        // When
        BusinessLogicException exception = new BusinessLogicException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getBusinessRule()).isNull();
        assertThat(exception.getEntityType()).isNull();
        assertThat(exception.getEntityId()).isNull();
    }

    @Test
    @DisplayName("Should create BusinessLogicException with business rule context")
    void shouldCreateBusinessLogicExceptionWithBusinessRuleContext() {
        // Given
        String message = "Inventory constraint violated";
        String businessRule = "INSUFFICIENT_INVENTORY";

        // When
        BusinessLogicException exception = new BusinessLogicException(message, businessRule);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getBusinessRule()).isEqualTo(businessRule);
    }

    @Test
    @DisplayName("Should create invalid state exception using factory method")
    void shouldCreateInvalidStateExceptionUsingFactory() {
        // Given
        String entityType = "WorkOrder";
        String entityId = "wo-123";
        String currentState = "OPEN";
        String requiredState = "ASSIGNED";

        // When
        BusinessLogicException exception = BusinessLogicException
            .invalidState(entityType, entityId, currentState, requiredState);

        // Then
        assertThat(exception.getMessage()).contains(entityType);
        assertThat(exception.getMessage()).contains(entityId);
        assertThat(exception.getMessage()).contains(currentState);
        assertThat(exception.getMessage()).contains(requiredState);
        assertThat(exception.getBusinessRule()).isEqualTo("INVALID_OPERATION_STATE");
        assertThat(exception.getEntityType()).isEqualTo(entityType);
        assertThat(exception.getEntityId()).isEqualTo(entityId);
    }

    @Test
    @DisplayName("Should create work order assignment failure using factory method")
    void shouldCreateWorkOrderAssignmentFailure() {
        // Given
        String workOrderId = "wo-456";
        String reason = "No available technicians";

        // When
        BusinessLogicException exception = BusinessLogicException
            .workOrderAssignmentFailed(workOrderId, reason);

        // Then
        assertThat(exception.getMessage()).contains("Cannot assign work order");
        assertThat(exception.getMessage()).contains(workOrderId);
        assertThat(exception.getMessage()).contains(reason);
        assertThat(exception.getBusinessRule()).isEqualTo("WORK_ORDER_ASSIGNMENT_FAILED");
    }

    @Test
    @DisplayName("Should create insufficient inventory exception using factory method")
    void shouldCreateInsufficientInventoryException() {
        // Given
        String itemId = "item-789";
        int requested = 10;
        int available = 3;

        // When
        BusinessLogicException exception = BusinessLogicException
            .insufficientInventory(itemId, requested, available);

        // Then
        assertThat(exception.getMessage()).contains("Insufficient inventory");
        assertThat(exception.getMessage()).contains(itemId);
        assertThat(exception.getMessage()).contains(String.valueOf(requested));
        assertThat(exception.getMessage()).contains(String.valueOf(available));
        assertThat(exception.getBusinessRule()).isEqualTo("INSUFFICIENT_INVENTORY");
    }

    @Test
    @DisplayName("Should create invalid date range exception using factory method")
    void shouldCreateInvalidDateRangeException() {
        // Given
        String startDate = "2024-12-01";
        String endDate = "2024-11-01";

        // When
        BusinessLogicException exception = BusinessLogicException
            .invalidDateRange(startDate, endDate);

        // Then
        assertThat(exception.getMessage()).contains("Invalid date range");
        assertThat(exception.getMessage()).contains(startDate);
        assertThat(exception.getMessage()).contains(endDate);
        assertThat(exception.getBusinessRule()).isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    @DisplayName("Should create ValidationException with basic information")
    void shouldCreateValidationExceptionWithBasicInfo() {
        // Given
        String message = "Validation failed";

        // When
        ValidationException exception = new ValidationException(message);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getValidationErrors()).isEmpty();
        assertThat(exception.getFieldName()).isNull();
        assertThat(exception.getFieldValue()).isNull();
    }

    @Test
    @DisplayName("Should create ValidationException with field context")
    void shouldCreateValidationExceptionWithFieldContext() {
        // Given
        String message = "Email format is invalid";
        String fieldName = "email";
        String fieldValue = "invalid-email";

        // When
        ValidationException exception = new ValidationException(message, fieldName, fieldValue);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getFieldName()).isEqualTo(fieldName);
        assertThat(exception.getFieldValue()).isEqualTo(fieldValue);
        assertThat(exception.getValidationErrors()).hasSize(1);
        
        ValidationError error = exception.getValidationErrors().get(0);
        assertThat(error.field()).isEqualTo(fieldName);
        assertThat(error.message()).isEqualTo(message);
    }

    @Test
    @DisplayName("Should create field validation exception using factory method")
    void shouldCreateFieldValidationExceptionUsingFactory() {
        // Given
        String fieldName = "username";
        String fieldValue = "ab";
        String message = "Username must be at least 3 characters";

        // When
        ValidationException exception = ValidationException
            .fieldValidation(fieldName, fieldValue, message);

        // Then
        assertThat(exception.getMessage()).contains("Validation failed for field");
        assertThat(exception.getMessage()).contains(fieldName);
        assertThat(exception.getFieldName()).isEqualTo(fieldName);
        assertThat(exception.getFieldValue()).isEqualTo(fieldValue);
    }

    @Test
    @DisplayName("Should create cross-field validation exception using factory method")
    void shouldCreateCrossFieldValidationExceptionUsingFactory() {
        // Given
        List<String> fields = List.of("startDate", "endDate");
        String message = "Start date must be before end date";

        // When
        ValidationException exception = ValidationException
            .crossFieldValidation(fields, message);

        // Then
        assertThat(exception.getMessage()).contains("Cross-field validation failed");
        assertThat(exception.getMessage()).contains("startDate, endDate");
        assertThat(exception.getValidationErrors()).hasSize(2);
        
        boolean hasStartDateError = exception.getValidationErrors().stream()
            .anyMatch(error -> "startDate".equals(error.field()));
        boolean hasEndDateError = exception.getValidationErrors().stream()
            .anyMatch(error -> "endDate".equals(error.field()));
        
        assertThat(hasStartDateError).isTrue();
        assertThat(hasEndDateError).isTrue();
    }

    @Test
    @DisplayName("Should create business rule validation exception using factory method")
    void shouldCreateBusinessRuleValidationExceptionUsingFactory() {
        // Given
        String fieldName = "workOrderStatus";
        String businessRule = "INVALID_STATE_TRANSITION";
        String message = "Cannot transition from OPEN to COMPLETED";

        // When
        ValidationException exception = ValidationException
            .businessRule(fieldName, businessRule, message);

        // Then
        assertThat(exception.getMessage()).contains("Business validation failed");
        assertThat(exception.getMessage()).contains(fieldName);
        assertThat(exception.getValidationErrors()).hasSize(1);
        
        ValidationError error = exception.getValidationErrors().get(0);
        assertThat(error.field()).isEqualTo(fieldName);
        assertThat(error.code()).isEqualTo(businessRule);
    }

    @Test
    @DisplayName("Should create unique constraint validation exception using factory method")
    void shouldCreateUniqueConstraintValidationExceptionUsingFactory() {
        // Given
        String fieldName = "email";
        String fieldValue = "test@example.com";
        String entityType = "User";

        // When
        ValidationException exception = ValidationException
            .uniqueConstraint(fieldName, fieldValue, entityType);

        // Then
        assertThat(exception.getMessage()).contains("already exists");
        assertThat(exception.getMessage()).contains(entityType);
        assertThat(exception.getMessage()).contains(fieldName);
        assertThat(exception.getValidationErrors()).hasSize(1);
        
        ValidationError error = exception.getValidationErrors().get(0);
        assertThat(error.field()).isEqualTo(fieldName);
        assertThat(error.value()).isEqualTo(fieldValue);
        assertThat(error.code()).isEqualTo("UNIQUE_CONSTRAINT_VIOLATION");
    }

    @Test
    @DisplayName("Should detect multiple validation errors correctly")
    void shouldDetectMultipleValidationErrorsCorrectly() {
        // Given
        List<ValidationError> errors = List.of(
            ValidationError.of("email", "Invalid email format"),
            ValidationError.of("username", "Username too short")
        );
        ValidationException exception = new ValidationException("Multiple validation errors", errors);

        // When & Then
        assertThat(exception.hasMultipleErrors()).isTrue();
    }

    @Test
    @DisplayName("Should detect single validation error correctly")
    void shouldDetectSingleValidationErrorCorrectly() {
        // Given
        ValidationException exception = new ValidationException("Single error", "email", "invalid");

        // When & Then
        assertThat(exception.hasMultipleErrors()).isFalse();
    }

    @Test
    @DisplayName("Should handle exception chaining correctly")
    void shouldHandleExceptionChainingCorrectly() {
        // Given
        RuntimeException cause = new RuntimeException("Root cause");
        String message = "Validation failed due to system error";

        // When
        ValidationException exception = new ValidationException(message, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}