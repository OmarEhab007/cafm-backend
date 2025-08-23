package com.cafm.cafmbackend;

import com.cafm.cafmbackend.exception.ResourceNotFoundException;
import com.cafm.cafmbackend.exception.BusinessLogicException;
import com.cafm.cafmbackend.exception.MultiTenantViolationException;
import com.cafm.cafmbackend.exception.ValidationException;
import com.cafm.cafmbackend.exception.ValidationError;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

/**
 * Test controller for triggering exception scenarios in tests.
 * Only active in test profile for security.
 */
@RestController
@RequestMapping("/api/v1/test")
@Profile("test")
public class TestController {

    @GetMapping("/trigger-error")
    public String triggerError() {
        throw new RuntimeException("This is a test runtime exception with sensitive information");
    }

    @GetMapping("/not-found")
    public String notFound() {
        throw new ResourceNotFoundException("TestResource", "id", "123");
    }

    @GetMapping("/business-error")
    public String businessError() {
        throw BusinessLogicException.invalidState("WorkOrder", "123", "OPEN", "ASSIGNED");
    }

    @GetMapping("/tenant-violation")
    public String tenantViolation() {
        throw MultiTenantViolationException.crossTenantAccess("tenant1", "tenant2");
    }

    @GetMapping("/validation-error")
    public String validationError() {
        throw new ValidationException("Validation failed", List.of(
            ValidationError.of("email", "invalid@email", "Email format is invalid", "INVALID_EMAIL"),
            ValidationError.of("password", "secretPassword123", "Password too weak", "WEAK_PASSWORD")
        ));
    }

    @GetMapping("/data-integrity")
    public String dataIntegrity() {
        throw new DataIntegrityViolationException("unique constraint violation on email column");
    }

    @PostMapping("/validate")
    public String validate(@Valid @RequestBody TestRequest request) {
        return "Valid request: " + request.email();
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "Admin access granted";
    }

    @GetMapping("/user/{id}")
    public String getUser(@PathVariable UUID id) {
        return "User ID: " + id;
    }

    public record TestRequest(
        @NotNull @Email String email,
        @NotNull String name
    ) {}
}