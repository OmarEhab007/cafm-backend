#!/bin/bash

# CAFM Backend - Comprehensive Test Runner
# This script runs all controller API tests in sequence

echo "=========================================="
echo "CAFM Backend - API Test Suite Runner"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run a test category
run_test_category() {
    local category=$1
    local test_class=$2
    
    echo -e "${YELLOW}Running $category tests...${NC}"
    echo "----------------------------------------"
    
    if mvn test -Dtest="$test_class" -q; then
        echo -e "${GREEN}✓ $category tests PASSED${NC}"
        ((PASSED_TESTS++))
    else
        echo -e "${RED}✗ $category tests FAILED${NC}"
        ((FAILED_TESTS++))
    fi
    ((TOTAL_TESTS++))
    echo ""
}

# Start time
START_TIME=$(date +%s)

# Ensure we're in the right directory
cd "$(dirname "$0")" || exit 1

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed. Please install Maven first.${NC}"
    exit 1
fi

# Start Docker containers if not running
echo "Starting test infrastructure..."
docker compose up -d postgres redis minio
sleep 5

# Clean and compile
echo "Cleaning and compiling project..."
mvn clean compile -q

echo ""
echo "=========================================="
echo "Running Integration Tests"
echo "=========================================="
echo ""

# Run each test category
run_test_category "Authentication & User Management" "AuthControllerIntegrationTest,UserControllerIntegrationTest"
run_test_category "Company & Tenant Management" "CompanyControllerIntegrationTest"
run_test_category "Report & Work Order Workflow" "ReportWorkOrderControllerIntegrationTest"
run_test_category "Security Vulnerability" "SecurityVulnerabilityTest"

echo "=========================================="
echo "Running Performance Tests"
echo "=========================================="
echo ""

# Performance tests (run separately as they can be intensive)
echo -e "${YELLOW}Running performance tests (this may take a while)...${NC}"
if mvn test -Dtest="PerformanceLoadTest" -Dgroups="performance" -q; then
    echo -e "${GREEN}✓ Performance tests PASSED${NC}"
    ((PASSED_TESTS++))
else
    echo -e "${RED}✗ Performance tests FAILED${NC}"
    ((FAILED_TESTS++))
fi
((TOTAL_TESTS++))

echo ""
echo "=========================================="
echo "Generating Test Reports"
echo "=========================================="
echo ""

# Generate test reports
mvn surefire-report:report -q
mvn jacoco:report -q

# Calculate test coverage
echo "Calculating test coverage..."
COVERAGE=$(mvn jacoco:report -q | grep -oP 'Total.*?\K[0-9]+(?=%)' | head -1)

# End time
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# Summary
echo ""
echo "=========================================="
echo "Test Execution Summary"
echo "=========================================="
echo ""
echo "Total Test Categories: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
if [ -n "$COVERAGE" ]; then
    echo "Code Coverage: $COVERAGE%"
fi
echo "Execution Time: ${DURATION} seconds"
echo ""

# Generate HTML report location
echo "Test reports available at:"
echo "  - target/surefire-reports/index.html"
echo "  - target/site/jacoco/index.html"
echo ""

# Exit with appropriate code
if [ $FAILED_TESTS -gt 0 ]; then
    echo -e "${RED}Some tests failed. Please review the test reports.${NC}"
    exit 1
else
    echo -e "${GREEN}All tests passed successfully!${NC}"
    exit 0
fi