package com.cafm.cafmbackend.configuration.web;

import com.cafm.cafmbackend.configuration.web.WebSocketAuthInterceptor;
import com.cafm.cafmbackend.configuration.web.WebSocketTenantInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time communication in the CAFM system.
 * 
 * This configuration enables:
 * - Real-time work order updates and notifications
 * - Live location tracking for supervisors and technicians
 * - Instant messaging between system users
 * - Real-time dashboard updates with metrics and alerts
 * - Multi-tenant support with proper tenant isolation
 * 
 * Uses Spring WebSocket with STOMP protocol for scalable real-time communication.
 * Implements authentication and tenant filtering to ensure security and data isolation.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final WebSocketTenantInterceptor tenantInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                          WebSocketTenantInterceptor tenantInterceptor) {
        this.authInterceptor = authInterceptor;
        this.tenantInterceptor = tenantInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable in-memory message broker for topics and queues
        config.enableSimpleBroker(
                "/topic",   // For broadcast messages (notifications, alerts)
                "/queue",   // For user-specific messages (private notifications)
                "/user"     // For user-specific subscriptions
        );
        
        // Set application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for user-specific messaging
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure based on CORS settings
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);
        
        // Additional endpoint without SockJS for native WebSocket clients
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication and tenant isolation interceptors
        registration.interceptors(authInterceptor, tenantInterceptor);
        
        // Configure thread pool for handling inbound messages
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(1000);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configure thread pool for handling outbound messages
        registration.taskExecutor()
                .corePoolSize(10)
                .maxPoolSize(50)
                .queueCapacity(1000);
    }

}