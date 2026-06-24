# Docker Deployment Guide

## Overview

All microservices now have Dockerfiles and can be built and deployed using Docker and Docker Compose.

## Created Files

### Server Files (Entry Points)
- `modules/auth-service/src/main/scala/com/education/grades/auth/AuthServer.scala` - Port 8080
- `modules/grade-ingestion/src/main/scala/com/education/grades/ingestion/GradeIngestionServer.scala` - Port 8081
- `modules/grade-calculation/src/main/scala/com/education/grades/calculation/GradeCalculationServer.scala` - Port 8082
- `modules/report-generation/src/main/scala/com/education/grades/reports/ReportGenerationServer.scala` - Port 8083
- `modules/audit-logging/src/main/scala/com/education/grades/audit/AuditLoggingServer.scala` - Port 8084

### Dockerfiles
- `modules/auth-service/Dockerfile`
- `modules/grade-ingestion/Dockerfile`
- `modules/grade-calculation/Dockerfile`
- `modules/report-generation/Dockerfile`
- `modules/audit-logging/Dockerfile`

## Docker Build Strategy

Each Dockerfile uses a **multi-stage build**:

1. **Builder Stage**: Uses `sbtscala/scala-sbt` image to compile and stage the application
2. **Runtime Stage**: Uses lightweight `eclipse-temurin:21-jre-jammy` image with only the runtime artifacts

### Benefits
- ✅ Smaller final image size (JRE only, no SBT or build tools)
- ✅ Faster deployments
- ✅ Better security (fewer components in runtime image)

## Quick Start

### 1. Build and Start All Services

```bash
# Start all services with dependencies (PostgreSQL, Redis)
docker-compose up --build

# Or run in detached mode
docker-compose up --build -d
```

### 2. Build Individual Service

```bash
# Build specific service
docker-compose build auth-service

# Build without cache
docker-compose build --no-cache auth-service
```

### 3. Check Service Health

```bash
# View logs
docker-compose logs -f auth-service

# Check all services
docker-compose ps

# Test health endpoint
curl http://localhost:8080/health  # Auth Service
curl http://localhost:8081/health  # Grade Ingestion
curl http://localhost:8082/health  # Grade Calculation
curl http://localhost:8083/health  # Report Generation
curl http://localhost:8084/health  # Audit Logging
```

### 4. Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## Service Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Docker Network                       │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────┐    ┌─────────────┐    ┌────────────┐  │
│  │  PostgreSQL │    │    Redis    │    │ Services:  │  │
│  │  Port: 5432 │    │ Port: 6379  │    │            │  │
│  └─────────────┘    └─────────────┘    │ • Auth     │  │
│         │                   │           │ • Ingestion│  │
│         └───────────────────┴───────────│ • Calc     │  │
│                                         │ • Report   │  │
│                                         │ • Audit    │  │
│                                         └────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Environment Variables

Each service supports the following environment variables (configured in docker-compose.yml):

```bash
# Database
DB_HOST=postgres
DB_PORT=5432
DB_NAME=grades_db
DB_USER=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT Authentication
JWT_SECRET=secret-key-change-in-production
```

## Port Mappings

| Service            | Container Port | Host Port |
|--------------------|----------------|-----------|
| Auth Service       | 8080           | 8080      |
| Grade Ingestion    | 8081           | 8081      |
| Grade Calculation  | 8082           | 8082      |
| Report Generation  | 8083           | 8083      |
| Audit Logging      | 8084           | 8084      |
| PostgreSQL         | 5432           | 5432      |
| Redis              | 6379           | 6379      |

## Troubleshooting

### Service won't start

```bash
# Check logs
docker-compose logs service-name

# Restart specific service
docker-compose restart service-name

# Rebuild service
docker-compose up --build service-name
```

### Database connection issues

```bash
# Verify PostgreSQL is healthy
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Connect to database directly
docker exec -it grades-postgres psql -U postgres -d grades_db
```

### Build failures

```bash
# Clean build cache
docker-compose build --no-cache service-name

# Remove old images
docker system prune -a

# Check disk space
docker system df
```

## Development Workflow

### Local Development
```bash
# Run services locally (without Docker)
sbt "authService/run"
sbt "gradeIngestion/run"
# etc.
```

### Docker Development
```bash
# Rebuild after code changes
docker-compose up --build

# Watch logs
docker-compose logs -f
```

## Production Considerations

For production deployment:

1. **Change JWT_SECRET** to a strong, unique value
2. **Use environment-specific configs** (don't hardcode passwords)
3. **Enable TLS/HTTPS**
4. **Set up proper logging and monitoring**
5. **Configure database backups**
6. **Use Docker secrets** for sensitive data
7. **Set resource limits** in docker-compose.yml:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '1.0'
         memory: 1G
       reservations:
         cpus: '0.5'
         memory: 512M
   ```

## Monitoring

### Health Checks

All services expose a `/health` endpoint:

```bash
curl http://localhost:8080/health
# Response: "Auth Service is healthy"
```

### Container Health

```bash
# Check container health status
docker-compose ps

# Inspect specific container
docker inspect grades-postgres --format='{{json .State.Health}}'
```

## Database Initialization

The PostgreSQL container automatically runs:
1. `database/schema.sql` - Creates tables and schema
2. `database/sample-data.sql` - Inserts sample data

These run on first container startup only.

## Next Steps

1. ✅ All Dockerfiles created
2. ✅ Server entry points created for all services
3. ✅ build.sbt updated with JavaAppPackaging
4. ⚠️  **TODO**: Implement placeholder repository and service logic
5. ⚠️  **TODO**: Set up proper authentication middleware
6. ⚠️  **TODO**: Add comprehensive error handling
7. ⚠️  **TODO**: Configure production-ready logging

## Testing the Deployment

```bash
# 1. Start everything
docker-compose up -d

# 2. Wait for services to be healthy (30-60 seconds)
sleep 30

# 3. Test each service
for port in 8080 8081 8082 8083 8084; do
  echo "Testing port $port..."
  curl -s http://localhost:$port/health
  echo ""
done

# 4. Check all containers are running
docker-compose ps
```

Expected output: All services should return their health status and be in "Up" state.

