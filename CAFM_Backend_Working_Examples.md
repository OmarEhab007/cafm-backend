# CAFM Backend API Working Examples

## Authentication

### Login
```json
POST /api/v1/auth/login
{
    "email": "admin@cafm.com",
    "password": "admin123"
}
```

## Company APIs

### Create Company (Minimal - Required Fields Only)
```json
POST /api/v1/companies
{
    "name": "Test Company {{$timestamp}}",
    "country": "Saudi Arabia",
    "subscriptionPlan": "FREE"  // Must be: FREE, BASIC, PROFESSIONAL, or ENTERPRISE
}
```

### Create Company (Full)
```json
POST /api/v1/companies
{
    "name": "Full Test Company {{$timestamp}}",
    "displayName": "Full Test Display",
    "domain": "testcompany{{$timestamp}}.com",
    "subdomain": "test{{$timestamp}}",
    "contactEmail": "contact{{$timestamp}}@test.com",
    "contactPhone": "+966501234567",
    "primaryContactName": "John Doe",
    "industry": "Education",
    "country": "Saudi Arabia",
    "city": "Riyadh",
    "address": "123 Main Street",
    "postalCode": "12345",
    "taxNumber": "TAX123456",
    "commercialRegistration": "CR123456",
    "timezone": "Asia/Riyadh",
    "locale": "ar_SA",
    "currency": "SAR",
    "subscriptionPlan": "PROFESSIONAL",
    "maxUsers": 100,
    "maxSchools": 50,
    "maxSupervisors": 20,
    "maxTechnicians": 30,
    "maxStorageGb": 100,
    "settings": "{}",
    "dataClassification": "internal"
}
```

## School APIs

### Create School (Minimal)
```json
POST /api/v1/schools
{
    "code": "SCH{{$timestamp}}",
    "name": "Test School {{$timestamp}}",
    "type": "PRIMARY",  // PRIMARY, INTERMEDIATE, SECONDARY, HIGH_SCHOOL, KINDERGARTEN, UNIVERSITY
    "gender": "MIXED",  // BOYS, GIRLS, MIXED
    "city": "Riyadh"
}
```

### Create School (Full)
```json
POST /api/v1/schools
{
    "code": "SCH{{$timestamp}}",
    "name": "Full Test School {{$timestamp}}",
    "nameAr": "مدرسة اختبار",
    "type": "PRIMARY",
    "gender": "MIXED",
    "address": "456 School Street",
    "city": "Riyadh",
    "maintenanceScore": 85,
    "activityLevel": "HIGH"  // LOW, MEDIUM, HIGH
}
```

**Note**: Avoid sending `latitude` and `longitude` fields as there's a Jackson serialization issue with BigDecimal fields that expects them in array format.
```

## User APIs

### Create Admin User
```json
POST /api/v1/users
{
    "email": "admin{{$timestamp}}@test.com",
    "password": "SecurePass123@",  // Must have: uppercase, lowercase, digit, special char
    "firstName": "Admin",
    "lastName": "User",
    "phone": "+966501234567",
    "userType": "ADMIN",
    "companyId": "{{companyId}}"
}
```

### Create Supervisor User
```json
POST /api/v1/users
{
    "email": "supervisor{{$timestamp}}@test.com",
    "password": "SecurePass123@",
    "firstName": "Supervisor",
    "lastName": "User",
    "phone": "+966502345678",
    "userType": "SUPERVISOR",
    "companyId": "{{companyId}}",
    "employeeId": "EMP{{$timestamp}}",
    "department": "Maintenance"
}
```

### Create Technician User
```json
POST /api/v1/users
{
    "email": "tech{{$timestamp}}@test.com",
    "password": "SecurePass123@",
    "firstName": "Technician",
    "lastName": "User",
    "phone": "+966503456789",
    "userType": "TECHNICIAN",
    "companyId": "{{companyId}}",
    "employeeId": "TECH{{$timestamp}}",
    "department": "Electrical",
    "specialization": "ELECTRICAL",  // ELECTRICAL, PLUMBING, HVAC, CARPENTRY, etc.
    "skillLevel": "INTERMEDIATE"  // BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, MASTER
}
```

## Asset APIs

### Create Asset (Minimal)
```json
POST /api/v1/assets
{
    "assetCode": "ASSET-{{$timestamp}}",
    "name": "Test Asset",
    "schoolId": "{{schoolId}}"
}
```

### Create Asset (Full)
```json
POST /api/v1/assets
{
    "assetCode": "LAPTOP-{{$timestamp}}",
    "name": "Dell Latitude 5520",
    "nameAr": "لابتوب ديل",
    "description": "Business laptop for office use",
    "manufacturer": "Dell",
    "model": "Latitude 5520",
    "serialNumber": "DL{{$timestamp}}",
    "barcode": "123456789012",
    "purchaseDate": "2024-01-01",
    "purchaseOrderNumber": "PO-2024-001",
    "supplier": "Dell Technologies",
    "warrantyStartDate": "2024-01-01",
    "warrantyEndDate": "2027-01-01",
    "purchaseCost": 1500.00,
    "currentValue": 1500.00,
    "salvageValue": 100.00,
    "depreciationMethod": "straight_line",
    "schoolId": "{{schoolId}}",
    "department": "IT Department",
    "location": "Room 201",
    "maintenanceFrequencyDays": 90,
    "status": "ACTIVE",  // ACTIVE, INACTIVE, MAINTENANCE, RETIRED, DISPOSED
    "condition": "GOOD"  // EXCELLENT, GOOD, FAIR, POOR, BROKEN
}
```

## Report APIs

### Create Report (Minimal)
```json
POST /api/v1/reports
{
    "schoolId": "{{schoolId}}",
    "supervisorId": "{{supervisorId}}",
    "companyId": "{{companyId}}",
    "title": "Electrical Issue in Lab",
    "description": "The main electrical panel in the science lab is showing signs of overheating",
    "priority": "HIGH",  // LOW, MEDIUM, HIGH, URGENT
    "category": "electrical"
}
```

### Create Report (Full)
```json
POST /api/v1/reports
{
    "schoolId": "{{schoolId}}",
    "supervisorId": "{{supervisorId}}",
    "companyId": "{{companyId}}",
    "title": "HVAC System Malfunction",
    "description": "The central air conditioning system in Building A is not cooling properly",
    "priority": "URGENT",
    "category": "hvac",
    "location": "Building A - Second Floor",
    "building": "Building A",
    "floor": "2nd Floor",
    "roomNumber": "A-201",
    "damageAssessment": "System appears to be running but not cooling",
    "isUrgent": true,
    "isSafetyHazard": false,
    "photoUrls": [],
    "notes": "Students have been moved to Building B temporarily",
    "contactName": "Mr. Abdullah",
    "contactPhone": "+966504567890"
}
```

## Work Order APIs

### Create Work Order
```json
POST /api/v1/work-orders
{
    "reportId": "{{reportId}}",
    "title": "Fix Electrical Panel Overheating",
    "description": "Inspect and repair the overheating electrical panel",
    "priority": "HIGH",
    "category": "electrical",
    "estimatedHours": 4,
    "estimatedCost": 500.00,
    "assignedToId": "{{technicianId}}",
    "schoolId": "{{schoolId}}",
    "scheduledDate": "2024-01-20",
    "dueDate": "2024-01-22"
}
```

### Update Work Order Status
```json
PATCH /api/v1/work-orders/{{workOrderId}}/status
{
    "status": "IN_PROGRESS",  // PENDING, ASSIGNED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    "notes": "Technician has arrived on site"
}
```

## Common Validation Rules

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character
- Example: `SecurePass123@`

### Phone Number Format
- Pattern: `^\\+?[0-9]{7,15}$`
- Examples: `+966501234567`, `0501234567`

### Email Format
- Standard email validation
- Must be unique in the system

### Asset Code Format
- Pattern: `^[A-Z0-9_-]+$`
- Uppercase letters, numbers, underscores, and hyphens only
- Example: `LAPTOP-001`, `ASSET_2024`

### Domain Format
- Pattern: `^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$`
- Example: `example.com`, `subdomain.example.org`

### Subdomain Format
- Pattern: `^[a-z0-9-]+$`
- Lowercase letters, numbers, and hyphens only
- Example: `test-company`, `client123`

## Rate Limiting

The API has rate limiting enabled:
- Default: 20 requests per minute per IP
- If exceeded, returns 429 status with retry-after header
- Wait for the specified time before retrying

## Important Notes

1. **Company Creation**: The `subscriptionPlan` field must be exactly one of: `FREE`, `BASIC`, `PROFESSIONAL`, or `ENTERPRISE` (case-sensitive)

2. **School Creation**: Requires valid `schoolType` and `educationLevel` enum values

3. **User Creation**: 
   - Password must meet complexity requirements
   - `userType` must be one of: `ADMIN`, `SUPERVISOR`, `TECHNICIAN`
   - `companyId` must reference an existing company

4. **Asset Creation**: 
   - `assetCode` must be unique and follow the format
   - `schoolId` must reference an existing school

5. **Report Creation**: 
   - Requires valid `schoolId`, `supervisorId`, and `companyId`
   - `priority` must be a valid enum value

6. **Work Order Creation**: 
   - Can reference a `reportId` or be created independently
   - `assignedToId` should reference a technician user

## Testing Workflow

1. **Login** to get access token
2. **Create Company** (if needed)
3. **Create School** (required for assets and reports)
4. **Create Users** (supervisor for reports, technician for work orders)
5. **Create Assets** (optional, for asset management)
6. **Create Reports** (maintenance issues)
7. **Create Work Orders** (assign work to technicians)
8. **Update Status** as work progresses