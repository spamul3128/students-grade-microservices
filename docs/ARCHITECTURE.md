# Students Grade Microservices - Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Gateway (Future)                         │
│                      Load Balancer / Routing                         │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                ┌───────────────────┼───────────────────┐
                │                   │                   │
                ▼                   ▼                   ▼
    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │  Auth Service    │  │ Grade Ingestion  │  │ Grade Calculation│
    │  Port: 8080      │  │  Port: 8081      │  │  Port: 8082      │
    └──────────────────┘  └──────────────────┘  └──────────────────┘
                │                   │                   │
                │         ┌─────────┼─────────┐         │
                ▼         ▼                   ▼         ▼
    ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
    │ Report Generator │  │  Audit Logging   │  │   PostgreSQL DB  │
    │  Port: 8083      │  │  Port: 8084      │  │   Port: 5432     │
    └──────────────────┘  └──────────────────┘  └──────────────────┘
                │                   │                   │
                └───────────────────┼───────────────────┘
                                    ▼
                        ┌──────────────────────┐
                        │    Redis Pub/Sub     │
                        │     Port: 6379       │
                        └──────────────────────┘
```

## Service Responsibilities

### 1. Auth Service
**Purpose**: Centralized authentication and authorization

**Responsibilities**:
- User registration and login
- JWT token generation and validation
- Password hashing with BCrypt
- Role-based access control

**Technology**:
- Http4s for HTTP server
- JWT-Scala for token generation
- BCrypt for password hashing

**Database Tables**:
- `users`

---

### 2. Grade Ingestion Service
**Purpose**: Handle grade submissions from teachers

**Responsibilities**:
- Accept individual grade submissions
- Handle bulk grade uploads
- Validate scores against assignment max scores
- Publish grade submission events
- Update existing grades

**Technology**:
- Http4s for REST API
- Skunk for PostgreSQL queries
- Redis4Cats for event publishing

**Database Tables**:
- `grades`
- `assignments`
- `students`

**Events Published**:
- `grade.submitted`
- `grade.updated`
- `grades.bulk_uploaded`

---

### 3. Grade Calculation Service
**Purpose**: Compute grades, GPAs, and statistics

**Responsibilities**:
- Calculate weighted course grades
- Convert numeric grades to letter grades
- Calculate semester GPA
- Calculate cumulative GPA
- Compute class averages

**Technology**:
- Http4s for REST API
- Skunk for database queries
- FS2 for streaming calculations

**Database Tables**:
- `grades`
- `assignments`
- `course_grades`

**Events Published**:
- `course_grade.calculated`

**Events Subscribed**:
- `grade.submitted` (triggers recalculation)
- `grade.updated` (triggers recalculation)

---

### 4. Report Generation Service
**Purpose**: Generate student reports and transcripts

**Responsibilities**:
- Generate digital report cards
- Create comprehensive transcripts
- Export reports in various formats (JSON, PDF)
- Aggregate data from multiple sources

**Technology**:
- Http4s for REST API
- Skunk for database queries
- PDF generation library (future)

**Database Tables**:
- `students`
- `course_grades`
- `courses`
- `teachers`

**Events Published**:
- `report_card.generated`

---

### 5. Audit Logging Service
**Purpose**: Security and compliance tracking

**Responsibilities**:
- Log all grade modifications
- Track user actions
- Provide audit trail queries
- Store IP addresses and timestamps

**Technology**:
- Http4s for REST API
- Skunk for PostgreSQL
- Structured logging

**Database Tables**:
- `audit_logs`

**Events Subscribed**:
- All domain events (for audit trail)

---

## Data Flow Examples

### Example 1: Teacher Submits Grade

```
1. Teacher → Auth Service
   POST /login
   Returns: JWT token

2. Teacher → Grade Ingestion Service
   POST /grades
   Authorization: Bearer {token}
   
3. Grade Ingestion Service:
   - Validates token with Auth Service
   - Validates score against assignment
   - Saves grade to database
   - Publishes "grade.submitted" event to Redis

4. Grade Calculation Service (listening to events):
   - Receives "grade.submitted" event
   - Recalculates course grade
   - Saves course grade
   - Publishes "course_grade.calculated" event

5. Audit Logging Service (listening to events):
   - Receives "grade.submitted" event
   - Logs the action to audit_logs table
```

### Example 2: Student Views Report Card

```
1. Student → Auth Service
   POST /login
   Returns: JWT token

2. Student → Report Generation Service
   GET /students/{id}/report-card/Fall/2024
   Authorization: Bearer {token}

3. Report Generation Service:
   - Validates token
   - Checks student authorization
   - Queries course_grades table
   - Queries courses and teachers tables
   - Aggregates data
   - Returns JSON report card

4. Audit Logging Service:
   - Logs "report card viewed" action
```

### Example 3: Admin Views Audit Logs

```
1. Admin → Auth Service
   POST /login (with admin credentials)
   Returns: JWT token with Admin role

2. Admin → Audit Logging Service
   GET /audit-logs?entityType=Grade&from=2024-01-01
   Authorization: Bearer {token}

3. Audit Logging Service:
   - Validates token
   - Checks for Admin role
   - Queries audit_logs table
   - Returns filtered results
```

---

## Event-Driven Architecture

### Events Flow

```
Grade Ingestion Service
    │
    ├─▶ grade.submitted ─────────┐
    │                             │
    └─▶ grade.updated ────────────┤
                                  │
                                  ▼
                          ┌──────────────┐
                          │ Redis Pub/Sub│
                          └──────────────┘
                                  │
                ┌─────────────────┼─────────────────┐
                ▼                 ▼                 ▼
    ┌──────────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Grade Calculation│ │Audit Logging │ │ Report Gen   │
    │    (Subscriber)  │ │ (Subscriber) │ │ (Subscriber) │
    └──────────────────┘ └──────────────┘ └──────────────┘
            │
            └─▶ course_grade.calculated
```

### Event Types

1. **grade.submitted**
   - When: New grade is entered
   - Data: gradeId, studentId, assignmentId, courseId, score, timestamp
   - Subscribers: Calculation Service, Audit Service

2. **grade.updated**
   - When: Existing grade is modified
   - Data: gradeId, oldScore, newScore, updatedBy, timestamp
   - Subscribers: Calculation Service, Audit Service

3. **course_grade.calculated**
   - When: Final course grade is computed
   - Data: studentId, courseId, letterGrade, numericGrade, timestamp
   - Subscribers: Report Service, Audit Service

4. **report_card.generated**
   - When: Report card is created
   - Data: studentId, semester, year, timestamp
   - Subscribers: Audit Service

5. **grades.bulk_uploaded**
   - When: Teacher uploads multiple grades
   - Data: uploadId, teacherId, courseId, numberOfGrades, timestamp
   - Subscribers: Calculation Service, Audit Service

---

## Security Model

### Authentication Flow

```
1. User submits credentials
   POST /login { username, password }

2. Auth Service:
   - Looks up user in database
   - Verifies password with BCrypt
   - Generates JWT token
   - Returns token + user info

3. Client includes token in subsequent requests
   Authorization: Bearer {token}

4. Each service validates token:
   - Decodes JWT
   - Verifies signature
   - Checks expiration
   - Extracts user roles
```

### Role-Based Access Control

```scala
Role: Student
  Permissions:
    - ViewOwnGrades
  
  Can Access:
    - GET /students/{own-id}/grades
    - GET /students/{own-id}/report-card
    - GET /students/{own-id}/transcript
    - GET /students/{own-id}/gpa/*

Role: Teacher
  Permissions:
    - ViewAllGrades
    - SubmitGrades
    - ModifyGrades
    - GenerateReports
  
  Can Access:
    - POST /grades
    - POST /grades/bulk
    - PUT /grades/{id}
    - GET /assignments/{id}/grades
    - POST /calculate/course/{studentId}/{courseId}
    - GET /courses/{id}/average

Role: Admin
  Permissions:
    - All Teacher permissions +
    - ViewAuditLogs
    - ManageUsers
  
  Can Access:
    - All endpoints
    - GET /audit-logs
```

---

## Database Schema Design

### Entity Relationship Diagram

```
┌─────────────┐         ┌─────────────┐
│  students   │         │  teachers   │
├─────────────┤         ├─────────────┤
│ id (PK)     │         │ id (PK)     │
│ first_name  │         │ first_name  │
│ last_name   │         │ last_name   │
│ email       │         │ email       │
└─────┬───────┘         └──────┬──────┘
      │                        │
      │                        │ teaches
      │                        │
      │                 ┌──────▼──────┐
      │                 │   courses   │
      │                 ├─────────────┤
      │                 │ id (PK)     │
      │  enrolled_in    │ name        │
      │                 │ code        │
      │                 │ teacher_id  │
      │                 └──────┬──────┘
      │                        │
      │                        │ has
      │                        │
      │                 ┌──────▼──────────┐
      │                 │  assignments    │
      │                 ├─────────────────┤
      │                 │ id (PK)         │
      │                 │ course_id (FK)  │
      │                 │ max_score       │
      │                 │ weight          │
      │                 └──────┬──────────┘
      │                        │
      │     submitted_for      │
      │                        │
      │                 ┌──────▼──────────┐
      └─────────────────│    grades       │
                        ├─────────────────┤
                        │ id (PK)         │
                        │ student_id (FK) │
                        │ assignment_id   │
                        │ score           │
                        │ graded_by       │
                        └─────────────────┘
```

---

## Scalability Considerations

### Horizontal Scaling
- All services are stateless
- Can run multiple instances behind load balancer
- Database connection pooling prevents bottlenecks
- Redis pub/sub supports multiple subscribers

### Performance Optimizations
- Database indexes on foreign keys and common queries
- Connection pooling (Skunk session pools)
- Streaming large result sets with FS2
- Asynchronous processing with Cats Effect
- Event-driven reduces synchronous dependencies

### Future Enhancements
- Implement API Gateway for unified entry point
- Add caching layer (Redis as cache)
- Implement database read replicas
- Add rate limiting per user/role
- Implement circuit breakers for resilience
- Add distributed tracing (Jaeger)
- Implement metrics collection (Prometheus)
- Add health checks and readiness probes

---

## Technology Stack Rationale

### Why Scala 3?
- Advanced type safety
- Improved syntax (enums, union types)
- Better inference
- Excellent for domain modeling

### Why Typelevel Stack?
- Cats Effect: Pure functional effects, excellent for async
- Http4s: Type-safe HTTP server/client
- FS2: Powerful streaming capabilities
- Skunk: Type-safe PostgreSQL client
- Strong ecosystem and community

### Why PostgreSQL?
- ACID compliance for critical grade data
- Strong relational model for complex queries
- Excellent performance
- JSON support for flexibility

### Why Redis?
- Fast, in-memory pub/sub
- Simple to set up and use
- Reliable message delivery
- Low latency

---

## Testing Strategy

### Unit Tests
- Test pure business logic
- Test domain model conversions
- Test validation rules

### Integration Tests
- Test database repositories
- Test HTTP endpoints
- Test event publishing/subscribing

### End-to-End Tests
- Test complete workflows
- Test authentication flow
- Test service-to-service communication

### Test Containers
- Use Testcontainers for real PostgreSQL and Redis
- Ensure tests are reproducible
- Test against production-like environment

---

## Deployment Strategy

### Local Development
```bash
# Start infrastructure
docker-compose up -d postgres redis

# Run services individually  
sbt "project gradeIngestion" run
```

### Docker Deployment
```bash
# Build and run all services
docker-compose up --build
```

### Kubernetes (Future)
- Deploy each service as a separate pod
- Use ConfigMaps for configuration
- Use Secrets for sensitive data
- Implement horizontal pod autoscaling
- Use readiness and liveness probes

---

## Monitoring & Observability

### Logging
- Structured logging with Logback
- Configurable log levels
- Request/response logging

### Metrics (Future)
- Prometheus for metrics collection
- Grafana for visualization
- Track: request rates, error rates, latency

### Tracing (Future)
- Jaeger for distributed tracing
- Track requests across services
- Identify bottlenecks

### Alerts (Future)
- Alert on high error rates
- Alert on high latency
- Alert on service downtime

