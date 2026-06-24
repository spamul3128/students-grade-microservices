#!/bin/bash
# Health check script for all services
# Verifies that all services are running and responding

set -e

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
    echo -e "${GREEN}[✓]${NC} $1"
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
}

check_service() {
    local name=$1
    local url=$2

    if curl -s -f "$url" > /dev/null 2>&1; then
        log_success "$name is healthy"
        return 0
    else
        log_error "$name is not responding"
        return 1
    fi
}

check_docker_service() {
    local container=$1
    local name=$2

    if docker ps | grep -q "$container"; then
        status=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "running")
        if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
            log_success "$name is running"
            return 0
        else
            log_error "$name is unhealthy (status: $status)"
            return 1
        fi
    else
        log_error "$name is not running"
        return 1
    fi
}

echo "=========================================="
echo "  Service Health Check"
echo "=========================================="
echo ""

FAILED=0

# Check Docker containers
log_info "Checking Docker containers..."
check_docker_service "grades-postgres" "PostgreSQL" || ((FAILED++))
check_docker_service "grades-redis" "Redis" || ((FAILED++))

echo ""

# Check microservices
log_info "Checking microservices..."
check_service "Auth Service (8080)" "http://localhost:8080/health" || ((FAILED++))
check_service "Grade Ingestion (8081)" "http://localhost:8081/health" || ((FAILED++))
check_service "Grade Calculation (8082)" "http://localhost:8082/health" || ((FAILED++))
check_service "Report Generation (8083)" "http://localhost:8083/health" || ((FAILED++))
check_service "Audit Logging (8084)" "http://localhost:8084/health" || ((FAILED++))

echo ""

if [ $FAILED -eq 0 ]; then
    echo "=========================================="
    log_success "All services are healthy! ✨"
    echo "=========================================="
    exit 0
else
    echo "=========================================="
    log_error "$FAILED service(s) failed health check"
    echo "=========================================="
    echo ""
    echo "Troubleshooting:"
    echo "  - Check logs: ./scripts/logs.sh"
    echo "  - Restart services: ./scripts/db-restart.sh"
    echo "  - Redeploy: ./scripts/deploy-docker.sh"
    exit 1
fi

