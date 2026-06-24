#!/bin/bash
# Deploy all microservices as Docker containers
# Usage: ./deploy-docker.sh [options]
#   Options:
#     --detach     Run in detached mode
#     --rebuild    Force rebuild images
#     --clean      Clean and rebuild everything (containers, images, volumes, databases, cache)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Parse command line arguments
DETACH_MODE=false
REBUILD=false
CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --detach|-d)
            DETACH_MODE=true
            shift
            ;;
        --rebuild|-r)
            REBUILD=true
            shift
            ;;
        --clean|-c)
            CLEAN=true
            REBUILD=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --detach, -d    Run in detached mode"
            echo "  --rebuild, -r   Force rebuild images"
            echo "  --clean, -c     Clean and rebuild everything (stops containers, removes images, clears DB & cache)"
            echo "  --help, -h      Show this help message"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Comprehensive cleanup function
cleanup_all() {
    log_info "Starting comprehensive cleanup..."

    # Step 1: Stop all running containers
    log_info "Stopping all containers..."
    docker-compose down --remove-orphans || true

    # Step 2: Remove service-specific Docker images
    log_info "Removing service images..."
    docker images | grep "students-grade-microservices" | awk '{print $3}' | xargs -r docker rmi -f 2>/dev/null || true

    # Step 3: Clean up database - drop and recreate
    log_info "Cleaning up PostgreSQL database..."
    docker-compose up -d grades-postgres
    sleep 5

    # Drop all tables and data
    docker exec grades-postgres psql -U postgres -d grades_db -c "
        DROP SCHEMA public CASCADE;
        CREATE SCHEMA public;
        GRANT ALL ON SCHEMA public TO postgres;
        GRANT ALL ON SCHEMA public TO public;
    " 2>/dev/null || log_warning "Database might not exist yet"

    docker-compose stop grades-postgres

    # Step 4: Clean up Redis cache
    log_info "Flushing Redis cache..."
    docker-compose up -d grades-redis
    sleep 3
    docker exec grades-redis redis-cli FLUSHALL 2>/dev/null || log_warning "Redis might not be running"
    docker-compose stop grades-redis

    # Step 5: Remove all volumes
    log_info "Removing Docker volumes..."
    docker-compose down -v

    # Step 6: Prune dangling images and containers
    log_info "Pruning Docker system..."
    docker system prune -f

    log_success "Comprehensive cleanup completed!"
}

# Clean if requested
if [ "$CLEAN" = true ]; then
    cleanup_all
fi

# Build the project
log_info "Building Scala project..."
sbt clean compile

# Build Docker images
BUILD_CMD="docker-compose build"
if [ "$REBUILD" = true ]; then
    BUILD_CMD="$BUILD_CMD --no-cache"
fi

log_info "Building Docker images..."
eval "$BUILD_CMD"
log_success "Docker images built successfully"

# Deploy services
DEPLOY_CMD="docker-compose up"
if [ "$DETACH_MODE" = true ]; then
    DEPLOY_CMD="$DEPLOY_CMD -d"
fi

log_info "Deploying services..."
eval "$DEPLOY_CMD"

if [ "$DETACH_MODE" = true ]; then
    log_success "Services deployed in detached mode"
    log_info "Waiting for services to be healthy..."
    sleep 10

    # Initialize database if clean was run
    if [ "$CLEAN" = true ]; then
        log_info "Initializing database schema..."
        docker exec -i grades-postgres psql -U postgres -d grades_db < database/schema.sql
        log_success "Database schema created"

        log_info "Loading demo data..."
        docker exec -i grades-postgres psql -U postgres -d grades_db < database/demo-data.sql
        log_success "Demo data loaded"
    fi

    log_info "Service Status:"
    docker-compose ps

    log_info "\nService URLs:"
    echo "  Auth Service:        http://localhost:8080"
    echo "  Grade Ingestion:     http://localhost:8081"
    echo "  Grade Calculation:   http://localhost:8082"
    echo "  Report Generation:   http://localhost:8083"
    echo "  Audit Logging:       http://localhost:8084"
    echo "  PostgreSQL:          localhost:5432"
    echo "  Redis:               localhost:6379"

    log_info "\nUseful commands:"
    echo "  View logs:           docker-compose logs -f"
    echo "  Stop services:       docker-compose down"
    echo "  Restart services:    docker-compose restart"
    echo "  View status:         docker-compose ps"
    echo "  Run demo:            bash demo.sh"
else
    log_success "Services deployed successfully"

    # Initialize database if clean was run
    if [ "$CLEAN" = true ]; then
        log_info "Initializing database schema..."
        sleep 5  # Wait a bit more for DB to be ready
        docker exec -i grades-postgres psql -U postgres -d grades_db < database/schema.sql
        log_success "Database schema created"

        log_info "Loading demo data..."
        docker exec -i grades-postgres psql -U postgres -d grades_db < database/demo-data.sql
        log_success "Demo data loaded"
    fi
fi

