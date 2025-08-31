package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.infrastructure.persistence.entity.Company;
import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.shared.enums.UserStatus;
import com.cafm.cafmbackend.shared.enums.UserType;
import com.cafm.cafmbackend.infrastructure.persistence.repository.CompanyRepository;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.application.service.tenant.TenantContextService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Debug controller for testing user creation issues.
 * ONLY AVAILABLE IN DEVELOPMENT PROFILE - DISABLED IN PRODUCTION.
 * 
 * Security Note: This controller contains test operations and should never
 * be enabled in production environments.
 */
@RestController
@RequestMapping("/api/v1/debug")
@org.springframework.context.annotation.Profile({"dev", "test"})
public class DebugController {
    
    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);
    
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantContextService tenantContextService;
    private final EntityManager entityManager;
    
    public DebugController(UserRepository userRepository, 
                          CompanyRepository companyRepository,
                          PasswordEncoder passwordEncoder,
                          TenantContextService tenantContextService,
                          EntityManager entityManager) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantContextService = tenantContextService;
        this.entityManager = entityManager;
    }
    
    @PostMapping("/test-user-creation")
    @Transactional
    public ResponseEntity<Map<String, Object>> testUserCreation() {
        try {
            logger.info("Starting test user creation");
            
            // Get company
            Company company = companyRepository.findById(UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .orElseThrow(() -> new RuntimeException("Company not found"));
            logger.info("Found company: {}", company.getName());
            
            // Set tenant context
            tenantContextService.setCurrentTenant(company.getId());
            
            // Create user with minimal fields first
            User user = new User();
            String timestamp = String.valueOf(System.currentTimeMillis());
            
            // Set only the absolutely required fields
            user.setEmail("debuguser" + timestamp + "@cafm.com");
            user.setUsername("debuguser" + timestamp);  // Simpler username
            user.setPasswordHash(passwordEncoder.encode("SecurePass123@"));
            user.setFirstName("Debug");
            user.setLastName("User");
            user.setUserType(UserType.TECHNICIAN);
            user.setCompany(company);
            user.setStatus(UserStatus.PENDING_VERIFICATION); // Set status explicitly
            
            // Set audit fields manually to bypass auditing issues
            user.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000000")); // System user
            user.setModifiedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
            
            logger.info("Created user object with email: {}", user.getEmail());
            logger.info("User type: {}", user.getUserType());
            logger.info("Company ID: {}", user.getCompany().getId());
            
            // Try to save
            try {
                userRepository.flush(); // Flush any pending changes first
                user = userRepository.save(user);
                userRepository.flush(); // Force immediate execution
                logger.info("Successfully saved user with ID: {}", user.getId());
                
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", user.getId(),
                    "email", user.getEmail(),
                    "userType", user.getUserType().toString()
                ));
            } catch (Exception saveEx) {
                logger.error("Failed to save user", saveEx);
                Throwable rootCause = saveEx;
                while (rootCause.getCause() != null) {
                    rootCause = rootCause.getCause();
                }
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", saveEx.getMessage(),
                    "rootCause", rootCause.getMessage(),
                    "type", saveEx.getClass().getSimpleName()
                ));
            }
            
        } catch (Exception ex) {
            logger.error("Test user creation failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", ex.getMessage()
            ));
        } finally {
            // Clear tenant context
            tenantContextService.clearTenantContext();
        }
    }
    
    @PostMapping("/test-native-user-creation")
    @Transactional
    public ResponseEntity<Map<String, Object>> testNativeUserCreation() {
        try {
            logger.info("Starting native SQL user creation");
            
            String timestamp = String.valueOf(System.currentTimeMillis());
            UUID userId = UUID.randomUUID();
            String email = "nativeuser" + timestamp + "@cafm.com";
            String username = "nativeuser" + timestamp;
            // WARNING: Hardcoded password for development testing only
            // This is acceptable only because this controller is profile-restricted to dev/test
            String passwordHash = passwordEncoder.encode("SecurePass123@");
            
            // Use native SQL to insert with cast workaround
            String sql = """
                INSERT INTO users (
                    id, email, username, password_hash, first_name, last_name,
                    user_type, status, company_id, is_active, is_locked,
                    email_verified, phone_verified, created_at, updated_at
                ) VALUES (
                    ?1, ?2, ?3, ?4, ?5, ?6,
                    ?7::user_type, ?8::user_status, ?9, 
                    ?10, ?11, ?12, ?13,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                )
                """;
            
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, userId);
            query.setParameter(2, email);
            query.setParameter(3, username);
            query.setParameter(4, passwordHash);
            query.setParameter(5, "Native");
            query.setParameter(6, "User");
            query.setParameter(7, "TECHNICIAN");
            query.setParameter(8, "PENDING_VERIFICATION");
            query.setParameter(9, UUID.fromString("00000000-0000-0000-0000-000000000001"));
            query.setParameter(10, true);
            query.setParameter(11, false);
            query.setParameter(12, false);
            query.setParameter(13, false);
            
            int result = query.executeUpdate();
            logger.info("Native SQL insert result: {}", result);
            
            if (result > 0) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "userId", userId.toString(),
                    "email", email,
                    "message", "User created using native SQL"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "No rows inserted"
                ));
            }
            
        } catch (Exception ex) {
            logger.error("Native user creation failed", ex);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", ex.getMessage(),
                "type", ex.getClass().getSimpleName()
            ));
        }
    }
}