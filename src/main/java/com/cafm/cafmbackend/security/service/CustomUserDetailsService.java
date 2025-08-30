package com.cafm.cafmbackend.security.service;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details from database for authentication.
 * 
 * Architecture: Spring Security integration service
 * Pattern: UserDetailsService implementation for authentication
 * Java 23: Modern exception handling and null safety
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final UserRepository userRepository;
    
    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Load user by username (email in our case).
     * 
     * @param username The username (email) to load
     * @return UserDetails for authentication
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
        // Try to find by email first
        User user = userRepository.findByEmail(username)
            .or(() -> userRepository.findByUsernameIgnoreCase(username))
            .orElseThrow(() -> {
                logger.warn("User not found with identifier: {}", username);
                return new UsernameNotFoundException("User not found with identifier: " + username);
            });
        
        // Check if user is soft deleted
        if (user.getDeletedAt() != null) {
            logger.warn("Attempt to authenticate soft-deleted user: {}", username);
            throw new UsernameNotFoundException("User account has been deleted");
        }
        
        // Check if user is active
        if (user.getIsActive() == null || !user.getIsActive()) {
            logger.warn("Attempt to authenticate inactive user: {}", username);
            throw new UsernameNotFoundException("User account is inactive");
        }
        
        logger.debug("Successfully loaded user: {} with roles: {}", 
                    user.getEmail(), user.getAuthorities());
        
        return user; // User entity implements UserDetails
    }
    
    /**
     * Load user by ID for token refresh and other operations.
     * 
     * @param userId The user ID to load
     * @return User entity
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User loadUserById(String userId) throws UsernameNotFoundException {
        logger.debug("Loading user by ID: {}", userId);
        
        try {
            User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> {
                    logger.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });
            
            // Check if user is soft deleted
            if (user.getDeletedAt() != null) {
                logger.warn("Attempt to load soft-deleted user by ID: {}", userId);
                throw new UsernameNotFoundException("User account has been deleted");
            }
            
            return user;
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid UUID format for user ID: {}", userId);
            throw new UsernameNotFoundException("Invalid user ID format");
        }
    }
}