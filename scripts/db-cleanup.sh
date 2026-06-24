#!/bin/bash
# Database cleanup script
# Cleans all data from tables or drops and recreates database

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Database connection details
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="grades_db"
DB_USER="postgres"
DB_PASSWORD="postgres"

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

# Execute SQL command
exec_sql() {
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "$1" -t
}

# Execute SQL file
exec_sql_file() {
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f "$1"
}

# Check if database is accessible
check_db_connection() {
    log_info "Checking database connection..."
    if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c '\q' 2>/dev/null; then
        log_success "Database is accessible"
        return 0
    else
        log_error "Cannot connect to database"
        return 1
    fi
}

# Clean all tables
clean_tables() {
    log_info "Cleaning all tables..."

    # Disable foreign key checks and truncate all tables
    exec_sql "TRUNCATE TABLE audit_logs CASCADE;"
    exec_sql "TRUNCATE TABLE course_grades CASCADE;"
    exec_sql "TRUNCATE TABLE grades CASCADE;"
    exec_sql "TRUNCATE TABLE assignments CASCADE;"
    exec_sql "TRUNCATE TABLE courses CASCADE;"
    exec_sql "TRUNCATE TABLE teachers CASCADE;"
    exec_sql "TRUNCATE TABLE users CASCADE;"
    exec_sql "TRUNCATE TABLE students CASCADE;"

    log_success "All tables cleaned"
}

# Reset database (drop and recreate)
reset_database() {
    log_warning "This will DROP and RECREATE the entire database!"
    read -p "Are you sure? (yes/no): " confirm

    if [ "$confirm" != "yes" ]; then
        log_info "Operation canceled"
        exit 0
    fi

    log_info "Dropping database..."
    cd "$PROJECT_ROOT"
    docker-compose exec -T postgres psql -U postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"

    log_info "Creating database..."
    docker-compose exec -T postgres psql -U postgres -c "CREATE DATABASE $DB_NAME;"

    log_info "Running schema..."
    exec_sql_file "$PROJECT_ROOT/database/schema.sql"

    log_success "Database reset completed"
}

# Load sample data
load_sample_data() {
    log_info "Loading sample data..."
    exec_sql_file "$PROJECT_ROOT/database/sample-data.sql"
    log_success "Sample data loaded"
}

# Show menu
show_menu() {
    echo "=========================================="
    echo "  Database Cleanup Utility"
    echo "=========================================="
    echo "1. Clean all tables (keep schema)"
    echo "2. Reset database (drop & recreate)"
    echo "3. Clean and reload sample data"
    echo "4. Show record counts"
    echo "5. Exit"
    echo ""
    read -p "Select option: " choice

    case $choice in
        1)
            check_db_connection && clean_tables
            ;;
        2)
            check_db_connection && reset_database
            ;;
        3)
            check_db_connection && clean_tables && load_sample_data
            ;;
        4)
            check_db_connection && "$SCRIPT_DIR/db-count.sh"
            ;;
        5)
            log_info "Exiting..."
            exit 0
            ;;
        *)
            log_error "Invalid option"
            exit 1
            ;;
    esac
}

# Parse command line arguments
if [ $# -eq 0 ]; then
    show_menu
else
    case $1 in
        --clean)
            check_db_connection && clean_tables
            ;;
        --reset)
            check_db_connection && reset_database
            ;;
        --reload)
            check_db_connection && clean_tables && load_sample_data
            ;;
        --help|-h)
            echo "Usage: $0 [option]"
            echo "Options:"
            echo "  --clean     Clean all tables"
            echo "  --reset     Drop and recreate database"
            echo "  --reload    Clean and reload sample data"
            echo "  --help      Show this help"
            echo ""
            echo "Run without options for interactive menu"
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
fi

