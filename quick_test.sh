#!/bin/bash

# Quick test to check if CSRF is disabled for API endpoints

echo "Testing login endpoint..."

# First, check if the endpoint exists with OPTIONS
echo "1. Testing OPTIONS request..."
curl -X OPTIONS http://localhost:8080/api/v1/auth/login -v

echo -e "\n\n2. Testing POST without CSRF token..."
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Requested-With: XMLHttpRequest" \
  -d '{"email": "admin@cafm.com", "password": "admin123"}' \
  -v

echo -e "\n\n3. Testing with different endpoint path..."
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "Origin: http://localhost:8080" \
  -d '{"email": "admin@cafm.com", "password": "admin123"}' \
  -v