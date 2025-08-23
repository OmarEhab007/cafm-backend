#!/bin/bash

# Login and get token
echo "Logging in..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@cafm.local", "password": "admin123"}')

TOKEN=$(echo $RESPONSE | jq -r .accessToken)
echo "Token obtained: ${TOKEN:0:50}..."

# Create unique user
TIMESTAMP=$(date +%s)
EMAIL="testuser${TIMESTAMP}@cafm.com"

echo -e "\nCreating user with email: $EMAIL"
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "email": "'$EMAIL'",
    "password": "SecurePass123@",
    "firstName": "Test",
    "lastName": "User",
    "userType": "TECHNICIAN",
    "companyId": "00000000-0000-0000-0000-000000000001",
    "phoneNumber": "+966509999999"
  }' | jq .