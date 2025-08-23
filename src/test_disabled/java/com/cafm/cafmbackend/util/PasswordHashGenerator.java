package com.cafm.cafmbackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for testing.
 * Run this to get the hash for a test password.
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // Generate hash for admin password
        String password = "Admin123!";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println();
        System.out.println("Use this hash in your SQL script:");
        System.out.println("password_hash = '" + hashedPassword + "'");
        
        // Verify the hash works
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Verification: " + (matches ? "SUCCESS" : "FAILED"));
    }
}