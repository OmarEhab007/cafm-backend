# 🏫 CAFM Backend API - Postman Collection

**Comprehensive Postman Collection for Computer-Aided Facility Management System**

This collection provides complete API testing capabilities for the CAFM Backend with proper authentication, test automation, and realistic Saudi Arabia educational context data.

## 📦 Collection Contents

### 📁 Files Included:
- `CAFM-Backend-API-Extended.postman_collection.json` - **Complete API collection**
- `CAFM-Development.postman_environment.json` - **Development environment**
- `CAFM-Staging.postman_environment.json` - **Staging environment**
- `CAFM-Production.postman_environment.json` - **Production environment**
- `README.md` - **This documentation**

## 🚀 Quick Start Guide

### 1. **Import Collection & Environment**
```bash
# Import in Postman:
1. Open Postman
2. Click "Import" → Select files:
   - CAFM-Backend-API-Extended.postman_collection.json
   - CAFM-Development.postman_environment.json (or your target environment)
3. Select "CAFM Backend - Development" environment
```

### 2. **Configure Environment Variables**
Update the environment variables for your setup:
- **Development**: `http://localhost:8080`
- **Staging**: `https://api-staging.cafm.sa`
- **Production**: `https://api.cafm.sa`

### 3. **Authentication Setup**
```bash
# For Development Environment:
adminEmail: admin@cafm.dev
adminPassword: Admin123!
supervisorEmail: supervisor@cafm.dev
supervisorPassword: Supervisor123!
```

### 4. **Run Authentication Flow**
1. Execute: `🔐 Authentication → Login - Admin`
2. Token will be automatically stored in environment
3. All subsequent requests will use the token automatically

## 📋 Collection Structure

### 🔐 **Authentication**
- **Login - Admin** - Administrative access with full permissions
- **Login - Supervisor** - Field operations and report management
- **Login - Technician** - Work order execution and maintenance
- **Refresh Token** - Automatic token renewal
- **Get Current User Profile** - User information retrieval
- **Logout** - Session termination
- **Forgot Password** - Password reset initiation
- **Reset Password** - Password reset completion

### 👥 **User Management**
- **Get All Users** - Paginated user listing with filtering
- **Create User - Supervisor** - New user creation with Saudi context
- **Get User by ID** - Individual user details
- **Update User** - User information modification
- **Delete User** - Soft user deletion

### 🏢 **Company Management**
- **Get All Companies** - Multi-tenant company listing
- **Create Company** - New educational district setup

### 🏫 **School Management**
- **Get All Schools** - Educational facility listing
- **Create School** - New school registration with Arabic names

### 🏭 **Asset Management**
- **Get All Assets** - Physical asset inventory
- **Create Asset - HVAC System** - Equipment registration example
- **Update Asset Status** - Asset status management (Active, Maintenance, etc.)

### 📋 **Report Management**
- **Get All Reports** - Maintenance issue tracking
- **Create Report - AC Problem** - Real-world maintenance scenario
- **Review Report** - Supervisor approval workflow

### 🔧 **Work Order Management**
- **Get All Work Orders** - Task management system
- **Create Work Order** - Work assignment creation
- **Assign Work Order** - Technician assignment
- **Start Work Order** - Task initiation
- **Complete Work Order** - Task completion with documentation

### 📦 **Inventory Management**
- **Get All Inventory Items** - Stock management
- **Create Inventory Item** - New stock item registration
- **Stock In - Receive Items** - Inventory replenishment
- **Stock Out - Issue Items** - Item distribution

### 📱 **Mobile Supervisor API**
- **Get Dashboard Data** - Mobile dashboard information
- **Sync Mobile Data** - Offline synchronization

### 📁 **File Management**
- **Upload File** - Document and image upload
- **Download File** - File retrieval

### 🔔 **Notifications**
- **Get My Notifications** - User notification center
- **Register FCM Token** - Push notification setup
- **Mark Notifications as Read** - Notification management

### 📊 **Analytics & Reporting**
- **Get Dashboard Statistics** - Performance metrics
- **Export Reports to Excel** - Data export functionality

### 🔍 **Audit Logs**
- **Get Audit Logs** - Security and compliance tracking
- **Get Security Events** - Security incident monitoring

### ⚠️ **Error Scenarios**
- **Unauthorized Request** - 401 error testing
- **Invalid Login Credentials** - Authentication failure
- **Resource Not Found** - 404 error testing
- **Validation Error** - 400 error testing

### 🎯 **Health Check**
- **Application Health** - Service status monitoring
- **Actuator Health** - Detailed health information

## 🧪 Testing Features

### **Automated Tests**
Each request includes comprehensive test scripts:
```javascript
// Response validation
pm.test('Response status should be successful', function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 201, 204]);
});

// Response time validation
pm.test('Response time should be acceptable', function () {
    pm.expect(pm.response.responseTime).to.be.below(5000);
});

// Data validation
pm.test('Response contains required fields', function () {
    const responseJson = pm.response.json();
    pm.expect(responseJson).to.have.property('id');
});
```

### **Dynamic Data Generation**
Automatic generation of realistic test data:
```javascript
// Arabic names for Saudi context
const arabicFirstNames = ['أحمد', 'محمد', 'عبدالله', 'سارة', 'فاطمة'];
const saudiCities = ['الرياض', 'جدة', 'مكة المكرمة', 'المدينة المنورة'];

// Saudi phone numbers
pm.environment.set('testPhoneNumber', '+966' + Math.floor(Math.random() * 900000000 + 100000000));
```

### **Token Management**
Automatic JWT token handling:
```javascript
// Auto token extraction and storage
if (responseJson.accessToken) {
    pm.environment.set('accessToken', responseJson.accessToken);
    pm.environment.set('refreshToken', responseJson.refreshToken);
    
    // Calculate token expiry
    const expiresIn = responseJson.expiresIn || 3600;
    const expiry = Date.now() + (expiresIn * 1000);
    pm.environment.set('tokenExpiry', expiry);
}
```

## 🌍 Multi-Environment Support

### **Development Environment**
```json
{
  "url": "http://localhost:8080",
  "adminEmail": "admin@cafm.dev",
  "adminPassword": "Admin123!"
}
```

### **Staging Environment**
```json
{
  "url": "https://api-staging.cafm.sa",
  "adminEmail": "admin@staging.cafm.sa"
}
```

### **Production Environment**
```json
{
  "url": "https://api.cafm.sa",
  // Credentials should be set securely
}
```

## 🔒 Security Best Practices

### **Development Testing**
- ✅ Use provided test credentials for development
- ✅ Test data includes realistic Saudi educational context
- ✅ All sensitive data properly handled

### **Staging/Production Testing**
- ⚠️ **NEVER commit production credentials**
- ⚠️ Use environment-specific secure passwords
- ⚠️ Limit production testing to necessary scenarios
- ⚠️ Always test in staging first

### **Token Security**
- 🔐 Tokens stored as secret environment variables
- 🔐 Automatic token expiry handling
- 🔐 Secure token refresh mechanism

## 📊 Running Test Scenarios

### **Complete Workflow Test**
```bash
1. Authentication → Login - Admin
2. User Management → Create User - Supervisor
3. School Management → Create School
4. Asset Management → Create Asset - HVAC System
5. Report Management → Create Report - AC Problem
6. Work Order Management → Create Work Order
7. Work Order Management → Assign Work Order
8. Work Order Management → Complete Work Order
```

### **Error Testing**
```bash
1. Error Scenarios → Unauthorized Request (No Token)
2. Error Scenarios → Invalid Login Credentials
3. Error Scenarios → Resource Not Found
4. Error Scenarios → Validation Error
```

### **Performance Testing**
Run collection with iterations to test:
- Response times under load
- Token refresh handling
- Pagination performance
- File upload/download performance

## 🎯 Collection Runner Usage

### **Full Collection Run**
```bash
1. Select collection: "CAFM Backend API - Complete Collection"
2. Select environment: "CAFM Backend - Development"
3. Set iterations: 1 (or more for stress testing)
4. Enable "Keep variable values"
5. Run collection
```

### **Specific Folder Run**
```bash
# Test only authentication
1. Select folder: "🔐 Authentication"
2. Run → Review results

# Test user management workflow
1. Select folder: "👥 User Management"
2. Run → Review results
```

### **Custom Test Scenarios**
Create custom test runs by:
1. Selecting specific requests
2. Arranging in logical order
3. Running with appropriate environment
4. Validating results in test report

## 📝 Example Test Data

### **Saudi Educational Context**
```json
{
  "schoolName": "King Abdulaziz Elementary School",
  "schoolNameAr": "مدرسة الملك عبدالعزيز الابتدائية",
  "city": "الرياض",
  "district": "Riyadh Education District",
  "phoneNumber": "+966112345678",
  "userName": "أحمد المحمد",
  "userEmail": "ahmed.mohammed@edu.sa"
}
```

### **Asset Management Examples**
```json
{
  "assetName": "Central AC Unit - Building A",
  "assetNameAr": "وحدة التكييف المركزي - المبنى أ",
  "category": "HVAC",
  "manufacturer": "Samsung",
  "model": "DVM S ECO",
  "location": "Building A, Roof Level",
  "locationAr": "المبنى أ، مستوى السطح"
}
```

## 🛠️ Troubleshooting

### **Common Issues**

1. **Token Expiry**
   ```bash
   Issue: 401 Unauthorized responses
   Solution: Run "Refresh Token" or re-login
   ```

2. **Environment Variables Missing**
   ```bash
   Issue: {{variable}} not resolved
   Solution: Ensure correct environment selected
   ```

3. **Test Failures**
   ```bash
   Issue: Tests failing unexpectedly
   Solution: Check API server status and environment URLs
   ```

4. **File Upload Issues**
   ```bash
   Issue: File upload endpoints failing
   Solution: Ensure file path is correct and file exists
   ```

### **Debug Tips**
- Enable Postman Console for detailed request/response logs
- Check environment variables are properly set
- Verify API server is running on expected URL
- Test authentication first before other endpoints
- Use realistic test data for better testing

## 📞 Support & Documentation

- **API Documentation**: Available at `{{baseUrl}}/swagger-ui.html`
- **OpenAPI Spec**: Available at `{{baseUrl}}/v3/api-docs`
- **Health Check**: Available at `{{baseUrl}}/health`
- **Issues**: Report at project repository

## 🎉 Happy Testing!

This comprehensive collection provides everything needed to test the CAFM Backend API effectively. From authentication to complex workflows, all scenarios are covered with realistic Saudi educational context data and proper error handling.

**Pro Tips:**
- Start with authentication before testing other endpoints
- Use the collection runner for automated testing
- Leverage environment variables for different deployment targets
- Review test results for performance and reliability insights
- Customize test data for your specific use cases