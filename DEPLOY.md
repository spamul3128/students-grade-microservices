# Deployment Guide

Complete guide for deploying the Student Grades Microservices application.

## 📋 Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Deployment Options](#deployment-options)
- [Testing](#testing)
- [Database Management](#database-management)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software

1. **Java Development Kit (JDK) 11+**
   ```bash
   java -version
   ```

2. **Scala Build Tool (SBT)**
   ```bash
   sbt -version
   ```

3. **Docker & Docker Compose**
   ```bash
   docker --version
   docker-compose --version
   ```

4. **PostgreSQL Client (optional but recommended)**
   ```bash
   psql --version
   ```

### Installation Instructions

#### macOS
```bash
# Using Homebrew
brew install openjdk@11 sbt docker docker-compose postgresql
```

#### Linux (Ubuntu/Debian)
```bash
# Java
sudo apt-get update
sudo apt-get install openjdk-11-jdk

# SBT
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt

# Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# PostgreSQL client
sudo apt-get install postgresql-client
```

## Quick Start

### 1. Setup Development Environment

```bash
# Run the setup script
./scripts/dev-setup.sh
```

This will:
- ✅ Verify all required tools
- ✅ Start PostgreSQL and Redis
- ✅ Compile the Scala project
- ✅ Initialize the database

### 2. Deploy All Services

```bash
# Deploy with logs visible
./scripts/deploy-docker.sh

# Or deploy in background (recommended)
./scripts/deploy-docker.sh --detach
```

### 3. Verify Deployment

```bash
# Check health of all services
./scripts/health-check.sh

# View service status
docker-compose ps

# Check database records
./scripts/db-count.sh
```

### 4. Run Tests

```bash
# Run automated API tests
./scripts/test-api.sh
```

## Deployment Options

### Interactive Menu (Easiest)

```bash
./scripts/run.sh
```

Provides a menu-driven interface for all operations.

### Docker Deployment

#### Standard Deployment
```bash
./scripts/deploy-docker.sh
```

#### Detached Mode (Background)
```bash
./scripts/deploy-docker.sh --detach
```

#### Force Rebuild
```bash
./scripts/deploy-docker.sh --rebuild
```

#### Clean Deployment
```bash
# Removes all existing containers, volumes, and images
./scripts/deploy-docker.sh --clean
```

### Local Development (Without Docker)

```bash
# 1. Start infrastructure only
docker-compose up -d postgres redis

# 2. Run services locally in separate terminals
# Terminal 1
sbt "project authService" run

# Terminal 2
sbt "project gradeIngestion" run

# Terminal 3
sbt "project gradeCalculation" run

# Terminal 4
sbt "project reportGeneration" run

# Terminal 5
sbt "project auditLogging" run
```

## Testing

### Automated API Testing

```bash
./scripts/test-api.sh
```

Features:
- Tests all service endpoints
- Handles authentication
- Creates test data
- Verifies responses
- Provides detailed results

### Manual Testing

Use the HTTP file for manual testing:
```bash
# Open in IntelliJ or VS Code with REST Client extension
docs/API_TESTS.http
```

### Example curl Commands

```bash
# Health check
curl http://localhost:8080/health

# Register user
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@edu","password":"Test123!","role":"Student"}'

# Login
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"Test123!"}'

# Submit grade (requires JWT token)
curl -X POST http://localhost:8081/grades \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"studentId":"...","assignmentId":"...","score":95.5}'
```

## Database Management

### View Record Counts

```bash
./scripts/db-count.sh
```

Output:
```
==========================================
  Database Record Counts
==========================================
Table                     Count
------------------------------------------
students                  3
teachers                  2
courses                   2
assignments               3
grades                    5
course_grades             0
users                     2
audit_logs                0
------------------------------------------
TOTAL                     17
==========================================
```

### Clean Database

```bash
# Interactive menu
./scripts/db-cleanup.sh

# Clean all tables (keep schema)
./scripts/db-cleanup.sh --clean

# Drop and recreate database
./scripts/db-cleanup.sh --reset

# Clean and reload sample data
./scripts/db-cleanup.sh --reload
```

### Restart Database

```bash
# Restart with data preserved
./scripts/db-restart.sh

# Restart with clean data
./scripts/db-restart.sh --clean
```

### Direct Database Access

```bash
# Using Docker
docker-compose exec postgres psql -U postgres -d grades_db

# Or use the Makefile
make shell-postgres

# Using local psql client
PGPASSWORD=postgres psql -h localhost -U postgres -d grades_db
```

Common SQL queries:
```sql
-- View all students
SELECT * FROM students;

-- View grades for a student
SELECT s.first_name, s.last_name, a.name, g.score
FROM grades g
JOIN students s ON g.student_id = s.id
JOIN assignments a ON g.assignment_id = a.id;

-- View course averages
SELECT c.name, AVG(g.score) as average
FROM grades g
JOIN assignments a ON g.assignment_id = a.id
JOIN courses c ON a.course_id = c.id
GROUP BY c.name;

-- Count records
SELECT 
    (SELECT COUNT(*) FROM students) as students,
    (SELECT COUNT(*) FROM teachers) as teachers,
    (SELECT COUNT(*) FROM grades) as grades;
```

## Monitoring

### View Logs

```bash
# All services
./scripts/logs.sh

# Specific service
./scripts/logs.sh --service auth-service

# Last 50 lines (no follow)
./scripts/logs.sh --tail 50 --no-follow

# Service names:
# - postgres
# - redis
# - auth-service
# - grade-ingestion
# - grade-calculation
# - report-generation
# - audit-logging
```

### Health Checks

```bash
# Check all services
./scripts/health-check.sh

# Individual health checks
curl http://localhost:8080/health  # Auth
curl http://localhost:8081/health  # Ingestion
curl http://localhost:8082/health  # Calculation
curl http://localhost:8083/health  # Reports
curl http://localhost:8084/health  # Audit
```

### Service Status

```bash
# Docker Compose status
docker-compose ps

# Detailed container info
docker ps -a

# Resource usage
docker stats
```

## Stopping Services

```bash
# Stop all services (keep data)
./scripts/stop-all.sh

# Stop and remove volumes
./scripts/stop-all.sh --volumes

# Stop and remove images
./scripts/stop-all.sh --images

# Remove everything
./scripts/stop-all.sh --all
```

Or using Docker Compose directly:
```bash
# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Stop and remove images
docker-compose down --rmi local
```

## Troubleshooting

### Services Won't Start

```bash
# Check Docker is running
docker info

# Check for port conflicts
lsof -i :8080  # Auth
lsof -i :8081  # Ingestion
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis

# View container logs
./scripts/logs.sh --service auth-service

# Restart services
./scripts/stop-all.sh
./scripts/deploy-docker.sh --rebuild
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check PostgreSQL logs
./scripts/logs.sh --service postgres

# Restart database
./scripts/db-restart.sh

# Test connection
PGPASSWORD=postgres psql -h localhost -U postgres -d grades_db -c "SELECT 1"
```

### Build Failures

```bash
# Clean and rebuild
sbt clean compile

# Check Java version
java -version  # Should be 11+

# Check SBT version
sbt version

# Clear SBT cache
rm -rf ~/.sbt/boot
rm -rf target/
```

### Redis Connection Issues

```bash
# Check Redis is running
docker-compose ps redis

# Test Redis connection
docker-compose exec redis redis-cli ping

# Restart Redis
docker-compose restart redis
```

### Test Failures

```bash
# Check services are healthy
./scripts/health-check.sh

# View service logs
./scripts/logs.sh

# Reload sample data
./scripts/db-cleanup.sh --reload

# Run tests with more detail
./scripts/test-api.sh | tee test-results.log
```

### Port Already in Use

```bash
# Find process using port
lsof -ti:8080

# Kill process
kill -9 $(lsof -ti:8080)

# Or stop all services first
./scripts/stop-all.sh
```

## Service URLs

| Service | Port | Health Check | Description |
|---------|------|--------------|-------------|
| Auth Service | 8080 | http://localhost:8080/health | Authentication & authorization |
| Grade Ingestion | 8081 | http://localhost:8081/health | Grade submission and management |
| Grade Calculation | 8082 | http://localhost:8082/health | GPA and grade calculations |
| Report Generation | 8083 | http://localhost:8083/health | Report cards and transcripts |
| Audit Logging | 8084 | http://localhost:8084/health | Audit trail and logging |
| PostgreSQL | 5432 | - | Database |
| Redis | 6379 | - | Pub/sub messaging |

## Environment Configuration

Default configuration (development):

```yaml
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=grades_db
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Auth
JWT_SECRET=secret-key-change-in-production
```

For production, create a `.env` file:
```bash
DB_PASSWORD=secure_password_here
JWT_SECRET=secure_jwt_secret_here
```

## Production Deployment Checklist

- [ ] Change database password
- [ ] Change JWT secret
- [ ] Enable SSL/TLS
- [ ] Configure proper logging
- [ ] Set up backup strategy
- [ ] Configure monitoring (Prometheus, Grafana)
- [ ] Set resource limits
- [ ] Configure reverse proxy (nginx)
- [ ] Set up CI/CD pipeline
- [ ] Document runbook procedures

## Backup and Restore

### Backup Database
```bash
# Export database
docker-compose exec -T postgres pg_dump -U postgres grades_db > backup.sql

# Or with timestamp
docker-compose exec -T postgres pg_dump -U postgres grades_db > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restore Database
```bash
# Import database
cat backup.sql | docker-compose exec -T postgres psql -U postgres -d grades_db

# Or reset and import
./scripts/db-cleanup.sh --reset
cat backup.sql | docker-compose exec -T postgres psql -U postgres -d grades_db
```

## Support

For issues or questions:
1. Check logs: `./scripts/logs.sh`
2. Run health check: `./scripts/health-check.sh`
3. Review documentation in `docs/`
4. Check database status: `./scripts/db-count.sh`

## Additional Resources

- [Scripts README](scripts/README.md) - Detailed script documentation
- [API Tests](docs/API_TESTS.http) - Manual API testing
- [Architecture](docs/ARCHITECTURE.md) - System architecture
- [Project Structure](docs/PROJECT_STRUCTURE.md) - Code organization

