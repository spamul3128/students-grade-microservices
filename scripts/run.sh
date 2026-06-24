#!/bin/bash
# Master script with interactive menu for all operations

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

clear

show_banner() {
    echo -e "${CYAN}"
    echo "==========================================="
    echo "   Student Grades Microservices Manager"
    echo "==========================================="
    echo -e "${NC}"
}

show_menu() {
    show_banner
    echo ""
    echo "📦 DEPLOYMENT & SETUP"
    echo "  1. Setup development environment"
    echo "  2. Deploy all services (Docker)"
    echo "  3. Deploy in detached mode"
    echo "  4. Stop all services"
    echo ""
    echo "🧪 TESTING & VERIFICATION"
    echo "  5. Run API tests"
    echo "  6. Health check all services"
    echo "  7. View service logs"
    echo ""
    echo "🗄️  DATABASE MANAGEMENT"
    echo "  8. Show database record counts"
    echo "  9. Clean database tables"
    echo " 10. Reset database (drop & recreate)"
    echo " 11. Restart database & Redis"
    echo " 12. Load sample data"
    echo ""
    echo "📊 MONITORING"
    echo " 13. Show service status"
    echo " 14. Show database connection info"
    echo ""
    echo " 0. Exit"
    echo ""
    echo -e "${YELLOW}-------------------------------------------${NC}"
    read -p "Select option: " choice
    echo ""
}

run_script() {
    local script=$1
    shift
    if [ -f "$SCRIPT_DIR/$script" ]; then
        bash "$SCRIPT_DIR/$script" "$@"
    else
        echo -e "${RED}Error: Script not found: $script${NC}"
    fi
    echo ""
    read -p "Press Enter to continue..."
}

show_service_status() {
    echo "=========================================="
    echo "  Service Status"
    echo "=========================================="
    docker-compose ps
    echo ""
    read -p "Press Enter to continue..."
}

show_db_info() {
    echo "=========================================="
    echo "  Database Connection Information"
    echo "=========================================="
    echo ""
    echo "PostgreSQL:"
    echo "  Host:     localhost"
    echo "  Port:     5432"
    echo "  Database: grades_db"
    echo "  User:     postgres"
    echo "  Password: postgres"
    echo ""
    echo "Redis:"
    echo "  Host:     localhost"
    echo "  Port:     6379"
    echo ""
    echo "Connection string:"
    echo "  postgresql://postgres:postgres@localhost:5432/grades_db"
    echo ""
    read -p "Press Enter to continue..."
}

# Main loop
while true; do
    show_menu

    case $choice in
        1)
            run_script "dev-setup.sh"
            ;;
        2)
            run_script "deploy-docker.sh"
            ;;
        3)
            run_script "deploy-docker.sh" "--detach"
            ;;
        4)
            run_script "stop-all.sh"
            ;;
        5)
            run_script "test-api.sh"
            ;;
        6)
            run_script "health-check.sh"
            ;;
        7)
            read -p "Enter service name (or press Enter for all): " service
            if [ -z "$service" ]; then
                run_script "logs.sh"
            else
                run_script "logs.sh" "--service" "$service"
            fi
            ;;
        8)
            run_script "db-count.sh"
            ;;
        9)
            run_script "db-cleanup.sh" "--clean"
            ;;
        10)
            run_script "db-cleanup.sh" "--reset"
            ;;
        11)
            read -p "Clean data? (yes/no): " clean
            if [ "$clean" = "yes" ]; then
                run_script "db-restart.sh" "--clean"
            else
                run_script "db-restart.sh"
            fi
            ;;
        12)
            run_script "db-cleanup.sh" "--reload"
            ;;
        13)
            show_service_status
            ;;
        14)
            show_db_info
            ;;
        0)
            echo "Goodbye! 👋"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option${NC}"
            sleep 2
            ;;
    esac
done

