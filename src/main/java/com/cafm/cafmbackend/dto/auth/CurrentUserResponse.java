package com.cafm.cafmbackend.dto.auth;

import com.cafm.cafmbackend.shared.enums.UserType;
import java.util.Set;
import java.util.UUID;

/**
 * Current user information response DTO.
 */
public record CurrentUserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String fullName,
    UserType userType,
    UUID companyId,
    String companyName,
    Set<String> roles,
    boolean emailVerified,
    boolean isActive,
    boolean isLocked
) {}