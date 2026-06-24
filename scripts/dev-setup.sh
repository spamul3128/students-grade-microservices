#!/bin/bash
# Development environment setup script
# Sets up local development environment with all dependencies

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

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if command -v $1 &> /dev/null; then
        log_success "$1 is installed"
        return 0
    else
        log_warning "$1 is not installed"
        return 1
    fi
}

echo "=========================================="
echo "  Development Environment Setup"
echo "=========================================="
echo ""

# Check required tools
log_info "Checking required tools..."

MISSING_TOOLS=()

if ! check_command "java"; then
    MISSING_TOOLS+=("java (OpenJDK 11 or later)")
fi

if ! check_command "sbt"; then
    MISSING_TOOLS+=("sbt (Scala Build Tool)")
fi

if ! check_command "docker"; then
    MISSING_TOOLS+=("docker")
fi

if ! check_command "docker-compose"; then
    MISSING_TOOLS+=("docker-compose")
fi

if ! check_command "psql"; then
    log_warning "psql (PostgreSQL client) is not installed (optional but recommended)"
fi

if [ ${#MISSING_TOOLS[@]} -gt 0 ]; then
    echo ""
    log_error "Missing required tools:"
    for tool in "${MISSING_TOOLS[@]}"; do
        echo "  - $tool"
    done
    echo ""
    echo "Please install missing tools and run this script again."
    exit 1
fi

echo ""
log_success "All required tools are installed"
echo ""

# Start infrastructure
log_info "Starting infrastructure (PostgreSQL & Redis)..."
docker-compose up -d postgres redis

log_info "Waiting for services to be ready..."
sleep 10

# Verify database connection
log_info "Verifying database connection..."
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
    log_error "PostgreSQL failed to start"
    exit 1
fi

# Compile the project
log_info "Compiling Scala project..."
sbt compile

log_success "Project compiled successfully"

echo ""
echo "=========================================="
echo "  Setup Complete!"
echo "=========================================="
echo ""
log_info "Your development environment is ready!"
echo ""
echo "Next steps:"
echo ""
echo "1. Run all services with Docker:"
echo "   ./scripts/deploy-docker.sh --detach"
echo ""
echo "2. Or run services locally (in separate terminals):"
echo "   Terminal 1: sbt 'project authService' run"
echo "   Terminal 2: sbt 'project gradeIngestion' run"
echo "   Terminal 3: sbt 'project gradeCalculation' run"
echo "   Terminal 4: sbt 'project reportGeneration' run"
echo "   Terminal 5: sbt 'project auditLogging' run"
echo ""
echo "3. Run API tests:"
echo "   ./scripts/test-api.sh"
echo ""
echo "4. Check database records:"
echo "   ./scripts/db-count.sh"
echo ""
echo "Service URLs:"
echo "  Auth Service:        http://localhost:8080"
echo "  Grade Ingestion:     http://localhost:8081"
echo "  Grade Calculation:   http://localhost:8082"
echo "  Report Generation:   http://localhost:8083"
echo "  Audit Logging:       http://localhost:8084"
echo ""
log_success "Happy coding! 🚀"

