package com.cafm.cafmbackend.configuration.web;

import com.cafm.cafmbackend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebSocket authentication interceptor that validates JWT tokens for WebSocket connections.
 * 
 * This interceptor ensures that:
 * - All WebSocket connections are properly authenticated
 * - JWT tokens are validated before establishing connections
 * - User context is properly set for message handling
 * - Unauthorized connections are rejected with appropriate error messages
 * 
 * The interceptor operates on STOMP CONNECT commands to authenticate users
 * and maintains security context throughout the WebSocket session.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    @Qualifier("customUserDetailsService")
    private UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.debug("Processing WebSocket CONNECT command");
            
            try {
                // Extract JWT token from headers
                List<String> authHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);
                String token = extractTokenFromHeaders(authHeaders);
                
                if (token == null || token.isEmpty()) {
                    log.warn("WebSocket connection attempt without authentication token");
                    throw new IllegalArgumentException("Authentication token is required");
                }
                
                // Validate JWT token
                if (!jwtTokenProvider.validateToken(token)) {
                    log.warn("WebSocket connection attempt with invalid token");
                    throw new IllegalArgumentException("Invalid authentication token");
                }
                
                // Extract username from token
                String username = jwtTokenProvider.getUsernameFromToken(token);
                if (username == null || username.isEmpty()) {
                    log.warn("WebSocket connection attempt with token containing no username");
                    throw new IllegalArgumentException("Invalid token: no username");
                }
                
                // Load user details and create authentication
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                
                // Set authentication in accessor for this session
                accessor.setUser(authentication);
                
                // Also set in security context for this thread
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.info("WebSocket connection authenticated for user: {}", username);
                
            } catch (Exception e) {
                log.error("WebSocket authentication failed", e);
                throw new RuntimeException("WebSocket authentication failed: " + e.getMessage());
            }
        }
        
        return message;
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, 
                                  boolean sent, Exception ex) {
        // Clear security context after message processing
        SecurityContextHolder.clearContext();
    }

    /**
     * Extracts JWT token from Authorization header values
     */
    private String extractTokenFromHeaders(List<String> authHeaders) {
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }
        
        String authHeader = authHeaders.get(0);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}