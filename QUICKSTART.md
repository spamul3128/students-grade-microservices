# Quick Start Guide

> 🎯 **NEW:** We now have automated scripts! See [Automated Setup](#automated-setup-recommended) below for the easiest way to get started.

## Automated Setup (Recommended)

### 🚀 One-Command Quick Start

```bash
# Interactive menu for all operations (EASIEST)
./scripts/run.sh

# Or automated CLI commands
./scripts/dev-setup.sh                # Setup environment
./scripts/deploy-docker.sh --detach   # Deploy all services
./scripts/test-api.sh                 # Run automated tests
./scripts/health-check.sh             # Verify everything works
```

### 📋 Available Scripts

All scripts are in the `./scripts/` directory:

**Deployment:**
- `deploy-docker.sh` - Deploy all services with Docker
- `stop-all.sh` - Stop all running services
- `dev-setup.sh` - Setup development environment

**Testing:**
- `test-api.sh` - Automated API integration tests
- `health-check.sh` - Check health of all services

**Database:**
- `db-count.sh` - Show database record counts
- `db-cleanup.sh` - Clean/reset database
- `db-restart.sh` - Restart database and Redis

**Monitoring:**
- `logs.sh` - View service logs
- `run.sh` - Interactive menu for all operations

### 🔧 Quick Examples

```bash
# Deploy everything
./scripts/deploy-docker.sh --detach

# Check if services are running
./scripts/health-check.sh

# Run automated tests
./scripts/test-api.sh

# View logs
./scripts/logs.sh --service auth-service

# Count database records
./scripts/db-count.sh

# Clean and reload sample data
./scripts/db-cleanup.sh --reload
```

📚 **Full Documentation:**
- [DEPLOY.md](DEPLOY.md) - Complete deployment guide
- [scripts/README.md](scripts/README.md) - All script documentation
- See below for manual setup instructions

---

## Prerequisites Check

Before you begin, ensure you have:
- ✅ Java 11 or higher installed
- ✅ SBT 1.9+ installed
- ✅ Docker installed and running
- ✅ Docker Compose installed

Check versions:
```bash
java -version
sbt --version
docker --version
docker-compose --version
```

## Option 1: Local Development Setup (Recommended for Development)

### Step 1: Start Infrastructure Only

Start PostgreSQL and Redis using Docker:

```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices

# Start database and Redis
docker-compose up -d postgres redis

# Wait for services to be ready (about 10 seconds)
docker-compose ps
```

You should see both services as "healthy".

### Step 2: Verify Database

```bash
# Connect to PostgreSQL to verify schema
docker-compose exec postgres psql -U postgres -d grades_db -c "\dt"

# You should see tables: students, teachers, courses, assignments, grades, etc.
```

### Step 3: Compile the Project

```bash
# Compile all modules
sbt compile

# This will download dependencies and compile all services
# First run may take 5-10 minutes
```

### Step 4: Run Tests

```bash
# Run all tests
sbt test
```

### Step 5: Run Services Individually

Open **5 terminal windows** and run each service:

**Terminal 1 - Auth Service (Port 8080):**
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
sbt "project authService" run
```

**Terminal 2 - Grade Ingestion (Port 8081):**
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
sbt "project gradeIngestion" run
```

**Terminal 3 - Grade Calculation (Port 8082):**
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
sbt "project gradeCalculation" run
```

**Terminal 4 - Report Generation (Port 8083):**
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
sbt "project reportGeneration" run
```

**Terminal 5 - Audit Logging (Port 8084):**
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
sbt "project auditLogging" run
```

### Step 6: Test the API

Use the HTTP test file or curl:

```bash
# Health check
curl http://localhost:8080/health

# Register a user
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@university.edu",
    "password": "password123",
    "role": "Student"
  }'

# Login
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

---

## Option 2: Full Docker Setup (Production-like)

**Note:** Docker builds require Dockerfiles which need to be created. This option will be available once Dockerfiles are added.

### Step 1: Build and Start All Services

```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices

# Build and start everything
docker-compose up --build
```

### Step 2: View Logs

```bash
# View all logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f grade-ingestion
```

### Step 3: Check Service Status

```bash
docker-compose ps
```

All services should show as "Up" and healthy.

---

## Using the Makefile (Simplified Commands)

The project includes a Makefile for common tasks:

```bash
# Show all available commands
make help

# Start infrastructure (PostgreSQL + Redis)
make db-up

# Stop infrastructure
make db-down

# Compile project
make build

# Run tests
make test

# Clean build artifacts
make clean

# Format code
make format

# Build Docker images
make docker-build

# Run with Docker
make run-docker

# View logs
make logs

# Check service status
make ps

# Open PostgreSQL shell
make shell-postgres

# Open Redis CLI
make shell-redis
```

---

## Testing the Complete Workflow

### 1. Register and Login

```bash
# Register a teacher
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher1",
    "email": "teacher@university.edu",
    "password": "teacher123",
    "role": "Teacher"
  }'

# Login and save the token
TOKEN=$(curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "teacher1",
    "password": "teacher123"
  }' | jq -r '.token')

echo "Token: $TOKEN"
```

### 2. Submit a Grade

```bash
# Submit a grade (replace UUIDs with actual values from database)
curl -X POST http://localhost:8081/grades \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "550e8400-e29b-41d4-a716-446655440001",
    "assignmentId": "880e8400-e29b-41d4-a716-446655440001",
    "score": 95.5,
    "comments": "Excellent work!"
  }'
```

### 3. Calculate Course Grade

```bash
curl -X POST "http://localhost:8082/calculate/course/550e8400-e29b-41d4-a716-446655440001/770e8400-e29b-41d4-a716-446655440001" \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Generate Report Card

```bash
curl "http://localhost:8083/students/550e8400-e29b-41d4-a716-446655440001/report-card/Fall/2024" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Troubleshooting

### Issue: "Port already in use"

**Solution:** Stop conflicting services or change ports in docker-compose.yml

```bash
# Check what's using the port
lsof -i :8080
lsof -i :5432

# Kill the process
kill -9 <PID>
```

### Issue: "Database connection failed"

**Solution:** Ensure PostgreSQL is running and healthy

```bash
# Check PostgreSQL status
docker-compose ps postgres

# Restart PostgreSQL
docker-compose restart postgres

# Check logs
docker-compose logs postgres
```

### Issue: "SBT compilation errors"

**Solution:** Clean and rebuild

```bash
sbt clean
sbt compile
```

### Issue: "Tests failing"

**Solution:** Ensure test database is clean

```bash
# Restart database to reset state
docker-compose down postgres
docker-compose up -d postgres
sleep 10
sbt test
```

### Issue: "Redis connection failed"

**Solution:** Verify Redis is running

```bash
# Check Redis status
docker-compose ps redis

# Test Redis connection
docker-compose exec redis redis-cli ping
# Should return: PONG
```

---

## Development Workflow

### 1. Make Changes

Edit Scala files in your IDE (IntelliJ IDEA recommended).

### 2. Compile

```bash
sbt compile
```

### 3. Run Tests

```bash
sbt test
```

### 4. Format Code

```bash
sbt scalafmt
```

### 5. Restart Service

Stop the service (Ctrl+C) and run again:
```bash
sbt "project gradeIngestion" run
```

---

## IDE Setup

### IntelliJ IDEA

1. Open the project root folder
2. IntelliJ will detect SBT project automatically
3. Wait for indexing to complete
4. Install Scala plugin if prompted
5. Right-click on a Server file → Run

### VS Code

1. Install "Metals" extension
2. Open project folder
3. Metals will import the project
4. Use the provided tasks to run services

---

## Next Steps

1. ✅ **Explore the API** - Use the HTTP test file in `docs/API_TESTS.http`
2. ✅ **Read Architecture** - Check `docs/ARCHITECTURE.md`
3. ✅ **Study Code** - Start with `modules/common/src/main/scala`
4. ✅ **Write Tests** - Add tests in `src/test/scala`
5. ✅ **Implement Features** - Add new endpoints or services
6. ✅ **Contribute** - Follow the contribution guidelines

---

## Useful Commands Reference

```bash
# SBT Commands
sbt compile                          # Compile all modules
sbt test                            # Run all tests
sbt "project gradeIngestion" run    # Run specific service
sbt clean                           # Clean build artifacts
sbt assembly                        # Create fat JARs

# Docker Commands
docker-compose up -d                # Start in background
docker-compose down                 # Stop all services
docker-compose logs -f SERVICE      # View logs
docker-compose restart SERVICE      # Restart service
docker-compose exec SERVICE bash    # Shell into service

# Database Commands
psql -U postgres -d grades_db       # Connect to PostgreSQL
\dt                                 # List tables
\d table_name                       # Describe table
SELECT * FROM students;             # Query data

# Redis Commands
redis-cli                           # Connect to Redis
PING                               # Test connection
KEYS *                             # List all keys
SUBSCRIBE grades.submitted         # Subscribe to channel
```

---

## Support

- **Issues**: Check the README.md for known issues
- **Documentation**: Read docs/ARCHITECTURE.md for system design
- **Code**: Explore modules/common for shared code
- **Tests**: Check test files for usage examples

---

## Summary of Ports

| Service            | Port | URL                     |
|-------------------|------|-------------------------|
| Auth Service      | 8080 | http://localhost:8080   |
| Grade Ingestion   | 8081 | http://localhost:8081   |
| Grade Calculation | 8082 | http://localhost:8082   |
| Report Generation | 8083 | http://localhost:8083   |
| Audit Logging     | 8084 | http://localhost:8084   |
| PostgreSQL        | 5432 | localhost:5432          |
| Redis             | 6379 | localhost:6379          |

---

Happy coding! 🚀

