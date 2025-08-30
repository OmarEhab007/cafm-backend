package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Test controller for email functionality.
 * 
 * Purpose: Provides endpoints to test email sending capabilities
 * Pattern: REST controller for testing email service
 * Java 23: Uses record for request DTOs
 * Architecture: Controller layer for testing utilities
 * Standards: Only available in dev/test profiles
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test Email", description = "Email testing endpoints (dev/test only)")
@Profile({"dev", "test", "local"})
public class TestEmailController {
    
    private final EmailService emailService;
    
    @Autowired
    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }
    
    public record EmailTestRequest(
        String to,
        String subject,
        String body
    ) {}
    
    @Operation(summary = "Send test email", description = "Send a simple test email to verify SMTP configuration")
    @PostMapping("/send-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendTestEmail(@RequestBody EmailTestRequest request) {
        CompletableFuture<Boolean> future = emailService.sendSimpleEmail(
            request.to(),
            request.subject(),
            request.body()
        );
        
        try {
            Boolean sent = future.get();
            if (sent) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Email sent successfully",
                    "to", request.to()
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to send email",
                    "to", request.to()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error sending email: " + e.getMessage(),
                "to", request.to()
            ));
        }
    }
    
    @Operation(summary = "Test password reset email", description = "Send a test password reset email")
    @PostMapping("/send-password-reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendPasswordResetEmail(
            @RequestParam String email,
            @RequestParam String userName) {
        
        String testToken = "test-reset-token-123456";
        CompletableFuture<Boolean> future = emailService.sendPasswordResetEmail(email, userName, testToken);
        
        try {
            Boolean sent = future.get();
            if (sent) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Password reset email sent",
                    "to", email,
                    "token", testToken
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to send password reset email",
                    "to", email
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error: " + e.getMessage(),
                "to", email
            ));
        }
    }
    
    @Operation(summary = "Test welcome email", description = "Send a test welcome email")
    @PostMapping("/send-welcome")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String userName) {
        
        String tempPassword = "TempPass123!";
        CompletableFuture<Boolean> future = emailService.sendWelcomeEmail(email, userName, tempPassword);
        
        try {
            Boolean sent = future.get();
            if (sent) {
                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Welcome email sent",
                    "to", email
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to send welcome email",
                    "to", email
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Error: " + e.getMessage(),
                "to", email
            ));
        }
    }
}