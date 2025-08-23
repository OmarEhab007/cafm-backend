package com.cafm.cafmbackend.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Test class to generate BCrypt password hash for Admin123!
 * This uses Spring Boot test context to ensure proper dependency loading.
 */
@SpringBootTest
public class PasswordHashGeneratorTest {

    @Test
    public void generatePasswordHashForAdmin123() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "Admin123!";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("=".repeat(60));
        System.out.println("PASSWORD HASH GENERATOR RESULTS");
        System.out.println("=".repeat(60));
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hashedPassword);
        System.out.println();
        System.out.println("SQL UPDATE STATEMENT:");
        System.out.println("UPDATE users SET password_hash = '" + hashedPassword + "' WHERE email = 'admin@example.com';");
        System.out.println();
        
        // Verify the hash works
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Verification Test: " + (matches ? "✅ SUCCESS" : "❌ FAILED"));
        
        // Test against some known incorrect passwords
        System.out.println("\nNegative Tests:");
        System.out.println("'admin123!' matches: " + encoder.matches("admin123!", hashedPassword) + " (should be false)");
        System.out.println("'Admin123' matches: " + encoder.matches("Admin123", hashedPassword) + " (should be false)");
        System.out.println("'ADMIN123!' matches: " + encoder.matches("ADMIN123!", hashedPassword) + " (should be false)");
        
        // Generate a few more hashes to show BCrypt's salt randomness
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ADDITIONAL HASHES (BCrypt generates different hashes each time due to random salt):");
        System.out.println("=".repeat(60));
        for (int i = 1; i <= 3; i++) {
            String newHash = encoder.encode(password);
            boolean newMatches = encoder.matches(password, newHash);
            System.out.println("Hash " + i + ": " + newHash);
            System.out.println("   Verification: " + (newMatches ? "✅ SUCCESS" : "❌ FAILED"));
            System.out.println();
        }
        System.out.println("=".repeat(60));
        
        // Assert for test framework
        assert matches : "Password hash verification failed";
    }
}