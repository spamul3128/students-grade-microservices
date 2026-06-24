#!/bin/bash
# Restart database and Redis containers
# Options to clean data or preserve it

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Parse arguments
CLEAN_DATA=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --clean|-c)
            CLEAN_DATA=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --clean, -c   Clean all data volumes"
            echo "  --help, -h    Show this help"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "  Database & Redis Restart"
echo "=========================================="

if [ "$CLEAN_DATA" = true ]; then
    log_warning "Restarting with CLEAN data (volumes will be removed)"
    read -p "Continue? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log_info "Operation canceled"
        exit 0
    fi

    log_info "Stopping containers and removing volumes..."
    docker-compose down postgres redis -v

    log_info "Starting containers with fresh data..."
    docker-compose up -d postgres redis
else
    log_info "Restarting containers (data preserved)..."
    docker-compose restart postgres redis
fi

# Wait for services to be healthy
log_info "Waiting for services to be healthy..."
sleep 5

# Check PostgreSQL
log_info "Checking PostgreSQL..."
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker-compose exec -T postgres pg_isready -U postgres >/dev/null 2>&1; then
        log_success "PostgreSQL is ready"
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}[ERROR]${NC} PostgreSQL failed to start"
    exit 1
fi

# Check Redis
log_info "Checking Redis..."
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if docker-compose exec -T redis redis-cli ping >/dev/null 2>&1; then
        log_success "Redis is ready"
        break
    fi
    attempt=$((attempt + 1))
    sleep 1
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}[ERROR]${NC} Redis failed to start"
    exit 1
fi

echo ""
log_success "Database and Redis restarted successfully"
echo ""

# Show connection info
log_info "Connection Information:"
echo "  PostgreSQL: localhost:5432"
echo "    Database: grades_db"
echo "    User:     postgres"
echo "    Password: postgres"
echo ""
echo "  Redis: localhost:6379"
echo ""

# Show record counts if data preserved
if [ "$CLEAN_DATA" = false ]; then
    log_info "Current data status:"
    "$SCRIPT_DIR/db-count.sh"
fi

