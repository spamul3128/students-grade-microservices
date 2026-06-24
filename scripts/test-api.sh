#!/bin/bash
# Automated API Testing Script
# Tests all microservices endpoints with demonstration data

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Base URLs
AUTH_URL="http://localhost:8080"
INGESTION_URL="http://localhost:8081"
CALCULATION_URL="http://localhost:8082"
REPORTS_URL="http://localhost:8083"
AUDIT_URL="http://localhost:8084"

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[✓ PASS]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗ FAIL]${NC} $1"
}

log_test() {
    echo -e "${YELLOW}[TEST]${NC} $1"
}

# Test function with improved response handling
test_endpoint() {
    local method=$1
    local url=$2
    local data=$3
    local expected_status=$4
    local auth_header=$5
    local description=$6

    log_test "$description"

    local curl_cmd="curl -s -w '\n%{http_code}' -X $method $url"

    if [ ! -z "$data" ]; then
        curl_cmd="$curl_cmd -H 'Content-Type: application/json' -d '$data'"
    fi

    if [ ! -z "$auth_header" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $auth_header'"
    fi

    local response=$(eval "$curl_cmd")
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "$expected_status" ] || [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        log_success "$description - Status: $http_code"
        ((TESTS_PASSED++))
        if [ ! -z "$body" ]; then
            echo "    Response: $body" | head -n 3
        fi
        echo "$body"
    else
        log_error "$description - Expected: $expected_status, Got: $http_code"
        ((TESTS_FAILED++))
        if [ ! -z "$body" ]; then
            echo "    Response: $body"
        fi
        echo ""
    fi
}

# Wait for services to be ready
wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=0

    log_info "Waiting for $service_name to be ready..."

    while [ $attempt -lt $max_attempts ]; do
        if curl -s -f "$url/health" > /dev/null 2>&1; then
            log_success "$service_name is ready"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 2
    done

    log_error "$service_name failed to start"
    return 1
}

# Start testing
echo "=========================================="
echo "  API Testing Demo - Grade Microservices"
echo "=========================================="
echo ""

# Check if services are running
log_info "Checking service availability..."
wait_for_service "$AUTH_URL" "Auth Service"
wait_for_service "$INGESTION_URL" "Grade Ingestion Service"
wait_for_service "$CALCULATION_URL" "Grade Calculation Service"
wait_for_service "$REPORTS_URL" "Report Generation Service"
wait_for_service "$AUDIT_URL" "Audit Logging Service"

echo ""
log_info "Starting API tests..."
echo ""

# ============================================================
# Test Auth Service
# ============================================================
echo "=========================================="
echo "  Testing Auth Service (Port 8080)"
echo "=========================================="

# Health check
test_endpoint "GET" "$AUTH_URL/health" "" "200" "" "Auth Service Health Check"

# Register a student
REGISTER_RESPONSE=$(test_endpoint "POST" "$AUTH_URL/register" \
    '{"username":"testuser1","email":"test1@university.edu","password":"Test123!","role":"Student"}' \
    "201" "" "Register New Student")

# Login as student
LOGIN_RESPONSE=$(test_endpoint "POST" "$AUTH_URL/login" \
    '{"username":"testuser1","password":"Test123!"}' \
    "200" "" "Login as Student")

# Extract JWT token (if the response contains it)
JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | sed 's/"token":"//' || echo "")

if [ ! -z "$JWT_TOKEN" ]; then
    log_success "JWT Token obtained: ${JWT_TOKEN:0:20}..."
else
    log_error "Failed to obtain JWT token"
fi

# Register a teacher
test_endpoint "POST" "$AUTH_URL/register" \
    '{"username":"testteacher","email":"teacher@university.edu","password":"Teach123!","role":"Teacher"}' \
    "201" "" "Register New Teacher"

echo ""

# ============================================================
# Test Grade Ingestion Service
# ============================================================
echo "=========================================="
echo "  Testing Grade Ingestion (Port 8081)"
echo "=========================================="

test_endpoint "GET" "$INGESTION_URL/health" "" "200" "" "Grade Ingestion Health Check"

# Submit a grade (may require authentication)
test_endpoint "POST" "$INGESTION_URL/grades" \
    '{"studentId":"550e8400-e29b-41d4-a716-446655440001","assignmentId":"880e8400-e29b-41d4-a716-446655440001","score":95.5,"comments":"Excellent work"}' \
    "201" "$JWT_TOKEN" "Submit Single Grade"

# Get student grades
test_endpoint "GET" "$INGESTION_URL/students/550e8400-e29b-41d4-a716-446655440001/grades" \
    "" "200" "$JWT_TOKEN" "Get Student Grades"

echo ""

# ============================================================
# Test Grade Calculation Service
# ============================================================
echo "=========================================="
echo "  Testing Grade Calculation (Port 8082)"
echo "=========================================="

test_endpoint "GET" "$CALCULATION_URL/health" "" "200" "" "Grade Calculation Health Check"

# Calculate course grade
test_endpoint "POST" "$CALCULATION_URL/calculate/course/550e8400-e29b-41d4-a716-446655440001/770e8400-e29b-41d4-a716-446655440001" \
    "" "200" "$JWT_TOKEN" "Calculate Course Grade"

# Get cumulative GPA
test_endpoint "GET" "$CALCULATION_URL/students/550e8400-e29b-41d4-a716-446655440001/gpa/cumulative" \
    "" "200" "$JWT_TOKEN" "Get Cumulative GPA"

echo ""

# ============================================================
# Test Report Generation Service
# ============================================================
echo "=========================================="
echo "  Testing Report Generation (Port 8083)"
echo "=========================================="

test_endpoint "GET" "$REPORTS_URL/health" "" "200" "" "Report Generation Health Check"

# Generate report card
test_endpoint "GET" "$REPORTS_URL/students/550e8400-e29b-41d4-a716-446655440001/report-card/Fall/2024" \
    "" "200" "$JWT_TOKEN" "Generate Report Card"

# Generate transcript
test_endpoint "GET" "$REPORTS_URL/students/550e8400-e29b-41d4-a716-446655440001/transcript" \
    "" "200" "$JWT_TOKEN" "Generate Transcript"

echo ""

# ============================================================
# Test Audit Logging Service
# ============================================================
echo "=========================================="
echo "  Testing Audit Logging (Port 8084)"
echo "=========================================="

test_endpoint "GET" "$AUDIT_URL/health" "" "200" "" "Audit Logging Health Check"

# Get audit logs (may require admin privileges)
test_endpoint "GET" "$AUDIT_URL/audit-logs" \
    "" "200" "$JWT_TOKEN" "Get Audit Logs"

echo ""

# ============================================================
# Test Summary
# ============================================================
echo "=========================================="
echo "  Test Results Summary"
echo "=========================================="
TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
echo "Total Tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $TESTS_PASSED${NC}"
echo -e "${RED}Failed:       $TESTS_FAILED${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    log_success "All tests passed! 🎉"
    exit 0
else
    echo ""
    log_error "Some tests failed. Please check the logs above."
    exit 1
fi

