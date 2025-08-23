package com.cafm.cafmbackend.validation;

import com.cafm.cafmbackend.data.entity.Company;
import com.cafm.cafmbackend.data.entity.User;
import com.cafm.cafmbackend.data.entity.base.TenantAware;
import com.cafm.cafmbackend.service.tenant.TenantContextService;
import com.cafm.cafmbackend.validation.aspect.TenantValidationAspect;
import com.cafm.cafmbackend.validation.constraint.TenantValidated;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Tenant Validation Aspect.
 * 
 * Explanation:
 * - Purpose: Validates AOP-based tenant security annotations work correctly
 * - Pattern: Aspect testing with comprehensive security scenario coverage
 * - Java 23: Modern AOP testing with security validation patterns
 * - Architecture: Cross-cutting concern testing for declarative security
 * - Standards: Zero-tolerance testing for tenant boundary violations
 */
@ExtendWith(MockitoExtension.class)
class TenantValidationAspectTest {
    
    @Mock
    private TenantContextService tenantContextService;
    
    @Mock
    private JoinPoint joinPoint;
    
    @Mock
    private MethodSignature methodSignature;
    
    @Mock
    private Method method;
    
    @Mock
    private TenantAware mockEntity;
    
    @Mock
    private Company mockCompany;
    
    private TenantValidationAspect aspect;
    
    private static final UUID COMPANY1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID COMPANY2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SYSTEM_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    
    @BeforeEach
    void setUp() {
        aspect = new TenantValidationAspect();
        // Use reflection to inject the mock service
        try {
            var field = TenantValidationAspect.class.getDeclaredField("tenantContextService");
            field.setAccessible(true);
            field.set(aspect, tenantContextService);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock service", e);
        }
    }
    
    @Test
    @DisplayName("SECURITY: Require tenant context validation passes with valid context")
    void testRequireTenantContextValid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.REQUIRE_TENANT_CONTEXT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true  // validateTenantStatus
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should not throw
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    @Test
    @DisplayName("SECURITY: Require tenant context validation fails without context")
    void testRequireTenantContextInvalid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.REQUIRE_TENANT_CONTEXT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true  // validateTenantStatus
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(false);
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should throw SecurityException
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> aspect.validateTenantAccess(joinPoint, annotation));
        
        assertTrue(exception.getMessage().contains("No tenant context available"));
    }
    
    @Test
    @DisplayName("SECURITY: System tenant is allowed when configured")
    void testSystemTenantAllowed() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.REQUIRE_TENANT_CONTEXT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true  // validateTenantStatus
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(SYSTEM_TENANT_ID);
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should not throw (system tenant allowed)
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
        
        // Verify tenant status validation was skipped for system tenant
        verify(tenantContextService, never()).validateCurrentTenantAccess();
    }
    
    @Test
    @DisplayName("SECURITY: Entity tenant validation passes for correct tenant")
    void testEntityTenantValidationValid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.VALIDATE_ENTITY_TENANT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            "entity" // entityParam
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        // Mock entity belonging to correct tenant
        when(mockEntity.validateTenantAccess(COMPANY1_ID)).thenReturn(true);
        
        setupJoinPoint("testMethod", new Object[]{mockEntity}, "entity");
        
        // Execute - should not throw
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    @Test
    @DisplayName("SECURITY: Entity tenant validation fails for wrong tenant")
    void testEntityTenantValidationInvalid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.VALIDATE_ENTITY_TENANT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            "entity" // entityParam
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        // Mock entity belonging to different tenant
        when(mockEntity.validateTenantAccess(COMPANY1_ID)).thenReturn(false);
        when(mockEntity.getCompanyId()).thenReturn(COMPANY2_ID);
        
        setupJoinPoint("testMethod", new Object[]{mockEntity}, "entity");
        
        // Execute - should throw SecurityException
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> aspect.validateTenantAccess(joinPoint, annotation));
        
        assertTrue(exception.getMessage().contains("Entity access denied"));
    }
    
    @Test
    @DisplayName("SECURITY: Tenant ID validation passes for matching tenant")
    void testTenantIdValidationValid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.VALIDATE_TENANT_ID,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            "tenantId" // tenantIdParam
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        setupJoinPoint("testMethod", new Object[]{COMPANY1_ID}, "tenantId");
        
        // Execute - should not throw
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    @Test
    @DisplayName("SECURITY: Tenant ID validation fails for mismatched tenant")
    void testTenantIdValidationInvalid() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.VALIDATE_TENANT_ID,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            "tenantId" // tenantIdParam
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        setupJoinPoint("testMethod", new Object[]{COMPANY2_ID}, "tenantId");
        
        // Execute - should throw SecurityException
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> aspect.validateTenantAccess(joinPoint, annotation));
        
        assertTrue(exception.getMessage().contains("Tenant ID mismatch"));
    }
    
    @Test
    @DisplayName("SECURITY: Write access validation requires active tenant")
    void testWriteAccessValidation() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.WRITE_ACCESS,
            true, // requireTenantContext
            true, // allowSystemTenant
            true  // validateTenantStatus
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(false); // Inactive tenant
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should throw SecurityException
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> aspect.validateTenantAccess(joinPoint, annotation));
        
        assertTrue(exception.getMessage().contains("Write access denied"));
    }
    
    @Test
    @DisplayName("SECURITY: Delete access validation requires write access")
    void testDeleteAccessValidation() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.DELETE_ACCESS,
            true, // requireTenantContext
            true, // allowSystemTenant
            true  // validateTenantStatus
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should not throw (valid delete access)
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    @Test
    @DisplayName("SECURITY: Validation continues when throwOnFailure is false")
    void testNoThrowOnFailure() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.REQUIRE_TENANT_CONTEXT,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            false // throwOnFailure
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(false);
        
        setupJoinPoint("testMethod", new Object[]{});
        
        // Execute - should not throw even with invalid tenant context
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    @Test
    @DisplayName("SECURITY: Entity IDs validation for list parameters")
    void testEntityIdsValidation() {
        // Setup
        TenantValidated annotation = createTenantValidatedAnnotation(
            TenantValidated.ValidationMode.VALIDATE_ENTITY_IDS,
            true, // requireTenantContext
            true, // allowSystemTenant
            true, // validateTenantStatus
            "entityIds" // entityIdsParam
        );
        
        when(tenantContextService.hasTenantContext()).thenReturn(true);
        when(tenantContextService.getCurrentTenant()).thenReturn(COMPANY1_ID);
        when(tenantContextService.validateCurrentTenantAccess()).thenReturn(true);
        
        List<UUID> entityIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        setupJoinPoint("testMethod", new Object[]{entityIds}, "entityIds");
        
        // Execute - should not throw (validation logic would need repository)
        assertDoesNotThrow(() -> aspect.validateTenantAccess(joinPoint, annotation));
    }
    
    // ========== Helper Methods ==========
    
    private TenantValidated createTenantValidatedAnnotation(
            TenantValidated.ValidationMode mode,
            boolean requireTenantContext,
            boolean allowSystemTenant,
            boolean validateTenantStatus) {
        return createTenantValidatedAnnotation(mode, requireTenantContext, allowSystemTenant, 
            validateTenantStatus, "", true);
    }
    
    private TenantValidated createTenantValidatedAnnotation(
            TenantValidated.ValidationMode mode,
            boolean requireTenantContext,
            boolean allowSystemTenant,
            boolean validateTenantStatus,
            String paramName) {
        return createTenantValidatedAnnotation(mode, requireTenantContext, allowSystemTenant, 
            validateTenantStatus, paramName, true);
    }
    
    private TenantValidated createTenantValidatedAnnotation(
            TenantValidated.ValidationMode mode,
            boolean requireTenantContext,
            boolean allowSystemTenant,
            boolean validateTenantStatus,
            boolean throwOnFailure) {
        return createTenantValidatedAnnotation(mode, requireTenantContext, allowSystemTenant, 
            validateTenantStatus, "", throwOnFailure);
    }
    
    private TenantValidated createTenantValidatedAnnotation(
            TenantValidated.ValidationMode mode,
            boolean requireTenantContext,
            boolean allowSystemTenant,
            boolean validateTenantStatus,
            String paramName,
            boolean throwOnFailure) {
        
        return new TenantValidated() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return TenantValidated.class;
            }
            
            @Override
            public boolean requireTenantContext() { return requireTenantContext; }
            
            @Override
            public boolean allowSystemTenant() { return allowSystemTenant; }
            
            @Override
            public String tenantIdParam() { 
                return mode == ValidationMode.VALIDATE_TENANT_ID ? paramName : ""; 
            }
            
            @Override
            public String entityParam() { 
                return mode == ValidationMode.VALIDATE_ENTITY_TENANT ? paramName : ""; 
            }
            
            @Override
            public String entityIdsParam() { 
                return mode == ValidationMode.VALIDATE_ENTITY_IDS ? paramName : ""; 
            }
            
            @Override
            public String operation() { return "TEST"; }
            
            @Override
            public String message() { return ""; }
            
            @Override
            public boolean auditLog() { return true; }
            
            @Override
            public boolean throwOnFailure() { return throwOnFailure; }
            
            @Override
            public ValidationMode mode() { return mode; }
            
            @Override
            public boolean validateTenantStatus() { return validateTenantStatus; }
            
            @Override
            public String resourceType() { return "TestResource"; }
        };
    }
    
    private void setupJoinPoint(String methodName, Object[] args) {
        setupJoinPoint(methodName, args, null);
    }
    
    private void setupJoinPoint(String methodName, Object[] args, String paramName) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getTarget()).thenReturn(new Object());
        when(joinPoint.getArgs()).thenReturn(args);
        when(methodSignature.getName()).thenReturn(methodName);
        when(methodSignature.getMethod()).thenReturn(method);
        
        if (paramName != null && args.length > 0) {
            Parameter[] parameters = new Parameter[args.length];
            for (int i = 0; i < args.length; i++) {
                Parameter param = mock(Parameter.class);
                when(param.getName()).thenReturn(i == 0 ? paramName : "param" + i);
                parameters[i] = param;
            }
            when(method.getParameters()).thenReturn(parameters);
        } else {
            when(method.getParameters()).thenReturn(new Parameter[0]);
        }
    }
}