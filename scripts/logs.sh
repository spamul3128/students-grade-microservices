#!/bin/bash
# View logs from services
# Can filter by service name

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Colors
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# Default to all services
SERVICE=""
FOLLOW=true
TAIL_LINES="100"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --service|-s)
            SERVICE="$2"
            shift 2
            ;;
        --no-follow|-n)
            FOLLOW=false
            shift
            ;;
        --tail|-t)
            TAIL_LINES="$2"
            shift 2
            ;;
        --help|-h)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  --service, -s      Service name (postgres, redis, auth-service, etc.)"
            echo "  --no-follow, -n    Don't follow logs"
            echo "  --tail, -t         Number of lines to show (default: 100)"
            echo "  --help, -h         Show this help"
            echo ""
            echo "Available services:"
            echo "  - postgres"
            echo "  - redis"
            echo "  - auth-service"
            echo "  - grade-ingestion"
            echo "  - grade-calculation"
            echo "  - report-generation"
            echo "  - audit-logging"
            exit 0
            ;;
        *)
            SERVICE="$1"
            shift
            ;;
    esac
done

CMD="docker-compose logs --tail=$TAIL_LINES"

if [ "$FOLLOW" = true ]; then
    CMD="$CMD -f"
fi

if [ ! -z "$SERVICE" ]; then
    log_info "Viewing logs for: $SERVICE"
    CMD="$CMD $SERVICE"
else
    log_info "Viewing logs for all services"
fi

eval "$CMD"

