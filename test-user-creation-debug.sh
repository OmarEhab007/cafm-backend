#!/bin/bash

# Test user creation with different approaches
echo "=== Testing User Creation ==="

# 1. Login and get token
echo "1. Getting auth token..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@cafm.local", "password": "admin123"}')

TOKEN=$(echo $RESPONSE | jq -r .accessToken)
COMPANY_ID=$(echo $RESPONSE | jq -r .companyId)

echo "Token: ${TOKEN:0:50}..."
echo "Company ID: $COMPANY_ID"

# 2. Try minimal user creation
TIMESTAMP=$(date +%s)
EMAIL="minimaluser${TIMESTAMP}@cafm.com"

echo -e "\n2. Creating minimal user with email: $EMAIL"
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"email\": \"$EMAIL\",
    \"password\": \"SecurePass123@\",
    \"firstName\": \"Minimal\",
    \"lastName\": \"User\",
    \"userType\": \"TECHNICIAN\",
    \"companyId\": \"$COMPANY_ID\"
  }" | jq .

# 3. Check if it's a UUID format issue
echo -e "\n3. Testing with explicit UUID string..."
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"email\": \"uuid${TIMESTAMP}@cafm.com\",
    \"password\": \"SecurePass123@\",
    \"firstName\": \"UUID\",
    \"lastName\": \"Test\",
    \"userType\": \"TECHNICIAN\",
    \"companyId\": \"00000000-0000-0000-0000-000000000001\"
  }" | jq .