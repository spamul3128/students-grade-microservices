# Demo Documentation for Student Grades Microservices

## Overview

This demo package provides comprehensive test data, Postman collections, and automated testing scripts for all microservices.

## 📦 What's Included

### 1. Test Data
- **File**: `database/demo-data.sql`
- **Content**:
  - 6 Users (1 admin, 2 teachers, 3 students)
  - 3 Courses (CS101, CS201, MATH101)
  - 15 Assignments across all courses
  - Multiple grades for each student

### 2. Postman Collections
Located in `postman/` directory:

| Collection | File | Description |
|------------|------|-------------|
| Auth Service | `Auth-Service.postman_collection.json` | Login, registration, token validation |
| Grade Ingestion | `Grade-Ingestion-Service.postman_collection.json` | Submit grades, bulk operations |
| Grade Calculation | `Grade-Calculation-Service.postman_collection.json` | GPA calculations, averages |
| Report Generation | `Report-Generation-Service.postman_collection.json` | Report cards, transcripts |
| Audit Logging | `Audit-Logging-Service.postman_collection.json` | Audit log retrieval |

### 3. Postman Environments

| Environment | File | Description |
|-------------|------|-------------|
| Local Development | `Local-Development.postman_environment.json` | For localhost testing |
| Docker Compose | `Docker-Compose.postman_environment.json` | For containerized services |

### 4. Automated Demo Script
- **File**: `demo.sh`
- Comprehensive automated testing of all services
- Generates detailed output file with all API responses

## 🚀 Quick Start Guide

### Step 1: Load Demo Data

```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices

# Make sure services are running
docker-compose up -d

# Wait for services to be ready (30 seconds)
sleep 30

# Load demo data
docker exec -i grades-postgres psql -U postgres -d grades_db < database/demo-data.sql
```

### Step 2: Import Postman Collections

1. Open Postman
2. Click **Import** button
3. Select all `.postman_collection.json` files from `postman/` directory
4. Import the environment file: `Local-Development.postman_environment.json`
5. Select "Local Development" environment from the dropdown

### Step 3: Run Automated Demo

```bash
./demo.sh
```

This will:
- Test all service health endpoints
- Authenticate as different user types
- Submit and retrieve grades
- Calculate GPAs and averages
- Generate reports
- Check audit logs
- Save detailed output to timestamped file

## 📊 Test Data Details

### Users & Credentials

All passwords are: `password123`

| Username | Role | Student ID | Teacher ID |
|----------|------|------------|------------|
| admin | Admin | - | - |
| prof.smith | Teacher | - | b2222222-...2222 |
| prof.jones | Teacher | - | b3333333-...3333 |
| john.doe | Student | c4444444-...4444 | - |
| jane.smith | Student | c5555555-...5555 | - |
| bob.johnson | Student | c6666666-...6666 | - |

### Students Profile

**John Doe** (Average Student)
- Major: Computer Science
- Enrolled Courses: CS101, CS201
- Average Grades: Mid 80s

**Jane Smith** (Top Student)
- Major: Computer Science
- Enrolled Courses: CS101, MATH101
- Average Grades: Low-Mid 90s

**Bob Johnson** (Below Average)
- Major: Data Science
- Enrolled Courses: CS101, MATH101
- Average Grades: Low-Mid 70s

### Courses

| Code | Name | Teacher | Credits | Assignments |
|------|------|---------|---------|-------------|
| CS101 | Intro to Programming | Prof. Smith | 3 | 5 |
| CS201 | Data Structures | Prof. Smith | 4 | 5 |
| MATH101 | Calculus I | Prof. Jones | 4 | 5 |

## 🧪 Manual Testing with Postman

### Workflow 1: Teacher Submitting Grades

1. **Auth Service** → Run "Login - Teacher (Prof Smith)"
   - Saves `teacher_token` to environment
   
2. **Grade Ingestion** → Run "Submit Single Grade"
   - Submits a grade for a student
   
3. **Grade Ingestion** → Run "Bulk Submit Grades"
   - Submits grades for multiple students
   
4. **Grade Calculation** → Run "Calculate Class Average"
   - Gets class performance metrics

### Workflow 2: Student Viewing Performance

1. **Auth Service** → Run "Login - Student (John Doe)"
   - Saves `student_token` to environment
   
2. **Grade Ingestion** → Run "Get Student Grades"
   - Views all personal grades
   
3. **Grade Calculation** → Run "Calculate Student Cumulative GPA"
   - Views overall GPA
   
4. **Report Generation** → Run "Generate Report Card"
   - Gets semester report card
   
5. **Report Generation** → Run "Generate Transcript"
   - Gets full academic transcript

### Workflow 3: Admin Monitoring

1. **Auth Service** → Run "Login - Admin"
   - Saves `auth_token` to environment
   
2. **Audit Logging** → Run "Get All Audit Logs"
   - Reviews system activity
   
3. **Audit Logging** → Run "Get Audit Logs by Entity Type"
   - Filters specific actions

## 📋 Demo Script Output

The `demo.sh` script generates a timestamped output file with:

- Service health check results
- Authentication responses with tokens
- Grade submission confirmations
- Calculation results (GPAs, averages)
- Report generation output
- Audit log entries
- Complete API response details (JSON formatted)

Example output file: `demo-output-20260610-193045.txt`

## 🔍 Testing Scenarios

### Scenario 1: End-of-Semester Grading

```bash
# 1. Teacher logs in
# 2. Bulk submits final exam grades
# 3. Calculates course grades for all students
# 4. Reviews class statistics
```

### Scenario 2: Student Progress Check

```bash
# 1. Student logs in
# 2. Views current grades
# 3. Checks GPA
# 4. Generates mid-semester report
```

### Scenario 3: Compliance Audit

```bash
# 1. Admin logs in
# 2. Retrieves audit logs for date range
# 3. Filters by grade modifications
# 4. Exports audit trail
```

## 🐛 Troubleshooting

### Services Not Responding

```bash
# Check service status
docker-compose ps

# View service logs
docker-compose logs -f [service-name]

# Restart services
docker-compose restart
```

### Database Connection Issues

```bash
# Check PostgreSQL
docker exec -it grades-postgres psql -U postgres -d grades_db -c "SELECT COUNT(*) FROM students;"

# Reload demo data
docker exec -i grades-postgres psql -U postgres -d grades_db < database/demo-data.sql
```

### Token Expiration

Tokens expire after 1 hour. Re-run the login requests in Postman to get fresh tokens.

## 📈 Expected Results

### Grade Ingestion
- Single grade submission: HTTP 200 with grade object
- Bulk submission: HTTP 200 with array of grades
- Invalid data: HTTP 400 with error message

### Grade Calculation
- Course grade: Letter grade + numeric grade + GPA
- Class average: Numeric average for the course
- Student GPA: 4.0 scale cumulative or semester GPA

### Report Generation
- Report card: Student info + course grades for semester
- Transcript: Complete academic history with all courses

### Audit Logging
- All grade operations logged
- User actions tracked with timestamps
- Filterable by entity type, user, date range

## 🎯 Success Criteria

✅ All health endpoints return "healthy"
✅ Authentication succeeds for all user types
✅ Grades can be submitted and retrieved
✅ GPA calculations return valid numbers (0.0-4.0)
✅ Reports contain student and grade information
✅ Audit logs capture system activities

## 📞 Support

For issues or questions:
1. Check service logs: `docker-compose logs [service-name]`
2. Review demo output file for API responses
3. Verify demo data loaded: Check database directly
4. Ensure all services are running: `docker-compose ps`

## 🔄 Reset Demo Environment

To start fresh:

```bash
# Stop all services
docker-compose down -v

# Restart services
docker-compose up -d

# Wait for startup
sleep 30

# Reload demo data
docker exec -i grades-postgres psql -U postgres -d grades_db < database/demo-data.sql

# Run demo again
./demo.sh
```

---

**Last Updated**: June 10, 2026
**Version**: 1.0

