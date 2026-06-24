#!/bin/bash
# Validation script to check all automation scripts and configurations
# Run this to verify everything is set up correctly

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

ERRORS=0
WARNINGS=0
SUCCESS=0

log_success() {
    echo -e "${GREEN}[✓]${NC} $1"
    SUCCESS=$((SUCCESS + 1))
}

log_error() {
    echo -e "${RED}[✗]${NC} $1"
    ERRORS=$((ERRORS + 1))
}

log_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
    WARNINGS=$((WARNINGS + 1))
}

log_info() {
    echo -e "${BLUE}[i]${NC} $1"
}

echo "=========================================="
echo "  Automation Scripts Validation"
echo "=========================================="
echo ""

# Check script directory
log_info "Checking scripts directory..."
if [ -d "scripts" ]; then
    log_success "Scripts directory exists"
else
    log_error "Scripts directory not found"
fi

# Check all required scripts exist
log_info "Checking required scripts..."
REQUIRED_SCRIPTS=(
    "run.sh"
    "deploy-docker.sh"
    "stop-all.sh"
    "dev-setup.sh"
    "test-api.sh"
    "health-check.sh"
    "db-count.sh"
    "db-cleanup.sh"
    "db-restart.sh"
    "logs.sh"
)

for script in "${REQUIRED_SCRIPTS[@]}"; do
    if [ -f "scripts/$script" ]; then
        if [ -x "scripts/$script" ]; then
            log_success "$script exists and is executable"
        else
            log_error "$script exists but is not executable"
        fi
    else
        log_error "$script not found"
    fi
done

# Check script syntax
log_info "Validating script syntax..."
for script in scripts/*.sh; do
    if bash -n "$script" 2>/dev/null; then
        log_success "$(basename $script) syntax is valid"
    else
        log_error "$(basename $script) has syntax errors"
    fi
done

# Check documentation files
log_info "Checking documentation files..."
DOC_FILES=(
    "README.md"
    "DEPLOY.md"
    "QUICKSTART.md"
    "AUTOMATION_OVERVIEW.md"
    "SCRIPTS_SUMMARY.md"
    "scripts/README.md"
    "scripts/COMMAND_REFERENCE.md"
)

for doc in "${DOC_FILES[@]}"; do
    if [ -f "$doc" ]; then
        log_success "$doc exists"
    else
        log_warning "$doc not found"
    fi
done

# Check configuration files
log_info "Checking configuration files..."
CONFIG_FILES=(
    "docker-compose.yml"
    "Makefile"
    "build.sbt"
)

for config in "${CONFIG_FILES[@]}"; do
    if [ -f "$config" ]; then
        log_success "$config exists"
    else
        log_error "$config not found"
    fi
done

# Check Makefile targets
log_info "Validating Makefile..."
if make -n help >/dev/null 2>&1; then
    log_success "Makefile is valid"
else
    log_error "Makefile has errors"
fi

# Check for correct Redis port
log_info "Checking Redis port configuration..."
if grep -q "6379:6379" docker-compose.yml 2>/dev/null; then
    log_success "Redis port is correctly configured (6379)"
else
    log_warning "Redis port may be incorrectly configured"
fi

# Check database schema
log_info "Checking database files..."
if [ -f "database/schema.sql" ]; then
    log_success "Database schema file exists"
else
    log_warning "Database schema file not found"
fi

if [ -f "database/sample-data.sql" ]; then
    log_success "Sample data file exists"
else
    log_warning "Sample data file not found"
fi

# Check for required tools
log_info "Checking required tools..."

if command -v docker >/dev/null 2>&1; then
    log_success "Docker is installed"
else
    log_warning "Docker is not installed (required for deployment)"
fi

if command -v docker-compose >/dev/null 2>&1; then
    log_success "Docker Compose is installed"
else
    log_warning "Docker Compose is not installed (required for deployment)"
fi

if command -v sbt >/dev/null 2>&1; then
    log_success "SBT is installed"
else
    log_warning "SBT is not installed (required for development)"
fi

if command -v java >/dev/null 2>&1; then
    log_success "Java is installed"
else
    log_warning "Java is not installed (required for development)"
fi

if command -v curl >/dev/null 2>&1; then
    log_success "curl is installed"
else
    log_warning "curl is not installed (required for API testing)"
fi

# Summary
echo ""
echo "=========================================="
echo "  Validation Summary"
echo "=========================================="
echo -e "${GREEN}Success: $SUCCESS${NC}"
echo -e "${YELLOW}Warnings: $WARNINGS${NC}"
echo -e "${RED}Errors: $ERRORS${NC}"
echo ""

if [ $ERRORS -eq 0 ]; then
    echo "=========================================="
    log_success "All checks passed! ✨"
    echo "=========================================="
    echo ""
    echo "You're ready to go!"
    echo ""
    echo "Next steps:"
    echo "  1. Run interactive menu: ./scripts/run.sh"
    echo "  2. Or deploy directly: ./scripts/deploy-docker.sh --detach"
    echo "  3. Read documentation: cat AUTOMATION_OVERVIEW.md"
    echo ""
    exit 0
else
    echo "=========================================="
    log_error "Validation failed with $ERRORS error(s)"
    echo "=========================================="
    echo ""
    echo "Please fix the errors above and run this script again."
    exit 1
fi

