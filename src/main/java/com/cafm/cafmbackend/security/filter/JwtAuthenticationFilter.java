package com.cafm.cafmbackend.security.filter;

import com.cafm.cafmbackend.infrastructure.persistence.entity.User;
import com.cafm.cafmbackend.infrastructure.persistence.repository.UserRepository;
import com.cafm.cafmbackend.security.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * JWT authentication filter for validating JWT tokens and setting authentication.
 * 
 * Architecture: Stateless JWT authentication filter
 * Pattern: Servlet filter for authentication
 * Java 23: Modern exception handling and validation
 * Order: Runs before TenantSecurityFilter to establish authentication context
 */
@Component
@Order(1) // Run first to establish authentication
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    
    @Autowired
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                  UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            String jwt = extractJwtFromRequest(request);
            
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Load user from database
                    Optional<User> userOptional = userRepository.findByEmail(username);
                    
                    if (userOptional.isPresent()) {
                        UserDetails userDetails = userOptional.get();
                        
                        // Check if user is active and not locked
                        if (userDetails.isAccountNonLocked() && userDetails.isEnabled()) {
                            // Create authentication token
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                                );
                            
                            authentication.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                            );
                            
                            // Set authentication in context
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            logger.debug("Set authentication for user: {}", username);
                        } else {
                            logger.warn("User account is locked or disabled: {}", username);
                        }
                    } else {
                        logger.warn("User not found for token: {}", username);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
            // Set request attribute for expired token
            request.setAttribute("token_status", "expired");
        } catch (Exception e) {
            logger.error("Cannot set user authentication", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header.
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        // Also check for token in query parameter (for WebSocket connections)
        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        
        return null;
    }
    
    /**
     * Should the filter be applied to this request?
     */
    @Override
    public boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT filtering for public endpoints
        return path.equals("/api/v1/auth/login") ||
               path.equals("/api/v1/auth/refresh") ||
               path.equals("/api/v1/auth/forgot-password") ||
               path.equals("/api/v1/auth/reset-password") ||
               path.startsWith("/api/v1/auth/verify-email") ||
               path.startsWith("/public/") ||
               path.startsWith("/health") ||
               path.startsWith("/actuator/") ||
               path.equals("/favicon.ico") ||
               path.equals("/swagger-ui.html") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars/");
    }
}