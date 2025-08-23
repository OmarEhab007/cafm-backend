package com.cafm.cafmbackend.api.controllers;

import com.cafm.cafmbackend.data.enums.UserStatus;
import com.cafm.cafmbackend.data.enums.UserType;
import com.cafm.cafmbackend.dto.user.*;
import com.cafm.cafmbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for user management operations.
 * Handles CRUD operations and user-specific actions.
 */
@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    /**
     * Get all users with pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Get all users", description = "Get paginated list of users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Page<UserResponseSimplified>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) @Parameter(description = "Filter by user type") UserType userType,
            @RequestParam(required = false) @Parameter(description = "Filter by status") UserStatus status,
            @RequestParam(required = false) @Parameter(description = "Search by name or email") String search) {

        logger.debug("Get all users request with page: {}, size: {}",
                    pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponseSimplified> users;

        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search, pageable);
        } else if (userType != null) {
            users = userService.getUsersByType(userType, pageable);
        } else if (status != null) {
            users = userService.getUsersByStatus(status, pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }

        return ResponseEntity.ok(users);
    }

    /**
     * Get user by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @userService.isSameCompany(#id, authentication.principal.username))")
    @Operation(summary = "Get user by ID", description = "Get detailed user information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<UserResponseSimplified> getUserById(
            @PathVariable @Parameter(description = "User ID") UUID id) {

        logger.debug("Get user by ID: {}", id);

        UserResponseSimplified user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Create a new user.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user", description = "Create a new user account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user data"),
        @ApiResponse(responseCode = "409", description = "Email already exists"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<UserResponseSimplified> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Create user request for email: {} by admin: {}",
                   request.email(), currentUser.getUsername());

        UserResponseSimplified user = userService.createUser(request);

        logger.info("User created successfully with ID: {}", user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Update user.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @userService.canManageUser(#id, authentication.principal.username))")
    @Operation(summary = "Update user", description = "Update user information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<UserResponseSimplified> updateUser(
            @PathVariable @Parameter(description = "User ID") UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Update user request for ID: {} by: {}", id, currentUser.getUsername());

        UserResponseSimplified user = userService.updateUser(id, request);

        logger.info("User updated successfully: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Delete user (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user account")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Not authorized")
    })
    public ResponseEntity<Void> deleteUser(
            @PathVariable @Parameter(description = "User ID") UUID id,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Delete user request for ID: {} by admin: {}", id, currentUser.getUsername());

        userService.deleteUser(id);

        logger.info("User deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate user account.
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate user", description = "Activate a user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User already active")
    })
    public ResponseEntity<UserResponseSimplified> activateUser(
            @PathVariable @Parameter(description = "User ID") UUID id) {

        logger.info("Activate user request for ID: {}", id);

        UserResponseSimplified user = userService.activateUser(id);

        logger.info("User activated successfully: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Deactivate user account.
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User already inactive")
    })
    public ResponseEntity<UserResponseSimplified> deactivateUser(
            @PathVariable @Parameter(description = "User ID") UUID id) {

        logger.info("Deactivate user request for ID: {}", id);

        UserResponseSimplified user = userService.deactivateUser(id);

        logger.info("User deactivated successfully: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Lock user account.
     */
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock user", description = "Lock a user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User locked successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User already locked")
    })
    public ResponseEntity<UserResponseSimplified> lockUser(
            @PathVariable @Parameter(description = "User ID") UUID id,
            @RequestParam @Parameter(description = "Lock reason") String reason) {

        logger.info("Lock user request for ID: {} with reason: {}", id, reason);

        UserResponseSimplified user = userService.lockUser(id, reason);

        logger.info("User locked successfully: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Unlock user account.
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock user", description = "Unlock a user account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User unlocked successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "User not locked")
    })
    public ResponseEntity<UserResponseSimplified> unlockUser(
            @PathVariable @Parameter(description = "User ID") UUID id) {

        logger.info("Unlock user request for ID: {}", id);

        UserResponseSimplified user = userService.unlockUser(id);

        logger.info("User unlocked successfully: {}", id);
        return ResponseEntity.ok(user);
    }

    /**
     * Reset user password (admin action).
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reset user password", description = "Admin reset of user password")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<PasswordResetResult> resetUserPassword(
            @PathVariable @Parameter(description = "User ID") UUID id,
            @RequestParam(required = false) @Parameter(description = "Send email notification")
            Boolean sendEmail) {

        logger.info("Admin password reset for user ID: {}", id);

        String temporaryPassword = userService.resetUserPassword(id);

        // In production, send email instead of returning password
        PasswordResetResult result = new PasswordResetResult(
            id,
            Boolean.TRUE.equals(sendEmail) ? "Password reset email sent" : temporaryPassword,
            Boolean.TRUE.equals(sendEmail)
        );

        logger.info("Password reset successfully for user: {}", id);
        return ResponseEntity.ok(result);
    }

    /**
     * Get users by company.
     */
    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPERVISOR') and @companyService.belongsToCompany(#companyId, authentication.principal.username))")
    @Operation(summary = "Get users by company", description = "Get all users in a company")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<Page<UserResponseSimplified>> getUsersByCompany(
            @PathVariable @Parameter(description = "Company ID") UUID companyId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        logger.debug("Get users for company: {}", companyId);

        Page<UserResponseSimplified> users = userService.getUsersByCompany(companyId, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get user profile.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile", description = "Get current user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponseSimplified> getMyProfile(
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.debug("Get profile for user: {}", currentUser.getUsername());

        UserResponseSimplified profile = userService.getUserByEmail(currentUser.getUsername());
        return ResponseEntity.ok(profile);
    }

    /**
     * Update user profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update my profile", description = "Update current user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid update data"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserResponseSimplified> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        logger.info("Update profile for user: {}", currentUser.getUsername());

        UserResponseSimplified profile = userService.updateUserProfile(
            currentUser.getUsername(), request);

        logger.info("Profile updated successfully for user: {}", currentUser.getUsername());
        return ResponseEntity.ok(profile);
    }


    // ========== Inner Classes ==========

    /**
     * Password reset result DTO.
     */
    public record PasswordResetResult(
        UUID userId,
        String message,
        boolean emailSent
    ) {}
}