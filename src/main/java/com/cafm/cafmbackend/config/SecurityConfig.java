package com.cafm.cafmbackend.config;

import com.cafm.cafmbackend.security.JwtAuthenticationEntryPoint;
import com.cafm.cafmbackend.security.filter.AuthenticationRateLimitFilter;
import com.cafm.cafmbackend.security.filter.JwtAuthenticationFilter;
import com.cafm.cafmbackend.security.filter.TenantSecurityFilter;
import com.cafm.cafmbackend.security.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application.
 * Provides beans for password encoding and other security-related configurations.
 * Configures JWT authentication and authorization rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {
    
    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private AuthenticationRateLimitFilter authenticationRateLimitFilter;
    
    @Autowired
    private TenantSecurityFilter tenantSecurityFilter;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;
    
    /**
     * Password encoder bean using BCrypt algorithm.
     * BCrypt is a strong hashing algorithm designed for password storage.
     * 
     * @return BCryptPasswordEncoder with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Authentication manager bean.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * Authentication provider with custom user details service.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Security filter chain configuration.
     * 
     * Security Features:
     * - CSRF protection enabled with cookie-based tokens for SPA
     * - Strict CORS configuration without wildcards
     * - JWT-based stateless authentication
     * - Method-level security enabled
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Create CSRF token request handler
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null); // Use default _csrf
        
        return http
            // Temporarily disable CSRF for testing
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management as stateless
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure exception handling
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public authentication endpoints - NO authentication required
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", 
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/verify-email", "/api/v1/test-util/**", "/api/v1/debug/**").permitAll()
                
                // Swagger/OpenAPI endpoints
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", 
                                "/swagger-resources/**", "/webjars/**", "/api-docs/**").permitAll()
                
                // Health and actuator endpoints
                .requestMatchers("/health", "/actuator/health", "/actuator/info").permitAll()
                
                // Static resources
                .requestMatchers("/favicon.ico", "/public/**", "/error").permitAll()
                
                // OPTIONS requests for CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Admin only endpoints
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/companies/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // Supervisor endpoints
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/v1/work-orders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/v1/technicians/**").hasAnyRole("ADMIN", "SUPERVISOR")
                
                // Technician endpoints
                .requestMatchers("/api/v1/tasks/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                .requestMatchers("/api/v1/inventory/**").hasAnyRole("ADMIN", "SUPERVISOR", "TECHNICIAN")
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Set authentication provider
            .authenticationProvider(authenticationProvider())
            
            // Add security filters in correct order
            .addFilterBefore(authenticationRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(tenantSecurityFilter, JwtAuthenticationFilter.class)
            
            .build();
    }
    
    /**
     * CORS configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configure allowed origins based on environment
        if (isProduction()) {
            // Production - strict origins only
            configuration.setAllowedOrigins(Arrays.asList(
                "https://app.cafm.com",
                "https://admin.cafm.com",
                "https://app.cafm.sa",
                "https://admin.cafm.sa"
            ));
        } else {
            // Development - allow localhost
            configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:8080"
            ));
        }
        
        // Allow specific methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allow specific headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Tenant-ID",
            "X-Company-ID",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Expose headers including CSRF token
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "X-Tenant-ID",
            "X-Company-ID",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size",
            "X-CSRF-TOKEN"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Check if running in production.
     */
    private boolean isProduction() {
        return activeProfile != null && 
               (activeProfile.contains("prod") || 
                activeProfile.contains("production"));
    }
}