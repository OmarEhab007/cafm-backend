package com.cafm.cafmbackend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI/Swagger configuration for API documentation.
 * 
 * Purpose: Configure comprehensive API documentation with Swagger UI
 * Pattern: OpenAPI 3.0 specification with Spring Boot integration
 * Java 23: Modern configuration with records and enhanced collections
 * Architecture: API documentation layer
 * Standards: OpenAPI 3.0 specification compliance
 */
@Configuration
@OpenAPIDefinition
@SecuritySchemes({
    @SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT authentication token"
    ),
    @SecurityScheme(
        name = "api-key",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = "X-API-Key",
        description = "API Key authentication"
    )
})
public class OpenApiConfig {
    
    @Value("${app.version:1.0.0}")
    private String appVersion;
    
    @Value("${app.api.title:CAFM Backend API}")
    private String apiTitle;
    
    @Value("${app.api.description:Computer-Aided Facility Management System for Educational Institutions}")
    private String apiDescription;
    
    @Value("${server.servlet.context-path:}")
    private String contextPath;
    
    /**
     * Main OpenAPI configuration bean.
     */
    @Bean
    public OpenAPI cafmOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .externalDocs(externalDocumentation())
            .servers(servers())
            .tags(apiTags())
            .components(apiComponents())
            .security(securityRequirements());
    }
    
    /**
     * API information configuration.
     */
    private Info apiInfo() {
        return new Info()
            .title(apiTitle)
            .description(apiDescription)
            .version(appVersion)
            .contact(new Contact()
                .name("CAFM Support Team")
                .email("support@cafm.sa")
                .url("https://cafm.sa"))
            .license(new License()
                .name("Proprietary")
                .url("https://cafm.sa/license"))
            .termsOfService("https://cafm.sa/terms");
    }
    
    /**
     * External documentation configuration.
     */
    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
            .description("CAFM API Documentation")
            .url("https://docs.cafm.sa");
    }
    
    /**
     * Server configuration for different environments.
     */
    private List<Server> servers() {
        return Arrays.asList(
            new Server()
                .url("http://localhost:8080" + contextPath)
                .description("Local Development Server"),
            new Server()
                .url("https://api-staging.cafm.sa" + contextPath)
                .description("Staging Server"),
            new Server()
                .url("https://api.cafm.sa" + contextPath)
                .description("Production Server")
        );
    }
    
    /**
     * API tags for grouping endpoints.
     */
    private List<Tag> apiTags() {
        return Arrays.asList(
            new Tag().name("Authentication").description("Authentication and authorization endpoints"),
            new Tag().name("Users").description("User management operations"),
            new Tag().name("Companies").description("Company/tenant management"),
            new Tag().name("Reports").description("Maintenance report operations"),
            new Tag().name("Work Orders").description("Work order management"),
            new Tag().name("Assets").description("Asset management operations"),
            new Tag().name("Schools").description("School management"),
            new Tag().name("Inventory").description("Inventory and stock management"),
            new Tag().name("Statistics").description("Analytics and reporting"),
            new Tag().name("Files").description("File upload and management"),
            new Tag().name("Notifications").description("Notification management"),
            new Tag().name("Audit").description("Audit log operations")
        );
    }
    
    /**
     * API components configuration.
     */
    private Components apiComponents() {
        return new Components()
            .addSchemas("ErrorResponse", errorResponseSchema())
            .addSchemas("PageResponse", pageResponseSchema())
            .addSchemas("ValidationError", validationErrorSchema());
    }
    
    /**
     * Error response schema.
     */
    private Schema<?> errorResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(Map.of(
            "timestamp", new Schema<>().type("string").format("date-time").example("2024-01-15T10:30:00Z"),
            "path", new Schema<>().type("string").example("/api/v1/users/123"),
            "code", new Schema<>().type("string").example("RESOURCE_NOT_FOUND"),
            "message", new Schema<>().type("string").example("User not found"),
            "status", new Schema<>().type("integer").example(404)
        ));
        return schema;
    }
    
    /**
     * Page response schema.
     */
    private Schema<?> pageResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(Map.of(
            "content", new Schema<>().type("array"),
            "totalElements", new Schema<>().type("integer").format("int64"),
            "totalPages", new Schema<>().type("integer"),
            "size", new Schema<>().type("integer"),
            "number", new Schema<>().type("integer"),
            "first", new Schema<>().type("boolean"),
            "last", new Schema<>().type("boolean")
        ));
        return schema;
    }
    
    /**
     * Validation error schema.
     */
    private Schema<?> validationErrorSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setProperties(Map.of(
            "field", new Schema<>().type("string").example("email"),
            "message", new Schema<>().type("string").example("Invalid email format"),
            "rejectedValue", new Schema<>().type("object").example("invalid-email")
        ));
        return schema;
    }
    
    /**
     * Security requirements for authenticated endpoints.
     */
    private List<SecurityRequirement> securityRequirements() {
        return Arrays.asList(
            new SecurityRequirement().addList("bearer-jwt"),
            new SecurityRequirement().addList("api-key")
        );
    }
    
    /**
     * Public API group (no authentication required).
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
            .group("public-api")
            .displayName("Public API")
            .pathsToMatch(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/forgot-password",
                "/api/v1/auth/reset-password",
                "/health/**",
                "/actuator/health/**"
            )
            .build();
    }
    
    /**
     * Admin API group (admin role required).
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
            .group("admin-api")
            .displayName("Admin API")
            .pathsToMatch(
                "/api/v1/users/**",
                "/api/v1/companies/**",
                "/api/v1/admin/**",
                "/api/v1/audit/**"
            )
            .addOpenApiCustomizer(openApi -> 
                openApi.info(new Info()
                    .title("CAFM Admin API")
                    .description("Administrative endpoints for CAFM system")
                    .version(appVersion)))
            .build();
    }
    
    /**
     * Operations API group (supervisor/technician operations).
     */
    @Bean
    public GroupedOpenApi operationsApi() {
        return GroupedOpenApi.builder()
            .group("operations-api")
            .displayName("Operations API")
            .pathsToMatch(
                "/api/v1/reports/**",
                "/api/v1/work-orders/**",
                "/api/v1/assets/**",
                "/api/v1/inventory/**",
                "/api/v1/schools/**"
            )
            .build();
    }
    
    /**
     * Customizer to add common headers to all endpoints.
     */
    @Bean
    public OpenApiCustomizer globalHeaderCustomizer() {
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem -> 
                pathItem.readOperations().forEach(operation -> {
                    // Add common parameters
                    operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                        .in("header")
                        .name("X-Request-Id")
                        .description("Unique request identifier for tracking")
                        .required(false)
                        .schema(new Schema<>().type("string").format("uuid")));
                    
                    operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                        .in("header")
                        .name("X-Company-ID")
                        .description("Company/tenant identifier")
                        .required(false)
                        .schema(new Schema<>().type("string").format("uuid")));
                    
                    // Add common responses
                    if (operation.getResponses() != null) {
                        operation.getResponses().addApiResponse("400", 
                            new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Bad Request - Invalid input parameters"));
                        
                        operation.getResponses().addApiResponse("401",
                            new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Unauthorized - Authentication required"));
                        
                        operation.getResponses().addApiResponse("403",
                            new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Forbidden - Insufficient permissions"));
                        
                        operation.getResponses().addApiResponse("429",
                            new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Too Many Requests - Rate limit exceeded"));
                        
                        operation.getResponses().addApiResponse("500",
                            new io.swagger.v3.oas.models.responses.ApiResponse()
                                .description("Internal Server Error"));
                    }
                })
            );
        };
    }
}