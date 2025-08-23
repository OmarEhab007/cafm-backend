package com.cafm.cafmbackend.validation.validator;

import com.cafm.cafmbackend.validation.constraint.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator for strong password requirements.
 * 
 * Security Requirements:
 * - Minimum 12 characters (configurable)
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 * - No common passwords
 * - No sequential or repeated characters
 * - No personal information (when context available)
 */
@Component
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    @Value("${security.password.min-length:12}")
    private int minLength;
    
    @Value("${security.password.max-length:128}")
    private int maxLength;
    
    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;
    
    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;
    
    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;
    
    @Value("${security.password.require-special:true}")
    private boolean requireSpecial;
    
    @Value("${security.password.check-common-passwords:true}")
    private boolean checkCommonPasswords;
    
    // Patterns for validation
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile(".*\\s.*");
    private static final Pattern SEQUENTIAL_NUMBERS = Pattern.compile(".*(012|123|234|345|456|567|678|789|890|987|876|765|654|543|432|321|210).*");
    private static final Pattern SEQUENTIAL_LETTERS = Pattern.compile(".*(abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_CHARS = Pattern.compile(".*(.)\\1{2,}.*");
    
    // Common weak passwords (top 100)
    private static final List<String> COMMON_PASSWORDS = List.of(
        "password", "password123", "password1", "password12", "password1234",
        "123456", "12345678", "123456789", "1234567890", "12345",
        "qwerty", "qwertyuiop", "qwerty123", "abc123", "admin",
        "letmein", "welcome", "welcome123", "monkey", "dragon",
        "master", "iloveyou", "trustno1", "1234567", "123123",
        "admin123", "root", "toor", "pass", "passw0rd",
        "p@ssw0rd", "p@ssword", "Password1", "Password123", "Password1234",
        "hello", "hello123", "1q2w3e4r", "1qaz2wsx", "qazwsx",
        "123qwe", "password!", "passw0rd!", "admin!", "admin1234",
        "football", "baseball", "superman", "batman", "michael",
        "charlie", "shadow", "jordan", "jennifer", "michelle",
        "default", "secret", "test", "test123", "demo",
        "demo123", "changeme", "changeme123", "guest", "guest123",
        "user", "user123", "oracle", "oracle123", "postgres",
        "mysql", "mongodb", "redis", "docker", "kubernetes",
        "spring", "springboot", "java", "python", "javascript",
        "angular", "react", "vuejs", "nodejs", "golang",
        "rust", "swift", "kotlin", "android", "iphone",
        "windows", "linux", "macos", "ubuntu", "debian",
        "centos", "fedora", "redhat", "amazon", "google",
        "microsoft", "apple", "facebook", "twitter", "instagram"
    );
    
    private String customMessage;
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.customMessage = constraintAnnotation.message();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // Let @NotNull handle null validation
        }
        
        List<String> violations = new ArrayList<>();
        
        // Check length
        if (password.length() < minLength) {
            violations.add(String.format("Password must be at least %d characters long", minLength));
        }
        
        if (password.length() > maxLength) {
            violations.add(String.format("Password must not exceed %d characters", maxLength));
        }
        
        // Check complexity requirements
        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one uppercase letter");
        }
        
        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one lowercase letter");
        }
        
        if (requireDigit && !DIGIT_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one digit");
        }
        
        if (requireSpecial && !SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            violations.add("Password must contain at least one special character (!@#$%^&*()_+-=[]{};':\"\\|,.<>/?)");
        }
        
        // Check for whitespace
        if (WHITESPACE_PATTERN.matcher(password).matches()) {
            violations.add("Password must not contain whitespace");
        }
        
        // Check for sequential patterns
        if (SEQUENTIAL_NUMBERS.matcher(password).matches()) {
            violations.add("Password must not contain sequential numbers");
        }
        
        if (SEQUENTIAL_LETTERS.matcher(password).matches()) {
            violations.add("Password must not contain sequential letters");
        }
        
        // Check for repeated characters
        if (REPEATED_CHARS.matcher(password).matches()) {
            violations.add("Password must not contain more than 2 repeated characters in a row");
        }
        
        // Check against common passwords
        if (checkCommonPasswords) {
            String lowerPassword = password.toLowerCase();
            if (COMMON_PASSWORDS.stream().anyMatch(common -> 
                lowerPassword.equals(common) || lowerPassword.contains(common))) {
                violations.add("Password is too common or contains common patterns");
            }
        }
        
        // Check entropy (simplified Shannon entropy)
        double entropy = calculateEntropy(password);
        if (entropy < 3.0) { // Threshold for reasonable entropy
            violations.add("Password is too predictable, please use more variety in characters");
        }
        
        // If there are violations, build custom message
        if (!violations.isEmpty()) {
            context.disableDefaultConstraintViolation();
            String message = String.join("; ", violations);
            context.buildConstraintViolationWithTemplate(message)
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate simplified Shannon entropy for password strength.
     */
    private double calculateEntropy(String password) {
        if (password.isEmpty()) {
            return 0;
        }
        
        int[] charCounts = new int[256];
        for (char c : password.toCharArray()) {
            charCounts[c]++;
        }
        
        double entropy = 0;
        int length = password.length();
        
        for (int count : charCounts) {
            if (count > 0) {
                double probability = (double) count / length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }
        
        return entropy;
    }
    
    /**
     * Check if password contains personal information.
     * This would be called with context about the user.
     */
    public boolean containsPersonalInfo(String password, String username, String email, String name) {
        if (password == null) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        
        // Check username
        if (username != null && !username.isEmpty()) {
            if (lowerPassword.contains(username.toLowerCase())) {
                return true;
            }
        }
        
        // Check email (without domain)
        if (email != null && !email.isEmpty()) {
            String emailLocal = email.split("@")[0].toLowerCase();
            if (lowerPassword.contains(emailLocal)) {
                return true;
            }
        }
        
        // Check name parts
        if (name != null && !name.isEmpty()) {
            String[] nameParts = name.toLowerCase().split("\\s+");
            for (String part : nameParts) {
                if (part.length() > 2 && lowerPassword.contains(part)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}