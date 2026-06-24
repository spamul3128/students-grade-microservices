#!/bin/bash
# Stop all services
# Options to remove volumes or keep data

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
REMOVE_VOLUMES=false
REMOVE_IMAGES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --volumes|-v)
            REMOVE_VOLUMES=true
            shift
            ;;
        --images|-i)
            REMOVE_IMAGES=true
            shift
            ;;
        --all|-a)
            REMOVE_VOLUMES=true
            REMOVE_IMAGES=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --volumes, -v   Remove data volumes"
            echo "  --images, -i    Remove Docker images"
            echo "  --all, -a       Remove volumes and images"
            echo "  --help, -h      Show this help"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

echo "=========================================="
echo "  Stopping All Services"
echo "=========================================="

log_info "Stopping Docker containers..."

if [ "$REMOVE_VOLUMES" = true ]; then
    log_warning "This will remove all data volumes!"
    docker-compose down -v
    log_success "Containers stopped and volumes removed"
else
    docker-compose down
    log_success "Containers stopped (data preserved)"
fi

if [ "$REMOVE_IMAGES" = true ]; then
    log_info "Removing Docker images..."
    docker-compose down --rmi local
    log_success "Docker images removed"
fi

log_success "All services stopped"

if [ "$REMOVE_VOLUMES" = false ]; then
    echo ""
    log_info "Note: Data volumes are preserved"
    echo "To remove volumes, use: $0 --volumes"
fi

