# DTO (Data Transfer Objects) Structure

All DTOs are organized in this directory following a domain-based structure.

## Directory Structure

```
dto/
├── achievement/          # School achievement submissions
│   ├── SchoolAchievementRequest.java
│   └── SchoolAchievementResponse.java
│
├── attendance/           # Supervisor attendance tracking
│   ├── SupervisorAttendanceRequest.java
│   └── SupervisorAttendanceResponse.java
│
├── auth/                 # Authentication and authorization
│   ├── ChangePasswordRequest.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── LogoutRequest.java
│   ├── ResetPasswordConfirmRequest.java
│   ├── ResetPasswordRequest.java
│   ├── TokenRefreshRequest.java
│   └── TokenRefreshResponse.java
│
├── common/               # Shared/common DTOs
│   ├── PageRequest.java
│   ├── PageResponse.java
│   └── PageUtils.java
│
├── damage/               # Damage count tracking
│   ├── DamageCountRequest.java
│   └── DamageCountResponse.java
│
├── dashboard/            # Dashboard data
│   ├── AdminDashboardResponse.java
│   ├── SupervisorDashboardResponse.java
│   └── TechnicianDashboardResponse.java
│
├── file/                 # File upload/download
│   ├── FileUploadRequest.java
│   └── FileUploadResponse.java
│
├── maintenance/          # Maintenance count tracking
│   ├── MaintenanceCountRequest.java
│   ├── MaintenanceCountResponse.java
│   └── MaintenanceCountUpdateRequest.java
│
├── notification/         # Push notifications and FCM
│   ├── FCMTokenRequest.java
│   ├── NotificationRequest.java
│   ├── NotificationResponse.java
│   └── PushNotificationRequest.java
│
├── offline/              # Offline sync support
│   └── OfflineDataPackage.java
│
├── report/               # Maintenance reports
│   ├── ReportCreateRequest.java
│   ├── ReportDetailResponse.java
│   ├── ReportListResponse.java
│   ├── ReportReviewRequest.java
│   └── ReportUpdateRequest.java
│
├── sync/                 # Data synchronization
│   ├── SyncRequest.java
│   └── SyncResponse.java
│
├── user/                 # User management
│   ├── UserCreateRequest.java
│   ├── UserResponse.java
│   └── UserUpdateRequest.java
│
└── workorder/            # Work order management
    ├── TaskUpdateRequest.java
    ├── WorkOrderAssignRequest.java
    ├── WorkOrderCreateRequest.java
    ├── WorkOrderDetailResponse.java
    ├── WorkOrderListResponse.java
    └── WorkOrderProgressRequest.java
```

## Naming Conventions

- **Request DTOs**: Used for incoming data (POST, PUT, PATCH requests)
  - Named as `{Entity}Request` or `{Entity}{Action}Request`
  - Example: `LoginRequest`, `UserCreateRequest`

- **Response DTOs**: Used for outgoing data (GET responses)
  - Named as `{Entity}Response` or `{Entity}{View}Response`
  - Example: `LoginResponse`, `ReportDetailResponse`

- **Update DTOs**: Used for partial updates (PATCH requests)
  - Named as `{Entity}UpdateRequest`
  - Example: `MaintenanceCountUpdateRequest`

## Package Structure

All DTOs follow the package pattern:
```
com.cafm.cafmbackend.dto.{domain}
```

Example:
- `com.cafm.cafmbackend.dto.auth`
- `com.cafm.cafmbackend.dto.user`
- `com.cafm.cafmbackend.dto.report`

## Features

### Record-based DTOs
All new DTOs use Java records for immutability and conciseness:
```java
public record LoginRequest(
    @NotNull String email,
    @NotNull String password
) {}
```

### Validation
DTOs include Jakarta Bean Validation annotations:
```java
@NotNull(message = "Email is required")
@Email(message = "Invalid email format")
String email
```

### Builder Pattern
Complex DTOs provide builder patterns for construction:
```java
MaintenanceCountRequest.builder()
    .schoolId(schoolId)
    .itemCounts(counts)
    .build();
```

### Calculated Metrics
Response DTOs may include calculated metrics:
```java
public record DamageCountResponse(
    // ... fields ...
    DamageAnalytics analytics
) {
    public record DamageAnalytics(
        Double damageRate,
        String severityLevel,
        // ... more metrics ...
    ) {}
}
```

## JSONB Support

DTOs handling PostgreSQL JSONB fields use Map or List types:
```java
Map<String, Integer> itemCounts,
Map<String, Object> customFields,
List<String> photos
```

## Usage with MapStruct

All DTOs are designed to work with MapStruct mappers located in:
```
com.cafm.cafmbackend.api.mappers
```

Example mapper usage:
```java
@Mapper(config = MapperConfig.class)
public interface UserMapper {
    UserResponse toResponse(User entity);
    User toEntity(UserCreateRequest request);
}
```