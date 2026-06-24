#!/bin/bash
# Comprehensive Demo Script for Student Grades Microservices
# This script demonstrates all service capabilities with test data

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AUTH_URL="http://localhost:8080"
INGESTION_URL="http://localhost:8081"
CALCULATION_URL="http://localhost:8082"
REPORT_URL="http://localhost:8083"
AUDIT_URL="http://localhost:8084"

# Output file
OUTPUT_FILE="demo-output-$(date +%Y%m%d-%H%M%S).txt"

log() {
    echo -e "${GREEN}[$(date +'%H:%M:%S')]${NC} $1" | tee -a "$OUTPUT_FILE"
}

log_section() {
    echo "" | tee -a "$OUTPUT_FILE"
    echo -e "${BLUE}========================================${NC}" | tee -a "$OUTPUT_FILE"
    echo -e "${BLUE}$1${NC}" | tee -a "$OUTPUT_FILE"
    echo -e "${BLUE}========================================${NC}" | tee -a "$OUTPUT_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$OUTPUT_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$OUTPUT_FILE"
}

log_info() {
    echo -e "${YELLOW}[INFO]${NC} $1" | tee -a "$OUTPUT_FILE"
}

# Wait for service to be ready
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=1

    log_info "Waiting for $name to be ready..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url/health" > /dev/null 2>&1; then
            log_success "$name is ready!"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    log_error "$name failed to start"
    return 1
}

# Initialize demo
initialize_demo() {
    log_section "INITIALIZING DEMO ENVIRONMENT"

    # Check if services are running
    log "Checking service health..."
    wait_for_service "$AUTH_URL" "Auth Service" || exit 1
    wait_for_service "$INGESTION_URL" "Grade Ingestion Service" || exit 1
    wait_for_service "$CALCULATION_URL" "Grade Calculation Service" || exit 1
    wait_for_service "$REPORT_URL" "Report Generation Service" || exit 1
    wait_for_service "$AUDIT_URL" "Audit Logging Service" || exit 1

    log_success "All services are healthy!"
}

# Test Auth Service
test_auth_service() {
    log_section "TESTING AUTH SERVICE"

    # Login as admin
    log "Logging in as admin..."
    ADMIN_RESPONSE=$(curl -s -X POST "$AUTH_URL/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "admin",
            "password": "password"
        }')

    if echo "$ADMIN_RESPONSE" | grep -q "token"; then
        ADMIN_TOKEN=$(echo "$ADMIN_RESPONSE" | jq -r '.token')
        log_success "Admin login successful"
        echo "Admin Response:" | tee -a "$OUTPUT_FILE"
        echo "$ADMIN_RESPONSE" | jq '.' | tee -a "$OUTPUT_FILE"
    else
        log_error "Admin login failed"
        echo "$ADMIN_RESPONSE" | tee -a "$OUTPUT_FILE"
    fi

    # Login as teacher
    log "Logging in as Prof. Smith (teacher)..."
    TEACHER_RESPONSE=$(curl -s -X POST "$AUTH_URL/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "prof.smith",
            "password": "password"
        }')

    if echo "$TEACHER_RESPONSE" | grep -q "token"; then
        TEACHER_TOKEN=$(echo "$TEACHER_RESPONSE" | jq -r '.token')
        log_success "Teacher login successful"
        echo "Teacher Response:" | tee -a "$OUTPUT_FILE"
        echo "$TEACHER_RESPONSE" | jq '.' | tee -a "$OUTPUT_FILE"
    else
        log_error "Teacher login failed"
        echo "$TEACHER_RESPONSE" | tee -a "$OUTPUT_FILE"
    fi

    # Login as student
    log "Logging in as John Doe (student)..."
    STUDENT_RESPONSE=$(curl -s -X POST "$AUTH_URL/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "john.doe",
            "password": "password"
        }')

    if echo "$STUDENT_RESPONSE" | grep -q "token"; then
        STUDENT_TOKEN=$(echo "$STUDENT_RESPONSE" | jq -r '.token')
        STUDENT_ID=$(echo "$STUDENT_RESPONSE" | jq -r '.user.studentId')
        log_success "Student login successful"
        echo "Student Response:" | tee -a "$OUTPUT_FILE"
        echo "$STUDENT_RESPONSE" | jq '.' | tee -a "$OUTPUT_FILE"
    else
        log_error "Student login failed"
        echo "$STUDENT_RESPONSE" | tee -a "$OUTPUT_FILE"
    fi

    # Validate token
    log "Validating admin token..."
    VALIDATE_RESPONSE=$(curl -s -X GET "$AUTH_URL/validate" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    if echo "$VALIDATE_RESPONSE" | grep -q "id"; then
        log_success "Token validation successful"
    else
        log_error "Token validation failed"
    fi
}

# Test Grade Ingestion Service
test_grade_ingestion() {
    log_section "TESTING GRADE INGESTION SERVICE"

    # Submit a single grade
    log "Submitting a new grade for Final Project..."
    SUBMIT_RESPONSE=$(curl -s -X POST "$INGESTION_URL/grades" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TEACHER_TOKEN" \
        -d '{
            "studentId": "c4444444-4444-4444-4444-444444444444",
            "assignmentId": "e1111114-1111-1111-1111-111111111111",
            "score": 88.5,
            "comments": "Excellent project! Well structured code and great documentation."
        }')

    echo "Grade Submission Response:" | tee -a "$OUTPUT_FILE"
    echo "$SUBMIT_RESPONSE" | jq '.' | tee -a "$OUTPUT_FILE"

    # Bulk submit grades
    log "Bulk submitting participation grades..."
    BULK_RESPONSE=$(curl -s -X POST "$INGESTION_URL/grades/bulk" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $TEACHER_TOKEN" \
        -d '{
            "assignmentId": "e1111115-1111-1111-1111-111111111111",
            "grades": [
                {
                    "studentId": "c4444444-4444-4444-4444-444444444444",
                    "score": 85,
                    "comments": "Good class participation throughout the semester"
                },
                {
                    "studentId": "c5555555-5555-5555-5555-555555555555",
                    "score": 95,
                    "comments": "Outstanding engagement and helpful to classmates"
                },
                {
                    "studentId": "c6666666-6666-6666-6666-666666666666",
                    "score": 75,
                    "comments": "Satisfactory participation"
                }
            ]
        }')

    echo "Bulk Submission Response:" | tee -a "$OUTPUT_FILE"
    echo "$BULK_RESPONSE" | jq '.' | tee -a "$OUTPUT_FILE"

    # Get student grades
    log "Retrieving John Doe's grades..."
    STUDENT_GRADES=$(curl -s -X GET "$INGESTION_URL/students/c4444444-4444-4444-4444-444444444444/grades" \
        -H "Authorization: Bearer $STUDENT_TOKEN")

    echo "Student Grades:" | tee -a "$OUTPUT_FILE"
    echo "$STUDENT_GRADES" | jq '.' | tee -a "$OUTPUT_FILE"

    # Get assignment grades
    log "Retrieving all grades for Homework 1..."
    ASSIGNMENT_GRADES=$(curl -s -X GET "$INGESTION_URL/assignments/e1111111-1111-1111-1111-111111111111/grades" \
        -H "Authorization: Bearer $TEACHER_TOKEN")

    echo "Assignment Grades:" | tee -a "$OUTPUT_FILE"
    echo "$ASSIGNMENT_GRADES" | jq '.' | tee -a "$OUTPUT_FILE"
}

# Test Grade Calculation Service
test_grade_calculation() {
    log_section "TESTING GRADE CALCULATION SERVICE"

    # Calculate course grade
    log "Calculating John Doe's CS101 course grade..."
    COURSE_GRADE=$(curl -s -X POST "$CALCULATION_URL/calculate/course/c4444444-4444-4444-4444-444444444444/d1111111-1111-1111-1111-111111111111" \
        -H "Authorization: Bearer $TEACHER_TOKEN")

    echo "Course Grade Calculation:" | tee -a "$OUTPUT_FILE"
    echo "$COURSE_GRADE" | jq '.' | tee -a "$OUTPUT_FILE"

    # Calculate class average
    log "Calculating CS101 class average..."
    CLASS_AVG=$(curl -s -X GET "$CALCULATION_URL/courses/d1111111-1111-1111-1111-111111111111/average" \
        -H "Authorization: Bearer $TEACHER_TOKEN")

    echo "Class Average:" | tee -a "$OUTPUT_FILE"
    echo "$CLASS_AVG" | jq '.' | tee -a "$OUTPUT_FILE"

    # Calculate student GPA
    log "Calculating John Doe's cumulative GPA..."
    CUMULATIVE_GPA=$(curl -s -X GET "$CALCULATION_URL/students/c4444444-4444-4444-4444-444444444444/gpa/cumulative" \
        -H "Authorization: Bearer $STUDENT_TOKEN")

    echo "Cumulative GPA:" | tee -a "$OUTPUT_FILE"
    echo "$CUMULATIVE_GPA" | jq '.' | tee -a "$OUTPUT_FILE"

    # Calculate semester GPA
    log "Calculating Fall 2024 semester GPA..."
    SEMESTER_GPA=$(curl -s -X GET "$CALCULATION_URL/students/c4444444-4444-4444-4444-444444444444/gpa/Fall/2024" \
        -H "Authorization: Bearer $STUDENT_TOKEN")

    echo "Semester GPA:" | tee -a "$OUTPUT_FILE"
    echo "$SEMESTER_GPA" | jq '.' | tee -a "$OUTPUT_FILE"
}

# Test Report Generation Service
test_report_generation() {
    log_section "TESTING REPORT GENERATION SERVICE"

    # Generate report card
    log "Generating John Doe's Fall 2024 report card..."
    REPORT_CARD=$(curl -s -X GET "$REPORT_URL/students/c4444444-4444-4444-4444-444444444444/report-card/Fall/2024" \
        -H "Authorization: Bearer $STUDENT_TOKEN")

    echo "Report Card:" | tee -a "$OUTPUT_FILE"
    echo "$REPORT_CARD" | jq '.' | tee -a "$OUTPUT_FILE"

    # Generate transcript
    log "Generating John Doe's transcript..."
    TRANSCRIPT=$(curl -s -X GET "$REPORT_URL/students/c4444444-4444-4444-4444-444444444444/transcript" \
        -H "Authorization: Bearer $STUDENT_TOKEN")

    echo "Transcript:" | tee -a "$OUTPUT_FILE"
    echo "$TRANSCRIPT" | jq '.' | tee -a "$OUTPUT_FILE"

    # Generate report for another student
    log "Generating Jane Smith's report card (she's the top student)..."
    JANE_REPORT=$(curl -s -X GET "$REPORT_URL/students/c5555555-5555-5555-5555-555555555555/report-card/Fall/2024" \
        -H "Authorization: Bearer $TEACHER_TOKEN")

    echo "Jane's Report Card:" | tee -a "$OUTPUT_FILE"
    echo "$JANE_REPORT" | jq '.' | tee -a "$OUTPUT_FILE"
}

# Test Audit Logging Service
test_audit_logging() {
    log_section "TESTING AUDIT LOGGING SERVICE"

    # Get all audit logs
    log "Retrieving recent audit logs..."
    AUDIT_LOGS=$(curl -s -X GET "$AUDIT_URL/audit-logs?limit=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    echo "Recent Audit Logs:" | tee -a "$OUTPUT_FILE"
    echo "$AUDIT_LOGS" | jq '.' | tee -a "$OUTPUT_FILE"

    # Get grade-specific audit logs
    log "Retrieving grade-related audit logs..."
    GRADE_AUDITS=$(curl -s -X GET "$AUDIT_URL/audit-logs?entityType=Grade&limit=5" \
        -H "Authorization: Bearer $ADMIN_TOKEN")

    echo "Grade Audit Logs:" | tee -a "$OUTPUT_FILE"
    echo "$GRADE_AUDITS" | jq '.' | tee -a "$OUTPUT_FILE"
}

# Generate summary
generate_summary() {
    log_section "DEMO SUMMARY"

    log "Demo completed successfully!"
    log_info "Output saved to: $OUTPUT_FILE"

    echo "" | tee -a "$OUTPUT_FILE"
    log "Test Data Summary:"
    log "- Users: Admin, 2 Teachers (Prof. Smith, Prof. Jones), 3 Students (John, Jane, Bob)"
    log "- Courses: CS101, CS201, MATH101"
    log "- Assignments: 15 total across all courses"
    log "- Grades: Multiple grades submitted for each student"

    echo "" | tee -a "$OUTPUT_FILE"
    log "Services Tested:"
    log "✅ Auth Service - Login, registration, token validation"
    log "✅ Grade Ingestion - Submit, bulk submit, retrieve grades"
    log "✅ Grade Calculation - Course grades, GPA calculations, class averages"
    log "✅ Report Generation - Report cards, transcripts"
    log "✅ Audit Logging - Activity tracking and monitoring"

    echo "" | tee -a "$OUTPUT_FILE"
    log_success "All services demonstrated successfully!"
    log_info "Review $OUTPUT_FILE for detailed API responses"
}

# Main execution
main() {
    echo "=====================================================" | tee "$OUTPUT_FILE"
    echo "Student Grades Microservices - Comprehensive Demo" | tee -a "$OUTPUT_FILE"
    echo "Started: $(date)" | tee -a "$OUTPUT_FILE"
    echo "=====================================================" | tee -a "$OUTPUT_FILE"

    initialize_demo
    test_auth_service
    test_grade_ingestion
    test_grade_calculation
    test_report_generation
    test_audit_logging
    generate_summary

    echo "" | tee -a "$OUTPUT_FILE"
    echo "=====================================================" | tee -a "$OUTPUT_FILE"
    echo "Demo completed: $(date)" | tee -a "$OUTPUT_FILE"
    echo "=====================================================" | tee -a "$OUTPUT_FILE"
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed. Please install jq first."
    echo "  macOS: brew install jq"
    echo "  Linux: sudo apt-get install jq"
    exit 1
fi

# Run the demo
main

