# deploy-docker.sh Script Update

## Date: June 10, 2026

## Overview
Updated the `scripts/deploy-docker.sh` script with comprehensive cleanup functionality to stop containers, remove images, clean databases, and flush Redis cache.

## New Features

### Comprehensive Cleanup Function
The `--clean` option now performs a complete cleanup:

1. **Stop All Containers**
   - Stops all running containers using `docker-compose down`
   - Removes orphaned containers

2. **Remove Service Images**
   - Removes all Docker images for the student-grades-microservices
   - Uses grep pattern matching to find and remove service-specific images

3. **Clean PostgreSQL Database**
   - Temporarily starts PostgreSQL container
   - Drops and recreates the public schema
   - Removes all tables and data
   - Stops the container

4. **Flush Redis Cache**
   - Temporarily starts Redis container
   - Executes FLUSHALL command to clear all cache data
   - Stops the container

5. **Remove Docker Volumes**
   - Removes all associated volumes including database and cache data
   - Ensures clean state for next deployment

6. **Prune Docker System**
   - Removes dangling images and containers
   - Frees up disk space

### Automatic Database Initialization
When using `--clean`, the script now automatically:
- Creates database schema from `database/schema.sql`
- Loads demo data from `database/demo-data.sql`
- Ensures services start with fresh, consistent data

## Usage

### Basic Deployment
```bash
./scripts/deploy-docker.sh --detach
```

### Clean Deployment (Recommended)
```bash
./scripts/deploy-docker.sh --clean --detach
```

This will:
1. Stop and remove all containers
2. Remove all service images
3. Clean database and Redis
4. Rebuild everything from scratch
5. Deploy in detached mode
6. Initialize database with schema and demo data

### Rebuild Without Clean
```bash
./scripts/deploy-docker.sh --rebuild --detach
```

### Help
```bash
./scripts/deploy-docker.sh --help
```

## Options

| Option | Short | Description |
|--------|-------|-------------|
| `--detach` | `-d` | Run containers in detached mode (background) |
| `--rebuild` | `-r` | Force rebuild of Docker images (no cache) |
| `--clean` | `-c` | Complete cleanup + rebuild (containers, images, DB, cache) |
| `--help` | `-h` | Show help message |

## Cleanup Details

### What Gets Cleaned
- ✅ All running containers (stopped and removed)
- ✅ All service Docker images
- ✅ PostgreSQL database schema and data
- ✅ Redis cache (all keys flushed)
- ✅ Docker volumes (postgres-data, redis-data)
- ✅ Dangling images and containers

### What Gets Preserved
- ❌ Base images (openjdk, postgres, redis)
- ❌ SBT/Scala dependencies (in local cache)
- ❌ Source code

## Example Output

```bash
$ ./scripts/deploy-docker.sh --clean --detach

[INFO] Starting comprehensive cleanup...
[INFO] Stopping all containers...
[INFO] Removing service images...
[INFO] Cleaning up PostgreSQL database...
[INFO] Flushing Redis cache...
[INFO] Removing Docker volumes...
[INFO] Pruning Docker system...
[SUCCESS] Comprehensive cleanup completed!
[INFO] Building Scala project...
[INFO] Building Docker images...
[SUCCESS] Docker images built successfully
[INFO] Deploying services...
[SUCCESS] Services deployed in detached mode
[INFO] Waiting for services to be healthy...
[INFO] Initializing database schema...
[SUCCESS] Database schema created
[INFO] Loading demo data...
[SUCCESS] Demo data loaded
[INFO] Service Status:
...
```

## Testing the Update

Run a clean deployment:
```bash
cd /Users/spamul002c@cable.comcast.com/Scala-Practice/students-grade-microservices
./scripts/deploy-docker.sh --clean --detach
```

Verify services are running:
```bash
docker-compose ps
```

Test authentication:
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password"}' | jq .
```

Run the demo:
```bash
bash demo.sh
```

## Benefits

1. **Consistent State**: Clean deployments ensure no leftover data
2. **Troubleshooting**: Easy to reset to known good state
3. **Development**: Quick way to test changes with fresh data
4. **CI/CD Ready**: Can be automated in deployment pipelines
5. **Storage Management**: Cleans up disk space regularly

## Related Files

- `scripts/deploy-docker.sh` - Main deployment script
- `database/schema.sql` - Database schema
- `database/demo-data.sql` - Demo data
- `docker-compose.yml` - Docker services configuration

## Notes

- The cleanup is **destructive** - all data will be lost
- Use `--clean` only when you want a fresh start
- Without `--clean`, existing data persists
- Database initialization only runs after `--clean`

