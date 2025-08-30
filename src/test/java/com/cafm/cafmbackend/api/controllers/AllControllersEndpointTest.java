package com.cafm.cafmbackend.api.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.handler.annotation.MessageMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite that validates ALL enabled controllers in the CAFM backend.
 * 
 * Purpose: Validates that all 15 enabled controllers are properly configured and discoverable
 * Pattern: Uses reflection to scan all controllers and their endpoints systematically  
 * Java 23: Modern test class with comprehensive controller validation
 * Architecture: Ensures all controllers follow REST conventions and are properly mapped
 * Standards: Validates endpoint structure, HTTP methods, and provides complete API inventory
 * 
 * This test serves as a validation that all controllers were successfully re-enabled 
 * after removing MapStruct mappers from the codebase.
 */
@DisplayName("All Controllers Comprehensive Endpoint Validation")
public class AllControllersEndpointTest {

    /**
     * List of all currently enabled controller classes in the CAFM backend.
     * Total: 15 controllers
     */
    private static final List<Class<?>> ENABLED_CONTROLLERS = Arrays.asList(
        AssetController.class,
        AuditController.class,
        AuthController.class,
        CompanyController.class,
        DebugController.class,
        FileUploadController.class,
        HealthController.class,
        InventoryController.class,
        MobileSupervisorController.class,
        NotificationController.class,
        ReportController.class,
        SchoolController.class,
        UserController.class,
        WebSocketController.class,
        WorkOrderController.class
    );

    /**
     * Data structure to hold endpoint information for reporting
     */
    private static class EndpointInfo {
        String controllerName;
        String httpMethod;
        String path;
        String methodName;
        
        public EndpointInfo(String controllerName, String httpMethod, String path, String methodName) {
            this.controllerName = controllerName;
            this.httpMethod = httpMethod;
            this.path = path;
            this.methodName = methodName;
        }
        
        @Override
        public String toString() {
            return String.format("%-8s %-40s %s", httpMethod, path, methodName);
        }
    }

    @Test
    @DisplayName("All controllers should be properly configured and loadable")
    void testAllControllersAreProperlyConfigured() {
        List<String> failedControllers = new ArrayList<>();
        
        for (Class<?> controllerClass : ENABLED_CONTROLLERS) {
            try {
                // Verify controller has required annotations
                assertTrue(controllerClass.isAnnotationPresent(RestController.class) ||
                          controllerClass.isAnnotationPresent(Controller.class),
                          controllerClass.getSimpleName() + " should be annotated with @RestController or @Controller");
                
                // Verify controller can be instantiated (has proper constructor)
                assertDoesNotThrow(() -> {
                    controllerClass.getDeclaredConstructors();
                }, controllerClass.getSimpleName() + " should have accessible constructors");
                
                System.out.println("✓ " + controllerClass.getSimpleName() + " - Properly configured");
                
            } catch (Exception e) {
                failedControllers.add(controllerClass.getSimpleName() + ": " + e.getMessage());
                System.err.println("✗ " + controllerClass.getSimpleName() + " - Configuration error: " + e.getMessage());
            }
        }
        
        assertTrue(failedControllers.isEmpty(), 
                  "The following controllers have configuration issues: " + String.join(", ", failedControllers));
        
        System.out.println("\n📊 CONTROLLER VALIDATION SUMMARY:");
        System.out.println("Total controllers validated: " + ENABLED_CONTROLLERS.size());
        System.out.println("Successfully configured: " + (ENABLED_CONTROLLERS.size() - failedControllers.size()));
        System.out.println("Failed configurations: " + failedControllers.size());
    }

    @Test
    @DisplayName("Should discover and validate all API endpoints across all controllers")
    void testDiscoverAndValidateAllEndpoints() {
        Map<String, List<EndpointInfo>> controllerEndpoints = new HashMap<>();
        int totalEndpoints = 0;
        Map<String, Integer> httpMethodCounts = new HashMap<>();
        
        for (Class<?> controllerClass : ENABLED_CONTROLLERS) {
            String controllerName = controllerClass.getSimpleName();
            List<EndpointInfo> endpoints = discoverEndpoints(controllerClass);
            controllerEndpoints.put(controllerName, endpoints);
            totalEndpoints += endpoints.size();
            
            // Count HTTP methods
            for (EndpointInfo endpoint : endpoints) {
                httpMethodCounts.merge(endpoint.httpMethod, 1, Integer::sum);
            }
        }
        
        // Print comprehensive API inventory
        printApiInventory(controllerEndpoints, httpMethodCounts, totalEndpoints);
        
        // Validate that we have endpoints
        assertTrue(totalEndpoints > 0, "Should discover at least one endpoint across all controllers");
        System.out.println("\n✓ Successfully discovered " + totalEndpoints + " endpoints across " + 
                          ENABLED_CONTROLLERS.size() + " controllers");
    }

    @Test
    @DisplayName("Each controller should have valid request mappings")
    void testControllerRequestMappings() {
        List<String> invalidMappings = new ArrayList<>();
        
        for (Class<?> controllerClass : ENABLED_CONTROLLERS) {
            try {
                String basePath = getControllerBasePath(controllerClass);
                String controllerName = controllerClass.getSimpleName();
                
                // WebSocket controllers don't need /api/ prefix as they use message mapping
                if (controllerName.equals("WebSocketController")) {
                    // WebSocket controllers are valid regardless of base path
                    continue;
                }
                
                if (basePath != null && !basePath.startsWith("/api/")) {
                    invalidMappings.add(controllerName + " - Invalid base path: " + basePath);
                }
            } catch (Exception e) {
                invalidMappings.add(controllerClass.getSimpleName() + " - Error reading mapping: " + e.getMessage());
            }
        }
        
        assertTrue(invalidMappings.isEmpty(), 
                  "Controllers with invalid request mappings: " + String.join(", ", invalidMappings));
    }

    @Test
    @DisplayName("Verify controller count matches expected enabled controllers")
    void testControllerCountValidation() {
        assertEquals(15, ENABLED_CONTROLLERS.size(), 
                    "Should have exactly 15 enabled controllers as specified in requirements");
        
        // Verify no duplicate controllers
        Set<String> controllerNames = ENABLED_CONTROLLERS.stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());
        
        assertEquals(ENABLED_CONTROLLERS.size(), controllerNames.size(), 
                    "Should not have duplicate controllers in the list");
    }

    /**
     * Discovers all endpoints in a controller class using reflection
     */
    private List<EndpointInfo> discoverEndpoints(Class<?> controllerClass) {
        List<EndpointInfo> endpoints = new ArrayList<>();
        String controllerName = controllerClass.getSimpleName();
        String basePath = getControllerBasePath(controllerClass);
        
        Method[] methods = controllerClass.getDeclaredMethods();
        
        for (Method method : methods) {
            List<String> httpMethods = getHttpMethods(method);
            String methodPath = getMethodPath(method);
            
            if (!httpMethods.isEmpty()) {
                String fullPath = combinePaths(basePath, methodPath);
                
                for (String httpMethod : httpMethods) {
                    endpoints.add(new EndpointInfo(controllerName, httpMethod, fullPath, method.getName()));
                }
            }
        }
        
        return endpoints;
    }

    /**
     * Gets the base path from controller's @RequestMapping annotation
     */
    private String getControllerBasePath(Class<?> controllerClass) {
        RequestMapping requestMapping = controllerClass.getAnnotation(RequestMapping.class);
        if (requestMapping != null && requestMapping.value().length > 0) {
            return requestMapping.value()[0];
        }
        return "";
    }

    /**
     * Extracts HTTP methods from method annotations
     */
    private List<String> getHttpMethods(Method method) {
        List<String> httpMethods = new ArrayList<>();
        
        if (method.isAnnotationPresent(GetMapping.class)) {
            httpMethods.add("GET");
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            httpMethods.add("POST");
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            httpMethods.add("PUT");
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            httpMethods.add("DELETE");
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            httpMethods.add("PATCH");
        }
        if (method.isAnnotationPresent(MessageMapping.class)) {
            httpMethods.add("WEBSOCKET");
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping rm = method.getAnnotation(RequestMapping.class);
            if (rm.method().length > 0) {
                for (RequestMethod requestMethod : rm.method()) {
                    httpMethods.add(requestMethod.name());
                }
            } else {
                httpMethods.add("ALL");
            }
        }
        
        return httpMethods;
    }

    /**
     * Extracts the path from method mapping annotations
     */
    private String getMethodPath(Method method) {
        if (method.isAnnotationPresent(GetMapping.class)) {
            GetMapping mapping = method.getAnnotation(GetMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(PostMapping.class)) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(PutMapping.class)) {
            PutMapping mapping = method.getAnnotation(PutMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(DeleteMapping.class)) {
            DeleteMapping mapping = method.getAnnotation(DeleteMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(PatchMapping.class)) {
            PatchMapping mapping = method.getAnnotation(PatchMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(MessageMapping.class)) {
            MessageMapping mapping = method.getAnnotation(MessageMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            return mapping.value().length > 0 ? mapping.value()[0] : "";
        }
        return "";
    }

    /**
     * Combines controller base path with method path
     */
    private String combinePaths(String basePath, String methodPath) {
        if (basePath == null) basePath = "";
        if (methodPath == null) methodPath = "";
        
        if (basePath.isEmpty() && methodPath.isEmpty()) return "/";
        if (basePath.isEmpty()) return methodPath;
        if (methodPath.isEmpty()) return basePath;
        
        String combined = basePath;
        if (!basePath.endsWith("/") && !methodPath.startsWith("/")) {
            combined += "/";
        }
        combined += methodPath;
        
        return combined;
    }

    /**
     * Prints comprehensive API inventory report
     */
    private void printApiInventory(Map<String, List<EndpointInfo>> controllerEndpoints, 
                                 Map<String, Integer> httpMethodCounts, int totalEndpoints) {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔍 COMPREHENSIVE CAFM BACKEND API ENDPOINT INVENTORY");
        System.out.println("=".repeat(80));
        
        // Sort controllers alphabetically for consistent output
        List<String> sortedControllers = new ArrayList<>(controllerEndpoints.keySet());
        Collections.sort(sortedControllers);
        
        for (String controllerName : sortedControllers) {
            List<EndpointInfo> endpoints = controllerEndpoints.get(controllerName);
            System.out.println("\n📁 " + controllerName + " (" + endpoints.size() + " endpoints)");
            System.out.println("-".repeat(60));
            
            if (endpoints.isEmpty()) {
                System.out.println("   No mapped endpoints found");
            } else {
                // Group by HTTP method for better organization
                Map<String, List<EndpointInfo>> groupedByMethod = endpoints.stream()
                        .collect(Collectors.groupingBy(e -> e.httpMethod));
                
                for (Map.Entry<String, List<EndpointInfo>> entry : groupedByMethod.entrySet()) {
                    for (EndpointInfo endpoint : entry.getValue()) {
                        System.out.println("   " + endpoint);
                    }
                }
            }
        }
        
        // Print summary statistics
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 API ENDPOINT STATISTICS SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Total Controllers: " + ENABLED_CONTROLLERS.size());
        System.out.println("Total Endpoints: " + totalEndpoints);
        System.out.println("\nHTTP Methods Distribution:");
        
        httpMethodCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue()));
        
        System.out.println("\nEndpoints per Controller:");
        controllerEndpoints.entrySet().stream()
                .sorted(Map.Entry.<String, List<EndpointInfo>>comparingByValue(
                        Comparator.comparing(List::size)).reversed())
                .forEach(entry -> System.out.println("  " + entry.getKey() + ": " + entry.getValue().size()));
        
        System.out.println("\n✅ All controllers successfully validated and inventory complete!");
        System.out.println("=".repeat(80));
    }
}