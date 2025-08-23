package com.cafm.cafmbackend.security.event;

import com.cafm.cafmbackend.data.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.*;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Security event logger for auditing authentication and authorization events.
 * 
 * Architecture: Event-driven security auditing
 * Pattern: Spring event listener for security events
 * Java 23: Record pattern for event data
 */
@Component
public class SecurityEventLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityEventLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    /**
     * Log successful authentication events.
     */
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth.getName();
        String principal = getPrincipalInfo(auth);
        
        auditLogger.info("Authentication SUCCESS - User: {} - Principal: {} - Time: {}", 
                        username, principal, LocalDateTime.now());
        logger.debug("User {} successfully authenticated", username);
    }
    
    /**
     * Log failed authentication attempts.
     */
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth != null ? auth.getName() : "unknown";
        Exception exception = event.getException();
        
        auditLogger.warn("Authentication FAILURE - User: {} - Reason: {} - Time: {}", 
                        username, exception.getMessage(), LocalDateTime.now());
        logger.warn("Authentication failed for user {}: {}", username, exception.getMessage());
        
        // Log specific failure types
        if (event instanceof AuthenticationFailureBadCredentialsEvent) {
            auditLogger.warn("Bad credentials for user: {}", username);
        } else if (event instanceof AuthenticationFailureLockedEvent) {
            auditLogger.warn("Account locked for user: {}", username);
        } else if (event instanceof AuthenticationFailureDisabledEvent) {
            auditLogger.warn("Account disabled for user: {}", username);
        } else if (event instanceof AuthenticationFailureExpiredEvent) {
            auditLogger.warn("Account expired for user: {}", username);
        } else if (event instanceof AuthenticationFailureCredentialsExpiredEvent) {
            auditLogger.warn("Credentials expired for user: {}", username);
        }
    }
    
    /**
     * Log authorization failures.
     */
    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication().get();
        String username = auth != null ? auth.getName() : "anonymous";
        String decision = event.getAuthorizationDecision().toString();
        
        auditLogger.warn("Authorization DENIED - User: {} - Decision: {} - Time: {}", 
                        username, decision, LocalDateTime.now());
        logger.warn("Authorization denied for user {}: {}", username, decision);
    }
    
    /**
     * Log session creation events.
     */
    @EventListener
    public void onSessionCreated(InteractiveAuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String username = auth.getName();
        
        auditLogger.info("Session CREATED - User: {} - Time: {}", 
                        username, LocalDateTime.now());
        logger.debug("Session created for user: {}", username);
    }
    
    /**
     * Extract principal information from authentication.
     */
    private String getPrincipalInfo(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return "unknown";
        }
        
        Object principal = auth.getPrincipal();
        
        if (principal instanceof User user) {
            return String.format("User[id=%s, email=%s, type=%s, company=%s]",
                user.getId(),
                user.getEmail(),
                user.getUserType(),
                user.getCompany() != null ? user.getCompany().getId() : "none"
            );
        }
        
        return principal.toString();
    }
    
    /**
     * Security event record for structured logging.
     */
    public record SecurityEvent(
        String eventType,
        String username,
        String ipAddress,
        LocalDateTime timestamp,
        boolean success,
        String details
    ) {
        public String toAuditLog() {
            return String.format("SECURITY_EVENT - Type: %s - User: %s - IP: %s - Success: %s - Time: %s - Details: %s",
                eventType, username, ipAddress, success, timestamp, details);
        }
    }
    
    /**
     * Log custom security event.
     */
    public void logSecurityEvent(SecurityEvent event) {
        if (event.success()) {
            auditLogger.info(event.toAuditLog());
        } else {
            auditLogger.warn(event.toAuditLog());
        }
    }
    
    /**
     * Log login attempt.
     */
    public void logLoginAttempt(String username, String ipAddress, boolean success, String details) {
        SecurityEvent event = new SecurityEvent(
            "LOGIN_ATTEMPT",
            username,
            ipAddress,
            LocalDateTime.now(),
            success,
            details
        );
        logSecurityEvent(event);
    }
    
    /**
     * Log logout event.
     */
    public void logLogout(String username, String ipAddress) {
        SecurityEvent event = new SecurityEvent(
            "LOGOUT",
            username,
            ipAddress,
            LocalDateTime.now(),
            true,
            "User logged out"
        );
        logSecurityEvent(event);
    }
    
    /**
     * Log token refresh.
     */
    public void logTokenRefresh(String username, String ipAddress, boolean success) {
        SecurityEvent event = new SecurityEvent(
            "TOKEN_REFRESH",
            username,
            ipAddress,
            LocalDateTime.now(),
            success,
            success ? "Token refreshed successfully" : "Token refresh failed"
        );
        logSecurityEvent(event);
    }
    
    /**
     * Log password change.
     */
    public void logPasswordChange(String username, String ipAddress, boolean success) {
        SecurityEvent event = new SecurityEvent(
            "PASSWORD_CHANGE",
            username,
            ipAddress,
            LocalDateTime.now(),
            success,
            success ? "Password changed successfully" : "Password change failed"
        );
        logSecurityEvent(event);
    }
    
    /**
     * Log account lockout.
     */
    public void logAccountLockout(String username, String ipAddress, String reason) {
        SecurityEvent event = new SecurityEvent(
            "ACCOUNT_LOCKOUT",
            username,
            ipAddress,
            LocalDateTime.now(),
            false,
            reason
        );
        logSecurityEvent(event);
    }
}