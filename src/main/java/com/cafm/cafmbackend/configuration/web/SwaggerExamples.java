package com.cafm.cafmbackend.configuration.web;

/**
 * Swagger/OpenAPI examples for consistent API documentation.
 * 
 * Purpose: Provide reusable example data for API documentation
 * Pattern: Constants class with nested example classes
 * Java 23: Uses text blocks for readable JSON examples
 * Architecture: API documentation support
 * Standards: OpenAPI 3.0 example specification
 */
public final class SwaggerExamples {
    
    private SwaggerExamples() {
        // Utility class
    }
    
    public static final class Auth {
        public static final String LOGIN_REQUEST = """
            {
              "email": "admin@school.edu.sa",
              "password": "SecurePassword123!",
              "rememberMe": false,
              "deviceName": "iPhone 14 Pro"
            }
            """;
        
        public static final String LOGIN_RESPONSE = """
            {
              "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
              "refreshToken": "def50200a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef",
              "tokenType": "Bearer",
              "expiresIn": 3600,
              "userId": "123e4567-e89b-12d3-a456-426614174000",
              "email": "admin@school.edu.sa",
              "firstName": "Ahmed",
              "lastName": "Al-Mohammed",
              "firstNameAr": "أحمد",
              "lastNameAr": "المحمد",
              "userType": "ADMIN",
              "roles": ["ADMIN", "USER"],
              "permissions": ["READ_USERS", "WRITE_REPORTS", "MANAGE_ASSETS"],
              "companyId": "456e7890-a12b-34c5-d678-901234567890",
              "companyName": "Ministry of Education - Riyadh",
              "companyStatus": "ACTIVE",
              "subscriptionPlan": "ENTERPRISE",
              "sessionId": "sess_789012345",
              "loginTime": "2024-01-15T10:30:00",
              "lastLoginTime": "2024-01-14T14:20:00",
              "language": "ar",
              "timezone": "Asia/Riyadh",
              "theme": "light",
              "isFirstLogin": false,
              "mustChangePassword": false,
              "twoFactorEnabled": true,
              "avatarUrl": "https://api.cafm.sa/avatars/user123.jpg"
            }
            """;
        
        public static final String ERROR_RESPONSE = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/auth/login",
              "code": "INVALID_CREDENTIALS",
              "message": "Invalid email or password",
              "status": 401
            }
            """;
    }
    
    public static final class User {
        public static final String CREATE_REQUEST = """
            {
              "email": "supervisor@school.edu.sa",
              "firstName": "Sara",
              "lastName": "Al-Zahra",
              "firstNameAr": "سارة",
              "lastNameAr": "الزهرة",
              "userType": "SUPERVISOR",
              "phoneNumber": "+966501234567",
              "isActive": true,
              "schoolIds": ["123e4567-e89b-12d3-a456-426614174000"],
              "language": "ar",
              "timezone": "Asia/Riyadh"
            }
            """;
        
        public static final String USER_RESPONSE = """
            {
              "id": "987f6543-a21b-43c5-d876-109876543210",
              "email": "supervisor@school.edu.sa",
              "firstName": "Sara",
              "lastName": "Al-Zahra",
              "firstNameAr": "سارة",
              "lastNameAr": "الزهرة",
              "userType": "SUPERVISOR",
              "phoneNumber": "+966501234567",
              "isActive": true,
              "createdAt": "2024-01-15T08:00:00",
              "updatedAt": "2024-01-15T10:30:00",
              "lastLoginAt": "2024-01-15T09:15:00",
              "avatarUrl": "https://api.cafm.sa/avatars/supervisor456.jpg",
              "language": "ar",
              "timezone": "Asia/Riyadh"
            }
            """;
    }
    
    public static final class Report {
        public static final String CREATE_REQUEST = """
            {
              "title": "Broken Air Conditioner in Classroom 205",
              "titleAr": "مكيف هواء معطل في الفصل 205",
              "description": "The air conditioning unit in classroom 205 is not working properly. Room temperature is too high for students.",
              "descriptionAr": "وحدة تكييف الهواء في الفصل 205 لا تعمل بشكل صحيح. درجة حرارة الغرفة مرتفعة جداً للطلاب.",
              "category": "HVAC",
              "priority": "HIGH",
              "schoolId": "123e4567-e89b-12d3-a456-426614174000",
              "assetId": "456e7890-a12b-34c5-d678-901234567890",
              "location": "Building A, Floor 2, Room 205",
              "locationAr": "المبنى أ، الطابق الثاني، الغرفة 205",
              "images": [
                "https://api.cafm.sa/uploads/reports/img1.jpg",
                "https://api.cafm.sa/uploads/reports/img2.jpg"
              ],
              "estimatedCost": 1500.00,
              "requiredDate": "2024-01-20T00:00:00"
            }
            """;
        
        public static final String REPORT_RESPONSE = """
            {
              "id": "789a0123-b45c-67d8-e901-234567890abc",
              "title": "Broken Air Conditioner in Classroom 205",
              "titleAr": "مكيف هواء معطل في الفصل 205",
              "description": "The air conditioning unit in classroom 205 is not working properly. Room temperature is too high for students.",
              "descriptionAr": "وحدة تكييف الهواء في الفصل 205 لا تعمل بشكل صحيح. درجة حرارة الغرفة مرتفعة جداً للطلاب.",
              "category": "HVAC",
              "priority": "HIGH",
              "status": "PENDING_REVIEW",
              "schoolId": "123e4567-e89b-12d3-a456-426614174000",
              "schoolName": "King Abdulaziz Elementary School",
              "assetId": "456e7890-a12b-34c5-d678-901234567890",
              "assetName": "Central AC Unit - Building A",
              "location": "Building A, Floor 2, Room 205",
              "locationAr": "المبنى أ، الطابق الثاني، الغرفة 205",
              "reportedById": "987f6543-a21b-43c5-d876-109876543210",
              "reportedByName": "Sara Al-Zahra",
              "images": [
                "https://api.cafm.sa/uploads/reports/img1.jpg",
                "https://api.cafm.sa/uploads/reports/img2.jpg"
              ],
              "estimatedCost": 1500.00,
              "actualCost": null,
              "requiredDate": "2024-01-20T00:00:00",
              "createdAt": "2024-01-15T10:30:00",
              "updatedAt": "2024-01-15T10:30:00",
              "reviewedAt": null,
              "completedAt": null
            }
            """;
    }
    
    public static final class WorkOrder {
        public static final String CREATE_REQUEST = """
            {
              "reportId": "789a0123-b45c-67d8-e901-234567890abc",
              "title": "AC Repair - Classroom 205",
              "titleAr": "إصلاح المكيف - الفصل 205",
              "description": "Replace faulty compressor and clean filters",
              "descriptionAr": "استبدال الضاغط المعطل وتنظيف المرشحات",
              "assignedToId": "abc123de-f456-789a-bcde-f0123456789a",
              "priority": "HIGH",
              "scheduledDate": "2024-01-18T09:00:00",
              "estimatedHours": 4.0,
              "requiredParts": [
                "Compressor Model XYZ-123",
                "Air Filter Set",
                "Refrigerant R410A"
              ],
              "specialInstructions": "Ensure classroom is empty during repair work"
            }
            """;
        
        public static final String WORK_ORDER_RESPONSE = """
            {
              "id": "def456gh-i789-012j-klmn-opqrstuvwxyz",
              "reportId": "789a0123-b45c-67d8-e901-234567890abc",
              "title": "AC Repair - Classroom 205",
              "titleAr": "إصلاح المكيف - الفصل 205",
              "description": "Replace faulty compressor and clean filters",
              "descriptionAr": "استبدال الضاغط المعطل وتنظيف المرشحات",
              "status": "ASSIGNED",
              "priority": "HIGH",
              "assignedToId": "abc123de-f456-789a-bcde-f0123456789a",
              "assignedToName": "Mohammed Al-Rashid",
              "schoolId": "123e4567-e89b-12d3-a456-426614174000",
              "schoolName": "King Abdulaziz Elementary School",
              "scheduledDate": "2024-01-18T09:00:00",
              "estimatedHours": 4.0,
              "actualHours": null,
              "estimatedCost": 1500.00,
              "actualCost": null,
              "requiredParts": [
                "Compressor Model XYZ-123",
                "Air Filter Set",
                "Refrigerant R410A"
              ],
              "specialInstructions": "Ensure classroom is empty during repair work",
              "createdAt": "2024-01-15T11:00:00",
              "updatedAt": "2024-01-15T11:00:00",
              "startedAt": null,
              "completedAt": null,
              "progress": 0
            }
            """;
    }
    
    public static final class Pagination {
        public static final String PAGE_RESPONSE = """
            {
              "content": [
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "name": "Sample Item"
                }
              ],
              "pageable": {
                "pageNumber": 0,
                "pageSize": 20,
                "sort": {
                  "sorted": true,
                  "ascending": false,
                  "empty": false
                }
              },
              "totalElements": 150,
              "totalPages": 8,
              "size": 20,
              "number": 0,
              "numberOfElements": 20,
              "first": true,
              "last": false,
              "empty": false
            }
            """;
    }
    
    public static final class Error {
        public static final String VALIDATION_ERROR = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/users",
              "code": "VALIDATION_FAILED",
              "message": "Validation failed for request",
              "status": 400,
              "details": [
                {
                  "field": "email",
                  "message": "Invalid email format",
                  "rejectedValue": "invalid-email"
                },
                {
                  "field": "firstName",
                  "message": "First name is required",
                  "rejectedValue": ""
                }
              ]
            }
            """;
        
        public static final String UNAUTHORIZED_ERROR = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/users/123",
              "code": "AUTHENTICATION_REQUIRED",
              "message": "Authentication is required to access this resource",
              "status": 401
            }
            """;
        
        public static final String FORBIDDEN_ERROR = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/admin/users",
              "code": "INSUFFICIENT_PERMISSIONS",
              "message": "Insufficient permissions to access this resource",
              "status": 403
            }
            """;
        
        public static final String NOT_FOUND_ERROR = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/users/123e4567-e89b-12d3-a456-426614174000",
              "code": "RESOURCE_NOT_FOUND",
              "message": "User not found",
              "status": 404
            }
            """;
        
        public static final String RATE_LIMIT_ERROR = """
            {
              "timestamp": "2024-01-15T10:30:00Z",
              "path": "/api/v1/auth/login",
              "code": "RATE_LIMIT_EXCEEDED",
              "message": "Too many requests. Please try again later.",
              "status": 429
            }
            """;
    }
}