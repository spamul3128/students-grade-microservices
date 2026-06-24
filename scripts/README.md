# Scripts Directory

This directory contains automation scripts for deploying, testing, and managing the Student Grades Microservices application.

## 🚀 Quick Start

### Interactive Menu (Recommended)
```bash
./scripts/run.sh
```

This provides an interactive menu for all operations.

## 📋 Available Scripts

### Deployment & Setup

#### `dev-setup.sh`
Sets up the local development environment.
```bash
./scripts/dev-setup.sh
```

Features:
- Checks for required tools (Java, SBT, Docker, etc.)
- Starts PostgreSQL and Redis
- Compiles the Scala project
- Provides next steps guidance

#### `deploy-docker.sh`
Deploys all services as Docker containers.
```bash
# Interactive mode (logs in foreground)
./scripts/deploy-docker.sh

# Detached mode (background)
./scripts/deploy-docker.sh --detach

# Force rebuild images
./scripts/deploy-docker.sh --rebuild

# Clean and rebuild everything
./scripts/deploy-docker.sh --clean
```

Features:
- Builds Scala project
- Creates Docker images
- Starts all microservices
- Shows service URLs and status

#### `stop-all.sh`
Stops all running services.
```bash
# Stop services (keep data)
./scripts/stop-all.sh

# Stop and remove volumes
./scripts/stop-all.sh --volumes

# Stop and remove images
./scripts/stop-all.sh --images

# Remove everything
./scripts/stop-all.sh --all
```

### 🧪 Testing & Verification

#### `test-api.sh`
Automated API testing for all microservices.
```bash
./scripts/test-api.sh
```

Features:
- Tests all service endpoints
- Authenticates and gets JWT tokens
- Tests CRUD operations
- Provides detailed test results
- Color-coded pass/fail output

Example test output:
```
[TEST] Auth Service Health Check
[✓ PASS] Auth Service Health Check - Status: 200
[TEST] Register New Student
[✓ PASS] Register New Student - Status: 201
...
Total Tests: 15
Passed: 15
Failed: 0
```

#### `health-check.sh`
Checks the health status of all services.
```bash
./scripts/health-check.sh
```

Verifies:
- Docker containers are running
- PostgreSQL is healthy
- Redis is responding
- All microservices are responding to health checks

### 🗄️ Database Management

#### `db-count.sh`
Shows record counts in all database tables.
```bash
./scripts/db-count.sh
```

Output includes:
- Count per table
- Total records
- Recent activity statistics
- Average scores

Example output:
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
...
TOTAL                     20
==========================================
```

#### `db-cleanup.sh`
Manages database cleanup operations.
```bash
# Interactive menu
./scripts/db-cleanup.sh

# Clean all tables
./scripts/db-cleanup.sh --clean

# Drop and recreate database
./scripts/db-cleanup.sh --reset

# Clean and reload sample data
./scripts/db-cleanup.sh --reload
```

Options:
1. **Clean all tables**: Removes all data but keeps schema
2. **Reset database**: Drops and recreates database with schema
3. **Clean and reload**: Removes data and loads sample data
4. **Show record counts**: Displays current data status

#### `db-restart.sh`
Restarts PostgreSQL and Redis containers.
```bash
# Restart and preserve data
./scripts/db-restart.sh

# Restart with clean data
./scripts/db-restart.sh --clean
```

Features:
- Graceful restart with health checks
- Option to clean data volumes
- Verifies connectivity
- Shows record counts after restart

### 📊 Monitoring

#### `logs.sh`
Views service logs.
```bash
# All services (follow mode)
./scripts/logs.sh

# Specific service
./scripts/logs.sh --service auth-service

# Don't follow (static output)
./scripts/logs.sh --no-follow

# Last 50 lines
./scripts/logs.sh --tail 50

# Specific service with options
./scripts/logs.sh --service postgres --tail 100 --no-follow
```

Available services:
- `postgres` - PostgreSQL database
- `redis` - Redis cache
- `auth-service` - Authentication service
- `grade-ingestion` - Grade ingestion service
- `grade-calculation` - Grade calculation service
- `report-generation` - Report generation service
- `audit-logging` - Audit logging service

## 🔧 Common Workflows

### Initial Setup
```bash
# 1. Setup environment
./scripts/dev-setup.sh

# 2. Deploy services
./scripts/deploy-docker.sh --detach

# 3. Verify everything is working
./scripts/health-check.sh

# 4. Run tests
./scripts/test-api.sh
```

### Development Workflow
```bash
# Start infrastructure only (for local development)
docker-compose up -d postgres redis

# Check database records during development
./scripts/db-count.sh

# View logs while developing
./scripts/logs.sh --service auth-service

# Clean and reload test data
./scripts/db-cleanup.sh --reload
```

### Testing Workflow
```bash
# Deploy everything
./scripts/deploy-docker.sh --detach

# Wait for services to be ready
sleep 10

# Run automated tests
./scripts/test-api.sh

# Check specific service logs if tests fail
./scripts/logs.sh --service grade-ingestion

# Clean up after testing
./scripts/stop-all.sh
```

### Production Deployment
```bash
# Clean deployment
./scripts/deploy-docker.sh --clean --detach

# Verify health
./scripts/health-check.sh

# Monitor logs
./scripts/logs.sh

# Check database status
./scripts/db-count.sh
```

### Troubleshooting
```bash
# Check service health
./scripts/health-check.sh

# View all logs
./scripts/logs.sh

# Restart database
./scripts/db-restart.sh

# Reset everything
./scripts/stop-all.sh --all
./scripts/deploy-docker.sh --clean --detach
```

## 🌐 Service URLs

After deployment, services are available at:

| Service | Port | URL |
|---------|------|-----|
| Auth Service | 8080 | http://localhost:8080 |
| Grade Ingestion | 8081 | http://localhost:8081 |
| Grade Calculation | 8082 | http://localhost:8082 |
| Report Generation | 8083 | http://localhost:8083 |
| Audit Logging | 8084 | http://localhost:8084 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |

Health check endpoints: `http://localhost:{port}/health`

## 📝 Environment Variables

Database connection (default values):
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=grades_db
DB_USER=postgres
DB_PASSWORD=postgres
```

Redis connection:
```bash
REDIS_HOST=localhost
REDIS_PORT=6379
```

## 🛠️ Requirements

- **Java**: OpenJDK 11 or later
- **SBT**: Scala Build Tool
- **Docker**: For containerization
- **Docker Compose**: For orchestration
- **psql** (optional): PostgreSQL client for direct DB access
- **curl**: For API testing

## 📚 Additional Resources

- Main project README: `../README.md`
- API documentation: `../docs/API_TESTS.http`
- Architecture: `../docs/ARCHITECTURE.md`
- Database schema: `../database/schema.sql`

## 🤝 Contributing

When adding new scripts:
1. Add shebang: `#!/bin/bash`
2. Add description comment
3. Include help option: `--help`
4. Use consistent color coding
5. Add error handling: `set -e`
6. Make executable: `chmod +x script.sh`
7. Update this README

## 📄 License

All scripts are part of the Student Grades Microservices project.

## ⚠️ Notes

- Scripts assume default Docker Compose configuration
- Database credentials are for development only
- Change credentials before production use
- Scripts are designed for Unix-like systems (Linux, macOS)
- For Windows, use WSL2 or Git Bash

