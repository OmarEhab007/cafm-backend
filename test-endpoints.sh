#!/bin/bash

# Login and get token
echo "=== 1. Testing Login ==="
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@cafm.local", "password": "admin123"}')

TOKEN=$(echo $RESPONSE | jq -r .accessToken)
COMPANY_ID=$(echo $RESPONSE | jq -r .companyId)
USER_ID=$(echo $RESPONSE | jq -r .userId)

echo "Token: ${TOKEN:0:50}..."
echo "Company ID: $COMPANY_ID"
echo "User ID: $USER_ID"

echo -e "\n=== 2. Testing GET /users (List Users) ==="
curl -s -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" | jq '.content | length'

echo -e "\n=== 3. Testing GET /dashboard/stats ==="
curl -s -X GET http://localhost:8080/api/v1/dashboard/stats \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n=== 4. Testing GET /notifications ==="
curl -s -X GET http://localhost:8080/api/v1/notifications \
  -H "Authorization: Bearer $TOKEN" | jq '.'

echo -e "\n=== 5. Testing GET /companies/$COMPANY_ID ==="
curl -s -X GET "http://localhost:8080/api/v1/companies/$COMPANY_ID" \
  -H "Authorization: Bearer $TOKEN" | jq '.'