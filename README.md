# Students Grade Microservices Suite

A robust, scalable microservices architecture for managing student grades, built with Scala 3 and the Typelevel stack.

## 🏗️ Architecture

This project implements a modular microservices architecture with the following services:

### Core Services

1. **Grade Ingestion Service** (Port 8081)
   - Accept individual or bulk assignment scores from teachers
   - Validate and store grade data
   - Publish grade submission events

2. **Grade Calculation Service** (Port 8082)
   - Calculate course grades, GPAs, and class averages
   - Apply weighted grading algorithms
   - Compute semester and cumulative GPAs

3. **Report Generation Service** (Port 8083)
   - Generate digital report cards
   - Create student transcripts
   - Export reports in various formats (JSON, PDF)

4. **Audit Logging Service** (Port 8084)
   - Track all grade modifications
   - Provide audit trail for compliance
   - Filter and query audit logs

5. **Auth Service** (Port 8080)
   - User authentication with JWT
   - Role-based access control (Students, Teachers, Admins)
   - Password hashing with BCrypt

### Cross-Cutting Concerns

- **Common Module**: Shared domain models, errors, and infrastructure
- **Database**: PostgreSQL with ACID compliance
- **Messaging**: Redis pub/sub for event-driven communication
- **Security**: JWT-based authentication and role-based authorization

## 🛠️ Tech Stack

- **Language**: Scala 3.3.1
- **Framework**: Typelevel Stack
  - Cats Effect 3 (functional effects)
  - Http4s (HTTP server/client)
  - FS2 (streaming)
  - Skunk (PostgreSQL client)
- **Database**: PostgreSQL 15
- **Messaging**: Redis 7
- **Authentication**: JWT with BCrypt password hashing
- **Testing**: MUnit with Cats Effect integration
- **Build**: SBT 1.9.7

## 📋 Prerequisites

- Java 11 or higher
- SBT 1.9+
- Docker and Docker Compose
- PostgreSQL 15 (or use Docker)
- Redis 7 (or use Docker)

## 🚀 Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL and Redis with Docker Compose
docker-compose up -d postgres redis

# Wait for services to be healthy
docker-compose ps
```

### 2. Build the Project

```bash
# Compile all modules
sbt compile

# Run tests
sbt test
```

### 3. Run Individual Services

```bash
# Terminal 1 - Auth Service
sbt "project authService" run

# Terminal 2 - Grade Ingestion Service
sbt "project gradeIngestion" run

# Terminal 3 - Grade Calculation Service
sbt "project gradeCalculation" run

# Terminal 4 - Report Generation Service
sbt "project reportGeneration" run

# Terminal 5 - Audit Logging Service
sbt "project auditLogging" run
```

### 4. Run All Services with Docker

```bash
# Build and start all services
docker-compose up --build

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## 📡 API Endpoints

### Auth Service (Port 8080)

```bash
# Register a new user
POST /register
Content-Type: application/json
{
  "username": "johndoe",
  "email": "john@university.edu",
  "password": "securepassword",
  "role": "Student"
}

# Login
POST /login
Content-Type: application/json
{
  "username": "johndoe",
  "password": "securepassword"
}

# Validate token
GET /validate
Authorization: Bearer <token>
```

### Grade Ingestion Service (Port 8081)

```bash
# Submit a grade
POST /grades
Authorization: Bearer <teacher-token>
Content-Type: application/json
{
  "studentId": "uuid",
  "assignmentId": "uuid",
  "score": 95.5,
  "comments": "Excellent work!"
}

# Bulk submit grades
POST /grades/bulk
Authorization: Bearer <teacher-token>
Content-Type: application/json
{
  "assignmentId": "uuid",
  "grades": [
    {
      "studentId": "uuid1",
      "score": 95,
      "comments": "Great!"
    },
    {
      "studentId": "uuid2",
      "score": 87,
      "comments": "Good job"
    }
  ]
}

# Update a grade
PUT /grades/{gradeId}
Authorization: Bearer <teacher-token>
Content-Type: application/json
{
  "score": 98.0
}

# Get student grades
GET /students/{studentId}/grades
Authorization: Bearer <token>
```

### Grade Calculation Service (Port 8082)

```bash
# Calculate course grade
POST /calculate/course/{studentId}/{courseId}
Authorization: Bearer <teacher-token>

# Get class average
GET /courses/{courseId}/average
Authorization: Bearer <teacher-token>

# Get cumulative GPA
GET /students/{studentId}/gpa/cumulative
Authorization: Bearer <token>

# Get semester GPA
GET /students/{studentId}/gpa/{semester}/{year}
Authorization: Bearer <token>
```

### Report Generation Service (Port 8083)

```bash
# Generate report card
GET /students/{studentId}/report-card/{semester}/{year}
Authorization: Bearer <token>

# Generate transcript
GET /students/{studentId}/transcript
Authorization: Bearer <token>

# Export report card as PDF
POST /students/{studentId}/report-card/{semester}/{year}/pdf
Authorization: Bearer <token>
```

### Audit Logging Service (Port 8084)

```bash
# Get audit logs (Admin only)
GET /audit-logs?entityType=Grade&userId=uuid&from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z
Authorization: Bearer <admin-token>
```

## 🧪 Testing

```bash
# Run all tests
sbt test

# Run tests for a specific module
sbt "project gradeIngestion" test

# Run tests with coverage
sbt coverage test coverageReport
```

## 📊 Database Schema

The database schema includes the following main tables:
- `students` - Student information
- `teachers` - Teacher information
- `courses` - Course definitions
- `assignments` - Assignment details with weights
- `grades` - Individual assignment grades
- `course_grades` - Final course grades and letter grades
- `users` - Authentication and authorization
- `audit_logs` - Audit trail for all operations

See `database/schema.sql` for the complete schema.

## 🔒 Security Features

- JWT-based authentication
- BCrypt password hashing
- Role-based access control (RBAC)
- Permission-based authorization
- Audit logging for all grade modifications
- Secure database queries with parameterized statements

## 🎯 Key Design Patterns

- **Microservices Architecture**: Each service is independently deployable
- **Event-Driven**: Services communicate via Redis pub/sub
- **Repository Pattern**: Clean separation of data access
- **Functional Effects**: Pure functional programming with Cats Effect
- **Type Safety**: Leveraging Scala 3's type system
- **Domain-Driven Design**: Clear domain models and bounded contexts

## 📈 Performance & Scalability

- Connection pooling for database and Redis
- Streaming support with FS2 for large data sets
- Stateless services for horizontal scaling
- Asynchronous processing with Cats Effect
- Event-driven architecture for loose coupling

## 🔧 Configuration

Services are configured via:
1. Environment variables (for Docker)
2. Configuration files (for local development)
3. Command-line arguments

Key configuration options:
- Database connection settings
- Redis connection settings
- JWT secret and expiration
- Server host and port

## 📝 Development Roadmap

- [ ] Implement repository layer for all services
- [ ] Add comprehensive integration tests
- [ ] Implement PDF generation for reports
- [ ] Add GraphQL API gateway
- [ ] Implement rate limiting
- [ ] Add metrics and monitoring (Prometheus)
- [ ] Add distributed tracing (Jaeger)
- [ ] Implement API documentation (OpenAPI/Swagger)
- [ ] Add CI/CD pipeline
- [ ] Kubernetes deployment manifests

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📚 Documentation

- **[DEPLOY.md](DEPLOY.md)** - Complete deployment guide with troubleshooting
- **[QUICKSTART.md](QUICKSTART.md)** - Quick reference for common commands
- **[scripts/README.md](scripts/README.md)** - Documentation for all automation scripts
- **[docs/API_TESTS.http](docs/API_TESTS.http)** - Manual API testing examples
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture details
- **[docs/PROJECT_STRUCTURE.md](docs/PROJECT_STRUCTURE.md)** - Code organization

## 📄 License

This project is licensed under the MIT License.

## 👥 Authors

Built as a demonstration of enterprise-grade Scala microservices architecture.

## 🙏 Acknowledgments

- Typelevel community for the amazing libraries
- Scala 3 language improvements
- The functional programming community

# students-grade-microservices
