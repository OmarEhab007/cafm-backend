package com.cafm.cafmbackend.dto.auth;

/**
 * Two-factor authentication setup response DTO.
 */
public record TwoFactorSetupResponse(
    String secret,
    String qrCodeUrl,
    String manualEntryKey,
    String[] backupCodes
) {}