# Scripts Summary - Command Reference Card

Quick reference for all automation scripts in the Students Grade Microservices project.

## 🎯 Most Used Commands

```bash
./scripts/run.sh              # Interactive menu (EASIEST)
./scripts/deploy-docker.sh -d # Deploy everything
./scripts/test-api.sh         # Run all tests
./scripts/health-check.sh     # Check status
./scripts/db-count.sh         # Count DB records
./scripts/logs.sh             # View logs
```

## 📦 Deployment Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `run.sh` | Interactive menu | `./scripts/run.sh` |
| `dev-setup.sh` | Setup dev environment | `./scripts/dev-setup.sh` |
| `deploy-docker.sh` | Deploy all services | `./scripts/deploy-docker.sh [--detach\|--rebuild\|--clean]` |
| `stop-all.sh` | Stop services | `./scripts/stop-all.sh [--volumes\|--images\|--all]` |

### Examples:
```bash
# Deploy in background
./scripts/deploy-docker.sh --detach

# Force rebuild and deploy
./scripts/deploy-docker.sh --rebuild

# Clean everything and redeploy
./scripts/deploy-docker.sh --clean

# Stop and remove volumes
./scripts/stop-all.sh --volumes
```

## 🧪 Testing Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `test-api.sh` | Automated API tests | `./scripts/test-api.sh` |
| `health-check.sh` | Health check all services | `./scripts/health-check.sh` |

### Examples:
```bash
# Run full test suite
./scripts/test-api.sh

# Check if everything is healthy
./scripts/health-check.sh
```

## 🗄️ Database Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `db-count.sh` | Show record counts | `./scripts/db-count.sh` |
| `db-cleanup.sh` | Clean/reset database | `./scripts/db-cleanup.sh [--clean\|--reset\|--reload]` |
| `db-restart.sh` | Restart DB & Redis | `./scripts/db-restart.sh [--clean]` |

### Examples:
```bash
# Show all record counts
./scripts/db-count.sh

# Clean all tables
./scripts/db-cleanup.sh --clean

# Drop and recreate DB
./scripts/db-cleanup.sh --reset

# Clean and reload sample data
./scripts/db-cleanup.sh --reload

# Restart preserving data
./scripts/db-restart.sh

# Restart with clean data
./scripts/db-restart.sh --clean
```

## 📊 Monitoring Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `logs.sh` | View service logs | `./scripts/logs.sh [--service NAME] [--tail N] [--no-follow]` |
| `health-check.sh` | Check service health | `./scripts/health-check.sh` |

### Examples:
```bash
# All logs (follow mode)
./scripts/logs.sh

# Specific service
./scripts/logs.sh --service auth-service

# Last 50 lines, no follow
./scripts/logs.sh --tail 50 --no-follow

# PostgreSQL logs
./scripts/logs.sh --service postgres
```

## 🔧 Make Shortcuts

All scripts have Makefile shortcuts:

```bash
make menu          # Interactive menu (./scripts/run.sh)
make setup         # Setup environment
make deploy        # Deploy all services
make test-api      # Run API tests
make health        # Health check
make db-count      # Show DB counts
make db-clean      # Clean database
make db-reset      # Reset database
make logs          # View all logs
make logs-auth     # Auth service logs
make logs-db       # PostgreSQL logs
make stop          # Stop all services
```

## 🌐 Service URLs

After starting services:

| Service | URL | Health Check |
|---------|-----|--------------|
| Auth Service | http://localhost:8080 | http://localhost:8080/health |
| Grade Ingestion | http://localhost:8081 | http://localhost:8081/health |
| Grade Calculation | http://localhost:8082 | http://localhost:8082/health |
| Report Generation | http://localhost:8083 | http://localhost:8083/health |
| Audit Logging | http://localhost:8084 | http://localhost:8084/health |
| PostgreSQL | localhost:5432 | - |
| Redis | localhost:6379 | - |

## 🎬 Common Workflows

### First Time Setup
```bash
./scripts/dev-setup.sh
./scripts/deploy-docker.sh --detach
./scripts/health-check.sh
./scripts/test-api.sh
```

### Daily Development
```bash
# Start infrastructure
docker-compose up -d postgres redis

# Work on code
sbt "project authService" run

# Test changes
./scripts/test-api.sh
./scripts/db-count.sh
```

### Testing Changes
```bash
./scripts/stop-all.sh
./scripts/deploy-docker.sh --rebuild --detach
./scripts/health-check.sh
./scripts/test-api.sh
```

### Database Management
```bash
# Check current state
./scripts/db-count.sh

# Clean and reload
./scripts/db-cleanup.sh --reload

# Full reset
./scripts/db-restart.sh --clean
./scripts/db-cleanup.sh --reset
```

### Troubleshooting
```bash
# Check everything
./scripts/health-check.sh

# View logs
./scripts/logs.sh

# Check DB
./scripts/db-count.sh

# Nuclear option
./scripts/stop-all.sh --all
./scripts/deploy-docker.sh --clean --detach
```

## 📱 Quick curl Commands

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

# Submit grade (with JWT token)
curl -X POST http://localhost:8081/grades \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"studentId":"UUID","assignmentId":"UUID","score":95.5}'
```

## 📊 Database Queries

```bash
# Connect to DB
docker-compose exec postgres psql -U postgres -d grades_db

# Or use make
make shell-postgres
```

Common queries:
```sql
-- Count all records
SELECT 
  (SELECT COUNT(*) FROM students) as students,
  (SELECT COUNT(*) FROM teachers) as teachers,
  (SELECT COUNT(*) FROM grades) as grades;

-- Recent grades
SELECT * FROM grades ORDER BY created_at DESC LIMIT 10;

-- Student averages
SELECT s.first_name, s.last_name, AVG(g.score) as avg
FROM students s
JOIN grades g ON s.id = g.student_id
GROUP BY s.id, s.first_name, s.last_name;
```

## 🐛 Troubleshooting

| Problem | Command |
|---------|---------|
| Port in use | `./scripts/stop-all.sh` |
| Services won't start | `./scripts/deploy-docker.sh --rebuild` |
| Database issues | `./scripts/db-restart.sh` |
| Test failures | `./scripts/health-check.sh && ./scripts/logs.sh` |
| Everything broken | `./scripts/stop-all.sh --all && ./scripts/deploy-docker.sh --clean` |

## ✅ Pre-Flight Checklist

Before deploying:
```bash
# Check tools
java -version      # Need 11+
sbt -version      # Need 1.9+
docker --version
docker-compose --version

# Check ports are free
lsof -i :8080     # Should be empty
lsof -i :5432     # Should be empty
```

After deploying:
```bash
./scripts/health-check.sh  # All green
./scripts/test-api.sh      # Tests pass
./scripts/db-count.sh      # Has data
docker-compose ps          # All running
```

## 📚 Documentation

- **[README.md](../README.md)** - Project overview
- **[DEPLOY.md](../DEPLOY.md)** - Complete deployment guide
- **[QUICKSTART.md](../QUICKSTART.md)** - Quick start guide
- **[scripts/README.md](README.md)** - Detailed scripts documentation
- **[docs/API_TESTS.http](../docs/API_TESTS.http)** - API examples

## 💡 Tips

1. Use `./scripts/run.sh` for interactive menu
2. Always check health after deployment: `./scripts/health-check.sh`
3. View logs while testing: `./scripts/logs.sh`
4. Count records frequently: `./scripts/db-count.sh`
5. Use `--detach` for background services
6. Make shortcuts are faster: `make deploy` vs `./scripts/deploy-docker.sh --detach`

---

Run `./scripts/run.sh` for the interactive menu!

